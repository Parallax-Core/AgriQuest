package com.agriquest.domain.model

/**
 * Enum defining fan operating modes.
 */
enum class FanMode {
    AUTO,
    ON,
    OFF;

    /** Returns next FanMode when cycled sequentially: AUTO -> ON -> OFF -> AUTO. */
    fun next(): FanMode = when (this) {
        AUTO -> ON
        ON -> OFF
        OFF -> AUTO
    }
}

/**
 * Domain model representing hardware actuator states (Water Pump & Fan).
 *
 * @param isPumpActive Indicates whether the water pump is actively pumping
 * @param pumpRemainingSeconds Countdown in seconds remaining for active pump cycle
 * @param fanMode Current Fan mode (AUTO / ON / OFF)
 */
data class ActuatorState(
    val isPumpActive: Boolean = false,
    val pumpRemainingSeconds: Int = 0,
    val fanMode: FanMode = FanMode.AUTO
)
