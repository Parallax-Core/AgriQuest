package com.agriquest.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.ui.theme.AppLayout

/** Estimated seed-to-harvest windows, not a diagnosis of actual maturity. */
@Composable
fun PlantStagesTimeline(stage: PlantGrowthStage) {
    Column(verticalArrangement = Arrangement.spacedBy(AppLayout.compactSpacing)) {
        PlantGrowthStage.entries.forEach { item ->
            AppText("${if (item == stage) "Current:" else ""} ${item.displayName}: ${item.timelineText}",
                style = MaterialTheme.typography.bodySmall)
        }
        AppText("Estimate: about 120 days from seed. Variety, light and temperature affect timing. Harvest when fruit is ripe.",
            style = MaterialTheme.typography.bodySmall)
        AppText("Set your sowing date in Plant Details to match your actual plant.",
            style = MaterialTheme.typography.bodySmall)
    }
}
