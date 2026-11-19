package com.agriquest.domain.model

/**
 * Progress of one Fan or Light command.
 *
 * Live IoT mode: SENDING (app -> database) -> WAITING (the ESP32 checks every ~5 s) -> DONE once the
 * ESP32 reports that it switched, or FAILED with the reason. The in-game animation changes only on DONE.
 * Emulated mode goes straight to DONE.
 */
data class CommandStatus(val phase: Phase, val message: String) {
    enum class Phase { SENDING, WAITING, DONE, FAILED }
}
