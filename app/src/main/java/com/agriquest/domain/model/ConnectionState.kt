package com.agriquest.domain.model

/**
 * Enum defining IoT hardware connection status.
 */
enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    SCANNING,
    AWAITING_DEVICE,
    DISCONNECTED
}

/**
 * Domain model representing BLE / IoT device connection state.
 *
 * @param deviceName Name of the connected hardware device ("ESP32")
 * @param status Connection status (CONNECTED, CONNECTING, SCANNING, AWAITING_DEVICE, DISCONNECTED)
 * @param statusText Display status label ("Connected")
 */
data class ConnectionState(
    val deviceName: String = "ESP32",
    val status: ConnectionStatus = ConnectionStatus.CONNECTED,
    val statusText: String = "Connected"
) {
    /** Indicator boolean for UI status pill (true = green indicator when connected). */
    val isIndicatorGreen: Boolean
        get() = status == ConnectionStatus.CONNECTED

    /** Color ARGB representation (Green for Connected, Orange for Connecting/Scanning, Blue/Red for Awaiting/Disconnected). */
    val indicatorColorArgb: Long
        get() = when (status) {
            ConnectionStatus.CONNECTED -> 0xFF4CAF50 // Green
            ConnectionStatus.CONNECTING,
            ConnectionStatus.SCANNING -> 0xFFFF9800 // Orange
            ConnectionStatus.AWAITING_DEVICE -> 0xFF0288D1 // Blue
            ConnectionStatus.DISCONNECTED -> 0xFFF44336 // Red
        }
}
