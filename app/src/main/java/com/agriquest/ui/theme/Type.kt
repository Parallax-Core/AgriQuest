package com.agriquest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
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
private fun appText(size: Int, height: Int, heading: Boolean = false) = TextStyle(
    fontFamily = if (heading) SilkscreenFontFamily else InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = size.sp,
    lineHeight = height.sp,
    letterSpacing = 0.sp
)

val Typography = Typography(
    displayLarge = appText(28, 33, heading = true),
    displayMedium = appText(28, 33, heading = true),
    displaySmall = appText(28, 33, heading = true),
    headlineLarge = appText(22, 28, heading = true),
    headlineMedium = appText(22, 28, heading = true),
    headlineSmall = appText(22, 28, heading = true),
    titleLarge = appText(16, 21),
    titleMedium = appText(16, 21, heading = true),
    titleSmall = appText(14, 19),
    bodyLarge = appText(14, 19),
    bodyMedium = appText(14, 19),
    bodySmall = appText(12, 17),
    labelLarge = appText(12, 17),
    labelMedium = appText(11, 16),
    labelSmall = appText(10, 15)
)
