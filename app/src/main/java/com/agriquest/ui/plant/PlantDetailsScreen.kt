package com.agriquest.ui.plant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.agriquest.domain.model.*
import com.agriquest.ui.components.*
import com.agriquest.ui.state.AgriQuestUiState
import com.agriquest.ui.theme.*

@Composable
fun PlantDetailsScreenContent(modifier: Modifier = Modifier, uiState: AgriQuestUiState = AgriQuestUiState(),
    selectedTab: Int = 1, onTabSelected: (Int) -> Unit = {},
    onAddJournalEntry: (String) -> Unit = {}, onSetSowingDate: (String) -> Unit = {}, snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }) {
    var editingDate by remember { mutableStateOf(false) }
    var sowingDate by remember(uiState.plant.sowingDate) { mutableStateOf(uiState.plant.sowingDate) }
    var journal by remember { mutableStateOf(false) }
    var addingNote by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    AppScreen(uiState, selectedTab, onTabSelected, snackbarHostState, modifier) {
        PixelPageHeader(uiState.plant.title.replace("🍅", "").trim(), "Plant Details", uiState)
        PlantHeroCard(uiState.plant)
        PixelButton("Set sowing date", { editingDate = true })
        Text("Estimated growth based on sowing date. Confirm stages by observing your plant.", style = MaterialTheme.typography.bodySmall)
        PlantStagesTimeline(uiState.plant.growthStage)
        Surface(color = PixelPaleGreen, shape = RoundedCornerShape(3.dp), border = BorderStroke(1.dp, AppCardBorder)) {
            Row(Modifier.fillMaxWidth().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Stats", "Journal").forEachIndexed { index, label ->
                    TextButton(onClick = { journal = index == 1 }, modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(3.dp), colors = ButtonDefaults.textButtonColors(
                            containerColor = if (journal == (index == 1)) AppAccentBackground else Color.Transparent,
                            contentColor = if (journal == (index == 1)) AppCardBackground else PixelGreen)) {
                        Text(label.uppercase(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        if (journal) PixelPanel {
            PixelHeading("Plant Journal", trailing = { PixelButton("+ Add Note", { addingNote = true }) })
            if (uiState.journalEntries.isEmpty()) Text("Your plant's story starts here. Add your first note.",
                style = MaterialTheme.typography.bodySmall, color = AppTextColor)
            uiState.journalEntries.forEach { JournalEntryCard(it) }
        } else {
            PixelPanel {
                PixelHeading("Live Readings", trailing = {
                    PixelChip(if (uiState.plantSource == PlantSource.ESP32_HARDWARE) "● ESP32: ${uiState.connection.statusText}" else "● Emulated")
                })
                TelemetryGrid(telemetry = uiState.telemetry, showTitle = false, embedded = true)
            }
            MoistureHistorySection(uiState.moistureHistory)
            PixelPanel {
                PixelHeading("Growth Stats", Icons.Rounded.Eco)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GrowthStat("${uiState.plant.dayCount}", "Days", Modifier.weight(1f))
                    GrowthStat("${uiState.plant.growthStage.stageLevel + 1}", "Stage", Modifier.weight(1f))
                    GrowthStat("${uiState.plant.healthPercent}%", "Health", Modifier.weight(1f))
                }
            }
        }
    }
    if (editingDate) AlertDialog(onDismissRequest = { editingDate = false }, title = { Text("When were the seeds sown?") },
        text = { OutlinedTextField(sowingDate, { sowingDate = it }, label = { Text("YYYY-MM-DD") }, singleLine = true) },
        confirmButton = { TextButton({ onSetSowingDate(sowingDate.trim()); editingDate = false }) { Text("Save") } },
        dismissButton = { TextButton({ editingDate = false }) { Text("Cancel") } })
    if (addingNote) AlertDialog(onDismissRequest = { addingNote = false }, title = { Text("Add plant note") },
        text = { OutlinedTextField(note, { note = it }, label = { Text("Care note") }) },
        confirmButton = { TextButton(onClick = {
            if (note.isNotBlank()) { onAddJournalEntry(note.trim()); note = ""; addingNote = false }
        }, enabled = note.isNotBlank()) { Text("Save Note") } },
        dismissButton = { TextButton({ addingNote = false }) { Text("Cancel") } })
}

@Composable
fun PlantHeroCard(plant: PlantAvatar, modifier: Modifier = Modifier) {
    PixelPanel(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            PlantAvatarCanvas(growthStage = plant.growthStage, size = 98.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    PixelChip(plant.growthStage.stageLabel, modifier = Modifier.weight(1f, fill = false))
                    Text("Day ${plant.dayCount}", style = MaterialTheme.typography.labelSmall, color = AppTextColor)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Plant health", style = MaterialTheme.typography.labelLarge, color = AppTextColor)
                    Text("${plant.healthPercent}%", style = MaterialTheme.typography.labelMedium, color = PixelGreen)
                }
                PixelProgress(plant.healthPercent / 100f, color = Color(0xFF649B32))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Level ${plant.levelInt}", style = MaterialTheme.typography.labelLarge, color = AppTextColor)
                    Text(plant.xpText, style = MaterialTheme.typography.labelSmall, color = AppTextColor)
                }
                PixelProgress(plant.xpProgress)
                Text(if (plant.nextWateringInHours != null) "Healthy · Next watering in ${plant.nextWateringInHours} h" else plant.statusTag,
                    style = MaterialTheme.typography.labelSmall, color = AppTextColor)
            }
        }
    }
}

@Composable
private fun GrowthStat(value: String, label: String, modifier: Modifier) {
    Surface(modifier, color = PixelTan, shape = RoundedCornerShape(3.dp), border = BorderStroke(1.dp, AppCardBorder)) {
        Column(Modifier.padding(9.dp)) {
            Text(value, style = MaterialTheme.typography.titleSmall, color = AppTextColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = AppTextColor)
        }
    }
}

@Composable
fun JournalEntryCard(entry: JournalEntry, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = PixelTan, shape = RoundedCornerShape(3.dp), border = BorderStroke(1.dp, AppCardBorder)) {
        Row(Modifier.padding(9.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(entry.category.emoji, style = MaterialTheme.typography.titleLarge)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(entry.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge, color = AppTextColor)
                    Text(entry.date, style = MaterialTheme.typography.labelSmall, color = AppTextColor)
                }
                Text(entry.note, style = MaterialTheme.typography.bodySmall, color = AppTextColor)
            }
        }
    }
}

