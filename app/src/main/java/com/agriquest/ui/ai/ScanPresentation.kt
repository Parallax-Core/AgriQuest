package com.agriquest.ui.ai

import androidx.compose.ui.graphics.Color
import com.agriquest.R
import com.agriquest.domain.model.AiScanResult

/** Presentation variants are derived from the actual result, never from the telemetry. */
data class ScanPresentation(val title: String, val healthy: Boolean, val subtitle: String,
    val findings: String, val actions: List<String>, val color: Color, val chipColor: Color,
    val artwork: Int?) {
    companion object {
        fun from(scan: AiScanResult): ScanPresentation = when {
            scan.isHealthy -> ScanPresentation(scan.title, true, "No disease detected · Severity: None",
                "Leaf color, texture and vein structure look normal. No spots, curling or discoloration detected.",
                listOf("Keep your current watering schedule.", "Check again in 3–4 days.", "Maintain good airflow around the plant."),
                Color(0xFF32651D), Color(0xFFE3EBB0), R.drawable.healthy_leaf)
            scan.title.contains("early blight", ignoreCase = true) -> ScanPresentation(scan.title, false,
                "Alternaria solani · Severity: Moderate",
                "Dark brown spots with ring patterns and yellow halos on lower leaves.",
                listOf("Remove and bin affected lower leaves.", "Water at the soil, not on the foliage.", "Monitor the plant and keep good airflow."),
                Color(0xFFBC790F), Color(0xFFFFE3AE), R.drawable.early_blight_leaf)
            scan.title.contains("bacterial spot", ignoreCase = true) -> ScanPresentation(scan.title, false,
                "Xanthomonas · Severity: High",
                "Small, dark, water-soaked spots with yellow edges. Spreads quickly through splashing water and wet leaves.",
                listOf("Isolate the plant and remove infected leaves.", "Avoid overhead watering and handling when wet.", "Improve airflow and monitor nearby plants."),
                Color(0xFFB53220), Color(0xFFFFD6CB), R.drawable.bacterial_spot_leaf)
            else -> ScanPresentation(scan.title, false, "Review this result", scan.subtext,
                listOf("Inspect the affected leaves.", "Record the result and monitor the plant."),
                Color(0xFFBC790F), Color(0xFFFFE3AE), null)
        }
    }
}