package com.agriquest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** What the drawing shows for a plant of a given age, following the tomato growth timeline. */
enum class TomatoLook(val description: String) {
    SEED("Seed in the soil"),
    SPROUTING("Sprout breaking through the soil"),
    COTYLEDONS("Seedling with two seed leaves"),
    TRUE_LEAVES("Seedling with its first true leaves"),
    LEAFY("Leafy young tomato plant"),
    FLOWERING("Tomato plant with yellow flowers"),
    GREEN_FRUIT("Tomato plant with green fruit"),
    RIPE_FRUIT("Tomato plant with ripening red fruit");

    companion object {
        fun forDay(day: Int): TomatoLook = when {
            day <= 5 -> SEED
            day <= 9 -> SPROUTING
            day <= 15 -> COTYLEDONS
            day <= 27 -> TRUE_LEAVES
            day <= 55 -> LEAFY
            day <= 69 -> FLOWERING
            day <= 99 -> GREEN_FRUIT
            else -> RIPE_FRUIT
        }
    }
}

private val Soil = Color(0xFF6B4423)
private val SoilDark = Color(0xFF4A2E17)
private val SeedColor = Color(0xFFE9D3A0)
private val Stem = Color(0xFF4E8A2E)
private val Leaf = Color(0xFF5DA83A)
private val LeafDark = Color(0xFF3E7A24)
private val Flower = Color(0xFFF7D037)
private val FruitGreen = Color(0xFF8CC63F)
private val FruitRed = Color(0xFFE0402B)

/**
 * 2D tomato plant drawn from its real age in days: a seed under the soil at first, then a sprout,
 * seed leaves, true leaves, a leafy plant, flowers, green fruit and finally red tomatoes.
 */
@Composable
fun GrowingTomatoPlant(dayCount: Int, modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val day = dayCount.coerceAtLeast(0)
    val look = TomatoLook.forDay(day)
    Canvas(modifier.size(size).semantics { contentDescription = "Day $day: ${look.description}" }) {
        drawTomatoPlant(day)
    }
}

private fun DrawScope.drawTomatoPlant(day: Int) {
    val w = size.width
    val h = size.height
    val soilTop = h * 0.84f
    val baseX = w / 2f

    // Soil mound
    drawOval(Soil, topLeft = Offset(w * 0.18f, soilTop - h * 0.03f), size = Size(w * 0.64f, h * 0.12f))
    drawOval(SoilDark, topLeft = Offset(w * 0.24f, soilTop + h * 0.02f), size = Size(w * 0.52f, h * 0.06f))

    when (TomatoLook.forDay(day)) {
        TomatoLook.SEED -> {
            // Seed resting just under the surface; it starts to crack open after day 3.
            val seedW = w * 0.09f
            val seedH = h * 0.06f
            val seedTop = soilTop + h * 0.005f
            drawOval(SeedColor, topLeft = Offset(baseX - seedW / 2, seedTop), size = Size(seedW, seedH))
            if (day >= 3) drawLine(LeafDark, Offset(baseX, seedTop), Offset(baseX + seedW * 0.2f, seedTop - h * 0.02f),
                strokeWidth = w * 0.012f, cap = StrokeCap.Round)
        }
        TomatoLook.SPROUTING -> {
            // A hooked stem pushing out of the soil, getting taller each day.
            val rise = h * (0.05f + 0.025f * (day - 6))
            val path = Path().apply {
                moveTo(baseX, soilTop)
                quadraticTo(baseX + w * 0.02f, soilTop - rise, baseX - w * 0.05f, soilTop - rise * 0.8f)
            }
            drawPath(path, Stem, style = Stroke(width = w * 0.025f, cap = StrokeCap.Round))
            drawOval(SeedColor, topLeft = Offset(baseX - w * 0.085f, soilTop - rise * 0.85f), size = Size(w * 0.06f, h * 0.04f))
        }
        else -> drawGrownPlant(day, baseX, soilTop)
    }
}

