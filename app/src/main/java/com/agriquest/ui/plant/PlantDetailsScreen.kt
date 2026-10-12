package com.agriquest.ui.plant

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agriquest.domain.model.JournalCategory
import com.agriquest.domain.model.JournalEntry
import com.agriquest.domain.model.MoistureHistoryEntry
import com.agriquest.domain.model.PlantAvatar
import com.agriquest.domain.model.TelemetryData
import com.agriquest.ui.components.AppCard
import com.agriquest.ui.components.AppPageTitle
import com.agriquest.ui.components.AppScreen
import com.agriquest.ui.components.PlantAvatarCanvas
import com.agriquest.ui.components.TelemetryGrid
import com.agriquest.ui.state.AgriQuestUiState
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Amber100
import com.agriquest.ui.theme.AppLayout
import com.agriquest.ui.theme.AppTextColor
import com.agriquest.ui.theme.Emerald100
import com.agriquest.ui.theme.Emerald200
import com.agriquest.ui.theme.Emerald50
import com.agriquest.ui.theme.Emerald600
import com.agriquest.ui.theme.Emerald700
import com.agriquest.ui.theme.PureWhite
import com.agriquest.ui.theme.Slate100
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate50
import com.agriquest.ui.theme.Slate500
import com.agriquest.ui.theme.Slate600
import com.agriquest.ui.theme.Slate800
import com.agriquest.ui.theme.Slate900
import com.agriquest.ui.viewmodel.AgriQuestViewModel

enum class PlantDetailsTab(val title: String, val icon: String) {
    STATS("Stats", "📊"),
    JOURNAL("Journal", "📖")
}

@Composable
fun PlantDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: AgriQuestViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.notificationEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    PlantDetailsScreenContent(
        modifier = modifier,
        uiState = uiState,
        onBackClick = onBackClick,
        onAddJournalEntry = { note -> viewModel.addJournalEntry(note) },
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun PlantDetailsScreenContent(
    modifier: Modifier = Modifier,
    uiState: AgriQuestUiState = AgriQuestUiState(),
    selectedTab: Int = 1,
    onTabSelected: (Int) -> Unit = {},
    onBackClick: () -> Unit = {},
    onAddJournalEntry: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var detailsTabState by remember { mutableStateOf(PlantDetailsTab.STATS) }

    AppScreen(
        modifier = modifier,
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        snackbarHostState = snackbarHostState
    ) {
        // 1. Header with Soft Cream/Slate Background
        PlantDetailsHeader(
            title = uiState.plant.title,
            subtitle = "PLANT DETAILS",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
            .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 2. Plant Hero Card (Day 47 badge, Stage 3 badge, PlantAvatarCanvas Stage 3, Plant Health 92% progress bar)
            PlantHeroCard(
                plant = uiState.plant
            )

            // 3. Stats / Journal Segmented Toggle Control
            StatsJournalSegmentedControl(
                selectedTab = detailsTabState,
                onTabSelected = { detailsTabState = it }
            )

            // 4. Tab Content Switcher (Stats vs Journal)
            Crossfade(
                targetState = detailsTabState,
                animationSpec = tween(durationMillis = 250),
                label = "PlantDetailsTabCrossfade"
            ) { tab ->
                when (tab) {
                    PlantDetailsTab.STATS -> {
                        StatsTabContent(
                            telemetry = uiState.telemetry,
                            moistureHistory = uiState.moistureHistory
                        )
                    }
                    PlantDetailsTab.JOURNAL -> {
                        JournalTabContent(
                            journalEntries = uiState.journalEntries,
                            onAddJournalEntry = onAddJournalEntry
                        )
                    }
                }
            }
        }

    }
}

/**
* Top Header section with soft cream background (`0xFFFBF9F1` / `0xFFF8FAFC`), title, and subtitle.
*/
@Composable
fun PlantDetailsHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    AppCard(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(AppLayout.cardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                        .size(38.dp)
                        .background(PureWhite, CircleShape)
                        .border(1.dp, Slate200, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Slate800,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AppPageTitle(title, subtitle, Modifier.weight(1f))
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier
                    .size(38.dp)
                    .background(PureWhite, CircleShape)
                    .border(1.dp, Slate200, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = Slate800,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
* Hero card displaying day counter pill badge, status badge, stage 3 canvas illustration, and health progress bar.
*/
@Composable
fun PlantHeroCard(
    plant: PlantAvatar,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(AppLayout.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing)
        ) {
            // Badges Row: Day Counter Pill Badge ("Day 47") & Status Badge ("Stage 3 - Mature")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day Counter Pill Badge
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
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Day Counter",
                            tint = Slate600,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Day ${plant.dayCount}",
                            style = MaterialTheme.typography.labelMedium.copy(color = AppTextColor)
                        )
                    }
                }

                // Stage Status Badge ("Stage 3 - Mature")
                Surface(
                    shape = CircleShape,
                    color = Emerald50,
                    modifier = Modifier.border(1.dp, Emerald200, CircleShape)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Status Mature",
                            tint = Emerald600,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = plant.stageTag,
                            style = MaterialTheme.typography.labelMedium.copy(color = Emerald700)
                        )
                    }
                }
            }

            // Imported plant image for the current growth stage
            PlantAvatarCanvas(
                growthStage = plant.growthStage,
                size = 145.dp
            )

            // Plant Health Progress Bar & Info Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Plant Health",
                        style = MaterialTheme.typography.bodyMedium.copy(color = AppTextColor)
                    )
                    Text(
                        text = "${plant.healthPercent}%",
                        style = MaterialTheme.typography.titleLarge.copy(color = Emerald600)
                    )
                }

                // Green Progress Indicator Bar
                Box(
                    modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(Emerald100, RoundedCornerShape(5.dp))
                ) {
                    Box(
                        modifier = Modifier
                        .fillMaxWidth(plant.healthPercent / 100f)
                        .height(10.dp)
                        .background(Emerald600, RoundedCornerShape(5.dp))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Level ${plant.levelInt}",
                        color = AppTextColor,
                        style = MaterialTheme.typography.bodySmall)
                    Text(text = "${plant.currentXp} / ${plant.maxXp} XP",
                        color = Color(0xFF32651D),
                        style = MaterialTheme.typography.bodySmall)
                }
                Box(
                    modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(Color(0xFFF3E0A0), RoundedCornerShape(2.dp))
                    .border(1.dp, Color(0xFFC9A86A), RoundedCornerShape(2.dp))
                ) {
                    if (plant.xpProgress > 0f) {
                        Box(
                            modifier = Modifier
                            .fillMaxWidth(plant.xpProgress)
                            .height(10.dp)
                            .background(Color(0xFFDEA92A), RoundedCornerShape(2.dp))
                        )
                    }
                }
                Text(text = plant.nextWateringInHours?.let { hours ->
                        val wateringNote = if (hours <= 0) "Watering due now" else "Next watering in $hours h"
                        "${plant.statusTag} · $wateringNote"
                    } ?: "${plant.statusTag} · Next watering not scheduled",
                    color = Color(0xFFB68147),
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.labelMedium)

                // Health Subtext Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Health Status",
                        tint = Emerald600,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = plant.healthSubtext,
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                    )
                }
            }
        }
    }
}

