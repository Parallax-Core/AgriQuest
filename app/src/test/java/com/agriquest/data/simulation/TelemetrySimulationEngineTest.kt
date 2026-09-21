package com.agriquest.data.simulation

import com.agriquest.domain.model.TelemetryData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetrySimulationEngineTest {

    @Test
    fun simulationTick_decaysMoistureAndFluctuatesParameters() {
        val engine = TelemetrySimulationEngine(random = Random(12345))
        val initial = TelemetryData(
            soilMoisture = 68.0f,
            airTemperature = 24.5f,
            relativeHumidity = 62.0f,
            lightIntensity = 14200.0f
        )

        val next = engine.tick(initial)

        // Soil moisture decays by ~0.5% - 1.0%
        assertTrue("Soil moisture should decay from initial 68.0", next.soilMoisture < 68.0f)
        assertTrue("Soil moisture decay floor is 40.0%", next.soilMoisture >= TelemetrySimulationEngine.MIN_SOIL_MOISTURE)

        // Air temp within bounds
        assertTrue("Temperature within min bound", next.airTemperature >= TelemetrySimulationEngine.MIN_TEMP)
        assertTrue("Temperature within max bound", next.airTemperature <= TelemetrySimulationEngine.MAX_TEMP)

        // Humidity within bounds
        assertTrue("Humidity within min bound", next.relativeHumidity >= TelemetrySimulationEngine.MIN_HUMIDITY)
        assertTrue("Humidity within max bound", next.relativeHumidity <= TelemetrySimulationEngine.MAX_HUMIDITY)

        // Light within bounds
        assertTrue("Light within min bound", next.lightIntensity >= TelemetrySimulationEngine.MIN_LIGHT)
        assertTrue("Light within max bound", next.lightIntensity <= TelemetrySimulationEngine.MAX_LIGHT)
    }

    @Test
    fun simulationTick_doesNotDecayMoistureBelowMinThreshold() {
        val engine = TelemetrySimulationEngine(random = Random(12345))
        val lowMoisture = TelemetryData(soilMoisture = 40.2f)

        val next = engine.tick(lowMoisture)

        assertEquals(40.0f, next.soilMoisture, 0.01f)
    }

    @Test
    fun restoreMoistureFromPump_restoresMoistureBy15PercentAndCapsAt100() {
        val engine = TelemetrySimulationEngine()
        val initial = TelemetryData(soilMoisture = 50.0f)

        val restored = engine.restoreMoistureFromPump(initial, 15.0f)
        assertEquals(65.0f, restored.soilMoisture, 0.01f)

        val highMoisture = TelemetryData(soilMoisture = 92.0f)
        val cappedRestored = engine.restoreMoistureFromPump(highMoisture, 15.0f)
        assertEquals(100.0f, cappedRestored.soilMoisture, 0.01f)
    }

    @Test
    fun telemetryFlow_emitsSimulatedData() = runTest {
        val engine = TelemetrySimulationEngine(tickIntervalMs = 10L, random = Random(42))
        val initial = TelemetryData(soilMoisture = 70.0f)

        val emitted = engine.telemetryFlow(initial).first()

        assertTrue(emitted.soilMoisture < 70.0f)
    }
}
