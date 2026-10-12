package com.agriquest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.agriquest.R
import com.agriquest.domain.model.PlantGrowthStage

/** The supplied timeline art already includes the stage markers, connectors and day ranges. */
@Composable
fun PlantStagesTimeline(stage: PlantGrowthStage) {
    val artwork = listOf(R.drawable.germination, R.drawable.seedling, R.drawable.vegetative,
        R.drawable.flowering, R.drawable.fruiting, R.drawable.harvest)
    Image(painterResource(artwork[stage.stageLevel]),
        contentDescription = "Tomato stages: ${stage.stageTitle}",
        modifier = Modifier.fillMaxWidth().aspectRatio(363f / 73f), contentScale = ContentScale.Fit)
}