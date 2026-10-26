package com.agriquest.ui.ai

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.agriquest.R
import com.agriquest.domain.model.AiScanResult
import com.agriquest.ui.components.*
import com.agriquest.ui.state.AgriQuestUiState
import com.agriquest.ui.state.LeafScanState
import com.agriquest.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val ScanCardGreen = Color(0xFF315B1D)
private val ScanButtonGold = Color(0xFFAF852D)
private val ScanErrorText = Color(0xFFFFD6CB)
private val PhotoShape = RoundedCornerShape(6.dp)

/**
 * AI Diagnostics tab: photographs a tomato leaf (camera or gallery), sends it to the
 * Leaf Detector server on Render and shows the diagnosis with care advice.
 */
@Composable
fun AiDiagnosticsScreenContent(
    modifier: Modifier = Modifier,
    uiState: AgriQuestUiState = AgriQuestUiState(),
    selectedTab: Int = 3,
    onTabSelected: (Int) -> Unit = {},
    scanState: LeafScanState = LeafScanState(),
    onScanPhoto: (Uri) -> Unit = {},
    onWakeServer: () -> Unit = {},
    onDismissScanError: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onAddJournalEntry: (String) -> Unit = {},
    initialResult: AiScanResult? = null
) {
    val context = LocalContext.current
    var result by remember { mutableStateOf(initialResult) }
    var saved by remember { mutableStateOf(false) }
    // Uris are kept as text so they survive the activity being recreated while the camera app is open.
    var captureUri by rememberSaveable { mutableStateOf<String?>(null) }
    var lastPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var handledResultId by rememberSaveable { mutableStateOf(scanState.latestResult?.id) }

    fun startScan(photo: Uri) {
        lastPhoto = photo.toString()
        onScanPhoto(photo)
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = captureUri?.let(Uri::parse)
        if (taken && uri != null) startScan(uri)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) startScan(uri)
    }

    // Wake the Render server while the user lines up the photo.
    LaunchedEffect(Unit) { onWakeServer() }

    // Show each new scan result once, as soon as it arrives.
    LaunchedEffect(scanState.latestResult?.id) {
        val latest = scanState.latestResult
        if (latest != null && latest.id != handledResultId) {
            handledResultId = latest.id
            result = latest
            saved = false
        }
    }

    AppScreen(uiState, selectedTab, onTabSelected, snackbarHostState, modifier) {
        PixelPageHeader("AI Agro-Assistant", "Diagnostics", uiState)
        val shown = result
        if (shown == null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Tomato leaf model · 9 conditions", style = MaterialTheme.typography.labelSmall, color = AppAccentBackground)
            }
            AppCard(tone = CardTone.ACCENT) {
                Column(Modifier.fillMaxWidth().background(ScanCardGreen).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.CameraAlt, null, Modifier.size(36.dp), tint = PixelPaleGreen)
                    Text("SCAN TOMATO LEAF", style = MaterialTheme.typography.titleMedium, color = AppCardBackground)
                    Text("Take a close, sharp photo of one leaf in good light.",
                        style = MaterialTheme.typography.bodySmall, color = AppCardBackground)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PixelButton("Take Photo", {
                            val uri = newCaptureUri(context)
                            captureUri = uri.toString()
                            try {
                                takePicture.launch(uri)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "No camera app found. Choose a photo from the gallery instead.",
                                    Toast.LENGTH_LONG).show()
                            }
                        }, Modifier.weight(1f), color = ScanButtonGold, enabled = !scanState.isScanning)
                        PixelButton("Gallery", {
                            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }, Modifier.weight(1f), color = ScanButtonGold, enabled = !scanState.isScanning)
                    }
                    if (scanState.isScanning) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PixelPaleGreen,
                            trackColor = AppAccentBackground)
                        Text(
                            when {
                                scanState.isSlow && !scanState.serverAwake ->
                                    "Waking up the AI server… the first scan can take up to a minute."
                                scanState.isSlow -> "Still analysing your leaf…"
                                else -> "Analysing your leaf…"
                            },
                            style = MaterialTheme.typography.bodySmall, color = AppCardBackground
                        )
                    }
                    val error = scanState.error
                    if (error != null && !scanState.isScanning) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Warning, null, Modifier.size(18.dp), tint = ScanErrorText)
                            Text(error, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = ScanErrorText)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val retryPhoto = lastPhoto
                            if (retryPhoto != null) {
                                PixelButton("Try Again", { startScan(Uri.parse(retryPhoto)) }, color = ScanButtonGold)
                            }
                            TextButton(onClick = onDismissScanError) { Text("Dismiss", color = AppCardBackground) }
                        }
                    }
                }
            }
            PixelPanel {
                PixelHeading("Current IoT Conditions")
                TelemetryGrid(telemetry = uiState.telemetry, showTitle = false, compact = true, embedded = true)
            }
            PixelPanel {
                PixelHeading("Recent Scans")
                if (uiState.recentScans.isEmpty()) {
                    Text("No scans yet. Your scans will appear here.", style = MaterialTheme.typography.bodySmall, color = AppTextColor)
                }
                uiState.recentScans.take(5).forEach { history ->
                    ScanHistoryRow(history) { result = history; saved = false }
                }
            }
        } else {
            val details = ScanPresentation.from(shown)
            LeafScanPreview(details, shown.imagePath)
            PixelPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (shown.isHealthy) Icons.Rounded.CheckCircle else Icons.Rounded.Warning, null,
                        Modifier.size(32.dp), tint = details.color)
                    Column(Modifier.weight(1f)) {
                        Text(shown.title.uppercase(), style = MaterialTheme.typography.titleMedium, color = details.color)
                        Text(details.subtitle, style = MaterialTheme.typography.labelSmall, color = AppTextColor)
                    }
                    PixelChip("${shown.confidence} sure", details.chipColor, details.color)
                }
                if (shown.lowConfidence) LowConfidenceNote()
                HorizontalDivider(color = AppCardBorder)
                PixelHeading("What We Found")
                Text(details.findings, style = MaterialTheme.typography.bodySmall, color = AppTextColor)
                PixelHeading("What To Do")
                details.actions.forEach { Text("▪ $it", style = MaterialTheme.typography.bodySmall, color = AppTextColor) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton("Scan Again", { result = null }, Modifier.weight(1f), color = PixelGreen)
                    PixelButton(if (saved) "✓ Saved" else "+ Add to Journal", {
                        onAddJournalEntry("AI scan: ${shown.title} (${shown.confidence}). ${details.findings}")
                        saved = true
                    }, Modifier.weight(1f), color = details.color, enabled = !saved)
                }
            }
            SimilarPhotosPanel(shown.title, shown.isHealthy)
            ReliabilityPanel()
            PixelPanel {
                TelemetryGrid(telemetry = uiState.telemetry, showTitle = false, compact = true, embedded = true)
            }
        }
    }
}

