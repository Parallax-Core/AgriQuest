package com.agriquest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriquest.domain.model.AgroQuizQuestion
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Amber100
import com.agriquest.ui.theme.Amber600
import com.agriquest.ui.theme.Emerald100
import com.agriquest.ui.theme.Emerald50
import com.agriquest.ui.theme.Emerald600
import com.agriquest.ui.theme.Emerald700
import com.agriquest.ui.theme.PureWhite
import com.agriquest.ui.theme.Slate100
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate300
import com.agriquest.ui.theme.Slate600
import com.agriquest.ui.theme.Slate700
import com.agriquest.ui.theme.Slate800
import com.agriquest.ui.theme.Slate900

val Red100 = Color(0xFFFEE2E2)
val Red600 = Color(0xFFDC2626)
val Red700 = Color(0xFFB91C1C)

/**
 * Daily Agro-Quiz UI Card Component for AgriQuest Mobile.
 *
 * Displays:
 * 1. Title "🧠 Daily Agro-Quiz" with XP reward badge.
 * 2. Active agriculture question text.
 * 3. 4 clickable option buttons.
 * 4. Correct/Incorrect answer state with explanation and gold trophy completed badge.
 * 5. "Next Question 🔄" shuffle option for testing.
 */
@Composable
fun DailyQuizCard(
    modifier: Modifier = Modifier,
    question: AgroQuizQuestion = AgroQuizQuestion.QUIZ_POOL.first(),
    @Suppress("UNUSED_PARAMETER") isQuizCompletedToday: Boolean = false,
    selectedOptionIndex: Int = -1,
    isAnswerCorrect: Boolean? = null,
    onAnswerSelected: (Int) -> Unit = {},
    onNextQuestion: () -> Unit = {}
) {
    val hasAnswered = selectedOptionIndex >= 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Title & XP Reward Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🧠 Daily Agro-Quiz",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Slate900
                        )
                    )
                }

                // Reward Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Emerald100
                ) {
                    Text(
                        text = "+${question.xpReward} XP",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = Emerald700
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Answer Result / Trophy Banner (if answered)
            if (hasAnswered) {
                if (isAnswerCorrect == true) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Amber100,
                        border = BorderStroke(1.dp, Amber600.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Amber600,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.EmojiEvents,
                                        contentDescription = "Completed Trophy",
                                        tint = PureWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "🎉 Correct! +30 XP Awarded!",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Amber600
                                    )
                                )
                                Text(
                                    text = "Great job keeping your plant knowledge sharp!",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                )
                            }
                        }
                    }
                } else if (isAnswerCorrect == false) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Red100,
                        border = BorderStroke(1.dp, Red600),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "❌ Incorrect! Review explanation below.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Red700
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Question Text
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Slate900,
                    lineHeight = 22.sp
                )
            )

            // Option Buttons List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                question.options.forEachIndexed { index, optionText ->
                    val isSelected = selectedOptionIndex == index
                    val isCorrectOption = index == question.correctOptionIndex

                    val backgroundColor = when {
                        hasAnswered && isCorrectOption -> Emerald100
                        hasAnswered && isSelected -> Red100
                        else -> PureWhite
                    }

                    val borderColor = when {
                        hasAnswered && isCorrectOption -> Emerald600
                        hasAnswered && isSelected -> Red600
                        else -> Slate200
                    }

                    val textColor = when {
                        hasAnswered && isCorrectOption -> Emerald700
                        hasAnswered && isSelected -> Red700
                        else -> Slate800
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = backgroundColor,
                        border = BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !hasAnswered) {
                                onAnswerSelected(index)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (hasAnswered && isCorrectOption) Emerald600 else if (hasAnswered && isSelected) Red600 else Slate100,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = ('A' + index).toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (hasAnswered && (isCorrectOption || isSelected)) PureWhite else Slate600
                                            )
                                        )
                                    }
                                }

                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (hasAnswered && (isCorrectOption || isSelected)) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = textColor
                                    )
                                )
                            }

                            if (hasAnswered) {
                                if (isCorrectOption) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Correct Answer",
                                        tint = Emerald600,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Cancel,
                                        contentDescription = "Incorrect Answer",
                                        tint = Red600,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Explanation Section (shown if answered)
            if (hasAnswered) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Emerald50,
                    border = BorderStroke(1.dp, Emerald100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "💡 Explanation & Agro-Insight",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Emerald700
                            )
                        )

                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = Slate700
                            )
                        )
                    }
                }
            }

            // Prominent "Answer Another Question 🔄" Action Button (shown when answered) or Shuffle/Next Question Action Button
            if (hasAnswered) {
                Button(
                    onClick = onNextQuestion,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald600,
                        contentColor = PureWhite
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Answer Another Question",
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Answer Another Question 🔄",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PureWhite
                        )
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onNextQuestion,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Slate300),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Next Question",
                            tint = Slate600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Next Question 🔄",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Slate700
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DailyQuizCardPreview() {
    AgriQuestTheme {
        DailyQuizCard(
            modifier = Modifier.padding(16.dp)
        )
    }
}
