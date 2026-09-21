package com.agriquest.domain.model

/**
 * Achievement / Award icon categories.
 */
enum class AchievementIcon {
    PLANT,
    WATER,
    SUN,
    HARVEST,
    IOT,
    STREAK
}

/**
 * Domain model representing an Achievement / Award in AgriQuest.
 *
 * @param id Unique achievement ID
 * @param title Title of the achievement (e.g., "First Plant")
 * @param description Subtext description of achievement requirements
 * @param xpReward XP reward value
 * @param isUnlocked Whether the achievement is unlocked
 * @param earnedDate Date string when earned (e.g., "Jul 29") or null if locked
 * @param iconType Icon category identifier
 */
data class Achievement(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val xpReward: Int = 100,
    val isUnlocked: Boolean = false,
    val earnedDate: String? = null,
    val iconType: AchievementIcon = AchievementIcon.PLANT
) {
    /** Formatted XP reward text (e.g., "+100 XP"). */
    val xpRewardText: String
        get() = "+$xpReward XP"

    /** Formatted earned date text (e.g., "Earned Jul 29" or "Locked"). */
    val earnedDateText: String
        get() = earnedDate?.let { "Earned $it" } ?: "Locked"

    companion object {
        /**
         * Pre-populated achievements matching design specifications and gamification goals.
         */
        val DEFAULT_ACHIEVEMENTS = listOf(
            Achievement(
                id = "ach_first_plant",
                title = "First Plant",
                description = "Started your first tomato growing journey.",
                xpReward = 100,
                isUnlocked = true,
                earnedDate = "Jul 29",
                iconType = AchievementIcon.PLANT
            ),
            Achievement(
                id = "ach_water_hero",
                title = "Water Hero",
                description = "Watered your plant 10 times without missing a day.",
                xpReward = 150,
                isUnlocked = true,
                earnedDate = "Aug 12",
                iconType = AchievementIcon.WATER
            ),
            Achievement(
                id = "ach_sun_seeker",
                title = "Sun Seeker",
                description = "Maintained optimal light for 5 consecutive days.",
                xpReward = 125,
                isUnlocked = true,
                earnedDate = "Aug 20",
                iconType = AchievementIcon.SUN
            ),
            Achievement(
                id = "ach_master_gardener",
                title = "Master Gardener",
                description = "Harvest your first crop with 90%+ health rating.",
                xpReward = 300,
                isUnlocked = false,
                earnedDate = null,
                iconType = AchievementIcon.HARVEST
            ),
            Achievement(
                id = "ach_iot_specialist",
                title = "IoT Specialist",
                description = "Connect an ESP32 hardware device to telemetry.",
                xpReward = 200,
                isUnlocked = false,
                earnedDate = null,
                iconType = AchievementIcon.IOT
            ),
            Achievement(
                id = "ach_green_thumb",
                title = "Green Thumb",
                description = "Reach a 14-day care streak.",
                xpReward = 250,
                isUnlocked = false,
                earnedDate = null,
                iconType = AchievementIcon.STREAK
            )
        )
    }
}
