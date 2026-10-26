package com.agriquest.ui.ai

import androidx.compose.ui.graphics.Color
import com.agriquest.R
import com.agriquest.domain.model.AiScanResult

/**
 * How each Leaf Detector result is shown: subtitle, what was found, what to do, colours and
 * fallback artwork. Covers all nine classes the model knows. Derived from the scan result only,
 * never from telemetry. "Risk" is how serious the disease usually is, not something the model measures.
 */
data class ScanPresentation(val title: String, val healthy: Boolean, val subtitle: String,
    val findings: String, val actions: List<String>, val color: Color, val chipColor: Color,
    val artwork: Int?) {
    companion object {
        private val GREEN = Color(0xFF32651D)
        private val GREEN_CHIP = Color(0xFFE3EBB0)
        private val AMBER = Color(0xFFBC790F)
        private val AMBER_CHIP = Color(0xFFFFE3AE)
        private val RED = Color(0xFFB53220)
        private val RED_CHIP = Color(0xFFFFD6CB)

        fun from(scan: AiScanResult): ScanPresentation {
            val title = scan.title
            fun named(text: String) = title.contains(text, ignoreCase = true)
            return when {
                scan.isHealthy -> ScanPresentation(title, true, "No disease detected",
                    "No signs of the eight tomato leaf diseases this model checks for.",
                    listOf("Keep your current watering schedule.", "Check again in 3–4 days.", "Maintain good airflow around the plant."),
                    GREEN, GREEN_CHIP, R.drawable.healthy_leaf)
                named("early blight") -> ScanPresentation(title, false,
                    "Alternaria solani · Risk: Moderate",
                    "Dark brown spots with ring patterns and yellow halos, usually starting on lower leaves.",
                    listOf("Remove and bin affected lower leaves.", "Water at the soil, not on the foliage.", "Monitor the plant and keep good airflow."),
                    AMBER, AMBER_CHIP, R.drawable.early_blight_leaf)
                named("bacterial spot") -> ScanPresentation(title, false,
                    "Xanthomonas · Risk: High",
                    "Small, dark, water-soaked spots with yellow edges. Spreads quickly through splashing water and wet leaves.",
                    listOf("Isolate the plant and remove infected leaves.", "Avoid overhead watering and handling when wet.", "Improve airflow and monitor nearby plants."),
                    RED, RED_CHIP, R.drawable.bacterial_spot_leaf)
                named("late blight") -> ScanPresentation(title, false,
                    "Phytophthora infestans · Risk: Very high",
                    "Large greasy grey-green to brown patches, sometimes with white fuzz underneath in damp weather. Spreads very fast when it is cool and wet.",
                    listOf("Remove and bag infected leaves right away; don't compost them.", "Keep leaves dry: water at the base in the morning.", "Check nearby tomato and potato plants daily, and ask your local agriculturist about treatment."),
                    RED, RED_CHIP, null)
                named("septoria") -> ScanPresentation(title, false,
                    "Septoria lycopersici · Risk: Moderate",
                    "Many small round spots with dark borders and grey or tan centres, often with tiny black dots, starting on lower leaves.",
                    listOf("Remove spotted lower leaves and clean your tools afterwards.", "Mulch the soil and water at the base to stop splashing.", "Space and prune plants for better airflow."),
                    AMBER, AMBER_CHIP, null)
                named("leaf mold") -> ScanPresentation(title, false,
                    "Passalora fulva · Risk: Moderate",
                    "Pale green or yellow patches on top of the leaf with olive-green to brown velvety mould underneath. Thrives in high humidity.",
                    listOf("Lower humidity: ventilate and avoid crowding plants.", "Remove affected leaves.", "Water early in the day, at the soil."),
                    AMBER, AMBER_CHIP, null)
                named("spider mite") -> ScanPresentation(title, false,
                    "Two-spotted spider mite · Risk: Moderate",
                    "Tiny yellow or white speckles on the leaves, sometimes with fine webbing underneath. Worse in hot, dry weather.",
                    listOf("Spray the undersides of leaves with water to knock mites off.", "Remove heavily infested leaves.", "Keep the plant well watered; consider insecticidal soap or neem oil."),
                    AMBER, AMBER_CHIP, null)
                named("yellow leaf curl") -> ScanPresentation(title, false,
                    "Tomato yellow leaf curl virus · Risk: High",
                    "Young leaves curl upwards, yellow at the edges and stay small; the plant looks stunted. Spread by whiteflies.",
                    listOf("There is no cure: remove and bag infected plants to protect the others.", "Control whiteflies with yellow sticky traps or fine netting.", "Clear weeds nearby that host whiteflies."),
                    RED, RED_CHIP, null)
                named("mosaic") -> ScanPresentation(title, false,
                    "Tomato mosaic virus · Risk: High",
                    "Light and dark green mottled patches, sometimes with twisted or narrow, fern-like leaves.",
                    listOf("There is no cure: remove and bag infected plants.", "Wash hands and disinfect tools after handling plants; it spreads by touch.", "Don't smoke or handle tobacco near the plants."),
                    RED, RED_CHIP, null)
                else -> ScanPresentation(title, false, "Review this result", scan.subtext,
                    listOf("Inspect the affected leaves.", "Record the result and monitor the plant."),
                    AMBER, AMBER_CHIP, null)
            }
        }
    }
}
