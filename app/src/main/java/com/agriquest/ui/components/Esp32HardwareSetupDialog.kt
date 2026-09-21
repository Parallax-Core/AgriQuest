package com.agriquest.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.agriquest.domain.model.ConnectionState
import com.agriquest.domain.model.ConnectionStatus
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Emerald100
import com.agriquest.ui.theme.Emerald50
import com.agriquest.ui.theme.Emerald600
import com.agriquest.ui.theme.Emerald700
import com.agriquest.ui.theme.PureWhite
import com.agriquest.ui.theme.Slate100
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate500
import com.agriquest.ui.theme.Slate700
import com.agriquest.ui.theme.Slate900
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data class representing discovered BLE ESP32 Hardware Device item in dialog scanner.
 */
data class DiscoveredBleDevice(
    val deviceName: String,
    val macAddress: String,
    val serviceUuid: String = "0000ffe0-0000-1000-8000-00805f9b34fb",
    val rssi: Int = -58
)

/**
 * Interactive Setup Modal Dialog for ESP32 Hardware Connection.
 *
 * Displays:
 * - Device scanner radar animation
 * - Clear step-by-step instructions ("1. Turn on ESP32 board. 2. Tap scan. 3. Select device to bind telemetry stream.")
 * - Discovered BLE devices ("ESP32-AgriQuest-01", "ESP32-Sensor-Node") with paired UUIDs
 * - Interactive "Connect Device" button and real-time connection status feedback.
 */
@Composable
fun Esp32HardwareSetupDialog(
    onDismissRequest: () -> Unit,
    connectionState: ConnectionState = ConnectionState(),
    onStartScan: () -> Unit = {},
    onConnectDevice: (DiscoveredBleDevice) -> Unit = {}
) {
    val mockDevices = remember {
        listOf(
            DiscoveredBleDevice("ESP32-AgriQuest-01", "24:62:AB:F1:82:10", rssi = -58),
            DiscoveredBleDevice("ESP32-Sensor-Node", "30:AE:A4:C2:19:88", rssi = -68)
        )
    }

    var selectedDevice by remember { mutableStateOf(mockDevices.first()) }
    val isScanning = connectionState.status == ConnectionStatus.SCANNING
    val isConnected = connectionState.status == ConnectionStatus.CONNECTED

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald50,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Sensors,
                                    contentDescription = "ESP32 Setup",
                                    tint = Emerald600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "ESP32 Hardware Setup",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Slate900
                                )
                            )
                            Text(
                                text = "Live IoT Telemetry Binding",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate500,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Dialog",
                            tint = Slate500
                        )
                    }
                }

                // Radar Scanner Animation Widget
                RadarScannerWidget(
                    isScanning = isScanning,
                    isConnected = isConnected
                )

                // Step-by-Step Instructions Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate100,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Instructions",
                                tint = Emerald600,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Quick Setup Guide",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Text(
                            text = "1. Turn on ESP32 board.\n2. Tap scan.\n3. Select device to bind telemetry stream.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate700,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }

                // Discovered BLE Devices Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Discovered BLE Devices",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate700,
                            fontSize = 12.sp
                        )
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mockDevices) { device ->
                            val isSelected = device == selectedDevice
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Emerald50 else PureWhite,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Emerald600 else Slate200,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedDevice = device }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isConnected && isSelected) Icons.Rounded.BluetoothConnected else Icons.AutoMirrored.Rounded.BluetoothSearching,
                                            contentDescription = "Device",
                                            tint = if (isSelected) Emerald600 else Slate500,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = device.deviceName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Slate900
                                                )
                                            )
                                            Text(
                                                text = "UUID: ${device.serviceUuid}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Slate500,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${device.rssi} dBm",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Emerald700,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Connection Status Chip / Banner
                Surface(
                    shape = CircleShape,
                    color = Slate100,
                    modifier = Modifier.border(1.dp, Slate200, CircleShape)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = Color(connectionState.indicatorColorArgb),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = "Status: ${connectionState.statusText}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Slate700
                            )
                        )
                    }
                }

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onStartScan,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Emerald600
                            )
                        } else {
                            Text(
                                text = "Scan",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onConnectDevice(selectedDevice)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald600,
                            contentColor = PureWhite
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Connect",
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Connect Device",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Radar / Scanner sweep animation widget.
 */
@Composable
private fun RadarScannerWidget(
    isScanning: Boolean,
    isConnected: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAngle"
    )

    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(CircleShape)
            .background(Emerald50)
            .border(1.dp, Emerald100, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f

            // Concentric radar circles
            drawCircle(color = Emerald100, radius = maxRadius, style = Stroke(width = 1.5f))
            drawCircle(color = Emerald100, radius = maxRadius * 0.65f, style = Stroke(width = 1.5f))
            drawCircle(color = Emerald100, radius = maxRadius * 0.35f, style = Stroke(width = 1.5f))

            if (isScanning) {
                // Radar sweep line
                val rad = Math.toRadians(angle.toDouble())
                val sweepX = center.x + maxRadius * cos(rad).toFloat()
                val sweepY = center.y + maxRadius * sin(rad).toFloat()
                drawLine(
                    color = Emerald600,
                    start = center,
                    end = Offset(sweepX, sweepY),
                    strokeWidth = 3f
                )
            }
        }

        Icon(
            imageVector = if (isConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.Sensors,
            contentDescription = "Radar Icon",
            tint = Emerald600,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Esp32HardwareSetupDialogPreview() {
    AgriQuestTheme {
        Esp32HardwareSetupDialog(
            onDismissRequest = {},
            connectionState = ConnectionState(
                deviceName = "ESP32",
                status = ConnectionStatus.SCANNING,
                statusText = "Scanning for ESP32..."
            )
        )
    }
}
