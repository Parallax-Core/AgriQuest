package com.agriquest.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agriquest.domain.model.Reward
import com.agriquest.ui.sound.GameSound
import com.agriquest.ui.sound.GameSounds
import com.agriquest.ui.theme.AppTextColor
import kotlin.random.Random

/** "Congratulations! You've gained 5 XP and completed the mission!" with confetti and a success sound. */
@Composable
fun RewardDialog(reward: Reward, onDismiss: () -> Unit) {
    LaunchedEffect(reward) { GameSounds.play(GameSound.SUCCESS) }
    val what = when (reward.kind) {
        Reward.Kind.QUIZ -> "answered the daily quiz correctly"
        Reward.Kind.ALL_QUESTS -> "completed all of today's missions"
        Reward.Kind.QUEST -> "completed the mission"
    }
    val message = if (reward.xpGranted > 0) "You've gained ${reward.xpGranted} XP and $what!"
        else "You've $what! You already reached today's XP limit, so no XP was added."
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Confetti(Modifier.fillMaxSize())
            PixelPanel(Modifier.widthIn(max = 340.dp).padding(24.dp)) {
                Text("🎉", style = MaterialTheme.typography.displaySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Congratulations!", style = MaterialTheme.typography.titleLarge, color = AppTextColor,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = AppTextColor, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                Text(reward.title, style = MaterialTheme.typography.labelMedium, color = PixelGreen, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
                PixelButton("Awesome!", onDismiss, Modifier.fillMaxWidth())
            }
        }
    }
}

private data class ConfettiPiece(val x: Float, val speed: Float, val phase: Float, val spin: Float, val color: Color)

@Composable
private fun Confetti(modifier: Modifier) {
    val pieces = remember {
        val colors = listOf(Color(0xFFE0402B), Color(0xFFFFDF6D), Color(0xFF649B32), Color(0xFF2F87AE), Color(0xFFF59E0B))
        List(40) { ConfettiPiece(Random.nextFloat(), 0.6f + Random.nextFloat() * 0.8f, Random.nextFloat(),
            Random.nextFloat() * 720f - 360f, colors[it % colors.size]) }
    }
    val time by rememberInfiniteTransition(label = "confetti")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "confettiTime")
    Canvas(modifier) {
        pieces.forEach { p ->
            val t = (time * p.speed + p.phase) % 1f
            val center = Offset(p.x * size.width + kotlin.math.sin((t + p.phase) * 12f) * 18f, t * (size.height + 40f) - 20f)
            rotate(p.spin * t, pivot = center) {
                drawRect(p.color, topLeft = center - Offset(5f, 9f), size = Size(10f, 18f))
            }
        }
    }
}
