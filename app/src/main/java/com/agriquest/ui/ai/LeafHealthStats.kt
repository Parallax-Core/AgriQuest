package com.agriquest.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.agriquest.domain.model.AiScanResult
import com.agriquest.ui.components.PixelHeading
import com.agriquest.ui.components.PixelPanel
import com.agriquest.ui.theme.AppTextColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Share of one diagnosis among a group of scans, e.g. Leaf Mold: 2 photos, 22%. */
data class DiagnosisShare(val label: String, val photos: Int, val percent: Int, val healthy: Boolean)

/** One calendar day of scans. */
data class DayScanStats(val dayLabel: String, val photos: Int, val shares: List<DiagnosisShare>)

object LeafHealthStatistics {
    private val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Scan time, read from the id ("scan_<millis>"); null for scans without one. */
    fun takenAt(scan: AiScanResult): Long? = scan.id.removePrefix("scan_").toLongOrNull()

    fun shares(scans: List<AiScanResult>): List<DiagnosisShare> {
        if (scans.isEmpty()) return emptyList()
        return scans.groupBy { it.title }.map { (label, group) ->
            DiagnosisShare(label, group.size, Math.round(group.size * 100f / scans.size), group.first().isHealthy)
        }.sortedWith(compareByDescending<DiagnosisShare> { it.healthy }.thenByDescending { it.photos })
    }

    /** Last [days] days that have scans, newest first. */
    fun byDay(scans: List<AiScanResult>, nowMs: Long = System.currentTimeMillis(), days: Int = 7): List<DayScanStats> {
        val today = dayKey.format(Date(nowMs))
        val yesterday = dayKey.format(Date(nowMs - 86_400_000L))
        val since = nowMs - days * 86_400_000L
        val label = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        return scans.mapNotNull { s -> takenAt(s)?.takeIf { it >= since }?.let { it to s } }
            .groupBy { dayKey.format(Date(it.first)) }
            .toSortedMap(compareByDescending { it })
            .map { (key, items) ->
                val name = when (key) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> label.format(Date(items.first().first))
                }
                DayScanStats(name, items.size, shares(items.map { it.second }))
            }
    }
}

private val HealthyColor = Color(0xFF649B32)
private val DiseaseColors = listOf(Color(0xFFD9822B), Color(0xFFB8432F), Color(0xFF8E5A9B), Color(0xFF3F7CA3), Color(0xFF9C7A2E))

/** "Leaf Health Stats": daily and 7-day breakdown of scan results, e.g. Healthy 78% (7 photos). */
@Composable
fun LeafHealthStatsPanel(scans: List<AiScanResult>) {
    val days = LeafHealthStatistics.byDay(scans)
    PixelPanel {
        PixelHeading("Leaf Health Stats")
        if (days.isEmpty()) {
            Text("Scan leaves to see how healthy your plant is each day.", style = MaterialTheme.typography.bodySmall, color = AppTextColor)
        } else {
        val weekScans = scans.filter { s -> LeafHealthStatistics.takenAt(s)?.let { it >= System.currentTimeMillis() - 7 * 86_400_000L } == true }
        val weekShares = LeafHealthStatistics.shares(weekScans)
        val colors = colorFor(weekShares)
        Text("Last 7 days · ${weekScans.size} photos", style = MaterialTheme.typography.labelLarge, color = AppTextColor)
        ShareBar(weekShares, colors)
        weekShares.forEach { ShareRow(it, colors.getValue(it.label)) }
        days.forEach { day ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${day.dayLabel} · ${day.photos} photo${if (day.photos == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium, color = AppTextColor)
                ShareBar(day.shares, colors)
                Text(day.shares.joinToString(" · ") { "${it.label} ${it.percent}% (${it.photos})" },
                    style = MaterialTheme.typography.labelSmall, color = AppTextColor)
            }
        }
        }
    }
}

private fun colorFor(shares: List<DiagnosisShare>): Map<String, Color> {
    var next = 0
    return shares.associate { it.label to if (it.healthy) HealthyColor else DiseaseColors[next++ % DiseaseColors.size] }
}

@Composable
private fun ShareBar(shares: List<DiagnosisShare>, colors: Map<String, Color>) {
    Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFF3E0A0))) {
        shares.forEach { share ->
            Box(Modifier.weight(share.photos.toFloat().coerceAtLeast(0.01f)).height(12.dp)
                .background(colors[share.label] ?: Color.Gray))
        }
    }
}

@Composable
private fun ShareRow(share: DiagnosisShare, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Text(share.label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = AppTextColor)
        Text("${share.percent}% (${share.photos} photo${if (share.photos == 1) "" else "s"})",
            style = MaterialTheme.typography.labelMedium, color = AppTextColor)
    }
}
