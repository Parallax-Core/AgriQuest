package com.agriquest.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.agriquest.data.repository.AgriQuestRepositoryImpl
import com.agriquest.domain.model.Achievement
import com.agriquest.domain.model.ActuatorState
import com.agriquest.domain.model.AgroQuizQuestion
import com.agriquest.domain.model.AiScanResult
import com.agriquest.domain.model.ConnectionState
import com.agriquest.domain.model.ConnectionStatus
import com.agriquest.domain.model.JournalCategory
import com.agriquest.domain.model.JournalEntry
import com.agriquest.domain.model.PlantAvatar
import com.agriquest.domain.model.PlantSource
import com.agriquest.domain.model.Quest
import com.agriquest.domain.model.TelemetryData
import com.agriquest.domain.repository.AgriQuestRepository
import com.agriquest.ui.state.AgriQuestUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing state management, user actions, leveling progression, and notification side effects.
 */
class AgriQuestViewModel(
    private val repository: AgriQuestRepository = AgriQuestRepositoryImpl()
) : ViewModel() {

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val repository: AgriQuestRepository
    ) : ViewModelProvider.Factory {

        constructor(context: Context) : this(AgriQuestRepositoryImpl.getInstance(context.applicationContext))

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AgriQuestViewModel::class.java)) {
                return AgriQuestViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return create(modelClass)
        }
    }

    // Notification / Toast UI event stream
    private val _notificationEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val notificationEvents: SharedFlow<String> = _notificationEvents.asSharedFlow()

    // Combined UI state flow for Jetpack Compose UI
    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<AgriQuestUiState> = combine(
        repository.telemetryData,
        repository.plantAvatar,
        repository.activeQuest,
        repository.quests,
        repository.achievements,
        repository.plantSource,
        repository.connectionState,
        repository.actuatorState,
        repository.recentScans,
        repository.currentQuizQuestion,
        repository.isQuizCompletedToday,
        repository.selectedQuizOptionIndex,
        repository.isQuizAnswerCorrect
    ) { flows: Array<Any?> ->
        AgriQuestUiState(
            telemetry = flows[0] as TelemetryData,
            plant = flows[1] as PlantAvatar,
            activeQuest = flows[2] as Quest,
            quests = flows[3] as List<Quest>,
            achievements = flows[4] as List<Achievement>,
            plantSource = flows[5] as PlantSource,
            connection = flows[6] as ConnectionState,
            actuator = flows[7] as ActuatorState,
            recentScans = flows[8] as List<AiScanResult>,
            currentQuizQuestion = flows[9] as AgroQuizQuestion,
            isQuizCompletedToday = flows[10] as Boolean,
            selectedQuizOptionIndex = flows[11] as Int,
            isQuizAnswerCorrect = flows[12] as Boolean?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AgriQuestUiState()
    )

    init {
        repository.resetQuizState()
        checkAndResetMidnightQuests()
        repository.resetQuizState()
    }

    /**
     * Action to check calendar date and reset midnight quests if a new calendar day has arrived.
     */
    fun checkAndResetMidnightQuests() {
        viewModelScope.launch {
            val wasReset = repository.checkAndResetMidnightQuests()
            if (wasReset) {
                _notificationEvents.emit("🌅 New Day! Daily quests updated for today!")
            }
        }
    }

    /**
     * Action to add XP points directly to user progression.
     */
    fun addXp(amount: Int) {
        viewModelScope.launch {
            repository.addXp(amount)
            _notificationEvents.emit("⭐ +$amount XP earned!")
        }
    }

    /**
     * Action to claim quest reward by ID.
     */
    fun claimQuest(questId: String) {
        viewModelScope.launch {
            val quest = uiState.value.quests.find { it.id == questId }
            if (quest != null && !quest.isClaimed) {
                val allCompleted = repository.claimQuest(questId)
                val updatedPlant = repository.plantAvatar.value
                val newStreak = updatedPlant.streakDays
                if (allCompleted) {
                    _notificationEvents.emit("🎉 All Daily Quests Completed! Care Streak: $newStreak / 7 Days 🔥")
                } else {
                    _notificationEvents.emit("🎉 Quest Completed! +${quest.xpReward} XP awarded!")
                }
            }
        }
    }

    /**
     * Action to simulate next day, shuffling and resetting 3 random daily quests from quest pool.
     */
    fun simulateNextDay() {
        viewModelScope.launch {
            repository.resetDailyQuests()
            _notificationEvents.emit("🌅 Next Day Simulated! 3 New Daily Quests Available!")
        }
    }

    /**
     * Action to select plant telemetry source (Offline Emulated vs ESP32 Hardware).
     */
    fun setPlantSource(source: PlantSource) {
        viewModelScope.launch {
            repository.setPlantSource(source)
            _notificationEvents.emit("🌱 Telemetry Source switched to: ${source.displayName}")
        }
    }

    /**
     * Action to increment user care streak by 1 day.
     */
    fun incrementStreak() {
        viewModelScope.launch {
            repository.incrementStreak()
            val updatedPlant = repository.plantAvatar.value
            val newStreak = updatedPlant.streakDays
            val targetStreak = updatedPlant.targetStreakDays
            _notificationEvents.emit("🔥 Care Streak Progressed! $newStreak / $targetStreak Days")
        }
    }

    /**
     * Action to reset user care streak days to 0.
     */
    fun resetStreak() {
        viewModelScope.launch {
            repository.resetStreak()
            _notificationEvents.emit("🔄 Care streak reset to 0 days.")
        }
    }

    /**
     * Action to trigger water pump for specified duration (default 3s).
     */
    fun triggerPump(durationSeconds: Int = 3) {
        viewModelScope.launch {
            repository.triggerPump(durationSeconds) { message ->
                viewModelScope.launch {
                    _notificationEvents.emit(message)
                }
            }
        }
    }

    /**
     * Action to toggle fan mode (AUTO -> ON -> OFF -> AUTO).
     */
    fun toggleFanMode() {
        repository.toggleFanMode()
    }

    /**
     * Action to update telemetry sensor state (from BLE or simulator).
     */
    fun updateTelemetry(newData: TelemetryData) {
        repository.updateTelemetryFromBle(newData)
    }

    /**
     * Action to trigger TFLite AI diagnostic inference.
     */
    fun runAiDiagnostic() {
        viewModelScope.launch {
            val result = repository.runTfLiteDiagnostic()
            _notificationEvents.emit(result)
        }
    }

    /**
     * Action to perform camera leaf scan simulation, logging scan result and awarding +25 XP.
     */
    fun performCameraScan() {
        viewModelScope.launch {
            val scanResult = repository.performCameraScan()
            _notificationEvents.emit("📸 AI Leaf Scan Complete: ${scanResult.title} (${scanResult.confidence}) (+25 XP awarded!)")
        }
    }

    /**
     * Helper to emit in-app toast / snackbar notification events.
     */
    fun showNotification(message: String) {
        viewModelScope.launch {
            _notificationEvents.emit(message)
        }
    }

    /**
     * Action to initiate BLE scan for ESP32 Hardware.
     */
    fun startEsp32BleScan() {
        repository.startEsp32BleScan()
    }

    /**
     * Action to add a new care log entry to plant journal.
     */
    fun addJournalEntry(note: String, category: JournalCategory = JournalCategory.WATERING) {
        viewModelScope.launch {
            val entry = JournalEntry(
                id = System.currentTimeMillis().toString(),
                date = "Today",
                note = note,
                category = category
            )
            repository.addJournalEntry(entry)
            _notificationEvents.emit("📖 Journal Note Added: \"$note\"")
        }
    }

    /**
     * Action to update hardware connection state.
     */
    fun setConnectionStatus(status: ConnectionStatus, deviceName: String = "ESP32") {
        repository.updateConnectionStatus(
            ConnectionState(
                deviceName = deviceName,
                status = status,
                statusText = when (status) {
                    ConnectionStatus.CONNECTED -> "Connected"
                    ConnectionStatus.CONNECTING -> "Connecting..."
                    ConnectionStatus.SCANNING -> "Scanning..."
                    ConnectionStatus.AWAITING_DEVICE -> "Awaiting Device"
                    ConnectionStatus.DISCONNECTED -> "Disconnected"
                }
            )
        )
    }

    /**
     * Action to submit daily quiz answer option index.
     * If selected option is correct: awards +30 XP, checks for level up, marks today's quiz completed in DataStore,
     * emits celebration toast "🎉 Correct! +30 XP Awarded!".
     * Returns answer result so UI displays correct/incorrect answer feedback and explanation.
     */
    suspend fun submitQuizAnswer(selectedOptionIndex: Int): Boolean {
        val isCorrect = repository.submitQuizAnswer(selectedOptionIndex)
        if (isCorrect) {
            _notificationEvents.emit("🎉 Correct! +30 XP Awarded!")
        }
        return isCorrect
    }

    /**
     * Action to advance to next question in quiz pool / shuffle quiz for testing.
     */
    fun nextQuizQuestion() {
        repository.nextQuizQuestion()
    }
}
