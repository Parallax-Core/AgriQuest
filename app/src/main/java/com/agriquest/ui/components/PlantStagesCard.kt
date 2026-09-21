package com.agriquest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.PureWhite
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate500
import com.agriquest.ui.theme.Slate900

data class PlantStageItem(
    val stageNumber: Int,
    val title: String,
    val durationText: String,
    val isCompleted: Boolean,
    val isActive: Boolean
)

/**
 * Plant Stages Stepper Timeline Card for AgriQuest Mobile.
 * Displays 6 stage timeline steps with active step highlighting and "View More ➔" action button.
 */
@Composable
fun PlantStagesCard(
    modifier: Modifier = Modifier,
    currentStage: PlantGrowthStage = PlantGrowthStage.SEEDLING_SPROUT,
    onViewMoreClick: () -> Unit = {}
) {
    val activeStageIndex = when (currentStage) {
        PlantGrowthStage.SEEDLING_SPROUT -> 1 // 2 Seedling (index 1)
        PlantGrowthStage.VEGETATIVE_LEAFY -> 2 // 3 Vegetative (index 2)
        PlantGrowthStage.FLOWERING -> 3 // 4 Flowering (index 3)
        PlantGrowthStage.FRUITING_HARVEST -> 4 // 5 Fruiting (index 4)
    }

    val stages = listOf(
        PlantStageItem(1, "Germination", "(5-10 Days)", isCompleted = activeStageIndex > 0, isActive = activeStageIndex == 0),
        PlantStageItem(2, "Seedling", "(2-4 Weeks)", isCompleted = activeStageIndex > 1, isActive = activeStageIndex == 1),
        PlantStageItem(3, "Vegetative", "(3-4 Weeks)", isCompleted = activeStageIndex > 2, isActive = activeStageIndex == 2),
        PlantStageItem(4, "Flowering", "(5-7 Weeks)", isCompleted = activeStageIndex > 3, isActive = activeStageIndex == 3),
        PlantStageItem(5, "Fruiting", "(7-9 Weeks)", isCompleted = activeStageIndex > 4, isActive = activeStageIndex == 4),
        PlantStageItem(6, "Harvest", "(9-12 Weeks)", isCompleted = activeStageIndex > 5, isActive = activeStageIndex == 5)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title `🍃 Plant Stages`
            Text(
                text = "🍃 Plant Stages",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Slate900
                )
            )

            // Stepper Timeline Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                stages.forEachIndexed { index, stageItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Stage Circle (Completed ✓ vs Active Blue vs Inactive Grey)
                        val circleBg = when {
                            stageItem.isCompleted -> Color(0xFF22C55E)
                            stageItem.isActive -> Color(0xFF0288D1)
                            else -> Color(0xFFE2E8F0)
                        }
                        val circleTextColor = if (stageItem.isCompleted || stageItem.isActive) PureWhite else Slate500

                        Surface(
                            shape = CircleShape,
                            color = circleBg,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (stageItem.isCompleted) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Completed",
                                        tint = PureWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${stageItem.stageNumber}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = circleTextColor
                                        )
                                    )
                                }
                            }
                        }

                        // Stage Label & Duration
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${stageItem.stageNumber} ${stageItem.title}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (stageItem.isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (stageItem.isActive) Color(0xFF0288D1) else Slate900
                                )
                            )
                            Text(
                                text = stageItem.durationText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            )
                        }

                        if (stageItem.isActive) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE0F2FE),
                                border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color(0xFF0288D1)
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

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
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Slate900
                        )
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
            modifier = Modifier.padding(16.dp)
        )
    }
}
