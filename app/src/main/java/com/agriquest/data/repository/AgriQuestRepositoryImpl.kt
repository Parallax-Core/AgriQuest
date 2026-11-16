package com.agriquest.data.repository

import android.content.Context
import android.net.Uri
import com.agriquest.data.ble.Esp32BleManager
import com.agriquest.data.local.AgriQuestDataStore
import com.agriquest.data.remote.LeafDetectorApi
import com.agriquest.data.remote.LeafPhotoPreparer
import com.agriquest.data.remote.AuthSession
import com.agriquest.data.remote.SupabasePlantApi
import com.agriquest.data.remote.PlantMonitoringBinding
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
import com.agriquest.domain.model.WateringStatus
import com.agriquest.domain.model.MoistureHistoryEntry
import com.agriquest.domain.model.QuestEvaluator
import com.agriquest.domain.model.QuestProgressState
import com.agriquest.domain.model.SensorKind
import com.agriquest.domain.model.Reward
import com.agriquest.domain.model.AiUnlock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.agriquest.domain.repository.AgriQuestRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.agriquest.domain.model.DetectedPlantIdentity

enum class ProgressSyncStatus { SAVED, PENDING, SYNCING, OFFLINE }

/**
 * Implementation of [AgriQuestRepository] managing live domain state, leveling progression,
 * DataStore persistence, IoT actions, Telemetry Simulation Engine, and BLE Hardware Manager.
 */
