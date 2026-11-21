package com.agriquest.domain.model

import java.util.Locale

/**
 * Today's sensor-based quest progress. Saved so progress survives closing the app.
 *
 * @param minutes minutes of readings spent in each timed goal's zone today
 * @param lastReadingAt phone time of the previous reading (0 = none yet today)
 * @param sawDry the soil was below the watering threshold at some point today
 * @param recovered after being dry, the soil came back above 60%
 * @param wateredAt phone time of today's last confirmed watering (0 = none)
 */
data class QuestProgressState(
    val date: String,
    val minutes: Map<QuestGoal, Float> = emptyMap(),
    val lastReadingAt: Long = 0L,
    val sawDry: Boolean = false,
    val recovered: Boolean = false,
    val wateredAt: Long = 0L
)

/** Turns sensor readings into quest progress. Pure logic, no Android code. */
object QuestEvaluator {
    /** A gap longer than this between readings is not counted (the sensor was offline). */
    const val MAX_GAP_MS = ConnectionState.HEARTBEAT_TIMEOUT_MS
    const val RECOVERED_MOISTURE = 25f

    fun onReading(state: QuestProgressState, telemetry: TelemetryData, nowMs: Long): QuestProgressState {
        val gap = if (state.lastReadingAt == 0L) 0L else nowMs - state.lastReadingAt
        val creditMinutes = if (gap in 1..MAX_GAP_MS) gap / 60_000f else 0f
        val minutes = state.minutes.toMutableMap()
        fun credit(goal: QuestGoal, inZone: Boolean) {
            if (inZone && creditMinutes > 0f) minutes[goal] = (minutes[goal] ?: 0f) + creditMinutes
        }
        val m = telemetry.soilMoisture
        credit(QuestGoal.STAY_ONLINE, true)
        credit(QuestGoal.MOISTURE_ZONE, m.isFinite() && m in TelemetryData.MIN_OPTIMAL_SOIL_MOISTURE..TelemetryData.MAX_OPTIMAL_SOIL_MOISTURE)
        credit(QuestGoal.TEMP_ZONE, telemetry.airTemperature.isFinite() && telemetry.isAirTemperatureOptimal)
        credit(QuestGoal.HUMIDITY_ZONE, telemetry.relativeHumidity.isFinite() && telemetry.isHumidityOptimal)
        credit(QuestGoal.LIGHT_TIME, telemetry.lightIntensity.isFinite() && telemetry.lightIntensity >= TelemetryData.MIN_OPTIMAL_LIGHT_LUX)
        val dryNow = telemetry.needsWatering
        val sawDry = state.sawDry || dryNow
        val recovered = state.recovered || (state.sawDry && m.isFinite() && m >= RECOVERED_MOISTURE)
        return state.copy(minutes = minutes, lastReadingAt = nowMs, sawDry = sawDry, recovered = recovered)
    }

    /** A pump run confirmed by the ESP32 (or the simulator). */
    fun onWatered(state: QuestProgressState, nowMs: Long): QuestProgressState = state.copy(wateredAt = nowMs, sawDry = true)

    fun progress(quest: Quest, state: QuestProgressState): Float = when (quest.goal) {
        QuestGoal.WATER_WHEN_DRY -> if (state.wateredAt > 0L) 1f else 0f
        QuestGoal.RECOVER_FROM_DRY -> when {
            state.recovered -> 1f
            state.sawDry -> 0.5f
            else -> 0f
        }
        else -> ((state.minutes[quest.goal] ?: 0f) / quest.target.coerceAtLeast(1f)).coerceIn(0f, 1f)
    }

    fun progressText(quest: Quest, state: QuestProgressState, telemetry: TelemetryData): String {
        fun value(v: Float, unit: String) = if (v.isFinite()) String.format(Locale.US, "%.0f%s", v, unit) else "—"
        val done = progress(quest, state) >= 1f
        return when (quest.goal) {
            QuestGoal.WATER_WHEN_DRY -> when {
                done -> "Watering confirmed"
                telemetry.needsWatering -> "Soil is dry: water now"
                else -> "Moisture ${value(telemetry.soilMoisture, "%")} · water when below ${TelemetryData.WATERING_THRESHOLD.toInt()}%"
            }
            QuestGoal.RECOVER_FROM_DRY -> when {
                done -> "Roots recovered"
                state.sawDry -> "Bring moisture above ${RECOVERED_MOISTURE.toInt()}% (now ${value(telemetry.soilMoisture, "%")})"
                else -> "Waits until the soil gets dry"
            }
            else -> {
                val reading = when (quest.goal) {
                    QuestGoal.MOISTURE_ZONE -> "Moisture ${value(telemetry.soilMoisture, "%")}"
                    QuestGoal.TEMP_ZONE -> "Temp ${value(telemetry.airTemperature, "°C")}"
                    QuestGoal.HUMIDITY_ZONE -> "Humidity ${value(telemetry.relativeHumidity, "%")}"
                    QuestGoal.LIGHT_TIME -> "Light ${value(telemetry.lightIntensity, " lux")}"
                    else -> "Online"
                }
                val min = (state.minutes[quest.goal] ?: 0f).toInt().coerceAtMost(quest.target.toInt())
                "$reading · $min/${quest.target.toInt()} min"
            }
        }
    }

    /** Updates every unclaimed quest's progress, claimable flag and text. */
    fun apply(quests: List<Quest>, state: QuestProgressState, telemetry: TelemetryData): List<Quest> = quests.map { q ->
        if (q.isClaimed) q else {
            val p = progress(q, state)
            q.copy(progress = p, isClaimable = p >= 1f, progressText = progressText(q, state, telemetry))
        }
    }
}
