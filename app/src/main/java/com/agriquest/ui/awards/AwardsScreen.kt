package com.agriquest.ui.awards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agriquest.domain.model.Achievement
import com.agriquest.domain.model.AchievementIcon
import com.agriquest.ui.components.AppCard
import com.agriquest.ui.components.AppPageTitle
import com.agriquest.ui.components.AppScreen
import com.agriquest.ui.components.CardTone
import com.agriquest.ui.components.CareStreakBadge
import com.agriquest.ui.state.AgriQuestUiState
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Amber500
import com.agriquest.ui.theme.AppLayout
import com.agriquest.ui.theme.AppTextColor
import com.agriquest.ui.theme.Emerald100
import com.agriquest.ui.theme.Emerald600
import com.agriquest.ui.theme.Emerald700
import com.agriquest.ui.theme.PureWhite
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate500
import com.agriquest.ui.theme.Slate900

private val GoldAccent = Color(0xFFF59E0B)
private val GoldLight = Color(0xFFFBBF24)

/**
* Achievements & Awards Screen for AgriQuest Mobile.
*
* Displays level progress, care streak hero card with streak reset control,
* quick stat chips row, and pre-populated gamification achievements list.
*/
@Composable
fun AwardsScreenContent(
    modifier: Modifier = Modifier,
    uiState: AgriQuestUiState = AgriQuestUiState(),
    selectedTab: Int = 4,
    onTabSelected: (Int) -> Unit = {},
    onResetStreak: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    AppScreen(
        modifier = modifier,
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        snackbarHostState = snackbarHostState
    ) {
        // 1. Header with Serif typography on cream background
        AwardsHeaderSection()

        // 2. Level & Care Streak Hero Card
        LevelStreakHeroCard(
            uiState = uiState,
            onResetStreak = onResetStreak
        )

        // 3. Quick Stat Chips Row (3 cards)
        QuickStatChipsRow(
            unlockedCountText = "${uiState.unlockedAchievementsCount}/${uiState.totalAchievementsCount}",
            totalXpText = uiState.unlockedXpTotal.toString(),
            rankText = "#42"
        )

        // 4. Achievements List Section
        AchievementsListSection(
            achievements = uiState.achievements
        )

        Spacer(modifier = Modifier.height(16.dp))

    }
}

/**
* Header section with "PROGRESS" subtitle and "Achievements" title in serif typography.
*/
@Composable
private fun AwardsHeaderSection() {
    AppCard(Modifier.fillMaxWidth()) {
        AppPageTitle("Achievements", "Progress", Modifier.padding(AppLayout.cardPadding))
    }
}

/**
* Hero Card with Dark Emerald background, Gold Trophy, Level badge, XP bar, Streak counter,
* and Action button to reset care streak to 0 days.
*/
@Composable
private fun LevelStreakHeroCard(
    uiState: AgriQuestUiState,
    onResetStreak: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth(),
        tone = CardTone.ACCENT) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(AppLayout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing)
                ) {
                    // Gold Trophy Icon Container
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = GoldAccent.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, GoldLight)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = "Gold Trophy",
                                tint = GoldLight,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(Modifier.weight(1f)) {
                        // Level Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GoldLight,
                            contentColor = Slate900
                        ) {
                            Text(
                                text = "LEVEL ${uiState.plant.levelInt}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = uiState.plant.level,
                            style = MaterialTheme.typography.bodyMedium.copy(color = PureWhite)
                        )
                    }
                }

                // Care Streak Badge
                CareStreakBadge(
                    streakDays = uiState.plant.streakDays,
                    targetStreakDays = uiState.plant.targetStreakDays
                )
            }

            // XP Progress Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "XP Progress",
                        style = MaterialTheme.typography.bodySmall.copy(color = PureWhite.copy(alpha = 0.8f))
                    )
                    Text(
                        text = "${uiState.plant.currentXp} / ${uiState.plant.maxXp} XP",
                        style = MaterialTheme.typography.bodySmall.copy(color = GoldLight)
                    )
                }

                LinearProgressIndicator(
                    progress = { uiState.plant.xpProgress },
                    modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                    color = GoldLight,
                    trackColor = PureWhite.copy(alpha = 0.2f),
                )
            }

            // Action Button: Reset Streak to 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onResetStreak,
                    shape = AppLayout.buttonShape,
                    border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PureWhite
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset Streak",
                        modifier = Modifier.size(14.dp),
                        tint = PureWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset Streak to 0",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
* Quick Stat Chips Row displaying 3 cards:
* 1. Unlocked count (e.g. "3/6")
* 2. Total XP earned (e.g. "375")
* 3. Rank (e.g. "#42")
*/
@Composable
private fun QuickStatChipsRow(
    unlockedCountText: String,
    totalXpText: String,
    rankText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickStatCard(
            modifier = Modifier.weight(1f),
            value = unlockedCountText,
            label = "Unlocked"
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            value = totalXpText,
            label = "Total XP"
        )
        QuickStatCard(
            modifier = Modifier.weight(1f),
            value = rankText,
            label = "Rank"
        )
    }
}

@Composable
private fun QuickStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(color = Emerald700)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
            )
        }
    }
}

/**
* Section displaying list of Achievements matching design specs.
*/
@Composable
private fun AchievementsListSection(
    achievements: List<Achievement>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Achievements",
            style = MaterialTheme.typography.titleMedium.copy(color = AppTextColor)
        )

        achievements.forEach { achievement ->
            AchievementCard(achievement = achievement)
        }
    }
}

/**
* Individual Achievement Card item.
*/
@Composable
private fun AchievementCard(
    achievement: Achievement
) {
    val icon = when (achievement.iconType) {
        AchievementIcon.PLANT -> Icons.Rounded.Eco
        AchievementIcon.WATER -> Icons.Rounded.InvertColors
        AchievementIcon.SUN -> Icons.Rounded.WbSunny
        AchievementIcon.HARVEST -> Icons.Rounded.Agriculture
        AchievementIcon.IOT -> Icons.Rounded.Memory
        AchievementIcon.STREAK -> Icons.Rounded.LocalFireDepartment
    }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
            .fillMaxWidth()
            .padding(AppLayout.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing)
        ) {
            // Icon Badge Container
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (achievement.isUnlocked) Emerald100 else Slate200,
                contentColor = if (achievement.isUnlocked) Emerald600 else Slate500
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (achievement.isUnlocked) icon else Icons.Rounded.Lock,
                        contentDescription = achievement.title,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Achievement details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.bodyMedium.copy(color = if (achievement.isUnlocked) Slate900 else Slate500)
                    )

                    // Unlocked / Locked Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (achievement.isUnlocked) Emerald100 else Slate200
                    ) {
                        Text(
                            text = if (achievement.isUnlocked) "Unlocked" else "Locked",
                            style = MaterialTheme.typography.labelSmall.copy(color = if (achievement.isUnlocked) Emerald700 else Slate500),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = AppTextColor)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = achievement.earnedDateText,
                        style = MaterialTheme.typography.labelSmall.copy(color = if (achievement.isUnlocked) Emerald700 else Slate500)
                    )

                    // XP Reward Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Amber500.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = achievement.xpRewardText,
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFD97706)),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AwardsScreenPreview() {
    AgriQuestTheme {
        AwardsScreenContent()
    }
}
