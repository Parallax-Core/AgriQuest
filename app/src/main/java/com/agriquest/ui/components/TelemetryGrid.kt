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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agriquest.R
import com.agriquest.domain.model.TelemetryData
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.InterFontFamily
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

        // Four equally sized cards fit in one row without horizontal scrolling.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Card 1: Soil Moisture
            val moistureStatus = when {
                telemetry.soilMoisture < 60f -> "Too Dry" to R.drawable.red
                telemetry.soilMoisture > 80f -> "Too Wet" to R.drawable.red
                else -> "Good" to R.drawable.green
            }
            EnvironmentMetricCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = "Soil Moisture",
                reading = "${telemetry.soilMoisture.toInt()}%",
                targetRange = "Target: 60-80%",
                iconRes = R.drawable.soil_moisture,
                statusLabel = moistureStatus.first,
                statusBackgroundRes = moistureStatus.second
            )

            // Card 2: Air Temperature
            val tempIconRes = when {
                telemetry.airTemperature < 15.0f -> R.drawable.temp_cold
                telemetry.airTemperature > 30.0f -> R.drawable.temp_high
                telemetry.airTemperature in 28.0f..30.0f -> R.drawable.temp_warm
                else -> R.drawable.temp_good
            }
            val temperatureStatus = when {
                telemetry.airTemperature < 15f -> "Too Cold" to R.drawable.blue
                telemetry.airTemperature > 30f -> "Too Hot" to R.drawable.red
                else -> "Good" to R.drawable.green
            }
            EnvironmentMetricCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = "Temperature",
                reading = "${telemetry.airTemperature.toInt()}°C",
                targetRange = "Target: 21-28°C",
                iconRes = tempIconRes,
                statusLabel = temperatureStatus.first,
                statusBackgroundRes = temperatureStatus.second
            )

            // Card 3: Humidity
            val humidityStatus = when {
                telemetry.relativeHumidity < 50f -> "Too Low" to R.drawable.red
                telemetry.relativeHumidity > 70f -> "Too High" to R.drawable.blue
                else -> "Good" to R.drawable.green
            }
            EnvironmentMetricCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = "Humidity",
                reading = "${telemetry.relativeHumidity.toInt()}%",
                targetRange = "Target: 50-70%",
                iconRes = R.drawable.humidity,
                statusLabel = humidityStatus.first,
                statusBackgroundRes = humidityStatus.second
            )

            // Card 4: Light
            val lightReading = if (telemetry.lightIntensity >= 1000f) {
                "${(telemetry.lightIntensity / 1000f).toInt()} klux"
            } else "${telemetry.lightIntensity.toInt()} lux"
            val lightStatus = if (telemetry.lightIntensity < 4000f) {
                "Too Low" to R.drawable.blue
            } else {
                "Good" to R.drawable.green
            }
            EnvironmentMetricCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                title = "Light",
                reading = lightReading,
                targetRange = "Target: >4.0 klux",
                iconRes = R.drawable.light,
                statusLabel = lightStatus.first,
                statusBackgroundRes = lightStatus.second
            )
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
    statusLabel: String,
    statusBackgroundRes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Stack the icon and larger status chip to fit the narrow cards.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(32.dp)
                )


            }

            // Middle: Title & Numerical Reading
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Slate500,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                )
                Text(
                    text = reading,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Slate900,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
                Box(
                    modifier = Modifier.fillMaxWidth(0.8f).height(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = statusBackgroundRes),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.FillBounds
                    )
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 2.dp),
                        color = when (statusBackgroundRes) {
                            R.drawable.green -> Color(0xFF1B5C28)
                            R.drawable.red -> Color(0xFF9A4A38)
                            R.drawable.blue -> Color(0xFF37606C)
                            else -> Slate900
                        },
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom: Target Range Text
            Text(
                text = targetRange,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Slate500,
                    fontWeight = FontWeight.Normal,
                    fontSize = 9.sp,
                    lineHeight = 11.sp
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
                soilMoisture = 60f,
                airTemperature = 35f,
                relativeHumidity = 80f,
                lightIntensity = 2000f
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
