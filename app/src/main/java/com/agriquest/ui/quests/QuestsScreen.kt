package com.agriquest.ui.quests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agriquest.domain.model.Quest
import com.agriquest.ui.components.AppCard
import com.agriquest.ui.components.AppScreen
import com.agriquest.ui.components.AppText
import com.agriquest.ui.components.CardTone
import com.agriquest.ui.components.DailyQuizCard
import com.agriquest.ui.components.PixelPageHeader
import com.agriquest.ui.components.PixelPanel
import com.agriquest.ui.state.AgriQuestUiState
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.AppLayout
import com.agriquest.ui.theme.AppTextColor

@Composable
fun QuestsScreenContent(
    modifier: Modifier = Modifier,
    uiState: AgriQuestUiState = AgriQuestUiState(),
    selectedTab: Int = 2,
    onTabSelected: (Int) -> Unit = {},
    onCompleteQuest: (String) -> Unit = {},
    onAnswerQuiz: (Int) -> Unit = {},
    onNextQuizQuestion: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    AppScreen(
        modifier = modifier,
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        snackbarHostState = snackbarHostState
    ) {
        PixelPageHeader("Quests", "Daily", uiState)
        AvailableXpBanner(uiState)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText("◎ Plant Care Quests", heading = true, style = MaterialTheme.typography.bodyMedium)
            Surface(
                color = Color(0xFFE3EBB0), shape = RoundedCornerShape(3.dp),
                border = BorderStroke(1.dp, Color(0xFFA9B666))
            ) {
                AppText(
                    "${uiState.quests.count { it.isClaimed }}/${uiState.quests.size} Done",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color(0xFF32651D), style = MaterialTheme.typography.labelMedium
                )
            }
        }
        uiState.quests.forEach { quest -> PlantCareQuestCard(quest, onCompleteQuest) }
        GreenThumbPanel(uiState)
        DailyQuizCard(
            question = uiState.currentQuizQuestion,
            isQuizCompletedToday = uiState.isQuizCompletedToday,
            selectedOptionIndex = uiState.selectedQuizOptionIndex,
            isAnswerCorrect = uiState.isQuizAnswerCorrect,
            onAnswerSelected = onAnswerQuiz,
            onNextQuestion = onNextQuizQuestion
        )

        PixelPanel(tone = CardTone.ACCENT) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AppText("★ Quiz Rewards", heading = true, color = Color(0xFFFFDF6D),
                    style = MaterialTheme.typography.titleMedium)
                AppText("+${uiState.currentQuizQuestion.xpReward} XP / answer", color = Color(0xFFFFF5D9),
                    style = MaterialTheme.typography.labelSmall)
            }
            AppText("Answer correctly to earn XP and grow your plant knowledge.", color = Color(0xFFADC3C8),
                style = MaterialTheme.typography.bodySmall, weight = FontWeight.SemiBold)
        }

    }
}

@Composable
private fun QuestProgress(progress: Float, color: Color, track: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(8.dp).background(track, RoundedCornerShape(1.dp))
        .border(1.dp, Color(0xFFC9A86A), RoundedCornerShape(1.dp))) {
        if (progress > 0f) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(8.dp).background(color))
        }
    }
}

