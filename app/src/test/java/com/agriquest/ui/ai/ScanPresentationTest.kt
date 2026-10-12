package com.agriquest.ui.ai

import com.agriquest.R
import com.agriquest.domain.model.AiScanResult
import org.junit.Assert.*
import org.junit.Test

class ScanPresentationTest {
    @Test fun healthyResultUsesHealthyArtworkAndActions() {
        val details = ScanPresentation.from(AiScanResult("1", "Healthy Leaf", "", "97%", true))
        assertTrue(details.healthy)
        assertEquals(R.drawable.healthy_leaf, details.artwork)
        assertTrue(details.subtitle.contains("None"))
    }

    @Test fun earlyBlightUsesModerateWarningPresentation() {
        val details = ScanPresentation.from(AiScanResult("2", "EARLY BLIGHT", "", "84%", false))
        assertFalse(details.healthy)
        assertTrue(details.subtitle.contains("Moderate"))
        assertEquals(R.drawable.early_blight_leaf, details.artwork)
    }

    @Test fun bacterialSpotHasItsOwnPresentation() {
        val details = ScanPresentation.from(AiScanResult("3", "Bacterial Spot", "", "91%", false))
        assertTrue(details.subtitle.contains("High"))
        assertEquals(R.drawable.bacterial_spot_leaf, details.artwork)
        assertTrue(details.actions.any { it.contains("Isolate") })
    }

    @Test fun unknownResultKeepsTheReportedDescription() {
        val scan = AiScanResult("4", "Other condition", "Inspect this leaf", "70%", false)
        val details = ScanPresentation.from(scan)
        assertEquals(scan.title, details.title)
        assertEquals(scan.subtext, details.findings)
        assertNull(details.artwork)
        assertFalse(details.healthy)
    }
}
