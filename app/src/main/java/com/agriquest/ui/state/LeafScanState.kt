package com.agriquest.ui.state

import com.agriquest.domain.model.AiScanResult

/**
 * State of the AI leaf scanner (AI Diagnostics tab), kept apart from [AgriQuestUiState]
 * because it changes during a single scan.
 *
 * @property isScanning a photo is being prepared or uploaded.
 * @property serverAwake the Render server has answered during this session.
 * @property isSlow the scan has taken over a few seconds, most likely because the server is waking up.
 * @property error a message to show when the last scan failed; null otherwise.
 * @property latestResult the newest successful scan, shown as soon as it arrives.
 */
data class LeafScanState(
    val isScanning: Boolean = false,
    val serverAwake: Boolean = false,
    val isSlow: Boolean = false,
    val error: String? = null,
    val latestResult: AiScanResult? = null
)
