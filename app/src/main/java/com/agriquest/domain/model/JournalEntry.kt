package com.agriquest.domain.model

/**
 * Category tag for plant care journal log entries.
 */
enum class JournalCategory(val emoji: String) {
    WATERING("💧"),
    PRUNING("✂️"),
    PLANTING("🌱"),
    FERTILIZING("🧪"),
    GENERAL("📝")
}

/**
 * Domain model representing a care log note entry in the plant journal.
 *
 * @param id Unique ID of entry
 * @param date Formatted date string (e.g. "Aug 12")
 * @param note Care note description (e.g. "Watered 250ml")
 * @param category Category type for icon display
 */
data class JournalEntry(
    val id: String,
    val date: String,
    val note: String,
    val category: JournalCategory = JournalCategory.WATERING
) {
    companion object {
        val DEFAULT_ENTRIES = listOf(
            JournalEntry("1", "Aug 12", "Watered 250ml", JournalCategory.WATERING),
            JournalEntry("2", "Aug 10", "Pruned bottom branches", JournalCategory.PRUNING),
            JournalEntry("3", "Aug 5", "Planted seedling", JournalCategory.PLANTING)
        )
    }
}
