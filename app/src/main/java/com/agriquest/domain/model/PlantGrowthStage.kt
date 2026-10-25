package com.agriquest.domain.model

/**
 * Domain enum representing plant growth stages, titles, and XP leveling thresholds.
 *
 * @param stageLevel Level index (0 through 5)
 * @param maxXp XP threshold required for this stage level
 * @param stageTitle Display title of growth stage ("Tiny Tomato Sprout")
 * @param avatarLevelText Avatar level display text ("Lvl. 0 Urban Beginner")
 * @param speechBubble Agro speech bubble message for avatar
 */
enum class PlantGrowthStage(
    val stageLevel: Int,
    val maxXp: Int,
    val stageTitle: String,
    val avatarLevelText: String,
    val speechBubble: String
) {
    GERMINATION(
        stageLevel = 0,
        maxXp = 1000,
        stageTitle = "Germinating Tomato Seed",
        avatarLevelText = "Lvl. 0 Urban Beginner",
        speechBubble = "Let's get growing! Keep the soil moist while your seed germinates."
    ),
    SEEDLING_SPROUT(
        stageLevel = 1,
        maxXp = 2800,
        stageTitle = "Tiny Tomato Sprout",
        avatarLevelText = "Lvl. 1 Seedling",
        speechBubble = "Welcome! I'm a tiny sprout getting started. Keep soil hydrated!"
    ),
    VEGETATIVE_LEAFY(
        stageLevel = 2,
        maxXp = 5600,
        stageTitle = "Growing Vegetative Plant",
        avatarLevelText = "Lvl. 2 Grower",
        speechBubble = "Leaves are expanding nicely! Moisture and ambient light look good."
    ),
    FLOWERING(
        stageLevel = 3,
        maxXp = 7000,
        stageTitle = "Flowering Tomato Plant",
        avatarLevelText = "Lvl. 3 Master",
        speechBubble = "Flowers are blooming! Your tomato plant is thriving."
    ),
    FRUITING_HARVEST(
        stageLevel = 4,
        maxXp = 10500,
        stageTitle = "Vibrant Fruiting Tomato",
        avatarLevelText = "Lvl. 4 Fruit Grower",
        speechBubble = "Tomatoes are forming! Keep caring for your growing fruit."
    ),
    RIPENING(
        stageLevel = 5,
        maxXp = 12000,
        stageTitle = "Ripening Tomato Plant",
        avatarLevelText = "Lvl. 5 Harvest Master",
        speechBubble = "Your tomatoes are ripening! Get ready to harvest."
    );

    val displayName: String
        get() = when (this) {
            GERMINATION -> "Seed Germination"
            SEEDLING_SPROUT -> "Seedling"
            VEGETATIVE_LEAFY -> "Vegetative Growth"
            FLOWERING -> "Flowering"
            FRUITING_HARVEST -> "Fruiting"
            RIPENING -> "Ripening"
        }

    val stageLabel: String
        get() = "Stage ${stageLevel + 1} - $displayName"

    val startDay: Int get() = listOf(0, 10, 28, 56, 70, 105)[stageLevel]
    val timelineText: String get() = listOf("Days 0-9", "Days 10-27", "Days 28-55", "Days 56-69", "Days 70-104", "Days 105-120+")[stageLevel]

    companion object {
        const val DAILY_XP_LIMIT = 100
        const val EXPECTED_HARVEST_DAY = 120
        fun fromDays(days: Int): PlantGrowthStage = entries.lastOrNull { days >= it.startDay } ?: GERMINATION

        fun fromLevel(level: Int): PlantGrowthStage {
            return entries[level.coerceIn(0, entries.lastIndex)]
        }

        fun fromXp(totalXp: Int): PlantGrowthStage =
            entries.firstOrNull { totalXp < it.maxXp } ?: RIPENING

        fun fromName(name: String?): PlantGrowthStage {
            return entries.find { it.name == name } ?: GERMINATION
        }
    }
}
