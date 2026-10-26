package com.agriquest.ui.ai

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agriquest.BuildConfig
import com.agriquest.ui.components.PixelChip
import com.agriquest.ui.components.PixelHeading
import com.agriquest.ui.components.PixelPanel
import com.agriquest.ui.theme.AppCardBorder
import com.agriquest.ui.theme.AppTextColor
import com.agriquest.ui.components.PixelTan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/*
 * Two cards shown under a scan result on the AI Diagnostics screen:
 *   - SimilarPhotosPanel: example photos of the diagnosed disease, for comparison
 *   - ReliabilityPanel:   the model's 5-fold cross-validation and Cohen's kappa results
 * Both read from the Leaf Detector server (GET /reference and GET /reliability), so the photos and
 * numbers can be updated on Render without a new app build.
 */

// ---------------------------------------------------------------- data

internal data class ReliabilityRun(
    val name: String,
    val testedOn: String,
    val accuracy: Double,
    val f1: Double,
    val kappa: Double,
    val kappaLow: Double,
    val kappaHigh: Double,
    val wrong: String,
    val usedByApp: Boolean,
)

internal data class ReliabilityInfo(
    val method: String,
    val kappaMeaning: String,
    val caveat: String,
    val runs: List<ReliabilityRun>,
)

private sealed interface Fetch<out T> {
    object Loading : Fetch<Nothing>
    object Failed : Fetch<Nothing>
    class Ok<T>(val value: T) : Fetch<T>
}

// ---------------------------------------------------------------- server calls

internal object ScanExtrasClient {
    // Only touched from the main thread (produceState), so plain maps are fine.
    private val photoLists = HashMap<String, List<String>>()
    private val bitmaps = HashMap<String, ImageBitmap>()
    private var reliabilityCache: ReliabilityInfo? = null

    private fun open(path: String): HttpURLConnection =
        (URL(BuildConfig.AI_SERVER_URL.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 30_000
            readTimeout = 90_000                      // covers Render's cold start
            if (BuildConfig.AI_API_KEY.isNotBlank()) setRequestProperty("X-API-Key", BuildConfig.AI_API_KEY)
        }

    private suspend fun getBytes(path: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val connection = open(path)
            try {
                if (connection.responseCode in 200..299) connection.inputStream.use { it.readBytes() } else null
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Server paths of the example photos for [label] (as /predict names it), or null if unavailable. */
    suspend fun referencePhotos(label: String): List<String>? {
        photoLists[label]?.let { return it }
        val body = getBytes("/reference?label=" + URLEncoder.encode(label, "UTF-8"))?.toString(Charsets.UTF_8) ?: return null
        val paths = try {
            val array = JSONObject(body).getJSONArray("photos")
            List(array.length()) { array.getString(it) }
        } catch (e: Exception) {
            return null
        }
        photoLists[label] = paths
        return paths
    }

    suspend fun bitmap(path: String): ImageBitmap? {
        bitmaps[path]?.let { return it }
        val bytes = getBytes(path) ?: return null
        val decoded = withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) } ?: return null
        return decoded.asImageBitmap().also { bitmaps[path] = it }
    }

    suspend fun reliability(): ReliabilityInfo? {
        reliabilityCache?.let { return it }
        val body = getBytes("/reliability")?.toString(Charsets.UTF_8) ?: return null
        val info = try {
            val json = JSONObject(body)
            val runs = json.getJSONArray("runs")
            ReliabilityInfo(
                method = json.getString("method"),
                kappaMeaning = json.getString("kappaMeaning"),
                caveat = json.getString("caveat"),
                runs = List(runs.length()) {
                    val r = runs.getJSONObject(it)
                    ReliabilityRun(
                        name = r.getString("name"),
                        testedOn = r.getString("testedOn"),
                        accuracy = r.getDouble("accuracy"),
                        f1 = r.getDouble("f1"),
                        kappa = r.getDouble("kappa"),
                        kappaLow = r.getDouble("kappaLow"),
                        kappaHigh = r.getDouble("kappaHigh"),
                        wrong = r.getString("wrong"),
                        usedByApp = r.optBoolean("usedByApp", false),
                    )
                },
            )
        } catch (e: Exception) {
            return null
        }
        reliabilityCache = info
        return info
    }
}

// ---------------------------------------------------------------- similar photos

private val ThumbShape = RoundedCornerShape(4.dp)

/** Example photos of the diagnosed disease, so the user can compare them with their own leaf. */
@Composable
fun SimilarPhotosPanel(label: String, isHealthy: Boolean) {
    val fetched by produceState<Fetch<List<String>>>(Fetch.Loading, label) {
        val paths = ScanExtrasClient.referencePhotos(label)
        value = if (paths.isNullOrEmpty()) Fetch.Failed else Fetch.Ok(paths)
    }
    PixelPanel {
        PixelHeading(if (isHealthy) "Healthy Leaf Examples" else "Similar Photos")
        when (val state = fetched) {
            Fetch.Loading -> Text("Loading example photos…", style = MaterialTheme.typography.bodySmall, color = AppTextColor)
            Fetch.Failed -> Text("Example photos are not available right now. Check your connection and scan again.",
                style = MaterialTheme.typography.bodySmall, color = AppTextColor)
            is Fetch.Ok -> {
                Text("Compare your leaf with these examples of $label. They are sample photos, not your plant.",
                    style = MaterialTheme.typography.bodySmall, color = AppTextColor)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.value.forEach { ReferenceThumb(it, label) }
                }
            }
        }
    }
}

