package com.agriquest.domain.model

/**
 * Domain model representing a gamified quest/mission for plant care.
 *
 * @param id Unique identifier for the quest
 * @param title Title of quest ("Prune Lower Leaves" or "⚡ Thirsty Roots...")
 * @param difficulty Difficulty level ("Easy", "Medium", "Hard")
 * @param progress Quest completion progress ratio (1.0f = 100% complete)
 * @param xpReward XP reward amount on completion (+50 XP)
 * @param subtext Description or subtext details for the quest
 * @param progressText Display text for progress bar (e.g. "2 / 2 leaves", "75% Moisture Goal")
 * @param subtextBadge Subtext badge description
 * @param isClaimed Whether quest reward has been claimed
 * @param isClaimable Whether quest reward can be claimed
 */
data class Quest(
    val id: String = "quest_thirsty_roots",
    val title: String = "⚡ Thirsty Roots: Keep soil moisture above 60%",
    val difficulty: String = "Easy",
    val progress: Float = 1.0f,
    val xpReward: Int = 50,
    val subtext: String = "Keep soil moisture above 60%.",
    val progressText: String = "68% Moisture Goal",
    val subtextBadge: String = "Unlock Progress: 0/7 Days to AI Diagnostic",
    val isClaimed: Boolean = false,
    val isClaimable: Boolean = true
) {
    /** Helper property returning whether the quest is fully completed. */
    val isCompleted: Boolean
        get() = progress >= 1.0f || isClaimed

    /** Helper property formatting XP reward badge (e.g. "+50 XP"). */
    val xpRewardText: String
        get() = "+$xpReward XP"

    /** Progress percentage as integer 0 - 100 %. */
    val progressPercent: Int
        get() = (progress * 100).toInt().coerceIn(0, 100)

    companion object {
        const val ROTATING_DAILY_COUNT = 3

        /** Existing tasks rotate daily; the ten additional tasks are always available. */
        val ROTATING_QUEST_POOL = listOf(
            Quest(
                id = "quest_prune_leaves",
                title = "Prune Lower Leaves",
                difficulty = "Easy",
                progress = 1.0f,
                xpReward = 50,
                subtext = "Remove yellowing bottom leaves to improve airflow and prevent disease.",
                progressText = "2 / 2 leaves",
                isClaimed = false,
                isClaimable = true
            ),
            Quest(
                id = "quest_deep_watering",
                title = "Deep Watering Session",
                difficulty = "Easy",
                progress = 1.0f,
                xpReward = 75,
                subtext = "Water thoroughly until soil moisture reaches 75%. Let the roots drink deeply.",
                progressText = "75% Moisture Goal",
                isClaimed = false,
                isClaimable = true
            ),
            Quest(
                id = "quest_stake_stem",
                title = "Stake Main Stem",
                difficulty = "Medium",
                progress = 1.0f,
                xpReward = 100,
                subtext = "Add support stake to prevent stem bend as tomatoes grow heavier.",
                progressText = "1 / 1 stake placed",
                isClaimed = false,
                isClaimable = true
            ),
            Quest(
                id = "quest_check_ph",
                title = "Check Soil pH Balance",
                difficulty = "Medium",
                progress = 1.0f,
                xpReward = 60,
                subtext = "Verify soil pH is between 6.0 and 6.8 for optimal nutrient absorption.",
                progressText = "pH 6.5 Optimal",
                isClaimed = false,
                isClaimable = true
            ),
            Quest(
                id = "quest_organic_fertilizer",
                title = "Apply Organic Fertilizer",
                difficulty = "Hard",
                progress = 1.0f,
                xpReward = 120,
                subtext = "Feed soil with compost tea or liquid kelp for fruiting energy.",
                progressText = "Fertilizer Applied",
                isClaimed = false,
                isClaimable = true
            ),
            Quest(
                id = "quest_inspect_undersides",
                title = "Inspect Leaf Undersides",
                difficulty = "Easy",
                progress = 1.0f,
                xpReward = 40,
                subtext = "Check under leaves for early signs of aphids, spider mites, or whiteflies.",
                progressText = "Pest Check Clean",
                isClaimed = false,
                isClaimable = true
            ),
            Quest(
                id = "quest_rotate_pot",
                title = "Rotate Pot for Sunlight",
                difficulty = "Easy",
                progress = 1.0f,
                xpReward = 30,
                subtext = "Turn pot 180 degrees to ensure even sunlight exposure on all sides.",
                progressText = "Rotated 180°",
                isClaimed = false,
                isClaimable = true
            ),
            Quest(
                id = "quest_wipe_dust",
                title = "Wipe Leaf Dust",
                difficulty = "Easy",
                progress = 1.0f,
                xpReward = 35,
                subtext = "Gently wipe dust off mature leaves to boost photosynthesis efficiency.",
                progressText = "Leaves Cleaned",
                isClaimed = false,
                isClaimable = true
            )
        )

        private fun careQuest(id: String, title: String, xp: Int, description: String) = Quest(
            id = id,
            title = title,
            xpReward = xp,
            subtext = description,
            progressText = "Mark when finished"
        )

        val ADDITIONAL_QUESTS = listOf(
            careQuest("quest_review_moisture", "Review Soil Moisture", 60,
                "Read the soil moisture sensor and record whether your plant needs water."),
            careQuest("quest_check_drainage", "Check Pot Drainage", 80,
                "Inspect the drainage holes and remove any blockages."),
            careQuest("quest_inspect_flowers", "Inspect Flower Buds", 100,
                "Check for flower buds and record what you find in the plant journal."),
            careQuest("quest_track_fruit", "Track Fruit Development", 150,
                "Check for forming tomatoes and record their development, or note that fruit has not appeared yet."),
            careQuest("quest_clean_tools", "Clean Gardening Tools", 60,
                "Clean your gardening tools before the next plant care session."),
            careQuest("quest_clear_debris", "Clear Fallen Debris", 80,
                "Remove fallen leaves and debris from around the pot."),
            careQuest("quest_check_irrigation", "Check the Watering System", 100,
                "Inspect the watering equipment for leaks and blocked outlets."),
            careQuest("quest_record_growth", "Record Plant Growth", 90,
                "Write a journal note about the plant's current growth stage and appearance."),
            careQuest("quest_check_ties", "Inspect Plant Supports", 100,
                "Check the stakes and ties, or note whether your plant is ready for a support."),
            careQuest("quest_plan_harvest", "Plan Your Harvest", 180,
                "Review the plant's stage and write a plan for monitoring its tomatoes as they ripen.")
        )

        val QUEST_POOL = ROTATING_QUEST_POOL + ADDITIONAL_QUESTS

        val DAILY_QUESTS = ROTATING_QUEST_POOL.take(ROTATING_DAILY_COUNT) + ADDITIONAL_QUESTS

        val DEFAULT_QUESTS = DAILY_QUESTS
    }
}
