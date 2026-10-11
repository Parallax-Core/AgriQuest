package com.agriquest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriquest.R
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Amber500
import com.agriquest.ui.theme.PureWhite

/**
 * Tab definitions for AgriQuest Persistent Bottom Navigation Bar.
 */
enum class AgriQuestTab(
    val title: String,
    val icon: ImageVector,
    val drawableRes: Int? = null
) {
    HOME("Home", Icons.Rounded.Home, R.drawable.home),
    PLANT("Plant", Icons.Rounded.LocalFlorist, R.drawable.plant),
    QUESTS("Quests", Icons.AutoMirrored.Rounded.Assignment, R.drawable.quests),
    AI_SCAN("AI", Icons.Rounded.SmartToy, R.drawable.ai),
    AWARDS("Awards", Icons.Rounded.EmojiEvents)
}

/**
 * Persistent Bottom Navigation Bar for AgriQuest Mobile.
 * Blue background (`0xFF1B3C4D`) with yellow selected images.
 */
@Composable
fun AgriQuestBottomNav(
    modifier: Modifier = Modifier,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    streakDays: Int = 0,
    targetStreakDays: Int = 7
) {
    val isAiScanLocked = streakDays < targetStreakDays
    val navBarColor = Color(0xFF1B3C4D)
    val unselectedIconColor = Color(0xFF9CA3AF)
    val selectedImageColor = Color(0xFFB8860B)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = navBarColor,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, navBarColor)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp),
                containerColor = navBarColor,
                contentColor = PureWhite,
                tonalElevation = 0.dp
            ) {
                AgriQuestTab.entries.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    val isLockedTab = tab == AgriQuestTab.AI_SCAN && isAiScanLocked

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onTabSelected(index) },
                        icon = {
                            if (isLockedTab) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = Amber500,
                                            contentColor = PureWhite,
                                            modifier = Modifier.size(14.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Lock,
                                                contentDescription = "Locked",
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                ) {
                                    NavigationTabImage(tab, isSelected, selectedImageColor)
                                }
                            } else {
                                NavigationTabImage(tab, isSelected, selectedImageColor)
                            }
                        },
                        label = if (tab.drawableRes == null) {
                            {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = if (isSelected) selectedImageColor else unselectedIconColor
                                    )
                                )
                            }
                        } else null,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = selectedImageColor,
                            selectedTextColor = selectedImageColor,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = unselectedIconColor,
                            unselectedTextColor = unselectedIconColor
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationTabImage(
    tab: AgriQuestTab,
    isSelected: Boolean,
    selectedImageColor: Color
) {
    val drawableRes = tab.drawableRes
    if (drawableRes != null) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = tab.title,
            modifier = Modifier.size(width = 40.dp, height = 48.dp),
            contentScale = ContentScale.Fit,
            colorFilter = if (isSelected) ColorFilter.tint(selectedImageColor) else null
        )
    } else {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.title,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AgriQuestBottomNavPreview() {
    AgriQuestTheme {
        AgriQuestBottomNav(
            selectedTab = 0,
            streakDays = 0,
            targetStreakDays = 7
        )
    }
}
