package com.agriquest.ui.quests

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agriquest.domain.model.Quest
import com.agriquest.ui.components.AgriQuestBottomNav
import com.agriquest.ui.components.DailyQuizCard
import com.agriquest.ui.state.AgriQuestUiState
import kotlinx.coroutines.launch
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Amber100
import com.agriquest.ui.theme.Amber600
import com.agriquest.ui.theme.Emerald100
import com.agriquest.ui.theme.Emerald50
import com.agriquest.ui.theme.Emerald600
import com.agriquest.ui.theme.Emerald700
import com.agriquest.ui.theme.PureWhite
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate500
import com.agriquest.ui.theme.Slate600
import com.agriquest.ui.theme.Slate900
import com.agriquest.ui.viewmodel.AgriQuestViewModel


@Composable
fun QuestsScreen(
    modifier: Modifier = Modifier,
    viewModel: AgriQuestViewModel = viewModel(),
    onTabSelected: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Listen for notification side effects (e.g. quest completed, +XP awarded, level-up)
    LaunchedEffect(viewModel) {
        viewModel.notificationEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            snackbarHostState.showSnackbar(message)
        }
    }

    QuestsScreenContent(
        modifier = modifier,
        uiState = uiState,
        selectedTab = 2,
        onTabSelected = onTabSelected,
        onCompleteQuest = { questId -> viewModel.claimQuest(questId) },
        onSimulateNextDay = { viewModel.simulateNextDay() },
        onAnswerQuiz = { optionIndex ->
            coroutineScope.launch { viewModel.submitQuizAnswer(optionIndex) }
        },
        onNextQuizQuestion = { viewModel.nextQuizQuestion() },
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun QuestsScreenContent(
    modifier: Modifier = Modifier,
    uiState: AgriQuestUiState = AgriQuestUiState(),
    selectedTab: Int = 2,
    onTabSelected: (Int) -> Unit = {},
    onCompleteQuest: (String) -> Unit = {},
    onSimulateNextDay: () -> Unit = {},
    onAnswerQuiz: (Int) -> Unit = {},
    onNextQuizQuestion: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFFF9E9),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AgriQuestBottomNav(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                streakDays = uiState.plant.streakDays,
                targetStreakDays = uiState.plant.targetStreakDays
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = 430.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvailableXpBanner(uiState)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuestText("◎ Plant Care Quests", pixel = true, size = 14)
                    Surface(
                        color = Color(0xFFE3EBB0), shape = RoundedCornerShape(3.dp),
                        border = BorderStroke(1.dp, Color(0xFFA9B666))
                    ) {
                        QuestText(
                            "${uiState.quests.count { it.isClaimed }}/${uiState.quests.size} Done",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = Color(0xFF32651D), size = 11
                        )
                    }
                }
                uiState.quests.forEach { quest -> PlantCareQuestCard(quest, onCompleteQuest) }
                DailyQuizCard(
                    question = uiState.currentQuizQuestion,
                    isQuizCompletedToday = uiState.isQuizCompletedToday,
                    selectedOptionIndex = uiState.selectedQuizOptionIndex,
                    isAnswerCorrect = uiState.isQuizAnswerCorrect,
                    onAnswerSelected = onAnswerQuiz,
                    onNextQuestion = onNextQuizQuestion
                )
                GreenThumbPanel(uiState)
                androidx.compose.material3.OutlinedButton(
                    onClick = onSimulateNextDay,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, Color(0xFFC9A86A))
                ) { QuestText("Simulate Next Day", size = 12) }
            }
        }
    }
}

@Composable
private fun QuestText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF68451F),
    size: Int = 12,
    pixel: Boolean = false,
    weight: FontWeight = FontWeight.Bold
) {
    Text(
        text = text, modifier = modifier, color = color,
        fontFamily = if (pixel) com.agriquest.ui.theme.SilkscreenFontFamily else com.agriquest.ui.theme.InterFontFamily,
        fontWeight = weight, fontSize = size.sp, lineHeight = (size + 5).sp
    )
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
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFDF6D)),
        border = BorderStroke(1.5.dp, Color(0xFFE3A32C))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                QuestText("★ Total XP Today", pixel = true, size = 16)
                Surface(color = Color(0xFFFFF5D9), shape = CircleShape,
                    border = BorderStroke(1.dp, Color(0xFFE3A32C))) {
                    QuestText("${uiState.streakDays}-day streak",
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp), size = 10)
                }
            }
            QuestText("+${uiState.quests.sumOf { it.xpReward }} XP", pixel = true, size = 28)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuestText("Level ${uiState.plant.levelInt}", size = 11, weight = FontWeight.SemiBold)
                QuestText("${uiState.plant.currentXp} / ${uiState.plant.maxXp} XP", size = 11)
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
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5D9)),
        border = BorderStroke(2.dp, Color(0xFFC9A86A))
    ) {
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
                    QuestText(quest.title, Modifier.weight(1f), color = Color(0xFF7A5A30), size = 14, weight = FontWeight.Bold)
                    Surface(color = Color(0xFFFFDF6D), shape = RoundedCornerShape(3.dp),
                        border = BorderStroke(1.dp, Color(0xFFE3A32C))) {
                        QuestText(quest.xpRewardText, Modifier.padding(horizontal = 10.dp, vertical = 2.dp), size = 10)
                    }
                }
                QuestText(quest.subtext, color = Color(0xFF7A5A30), size = 12, weight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuestProgress(if (done) 1f else quest.progress,
                        if (done) Color(0xFF649B32) else Color(0xFFE2A72B),
                        Color(0xFFF3E0A0), Modifier.weight(1f))
                    QuestText(quest.progressText, size = 9, color = Color(0xFF32651D))
                }
                if (done) {
                    Surface(modifier = Modifier.align(Alignment.End).height(22.4.dp),
                        color = Color(0xFFE3EBB0), shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFA9B666))) {
                        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                            QuestText("✓ Completed", color = Color(0xFF32651D), size = 11)
                        }
                    }
                } else {
                    Button(
                        onClick = { onCompleteQuest(quest.id) },
                        modifier = Modifier.align(Alignment.End).height(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF193F4E)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) { QuestText("Mark Complete", color = Color(0xFFFFF5D9), size = 11) }
                }
            }
        }
    }
}

@Composable
private fun GreenThumbPanel(uiState: AgriQuestUiState) {
    val target = uiState.targetStreakDays.coerceAtLeast(1)
    val days = uiState.streakDays.coerceIn(0, target)
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF193F4E)),
        border = BorderStroke(1.dp, Color(0xFFC9A86A))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuestText("★ Green Thumb", color = Color(0xFFFFDF6D), pixel = true, size = 12)
                QuestText("$days / $target days", color = Color(0xFFFFF5D9), size = 11)
            }
            QuestText("Complete all daily quests $target days in a row.",
                color = Color(0xFFADC3C8), size = 11, weight = FontWeight.SemiBold)
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