/** A new empty file in the cache for the camera app to write the photo into. */
private fun newCaptureUri(context: Context): Uri {
    val folder = File(context.cacheDir, "leaf_photos").apply { mkdirs() }
    val file = File(folder, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
private fun LowConfidenceNote() {
    Surface(Modifier.fillMaxWidth(), color = Color(0xFFFFE3AE), shape = RoundedCornerShape(3.dp),
        border = BorderStroke(1.dp, AppCardBorder)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Warning, null, Modifier.size(18.dp), tint = Color(0xFFBC790F))
            Text("Not sure about this one. Try a closer, sharper photo of a single leaf in good light.",
                style = MaterialTheme.typography.bodySmall, color = AppTextColor)
        }
    }
}

@Composable
private fun ScanHistoryRow(scan: AiScanResult, onClick: () -> Unit) {
    val details = ScanPresentation.from(scan)
    val thumbnail = rememberScanPhoto(scan.imagePath)
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), color = PixelTan,
        shape = RoundedCornerShape(3.dp), border = BorderStroke(1.dp, AppCardBorder)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (thumbnail is ScanPhoto.Ready) {
                Image(thumbnail.bitmap, contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(3.dp)), contentScale = ContentScale.Crop)
            } else {
                Icon(if (scan.isHealthy) Icons.Rounded.CheckCircle else Icons.Rounded.Warning, null, tint = details.color)
            }
            Column(Modifier.weight(1f)) {
                Text(scan.title, style = MaterialTheme.typography.labelLarge, color = AppTextColor)
                Text(scan.subtext, style = MaterialTheme.typography.labelSmall, color = AppTextColor)
            }
            PixelChip("${scan.confidence} sure", details.chipColor, details.color)
        }
    }
}

