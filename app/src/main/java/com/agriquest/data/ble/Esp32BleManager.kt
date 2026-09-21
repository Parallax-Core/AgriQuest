package com.agriquest.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.agriquest.domain.model.ConnectionState
import com.agriquest.domain.model.ConnectionStatus
import com.agriquest.domain.model.FanMode
import com.agriquest.domain.model.TelemetryData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * BLE Device Scanner & Hardware Manager for ESP32 IoT Microcontroller.
 *
 * Handles Bluetooth Low Energy lifecycle:
 * - Scanning filtered by ESP32 Device Name / Service UUID (`0000ffe0-0000-1000-8000-00805f9b34fb`).
 * - Connection lifecycle: [ConnectionStatus.DISCONNECTED], [ConnectionStatus.SCANNING],
 *   [ConnectionStatus.AWAITING_DEVICE], [ConnectionStatus.CONNECTING], [ConnectionStatus.CONNECTED].
 * - GATT Characteristic Notification/Read listeners for live JSON telemetry payloads:
 *   e.g. `{"moisture": 68.5, "temp": 24.2, "humidity": 61.0, "light": 14500}`.
 * - Command writing hooks for actuator triggers: `PUMP_ON:3`, `FAN_MODE:AUTO/ON/OFF`.
 */
class Esp32BleManager(
    private val context: Context? = null,
    coroutineScope: CoroutineScope? = null
) {
    private val scope = coroutineScope ?: CoroutineScope(Dispatchers.Default)

    private val bluetoothManager: BluetoothManager? =
        context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private var targetCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow(
        ConnectionState(
            deviceName = "ESP32",
            status = ConnectionStatus.DISCONNECTED,
            statusText = "Disconnected"
        )
    )
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _telemetryDataFlow = MutableSharedFlow<TelemetryData>(replay = 1)
    val telemetryDataFlow: SharedFlow<TelemetryData> = _telemetryDataFlow.asSharedFlow()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val deviceName = try { device.name ?: "ESP32" } catch (_: SecurityException) { "ESP32" }
                if (deviceName.contains("ESP32", ignoreCase = true) || deviceName.contains("AgriQuest", ignoreCase = true)) {
                    stopScan()
                    connectToDevice(device)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            updateConnectionStatus(ConnectionStatus.AWAITING_DEVICE, "Scan Failed (Error $errorCode)")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                updateConnectionStatus(ConnectionStatus.CONNECTING, "Discovering Services...")
                try {
                    gatt?.discoverServices()
                } catch (e: SecurityException) {
                    updateConnectionStatus(ConnectionStatus.AWAITING_DEVICE, "Permission Denied")
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bluetoothGatt?.close()
                bluetoothGatt = null
                targetCharacteristic = null
                updateConnectionStatus(ConnectionStatus.DISCONNECTED, "Disconnected")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val service = gatt.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                if (characteristic != null) {
                    targetCharacteristic = characteristic
                    try {
                        gatt.setCharacteristicNotification(characteristic, true)
                        val descriptor = characteristic.getDescriptor(CCCD_UUID)
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                    } catch (_: SecurityException) { }
                    updateConnectionStatus(ConnectionStatus.CONNECTED, "Connected")
                } else {
                    updateConnectionStatus(ConnectionStatus.CONNECTED, "Connected (GATT Ready)")
                }
            } else {
                updateConnectionStatus(ConnectionStatus.AWAITING_DEVICE, "Service Discovery Failed")
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val bytes = characteristic?.value ?: return
            val payload = String(bytes, StandardCharsets.UTF_8)
            processIncomingPayload(payload)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val payload = String(value, StandardCharsets.UTF_8)
            processIncomingPayload(payload)
        }
    }

    /**
     * Starts scanning for nearby ESP32 BLE hardware filtered by name or Service UUID.
     */
    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            updateConnectionStatus(ConnectionStatus.AWAITING_DEVICE, "Awaiting ESP32 Hardware")
            return
        }

        updateConnectionStatus(ConnectionStatus.SCANNING, "Scanning for ESP32...")

        runCatching {
            val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
            if (scanner != null) {
                val filter = ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(SERVICE_UUID))
                    .build()
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                scanner.startScan(listOf(filter), settings, scanCallback)
            } else {
                updateConnectionStatus(ConnectionStatus.AWAITING_DEVICE, "BLE Scanner Unavailable")
            }
        }.onFailure {
            updateConnectionStatus(ConnectionStatus.AWAITING_DEVICE, "Awaiting ESP32 Hardware")
        }
    }

    /**
     * Stops current BLE scan routine.
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        runCatching {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        }
        if (_connectionState.value.status == ConnectionStatus.SCANNING) {
            updateConnectionStatus(ConnectionStatus.AWAITING_DEVICE, "Awaiting ESP32 Hardware")
        }
    }

    /**
     * Connects to specified Bluetooth device.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        val deviceName = try { device.name ?: "ESP32" } catch (_: SecurityException) { "ESP32" }
        updateConnectionStatus(ConnectionStatus.CONNECTING, "Connecting to $deviceName...")

        runCatching {
            if (context != null) {
                bluetoothGatt = device.connectGatt(context, false, gattCallback)
            }
        }.onFailure {
            updateConnectionStatus(ConnectionStatus.AWAITING_DEVICE, "Connection Failed")
        }
    }

    /**
     * Disconnects active GATT session.
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        runCatching {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        }
        bluetoothGatt = null
        targetCharacteristic = null
        updateConnectionStatus(ConnectionStatus.DISCONNECTED, "Disconnected")
    }

    /**
     * Writes pump activation command to ESP32 GATT characteristic: `PUMP_ON:<durationSeconds>`.
     */
    fun sendPumpCommand(durationSeconds: Int = 3) {
        sendCommand("PUMP_ON:$durationSeconds")
    }

    /**
     * Writes fan control mode command to ESP32 GATT characteristic: `FAN_MODE:<AUTO/ON/OFF>`.
     */
    fun sendFanModeCommand(mode: FanMode) {
        sendCommand("FAN_MODE:${mode.name}")
    }

    /**
     * Transmits raw String command to ESP32 BLE peripheral.
     */
    @SuppressLint("MissingPermission")
    fun sendCommand(command: String) {
        val gatt = bluetoothGatt ?: return
        val characteristic = targetCharacteristic ?: return
        runCatching {
            val bytes = command.toByteArray(StandardCharsets.UTF_8)
            characteristic.value = bytes
            gatt.writeCharacteristic(characteristic)
        }
    }

    /**
     * Processes incoming raw payload String from ESP32 BLE notification.
     */
    fun processIncomingPayload(payload: String) {
        val telemetry = parseTelemetryJson(payload)
        if (telemetry != null) {
            scope.launch {
                _telemetryDataFlow.emit(telemetry)
            }
        }
    }

    /**
     * Parses JSON telemetry payload into [TelemetryData].
     * Supports formats like `{"moisture": 68.5, "temp": 24.2, "humidity": 61.0, "light": 14500}`.
     */
    fun parseTelemetryJson(jsonString: String): TelemetryData? {
        val trimmed = jsonString.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return null
        }
        return runCatching {
            val moisture = extractJsonDouble(trimmed, "moisture", "soilMoisture")?.toFloat()
            val temp = extractJsonDouble(trimmed, "temp", "airTemperature", "temperature")?.toFloat()
            val humidity = extractJsonDouble(trimmed, "humidity", "relativeHumidity")?.toFloat()
            val light = extractJsonDouble(trimmed, "light", "lightIntensity", "lux")?.toFloat()

            if (moisture == null && temp == null && humidity == null && light == null) {
                return null
            }

            TelemetryData(
                soilMoisture = moisture ?: 68.0f,
                airTemperature = temp ?: 24.5f,
                relativeHumidity = humidity ?: 62.0f,
                lightIntensity = light ?: 14200.0f
            )
        }.getOrNull()
    }

    private fun extractJsonDouble(json: String, vararg keys: String): Double? {
        for (key in keys) {
            val pattern = """"$key"\s*:\s*(-?[0-9]+(?:\.[0-9]+)?)""".toRegex(RegexOption.IGNORE_CASE)
            val match = pattern.find(json)
            if (match != null) {
                return match.groupValues[1].toDoubleOrNull()
            }
        }
        return null
    }

    /**
     * Helper to manually set connection state (useful for UI state binding & unit tests).
     */
    fun updateConnectionStatus(status: ConnectionStatus, statusText: String? = null) {
        val defaultText = when (status) {
            ConnectionStatus.CONNECTED -> "Connected"
            ConnectionStatus.CONNECTING -> "Connecting..."
            ConnectionStatus.SCANNING -> "Scanning for ESP32..."
            ConnectionStatus.AWAITING_DEVICE -> "Awaiting ESP32 Hardware"
            ConnectionStatus.DISCONNECTED -> "Disconnected"
        }
        _connectionState.value = _connectionState.value.copy(
            status = status,
            statusText = statusText ?: defaultText
        )
    }

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
