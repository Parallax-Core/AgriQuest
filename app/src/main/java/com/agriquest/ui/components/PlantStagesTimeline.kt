package com.agriquest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.agriquest.R

/** Illustrated seed-to-harvest timeline, in growth order. */
@Composable
fun PlantStagesTimeline() {
    val illustrations = listOf(
        Triple(R.drawable.germination, "Seed germination", 370f / 73f),
        Triple(R.drawable.seedling, "Seedling", 363f / 73f),
        Triple(R.drawable.vegetative, "Vegetative growth", 363f / 73f),
        Triple(R.drawable.flowering, "Flowering", 363f / 73f),
        Triple(R.drawable.fruiting, "Fruit set and development", 363f / 73f),
        Triple(R.drawable.harvest, "Ripening and harvesting", 363f / 73f)
    )
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        illustrations.forEach { (drawable, description, ratio) ->
            Image(
                painter = painterResource(drawable),
                contentDescription = description,
                modifier = Modifier.fillMaxWidth().aspectRatio(ratio),
                contentScale = ContentScale.Fit
            )
        }
    }
}
