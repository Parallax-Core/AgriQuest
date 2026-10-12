package com.agriquest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agriquest.R
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.ui.components.AppCard
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.AppLayout
import com.agriquest.ui.theme.AppTextColor
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate900

/**
* Plant Stages Stepper Timeline Card for AgriQuest Mobile.
* Displays 6 stage timeline steps with active step highlighting and "View More ➔" action button.
*/
@Composable
fun PlantStagesCard(
    modifier: Modifier = Modifier,
    currentStage: PlantGrowthStage = PlantGrowthStage.GERMINATION,
    onViewMoreClick: () -> Unit = {}
) {
    val activeStageIndex = when (currentStage) {
        PlantGrowthStage.GERMINATION -> 0
        PlantGrowthStage.SEEDLING_SPROUT -> 1 // 2 Seedling (index 1)
        PlantGrowthStage.VEGETATIVE_LEAFY -> 2 // 3 Vegetative (index 2)
        PlantGrowthStage.FLOWERING -> 3 // 4 Flowering (index 3)
        PlantGrowthStage.FRUITING_HARVEST -> 4 // 5 Fruiting (index 4)
        PlantGrowthStage.RIPENING -> 5
    }

    val stageImages = listOf(
        R.drawable.tomato_stages_germination,
        R.drawable.tomato_stages_seedling,
        R.drawable.tomato_stages_vegetative,
        R.drawable.tomato_stages_flowering,
        R.drawable.tomato_stages_fruit,
        R.drawable.tomato_stages_ripening
    )
    val stageNames = listOf("Germination", "Seedling", "Vegetative", "Flowering", "Fruiting", "Ripening")

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(AppLayout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing)
        ) {
            // Header Row: Title `🍃 Plant Stages`
            Text(
                text = "🍃 Plant Stages",
                style = MaterialTheme.typography.titleMedium.copy(color = AppTextColor)
            )

            // Each image contains the complete timeline with its current stage highlighted.
            Image(
                painter = painterResource(id = stageImages[activeStageIndex]),
                contentDescription = "Tomato growth timeline: ${stageNames[activeStageIndex]} active",
                modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(363f / 75f),
                contentScale = ContentScale.Fit
            )

            // Button: `View More ➔`
            Surface(
                modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewMoreClick() },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Row(
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View More Details",
                        style = MaterialTheme.typography.labelLarge.copy(color = AppTextColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "View More",
                        tint = Slate900,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlantStagesCardPreview() {
    AgriQuestTheme {
        PlantStagesCard(
            modifier = Modifier.padding(AppLayout.cardPadding)
        )
    }
}
