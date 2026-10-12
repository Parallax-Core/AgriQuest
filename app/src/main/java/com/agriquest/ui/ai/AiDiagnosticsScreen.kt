package com.agriquest.ui.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.agriquest.R
import com.agriquest.domain.model.AiScanResult
import com.agriquest.ui.components.*
import com.agriquest.ui.state.AgriQuestUiState
import com.agriquest.ui.theme.*

@Composable
fun AiDiagnosticsScreenContent(modifier: Modifier = Modifier, uiState: AgriQuestUiState = AgriQuestUiState(),
    selectedTab: Int = 3, onTabSelected: (Int) -> Unit = {}, onPerformScan: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }, onAddJournalEntry: (String) -> Unit = {},
    initialResult: AiScanResult? = null) {
    var result by remember { mutableStateOf(initialResult) }
    var camera by remember { mutableStateOf(false) }
    var previousScanId by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val locked = uiState.plant.streakDays < uiState.plant.targetStreakDays
    LaunchedEffect(uiState.recentScans.firstOrNull()?.id, pending) {
        val newest = uiState.recentScans.firstOrNull()
        if (pending && newest != null && newest.id != previousScanId) {
            result = newest; pending = false; camera = false; saved = false
        }
    }
    AppScreen(uiState, selectedTab, onTabSelected, snackbarHostState, modifier) {
        PixelPageHeader("AI Agro-Assistant", "Diagnostics", uiState)
        val scan = result
        if (scan == null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Tomato model · Demo", style = MaterialTheme.typography.labelSmall, color = AppAccentBackground)
            }
            AppCard(tone = CardTone.ACCENT) {
                Column(Modifier.fillMaxWidth().background(Color(0xFF315B1D)).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.CameraAlt, null, Modifier.size(36.dp), tint = PixelPaleGreen)
                    Text("SCAN TOMATO LEAF", style = MaterialTheme.typography.titleMedium, color = AppCardBackground)
                    Text(if (locked) "Unlock at a ${uiState.plant.targetStreakDays}-day care streak (${uiState.plant.streakDays}/${uiState.plant.targetStreakDays})."
                        else "Point your camera at a leaf for a scan preview.", style = MaterialTheme.typography.bodySmall, color = AppCardBackground)
                    PixelButton(if (locked) "Scan Locked" else "Open Camera", { camera = true },
                        color = Color(0xFFAF852D), enabled = !locked && !pending)
                }
            }
            PixelPanel {
                PixelHeading("Current IoT Conditions")
                TelemetryGrid(telemetry = uiState.telemetry, showTitle = false, compact = true, embedded = true)
            }
            PixelPanel {
                PixelHeading("Recent Scans")
                if (uiState.recentScans.isEmpty()) Text("No scans yet.", style = MaterialTheme.typography.bodySmall)
                uiState.recentScans.take(5).forEach { history ->
                    ScanHistoryRow(history) { result = history; saved = false }
                }
            }
        } else {
            val details = ScanPresentation.from(scan)
            LeafScanPreview(details)
            PixelPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (scan.isHealthy) Icons.Rounded.CheckCircle else Icons.Rounded.Warning, null,
                        Modifier.size(32.dp), tint = details.color)
                    Column(Modifier.weight(1f)) {
                        Text(scan.title.uppercase(), style = MaterialTheme.typography.titleMedium, color = details.color)
                        Text(details.subtitle, style = MaterialTheme.typography.labelSmall, color = AppTextColor)
                    }
                    PixelChip("${scan.confidence} sure", details.chipColor, details.color)
                }
                HorizontalDivider(color = AppCardBorder)
                PixelHeading("What We Found")
                Text(details.findings, style = MaterialTheme.typography.bodySmall, color = AppTextColor)
                PixelHeading("What To Do")
                details.actions.forEach { Text("▪ $it", style = MaterialTheme.typography.bodySmall, color = AppTextColor) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton("Scan Again", { result = null }, Modifier.weight(1f), color = PixelGreen)
                    PixelButton(if (saved) "✓ Saved" else "+ Add to Journal", {
                        onAddJournalEntry("AI scan: ${scan.title} (${scan.confidence}). ${details.findings}")
                        saved = true
                    }, Modifier.weight(1f), color = details.color, enabled = !saved)
                }
            }
            PixelPanel {
                TelemetryGrid(telemetry = uiState.telemetry, showTitle = false, compact = true, embedded = true)
            }
        }
    }
    if (camera) Dialog(onDismissRequest = { if (!pending) camera = false }) {
        PixelPanel {
            PixelHeading("Camera Preview")
            LeafScanPreview(ScanPresentation.from(AiScanResult("preview", "Healthy Leaf", "", "", true)))
            Text("Demo scan preview. This build simulates the diagnosis; it does not analyze a camera image.",
                style = MaterialTheme.typography.bodySmall, color = AppTextColor)
            if (pending) LinearProgressIndicator(Modifier.fillMaxWidth())
            PixelButton(if (pending) "Scanning…" else "Run Demo Scan", {
                previousScanId = uiState.recentScans.firstOrNull()?.id; pending = true; onPerformScan()
            }, Modifier.fillMaxWidth(), enabled = !pending)
            TextButton({ camera = false; pending = false }) { Text("Cancel") }
        }
    }
}

@Composable
private fun ScanHistoryRow(scan: AiScanResult, onClick: () -> Unit) {
    val details = ScanPresentation.from(scan)
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), color = PixelTan,
        shape = RoundedCornerShape(3.dp), border = BorderStroke(1.dp, AppCardBorder)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (scan.isHealthy) Icons.Rounded.CheckCircle else Icons.Rounded.Warning, null, tint = details.color)
            Column(Modifier.weight(1f)) {
                Text(scan.title, style = MaterialTheme.typography.labelLarge, color = AppTextColor)
                Text(scan.subtext, style = MaterialTheme.typography.labelSmall, color = AppTextColor)
            }
            PixelChip("${scan.confidence} sure", details.chipColor, details.color)
        }
    }
}

/** Uses the supplied capture frame and the exact artwork for each diagnosis. */
@Composable
private fun LeafScanPreview(details: ScanPresentation) {
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

@Preview(name = "AI — Bacterial Spot Result", widthDp = 393, heightDp = 852)
@Composable
private fun BacterialSpotResultPreview() {
    AgriQuestTheme { AiDiagnosticsScreenContent(initialResult = AiScanResult("preview", "Bacterial Spot", "", "91%", false)) }
}
