package com.agriquest.domain.repository

import android.net.Uri
import com.agriquest.domain.model.Achievement
import com.agriquest.domain.model.ActuatorState
import com.agriquest.domain.model.AgroQuizQuestion
import com.agriquest.domain.model.AiScanResult
import com.agriquest.domain.model.ConnectionState
import com.agriquest.domain.model.JournalEntry
import com.agriquest.domain.model.PlantAvatar
import com.agriquest.domain.model.PlantSource
import com.agriquest.domain.model.Quest
import com.agriquest.domain.model.TelemetryData
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean Architecture Repository interface for AgriQuest IoT, domain state, leveling, and hardware control.
 */
interface AgriQuestRepository {
    /** Flow of real-time IoT Telemetry sensor readings. */
    val telemetryData: StateFlow<TelemetryData>

    /** Flow of Plant Avatar state and gamification stats. */
    val plantAvatar: StateFlow<PlantAvatar>

    /** Flow of active user quest. */
    val activeQuest: StateFlow<Quest>

    /** Flow of all user quests. */
    val quests: StateFlow<List<Quest>>

    /** Flow of user achievements / awards. */
    val achievements: StateFlow<List<Achievement>>

    /** Flow of selected plant telemetry source (Offline Emulated vs ESP32 Hardware). */
    val plantSource: StateFlow<PlantSource>

    /** Flow of BLE / IoT device connection status. */
    val connectionState: StateFlow<ConnectionState>

    /** Flow of actuator states (Water pump & Fan). */
    val actuatorState: StateFlow<ActuatorState>

    /** Flow of AI leaf diagnostic scan history entries. */
    val recentScans: StateFlow<List<AiScanResult>>

    /** Flow of active daily quiz question. */
    val currentQuizQuestion: StateFlow<AgroQuizQuestion>

    /** Flow indicating if today's quiz has been completed. */
    val isQuizCompletedToday: StateFlow<Boolean>

    /** Flow of currently selected option index for active quiz (-1 if uncompleted). */
    val selectedQuizOptionIndex: StateFlow<Int>

    /** Flow of result correctness for selected quiz option (or null if uncompleted). */
    val isQuizAnswerCorrect: StateFlow<Boolean?>

    /**
     * Increases care XP by up to [amount], subject to the persisted daily cap,
     * while estimated biological growth follows elapsed days since sowing.
     */
    suspend fun setSowingDate(date: String): Boolean

    suspend fun addXp(amount: Int)

    /**
     * Claims XP reward for quest matching [questId] if completed and not yet claimed.
     * @return true if claiming this quest completed ALL daily quests for the day, false otherwise.
     */
    suspend fun claimQuest(questId: String): Boolean

    /**
     * Restores the three daily observation missions and resets completion status,
     * and updates the daily quests list.
     * @return the three available daily quests.
     */
    suspend fun resetDailyQuests(): List<Quest>

    /**
     * Checks if calendar date has changed since last quest reset.
     * If today is a new calendar day (or lastResetDate is null):
     * - Resets and reshuffles uncompleted daily quests.
     * - Saves today's date in DataStore.
     * - If user missed a day (daysBetween > 1), resets care streak to 0.
     * @return true if daily quests were reset for a new day, false if already reset today.
     */
    suspend fun checkAndResetMidnightQuests(): Boolean

    /**
     * Updates selected plant telemetry source and persists change.
     */
    suspend fun setPlantSource(source: PlantSource)

    /**
     * Updates streak days and persists change.
     */
    suspend fun setStreakDays(streakDays: Int)

    /**
     * Increments care streak by 1 day and persists change.
     */
    suspend fun incrementStreak()

    /**
     * Resets care streak days to 0 and persists change.
     */
    suspend fun resetStreak()

    /**
     * Triggers the water pump for specified [durationSeconds] (default 3 seconds).
     *
     * - Updates remaining seconds countdown (3 -> 2 -> 1 -> 0).
     * - Increments soil moisture percentage upon pumping.
     * - Marks 'First Water' quest as completed if active.
     * - Invokes [onNotification] callback with user message/toast.
     */
    suspend fun triggerPump(
        durationSeconds: Int = 3,
        onNotification: (String) -> Unit = {}
    )

    /**
     * Toggles fan mode, cycling through AUTO -> ON -> OFF -> AUTO.
     */
    fun toggleFanMode()

    /**
     * Triggers BLE scan for nearby ESP32 hardware when in [PlantSource.ESP32_HARDWARE] mode.
     */
    fun startEsp32BleScan()

    /**
     * Connectivity hook placeholder for updating telemetry from BLE or IoT sensor streams.
     *
     * @param newData Updated telemetry readings received from hardware sensor payload.
     */
    fun updateTelemetryFromBle(newData: TelemetryData)

    /**
     * Connectivity hook placeholder for device connection state changes.
     */
    fun updateConnectionStatus(connectionState: ConnectionState)

    /**
     * TFLite Model integration placeholder: Analyzes plant health / leaf imagery or telemetry patterns.
     *
     * @return Diagnostic summary text or result tag.
     */
    suspend fun runTfLiteDiagnostic(): String

    /**
     * Sends the leaf photo at [photo] (camera or gallery) to the AI Leaf Detector server,
     * adds the diagnosis to Recent Scans and returns it. Scans give no XP.
     * @throws java.io.IOException if the photo can't be read or the server can't be reached.
     */
    suspend fun performCameraScan(photo: Uri): AiScanResult

    /**
     * Wakes the AI Leaf Detector server (Render sleeps after 15 idle minutes).
     * @return true once the server answers.
     */
    suspend fun wakeAiServer(): Boolean

    /**
     * Adds a care log note entry to the plant journal.
     */
    suspend fun addJournalEntry(entry: JournalEntry)

    /**
     * Resets quiz state to clean, unanswered state (selectedQuizOptionIndex = -1, isQuizCompletedToday = false).
     */
    fun resetQuizState()

    /**
     * Submits selected answer option index for the active daily quiz question.
     * If correct: awards up to +10 XP, checks for level up, marks today's quiz completed, and returns true.
     * @return true if answer is correct, false otherwise.
     */
    suspend fun submitQuizAnswer(selectedOptionIndex: Int): Boolean

    /**
     * Advances to next question in quiz pool and resets selected option/correctness states.
     */
    fun nextQuizQuestion()
}
