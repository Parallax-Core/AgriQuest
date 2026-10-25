package com.agriquest.data.simulation

import com.agriquest.domain.model.TelemetryData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.round
import kotlin.random.Random

/**
 * Real-Time Telemetry Simulation Engine for AgriQuest.
 *
 * Runs background simulation routine when [com.agriquest.domain.model.PlantSource.EMULATED_OFFLINE] is active:
 * - Gradually decays soil moisture by ~0.5% - 1.0% every tick until min threshold (40.0%).
 * - Fluctuate air temperature (23.5°C to 26.0°C), humidity (58% to 65%), and light intensity (12,000 to 15,500 Lux).
 */
class TelemetrySimulationEngine(
    private val tickIntervalMs: Long = 2500L,
    private val random: Random = Random.Default
) {
    /**
     * Computes the next simulated telemetry state based on realistic environmental dynamics.
     *
     * @param current Current sensor readings
     * @return Updated simulated [TelemetryData]
     */
    fun tick(current: TelemetryData): TelemetryData {
        // Decay soil moisture by ~0.5% to 1.0% per tick down to MIN_SOIL_MOISTURE (40.0%)
        val decayAmount = random.nextDouble(0.5, 1.0).toFloat()
        val nextMoisture = (current.soilMoisture - decayAmount).coerceAtLeast(MIN_SOIL_MOISTURE)

        // Fluctuate air temperature between 23.5°C and 26.0°C
        val tempDelta = random.nextDouble(-0.25, 0.25).toFloat()
        val nextTemp = (current.airTemperature + tempDelta).coerceIn(MIN_TEMP, MAX_TEMP)

        // Fluctuate relative humidity between 58.0% and 65.0%
        val humidityDelta = random.nextDouble(-0.4, 0.4).toFloat()
        val nextHumidity = (current.relativeHumidity + humidityDelta).coerceIn(MIN_HUMIDITY, MAX_HUMIDITY)

        // Fluctuate light intensity between 12,000 and 15,500 Lux
        val lightDelta = random.nextDouble(-150.0, 150.0).toFloat()
        val nextLight = (current.lightIntensity + lightDelta).coerceIn(MIN_LIGHT, MAX_LIGHT)

        return current.copy(
            soilMoisture = roundToSingleDecimal(nextMoisture),
            airTemperature = roundToSingleDecimal(nextTemp),
            relativeHumidity = roundToSingleDecimal(nextHumidity),
            lightIntensity = roundToSingleDecimal(nextLight)
        )
    }

    /**
     * Calculates updated telemetry data when pump is triggered.
     * Restores soil moisture by [restoreAmount] (default +15.0%), capped at 100.0%.
     */
    fun restoreMoistureFromPump(
        current: TelemetryData,
        restoreAmount: Float = DEFAULT_PUMP_RESTORE_AMOUNT
    ): TelemetryData {
        val updatedMoisture = (current.soilMoisture + restoreAmount).coerceAtMost(100.0f)
        return current.copy(soilMoisture = roundToSingleDecimal(updatedMoisture))
    }

    /**
     * Continuous coroutine flow emitting simulated telemetry updates periodically.
     */
    fun telemetryFlow(initial: TelemetryData = TelemetryData()): Flow<TelemetryData> = flow {
        var current = initial
        while (true) {
            delay(tickIntervalMs)
            current = tick(current)
            emit(current)
        }
    }

    private fun roundToSingleDecimal(value: Float): Float {
        return round(value * 10.0f) / 10.0f
    }

    companion object {
        const val DEFAULT_PUMP_RESTORE_AMOUNT = 15.0f

        const val MIN_SOIL_MOISTURE = 40.0f
        const val MIN_TEMP = 23.5f
        const val MAX_TEMP = 26.0f
        const val MIN_HUMIDITY = 58.0f
        const val MAX_HUMIDITY = 65.0f
        const val MIN_LIGHT = 12000.0f
        const val MAX_LIGHT = 15500.0f
    }
}
