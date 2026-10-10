package com.agriquest.ui.state

import com.agriquest.domain.model.Achievement
import com.agriquest.domain.model.ActuatorState
import com.agriquest.domain.model.AgroQuizQuestion
import com.agriquest.domain.model.AiScanResult
import com.agriquest.domain.model.ConnectionState
import com.agriquest.domain.model.JournalEntry
import com.agriquest.domain.model.MoistureHistoryEntry
import com.agriquest.domain.model.PlantAvatar
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.domain.model.PlantMoodle
import com.agriquest.domain.model.PlantSource
import com.agriquest.domain.model.Quest
import com.agriquest.domain.model.TelemetryData

/**
 * Unified UI State holding all domain models for AgriQuest Compose screens.
 */
data class AgriQuestUiState(
    val telemetry: TelemetryData = TelemetryData(),
    val plant: PlantAvatar = PlantAvatar(),
    val activeQuest: Quest = Quest(),
    val quests: List<Quest> = Quest.DEFAULT_QUESTS,
    val achievements: List<Achievement> = Achievement.DEFAULT_ACHIEVEMENTS,
    val plantSource: PlantSource = PlantSource.EMULATED_OFFLINE,
    val connection: ConnectionState = ConnectionState(),
    val actuator: ActuatorState = ActuatorState(),
    val recentScans: List<AiScanResult> = AiScanResult.DEFAULT_SCANS,
    val currentQuizQuestion: AgroQuizQuestion = AgroQuizQuestion.QUIZ_POOL.first(),
    val isQuizCompletedToday: Boolean = false,
    val selectedQuizOptionIndex: Int = -1,
    val isQuizAnswerCorrect: Boolean? = null,
    val diagnosticResult: String? = null,
    val isLoading: Boolean = false
) {
    /** Number of unlocked achievements. */
    val unlockedAchievementsCount: Int
        get() = achievements.count { it.isUnlocked }

    /** Total number of achievements. */
    val totalAchievementsCount: Int
        get() = achievements.size

    /** Total XP from unlocked achievements. */
    val unlockedXpTotal: Int
        get() = achievements.filter { it.isUnlocked }.sumOf { it.xpReward }

    /** Exposes plant health percentage (e.g. 92%). */
    val plantHealthPercent: Int
        get() = plant.healthPercent

    /** Exposes plant day count (e.g. Day 47). */
    val plantDayCount: Int
        get() = plant.dayCount

    /** Exposes care streak days count (e.g. 0). */
    val streakDays: Int
        get() = plant.streakDays

    /** Exposes target care streak days count (e.g. 7). */
    val targetStreakDays: Int
        get() = plant.targetStreakDays

    /** Exposes care log journal entries list. */
    val journalEntries: List<JournalEntry>
        get() = plant.journalEntries

    /** Exposes soil moisture daily history entries list. */
    val moistureHistory: List<MoistureHistoryEntry>
        get() = plant.moistureHistory

    /**
     * Dynamically computes active 7 plant status moodles based on current telemetry values & plant growth stage.
     */
    val activeMoodles: List<PlantMoodle>
        get() {
            val list = mutableListOf<PlantMoodle>()
            if (telemetry.soilMoisture < 15.0f) {
                list.add(PlantMoodle.WILTING)
            } else if (telemetry.soilMoisture < 30.0f) {
                list.add(PlantMoodle.THIRSTY)
            }

            if (telemetry.soilMoisture > 85.0f) {
                list.add(PlantMoodle.OVER_WATERED)
            }

            if (telemetry.airTemperature < 15.0f) {
                list.add(PlantMoodle.TOO_COLD)
            } else if (telemetry.airTemperature > 30.0f) {
                list.add(PlantMoodle.HEAT_STRESSED)
            }

            if (telemetry.soilMoisture in 60.0f..80.0f && telemetry.airTemperature in 22.0f..28.0f) {
                list.add(PlantMoodle.THRIVING)
            }

            if (plant.growthStage == PlantGrowthStage.FRUITING_HARVEST || plant.levelInt >= 3) {
                list.add(PlantMoodle.READY_TO_HARVEST)
            }

            return if (list.isEmpty()) listOf(PlantMoodle.THRIVING) else list
        }
}
