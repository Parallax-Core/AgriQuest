package com.agriquest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriquest.R
import com.agriquest.domain.model.ConnectionState
import com.agriquest.domain.model.ConnectionStatus
import com.agriquest.domain.model.PlantSource
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Emerald600
import com.agriquest.ui.theme.PureWhite
import com.agriquest.ui.theme.Slate100
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate700
import com.agriquest.ui.theme.Slate900

/**
 * Sky Blue Header Component for AgriQuest Mobile.
 *
 * Features:
 * - Soft Sky Blue background (`0xFFE0F2FE`).
 * - Logo image `aqlogo.png` & Settings gear icon `⚙`.
 * - Speech bubble (`"Your tomato is looking healthy!"`).
 * - Level Progress Bar (`Lv. 3`, `120 / 250 XP`).
 * - Plant source dropdown selector & ESP32 connection badge.
 */
@Composable
fun TopLevelBanner(
    modifier: Modifier = Modifier,
    userLevel: String = "Lvl. 3 Harvest Master",
    userLevelInt: Int = 3,
    currentXp: Int = 120,
    maxXp: Int = 250,
    streakDays: Int = 0,
    targetStreakDays: Int = 7,
    speechBubble: String = "Your tomato is looking healthy!",
    connectionState: ConnectionState = ConnectionState(),
    selectedPlantSource: PlantSource = PlantSource.EMULATED_OFFLINE,
    onPlantSourceSelected: (PlantSource) -> Unit = {},
    onScanForEsp32: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val xpProgress = if (maxXp > 0) (currentXp.toFloat() / maxXp.toFloat()).coerceIn(0f, 1f) else 0f
    var dropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFBAE6FD))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: `aqlogo.png` logo & Settings Gear Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo Image
                Image(
                    painter = painterResource(id = R.drawable.aqlogo),
                    contentDescription = "AgriQuest Logo",
                    modifier = Modifier.height(52.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Care Streak Badge
                    CareStreakBadge(
                        streakDays = streakDays,
                        targetStreakDays = targetStreakDays
                    )

                    // Settings Gear Button ⚙
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(36.dp)
                            .background(PureWhite, CircleShape)
                            .border(1.dp, Color(0xFFBAE6FD), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings Gear",
                            tint = Color(0xFF0369A1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Speech Bubble Box Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = PureWhite
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI Speech",
                        tint = Emerald600,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = speechBubble,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate900,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            // Level Progress Bar Section (`Lv. 3`, `120 / 250 XP`)
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
                        text = "Lv. $userLevelInt ($userLevel)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF0369A1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "$currentXp / $maxXp XP",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate900,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    )
                }

                // XP Progress Bar
                LinearProgressIndicator(
                    progress = { xpProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF0288D1),
                    trackColor = PureWhite
                )
            }

            // Plant Source Selector & ESP32 Status Row
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentSourceLabel = when (selectedPlantSource) {
                    PlantSource.EMULATED_OFFLINE -> "🌱 Offline Plant (Emulated)"
                    PlantSource.ESP32_HARDWARE -> "📡 ESP32 Hardware (Live IoT)"
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PureWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(12.dp))
                        .clickable { dropdownExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sensors,
                                contentDescription = "Plant Source",
                                tint = Emerald600,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Source: $currentSourceLabel",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Slate700
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = "Expand Options",
                            tint = Slate700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    PlantSource.entries.forEach { source ->
                        val itemLabel = when (source) {
                            PlantSource.EMULATED_OFFLINE -> "🌱 Offline Plant (Emulated)"
                            PlantSource.ESP32_HARDWARE -> "📡 ESP32 Hardware (Live IoT)"
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = itemLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (source == selectedPlantSource) FontWeight.Bold else FontWeight.Normal,
                                        color = if (source == selectedPlantSource) Emerald600 else Slate900
                                    )
                                )
                            },
                            onClick = {
                                dropdownExpanded = false
                                onPlantSourceSelected(source)
                            }
                        )
                    }
                }
            }

            // Scan Hardware Button if ESP32 source active
            if (selectedPlantSource == PlantSource.ESP32_HARDWARE) {
                val isScanning = connectionState.status == ConnectionStatus.SCANNING
                val buttonText = if (isScanning) "Scanning for ESP32..." else "Scan for ESP32 Hardware"

                Button(
                    onClick = onScanForEsp32,
                    enabled = !isScanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald600,
                        contentColor = PureWhite,
                        disabledContainerColor = Emerald600.copy(alpha = 0.7f),
                        disabledContentColor = PureWhite
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.BluetoothSearching,
                                contentDescription = "Scan for ESP32",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
fun TopLevelBannerPreview() {
    AgriQuestTheme {
        TopLevelBanner()
    }
}
