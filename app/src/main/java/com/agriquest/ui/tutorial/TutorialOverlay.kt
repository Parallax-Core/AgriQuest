package com.agriquest.ui.tutorial

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.agriquest.ui.components.PixelButton
import com.agriquest.ui.components.PixelChip
import com.agriquest.ui.components.PixelHeading
import com.agriquest.ui.components.PixelPanel
import com.agriquest.ui.components.PixelProgress
import com.agriquest.ui.theme.AppTextColor

/** One tutorial step. [tab] is the bottom-nav tab shown behind it (0 Home, 1 Plant, 2 Quests, 3 AI, 4 Awards). */
data class TutorialStep(val tab: Int, val title: String, val text: String)

object Tutorial {
    private const val PREFS = "agriquest_tutorial"
    private const val KEY_DONE = "done_v1"

    val STEPS = listOf(
        TutorialStep(0, "Meet your tomato", "This is your plant. It grows with your real plant's age: a seed first, then a sprout, leaves, flowers and tomatoes."),
        TutorialStep(0, "Live readings", "These numbers come from your ESP32 sensor (or the simulator): soil moisture, temperature, humidity and light."),
        TutorialStep(0, "Water, Fan and Light", "Tap Water when the soil is dry to run the pump. Fan and Light show their effect on your plant in the game."),
        TutorialStep(2, "Daily quests", "You get 3 to 5 quests a day. They complete only when the sensor detects the goal. Then tap Claim Reward for XP."),
        TutorialStep(2, "Daily quiz", "Answer one tomato quiz a day for bonus XP."),
        TutorialStep(1, "Plant details", "See growth stats, moisture history and your plant journal. You can change the sowing date here."),
        TutorialStep(3, "AI Leaf Detector", "After you log in on 7 days, scan leaves to detect diseases and see daily leaf health stats."),
        TutorialStep(4, "Awards and outfits", "Earn awards for good care, and spend points on outfits for your tomato."),
    )

    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)

    fun markDone(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply()
    }
}

/** Dimmed overlay with a step card at the bottom: Next, Back and Skip. */
@Composable
fun TutorialOverlay(step: Int, onNext: () -> Unit, onBack: () -> Unit, onSkip: () -> Unit) {
    val current = Tutorial.STEPS[step]
    val last = step == Tutorial.STEPS.lastIndex
    Box(Modifier.fillMaxSize().background(Color(0x99000000))
        .clickable(remember { MutableInteractionSource() }, indication = null) { /* block taps behind */ },
        contentAlignment = Alignment.BottomCenter) {
        PixelPanel(Modifier.widthIn(max = 420.dp).navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 84.dp)) {
            PixelHeading(current.title, trailing = { PixelChip("${step + 1}/${Tutorial.STEPS.size}") })
            Text(current.text, style = MaterialTheme.typography.bodyMedium, color = AppTextColor)
            PixelProgress((step + 1f) / Tutorial.STEPS.size)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onSkip) { Text("Skip") }
                Box(Modifier.weight(1f))
                if (step > 0) TextButton(onBack) { Text("Back") }
                PixelButton(if (last) "Let's grow!" else "Next", onNext)
            }
        }
    }
}
