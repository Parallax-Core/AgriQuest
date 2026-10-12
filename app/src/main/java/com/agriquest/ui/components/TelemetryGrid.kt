package com.agriquest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.agriquest.R
import com.agriquest.domain.model.TelemetryData
import com.agriquest.ui.theme.*
import java.util.Locale

private data class SensorReading(val label: String, val value: String, val icon: Int,
    val status: String, val good: Boolean, val color: Color)

private fun TelemetryData.readings() = listOf(
    SensorReading("Soil Moisture", "${soilMoisture.toInt()}%", R.drawable.soil_moisture,
        if (soilMoisture < 60f) "Too Dry" else if (soilMoisture > 80f) "Too Wet" else "Optimal",
        soilMoisture in 60f..80f, Color(0xFFDCEBF0)),
    SensorReading("Temperature", "${airTemperature.toInt()}°C", when {
        airTemperature < 15f -> R.drawable.temp_cold
        airTemperature > 30f -> R.drawable.temp_high
        airTemperature >= 28f -> R.drawable.temp_warm
        else -> R.drawable.temp_good
    },
        if (airTemperature < 15f) "Too Cold" else if (airTemperature > 30f) "Too Hot" else "Ideal",
        airTemperature in 15f..30f, Color(0xFFFBE3C4)),
    SensorReading("Humidity", "${relativeHumidity.toInt()}%", R.drawable.humidity,
        if (relativeHumidity < 50f) "Too Low" else if (relativeHumidity > 70f) "Too High" else "Good",
        relativeHumidity in 50f..70f, Color(0xFFDFECCB)),
    SensorReading("Light", if (lightIntensity >= 1000f) String.format(Locale.US, "%.1f klux", lightIntensity / 1000f)
        else "${lightIntensity.toInt()} lux", R.drawable.light,
        if (lightIntensity < 4000f) "Too Low" else "Good", lightIntensity >= 4000f, Color(0xFFFBEFBD))
)

/** Shared metric presentation: four compact categories or two detailed columns. */
@Composable
fun TelemetryGrid(modifier: Modifier = Modifier, telemetry: TelemetryData = TelemetryData(),
    showTitle: Boolean = true, compact: Boolean = false, embedded: Boolean = false) {
    val content: @Composable ColumnScope.() -> Unit = {
        if (showTitle) PixelHeading(if (compact) "Environment Conditions" else "Live Readings")
        telemetry.readings().chunked(if (compact) 4 else 2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { sensor -> SensorTile(sensor, compact, Modifier.weight(1f)) }
            }
        }
    }
    if (embedded) Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    else PixelPanel(modifier, content = content)
}

@Composable
private fun SensorTile(sensor: SensorReading, compact: Boolean, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(3.dp), color = sensor.color,
        border = BorderStroke(1.dp, AppCardBorder)) {
        if (compact) Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Image(painterResource(sensor.icon), null, Modifier.size(14.dp))
                Text(if (sensor.label == "Temperature") "Temp" else sensor.label.substringBefore(" "),
                    style = MaterialTheme.typography.labelSmall, color = AppTextColor)
            }
            Text(sensor.value, style = MaterialTheme.typography.labelLarge, color = AppTextColor)
        } else Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Image(painterResource(sensor.icon), null, Modifier.size(26.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(sensor.label, style = MaterialTheme.typography.labelMedium, color = AppTextColor)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(sensor.value, style = MaterialTheme.typography.titleSmall, color = AppTextColor)
                    Text(sensor.status, style = MaterialTheme.typography.labelSmall,
                        color = if (sensor.good) PixelGreen else Color(0xFFAE5734))
                }
            }
        }
    }
}
