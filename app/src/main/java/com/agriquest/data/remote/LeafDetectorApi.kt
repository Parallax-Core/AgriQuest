package com.agriquest.data.remote

import com.agriquest.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client for the AgriQuest Leaf Detector server on Render
 * (repo: Ritsmundz/AgriQuest-AI-Server).
 *
 * - `GET /health` wakes the server. Render's free plan sleeps after 15 idle minutes
 *   and takes about a minute to start again.
 * - `POST /predict` uploads one JPEG (multipart field "file") and returns the diagnosis.
 *
 * Uses Android's built-in HttpURLConnection and org.json, so no extra libraries are needed.
 */
class LeafDetectorApi(
    private val baseUrl: String = BuildConfig.AI_SERVER_URL,
    private val apiKey: String = BuildConfig.AI_API_KEY,
) {

    /** One `/predict` answer. [label] matches the names ScanPresentation knows. */
    data class Prediction(
        val label: String,
        val classId: String,
        val pathogen: String?,
        val confidence: Float,
        val confidenceText: String,
        val isHealthy: Boolean,
        val lowConfidence: Boolean,
    )

    /** The server answered with an error status (400 bad image, 401 wrong key, 413 too big, 5xx). */
    class ServerException(val statusCode: Int, message: String) : IOException(message)

    /** Pings `/health`; returns true once the server answers. Never throws. */
    suspend fun wake(): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = open("/health", "GET")
            try {
                connection.responseCode == HttpURLConnection.HTTP_OK
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Uploads [jpeg] and returns the model's answer.
     * Retries once if Render answers 502/503/504 while the server is still starting.
     */
    suspend fun predict(jpeg: ByteArray): Prediction {
        return try {
            upload(jpeg)
        } catch (e: ServerException) {
            if (e.statusCode !in 502..504) throw e
            delay(3_000L)
            upload(jpeg)
        }
    }

    private suspend fun upload(jpeg: ByteArray): Prediction = withContext(Dispatchers.IO) {
        val boundary = "AgriQuestBoundary${System.currentTimeMillis()}"
        val head = ("--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"leaf.jpg\"\r\n" +
            "Content-Type: image/jpeg\r\n\r\n").toByteArray(Charsets.UTF_8)
        val tail = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)

        val connection = open("/predict", "POST")
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
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw ServerException(status, errorDetail(body, status))
            parse(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun open(path: String, method: String): HttpURLConnection =
        (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            if (apiKey.isNotBlank()) setRequestProperty("X-API-Key", apiKey)
        }

    private fun parse(body: String): Prediction {
        val json = JSONObject(body)
        val confidence = json.getDouble("confidence").toFloat()
        return Prediction(
            label = json.getString("label"),
            classId = json.optString("classId"),
            pathogen = if (json.isNull("pathogen")) null else json.optString("pathogen"),
            confidence = confidence,
            confidenceText = json.optString("confidenceText").ifBlank { "${Math.round(confidence * 100)}%" },
            isHealthy = json.getBoolean("isHealthy"),
            lowConfidence = json.optBoolean("lowConfidence", confidence < 0.5f),
        )
    }

    /** FastAPI errors look like {"detail": "..."}; validation errors carry a list instead. */
    private fun errorDetail(body: String, status: Int): String {
        val detail = try {
            JSONObject(body).opt("detail") as? String
        } catch (e: Exception) {
            null
        }
        return detail?.takeIf { it.isNotBlank() } ?: "Server error $status"
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 30_000
        // Long enough to cover Render's ~1 minute cold start.
        const val READ_TIMEOUT_MS = 90_000
    }
}
