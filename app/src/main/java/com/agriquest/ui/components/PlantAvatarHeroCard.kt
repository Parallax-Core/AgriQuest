package com.agriquest.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriquest.domain.model.PlantAvatar
import com.agriquest.domain.model.PlantMoodle
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Emerald200
import com.agriquest.ui.theme.Emerald50
import com.agriquest.ui.theme.Emerald600
import com.agriquest.ui.theme.Emerald700
import com.agriquest.ui.theme.PureWhite
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate50
import com.agriquest.ui.theme.Slate800
import com.agriquest.ui.theme.Slate900
import kotlinx.coroutines.delay

/**
 * Digital Twin / Plant Avatar Hero Card displaying 2D Plant Illustration,
 * Floating Moodles Column Badge Stack, Health Tag, Streak Badge, and Speech Bubble.
 */
@Composable
fun PlantAvatarHeroCard(
    modifier: Modifier = Modifier,
    plant: PlantAvatar = PlantAvatar(),
    activeMoodles: List<PlantMoodle> = listOf(PlantMoodle.THRIVING)
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Status Tag ("Vibrant & Healthy") & Glowing Streak Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plant Status Tag
                Surface(
                    shape = CircleShape,
                    color = Emerald50,
                    modifier = Modifier.border(1.dp, Emerald200, CircleShape)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Status Healthy",
                            tint = Emerald600,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = plant.statusTag,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Emerald700
                            )
                        )
                    }
                }

                // Glowing Care Streak Badge Chip ("$streakDays / $targetStreakDays Days 🔥")
                CareStreakBadge(
                    streakDays = plant.streakDays,
                    targetStreakDays = plant.targetStreakDays
                )
            }

            // Central Plant Avatar Container + Floating Right Moodles Column
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Imported plant image for the current growth stage
                PlantAvatarCanvas(
                    growthStage = plant.growthStage,
                    size = 140.dp
                )

                // Stack of Floating Vertical Moodle Circle Badges on the right side
                if (activeMoodles.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        activeMoodles.forEach { moodle ->
                            Surface(
                                shape = CircleShape,
                                color = PureWhite,
                                shadowElevation = 4.dp,
                                border = BorderStroke(1.5.dp, Emerald200),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = moodle.iconResId),
                                        contentDescription = moodle.displayName,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Agro-Speech Bubble Widget with Tail Pointing Towards Plant
            SpeechBubbleWidget(
                message = plant.speechBubble
            )
        }
    }
}

/**
 * Agro-speech bubble widget with a triangle tail pointing upwards towards the plant avatar.
 */
@Composable
private fun SpeechBubbleWidget(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Speech Bubble Tail (Triangle pointing UP)
        Canvas(
            modifier = Modifier.size(width = 16.dp, height = 8.dp)
        ) {
            val tailPath = Path().apply {
                moveTo(size.width / 2f, 0f) // Top apex pointing towards plant
                lineTo(0f, size.height)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(
                path = tailPath,
                color = Slate50
            )
        }

        // Speech Bubble Box Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Slate200, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = Slate50
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "AI Speech",
                    tint = Emerald600,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Slate800,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
fun PlantAvatarHeroCardPreview() {
    AgriQuestTheme {
        PlantAvatarHeroCard(
            plant = PlantAvatar(
                name = "Flora",
                level = "Lvl. 3 Urban Grower",
                currentXp = 340,
                maxXp = 450,
                statusTag = "Vibrant & Healthy",
                streakDays = 3,
                targetStreakDays = 7,
                speechBubble = "Soil is well-hydrated. Morning light levels look great!"
            ),
            activeMoodles = listOf(PlantMoodle.THIRSTY, PlantMoodle.HEAT_STRESSED)
        )
    }
}

/**
 * Care Streak Pill Badge chip displaying glowing fire highlight state when streak > 0 vs gray/muted chip when streak == 0,
 * with pulsing/pop scale animation when streak increments.
 */
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
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = textColor
                )
            )
        }
    }
}
