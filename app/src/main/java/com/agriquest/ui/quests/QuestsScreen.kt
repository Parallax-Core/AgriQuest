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
import com.agriquest.ui.state.AgriQuestUiState
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

/**
 * Daily Quests Screen for AgriQuest Mobile.
 *
 * Displays:
 * 1. Clean serif typography header with cream background.
 * 2. Available XP Gold/Amber banner ("Available Today +225 XP").
 * 3. Plant Care Quests section with 3 specific quest cards ("Prune Lower Leaves", "Deep Watering Session", "Stake the Main Stem").
 * 4. "Mark Complete" action button that completes the quest, awards XP, checks for level up, persists state, and emits notifications.
 */
@Composable
fun QuestsScreen(
    modifier: Modifier = Modifier,
    viewModel: AgriQuestViewModel = viewModel(),
    onTabSelected: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val dailyQuestsList = uiState.quests
    val totalXpToday = dailyQuestsList.sumOf { it.xpReward }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFAF6EE),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            AgriQuestBottomNav(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                streakDays = uiState.plant.streakDays,
                targetStreakDays = uiState.plant.targetStreakDays
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFAF6EE)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Clean Serif Typography Header on Cream Background
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "DAILY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                            color = Color(0xFF8B7E66)
                        )
                    )
                    Text(
                        text = "Quests",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = Slate900
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 2. Gold/Amber Gradient Available XP Banner
                    AvailableXpBanner(totalXpAvailable = totalXpToday)

                    // 3. Prominent "Simulate Next Day 🌅 (Reset Quests)" Action Button
                    Button(
                        onClick = onSimulateNextDay,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEF3C7)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Simulate Next Day 🌅 (Reset Quests)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFB45309)
                            )
                        )
                    }

                    // 4. Plant Care Quests Section Title
                    Text(
                        text = "Plant Care Quests",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate900
                        )
                    )

                    // 5. Render Quest Cards
                    dailyQuestsList.forEach { quest ->
                        PlantCareQuestCard(
                            quest = quest,
                            onCompleteQuest = onCompleteQuest
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * Gold / Amber Gradient Banner displaying available daily XP with trophy icon.
 */
@Composable
private fun AvailableXpBanner(
    totalXpAvailable: Int = 225
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF59E0B),
                            Color(0xFFD97706)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Available Today +${totalXpAvailable} XP",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = PureWhite
                        )
                    )
                    Text(
                        text = "Complete all 3 quests",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = PureWhite.copy(alpha = 0.88f)
                        )
                    )
                }

                // Star Trophy Icon Badge
                Surface(
                    shape = CircleShape,
                    color = PureWhite.copy(alpha = 0.22f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Star Trophy",
                            tint = PureWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Plant Care Quest Card with difficulty pill, reward badge, subtext, progress bar, and complete button.
 */
@Composable
private fun PlantCareQuestCard(
    quest: Quest,
    onCompleteQuest: (String) -> Unit
) {
    val isCompletedOrClaimed = quest.isClaimed || quest.isCompleted

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Difficulty Pill & Reward Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Difficulty Pill
                    val isEasy = quest.difficulty.equals("Easy", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isEasy) Emerald100 else Amber100
                    ) {
                        Text(
                            text = quest.difficulty,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isEasy) Emerald700 else Amber600
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Slate500,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // XP Reward Text
                    Text(
                        text = quest.xpRewardText,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Emerald700
                        )
                    )
                }
            }

            // Quest Title
            Text(
                text = quest.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Slate900
                )
            )

            // Quest Subtext Details
            if (quest.subtext.isNotEmpty()) {
                Text(
                    text = quest.subtext,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Slate600,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )
            }

            // Progress Bar Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate500,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = if (isCompletedOrClaimed && quest.progressText.contains("Goal")) "Goal Achieved ✓" else quest.progressText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isCompletedOrClaimed) Emerald700 else Slate900,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }

                val targetProgress = if (isCompletedOrClaimed) 1.0f else quest.progress.coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { targetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Emerald600,
                    trackColor = Emerald100
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Action Button: "Mark Complete" or "Claimed ✓"
            if (quest.isClaimed) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Emerald50,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.dp, Emerald100, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Claimed",
                            tint = Emerald600,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = "Claimed ✓",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Emerald700
                            )
                        )
                    }
                }
            } else {
                Button(
                    onClick = { onCompleteQuest(quest.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "Mark Complete",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PureWhite
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun QuestsScreenPreview() {
    AgriQuestTheme {
        QuestsScreenContent()
    }
}