@Composable
private fun AvailableXpBanner(uiState: AgriQuestUiState) {
    AppCard(modifier = Modifier.fillMaxWidth(),
        tone = CardTone.HIGHLIGHT) {
        Column(Modifier.padding(AppLayout.cardPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                AppText("★ Total XP Today", heading = true, style = MaterialTheme.typography.titleMedium)
                Surface(color = Color(0xFFFFF5D9), shape = CircleShape,
                    border = BorderStroke(1.dp, Color(0xFFE3A32C))) {
                    AppText("${uiState.streakDays}-day streak",
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            AppText("+${uiState.quests.sumOf { it.xpReward }} XP", heading = true, style = MaterialTheme.typography.displaySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AppText("Level ${uiState.plant.levelInt}", style = MaterialTheme.typography.labelMedium, weight = FontWeight.SemiBold)
                AppText("${uiState.plant.currentXp} / ${uiState.plant.maxXp} XP", style = MaterialTheme.typography.labelMedium)
            }
            QuestProgress(uiState.plant.xpProgress, Color(0xFF649B32), Color(0xFFF3E0A0))
        }
    }
}

@Composable
private fun PlantCareQuestCard(quest: Quest, onCompleteQuest: (String) -> Unit) {
    val done = quest.isClaimed
    val icon = when {
        quest.id.contains("water") || quest.id.contains("thirst") -> Icons.Rounded.WaterDrop
        quest.id.contains("prune") -> Icons.Rounded.ContentCut
        else -> Icons.Rounded.Search
    }
    val iconColor = when {
        quest.id.contains("water") || quest.id.contains("thirst") -> Color(0xFF2F87AE)
        quest.id.contains("prune") -> Color(0xFF8B5237)
        else -> Color(0xFF32651D)
    }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier.size(40.dp), shape = RoundedCornerShape(3.dp),
                color = iconColor.copy(alpha = 0.12f), border = BorderStroke(1.dp, Color(0xFFC9A86A))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top) {
                    AppText(quest.title, Modifier.weight(1f), color = AppTextColor, style = MaterialTheme.typography.bodyMedium, weight = FontWeight.Bold)
                    Surface(color = Color(0xFFFFDF6D), shape = RoundedCornerShape(3.dp),
                        border = BorderStroke(1.dp, Color(0xFFE3A32C))) {
                        AppText(quest.xpRewardText, Modifier.padding(horizontal = 10.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
                AppText(quest.subtext, color = AppTextColor, style = MaterialTheme.typography.bodySmall, weight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuestProgress(if (done) 1f else quest.progress,
                        if (done) Color(0xFF649B32) else Color(0xFFE2A72B),
                        Color(0xFFF3E0A0), Modifier.weight(1f))
                    AppText(quest.progressText, style = MaterialTheme.typography.labelSmall, color = Color(0xFF32651D))
                }
                if (done) {
                    Surface(modifier = Modifier.align(Alignment.End).height(22.4.dp),
                        color = Color(0xFFE3EBB0), shape = RoundedCornerShape(3.dp),
                        border = BorderStroke(1.dp, Color(0xFFA9B666))) {
                        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                            AppText("✓ Completed", color = Color(0xFF32651D), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Button(
                        onClick = { onCompleteQuest(quest.id) },
                        modifier = Modifier.align(Alignment.End).height(30.dp),
                        shape = AppLayout.buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF193F4E)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) { AppText("Mark Complete", color = Color(0xFFFFF5D9), style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

@Composable
private fun GreenThumbPanel(uiState: AgriQuestUiState) {
    val target = uiState.targetStreakDays.coerceAtLeast(1)
    val days = uiState.streakDays.coerceIn(0, target)
    AppCard(modifier = Modifier.fillMaxWidth(),
        tone = CardTone.ACCENT) {
        Column(Modifier.padding(AppLayout.cardPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AppText("★ Green Thumb", color = Color(0xFFFFDF6D), heading = true, style = MaterialTheme.typography.bodySmall)
                AppText("$days / $target days", color = Color(0xFFFFF5D9), style = MaterialTheme.typography.labelMedium)
            }
            AppText("Complete all daily quests $target days in a row.",
                color = Color(0xFFADC3C8), style = MaterialTheme.typography.labelMedium, weight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(target) { index ->
                    Box(Modifier.weight(1f).height(9.dp).background(
                            if (index < days) Color(0xFFFFDF6D) else Color(0xFF345C69), RoundedCornerShape(1.dp)))
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun QuestsScreenPreview() {
    AgriQuestTheme { QuestsScreenContent() }
}