@Composable
fun MoistureHistorySection(history: List<MoistureHistoryEntry>, modifier: Modifier = Modifier) {
    PixelPanel(modifier) {
        PixelHeading("Moisture History", trailing = {
            Text("Recent days", style = MaterialTheme.typography.labelSmall, color = AppTextColor)
        })
        MoistureHistoryChartCanvas(history, Modifier.fillMaxWidth().height(76.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(history.firstOrNull()?.dayLabel.orEmpty(), style = MaterialTheme.typography.labelSmall, color = AppTextColor)
            Text(history.lastOrNull()?.dayLabel.orEmpty(), style = MaterialTheme.typography.labelSmall, color = AppTextColor)
        }
    }
}

@Composable
fun MoistureHistoryChartCanvas(history: List<MoistureHistoryEntry>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFFDCEBF0))
        if (history.isNotEmpty()) {
            val step = size.width / (history.size - 1).coerceAtLeast(1)
            val points = history.mapIndexed { index, item ->
                Offset(index * step, size.height * (1f - item.moisturePercent.coerceIn(0f, 100f) / 100f))
            }
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEachIndexed { index, point ->
                    lineTo(point.x, points[index].y); lineTo(point.x, point.y)
                }
            }
            drawPath(path, Color(0xFF318CAD), style = Stroke(2.dp.toPx()))
        }
    }
}
