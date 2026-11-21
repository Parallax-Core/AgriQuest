package com.agriquest.data.remote

import android.util.Base64
import com.agriquest.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client for the `assess-leaf` Supabase Edge Function (supabase/functions/assess-leaf).
 *
 * The function forwards the leaf photo to the EfficientNet-B0 model on Render, asks Gemini for care
 * guidance from the assessed condition only, and saves the result to the player's AI scan history
 * (`leaf_scans`). The model and Gemini keys stay on the server, as the paper requires.
 *
 * - `GET`  wakes the Render model.
 * - `POST` multipart: "file" (JPEG) + "plant_id".
 */
class AssessLeafApi(
    private val session: AuthSession,
    private val baseUrl: String = BuildConfig.SUPABASE_URL,
    private val publicKey: String = BuildConfig.SUPABASE_ANON_KEY,
) {
    /** The model's answer plus Gemini's guidance. [careActions] is null when guidance was unavailable. */
    data class Assessment(
        val prediction: LeafDetectorApi.Prediction,
        val careActions: List<String>?,
        val scanId: String?,
    )

    /** The Edge Function isn't deployed yet (HTTP 404); the caller can fall back to Render directly. */
    class NotDeployedException : IOException("The assess-leaf function is not deployed.")

    /** Wakes the Render model through the function. Never throws except [NotDeployedException]. */
    suspend fun wake(): Boolean = withContext(Dispatchers.IO) {
        val connection = try { open("GET") } catch (e: IOException) { return@withContext false }
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> true
                HttpURLConnection.HTTP_NOT_FOUND -> throw NotDeployedException()
                else -> false
            }
        } catch (e: NotDeployedException) {
            throw e
        } catch (e: IOException) {
            false
        } finally {
            connection.disconnect()
        }
    }

    suspend fun assess(jpeg: ByteArray, plantId: String): Assessment = withContext(Dispatchers.IO) {
        val boundary = "AgriQuestBoundary${System.currentTimeMillis()}"
        val head = ("--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"plant_id\"\r\n\r\n$plantId\r\n" +
            "--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"leaf.jpg\"\r\n" +
            "Content-Type: image/jpeg\r\n\r\n").toByteArray(Charsets.UTF_8)
        val tail = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)

        val connection = open("POST")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setFixedLengthStreamingMode(head.size + jpeg.size + tail.size)
            connection.outputStream.use { out ->
                out.write(head)
                out.write(jpeg)
                out.write(tail)
            }
            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_NOT_FOUND) throw NotDeployedException()
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw LeafDetectorApi.ServerException(status, errorMessage(body, status))
            parse(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun open(method: String): HttpURLConnection =
        (URL(baseUrl.trimEnd('/') + "/functions/v1/assess-leaf").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("apikey", publicKey)
            setRequestProperty("Authorization", "Bearer ${session.accessToken}")
        }

    private fun parse(body: String): Assessment {
        val json = JSONObject(body)
        val confidence = json.getDouble("confidence").toFloat()
        val prediction = LeafDetectorApi.Prediction(
            label = json.getString("label"),
            classId = json.optString("classId"),
            pathogen = if (json.isNull("pathogen")) null else json.optString("pathogen"),
            confidence = confidence,
            confidenceText = json.optString("confidenceText").ifBlank { "${Math.round(confidence * 100)}%" },
            isHealthy = json.getBoolean("isHealthy"),
            lowConfidence = json.optBoolean("lowConfidence", confidence < 0.5f),
            heatmapPng = json.optString("heatmapPng").takeIf { it.isNotBlank() && it != "null" }?.let {
                runCatching { Base64.decode(it, Base64.DEFAULT) }.getOrNull()
            },
        )
        val actions = json.optJSONArray("careGuidance")?.let { array ->
            (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
        }?.takeIf { it.isNotEmpty() }
        val scanId = if (json.isNull("scanId")) null else json.optString("scanId").takeIf { it.isNotBlank() }
        return Assessment(prediction, actions, scanId)
    }

    /** The function answers errors as {"code": "...", "message": "..."}. */
    private fun errorMessage(body: String, status: Int): String =
        runCatching { JSONObject(body).optString("message") }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: "AI service error $status"

    private companion object {
        const val CONNECT_TIMEOUT_MS = 30_000
        // Render's cold start (~1 min) plus Gemini.
        const val READ_TIMEOUT_MS = 120_000
    }
}