/**
* Segmented toggle control: `📊 Stats` | `📖 Journal`.
*/
@Composable
fun StatsJournalSegmentedControl(
    selectedTab: PlantDetailsTab,
    onTabSelected: (PlantDetailsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFFFF5D9),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFB68E64))
    ) {
        Row(
            modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PlantDetailsTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(
                        color = if (isSelected) PureWhite else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tab.icon,
                            fontSize = 14.sp
                        )
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelLarge.copy(color = if (isSelected) Slate900 else Slate500)
                        )
                    }
                }
            }
        }
    }
}

/**
* Stats Tab View containing:
* a) Live Readings 2x2 grid (Soil Moisture 68%, Temperature 24°C, Humidity 62%, Light 4.2 klux).
* b) Moisture History line/bar chart section.
*/
@Composable
fun StatsTabContent(
    telemetry: TelemetryData,
    moistureHistory: List<MoistureHistoryEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing)
    ) {
        // Section Title: Live Sensor Readings
        Text(
            text = "Live Readings",
            style = MaterialTheme.typography.titleMedium.copy(color = AppTextColor)
        )

        // 2x2 Live Telemetry Grid
        PlantDetailsTelemetryGrid(telemetry = telemetry)

        // Moisture History Section
        MoistureHistorySection(history = moistureHistory)
    }
}

/**
* 2x2 Telemetry Grid displaying explicit metric values and pixel-art drawables:
* Soil Moisture 68%, Temperature 24°C, Humidity 62%, Light 4.2 klux.
*/
@Composable
fun PlantDetailsTelemetryGrid(telemetry: TelemetryData, modifier: Modifier = Modifier) {
    TelemetryGrid(modifier = modifier, telemetry = telemetry, showTitle = false)
}

/**
* Moisture History section displaying a custom chart & daily timeline ("Today", "Yesterday", "2 days ago" values).
*/
@Composable
fun MoistureHistorySection(
    history: List<MoistureHistoryEntry>,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(AppLayout.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing)
        ) {
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
                        imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                        contentDescription = "Chart Icon",
                        tint = Emerald600,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Moisture History",
                        style = MaterialTheme.typography.titleMedium.copy(color = AppTextColor)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Emerald50,
                    modifier = Modifier.border(1.dp, Emerald200, CircleShape)
                ) {
                    Text(
                        text = "Target 60-80%",
                        style = MaterialTheme.typography.labelSmall.copy(color = Emerald700),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Visual Soil Moisture Bar/Line Chart Canvas
            MoistureHistoryChartCanvas(
                history = history,
                modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
            )

            // Timeline values breakdown cards ("Today", "Yesterday", "2 days ago")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                history.takeLast(3).reversed().forEach { entry ->
                    Row(
                        modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate50, RoundedCornerShape(12.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                .size(8.dp)
                                .background(Emerald600, CircleShape)
                            )
                            Text(
                                text = entry.dayLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(color = AppTextColor)
                            )
                        }

                        Text(
                            text = "${entry.moisturePercent.toInt()}% Soil Moisture",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Emerald700)
                        )
                    }
                }
            }
        }
    }
}

