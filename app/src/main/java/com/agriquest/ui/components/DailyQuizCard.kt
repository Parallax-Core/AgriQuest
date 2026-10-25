package com.agriquest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.agriquest.domain.model.AgroQuizQuestion
import com.agriquest.ui.theme.*

@Composable
fun DailyQuizCard(question: AgroQuizQuestion = AgroQuizQuestion.QUIZ_POOL.first(),
    isQuizCompletedToday: Boolean = false, selectedOptionIndex: Int = -1,
    isAnswerCorrect: Boolean? = null, onAnswerSelected: (Int) -> Unit = {},
    onNextQuestion: () -> Unit = {}, modifier: Modifier = Modifier) {
    val questionIndex = AgroQuizQuestion.QUIZ_POOL.indexOfFirst { it.id == question.id }.coerceAtLeast(0)
    PixelPanel(modifier) {
        PixelHeading("Tomato Quiz", trailing = { PixelChip("+${question.xpReward} XP", AppHighlightBackground, AppTextColor) })
        Text("Question ${questionIndex + 1} of ${AgroQuizQuestion.QUIZ_POOL.size}",
            style = MaterialTheme.typography.labelSmall, color = AppTextColor)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            AgroQuizQuestion.QUIZ_POOL.forEachIndexed { index, _ ->
                PixelProgress(if (index <= questionIndex) 1f else 0f, Modifier.weight(1f), color = Color(0xFF649B32))
            }
        }
        Text(question.questionText, style = MaterialTheme.typography.bodyMedium, color = AppTextColor)
        question.options.forEachIndexed { index, option ->
            val selected = index == selectedOptionIndex
            val correct = selected && isAnswerCorrect == true
            val fill = if (correct) PixelPaleGreen else if (selected && isAnswerCorrect == false) Color(0xFFFFDDD0) else PixelTan
            Surface(shape = RoundedCornerShape(3.dp), color = fill,
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (correct) PixelGreen else AppCardBorder),
                modifier = Modifier.fillMaxWidth().clickable(enabled = !isQuizCompletedToday) { onAnswerSelected(index) }) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    PixelChip(('A' + index).toString(), if (correct) PixelGreen else AppCardBackground,
                        if (correct) AppCardBackground else AppTextColor)
                    Text(option, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = AppTextColor)
                    if (selected) Text(if (correct) "✓" else "×", color = if (correct) PixelGreen else Color(0xFFB53220))
                }
            }
        }
        if (isQuizCompletedToday) {
            Surface(shape = RoundedCornerShape(3.dp), color = if (isAnswerCorrect == true) PixelPaleGreen else PixelTan,
                border = BorderStroke(1.dp, AppCardBorder)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(if (isAnswerCorrect == true) "CORRECT! +${question.xpReward} XP" else "Daily quiz completed. Try again tomorrow.",
                        style = MaterialTheme.typography.titleMedium, color = PixelGreen)
                    Text(question.explanation, style = MaterialTheme.typography.bodySmall, color = AppTextColor)
                }
            }
            Text("Next quiz reward available tomorrow.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
