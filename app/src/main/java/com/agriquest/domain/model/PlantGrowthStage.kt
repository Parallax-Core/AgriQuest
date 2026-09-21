package com.agriquest.domain.model

/**
 * Domain enum representing plant growth stages, titles, and XP leveling thresholds.
 *
 * @param stageLevel Level index (0, 1, 2, 3+)
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
    SEEDLING_SPROUT(
        stageLevel = 0,
        maxXp = 100,
        stageTitle = "Tiny Tomato Sprout",
        avatarLevelText = "Lvl. 0 Urban Beginner",
        speechBubble = "Welcome! I'm a tiny sprout getting started. Keep soil hydrated!"
    ),
    VEGETATIVE_LEAFY(
        stageLevel = 1,
        maxXp = 250,
        stageTitle = "Growing Vegetative Plant",
        avatarLevelText = "Lvl. 1 Grower",
        speechBubble = "Leaves are expanding nicely! Moisture and ambient light look good."
    ),
    FLOWERING(
        stageLevel = 2,
        maxXp = 450,
        stageTitle = "Flowering Tomato Plant",
        avatarLevelText = "Lvl. 2 Master",
        speechBubble = "Flowers are blooming! Your tomato plant is thriving."
    ),
    FRUITING_HARVEST(
        stageLevel = 3,
        maxXp = 700,
        stageTitle = "Vibrant Fruiting Tomato",
        avatarLevelText = "Lvl. 3 Harvest Master",
        speechBubble = "Fruits are ripening! Excellent urban farming skills."
    );

    companion object {
        fun fromLevel(level: Int): PlantGrowthStage {
            return when {
                level <= 0 -> SEEDLING_SPROUT
                level == 1 -> VEGETATIVE_LEAFY
                level == 2 -> FLOWERING
                else -> FRUITING_HARVEST
            }
        }

        fun fromName(name: String?): PlantGrowthStage {
            return entries.find { it.name == name } ?: SEEDLING_SPROUT
        }
    }
}