/**
* Canvas Bar & Line Chart for Moisture History.
*/
@Composable
fun MoistureHistoryChartCanvas(
    history: List<MoistureHistoryEntry>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 16f
        val paddingRight = 16f
        val paddingTop = 24f
        val paddingBottom = 32f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val maxVal = 100f
        val minVal = 0f

        // Draw Optimal Range Band (60% to 80%)
        val bandTop = paddingTop + chartHeight * (1f - 80f / 100f)
        val bandBottom = paddingTop + chartHeight * (1f - 60f / 100f)
        drawRect(
            color = Emerald50,
            topLeft = Offset(paddingLeft, bandTop),
            size = Size(chartWidth, bandBottom - bandTop)
        )

        // Draw grid lines
        listOf(0.2f, 0.5f, 0.8f).forEach { ratio ->
            val y = paddingTop + chartHeight * (1f - ratio)
            drawLine(
                color = Slate200,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1f
            )
        }

        if (history.isEmpty()) return@Canvas

        val stepX = chartWidth / (history.size - 1).coerceAtLeast(1)
        val points = history.mapIndexed { index, item ->
            val x = paddingLeft + index * stepX
            val ratio = (item.moisturePercent - minVal) / (maxVal - minVal)
            val y = paddingTop + chartHeight * (1f - ratio.coerceIn(0f, 1f))
            Offset(x, y)
        }

        // Draw Bars
        val barWidth = 24f
        history.forEachIndexed { index, item ->
            val x = paddingLeft + index * stepX
            val ratio = (item.moisturePercent - minVal) / (maxVal - minVal)
            val barH = chartHeight * ratio.coerceIn(0f, 1f)
            val barY = paddingTop + chartHeight - barH

            drawRoundRect(
                color = Emerald100,
                topLeft = Offset(x - barWidth / 2f, barY),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }

        // Draw Line connecting data points
        val linePath = Path()
        points.forEachIndexed { index, pt ->
            if (index == 0) linePath.moveTo(pt.x, pt.y) else linePath.lineTo(pt.x, pt.y)

        }
        drawPath(
            path = linePath,
            color = Emerald600,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        // Draw Data Point Nodes & Value Labels
        points.forEach { pt ->
            drawCircle(color = PureWhite, radius = 7f, center = pt)
            drawCircle(color = Emerald600, radius = 5f, center = pt)
        }
    }
}

/**
* Journal Tab View containing care log notes ("Aug 12: Watered 250ml", "Aug 10: Pruned bottom branches", "Aug 5: Planted seedling").
*/
@Composable
fun JournalTabContent(
    journalEntries: List<JournalEntry>,
    modifier: Modifier = Modifier,
    onAddJournalEntry: (String) -> Unit = {}
) {
    var newNoteText by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing)
    ) {
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
                    imageVector = Icons.Rounded.Book,
                    contentDescription = "Journal Icon",
                    tint = Emerald600,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Care Log Notes",
                    style = MaterialTheme.typography.titleMedium.copy(color = AppTextColor)
                )
            }

            Text(
                text = "${journalEntries.size} entries",
                style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
            )
        }

        // Quick Add Note Input Row
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newNoteText,
                    onValueChange = { newNoteText = it },
                    placeholder = { Text("Add care log note...",
                            color = Slate500,
                            style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (newNoteText.isNotBlank()) {
                            onAddJournalEntry(newNoteText.trim())
                            newNoteText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = AppLayout.buttonShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Log",
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Journal Entries List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            journalEntries.forEach { entry ->
                JournalEntryCard(entry = entry)
            }
        }
    }
}

/**
* Card for individual care journal log entries.
*/
@Composable
fun JournalEntryCard(
    entry: JournalEntry,
    modifier: Modifier = Modifier
) {
    val categoryBg = when (entry.category) {
        JournalCategory.WATERING -> Emerald50
        JournalCategory.PRUNING -> Amber100
        JournalCategory.PLANTING -> Emerald100
        else -> Slate100
    }

    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
            .fillMaxWidth()
            .padding(AppLayout.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppLayout.sectionSpacing)
            ) {
                Box(
                    modifier = Modifier
                    .size(38.dp)
                    .background(categoryBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.category.emoji,
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodyMedium.copy(color = AppTextColor)
                )
            }

            Surface(
                shape = CircleShape,
                color = Slate100,
                modifier = Modifier.border(1.dp, Slate200, CircleShape)
            ) {
                Text(
                    text = entry.date,
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate600),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun PlantDetailsScreenPreview() {
    AgriQuestTheme {
        PlantDetailsScreenContent()
    }
}
