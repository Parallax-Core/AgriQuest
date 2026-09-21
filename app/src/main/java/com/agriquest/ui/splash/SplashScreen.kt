package com.agriquest.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriquest.R
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.PureWhite

/**
 * Splash Screen & Start Screen for AgriQuest Mobile.
 *
 * Renders full-screen background image `splash_bg.png`, centered logo `aqlogo.png`,
 * and dark forest green pixel-art rounded start button ("START GAME" / "TAP TO START")
 * matching reference image `start screen.png`.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onStartGame: () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Full-Screen Background Image
        Image(
            painter = painterResource(id = R.drawable.splash_bg),
            contentDescription = "Splash Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Foreground Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 430.dp)
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Upper Middle: Logo `aqlogo.png`
            Image(
                painter = painterResource(id = R.drawable.aqlogo),
                contentDescription = "AgriQuest Logo",
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .widthIn(max = 380.dp)
                    .padding(top = 40.dp),
                contentScale = ContentScale.Fit
            )

            // Bottom Section: Dark Forest Green Pixel Art Start Button
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.98f,
                targetValue = 1.03f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "startPulseScale"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dark Forest Green Pixel Art Rounded Start Button
                Button(
                    onClick = onStartGame,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(60.dp)
                        .scale(pulseScale)
                        .border(3.dp, Color(0xFF15803D), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF064E3B),
                        contentColor = PureWhite
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Start",
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START GAME",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                letterSpacing = 2.sp,
                                color = PureWhite
                            )
                        )
                    }
                }

                // Subtitle / Prompt Tag
                Box(
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "TAP TO START YOUR FARMING QUEST",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF86EFAC),
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun SplashScreenPreview() {
    AgriQuestTheme {
        SplashScreen()
    }
}
