package com.agriquest.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class FallingTomato(val x: Float, val speed: Float, val phase: Float, val size: Float, val spin: Float)

/** Pixel-style tomatoes slowly falling and tumbling behind the landing page logo. */
@Composable
fun FallingTomatoes(modifier: Modifier = Modifier, count: Int = 14) {
    val tomatoes = remember(count) {
        val random = Random(42)
        List(count) {
            FallingTomato(x = random.nextFloat(), speed = 0.55f + random.nextFloat() * 0.7f, phase = random.nextFloat(),
                size = 14f + random.nextFloat() * 18f, spin = (random.nextFloat() - 0.5f) * 540f)
        }
    }
    val time by rememberInfiniteTransition(label = "fallingTomatoes")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "fallTime")
    Canvas(modifier) {
        tomatoes.forEach { t ->
            val progress = (time * t.speed * 2f + t.phase) % 1f
            val px = t.size * density / 2f
            val center = Offset(t.x * size.width + kotlin.math.sin(progress * 6.28f + t.phase * 6f) * 14f,
                progress * (size.height + px * 4) - px * 2)
            rotate(t.spin * progress, pivot = center) { drawPixelTomato(center, px) }
        }
    }
}

/** A chunky "pixel" tomato: square red body with a highlight and a green leafy top. */
private fun DrawScope.drawPixelTomato(c: Offset, r: Float) {
    val p = r / 3f // one "pixel"
    val red = Color(0xFFE0402B)
    val dark = Color(0xFFB02E1E)
    val green = Color(0xFF3E8A26)
    fun px(dx: Int, dy: Int, color: Color, w: Int = 1, h: Int = 1) =
        drawRect(color, topLeft = Offset(c.x + dx * p, c.y + dy * p), size = Size(w * p + 0.5f, h * p + 0.5f))
    px(-2, -2, red, 4, 1); px(-3, -1, red, 6, 3); px(-2, 2, dark, 4, 1)
    px(-2, -1, Color(0xFFFF8A70)) // highlight
    px(-1, -3, green, 2, 1); px(-2, -3, green); px(1, -3, green); px(0, -4, green)
}
