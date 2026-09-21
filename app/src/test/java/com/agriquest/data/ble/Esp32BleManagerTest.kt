package com.agriquest.data.ble

import com.agriquest.domain.model.ConnectionStatus
import com.agriquest.domain.model.TelemetryData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Esp32BleManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun parseTelemetryJson_validStandardJson_parsesCorrectly() {
        val manager = Esp32BleManager(coroutineScope = testScope)
        val jsonString = """{"moisture": 68.5, "temp": 24.2, "humidity": 61.0, "light": 14500}"""

        val telemetry = manager.parseTelemetryJson(jsonString)

        assertNotNull(telemetry)
        assertEquals(68.5f, telemetry!!.soilMoisture, 0.01f)
        assertEquals(24.2f, telemetry.airTemperature, 0.01f)
        assertEquals(61.0f, telemetry.relativeHumidity, 0.01f)
        assertEquals(14500.0f, telemetry.lightIntensity, 0.01f)
    }

    @Test
    fun parseTelemetryJson_alternativeFieldKeys_parsesCorrectly() {
        val manager = Esp32BleManager(coroutineScope = testScope)
        val jsonString = """{"soilMoisture": 72.0, "airTemperature": 25.1, "relativeHumidity": 64.5, "lightIntensity": 15000.0}"""

        val telemetry = manager.parseTelemetryJson(jsonString)

        assertNotNull(telemetry)
        assertEquals(72.0f, telemetry!!.soilMoisture, 0.01f)
        assertEquals(25.1f, telemetry.airTemperature, 0.01f)
        assertEquals(64.5f, telemetry.relativeHumidity, 0.01f)
        assertEquals(15000.0f, telemetry.lightIntensity, 0.01f)
    }

    @Test
    fun parseTelemetryJson_invalidJson_returnsNull() {
        val manager = Esp32BleManager(coroutineScope = testScope)

        val telemetry = manager.parseTelemetryJson("INVALID_NON_JSON")

        assertNull(telemetry)
    }

    @Test
    fun connectionStateTransitions_updatesStateFlowCorrectly() {
        val manager = Esp32BleManager(coroutineScope = testScope)

        assertEquals(ConnectionStatus.DISCONNECTED, manager.connectionState.value.status)

        manager.updateConnectionStatus(ConnectionStatus.SCANNING)
        assertEquals(ConnectionStatus.SCANNING, manager.connectionState.value.status)
        assertEquals("Scanning for ESP32...", manager.connectionState.value.statusText)

        manager.updateConnectionStatus(ConnectionStatus.AWAITING_DEVICE)
        assertEquals(ConnectionStatus.AWAITING_DEVICE, manager.connectionState.value.status)
        assertEquals("Awaiting ESP32 Hardware", manager.connectionState.value.statusText)

        manager.updateConnectionStatus(ConnectionStatus.CONNECTING)
        assertEquals(ConnectionStatus.CONNECTING, manager.connectionState.value.status)

        manager.updateConnectionStatus(ConnectionStatus.CONNECTED)
        assertEquals(ConnectionStatus.CONNECTED, manager.connectionState.value.status)
    }

    @Test
    fun processIncomingPayload_emitsTelemetryDataToFlow() = runTest {
        val manager = Esp32BleManager(coroutineScope = this)
        val payload = """{"moisture": 75.0, "temp": 25.0, "humidity": 60.0, "light": 14000}"""

        manager.processIncomingPayload(payload)

        val received = manager.telemetryDataFlow.first()
        assertEquals(75.0f, received.soilMoisture, 0.01f)
    }
}
