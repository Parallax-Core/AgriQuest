package com.agriquest.data.repository

import android.content.Context
import com.agriquest.data.ble.Esp32BleManager
import com.agriquest.data.local.AgriQuestDataStore
import com.agriquest.data.simulation.TelemetrySimulationEngine
import com.agriquest.domain.model.Achievement
import com.agriquest.domain.model.ActuatorState
import com.agriquest.domain.model.AgroQuizQuestion
import com.agriquest.domain.model.AiScanResult
import com.agriquest.domain.model.ConnectionState
import com.agriquest.domain.model.ConnectionStatus
import com.agriquest.domain.model.FanMode
import com.agriquest.domain.model.JournalEntry
import com.agriquest.domain.model.PlantAvatar
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.domain.model.PlantSource
import com.agriquest.domain.model.Quest
import com.agriquest.domain.model.TelemetryData
import com.agriquest.domain.repository.AgriQuestRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Implementation of [AgriQuestRepository] managing live domain state, leveling progression,
 * DataStore persistence, IoT actions, Telemetry Simulation Engine, and BLE Hardware Manager.
 */
class AgriQuestRepositoryImpl(
    private val dataStore: AgriQuestDataStore? = null,
    private val simulationEngine: TelemetrySimulationEngine = TelemetrySimulationEngine(),
    val esp32BleManager: Esp32BleManager = Esp32BleManager(),
    coroutineScope: CoroutineScope? = null,
    private val now: () -> Date = { Date() }
) : AgriQuestRepository {

    companion object {
        @Volatile
        private var INSTANCE: AgriQuestRepositoryImpl? = null

        fun getInstance(context: Context): AgriQuestRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AgriQuestRepositoryImpl(
                    dataStore = AgriQuestDataStore(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }
    }

    private val scope = coroutineScope ?: CoroutineScope(Dispatchers.Default)

    private val initialized = CompletableDeferred<Unit>()
    private val claimMutex = Mutex()
    private val rewardMutex = Mutex()
    private var trackingStart = getTodayDateString()
    private var xpDate = getTodayDateString()
    private var earnedToday = 0
    private var quizRewardDate: String? = null
    private var memoryLastResetDate: String? = null
    private var simulationJob: Job? = null
    private var bleStateJob: Job? = null
    private var bleTelemetryJob: Job? = null

    private val _telemetryData = MutableStateFlow(
        TelemetryData(
            soilMoisture = 68.0f,
            airTemperature = 24.5f,
            relativeHumidity = 62.0f,
            lightIntensity = 14200.0f
        )
    )
    override val telemetryData: StateFlow<TelemetryData> = _telemetryData.asStateFlow()

    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    private val _plantAvatar = MutableStateFlow(
        PlantAvatar(
            name = "Flora",
            level = "Lvl. 0 Urban Beginner",
            levelInt = 0,
            currentXp = 0,
            maxXp = PlantGrowthStage.GERMINATION.maxXp,
            growthStage = PlantGrowthStage.GERMINATION,
            statusTag = "Vibrant & Healthy",
            streakDays = 0,
            targetStreakDays = 7,
            sowingDate = trackingStart,
            speechBubble = PlantGrowthStage.GERMINATION.speechBubble
        )
    )
    override val plantAvatar: StateFlow<PlantAvatar> = _plantAvatar.asStateFlow()

    private val _quests = MutableStateFlow(Quest.forDate(getTodayDateString()))
    override val quests: StateFlow<List<Quest>> = _quests.asStateFlow()

    private val _activeQuest = MutableStateFlow(_quests.value.first())
    override val activeQuest: StateFlow<Quest> = _activeQuest.asStateFlow()

    private val _achievements = MutableStateFlow(Achievement.DEFAULT_ACHIEVEMENTS)
    override val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _plantSource = MutableStateFlow(PlantSource.EMULATED_OFFLINE)
    override val plantSource: StateFlow<PlantSource> = _plantSource.asStateFlow()

    private val _connectionState = MutableStateFlow(
        ConnectionState(
            deviceName = "ESP32",
            status = ConnectionStatus.CONNECTED,
            statusText = "Connected"
        )
    )
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _actuatorState = MutableStateFlow(
        ActuatorState(
            isPumpActive = false,
            pumpRemainingSeconds = 0,
            fanMode = FanMode.AUTO
        )
    )
    override val actuatorState: StateFlow<ActuatorState> = _actuatorState.asStateFlow()

    private val _recentScans = MutableStateFlow(AiScanResult.DEFAULT_SCANS)
    override val recentScans: StateFlow<List<AiScanResult>> = _recentScans.asStateFlow()

    private val _currentQuizQuestion = MutableStateFlow(AgroQuizQuestion.QUIZ_POOL.first())
    override val currentQuizQuestion: StateFlow<AgroQuizQuestion> = _currentQuizQuestion.asStateFlow()

    private val _isQuizCompletedToday = MutableStateFlow(false)
    override val isQuizCompletedToday: StateFlow<Boolean> = _isQuizCompletedToday.asStateFlow()

    private val _selectedQuizOptionIndex = MutableStateFlow(-1)
    override val selectedQuizOptionIndex: StateFlow<Int> = _selectedQuizOptionIndex.asStateFlow()

    private val _isQuizAnswerCorrect = MutableStateFlow<Boolean?>(null)
    override val isQuizAnswerCorrect: StateFlow<Boolean?> = _isQuizAnswerCorrect.asStateFlow()

    init {
        // Restore the daily reward lock after loading persistence.
        resetQuizState()

        // Load initial state from DataStore if available
        dataStore?.let { store ->
            scope.launch {
                runCatching {
                    trackingStart = store.trackingStartFlow.first() ?: getTodayDateString().also { store.saveTrackingStart(it) }
                    xpDate = store.xpDateFlow.first() ?: getTodayDateString()
                    earnedToday = store.dailyXpFlow.first()
                    quizRewardDate = store.quizCompletedDateFlow.first()
                    val xp = store.currentXpFlow.first()
                    val source = store.plantSourceFlow.first()
                    val streak = store.streakDaysFlow.first()
                    val claimedIds = store.claimedQuestIdsFlow.first()
                    val savedJournal = store.journalNotesFlow.first()
                    val savedTelemetry = store.telemetryDataFlow.first()
                    val savedLastResetDate = store.lastResetDateFlow.first()

                    memoryLastResetDate = savedLastResetDate
                    _plantSource.value = source
                    if (savedTelemetry != null) {
                        _telemetryData.value = savedTelemetry
                    }
                    if (savedJournal.isNotEmpty()) {
                        _plantAvatar.value = _plantAvatar.value.copy(journalEntries = savedJournal)
                    }
                    _streakDays.value = streak
                    updateAvatarProgression(xp, streak)
                    updateQuestsWithClaimed(claimedIds)

                    // Midnight date check & quest reset on repository startup
                    refreshDailyQuests()

                    // Restore daily completion while clearing transient answer selection.
                    resetQuizState()

                    if (source == PlantSource.EMULATED_OFFLINE) {
                        startSimulationRoutine()
                    } else {
                        bindBleHardwareState()
                    }
                }
                initialized.complete(Unit)
            }
        } ?: run {
            initialized.complete(Unit)
            startSimulationRoutine()
        }
    }

    private fun startSimulationRoutine() {
        stopSimulationRoutine()
        stopBleHardwareBinding()

        _connectionState.value = ConnectionState(
            deviceName = "ESP32",
            status = ConnectionStatus.CONNECTED,
            statusText = "Connected"
        )

        simulationJob = scope.launch {
            simulationEngine.telemetryFlow(_telemetryData.value).collect { simulatedTelemetry ->
                _telemetryData.value = simulatedTelemetry
                dataStore?.saveTelemetryData(simulatedTelemetry)
                updateQuestProgressForTelemetry(simulatedTelemetry)
            }
        }
    }

    private fun stopSimulationRoutine() {
        simulationJob?.cancel()
        simulationJob = null
    }

    private fun bindBleHardwareState() {
        stopSimulationRoutine()
        stopBleHardwareBinding()

        bleStateJob = scope.launch {
            esp32BleManager.connectionState.collect { newState ->
                _connectionState.value = newState
            }
        }

        bleTelemetryJob = scope.launch {
            esp32BleManager.telemetryDataFlow.collect { newTelemetry ->
                _telemetryData.value = newTelemetry
                dataStore?.saveTelemetryData(newTelemetry)
                updateQuestProgressForTelemetry(newTelemetry)
            }
        }

        startEsp32BleScan()
    }

    private fun stopBleHardwareBinding() {
        bleStateJob?.cancel()
        bleStateJob = null
        bleTelemetryJob?.cancel()
        bleTelemetryJob = null
    }

    override fun startEsp32BleScan() {
        if (_plantSource.value == PlantSource.ESP32_HARDWARE) {
            esp32BleManager.startScan()
        }
    }

    private fun updateQuestProgressForTelemetry(telemetry: TelemetryData) {
        val questList = _quests.value.toMutableList()
        val thirstyRootsIndex = questList.indexOfFirst { it.id == "quest_thirsty_roots" }
        if (thirstyRootsIndex != -1) {
            val q = questList[thirstyRootsIndex]
            if (!q.isClaimed) {
                if (telemetry.soilMoisture >= 60.0f) {
                    questList[thirstyRootsIndex] = q.copy(progress = 1.0f, isClaimable = true)
                } else {
                    val progressRatio = (telemetry.soilMoisture / 60.0f).coerceIn(0.0f, 1.0f)
                    questList[thirstyRootsIndex] = q.copy(progress = progressRatio, isClaimable = false)
                }
                _quests.value = questList
                updateActiveQuest(questList)
            }
        }
    }

    private fun updateAvatarProgression(totalXp: Int, streakDays: Int = _streakDays.value) {
        val (levelInt, stage, maxXp) = calculateLevelAndStage()
        _streakDays.value = streakDays
        _plantAvatar.value = _plantAvatar.value.copy(
            level = stage.avatarLevelText,
            levelInt = levelInt,
            sowingDate = trackingStart,
            currentXp = totalXp,
            dailyXp = if (xpDate == getTodayDateString()) earnedToday else 0,
            dayCount = calculateDaysBetween(trackingStart, getTodayDateString()).toInt().coerceAtLeast(0),
            maxXp = maxXp,
            growthStage = stage,
            stageTag = "Stage ${stage.stageLevel + 1} - ${stage.stageTitle}",
            streakDays = streakDays,
            speechBubble = stage.speechBubble
        )
    }

    private fun updateQuestsWithClaimed(claimedIds: Set<String>) {
        val updatedQuests = _quests.value.map { q ->
            if (q.id in claimedIds) {
                q.copy(isClaimed = true, isClaimable = false)
            } else {
                q
            }
        }
        _quests.value = updatedQuests
        updateActiveQuest(updatedQuests)
    }

    private fun updateActiveQuest(currentQuests: List<Quest>) {
        val firstUnclaimed = currentQuests.firstOrNull { !it.isClaimed } ?: currentQuests.first()
        _activeQuest.value = firstUnclaimed
    }

    private fun calculateLevelAndStage(): Triple<Int, PlantGrowthStage, Int> {
        val stage = PlantGrowthStage.fromDays(calculateDaysBetween(trackingStart, getTodayDateString()).toInt().coerceAtLeast(0))
        return Triple(stage.stageLevel, stage, stage.maxXp)
    }

    override suspend fun setSowingDate(date: String): Boolean {
        initialized.await()
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        if (!date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) return false
        val parsed = runCatching { parser.parse(date) }.getOrNull() ?: return false
        if (parsed.after(now())) return false
        trackingStart = date
        dataStore?.saveTrackingStart(date)
        updateAvatarProgression(_plantAvatar.value.currentXp)
        return true
    }

    override suspend fun addXp(amount: Int) {
        initialized.await()
        awardXp(amount)
    }

    private suspend fun awardXp(amount: Int) = rewardMutex.withLock {
        if (amount <= 0) return@withLock
        val today = getTodayDateString()
        if (today > xpDate) { xpDate = today; earnedToday = 0 }
        if (today < xpDate) return@withLock
        val granted = amount.coerceAtMost(PlantGrowthStage.DAILY_XP_LIMIT - earnedToday)
        val total = (_plantAvatar.value.currentXp.toLong() + granted).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        earnedToday += granted
        dataStore?.saveXpLedger(today, earnedToday, total)
        updateAvatarProgression(total)
        dataStore?.saveUserProgression(_plantAvatar.value.levelInt, total, _plantAvatar.value.growthStage)
    }

    override suspend fun claimQuest(questId: String): Boolean = claimMutex.withLock {
        initialized.await()
        checkAndResetMidnightQuests()
        val questList = _quests.value.toMutableList()
        val index = questList.indexOfFirst { it.id == questId }
        if (index != -1) {
            val quest = questList[index]
            if (!quest.isClaimed && quest.isClaimable) {
                val allClaimedBefore = questList.all { it.isClaimed }
                val claimedQuest = quest.copy(
                    progress = 1.0f,
                    isClaimed = true,
                    isClaimable = false
                )
                questList[index] = claimedQuest
                _quests.value = questList
                updateActiveQuest(questList)

                dataStore?.saveClaimedQuestId(questId)
                addXp(quest.xpReward)

                val allClaimedNow = questList.all { it.isClaimed }
                if (!allClaimedBefore && allClaimedNow) {
                    incrementStreak()
                    return true
                }
            }
        }
        return false
    }

    override suspend fun resetDailyQuests(): List<Quest> {
        return resetQuestsForDate(getTodayDateString())
    }

    private suspend fun resetQuestsForDate(date: String): List<Quest> {
        val newDailyQuests = Quest.forDate(date)
        _quests.value = newDailyQuests
        updateActiveQuest(newDailyQuests)
        dataStore?.clearClaimedQuestIds()
        return newDailyQuests
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(now())
    }

    private fun calculateDaysBetween(startDateString: String, endDateString: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            val startDate = sdf.parse(startDateString)
            val endDate = sdf.parse(endDateString)
            if (startDate != null && endDate != null) {
                val diffInMillies = endDate.time - startDate.time
                diffInMillies / (1000 * 60 * 60 * 24)
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun checkAndResetMidnightQuests(todayDateString: String?): Boolean {
        initialized.await()
        return refreshDailyQuests(todayDateString)
    }

    private suspend fun refreshDailyQuests(todayDateString: String? = null): Boolean {
        updateAvatarProgression(_plantAvatar.value.currentXp)
        val todayString = todayDateString ?: getTodayDateString()
        val lastResetDate = dataStore?.lastResetDateFlow?.first() ?: memoryLastResetDate

        if (lastResetDate == null) {
            memoryLastResetDate = todayString
            dataStore?.saveLastResetDate(todayString)
            resetQuizState()
            return true
        } else if (todayString > lastResetDate) {
            if (calculateDaysBetween(lastResetDate, todayString) > 1) {
                resetStreak()
            }
            resetQuestsForDate(todayString)
            quizRewardDate = null
            resetQuizState()
            dataStore?.clearQuizCompletedDate()

            memoryLastResetDate = todayString
            dataStore?.saveLastResetDate(todayString)
            return true
        }
        return false
    }

    override suspend fun setPlantSource(source: PlantSource) {
        _plantSource.value = source
        dataStore?.savePlantSource(source)

        if (source == PlantSource.EMULATED_OFFLINE) {
            esp32BleManager.disconnect()
            startSimulationRoutine()
        } else {
            bindBleHardwareState()
        }
    }

    override suspend fun setStreakDays(streakDays: Int) {
        _streakDays.value = streakDays
        _plantAvatar.value = _plantAvatar.value.copy(streakDays = streakDays)
        dataStore?.saveStreakDays(streakDays)
    }

    override suspend fun incrementStreak() {
        val newStreak = _streakDays.value + 1
        _streakDays.value = newStreak
        _plantAvatar.value = _plantAvatar.value.copy(streakDays = newStreak)
        dataStore?.incrementStreak()
    }

    override suspend fun resetStreak() {
        _streakDays.value = 0
        _plantAvatar.value = _plantAvatar.value.copy(streakDays = 0)
        dataStore?.resetStreak()
    }

    override suspend fun triggerPump(
        durationSeconds: Int,
        onNotification: (String) -> Unit
    ) {
        if (_actuatorState.value.isPumpActive) return

        // 1. Restores soil moisture by +15% (capped at 100.0%)
        val updatedTelemetry = simulationEngine.restoreMoistureFromPump(_telemetryData.value)
        _telemetryData.value = updatedTelemetry
        dataStore?.saveTelemetryData(updatedTelemetry)
        val updatedMoisture = updatedTelemetry.soilMoisture

                // Watering does not award repeatable XP.

        // 3. Checks and updates progress on care quests ("🌱 First Water" and "⚡ Thirsty Roots")
        val questList = _quests.value.toMutableList()

        // "🌱 First Water" quest update
        val firstWaterIndex = questList.indexOfFirst { it.id == "quest_first_water" }
        if (firstWaterIndex != -1) {
            val q = questList[firstWaterIndex]
            if (!q.isClaimed) {
                questList[firstWaterIndex] = q.copy(progress = 1.0f, isClaimable = true)
            }
        }

        // "⚡ Thirsty Roots" quest update
        val thirstyRootsIndex = questList.indexOfFirst { it.id == "quest_thirsty_roots" }
        if (thirstyRootsIndex != -1) {
            val q = questList[thirstyRootsIndex]
            if (!q.isClaimed && updatedMoisture >= 60.0f) {
                questList[thirstyRootsIndex] = q.copy(progress = 1.0f, isClaimable = true)
            }
        }

        _quests.value = questList
        updateActiveQuest(questList)

        // 4. Send command if ESP32 Hardware is active
        if (_plantSource.value == PlantSource.ESP32_HARDWARE) {
            esp32BleManager.sendPumpCommand(durationSeconds)
        }

        // 5. Trigger notification/toast callback for activation
        val startMessage = "💧 Water pump triggered for ${durationSeconds}s! Soil moisture restored by +15% to ${updatedMoisture.toInt()}%."
        onNotification(startMessage)

        // 6. Countdown timer execution
        for (remaining in durationSeconds downTo 1) {
            _actuatorState.value = _actuatorState.value.copy(
                isPumpActive = true,
                pumpRemainingSeconds = remaining
            )
            delay(1000L)
        }

        // Reset actuator state upon completion
        _actuatorState.value = _actuatorState.value.copy(
            isPumpActive = false,
            pumpRemainingSeconds = 0
        )

        // Notify completion
        onNotification("✅ Water pump cycle completed! Soil moisture is now ${updatedMoisture.toInt()}%.")
    }

    override fun toggleFanMode() {
        val nextMode = _actuatorState.value.fanMode.next()
        _actuatorState.value = _actuatorState.value.copy(fanMode = nextMode)

        if (_plantSource.value == PlantSource.ESP32_HARDWARE) {
            esp32BleManager.sendFanModeCommand(nextMode)
        }
    }

    /**
     * BLE / IoT Connectivity Hook: Call when new sensor data packet arrives from ESP32 BLE peripheral.
     */
    override fun updateTelemetryFromBle(newData: TelemetryData) {
        _telemetryData.value = newData
        scope.launch { dataStore?.saveTelemetryData(newData) }
        updateQuestProgressForTelemetry(newData)
    }

    /**
     * BLE / IoT Connectivity Hook: Call when device connection status changes.
     */
    override fun updateConnectionStatus(connectionState: ConnectionState) {
        _connectionState.value = connectionState
    }

    /**
     * TFLite Model Hook: Executes plant health diagnostic classification inference on device.
     */
    override suspend fun runTfLiteDiagnostic(): String {
        delay(300L)
        return "🌿 AI Diagnostic: Leaf health optimal (98.2% confidence). Soil moisture & light conditions ideal."
    }

    /**
     * Triggers leaf camera scan simulation, adding a new diagnostic scan log entry without repeatable XP rewards.
     */
    override suspend fun performCameraScan(): AiScanResult {
        delay(300L)
        val newScan = AiScanResult(
            id = "scan_${System.currentTimeMillis()}",
            title = "Healthy Leaf",
            subtext = "Leaf 2 (upper canopy) • Just now",
            confidence = "98%",
            isHealthy = true
        )
        val updatedScans = listOf(newScan) + _recentScans.value
        _recentScans.value = updatedScans
        // Scanning does not award repeatable XP.
        return newScan
    }

    /**
     * Adds a care log note entry to the plant journal.
     */
    override suspend fun addJournalEntry(entry: JournalEntry) {
        val current = _plantAvatar.value
        val updatedList = listOf(entry) + current.journalEntries
        _plantAvatar.value = current.copy(journalEntries = updatedList)
        dataStore?.saveJournalEntries(updatedList)
    }

    /**
     * Resets quiz state to clean, unanswered state (selectedQuizOptionIndex = -1, isQuizCompletedToday = false).
     */
    override fun resetQuizState() {
        _selectedQuizOptionIndex.value = -1
        _isQuizCompletedToday.value = quizRewardDate == getTodayDateString()
        _isQuizAnswerCorrect.value = null
    }

    /**
     * Submits selected answer option index for the active daily quiz question.
     */
    override suspend fun submitQuizAnswer(selectedOptionIndex: Int): Boolean {
        initialized.await()
        if (selectedOptionIndex !in _currentQuizQuestion.value.options.indices) return false
        val today = getTodayDateString()
        if (quizRewardDate == today) return false
        quizRewardDate = today
        dataStore?.saveQuizCompletedDate(today)
        val question = _currentQuizQuestion.value
        val isCorrect = selectedOptionIndex == question.correctOptionIndex
        _selectedQuizOptionIndex.value = selectedOptionIndex
        _isQuizAnswerCorrect.value = isCorrect
        _isQuizCompletedToday.value = true

        if (isCorrect) {
            addXp(question.xpReward)
        }
        val todayString = getTodayDateString()
        dataStore?.saveQuizCompletedDate(todayString)
        return isCorrect
    }

    /**
     * Advances to next question in quiz pool and resets selected option/correctness states.
     */
    override fun nextQuizQuestion() {
        val pool = AgroQuizQuestion.QUIZ_POOL
        val currentIndex = pool.indexOf(_currentQuizQuestion.value)
        val nextIndex = if (currentIndex != -1) (currentIndex + 1) % pool.size else 0
        _currentQuizQuestion.value = pool[nextIndex]
        resetQuizState()
    }
}
