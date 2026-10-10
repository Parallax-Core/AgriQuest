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
        /**
         * Rich pool of 8+ agricultural quests for daily rotation and shuffle.
         */
        val QUEST_POOL = listOf(
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

        val DAILY_QUESTS = QUEST_POOL.take(3)

        val DEFAULT_QUESTS = DAILY_QUESTS
    }
}
