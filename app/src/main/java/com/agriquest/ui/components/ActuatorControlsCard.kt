package com.agriquest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.agriquest.domain.model.ActuatorState
import com.agriquest.domain.model.FanMode
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Emerald50
import com.agriquest.ui.theme.Emerald600
import com.agriquest.ui.theme.Emerald700
import com.agriquest.ui.theme.PureWhite
import com.agriquest.ui.theme.Slate100
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate700
import com.agriquest.ui.theme.Slate900

/**
 * Controls Section Card allowing live IoT interactive pump triggers
 * and fan mode cycling (AUTO -> ON -> OFF -> AUTO).
 */
@Composable
fun ActuatorControlsCard(
    modifier: Modifier = Modifier,
    actuatorState: ActuatorState = ActuatorState(),
    onTriggerPump: () -> Unit = {},
    onToggleFanMode: () -> Unit = {}
) {
    val darkTealColor = Color(0xFF0F766E)

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title Header Row: "👤 Controls"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = "Controls Icon",
                        tint = darkTealColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "👤 Controls",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Slate900
                        )
                    )
                }

                // Status Badge Chip
                val statusText = if (actuatorState.isPumpActive) "PUMP ACTIVE" else "READY"
                val statusBg = if (actuatorState.isPumpActive) Emerald50 else Slate100
                val statusColor = if (actuatorState.isPumpActive) Emerald700 else Slate700

                Surface(
                    shape = CircleShape,
                    color = statusBg,
                    modifier = Modifier.border(1.dp, if (actuatorState.isPumpActive) Emerald600 else Slate200, CircleShape)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = statusColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Controls Buttons Layout: 2 dark teal/blue rounded buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Trigger Pump (3s)
                val pumpButtonText = if (actuatorState.isPumpActive) {
                    "Pumping... (${actuatorState.pumpRemainingSeconds}s)"
                } else {
                    "Trigger Pump (3s)"
                }

                Button(
                    onClick = onTriggerPump,
                    enabled = !actuatorState.isPumpActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = darkTealColor,
                        contentColor = PureWhite,
                        disabledContainerColor = darkTealColor.copy(alpha = 0.6f),
                        disabledContentColor = PureWhite
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (actuatorState.isPumpActive) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.WaterDrop,
                                contentDescription = "Water Pump",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pumpButtonText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                // Button 2: Toggle Fan: AUTO
                val fanModeText = "Toggle Fan: ${actuatorState.fanMode.name}"

                Button(
                    onClick = onToggleFanMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0288D1),
                        contentColor = PureWhite
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Air,
                            contentDescription = "Fan Control",
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = fanModeText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
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
fun ActuatorControlsCardPreview() {
    AgriQuestTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActuatorControlsCard(
                actuatorState = ActuatorState(
                    isPumpActive = false,
                    pumpRemainingSeconds = 0,
                    fanMode = FanMode.AUTO
                )
            )
        }
    }
}
