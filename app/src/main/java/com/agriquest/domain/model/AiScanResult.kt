package com.agriquest.domain.model

/**
 * Domain model representing AI Leaf Diagnostic Scan history entries.
 */
data class AiScanResult(
    val id: String,
    val title: String,
    val subtext: String,
    val confidence: String,
    val isHealthy: Boolean = true
) {
    companion object {
        val DEFAULT_SCANS = listOf(
            AiScanResult(
                id = "scan_1",
                title = "Healthy Leaf",
                subtext = "Leaf 4 (middle branch) • Sep 12 - 2:14 PM",
                confidence = "97%",
                isHealthy = true
            ),
            AiScanResult(
                id = "scan_2",
                title = "Early Blight",
                subtext = "Leaf 7 (lower) • Sep 10 - 11:02 AM",
                confidence = "89%",
                isHealthy = false
            )
        )
    }
}
