package com.agriquest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriquest.R
import com.agriquest.domain.model.TelemetryData
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Emerald200
import com.agriquest.ui.theme.Emerald50
import com.agriquest.ui.theme.Emerald700
import com.agriquest.ui.theme.PureWhite
import com.agriquest.ui.theme.Slate500
import com.agriquest.ui.theme.Slate900

/**
 * Environment Conditions Section displaying Title `🍃 Environment Conditions`,
 * ESP32 connection badge chip, and 4 telemetry cards using pixel-art metric icons & state chips.
 */
@Composable
fun TelemetryGrid(
    modifier: Modifier = Modifier,
    telemetry: TelemetryData = TelemetryData()
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header Row: Title & ESP32 Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🍃 Environment Conditions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Slate900
                )
            )

            // Green ESP32 Connection Badge
            Surface(
                shape = CircleShape,
                color = Emerald50,
                modifier = Modifier.border(1.dp, Emerald200, CircleShape)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF22C55E), CircleShape)
                    )
                    Text(
                        text = "📡 ESP32",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Emerald700
                        )
                    )
                }
            }
        }

        // 2x2 Environment Telemetry Cards Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Soil Moisture & Temperature
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Soil Moisture
                val moistureStateRes = if (telemetry.soilMoisture < 30.0f) {
                    R.drawable.temp_state_cold
                } else {
                    R.drawable.temp_state_good
                }
                EnvironmentMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Soil Moisture",
                    reading = "${telemetry.soilMoisture.toInt()}%",
                    targetRange = "Target: 60-80%",
                    iconRes = R.drawable.soil_moisture,
                    stateChipRes = moistureStateRes
                )

                // Card 2: Air Temperature
                val tempIconRes = when {
                    telemetry.airTemperature < 15.0f -> R.drawable.temp_cold
                    telemetry.airTemperature > 30.0f -> R.drawable.temp_high
                    telemetry.airTemperature in 28.0f..30.0f -> R.drawable.temp_warm
                    else -> R.drawable.temp_good
                }
                val tempStateRes = when {
                    telemetry.airTemperature < 15.0f -> R.drawable.temp_state_cold
                    telemetry.airTemperature > 30.0f -> R.drawable.temp_state_high
                    telemetry.airTemperature in 28.0f..30.0f -> R.drawable.temp_state_warm
                    else -> R.drawable.temp_state_good
                }
                EnvironmentMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Temperature",
                    reading = "${telemetry.airTemperature.toInt()}°C",
                    targetRange = "Target: 21-28°C",
                    iconRes = tempIconRes,
                    stateChipRes = tempStateRes
                )
            }

            // Row 2: Relative Humidity & Light Intensity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 3: Humidity
                EnvironmentMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Humidity",
                    reading = "${telemetry.relativeHumidity.toInt()}%",
                    targetRange = "Target: 50-70%",
                    iconRes = R.drawable.humidity,
                    stateChipRes = R.drawable.temp_state_good
                )

                // Card 4: Light
                val lightReading = if (telemetry.lightIntensity >= 1000f) {
                    "${(telemetry.lightIntensity / 1000f).toInt()} klux"
                } else "High"
                EnvironmentMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Light",
                    reading = lightReading,
                    targetRange = "Target: >4.0 klux",
                    iconRes = R.drawable.light,
                    stateChipRes = R.drawable.temp_state_good
                )
            }
        }
    }
}

/**
 * Individual modular Environment Metric Card using pixel art icons & state chips.
 */
@Composable
fun EnvironmentMetricCard(
    title: String,
    reading: String,
    targetRange: String,
    iconRes: Int,
    stateChipRes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Pixel Art Metric Icon & Pixel Art State Chip Image
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(32.dp)
                )

                Image(
                    painter = painterResource(id = stateChipRes),
                    contentDescription = "State Chip",
                    modifier = Modifier.height(22.dp)
                )
            }

            // Middle: Title & Numerical Reading
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Slate500,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
                Text(
                    text = reading,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Slate900,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
            }

            // Bottom: Target Range Text
            Text(
                text = targetRange,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Slate500,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
fun TelemetryGridPreview() {
    AgriQuestTheme {
        TelemetryGrid(
            telemetry = TelemetryData(
                soilMoisture = 68.0f,
                airTemperature = 24.5f,
                relativeHumidity = 62.0f,
                lightIntensity = 14200.0f
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