private fun DrawScope.drawGrownPlant(day: Int, baseX: Float, soilTop: Float) {
    val w = size.width
    val h = size.height
    // Height grows quickly until flowering, then levels off.
    val growth = ((day - 10).coerceAtLeast(0) / 50f).coerceIn(0f, 1f)
    val stemHeight = h * (0.16f + 0.52f * growth)
    val top = Offset(baseX, soilTop - stemHeight)
    drawLine(Stem, Offset(baseX, soilTop), top, strokeWidth = w * (0.025f + 0.015f * growth), cap = StrokeCap.Round)

    val look = TomatoLook.forDay(day)
    // Two cotyledons (seed leaves) at the top of young seedlings, lower down once the plant is bigger.
    val cotyY = if (look == TomatoLook.COTYLEDONS) top.y else soilTop - stemHeight * 0.18f
    val cotyW = w * 0.11f
    val cotyH = h * 0.045f
    drawOval(Leaf, topLeft = Offset(baseX - cotyW, cotyY - cotyH / 2), size = Size(cotyW, cotyH))
    drawOval(Leaf, topLeft = Offset(baseX, cotyY - cotyH / 2), size = Size(cotyW, cotyH))
    if (look == TomatoLook.COTYLEDONS) return

    // True leaves: compound leaves alternating left and right; more of them as the plant ages.
    val leafPairs = when (look) {
        TomatoLook.TRUE_LEAVES -> 1 + (day - 16) / 6
        else -> min(3 + (day - 28) / 8, 7)
    }.coerceAtLeast(1)
    val leafScale = 0.6f + 0.4f * growth
    for (i in 0 until leafPairs) {
        val t = (i + 1f) / (leafPairs + 1f)
        val y = soilTop - stemHeight * (0.25f + 0.7f * t)
        val side = if (i % 2 == 0) -1f else 1f
        drawCompoundLeaf(Offset(baseX, y), side, w * 0.22f * leafScale * (1.1f - 0.4f * t))
    }
    // Small top leaves
    drawCompoundLeaf(top, -1f, w * 0.09f)
    drawCompoundLeaf(top, 1f, w * 0.09f)

    if (day >= 56) {
        // Flower trusses near the upper branches.
        val flowerSpots = listOf(Offset(baseX - w * 0.16f, top.y + stemHeight * 0.2f),
            Offset(baseX + w * 0.17f, top.y + stemHeight * 0.35f), Offset(baseX - w * 0.05f, top.y + stemHeight * 0.08f))
        val flowers = if (day >= 70) flowerSpots.take(1) else flowerSpots
        flowers.forEach { drawFlower(it, w * 0.03f) }
    }
    if (day >= 70) {
        // Fruit grows bigger, then turns from green to red from day 95.
        val fruitSize = w * (0.05f + 0.035f * ((day - 70) / 25f).coerceIn(0f, 1f))
        val ripeness = ((day - 95) / 15f).coerceIn(0f, 1f)
        val color = lerp(FruitGreen, FruitRed, ripeness)
        listOf(Offset(baseX + w * 0.15f, soilTop - stemHeight * 0.42f),
            Offset(baseX - w * 0.17f, soilTop - stemHeight * 0.55f),
            Offset(baseX + w * 0.08f, soilTop - stemHeight * 0.66f)).forEach { center ->
            drawCircle(color, fruitSize, center)
            drawCircle(Color.White.copy(alpha = 0.35f), fruitSize * 0.3f, center + Offset(-fruitSize * 0.35f, -fruitSize * 0.35f))
            drawLine(LeafDark, center + Offset(0f, -fruitSize), center + Offset(0f, -fruitSize * 1.4f), strokeWidth = w * 0.012f)
        }
    }
}

/** A tomato leaf: a stalk with small leaflets, pointing left (side = -1) or right (side = 1). */
private fun DrawScope.drawCompoundLeaf(origin: Offset, side: Float, length: Float) {
    val end = origin + Offset(side * length, -length * 0.25f)
    drawLine(Stem, origin, end, strokeWidth = length * 0.07f, cap = StrokeCap.Round)
    val leaflet = Size(length * 0.32f, length * 0.2f)
    for (k in 1..3) {
        val p = origin + (end - origin) * (k / 3f)
        rotate(degrees = side * -25f, pivot = p) {
            drawOval(if (k % 2 == 0) LeafDark else Leaf, topLeft = p - Offset(leaflet.width / 2, leaflet.height), size = leaflet)
        }
        rotate(degrees = side * 25f, pivot = p) {
            drawOval(Leaf, topLeft = p - Offset(leaflet.width / 2, 0f), size = leaflet)
        }
    }
}

private fun DrawScope.drawFlower(center: Offset, r: Float) {
    for (k in 0 until 5) {
        val angle = Math.toRadians(k * 72.0)
        drawCircle(Flower, r * 0.55f, center + Offset((kotlin.math.cos(angle) * r).toFloat(), (kotlin.math.sin(angle) * r).toFloat()))
    }
    drawCircle(Color(0xFFE08A1E), r * 0.45f, center)
}

@Preview(showBackground = true, widthDp = 420, heightDp = 300)
@Composable
private fun GrowingTomatoPlantPreview() {
    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(listOf(2, 7, 12, 20), listOf(40, 60, 85, 112)).forEach { days ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { d ->
                    Column { GrowingTomatoPlant(d, size = 90.dp); Text("Day $d") }
                }
            }
        }
    }
}

/** Where the top of the drawn plant is, as a fraction of the drawing's height (0 = top edge). Used to place outfits. */
fun tomatoTopFraction(dayCount: Int): Float {
    val day = dayCount.coerceAtLeast(0)
    val soilTop = 0.84f
    return when (TomatoLook.forDay(day)) {
        TomatoLook.SEED -> soilTop - 0.01f
        TomatoLook.SPROUTING -> soilTop - (0.05f + 0.025f * (day - 6)) * 0.85f
        else -> soilTop - (0.16f + 0.52f * ((day - 10).coerceAtLeast(0) / 50f).coerceIn(0f, 1f))
    }
}

/** Height of the drawn stem as a fraction of the drawing's height. */
fun tomatoStemFraction(dayCount: Int): Float = 0.84f - tomatoTopFraction(dayCount)
