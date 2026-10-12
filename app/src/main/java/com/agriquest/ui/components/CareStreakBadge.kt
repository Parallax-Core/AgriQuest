package com.agriquest.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Shows the care streak and animates when it increases. */
@Composable
fun CareStreakBadge(
    streakDays: Int,
    targetStreakDays: Int = 7,
    modifier: Modifier = Modifier
) {
    var prevStreak by remember { mutableIntStateOf(streakDays) }
    var isPulsing by remember { mutableStateOf(false) }

    LaunchedEffect(streakDays) {
        if (streakDays > prevStreak) {
            isPulsing = true
            delay(600L)
            isPulsing = false
        }
        prevStreak = streakDays
    }

    val scale by animateFloatAsState(
        targetValue = if (isPulsing) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "CareStreakPopScale"
    )

    val isStreakActive = streakDays > 0
    val containerColor = if (isStreakActive) Color(0xFFFFF7ED) else Color(0xFFF1F5F9)
    val borderColor = if (isStreakActive) Color(0xFFEA580C) else Color(0xFFCBD5E1)
    val textColor = if (isStreakActive) Color(0xFFC2410C) else Color(0xFF64748B)

    Surface(
        modifier = modifier.scale(scale),
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(if (isStreakActive) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "🔥 $streakDays / $targetStreakDays Days",
                style = MaterialTheme.typography.labelMedium.copy(color = textColor)
            )
        }
    }
}
