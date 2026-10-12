package com.agriquest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.agriquest.R

@OptIn(ExperimentalTextApi::class)
val InterFontFamily = FontFamily(
    Font(R.font.inter, weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter, weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter, weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.inter, weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800)))
)

val SilkscreenFontFamily = FontFamily(
    Font(R.font.silkscreen_bold, weight = FontWeight.Bold)
)

// Quests is the reference: pixel headings, compact Inter content and badges.
private const val AppTextScale = 1.15f

fun scaledTextSize(size: Int) = (size * AppTextScale).sp

private fun appText(size: Int, height: Int, heading: Boolean = false, weight: FontWeight = FontWeight.SemiBold) = TextStyle(
    fontFamily = if (heading) SilkscreenFontFamily else InterFontFamily,
    fontWeight = if (heading) FontWeight.Bold else weight,
    fontSize = scaledTextSize(size),
    lineHeight = scaledTextSize(height),
    letterSpacing = if (heading) scaledTextSize(1) else 0.sp
)

val Typography = Typography(
    displayLarge = appText(28, 33, heading = true),
    displayMedium = appText(28, 33, heading = true),
    displaySmall = appText(28, 33, heading = true),
    headlineLarge = appText(14, 19, heading = true),
    headlineMedium = appText(12, 17, heading = true),
    headlineSmall = appText(12, 17, heading = true),
    titleLarge = appText(16, 20, weight = FontWeight.Bold),
    titleMedium = appText(11, 16, heading = true),
    titleSmall = appText(12, 16, weight = FontWeight.Bold),
    bodyLarge = appText(13, 18),
    bodyMedium = appText(12, 17, weight = FontWeight.Bold),
    bodySmall = appText(11, 16),
    labelLarge = appText(11, 16, weight = FontWeight.Bold),
    labelMedium = appText(10, 15, weight = FontWeight.Bold),
    labelSmall = appText(10, 15)
)
