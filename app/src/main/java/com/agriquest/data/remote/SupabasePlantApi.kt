package com.agriquest.data.remote

import com.agriquest.BuildConfig
import com.agriquest.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class PlantMonitoringBinding(val accountId: String, val plantId: String, val deviceId: String)
data class PlantSensorReading(val telemetry: TelemetryData, val recordedAt: String) {
    fun isFresh(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val normalized = recordedAt.replace(Regex("\\.\\d+(?=Z|[+-])"), "")
        val timestamp = runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(normalized)?.time }.getOrNull() ?: return false
        return nowMillis - timestamp in -30_000L..120_000L
    }
}

/** Uses existing RLS-protected tables/RPCs only. No schema changes or privileged keys. */
class SupabasePlantApi(
    val session: AuthSession,
    private val baseUrl: String = BuildConfig.SUPABASE_URL,
    private val publicKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
) {
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    private val userFilter get() = encode(session.userId)
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    suspend fun loadProgress(plantId: String? = null): AccountPlantProgress {
        val profile = rows("profiles?id=eq.$userFilter&select=exp,level,streak_days,last_reset_date,quiz_completed_date").optJSONObject(0)
            ?: throw IOException("Your account profile is not available yet. Please try logging in again.")
        val plantFilter = plantId?.let { "&id=eq.${encode(it)}" }.orEmpty()
        val plant = rows("plants?owner_id=eq.$userFilter$plantFilter&select=id,name,sowing_date,growth_stage&order=created_at.asc&limit=1").optJSONObject(0)
            ?: throw IOException("Your account has no plant available. Contact support.")
        val date = today()
        val quests = rows("quest_progress?user_id=eq.$userFilter&quest_date=eq.$date&select=quest_id,exp_awarded")
        val claimed = mutableSetOf<String>()
        var dailyXp = 0
        for (i in 0 until quests.length()) {
            val item = quests.getJSONObject(i)
            val id = item.getString("quest_id")
            if (!id.startsWith("_")) claimed.add(id)
            dailyXp += item.optInt("exp_awarded").coerceAtLeast(0)
        }
        val notes = rows("journal_entries?owner_id=eq.$userFilter&plant_id=eq.${encode(plant.getString("id"))}&select=id,note,category,created_at&order=created_at.desc")
        val journal = (0 until notes.length()).map { notes.getJSONObject(it).let { note ->
            JournalEntry("remote_${note.getLong("id")}", note.getString("created_at").take(10), note.getString("note"),
                runCatching { JournalCategory.valueOf(note.getString("category")) }.getOrDefault(JournalCategory.GENERAL))
        } }
        return AccountPlantProgress(plant.getString("id"), plant.getString("name"), plant.getString("sowing_date"),
            profile.getInt("exp").coerceAtLeast(0), profile.getInt("level").coerceAtLeast(0),
            PlantGrowthStage.fromLevel(maxOf(profile.getInt("level"), PlantGrowthStage.fromName(plant.optString("growth_stage")).stageLevel)), profile.getInt("streak_days").coerceAtLeast(0),
            profile.nullableString("last_reset_date"), profile.nullableString("quiz_completed_date"), date,
            dailyXp.coerceAtMost(PlantGrowthStage.DAILY_XP_LIMIT), claimed, journal)
    }

    suspend fun saveProgress(progress: AccountPlantProgress) {
        // Recheck ownership before applying a queued snapshot. RLS also enforces this on every request.
        val owner = rows("plants?id=eq.${encode(progress.plantId)}&owner_id=eq.$userFilter&select=id")
        if (owner.length() != 1) throw IOException("This plant does not belong to the current account.")
        val remote = rows("profiles?id=eq.$userFilter&select=exp").optJSONObject(0)
            ?: throw IOException("Account profile not found.")
        val profile = JSONObject().put("exp", maxOf(progress.totalXp, remote.getInt("exp")))
            .put("level", progress.level).put("streak_days", progress.streakDays)
            .put("last_reset_date", progress.lastResetDate ?: JSONObject.NULL)
            .put("quiz_completed_date", progress.quizCompletedDate ?: JSONObject.NULL)
        request("profiles?id=eq.$userFilter", "PATCH", profile.toString())
        request("plants?id=eq.${encode(progress.plantId)}&owner_id=eq.$userFilter", "PATCH",
            JSONObject().put("sowing_date", progress.sowingDate).put("growth_stage", progress.stage.name).toString())

        val claims = JSONArray()
        var allocated = 0
        progress.claimedQuestIds.sorted().forEach { id ->
            val reward = (progress.dailyXp - allocated).coerceIn(0, 30)
            allocated += reward
            claims.put(claim(id, progress.xpDate, reward))
        }
        if (progress.quizCompletedDate == progress.xpDate) {
            val reward = (progress.dailyXp - allocated).coerceIn(0, 10)
            allocated += reward
            claims.put(claim("_daily_quiz", progress.xpDate, reward))
        }
        if (progress.dailyXp > allocated) claims.put(claim("_care_bonus", progress.xpDate, progress.dailyXp - allocated))
        if (claims.length() > 0) request("quest_progress?on_conflict=user_id,quest_id,quest_date", "POST", claims.toString(), true)
        // Identity columns remain server-generated. A timestamp lookup makes ordinary retries idempotent.
        progress.journal.filterNot { it.id.startsWith("remote_") }.forEach { entry ->
            val millis = entry.id.toLongOrNull() ?: return@forEach
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(millis))
            val exists = rows("journal_entries?owner_id=eq.$userFilter&plant_id=eq.${encode(progress.plantId)}&created_at=eq.${encode(timestamp)}&select=id")
            if (exists.length() == 0) request("journal_entries", "POST", JSONObject()
                .put("plant_id", progress.plantId).put("owner_id", session.userId).put("note", entry.note)
                .put("category", entry.category.name).put("created_at", timestamp).toString())
        }
    }

    private fun claim(id: String, date: String, xp: Int) = JSONObject().put("user_id", session.userId)
        .put("quest_id", id).put("quest_date", date).put("exp_awarded", xp)

    suspend fun listPlants(): List<OwnedPlant> {
        val data = rows("plants?owner_id=eq.$userFilter&select=id,name&order=created_at.asc")
        return (0 until data.length()).map { data.getJSONObject(it).let { plant -> OwnedPlant(plant.getString("id"), plant.getString("name")) } }
    }

    suspend fun listDevices(): List<OwnedEsp32Device> {
        val data = rows("devices?owner_id=eq.$userFilter&select=id,plant_id&order=created_at.asc")
        return (0 until data.length()).map { data.getJSONObject(it).let { device -> OwnedEsp32Device(device.getString("id"), device.nullableString("plant_id")) } }
    }

    suspend fun recordAccountLogin(): Int = request("rpc/record_account_login", "POST", "{}").trim().toInt().coerceIn(0, 7)

    suspend fun aiLoginDays(): Int = request("rpc/ai_login_days", "POST", "{}").trim().toInt().coerceIn(0, 7)

    suspend fun pairDevice(plantId: String, proof: DevicePairingProof): PlantMonitoringBinding {
        val result = request("rpc/pair_device", "POST", JSONObject().put("p_plant_id", plantId)
            .put("p_device_id", proof.deviceId).put("p_pairing_code", proof.pairingCode).toString())
        if (JSONArray("[$result]").optString(0) != proof.deviceId) throw IOException("Device ownership was not confirmed.")
        // Read back through RLS before showing a successful connection.
        val owned = listDevices().singleOrNull { it.deviceId == proof.deviceId && it.plantId == plantId }
            ?: throw IOException("Device assignment could not be verified.")
        return PlantMonitoringBinding(session.userId, plantId, owned.deviceId)
    }

    suspend fun unpairDevice(deviceId: String) {
        request("rpc/unpair_device", "POST", JSONObject().put("p_device_id", deviceId).toString())
    }

    suspend fun monitoringBinding(plantId: String, deviceId: String? = null): PlantMonitoringBinding? {
        if (deviceId != null) {
            val device = listDevices().singleOrNull { it.deviceId == deviceId && it.plantId == plantId } ?: return null
            return PlantMonitoringBinding(session.userId, plantId, device.deviceId)
        }
        val device = rows("devices?owner_id=eq.$userFilter&plant_id=eq.${encode(plantId)}&select=id&order=created_at.asc&limit=1").optJSONObject(0)
            ?: return null
        return PlantMonitoringBinding(session.userId, plantId, device.getString("id"))
    }

    suspend fun latestReading(binding: PlantMonitoringBinding): PlantSensorReading? {
        require(binding.accountId == session.userId)
        val data = rows("sensor_readings?plant_id=eq.${encode(binding.plantId)}&device_id=eq.${encode(binding.deviceId)}&select=soil_moisture,air_temp,humidity,light_lux,recorded_at&order=recorded_at.desc&limit=1").optJSONObject(0) ?: return null
        if (listOf("soil_moisture", "air_temp", "humidity", "light_lux").any { data.isNull(it) }) return null
        val telemetry = TelemetryData(data.getDouble("soil_moisture").toFloat(), data.getDouble("air_temp").toFloat(),
            data.getDouble("humidity").toFloat(), data.getDouble("light_lux").toFloat())
        if (!listOf(telemetry.soilMoisture, telemetry.airTemperature, telemetry.relativeHumidity, telemetry.lightIntensity).all(Float::isFinite)) return null
        return PlantSensorReading(telemetry, data.getString("recorded_at"))
    }

    /** Sends through the existing safety-checked RPC; firmware later consumes get_pending_commands. */
    suspend fun requestCommand(binding: PlantMonitoringBinding, actuator: String, seconds: Int) {
        require(binding.accountId == session.userId)
        val raw = request("rpc/request_command", "POST", JSONObject().put("p_plant_id", binding.plantId)
            .put("p_actuator", actuator).put("p_duration_s", seconds).toString())
        val command = if (raw.startsWith("[")) JSONArray(raw).getJSONObject(0) else JSONObject(raw)
        if (command.optString("status") != "pending") throw IOException(command.optString("reason", "Device command was rejected."))
    }

    private suspend fun rows(path: String) = JSONArray(request(path))
    fun cancelRequests() { client.dispatcher.cancelAll() }
    private fun JSONObject.nullableString(key: String) = if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private suspend fun request(path: String, method: String = "GET", body: String? = null, merge: Boolean = false): String = withContext(Dispatchers.IO) {
        require(baseUrl.startsWith("https://") && publicKey.isNotBlank())
        val request = Request.Builder().url(baseUrl.trimEnd('/') + "/rest/v1/" + path)
            .header("apikey", publicKey).header("Authorization", "Bearer ${session.accessToken}")
            .header("Prefer", if (merge) "resolution=merge-duplicates,return=minimal" else "return=minimal")
            .method(method, body?.toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                if (path.startsWith("rpc/pair_device") || path.startsWith("rpc/unpair_device")) {
                    throw IOException(when (response.code) {
                        401 -> "The pairing proof expired or was already used. Press the device pairing button again."
                        403 -> "This device or plant is unavailable to your account."
                        else -> "Secure pairing failed. Verify the device pairing mode and try again."
                    })
                }
                throw IOException("Account sync failed (HTTP ${response.code}). Your changes remain saved on this device.")
            }
            response.body?.string().orEmpty()
        }
    }
}
