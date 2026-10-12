package com.agriquest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.agriquest.ui.state.AgriQuestUiState
import com.agriquest.ui.theme.*

val PixelGreen = Color(0xFF32651D)
val PixelPaleGreen = Color(0xFFE3EBB0)
val PixelGold = Color(0xFFE2A72B)
val PixelTan = Color(0xFFFBE9BD)

@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.DEFAULT,
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard(modifier.fillMaxWidth(), tone) {
        Column(Modifier.fillMaxWidth().padding(AppLayout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppLayout.compactSpacing), content = content)
    }
}

@Composable
fun PixelHeading(title: String, icon: ImageVector? = null, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (icon != null) Icon(icon, null, Modifier.size(16.dp), tint = AppTextColor)
        Text(title.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = AppTextColor)
        trailing()
    }
}

@Composable
fun PixelChip(text: String, color: Color = PixelPaleGreen, textColor: Color = PixelGreen,
    modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = color, shape = RoundedCornerShape(3.dp), border = BorderStroke(1.dp, AppCardBorder)) {
        Text(text, Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium, color = textColor)
    }
}

@Composable
fun PixelButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    color: Color = AppAccentBackground, enabled: Boolean = true) {
    Button(onClick, modifier.heightIn(min = 32.dp), enabled = enabled,
        shape = AppLayout.buttonShape, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = AppCardBackground,
            disabledContainerColor = AppAccentBackground, disabledContentColor = AppCardBackground.copy(alpha = .85f))) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun PixelProgress(progress: Float, modifier: Modifier = Modifier, color: Color = PixelGold) {
    Box(modifier.fillMaxWidth().height(7.dp).background(Color(0xFFF3E0A0))
        .border(1.dp, AppCardBorder)) {
        if (progress > 0f) Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}

@Composable
fun PixelPageHeader(title: String, subtitle: String, uiState: AgriQuestUiState) {
    var showSettings by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().heightIn(min = 62.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f).padding(bottom = 7.dp)) {
            Text(subtitle.uppercase(), style = MaterialTheme.typography.labelMedium, color = PixelGreen)
            Text("🌱 ${title.uppercase()}", style = MaterialTheme.typography.headlineSmall, color = AppTextColor)
        }
        IconButton(onClick = { showSettings = true }) {
            Icon(Icons.Rounded.Settings, "Settings", tint = AppAccentBackground)
        }
    }
    if (showSettings) AlertDialog(
        onDismissRequest = { showSettings = false }, title = { Text("Plant settings") },
        text = { Text("Source: ${uiState.plantSource.displayName}\nConnection: ${uiState.connection.statusText}") },
        confirmButton = { TextButton({ showSettings = false }) { Text("Done") } }
    )
}

@Composable
fun PlantLevelBar(uiState: AgriQuestUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("⌃ Level: ${uiState.plant.levelInt}", style = MaterialTheme.typography.labelLarge, color = AppAccentBackground)
        Text(uiState.plant.xpText, style = MaterialTheme.typography.labelSmall, color = AppTextColor)
    }
    PixelProgress(uiState.plant.xpProgress, color = Color(0xFF43889B))
}
