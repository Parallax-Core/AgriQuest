package com.agriquest.domain.repository

import com.agriquest.domain.model.Achievement
import com.agriquest.domain.model.ActuatorState
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

    /**
     * Increases user current XP by [amount], leveling up when [currentXp] reaches [maxXpForLevel],
     * recalculating max XP, growth stage, avatar level text, and agro speech bubble.
     */
    suspend fun addXp(amount: Int)

    /**
     * Claims XP reward for quest matching [questId] if completed and not yet claimed.
     * @return true if claiming this quest completed ALL daily quests for the day, false otherwise.
     */
    suspend fun claimQuest(questId: String): Boolean

    /**
     * Shuffles 3 random quests from the quest pool, resets their completion status,
     * and updates the daily quests list.
     * @return the newly selected 3 daily quests.
     */
    suspend fun resetDailyQuests(): List<Quest>

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
     * Triggers leaf camera scan simulation, adding a new diagnostic scan log entry and awarding +25 XP.
     */
    suspend fun performCameraScan(): AiScanResult

    /**
     * Adds a care log note entry to the plant journal.
     */
    suspend fun addJournalEntry(entry: JournalEntry)
}
