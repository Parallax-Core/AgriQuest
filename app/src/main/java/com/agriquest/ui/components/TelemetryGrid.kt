package com.agriquest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agriquest.R
import com.agriquest.domain.model.TelemetryData
import com.agriquest.ui.components.AppCard
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.AppLayout
import com.agriquest.ui.theme.AppTextColor
import com.agriquest.ui.theme.Slate500
import com.agriquest.ui.theme.Slate900

/**
* Environment Conditions Section displaying Title `🍃 Environment Conditions`,
* Grouped container and 4 telemetry cards using pixel-art metric icons & state chips.
*/
@Composable
fun TelemetryGrid(
    modifier: Modifier = Modifier,
    telemetry: TelemetryData = TelemetryData(),
    showTitle: Boolean = true
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppLayout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing)
        ) {
            // Environment section title
            if (showTitle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🍃 Environment Conditions",
                        style = MaterialTheme.typography.titleMedium.copy(color = AppTextColor)
                    )

                }
            }

            // Shared two-column layout keeps metric labels readable on every page.
            Row(
                modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(AppLayout.compactSpacing)
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

            }
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(AppLayout.compactSpacing)
            ) {
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
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(AppLayout.compactSpacing),
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
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                )
                Text(
                    text = reading,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge.copy(color = AppTextColor)
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
                    Text(text = statusLabel,
                        modifier = Modifier.padding(horizontal = 2.dp),
                        color = when (statusBackgroundRes) {
                            R.drawable.green -> Color(0xFF1B5C28)
                            R.drawable.red -> Color(0xFF9A4A38)
                            R.drawable.blue -> Color(0xFF37606C)
                            else -> Slate900
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium)
                }
            }

            // Bottom: Target Range Text
            Text(
                text = targetRange,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
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
            modifier = Modifier.padding(AppLayout.cardPadding)
        )
    }
}
