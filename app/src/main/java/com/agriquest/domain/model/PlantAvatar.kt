package com.agriquest.domain.model

/**
 * Domain model representing the gamified Plant Avatar.
 *
 * @param name Name of the plant avatar
 * @param title Variety/display title ("Roma Rosso 🍅")
 * @param level Current level display title ("Lvl. 0 Urban Beginner")
 * @param levelInt Current integer level (0, 1, 2, 3...)
 * @param currentXp Current XP points earned (e.g. 0)
 * @param maxXp XP required for next level (e.g. 100)
 * @param growthStage Current plant growth stage
 * @param statusTag Health/status label ("Vibrant & Healthy")
 * @param streakDays Current streak in days (e.g. 0)
 * @param targetStreakDays Target streak duration (e.g. 7)
 * @param speechBubble Speech bubble message from avatar
 * @param dayCount Days since planting (e.g. 47)
 * @param healthPercent Plant health percentage (e.g. 92)
 * @param healthSubtext Detailed health status subtext
 * @param stageTag Growth stage badge text ("Stage 3 - Mature")
 * @param journalEntries Care log notes list
 * @param moistureHistory Soil moisture daily history entries
 */
data class PlantAvatar(
    val name: String = "Flora",
    val title: String = "Roma Rosso 🍅",
    val level: String = "Lvl. 0 Urban Beginner",
    val levelInt: Int = 0,
    val currentXp: Int = 0,
    val maxXp: Int = 100,
    val growthStage: PlantGrowthStage = PlantGrowthStage.SEEDLING_SPROUT,
    val statusTag: String = "Vibrant & Healthy",
    val streakDays: Int = 0,
    val targetStreakDays: Int = 7,
    val speechBubble: String = "Welcome! I'm a tiny sprout getting started. Keep soil hydrated!",
    val dayCount: Int = 47,
    val healthPercent: Int = 92,
    val healthSubtext: String = "Excellent condition • 3 tomatoes ripening",
    val stageTag: String = "Stage 3 - Mature",
    val journalEntries: List<JournalEntry> = DEFAULT_JOURNAL_ENTRIES,
    val moistureHistory: List<MoistureHistoryEntry> = DEFAULT_MOISTURE_HISTORY
) {
    companion object {
        val DEFAULT_JOURNAL_ENTRIES = listOf(
            JournalEntry("1", "Aug 12", "Watered 250ml", JournalCategory.WATERING),
            JournalEntry("2", "Aug 10", "Pruned bottom branches", JournalCategory.PRUNING),
            JournalEntry("3", "Aug 5", "Planted seedling", JournalCategory.PLANTING)
        )

        val DEFAULT_MOISTURE_HISTORY = listOf(
            MoistureHistoryEntry("4 days ago", 60.0f),
            MoistureHistoryEntry("3 days ago", 62.0f),
            MoistureHistoryEntry("2 days ago", 70.0f),
            MoistureHistoryEntry("Yesterday", 64.0f),
            MoistureHistoryEntry("Today", 68.0f)
        )
    }

    /** Helper property formatting XP display string (e.g. "0/100 XP"). */
    val xpText: String
        get() = "$currentXp/$maxXp XP"

    /** Helper property formatting streak ratio (e.g. "$streakDays/$targetStreakDays"). */
    val streakText: String
        get() = "$streakDays/$targetStreakDays"

    /** Calculated XP progress ratio between 0.0f and 1.0f. */
    val xpProgress: Float
        get() = if (maxXp > 0) (currentXp.toFloat() / maxXp.toFloat()).coerceIn(0f, 1f) else 0f
}
