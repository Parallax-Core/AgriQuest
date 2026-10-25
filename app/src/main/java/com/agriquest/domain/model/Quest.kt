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
        private fun careQuest(id: String, title: String, xp: Int, description: String) = Quest(
            id = id,
            title = title,
            xpReward = xp,
            subtext = description,
            progressText = "Mark when finished"
        )

        // Safe daily observations; water only when needed.
        private val DAILY_CORE = listOf(
            careQuest("quest_review_moisture", "Check Soil and Water Needs", 30,
                "Check soil and drainage. Water only if needed, never just to earn XP."),
            careQuest("quest_inspect_plant", "Inspect Plant and Growing Conditions", 30,
                "Check light, temperature and growth. After emergence, inspect leaves for pests.")
        )

        // All are safe at every stage, including seeds that have not emerged yet.
        val ROTATING_QUESTS = listOf(
            careQuest("quest_record_growth", "Record Today's Growth", 30,
                "Record emergence, leaves, flowers or fruit in your journal. Note no change if appropriate."),
            careQuest("quest_check_drainage", "Inspect Pot Drainage", 30,
                "Check that drainage holes are clear and the pot is not sitting in standing water."),
            careQuest("quest_review_light", "Review Today's Light Exposure", 30,
                "Record where the pot receives light and any prolonged shade. If seedlings have emerged, check for leaning."),
            careQuest("quest_clean_tools", "Check Gardening Tool Cleanliness", 30,
                "Inspect tools and clean dirty surfaces before the next care session. Note if tools are already clean."),
            careQuest("quest_check_airflow", "Check Airflow Around the Pot", 30,
                "Check for blocked ventilation around the growing area, while protecting young seedlings from strong wind."),
            careQuest("quest_clear_debris", "Inspect the Growing Area", 30,
                "Look for fallen debris around the pot and clear it if present. Avoid disturbing seeds or roots."),
            careQuest("quest_compare_growth", "Compare With Your Previous Growth Note", 30,
                "Compare the plant with an earlier note or photo. If none exists, create a baseline; no visible change is valid."),
            careQuest("quest_review_temperature", "Review Temperature Changes", 30,
                "Record the temperature and any recent hot or cool conditions around the pot.")
        )

        /** Calendar rotation guarantees a different third mission on consecutive days.
         * UTC is used only to calculate a day index from a local calendar date (DST safe).
         * Reopening or refreshing on the same date always returns the same mission IDs.
         */
        fun forDate(date: String): List<Quest> {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                isLenient = false
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val day = requireNotNull(parser.parse(date)).time / 86_400_000L
            val index = Math.floorMod(day, ROTATING_QUESTS.size.toLong()).toInt()
            return DAILY_CORE + ROTATING_QUESTS[index]
        }

        val QUEST_POOL = DAILY_CORE + ROTATING_QUESTS
        val DAILY_QUESTS: List<Quest>
            get() = forDate(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()))
        val DEFAULT_QUESTS: List<Quest> get() = DAILY_QUESTS
    }
}
