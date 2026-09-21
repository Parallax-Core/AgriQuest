package com.agriquest.domain.model

import androidx.annotation.DrawableRes
import com.agriquest.R

/**
 * 7 Plant Status Moodles domain enum for AgriQuest Mobile.
 */
enum class PlantMoodle(
    val displayName: String,
    @DrawableRes val iconResId: Int
) {
    WILTING("Wilting", R.drawable.moodle_wilting),
    THRIVING("Thriving", R.drawable.moodle_thriving),
    THIRSTY("Thirsty", R.drawable.moodle_thirsty),
    READY_TO_HARVEST("Ready to Harvest", R.drawable.moodle_ready_to_harvest),
    HEAT_STRESSED("Heat-Stressed", R.drawable.moodle_heat_stressed),
    TOO_COLD("Too Cold", R.drawable.moodle_too_cold),
    OVER_WATERED("Over-Watered", R.drawable.moodle_over_watered)
}
