package com.agriquest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agriquest.R
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.ui.theme.AgriQuestTheme

/** Displays the imported tomato image for the current growth stage. */
@Composable
fun PlantAvatarCanvas(
    modifier: Modifier = Modifier,
    growthStage: PlantGrowthStage = PlantGrowthStage.GERMINATION,
    size: Dp = 140.dp
) {
    val imageRes = when (growthStage) {
        PlantGrowthStage.GERMINATION -> R.drawable.tomato_stages_germination
        PlantGrowthStage.SEEDLING_SPROUT -> R.drawable.tomato_stages_seedling
        PlantGrowthStage.VEGETATIVE_LEAFY -> R.drawable.tomato_stages_vegetative
        PlantGrowthStage.FLOWERING -> R.drawable.tomato_stages_flowering
        PlantGrowthStage.FRUITING_HARVEST -> R.drawable.tomato_stages_fruit
        PlantGrowthStage.RIPENING -> R.drawable.tomato_stages_ripening
    }
    Image(
        painter = painterResource(id = imageRes),
        contentDescription = "Tomato plant: ${growthStage.stageTitle}",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC, widthDp = 330, heightDp = 270)
@Composable
fun PlantAvatarCanvasStagesPreview() {
    AgriQuestTheme {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlantGrowthStage.entries.chunked(3).forEach { stages ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    stages.forEach { stage ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PlantAvatarCanvas(growthStage = stage, size = 90.dp)
                        }
                    }
                }
            }
        }
    }
}
