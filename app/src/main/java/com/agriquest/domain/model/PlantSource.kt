package com.agriquest.domain.model

/**
 * Domain enum representing the hardware/emulation source of plant telemetry data.
 *
 * @param displayName Human-readable label for UI selection options.
 */
enum class PlantSource(val displayName: String) {
    EMULATED_OFFLINE("Offline Plant (Emulated)"),
    ESP32_HARDWARE("ESP32 Hardware (Live IoT)");

    companion object {
        fun fromName(name: String?): PlantSource {
            return entries.find { it.name == name } ?: EMULATED_OFFLINE
        }
    }
}