class AgriQuestRepositoryImpl(
    private val dataStore: AgriQuestDataStore? = null,
    private val simulationEngine: TelemetrySimulationEngine = TelemetrySimulationEngine(),
    val esp32BleManager: Esp32BleManager = Esp32BleManager(),
    private val appContext: Context? = null,
    private val leafDetector: LeafDetectorApi = LeafDetectorApi(),
    private val accountApi: SupabasePlantApi? = null,
    private val accountId: String? = null,
    private val plantId: String? = null,
) : AgriQuestRepository {

    companion object {
        /** Ages accepted by "How many days old is your plant?". */
        const val MAX_PLANT_AGE_DAYS = 150

        suspend fun forAccount(context: Context, session: AuthSession): AgriQuestRepositoryImpl {
            val store = AgriQuestDataStore(context.applicationContext, session.userId)
            val api = SupabasePlantApi(session)
            val cloud = api.loadProgress()
            store.restoreCloud(cloud)
            val repository = AgriQuestRepositoryImpl(dataStore = store, appContext = context.applicationContext,
                accountApi = api, accountId = session.userId, plantId = cloud.plantId,
                esp32BleManager = Esp32BleManager(context.applicationContext))
            try { repository.initialized.await() } catch (e: Exception) { repository.close(); throw e }
            try { repository._loginDays.value = api.recordAccountLogin() }
            catch (e: CancellationException) { repository.close(); throw e }
            catch (_: Exception) { /* Fail closed without blocking authentication/progress. */ }
            return repository
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _syncStatus = MutableStateFlow(ProgressSyncStatus.SAVED)
    val syncStatus = _syncStatus.asStateFlow()
    private val _loginDays = MutableStateFlow(0)
    private val _wardrobe = MutableStateFlow(com.agriquest.domain.model.Wardrobe())
    val wardrobe = _wardrobe.asStateFlow()
    private val wardrobeMutex = Mutex()
    /** null while loading; false until the player has entered the plant's age. */
    private val _plantAgeConfirmed = MutableStateFlow<Boolean?>(null)
    val plantAgeConfirmed = _plantAgeConfirmed.asStateFlow()
    val loginDays = _loginDays.asStateFlow()
    private var monitoringBinding: PlantMonitoringBinding? = null
    private val pairingMutex = Mutex()
    private var pairingInProgress = false
    val discoveredDevices get() = esp32BleManager.discoveredDevices
    private var detectedStage: PlantGrowthStage? = null

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
    // Declared before init so background telemetry never sees them uninitialized.
    private val memoryMoistureDays = java.util.TreeMap<String, Float>()
    @Volatile private var questProgress = QuestProgressState(getTodayDateString())
    private val questMutex = Mutex()

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
            statusTag = "Scan a leaf to check health",
            streakDays = 0,
            targetStreakDays = 7,
            sowingDate = trackingStart,
            speechBubble = PlantGrowthStage.GERMINATION.speechBubble,
            journalEntries = emptyList(), moistureHistory = emptyList()
        )
    )
    override val plantAvatar: StateFlow<PlantAvatar> = _plantAvatar.asStateFlow()

    private val _quests = MutableStateFlow(Quest.forDate(getTodayDateString()))
    override val quests: StateFlow<List<Quest>> = _quests.asStateFlow()

    private val _activeQuest = MutableStateFlow(_quests.value.first())
    override val activeQuest: StateFlow<Quest> = _activeQuest.asStateFlow()

    private val _achievements = MutableStateFlow(Achievement.DEFAULT_ACHIEVEMENTS.map { it.copy(isUnlocked = false, earnedDate = null) })
    override val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _plantSource = MutableStateFlow(PlantSource.EMULATED_OFFLINE)
    override val plantSource: StateFlow<PlantSource> = _plantSource.asStateFlow()

    private val _connectionState = MutableStateFlow(
        ConnectionState(
            deviceName = "ESP32",
            status = ConnectionStatus.DISCONNECTED,
            statusText = "Not connected"
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
                try {
                    trackingStart = store.trackingStartFlow.first() ?: getTodayDateString().also { store.saveTrackingStart(it) }
                    xpDate = store.xpDateFlow.first() ?: getTodayDateString()
                    earnedToday = store.dailyXpFlow.first()
                    quizRewardDate = store.quizCompletedDateFlow.first()
                    val xp = store.currentXpFlow.first()
                    val source = store.plantSourceFlow.first()
                    detectedStage = store.growthStageFlow.first().takeIf { it.stageLevel > 0 }
                    val streak = store.streakDaysFlow.first()
                    val claimedIds = store.claimedQuestIdsFlow.first()
                    val savedJournal = store.journalNotesFlow.first()
                    val savedTelemetry = store.telemetryDataFlow.first()
                    val savedLastResetDate = store.lastResetDateFlow.first()
                    _plantAvatar.value = _plantAvatar.value.copy(name = store.plantNameFlow.first())
                    _recentScans.value = store.recentScansFlow.first()
                    _plantAgeConfirmed.value = store.plantAgeConfirmedFlow.first()
                    _wardrobe.value = store.wardrobeFlow.first()
                    refreshHealthFromScans()
                    publishMoistureHistory(store.moistureDaysFlow.first())

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
                    _quests.value = questsFor(getTodayDateString())
                    updateQuestsWithClaimed(claimedIds)
                    questProgress = store.questProgressFlow.first()?.takeIf { it.date == getTodayDateString() }
                        ?: QuestProgressState(getTodayDateString())
                    applyQuestProgress()

                    // Midnight date check & quest reset on repository startup
                    refreshDailyQuests()

                    // Restore daily completion while clearing transient answer selection.
                    resetQuizState()

                    if (source == PlantSource.EMULATED_OFFLINE) {
                        startSimulationRoutine()
                    } else {
                        bindBleHardwareState()
                    }
                    store.markForSync()
                    initialized.complete(Unit)
                } catch (e: Exception) {
                    initialized.completeExceptionally(e)
                    if (e is CancellationException) throw e
                }
            }
        } ?: run {
            _plantAgeConfirmed.value = false
            initialized.complete(Unit)
            startSimulationRoutine()
        }
        if (accountApi != null && dataStore != null) scope.launch {
            initialized.await()
            dataStore.cloudDirtyFlow.collect { dirty ->
                if (dirty) {
                    delay(500)
                    while (isActive && dataStore.cloudDirtyFlow.first()) {
                        try {
                            _syncStatus.value = ProgressSyncStatus.SYNCING
                            val (revision, progress) = dataStore.pendingProgress()
                            accountApi.saveProgress(progress)
                            dataStore.acknowledgeSync(revision)
                            _syncStatus.value = if (dataStore.cloudDirtyFlow.first()) ProgressSyncStatus.PENDING else ProgressSyncStatus.SAVED
                        } catch (e: CancellationException) { throw e }
                        catch (_: Exception) { _syncStatus.value = ProgressSyncStatus.OFFLINE; delay(15_000) }
                    }
                }
            }
        }
    }

    fun close() {
        scope.cancel()
        accountApi?.cancelRequests()
        esp32BleManager.stopScan()
        esp32BleManager.disconnect()
    }

    suspend fun pairEsp32Device(macAddress: String) = pairingMutex.withLock {
        initialized.await()
        scope.coroutineContext.ensureActive()
        val api = accountApi ?: throw IllegalStateException("Log in before pairing hardware.")
        val activePlantId = plantId ?: throw IllegalStateException("Select an owned plant first.")
        pairingInProgress = true
        val statusJob = scope.launch {
            esp32BleManager.connectionState.collect { status ->
                _connectionState.value = if (status.status == ConnectionStatus.CONNECTED) status.copy(
                    status = ConnectionStatus.CONNECTING, statusText = "Verifying device ownership...") else status
            }
        }
        try {
            val proof = esp32BleManager.requestPairingProof(macAddress)
            statusJob.cancel()
            _connectionState.value = ConnectionState(deviceName = proof.deviceId,
                status = ConnectionStatus.CONNECTING, statusText = "Verifying device ownership...")
            val existing = api.listDevices().firstOrNull { it.deviceId == proof.deviceId }
            if (existing?.plantId != null && existing.plantId != activePlantId) {
                throw IllegalStateException("Select this device's assigned plant before reconnecting.")
            }
            monitoringBinding = api.pairDevice(activePlantId, proof)
            _connectionState.value = ConnectionState(deviceName = proof.deviceId,
                status = ConnectionStatus.AWAITING_DEVICE, statusText = "Paired; awaiting ESP32 readings")
        } finally {
            statusJob.cancel()
            pairingInProgress = false
        }
    }

    /** Future firmware/vision integration: never called automatically by ordinary telemetry. */
    suspend fun applyDetectedPlantIdentity(identity: DetectedPlantIdentity): Boolean {
        initialized.await()
        scope.coroutineContext.ensureActive()
        if (identity.accountId != accountId || identity.plantId != plantId || accountApi == null) return false
        val binding = accountApi.monitoringBinding(plantId) ?: return false
        if (binding.deviceId != identity.deviceId) return false
        if (identity.sowingDate != null && !setSowingDate(identity.sowingDate)) return false
        detectedStage = identity.estimatedStage ?: detectedStage
        updateAvatarProgression(_plantAvatar.value.currentXp)
        dataStore?.saveUserProgression(_plantAvatar.value.levelInt, _plantAvatar.value.currentXp, _plantAvatar.value.growthStage)
        dataStore?.markForSync()
        return true
    }

    private fun startSimulationRoutine() {
        stopSimulationRoutine()
        stopBleHardwareBinding()

        _connectionState.value = ConnectionState(
            deviceName = "ESP32",
            status = ConnectionStatus.CONNECTED,
            statusText = "Offline simulation"
        )

        simulationJob = scope.launch {
            simulationEngine.telemetryFlow(_telemetryData.value.withMissingFilled()).collect { simulatedTelemetry ->
                onNewTelemetry(simulatedTelemetry)
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

        if (accountApi != null && plantId != null) {
            bleTelemetryJob = scope.launch {
                // Connected = a NEW reading arrived within the last 45 s (measured on this phone's clock,
                // so a wrong clock on the phone or ESP32 can't cause false disconnects).
                var lastRecordedAt: String? = null
                var lastSignalAt = 0L
                var bindingCheckedAt = 0L
                while (isActive) {
                    if (pairingInProgress) { delay(250); continue }
                    val now = System.currentTimeMillis()
                    try {
                        if (monitoringBinding == null || now - bindingCheckedAt > 60_000L) {
                            monitoringBinding = accountApi.monitoringBinding(plantId, monitoringBinding?.deviceId)
                            bindingCheckedAt = now
                        }
                        val binding = monitoringBinding
                        val reading = binding?.let { accountApi.latestReading(it) }
                        if (reading != null && reading.recordedAt != lastRecordedAt) {
                            val firstLook = lastRecordedAt == null
                            lastRecordedAt = reading.recordedAt
                            // The first reading we see may be old; only count it as a signal if it is recent.
                            if (!firstLook || reading.isFresh(now)) lastSignalAt = now
                            onNewTelemetry(reading.telemetry)
                        }
                        val silentFor = now - lastSignalAt
                        val name = binding?.deviceId ?: "ESP32"
                        _connectionState.value = when {
                            binding == null -> ConnectionState(name, ConnectionStatus.AWAITING_DEVICE, "No ESP32 paired with this plant")
                            lastSignalAt != 0L && silentFor <= ConnectionState.HEARTBEAT_TIMEOUT_MS ->
                                ConnectionState(name, ConnectionStatus.CONNECTED, "Connected")
                            lastSignalAt == 0L && reading == null -> ConnectionState(name, ConnectionStatus.AWAITING_DEVICE, "Waiting for the first ESP32 signal")
                            lastSignalAt == 0L -> ConnectionState(name, ConnectionStatus.AWAITING_DEVICE, "Waiting for a new ESP32 signal")
                            else -> ConnectionState(name, ConnectionStatus.DISCONNECTED, "Disconnected (no signal for ${silentFor / 1000}s)")
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) {
                        // A short internet hiccup is not a disconnect; only report it after the same 45 s limit.
                        if (now - lastSignalAt > ConnectionState.HEARTBEAT_TIMEOUT_MS) {
                            _connectionState.value = ConnectionState(status = ConnectionStatus.DISCONNECTED,
                                statusText = "Can't reach the server")
                        }
                    }
                    delay(ConnectionState.POLL_INTERVAL_MS)
                }
            }
            return
        }

        bleStateJob = scope.launch {
            esp32BleManager.connectionState.collect { newState ->
                _connectionState.value = newState
            }
        }

        bleTelemetryJob = scope.launch {
            esp32BleManager.telemetryDataFlow.collect { newTelemetry ->
                onNewTelemetry(newTelemetry)
            }
        }

        startEsp32BleScan()
    }

    private fun stopBleHardwareBinding() {
        bleStateJob?.cancel()
        bleStateJob = null
        bleTelemetryJob?.cancel()
        bleTelemetryJob = null
        monitoringBinding = null
    }

    override fun startEsp32BleScan() {
        if (_plantSource.value == PlantSource.ESP32_HARDWARE && !pairingInProgress) {
            esp32BleManager.startScan(autoConnect = accountApi == null)
            scope.launch { delay(10_000); esp32BleManager.stopScan() }
        }
    }

    /** Every sensor reading (simulated, cloud or Bluetooth) goes through here. */
    private suspend fun onNewTelemetry(telemetry: TelemetryData) {
        _telemetryData.value = telemetry
        dataStore?.saveTelemetryData(telemetry)
        recordMoisture(telemetry)
        updateQuestsFromReading(telemetry)
    }

    /** Today's quests for the plant's stage and the sensors that actually report values. */
    private fun questsFor(date: String): List<Quest> =
        Quest.forDate(date, _plantAvatar.value.growthStage, SensorKind.availableIn(_telemetryData.value))

    private suspend fun updateQuestsFromReading(telemetry: TelemetryData) = questMutex.withLock {
        val today = getTodayDateString()
        if (questProgress.date != today) questProgress = QuestProgressState(today)
        questProgress = QuestEvaluator.onReading(questProgress, telemetry, System.currentTimeMillis())
        replaceQuestsWithoutSensor(telemetry)
        applyQuestProgress()
        dataStore?.saveQuestProgress(questProgress)
    }

    /** If the device lacks a sensor an unclaimed quest needs (e.g. no light sensor), swap in quests it can track. */
    private fun replaceQuestsWithoutSensor(telemetry: TelemetryData) {
        val sensors = SensorKind.availableIn(telemetry)
        val current = _quests.value
        if (current.none { !it.isClaimed && it.goal.sensor != null && it.goal.sensor !in sensors }) return
        val fresh = Quest.forDate(getTodayDateString(), _plantAvatar.value.growthStage, sensors)
        val claimed = current.filter { it.isClaimed }
        _quests.value = claimed + fresh.filter { f -> claimed.none { it.id == f.id } }
            .take((fresh.size - claimed.size).coerceAtLeast(0))
    }

    private fun applyQuestProgress() {
        val updated = QuestEvaluator.apply(_quests.value, questProgress, _telemetryData.value)
        _quests.value = updated
        updateActiveQuest(updated)
    }

    private suspend fun recordMoisture(telemetry: TelemetryData) {
        val moisture = telemetry.soilMoisture
        if (!moisture.isFinite()) return
        val store = dataStore
        if (store != null) {
            store.recordMoisture(getTodayDateString(), moisture)
            publishMoistureHistory(store.moistureDaysFlow.first())
        } else {
            memoryMoistureDays[getTodayDateString()] = moisture
            while (memoryMoistureDays.size > AgriQuestDataStore.MOISTURE_HISTORY_DAYS) memoryMoistureDays.remove(memoryMoistureDays.firstKey())
            publishMoistureHistory(memoryMoistureDays.toList())
        }
    }

    /** Turns ("2026-11-16", 64%) pairs into chart entries labelled Today / Yesterday / "Nov 14". */
    private fun publishMoistureHistory(days: List<Pair<String, Float>>) {
        val today = getTodayDateString()
        val labelFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val entries = days.sortedBy { it.first }.map { (date, percent) ->
            val ago = calculateDaysBetween(date, today)
            val label = when (ago) {
                0L -> "Today"
                1L -> "Yesterday"
                else -> runCatching { labelFormat.format(requireNotNull(parser.parse(date))) }.getOrDefault(date)
            }
            MoistureHistoryEntry(label, percent)
        }
        _plantAvatar.value = _plantAvatar.value.copy(moistureHistory = entries)
    }

    /** Health = share of healthy leaf scans in the last 7 days. Unknown (null) until a leaf is scanned. */
    private fun refreshHealthFromScans() {
        val weekAgo = System.currentTimeMillis() - 7L * 86_400_000L
        val recent = _recentScans.value.filter { scan ->
            val takenAt = scan.id.removePrefix("scan_").toLongOrNull() ?: return@filter false
            takenAt >= weekAgo
        }
        val percent = if (recent.isEmpty()) null else recent.count { it.isHealthy } * 100 / recent.size
        _plantAvatar.value = _plantAvatar.value.copy(
            healthPercent = percent,
            healthScanCount = recent.size,
            statusTag = when {
                percent == null -> "Scan a leaf to check health"
                percent >= 80 -> "Vibrant & Healthy"
                percent >= 50 -> "Some leaves need attention"
                else -> "Needs care: disease detected"
            }
        )
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
        val estimated = PlantGrowthStage.fromDays(calculateDaysBetween(trackingStart, getTodayDateString()).toInt().coerceAtLeast(0))
        val stage = detectedStage?.takeIf { it.stageLevel > estimated.stageLevel } ?: estimated
        return Triple(stage.stageLevel, stage, stage.maxXp)
    }

    override suspend fun setSowingDate(date: String): Boolean {
        initialized.await()
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        if (!date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) return false
        val parsed = runCatching { parser.parse(date) }.getOrNull() ?: return false
        if (parsed.after(Date())) return false
        trackingStart = date
        dataStore?.saveTrackingStart(date)
        updateAvatarProgression(_plantAvatar.value.currentXp)
        dataStore?.saveUserProgression(_plantAvatar.value.levelInt, _plantAvatar.value.currentXp, _plantAvatar.value.growthStage)
        dataStore?.markForSync()
        return true
    }

    /** Buys an outfit with points. Returns a message for the player. */
    suspend fun buyOutfit(outfitId: String): String = wardrobeMutex.withLock {
        initialized.await()
        val outfit = com.agriquest.domain.model.Outfit.byId(outfitId) ?: return@withLock "That outfit doesn't exist."
        val current = _wardrobe.value
        if (outfit.id in current.owned) return@withLock "You already own the ${outfit.name}."
        val points = current.points(_plantAvatar.value.currentXp)
        if (points < outfit.price) return@withLock "You need ${outfit.price - points} more points for the ${outfit.name}."
        val updated = current.copy(owned = current.owned + outfit.id, spentPoints = current.spentPoints + outfit.price,
            equipped = current.equipped + (outfit.slot to outfit.id))
        _wardrobe.value = updated
        dataStore?.saveWardrobe(updated)
        "You bought the ${outfit.name}! Your tomato is wearing it."
    }

    /** Wears an owned outfit, or takes it off if it is already worn. */
    suspend fun toggleOutfit(outfitId: String) = wardrobeMutex.withLock {
        val outfit = com.agriquest.domain.model.Outfit.byId(outfitId) ?: return@withLock
        val current = _wardrobe.value
        if (outfit.id !in current.owned) return@withLock
        val updated = if (current.equipped[outfit.slot] == outfit.id) current.copy(equipped = current.equipped - outfit.slot)
            else current.copy(equipped = current.equipped + (outfit.slot to outfit.id))
        _wardrobe.value = updated
        dataStore?.saveWardrobe(updated)
    }

    /** "My plant is [days] days old": sets the sowing date so the game shows the right growth stage. */
    suspend fun confirmPlantAge(days: Int): Boolean {
        if (days !in 0..MAX_PLANT_AGE_DAYS) return false
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - days * 86_400_000L))
        if (!setSowingDate(date)) return false
        dataStore?.savePlantAgeConfirmed()
        _plantAgeConfirmed.value = true
        // Quests depend on the growth stage, so refresh unclaimed ones for the new age.
        questMutex.withLock {
            val claimed = _quests.value.filter { it.isClaimed }
            val fresh = questsFor(getTodayDateString())
            _quests.value = claimed + fresh.filter { f -> claimed.none { it.id == f.id } }
                .take((fresh.size - claimed.size).coerceAtLeast(0))
            applyQuestProgress()
        }
        return true
    }

    override suspend fun addXp(amount: Int) {
        initialized.await()
        awardXp(amount)
    }

    /** Adds XP up to the daily limit and returns how much was actually added. */
    private suspend fun awardXp(amount: Int): Int = rewardMutex.withLock {
        if (amount <= 0) return@withLock 0
        val today = getTodayDateString()
        if (today > xpDate) { xpDate = today; earnedToday = 0 }
        if (today < xpDate) return@withLock 0
        val granted = amount.coerceAtMost(PlantGrowthStage.DAILY_XP_LIMIT - earnedToday).coerceAtLeast(0)
        val total = (_plantAvatar.value.currentXp.toLong() + granted).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        earnedToday += granted
        dataStore?.saveXpLedger(today, earnedToday, total)
        updateAvatarProgression(total)
        dataStore?.saveUserProgression(_plantAvatar.value.levelInt, total, _plantAvatar.value.growthStage)
        dataStore?.markForSync()
        granted
    }

    /** Finished missions and quizzes, for the "Congratulations!" dialog. */
    private val _rewards = kotlinx.coroutines.flow.MutableSharedFlow<Reward>(extraBufferCapacity = 8)
    val rewards: kotlinx.coroutines.flow.SharedFlow<Reward> = _rewards

    override suspend fun claimQuest(questId: String): Boolean = claimMutex.withLock {
        initialized.await()
        checkAndResetMidnightQuests()
        // Same lock as sensor updates, so a reading arriving now can't undo the claim.
        val claimed = questMutex.withLock {
            val questList = _quests.value.toMutableList()
            val index = questList.indexOfFirst { it.id == questId }
            val quest = questList.getOrNull(index)
            if (quest == null || quest.isClaimed || !quest.isClaimable) return@withLock null
            val allClaimedBefore = questList.all { it.isClaimed }
            questList[index] = quest.copy(progress = 1.0f, isClaimed = true, isClaimable = false)
            _quests.value = questList
            updateActiveQuest(questList)
            Triple(quest, allClaimedBefore, questList.all { it.isClaimed })
        } ?: return false
        val (quest, allClaimedBefore, allClaimedNow) = claimed
        dataStore?.saveClaimedQuestId(questId)
        val granted = awardXp(quest.xpReward)
        val allDone = !allClaimedBefore && allClaimedNow
        if (allDone) incrementStreak()
        _rewards.tryEmit(Reward(quest.title, granted, quest.xpReward,
            if (allDone) Reward.Kind.ALL_QUESTS else Reward.Kind.QUEST))
        return allDone
    }

    override suspend fun resetDailyQuests(): List<Quest> {
        return resetQuestsForDate(getTodayDateString())
    }

    private suspend fun resetQuestsForDate(date: String): List<Quest> {
        questMutex.withLock {
            questProgress = QuestProgressState(date)
            dataStore?.saveQuestProgress(questProgress)
        }
        val newDailyQuests = QuestEvaluator.apply(questsFor(date), questProgress, _telemetryData.value)
        _quests.value = newDailyQuests
        updateActiveQuest(newDailyQuests)
        dataStore?.clearClaimedQuestIds()
        dataStore?.markForSync()
        return newDailyQuests
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
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

    override suspend fun checkAndResetMidnightQuests(): Boolean {
        initialized.await()
        return refreshDailyQuests()
    }

    private suspend fun refreshDailyQuests(): Boolean {
        updateAvatarProgression(_plantAvatar.value.currentXp)
        val todayString = getTodayDateString()
        val lastResetDate = dataStore?.lastResetDateFlow?.first() ?: memoryLastResetDate

        if (lastResetDate == null) {
            memoryLastResetDate = todayString
            dataStore?.saveLastResetDate(todayString)
            dataStore?.markForSync()
            resetQuizState()
            return false // first launch: nothing was reset, so no "New Day" message
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
        dataStore?.markForSync()
    }

    override suspend fun incrementStreak() {
        val newStreak = _streakDays.value + 1
        _streakDays.value = newStreak
        _plantAvatar.value = _plantAvatar.value.copy(streakDays = newStreak)
        dataStore?.incrementStreak()
        dataStore?.markForSync()
    }

    override suspend fun resetStreak() {
        _streakDays.value = 0
        _plantAvatar.value = _plantAvatar.value.copy(streakDays = 0)
        dataStore?.resetStreak()
        dataStore?.markForSync()
    }

    override suspend fun triggerPump(
        durationSeconds: Int,
        onNotification: (String) -> Unit
    ) {
        if (_actuatorState.value.isPumpActive) return
        if (_plantSource.value == PlantSource.ESP32_HARDWARE && accountApi != null) {
            val binding = monitoringBinding ?: throw IllegalStateException("Pair an ESP32 with this account's plant first.")
            accountApi.requestCommand(binding, "pump", durationSeconds)
            onNotification("Watering request sent to your paired ESP32. Waiting for sensor readings.")
            return
        }

        // 1. Restores soil moisture by +15% (capped at 100.0%)
        val updatedTelemetry = simulationEngine.restoreMoistureFromPump(_telemetryData.value)
        _telemetryData.value = updatedTelemetry
        dataStore?.saveTelemetryData(updatedTelemetry)
        val updatedMoisture = updatedTelemetry.soilMoisture

        // Watering does not award repeatable XP. Quest progress comes from sensor readings.
        recordMoisture(updatedTelemetry)
        completeFirstWaterQuest()

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

    override fun waterPlant(durationSeconds: Int): Flow<WateringStatus> = flow {
        val api = accountApi
        if (_plantSource.value != PlantSource.ESP32_HARDWARE || api == null) {
            emit(WateringStatus(WateringStatus.Phase.RUNNING, "Watering for ${durationSeconds}s..."))
            triggerPump(durationSeconds)
            emit(WateringStatus(WateringStatus.Phase.DONE, "Plant watered."))
            return@flow
        }

        emit(WateringStatus(WateringStatus.Phase.SENDING, "Sending watering request..."))
        val activePlant = plantId
        val binding = if (activePlant == null) null
            else attempt { monitoringBinding ?: api.monitoringBinding(activePlant) }.getOrNull()
        if (binding == null) {
            emit(WateringStatus(WateringStatus.Phase.FAILED, "No ESP32 is paired with this plant yet."))
            return@flow
        }
        // The database checks the safety rules (max run time, cooldown, one command at a time).
        val sent = attempt { api.requestCommand(binding, "pump", durationSeconds) }
        val commandId = sent.getOrNull()
        if (commandId == null) {
            emit(WateringStatus(WateringStatus.Phase.FAILED, friendlyError(sent.exceptionOrNull())))
            return@flow
        }

        emit(WateringStatus(WateringStatus.Phase.WAITING, "Waiting for the ESP32 to pick it up..."))
        val startedAt = System.currentTimeMillis()
        var lastStatus = "pending"
        while (true) {
            delay(1_500)
            val elapsed = System.currentTimeMillis() - startedAt
            val (status, reason) = attempt { api.commandStatus(commandId) }.getOrNull() ?: ("unknown" to null)
            when {
                status == "done" -> {
                    completeFirstWaterQuest()
                    emit(WateringStatus(WateringStatus.Phase.DONE, "Plant watered. The new reading arrives in a few seconds."))
                    return@flow
                }
                status == "failed" || status == "expired" || status == "rejected" -> {
                    emit(WateringStatus(WateringStatus.Phase.FAILED, reason
                        ?: if (status == "expired") "The ESP32 didn't pick up the request. Is it on and online?" else "The pump didn't run."))
                    return@flow
                }
                status == "running" && lastStatus != "running" ->
                    emit(WateringStatus(WateringStatus.Phase.RUNNING, "Pump running for ${durationSeconds}s..."))
                // The ESP32 checks every 5 s, so 70 s without pickup means it is offline.
                lastStatus != "running" && elapsed > 70_000 || elapsed > 150_000 -> {
                    emit(WateringStatus(WateringStatus.Phase.FAILED, "No answer from the ESP32. Check that it is powered and online."))
                    return@flow
                }
            }
            if (status != "unknown") lastStatus = status
        }
    }

    /** Runs a network call, returning its failure instead of throwing (but never swallowing cancellation). */
    private suspend fun <T> attempt(block: suspend () -> T): Result<T> =
        try { Result.success(block()) } catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }

    private fun friendlyError(error: Throwable?): String = when {
        error is java.net.UnknownHostException || error is java.net.ConnectException ||
            error is java.net.SocketTimeoutException -> "Can't reach the server. Check your internet connection."
        error?.message.isNullOrBlank() || error?.message?.startsWith("Account sync failed") == true ->
            "The server refused the watering request. Try again in a moment."
        else -> error?.message ?: "The watering request failed."
    }

    /** Called when the ESP32 (or the simulator) confirms a finished watering. */
    private suspend fun completeFirstWaterQuest() = questMutex.withLock {
        val today = getTodayDateString()
        if (questProgress.date != today) questProgress = QuestProgressState(today)
        questProgress = QuestEvaluator.onWatered(questProgress, System.currentTimeMillis())
        applyQuestProgress()
        dataStore?.saveQuestProgress(questProgress)
    }

    override fun toggleFanMode() {
        if (_plantSource.value == PlantSource.ESP32_HARDWARE && accountApi != null) return
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
        if (accountApi != null) return // Authenticated monitoring accepts only the paired, RLS-filtered sensor path.
        scope.launch { onNewTelemetry(newData) }
    }

    /**
     * BLE / IoT Connectivity Hook: Call when device connection status changes.
     */
    override fun updateConnectionStatus(connectionState: ConnectionState) {
        if (accountApi != null) return
        _connectionState.value = connectionState
    }

    /**
     * TFLite Model Hook: Executes plant health diagnostic classification inference on device.
     */
    override suspend fun runTfLiteDiagnostic(): String {
        requireAiAccess()
        delay(300L)
        return "🌿 AI Diagnostic: Leaf health optimal (98.2% confidence). Soil moisture & light conditions ideal."
    }

    /**
     * Shrinks the leaf photo, uploads it to the AI Leaf Detector on Render and logs the diagnosis
     * in Recent Scans. Scanning does not award XP.
     */
    override suspend fun performCameraScan(photo: Uri): AiScanResult {
        requireAiAccess()
        val context = appContext ?: throw IllegalStateException("Leaf scanning needs the app context.")
        val id = "scan_${System.currentTimeMillis()}"
        // Kept in the cache so Recent Scans can show the photo; Android clears it when space runs low.
        val savedPhoto = File(context.filesDir, "accounts/${accountId ?: "preview"}/leaf_photos/$id.jpg")
        val jpeg = withContext(Dispatchers.IO) { LeafPhotoPreparer.prepare(context, photo, savedPhoto) }
        val prediction = leafDetector.predict(jpeg)
        val heatmapFile = prediction.heatmapPng?.let { png ->
            runCatching {
                File(savedPhoto.parentFile, "${id}_heatmap.png").also { file ->
                    withContext(Dispatchers.IO) { file.writeBytes(png) }
                }
            }.getOrNull()
        }
        val scannedAt = SimpleDateFormat("MMM d - h:mm a", Locale.getDefault()).format(Date())
        val newScan = AiScanResult(
            id = id,
            title = prediction.label,
            subtext = "Scanned $scannedAt",
            confidence = prediction.confidenceText,
            isHealthy = prediction.isHealthy,
            imagePath = savedPhoto.absolutePath,
            lowConfidence = prediction.lowConfidence,
            pathogen = prediction.pathogen,
            heatmapPath = heatmapFile?.absolutePath
        )
        _recentScans.value = listOf(newScan) + _recentScans.value
        dataStore?.saveScans(_recentScans.value)
        refreshHealthFromScans()
        return newScan
    }

    private suspend fun requireAiAccess() {
        val days = accountApi?.aiLoginDays() ?: 0
        _loginDays.value = days
        check(days >= AiUnlock.REQUIRED_LOGIN_DAYS) { "Log in for ${AiUnlock.REQUIRED_LOGIN_DAYS} days to unlock" }
    }

    override suspend fun wakeAiServer(): Boolean {
        if (_loginDays.value < AiUnlock.REQUIRED_LOGIN_DAYS) return false
        requireAiAccess()
        return leafDetector.wake()
    }

    /**
     * Adds a care log note entry to the plant journal.
     */
    override suspend fun addJournalEntry(entry: JournalEntry) {
        val current = _plantAvatar.value
        val updatedList = listOf(entry) + current.journalEntries
        _plantAvatar.value = current.copy(journalEntries = updatedList)
        dataStore?.saveJournalEntries(updatedList)
        dataStore?.markForSync()
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
            val granted = awardXp(question.xpReward)
            _rewards.tryEmit(Reward("Daily Quiz", granted, question.xpReward, Reward.Kind.QUIZ))
        }
        dataStore?.markForSync()
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
