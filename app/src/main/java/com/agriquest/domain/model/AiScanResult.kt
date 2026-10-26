package com.agriquest.domain.model

/**
 * One leaf scan from the AI Leaf Detector, shown on the result screen and in Recent Scans.
 *
 * @property title the diagnosis, e.g. "Early Blight" (see ScanPresentation for all nine).
 * @property subtext when the scan was taken, e.g. "Scanned Oct 25 - 10:14 PM".
 * @property confidence the model's confidence as text, e.g. "89%".
 * @property imagePath the uploaded (shrunk) photo in the app's cache; null for previews.
 * @property lowConfidence true when the model is under 50% sure; the app suggests a clearer photo.
 * @property pathogen the organism behind the disease, from the server; null when healthy.
 */
data class AiScanResult(
    val id: String,
    val title: String,
    val subtext: String,
    val confidence: String,
    val isHealthy: Boolean = true,
    val imagePath: String? = null,
    val lowConfidence: Boolean = false,
    val pathogen: String? = null
) {
    companion object {
        /** Recent Scans starts empty; entries come from real scans only. */
        val DEFAULT_SCANS: List<AiScanResult> = emptyList()
    }
}
