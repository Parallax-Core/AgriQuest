package com.agriquest.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriquest.domain.model.Quest
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Emerald100
import com.agriquest.ui.theme.Emerald50
import com.agriquest.ui.theme.Emerald600
import com.agriquest.ui.theme.Emerald700
import com.agriquest.ui.theme.PureWhite
import com.agriquest.ui.theme.Slate100
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate500
import com.agriquest.ui.theme.Slate700
import com.agriquest.ui.theme.Slate900
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Active Quest Banner & Mission List Component displaying active plant care quests with progress fill bars,
 * XP reward buttons ("CLAIM +50 XP"), claim reward animations, and unlock status badges.
 */
@Composable
fun QuestBanner(
    modifier: Modifier = Modifier,
    quest: Quest = Quest(),
    quests: List<Quest> = emptyList(),
    onClaimQuest: (String) -> Unit = {}
) {
    val questListToDisplay = if (quests.isNotEmpty()) quests else listOf(quest)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = "Quest Icon",
                        tint = Emerald600,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ACTIVE QUESTS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp,
                            color = Slate700
                        )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Emerald50,
                    modifier = Modifier.border(1.dp, Emerald100, CircleShape)
                ) {
                    val unclaimedCount = questListToDisplay.count { !it.isClaimed }
                    Text(
                        text = "$unclaimedCount Active",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Emerald700
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Display List of Active Quests
            questListToDisplay.forEach { activeQuest ->
                QuestItemCard(
                    quest = activeQuest,
                    onClaimQuest = onClaimQuest
                )
            }
        }
    }
}

/**
 * Individual Quest Card with title, progress bar, XP reward pill / claim button with animation effect.
 */
@Composable
private fun QuestItemCard(
    quest: Quest,
    onClaimQuest: (String) -> Unit
) {
    var isPressedAnimation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressedAnimation) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ClaimButtonScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quest Title & Claim Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = quest.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate900,
                    lineHeight = 18.sp
                ),
                modifier = Modifier.weight(1f)
            )

            // Action Button or Badge
            if (quest.isCompleted && !quest.isClaimed) {
                Button(
                    onClick = {
                        scope.launch {
                            isPressedAnimation = true
                            delay(150L)
                            isPressedAnimation = false
                            onClaimQuest(quest.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .scale(buttonScale)
                ) {
                    Text(
                        text = "CLAIM ${quest.xpRewardText}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = PureWhite
                        )
                    )
                }
            } else if (quest.isClaimed) {
                Surface(
                    shape = CircleShape,
                    color = Emerald50,
                    modifier = Modifier.border(1.dp, Emerald100, CircleShape)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Claimed",
                            tint = Emerald600,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "CLAIMED",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = Emerald700
                            )
                        )
                    }
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = Emerald50,
                    modifier = Modifier.border(1.dp, Emerald100, CircleShape)
                ) {
                    Text(
                        text = quest.xpRewardText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = Emerald700
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Progress Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    text = "${quest.progressPercent}% Completed",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (quest.isCompleted) Emerald700 else Slate900,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }

            LinearProgressIndicator(
                progress = { quest.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Emerald600,
                trackColor = Emerald100
            )
        }

        // Subtext Badge Info
        if (quest.subtextBadge.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate100,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Unlock Info",
                        tint = Slate700,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = quest.subtextBadge,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = Slate700
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
fun QuestBannerPreview() {
    AgriQuestTheme {
        QuestBanner(
            quests = Quest.DEFAULT_QUESTS
        )
    }
}