@Composable
private fun ReferenceThumb(path: String, label: String) {
    val bitmap by produceState<ImageBitmap?>(null, path) { value = ScanExtrasClient.bitmap(path) }
    Box(Modifier.size(110.dp).clip(ThumbShape).background(PixelTan).border(1.dp, AppCardBorder, ThumbShape)) {
        bitmap?.let {
            Image(it, contentDescription = "Example photo: $label", modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop)
        }
    }
}

// ---------------------------------------------------------------- reliability

private fun percent(v: Double) = String.format(Locale.US, "%.2f%%", v * 100)
private fun kappaText(v: Double) = String.format(Locale.US, "%.4f", v)

/** Cross-validation and Cohen's kappa results for the model the app uses, with the other two tests on request. */
@Composable
fun ReliabilityPanel() {
    val fetched by produceState<Fetch<ReliabilityInfo>>(Fetch.Loading) {
        val info = ScanExtrasClient.reliability()
        value = if (info == null || info.runs.isEmpty()) Fetch.Failed else Fetch.Ok(info)
    }
    var expanded by rememberSaveable { mutableStateOf(false) }

    PixelPanel {
        PixelHeading("Model Reliability & Accuracy")
        when (val state = fetched) {
            Fetch.Loading -> Text("Loading reliability results…", style = MaterialTheme.typography.bodySmall, color = AppTextColor)
            Fetch.Failed -> Text("Reliability results are not available right now.",
                style = MaterialTheme.typography.bodySmall, color = AppTextColor)
            is Fetch.Ok -> {
                val info = state.value
                val main = info.runs.firstOrNull { it.usedByApp } ?: info.runs.last()
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PixelChip("Accuracy ${percent(main.accuracy)}")
                    PixelChip("F1 ${percent(main.f1)}")
                    PixelChip("Kappa ${kappaText(main.kappa)}")
                }
                Text(info.kappaMeaning, style = MaterialTheme.typography.bodySmall, color = AppTextColor)
                Text(info.method, style = MaterialTheme.typography.labelSmall, color = AppTextColor)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide details" else "Show all three tests", color = AppTextColor)
                }
                if (expanded) {
                    HorizontalDivider(color = AppCardBorder)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Trained on", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                            color = AppTextColor, fontWeight = FontWeight.Bold)
                        Text("Accuracy", Modifier.width(64.dp), style = MaterialTheme.typography.labelSmall,
                            color = AppTextColor, fontWeight = FontWeight.Bold)
                        Text("Kappa", Modifier.width(56.dp), style = MaterialTheme.typography.labelSmall,
                            color = AppTextColor, fontWeight = FontWeight.Bold)
                    }
                    info.runs.forEach { run ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (run.usedByApp) "${run.name} (this app)" else run.name, Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall, color = AppTextColor)
                            Text(percent(run.accuracy), Modifier.width(64.dp),
                                style = MaterialTheme.typography.bodySmall, color = AppTextColor)
                            Text(kappaText(run.kappa), Modifier.width(56.dp),
                                style = MaterialTheme.typography.bodySmall, color = AppTextColor)
                        }
                        Text("Tested on ${run.testedOn}. Wrong: ${run.wrong}. Kappa range ${kappaText(run.kappaLow)} to ${kappaText(run.kappaHigh)}.",
                            style = MaterialTheme.typography.labelSmall, color = AppTextColor)
                    }
                }
                HorizontalDivider(color = AppCardBorder)
                Text(info.caveat, style = MaterialTheme.typography.labelSmall, color = AppTextColor)
            }
        }
    }
}
