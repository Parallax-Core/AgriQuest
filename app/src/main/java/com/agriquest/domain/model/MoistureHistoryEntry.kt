package com.agriquest.domain.model

/**
 * Domain model representing a soil moisture reading point in the history timeline/chart.
 *
 * @param dayLabel Timeline label (e.g. "Today", "Yesterday", "2 days ago")
 * @param moisturePercent Soil moisture percentage value (e.g. 68.0f)
 */
data class MoistureHistoryEntry(
    val dayLabel: String,
    val moisturePercent: Float
)
