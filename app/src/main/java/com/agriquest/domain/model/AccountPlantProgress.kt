package com.agriquest.domain.model

/** Snapshot mapped only to the existing profiles/plants/quest_progress columns. */
data class AccountPlantProgress(
    val plantId: String,
    val plantName: String = "Flora",
    val sowingDate: String,
    val totalXp: Int = 0,
    val level: Int = 0,
    val stage: PlantGrowthStage = PlantGrowthStage.GERMINATION,
    val streakDays: Int = 0,
    val lastResetDate: String? = null,
    val quizCompletedDate: String? = null,
    val xpDate: String,
    val dailyXp: Int = 0,
    val claimedQuestIds: Set<String> = emptySet(),
    val journal: List<JournalEntry> = emptyList()
)

/** Future firmware input. Soil/environment readings alone cannot determine a plant's age. */
data class DetectedPlantIdentity(
    val accountId: String,
    val plantId: String,
    val deviceId: String,
    val sowingDate: String? = null,
    val estimatedStage: PlantGrowthStage? = null
)
