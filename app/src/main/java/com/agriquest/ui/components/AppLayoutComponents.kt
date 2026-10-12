package com.agriquest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.agriquest.ui.state.AgriQuestUiState
import com.agriquest.ui.theme.AppAccentBackground
import com.agriquest.ui.theme.AppCardBackground
import com.agriquest.ui.theme.AppCardBorder
import com.agriquest.ui.theme.AppHighlightBackground
import com.agriquest.ui.theme.AppLayout
import com.agriquest.ui.theme.AppPageBackground
import com.agriquest.ui.theme.AppTextColor
import com.agriquest.ui.theme.SilkscreenFontFamily

enum class CardTone { DEFAULT, ACCENT, HIGHLIGHT }

/** Common navigation, insets, scrolling, width, padding and section spacing. */
@Composable
fun AppScreen(
    uiState: AgriQuestUiState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppPageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AgriQuestBottomNav(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                streakDays = uiState.plant.streakDays,
                targetStreakDays = uiState.plant.targetStreakDays
            )
        }
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = AppLayout.maxWidth).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(AppLayout.pagePadding),
                verticalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing),
                content = content
            )
        }
    }
}

/** Every content card uses the same surface, border, radius and elevation. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.DEFAULT,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = AppLayout.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = when (tone) {
                CardTone.DEFAULT -> AppCardBackground
                CardTone.ACCENT -> AppAccentBackground
                CardTone.HIGHLIGHT -> AppHighlightBackground
            },
            contentColor = if (tone == CardTone.ACCENT) AppCardBackground else AppTextColor
        ),
        border = BorderStroke(AppLayout.cardBorderWidth, AppCardBorder),
        elevation = CardDefaults.cardElevation(AppLayout.cardElevation),
        content = content
    )
}

/** Compact text roles shared with the Quests page. */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppTextColor,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    heading: Boolean = false,
    weight: FontWeight = FontWeight.Bold
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = if (heading) style.copy(fontFamily = SilkscreenFontFamily) else style,
        fontWeight = weight
    )
}

@Composable
fun AppPageTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppLayout.compactSpacing)) {
        Text(subtitle.uppercase(), style = MaterialTheme.typography.labelSmall, color = AppTextColor)
        Text(title, style = MaterialTheme.typography.headlineSmall, color = AppTextColor)
    }
}
