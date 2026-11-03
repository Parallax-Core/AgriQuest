package com.agriquest.domain.model

/**
 * Progress of one "Water Plant" request, shown under the soil-moisture reading.
 *
 * In Live IoT mode it follows the pump command through Supabase:
 * SENDING (app -> database safety checks) -> WAITING (ESP32 picks it up within ~5 s)
 * -> RUNNING (pump on) -> DONE, or FAILED with the reason (e.g. the cooldown).
 */
data class WateringStatus(val phase: Phase, val message: String) {
    enum class Phase { SENDING, WAITING, RUNNING, DONE, FAILED }

    /** True while a watering request is still in progress. */
    val isBusy: Boolean
        get() = phase == Phase.SENDING || phase == Phase.WAITING || phase == Phase.RUNNING
}
