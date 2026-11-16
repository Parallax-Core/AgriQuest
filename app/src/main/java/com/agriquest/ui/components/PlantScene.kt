package com.agriquest.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agriquest.ui.theme.AppCardBorder
import com.agriquest.ui.theme.AppTextColor

/**
 * The plant with its actuator effects: a shower of water while watering, a spinning fan with the
 * plant swaying in the breeze, and a grow lamp that makes the plant glow.
 */
@Composable
fun BoxScope.PlantScene(dayCount: Int, watering: Boolean, fanOn: Boolean, lightOn: Boolean, plantSize: Dp) {
    val loop = rememberInfiniteTransition(label = "plantScene")
    val swayPhase by loop.animateFloat(-1f, 1f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway")
    val spin by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(450, easing = LinearEasing)), label = "spin")
    val fall by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "fall")
    val swayAmount by animateFloatAsState(if (fanOn) 6f else 0f, tween(600), label = "swayAmount")
    val glow by animateFloatAsState(if (lightOn) 1f else 0f, tween(500), label = "glow")

    // Lamp glow behind the plant
    if (glow > 0f) Canvas(Modifier.matchParentSize()) {
        val lamp = Offset(size.width * 0.86f, size.height * 0.16f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3B0).copy(alpha = 0.75f * glow), Color.Transparent),
            center = lamp, radius = size.width * 0.7f), radius = size.width * 0.7f, center = lamp)
    }

    GrowingTomatoPlant(
        dayCount = dayCount, size = plantSize,
        modifier = Modifier.align(Alignment.BottomCenter)
            .graphicsLayer {
                rotationZ = swayPhase * swayAmount
                transformOrigin = TransformOrigin(0.5f, 0.88f)
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                // Brighter plant under the lamp: tint only the plant's own pixels.
                if (glow > 0f) drawRect(Color(0xFFFFF59D).copy(alpha = 0.45f * glow), blendMode = BlendMode.SrcAtop)
            }
    )

    Canvas(Modifier.matchParentSize()) {
        val w = size.width
        val h = size.height
        if (glow > 0f) {
            // Grow lamp at the side
            val lampCenter = Offset(w * 0.86f, h * 0.16f)
            drawLine(Color(0xFF5C5C5C), Offset(w * 0.86f, 0f), lampCenter, strokeWidth = 4.dp.toPx())
            drawArc(Color(0xFF3D4B57), 180f, 180f, useCenter = true,
                topLeft = lampCenter - Offset(w * 0.07f, h * 0.05f), size = Size(w * 0.14f, h * 0.1f))
            drawCircle(Color(0xFFFFF7C2).copy(alpha = glow), w * 0.035f, lampCenter)
        }
        if (watering) {
            // Watering can above the plant, with drops falling in a loop
            val spout = Offset(w * 0.62f, h * 0.2f)
            drawRoundRect(Color(0xFF3B8FC2), topLeft = Offset(w * 0.64f, h * 0.08f), size = Size(w * 0.16f, h * 0.12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
            drawLine(Color(0xFF2C6F98), Offset(w * 0.65f, h * 0.16f), spout, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
            for (k in 0 until 9) {
                val p = (fall + k / 9f) % 1f
                val x = spout.x - w * 0.12f + (k % 3) * w * 0.09f + p * w * 0.02f
                val y = spout.y + p * (h * 0.62f)
                drawLine(Color(0xFF6EC6F0).copy(alpha = 1f - p * 0.4f), Offset(x, y), Offset(x, y + h * 0.035f),
                    strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        if (fanOn) {
            // Fan on the left, blades spinning, with air lines blowing toward the plant
            val hub = Offset(w * 0.13f, h * 0.55f)
            val r = w * 0.09f
            drawLine(Color(0xFF6B6B6B), hub, Offset(hub.x, h * 0.86f), strokeWidth = 5.dp.toPx())
            drawCircle(Color(0xFFCFD8DC), r * 1.15f, hub, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
            rotate(spin, pivot = hub) {
                for (b in 0 until 3) rotate(b * 120f, pivot = hub) {
                    drawOval(Color(0xFF90A4AE), topLeft = Offset(hub.x - r * 0.25f, hub.y - r), size = Size(r * 0.5f, r))
                }
            }
            drawCircle(Color(0xFF455A64), r * 0.2f, hub)
            for (k in 0 until 4) {
                val p = (fall + k / 4f) % 1f
                val y = hub.y - r * 0.8f + k * r * 0.55f
                val x0 = hub.x + r * 1.4f + p * w * 0.25f
                drawLine(Color.White.copy(alpha = 0.8f * (1f - p)), Offset(x0, y), Offset(x0 + w * 0.08f, y),
                    strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

/** Water / Fan / Light buttons under the plant scene. */
@Composable
fun ActuatorBar(
    watering: Boolean, fanOn: Boolean, lightOn: Boolean, canWater: Boolean,
    onWater: () -> Unit, onFan: () -> Unit, onLight: () -> Unit, note: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActuatorButton("Water", if (watering) "Watering" else if (canWater) "Ready" else "Moist",
                Icons.Rounded.WaterDrop, watering, Color(0xFF2F87AE), onWater, Modifier.weight(1f))
            ActuatorButton("Fan", if (fanOn) "On" else "Off", Icons.Rounded.Air, fanOn, Color(0xFF546E7A), onFan, Modifier.weight(1f))
            ActuatorButton("Light", if (lightOn) "On" else "Off", Icons.Rounded.Lightbulb, lightOn, Color(0xFFE2A72B), onLight, Modifier.weight(1f))
        }
        if (note != null) Text(note, style = MaterialTheme.typography.labelSmall, color = AppTextColor)
    }
}

@Composable
private fun ActuatorButton(label: String, state: String, icon: ImageVector, active: Boolean, color: Color,
    onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier.height(54.dp), shape = RoundedCornerShape(4.dp),
        color = if (active) color else Color(0xFFFBE9BD), border = BorderStroke(1.dp, AppCardBorder)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(22.dp), tint = if (active) Color.White else color)
            Column {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (active) Color.White else AppTextColor)
                Text(state, style = MaterialTheme.typography.labelSmall, color = if (active) Color.White else AppTextColor)
            }
        }
    }
}