/** The user's photo when there is one; otherwise the camera frame with the diagnosis artwork. */
@Composable
private fun LeafScanPreview(details: ScanPresentation, imagePath: String?) {
    when (val photo = rememberScanPhoto(imagePath)) {
        is ScanPhoto.Ready -> Image(photo.bitmap, contentDescription = "Scanned leaf: ${details.title}",
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(PhotoShape)
                .border(2.dp, AppCardBorder, PhotoShape),
            contentScale = ContentScale.Crop)
        ScanPhoto.Loading -> Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(PhotoShape)
            .background(PixelTan).border(2.dp, AppCardBorder, PhotoShape))
        ScanPhoto.Missing -> ArtworkPreview(details)
    }
}

@Composable
private fun ArtworkPreview(details: ScanPresentation) {
    Box(Modifier.fillMaxWidth().aspectRatio(396f / 150f), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.camera_capture), contentDescription = null,
            modifier = Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
        val artwork = details.artwork
        if (artwork != null) Image(painterResource(artwork), contentDescription = details.title,
            modifier = Modifier.fillMaxHeight(.8f).aspectRatio(1f).padding(bottom = 12.dp),
            contentScale = ContentScale.Fit)
        else Icon(Icons.Rounded.CameraAlt, details.title, Modifier.size(48.dp), tint = PixelPaleGreen)
    }
}

private sealed interface ScanPhoto {
    object Loading : ScanPhoto
    object Missing : ScanPhoto
    class Ready(val bitmap: ImageBitmap) : ScanPhoto
}

/** Loads a saved scan photo off the main thread. Missing when there is no path or the cache was cleared. */
@Composable
private fun rememberScanPhoto(path: String?): ScanPhoto {
    val photo by produceState<ScanPhoto>(if (path == null) ScanPhoto.Missing else ScanPhoto.Loading, path) {
        value = if (path == null) {
            ScanPhoto.Missing
        } else {
            withContext<ScanPhoto>(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) ScanPhoto.Ready(bitmap.asImageBitmap()) else ScanPhoto.Missing
            }
        }
    }
    return photo
}

@Preview(name = "AI — Scanner", widthDp = 393, heightDp = 852)
@Composable
private fun ScannerPreview() {
    AgriQuestTheme { AiDiagnosticsScreenContent(scanState = LeafScanState(isScanning = true, isSlow = true)) }
}

@Preview(name = "AI — Healthy Result", widthDp = 393, heightDp = 852)
@Composable
private fun HealthyResultPreview() {
    AgriQuestTheme { AiDiagnosticsScreenContent(initialResult = AiScanResult("preview", "Healthy Leaf", "", "97%")) }
}

@Preview(name = "AI — Early Blight Result", widthDp = 393, heightDp = 852)
@Composable
private fun EarlyBlightResultPreview() {
    AgriQuestTheme { AiDiagnosticsScreenContent(initialResult = AiScanResult("preview", "Early Blight", "", "84%", false)) }
}

@Preview(name = "AI — Late Blight, low confidence", widthDp = 393, heightDp = 852)
@Composable
private fun LateBlightResultPreview() {
    AgriQuestTheme {
        AiDiagnosticsScreenContent(initialResult = AiScanResult("preview", "Late Blight", "", "46%", false, lowConfidence = true))
    }
}
