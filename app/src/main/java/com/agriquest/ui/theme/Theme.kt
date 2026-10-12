package com.agriquest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Emerald500,
    onPrimary = PureWhite,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald700,
    secondary = Emerald600,
    onSecondary = PureWhite,
    secondaryContainer = Emerald50,
    onSecondaryContainer = Emerald600,
    tertiary = Amber500,
    onTertiary = PureWhite,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber500,
    background = AppPageBackground,
    onBackground = AppTextColor,
    surface = AppCardBackground,
    onSurface = AppTextColor,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    outline = AppCardBorder,
    outlineVariant = Slate300
)

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = PureWhite,
    secondary = Emerald600,
    onSecondary = PureWhite,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    outline = Slate600
)

@Composable
fun AgriQuestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
