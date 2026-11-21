package com.agriquest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriquest.R
import com.agriquest.ui.theme.AppAccentBackground
import com.agriquest.ui.theme.AppTextColor
import com.agriquest.ui.theme.InterFontFamily

private data class GrowthStage(val name: String, val duration: String, val strip: Int)

private val Stages = listOf(
    GrowthStage("Seed\nGermination", "5-10 Days", R.drawable.germination),
    GrowthStage("Seedling\nStage", "2-4 Weeks", R.drawable.seedling),
    GrowthStage("Vegetative\nGrowth", "3-4 Weeks", R.drawable.vegetative),
    GrowthStage("Flowering\nStage", "5-7 Weeks", R.drawable.flowering),
    GrowthStage("Fruit Set &\nDevelopment", "7-9 Weeks", R.drawable.fruiting),
    GrowthStage("Ripening &\nHarvesting", "9-12 Weeks", R.drawable.harvest)
)

// The strips are 363 x 33 px pixel art. Circle centres, in strip pixels, measured from the art.
private const val STRIP_WIDTH = 363f
private const val STRIP_HEIGHT = 33f
private val CircleCentres = listOf(19.5f, 83.5f, 149.5f, 209.5f, 273.5f, 339.5f)
// The germination strip's art sits 2 px further right than the others.
private fun centres(stageIndex: Int) =
    if (stageIndex == 0) CircleCentres.map { it + 2f } else CircleCentres

/** Illustrated seed-to-harvest timeline, in growth order. Pixel circles are images; labels are real text. */
@Composable
fun PlantStagesTimeline() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Stages.forEachIndexed { current, stage ->
            Column {
                Image(
                    painter = painterResource(stage.strip),
                    contentDescription = "${stage.name.replace('\n', ' ')}, stage ${current + 1} of ${Stages.size}",
                    modifier = Modifier.fillMaxWidth().aspectRatio(STRIP_WIDTH / STRIP_HEIGHT),
                    contentScale = ContentScale.Fit
                )
                StageLabels(current, centres(current), Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        }
    }
}

/** One label under each circle: the current stage shows its name and duration, the rest just the duration. */
@Composable
private fun StageLabels(current: Int, centres: List<Float>, modifier: Modifier) {
    Layout(
        modifier = modifier,
        content = {
            Stages.forEachIndexed { index, stage ->
                if (index == current) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stage.name, color = AppAccentBackground, fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp,
                            textAlign = TextAlign.Center)
                        Text("(${stage.duration})", color = AppTextColor, fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold, fontSize = 9.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    Text("(${stage.duration})", color = AppTextColor, fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold, fontSize = 9.sp, textAlign = TextAlign.Center)
                }
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val placeables = measurables.map { it.measure(Constraints(maxWidth = width / 3)) }
        layout(width, placeables.maxOf { it.height }) {
            placeables.forEachIndexed { index, placeable ->
                // Centre each label under its circle, but keep it inside the row.
                val centreX = (centres[index] / STRIP_WIDTH * width).toInt()
                val x = (centreX - placeable.width / 2).coerceIn(0, width - placeable.width)
                placeable.place(x, 0)
            }
        }
    }
}
