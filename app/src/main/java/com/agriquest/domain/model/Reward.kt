package com.agriquest.domain.model

/** A finished mission or quiz, shown in the "Congratulations!" dialog. */
data class Reward(
    val title: String,
    /** XP actually added (can be less than [xpOffered] once the daily XP limit is reached). */
    val xpGranted: Int,
    val xpOffered: Int,
    val kind: Kind
) {
    enum class Kind { QUEST, QUIZ, ALL_QUESTS }
}

/** When the AI Leaf Detector unlocks. Change this one number to move the unlock day. */
object AiUnlock {
    const val REQUIRED_LOGIN_DAYS = 7
}
