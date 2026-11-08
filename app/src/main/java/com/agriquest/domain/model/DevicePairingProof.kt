package com.agriquest.domain.model

import org.json.JSONObject

data class Esp32DiscoveredDevice(val deviceName: String, val macAddress: String, val serviceUuid: String, val rssi: Int)
data class OwnedEsp32Device(val deviceId: String, val plantId: String?)
data class OwnedPlant(val plantId: String, val name: String)

/** Memory-only, one-use physical-access proof. Never persist or log this code. */
class DevicePairingProof(val deviceId: String, val pairingCode: String) {
    init {
        require(deviceId.matches(Regex("[A-Za-z0-9_-]{3,40}"))) { "Invalid pairing device identifier." }
        require(pairingCode.matches(Regex("[0-9a-f]{64}"))) { "Firmware must provide a secure pairing proof." }
    }

    override fun toString() = "DevicePairingProof(deviceId=$deviceId, pairingCode=[redacted])"

    companion object {
        fun parse(payload: String): DevicePairingProof {
            require(payload.length <= 1024) { "Invalid pairing response." }
            val json = JSONObject(payload)
            require(json.optString("type") == "pairing_proof" && json.optInt("version") == 1) {
                "Firmware does not support secure pairing."
            }
            return DevicePairingProof(json.getString("deviceId"), json.getString("pairingCode"))
        }
    }
}
