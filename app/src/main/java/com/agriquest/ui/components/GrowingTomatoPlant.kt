package com.agriquest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agriquest.R

/**
 * What the plant looks like at a given age. The art is the team's reference set
 * res/drawable/tomato_stages_<stage>.png (see docs/art-guidelines.md); "seed" is the germination
 * image with the sprout removed, so it is a seed in the soil only.
 *
 * @param topFraction where the top of the plant is in the image (0 = top edge), used to place outfits
 */
enum class TomatoLook(val description: String, val imageRes: Int, val topFraction: Float) {
    SEED("Seed in the soil", R.drawable.tomato_stages_seed, 0.86f),
    SPROUT("Sprout with its first small leaves", R.drawable.tomato_stages_germination, 0.695f),
    SEEDLING("Young seedling", R.drawable.tomato_stages_seedling, 0.40f),
    VEGETATIVE("Leafy tomato plant", R.drawable.tomato_stages_vegetative, 0.03f),
    FLOWERING("Tomato plant with yellow flowers", R.drawable.tomato_stages_flowering, 0.03f),
    GREEN_FRUIT("Tomato plant with green fruit", R.drawable.tomato_stages_fruit, 0.03f),
    RIPE_FRUIT("Tomato plant with ripe red tomatoes", R.drawable.tomato_stages_ripening, 0.03f);

    companion object {
        /** Soil line in every stage image, as a fraction of its height. */
        const val SOIL_FRACTION = 0.867f

        /** Follows the growth timeline in PlantGrowthStage (days since sowing). */
        fun forDay(day: Int): TomatoLook = when {
            day <= 4 -> SEED
            day <= 9 -> SPROUT
            day <= 27 -> SEEDLING
            day <= 55 -> VEGETATIVE
            day <= 69 -> FLOWERING
            day <= 104 -> GREEN_FRUIT
            else -> RIPE_FRUIT
        }
    }
}

/**
 * The tomato plant for its real age: a seed in the soil first (days 0-4), then the sprout,
 * seedling, leafy plant, flowers, green fruit and ripe tomatoes, using the stage art.
 */
@Composable
fun GrowingTomatoPlant(dayCount: Int, modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val day = dayCount.coerceAtLeast(0)
    val look = TomatoLook.forDay(day)
    Image(
        bitmap = ImageBitmap.imageResource(look.imageRes),
        contentDescription = "Day $day: ${look.description}",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None // keep the pixel art crisp
    )
}

/** Where the top of the plant is, as a fraction of the drawing's height (0 = top edge). Used to place outfits. */
fun tomatoTopFraction(dayCount: Int): Float = TomatoLook.forDay(dayCount.coerceAtLeast(0)).topFraction

/** Height from the soil to the top of the plant, as a fraction of the drawing's height. */
fun tomatoStemFraction(dayCount: Int): Float = TomatoLook.SOIL_FRACTION - tomatoTopFraction(dayCount)

@Preview(showBackground = true, widthDp = 420, heightDp = 300)
@Composable
private fun GrowingTomatoPlantPreview() {
    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(listOf(2, 7, 15, 40), listOf(60, 85, 112)).forEach { days ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { d ->
                    Column { GrowingTomatoPlant(d, size = 90.dp); Text("Day $d") }
                }
            }
        }
    }
}
