package com.agriquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agriquest.domain.model.ConnectionState
import com.agriquest.domain.model.ConnectionStatus
import com.agriquest.ui.theme.AppTextColor

/**
 * Shown when the game starts, before any plant info: the player connects the ESP32
 * or chooses to play with simulated readings. It cannot be dismissed by tapping outside.
 */
@Composable
fun ConnectGateDialog(
    connection: ConnectionState,
    usingHardware: Boolean,
    onConnectEsp32: () -> Unit,
    onPlaySimulation: () -> Unit,
    onContinue: () -> Unit
) {
    val connected = usingHardware && connection.status == ConnectionStatus.CONNECTED
    val waiting = usingHardware && !connected && connection.status != ConnectionStatus.DISCONNECTED
    Dialog(onDismissRequest = {}, properties = DialogProperties(
        dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xE6182812)), contentAlignment = Alignment.Center) {
            PixelPanel(Modifier.widthIn(max = 380.dp).padding(16.dp)) {
                PixelHeading("Connect")
                Text("Connect your ESP32 so the game shows your real plant's readings, " +
                    "or play with simulated readings.", style = MaterialTheme.typography.bodySmall, color = AppTextColor)
                if (usingHardware) PixelChip("ESP32: ${connection.statusText}",
                    color = Color(connection.indicatorColorArgb).copy(alpha = 0.18f), textColor = AppTextColor)
                if (waiting) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(4.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (connected) {
                        PixelButton("Start playing", onContinue, Modifier.fillMaxWidth())
                    } else {
                        PixelButton(if (usingHardware) "Pair / reconnect ESP32" else "Connect ESP32",
                            onConnectEsp32, Modifier.fillMaxWidth())
                        PixelButton("Play in simulation", onPlaySimulation, Modifier.fillMaxWidth(), color = PixelGreen)
                    }
                }
                Text("The ESP32 sends a signal every 15 seconds. If none arrives for 45 seconds, the game shows it as disconnected.",
                    style = MaterialTheme.typography.labelSmall, color = AppTextColor, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
