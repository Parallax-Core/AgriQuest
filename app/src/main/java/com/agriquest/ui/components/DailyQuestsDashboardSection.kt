package com.agriquest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agriquest.domain.model.Quest
import com.agriquest.ui.components.AppCard
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.AppLayout
import com.agriquest.ui.theme.AppTextColor
import com.agriquest.ui.theme.Emerald100
import com.agriquest.ui.theme.Emerald50
import com.agriquest.ui.theme.Emerald600
import com.agriquest.ui.theme.Emerald700
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate500
import com.agriquest.ui.theme.Slate900

/**
* Daily Quests Dashboard Section for AgriQuest Mobile.
*
* Displays Title `🎯 Daily Quests` with completion counter, subtitle *"Complete your daily plant care tasks"*,
* task checklist items, and `View All Quests ➔` action button.
*/
@Composable
fun DailyQuestsDashboardSection(
    modifier: Modifier = Modifier,
    quests: List<Quest> = Quest.DEFAULT_QUESTS,
    onClaimQuest: (String) -> Unit = {},
    onViewAllQuestsClick: () -> Unit = {}
) {
    val completedCount = quests.count { it.isClaimed || it.isCompleted }
    val totalCount = quests.size.coerceAtLeast(1)

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(AppLayout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing)
        ) {
            // Header Row: Title `🎯 Daily Quests` & Counter `2/3 completed`
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "🎯 Daily Quests",
                        style = MaterialTheme.typography.titleMedium.copy(color = AppTextColor)
                    )
                    Text(
                        text = "Complete your daily plant care tasks",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Emerald50,
                    modifier = Modifier.border(1.dp, Emerald100, CircleShape)
                ) {
                    Text(
                        text = "$completedCount/$totalCount completed",
                        style = MaterialTheme.typography.labelSmall.copy(color = Emerald700),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Task Items List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quests.forEach { quest ->
                    val isDone = quest.isClaimed || quest.isCompleted
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDone) Emerald50 else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDone) Emerald100 else Slate200),
                        modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!quest.isClaimed) {
                                onClaimQuest(quest.id)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = quest.title,
                                style = MaterialTheme.typography.bodyMedium.copy(color = if (isDone) Emerald700 else Slate900),
                                modifier = Modifier.weight(1f)
                            )

                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Done",
                                    tint = Emerald600,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = "Pending",
                                    tint = Slate500,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Action Button: `View All Quests ➔`
            Surface(
                modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewAllQuestsClick() },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Row(
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View All Quests",
                        style = MaterialTheme.typography.labelLarge.copy(color = AppTextColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "View All",
                        tint = Slate900,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DailyQuestsDashboardSectionPreview() {
    AgriQuestTheme {
        DailyQuestsDashboardSection(
            modifier = Modifier.padding(AppLayout.cardPadding)
        )
    }
}
