package com.agriquest.data.remote

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * The AgriQuest queries used by the app, on top of [SupabaseClient].
 * Row Level Security in the database makes every query return only the logged-in player's rows.
 */
class SupabaseCloud(private val client: SupabaseClient) {

    data class Plant(val id: String, val name: String, val sowingDate: String?)

    /** One row of actuator_commands. [status]: pending, rejected, running, done, failed or expired. */
    data class Command(val id: Long, val status: String, val reason: String?)

    /** One row of sensor_readings. A null value means that sensor isn't fitted yet. */
    data class Reading(
        val soilMoisture: Float?,
        val airTemp: Float?,
        val humidity: Float?,
        val lightLux: Float?,
        val recordedAtMillis: Long,
    )

    /** The player's first plant (the database creates "Flora" for every new account). */
    suspend fun myPlant(): Plant? {
        val rows = JSONArray(client.get("plants?select=id,name,sowing_date&order=created_at.asc&limit=1"))
        if (rows.length() == 0) return null
        val row = rows.getJSONObject(0)
        return Plant(row.getString("id"), row.getString("name"), row.optStringOrNull("sowing_date"))
    }

    suspend fun displayName(): String? {
        val rows = JSONArray(client.get("profiles?select=display_name&limit=1"))
        return if (rows.length() == 0) null else rows.getJSONObject(0).optStringOrNull("display_name")
    }

    /** The newest reading for [plantId], or null if the plant has no readings yet. */
    suspend fun latestReading(plantId: String): Reading? {
        val rows = JSONArray(client.get(
            "sensor_readings?select=soil_moisture,air_temp,humidity,light_lux,recorded_at" +
                "&plant_id=eq.$plantId&order=recorded_at.desc&limit=1"
        ))
        if (rows.length() == 0) return null
        val row = rows.getJSONObject(0)
        return Reading(
            soilMoisture = row.optFloatOrNull("soil_moisture"),
            airTemp = row.optFloatOrNull("air_temp"),
            humidity = row.optFloatOrNull("humidity"),
            lightLux = row.optFloatOrNull("light_lux"),
            recordedAtMillis = parseTimestamp(row.getString("recorded_at")),
        )
    }

    /**
     * Asks the database to run an actuator (paper process 4.0). The database checks the safety
     * rules (maximum run time, cooldown, one command at a time) and returns the saved command:
     * status "pending" until the ESP32 picks it up, or "rejected" with the reason.
     */
    suspend fun requestCommand(plantId: String, actuator: String, durationSeconds: Int): Command {
        val args = JSONObject()
            .put("p_plant_id", plantId)
            .put("p_actuator", actuator)
            .put("p_duration_s", durationSeconds)
        val text = client.rpc("request_command", args).trim()
        // A function returning one row comes back as an object; accept a one-item list too.
        val row = if (text.startsWith("[")) JSONArray(text).getJSONObject(0) else JSONObject(text)
        return row.toCommand()
    }

    /** The current state of a command, to follow it from pending to done. */
    suspend fun command(id: Long): Command? {
        val rows = JSONArray(client.get("actuator_commands?select=id,status,reason&id=eq.$id"))
        return if (rows.length() == 0) null else rows.getJSONObject(0).toCommand()
    }

    private fun JSONObject.toCommand() = Command(getLong("id"), getString("status"), optStringOrNull("reason"))

    companion object {
        /** Parses Supabase timestamps like "2026-11-02T06:01:02.123456+00:00" (works on Android 7). */
        fun parseTimestamp(text: String): Long {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val base = format.parse(text.substring(0, 19))?.time ?: return System.currentTimeMillis()
            val offset = Regex("([+-])(\\d{2}):?(\\d{2})$").find(text)?.destructured?.let { (sign, hours, minutes) ->
                val millis = (hours.toLong() * 60 + minutes.toLong()) * 60_000L
                if (sign == "+") millis else -millis
            } ?: 0L
            return base - offset
        }
    }
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

private fun JSONObject.optFloatOrNull(key: String): Float? =
    if (isNull(key)) null else optDouble(key).takeIf { !it.isNaN() }?.toFloat()
