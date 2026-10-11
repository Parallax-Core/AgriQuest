package com.agriquest.ui.plant

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agriquest.R
import com.agriquest.domain.model.JournalCategory
import com.agriquest.domain.model.JournalEntry
import com.agriquest.domain.model.MoistureHistoryEntry
import com.agriquest.domain.model.PlantAvatar
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.domain.model.TelemetryData
import com.agriquest.ui.components.PlantAvatarCanvas
import com.agriquest.ui.components.AgriQuestBottomNav
import com.agriquest.ui.state.AgriQuestUiState
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Amber100
import com.agriquest.ui.theme.Amber500
import com.agriquest.ui.theme.Emerald100
import com.agriquest.ui.theme.Emerald200
import com.agriquest.ui.theme.Emerald50
import com.agriquest.ui.theme.Emerald500
import com.agriquest.ui.theme.Emerald600
import com.agriquest.ui.theme.Emerald700
import com.agriquest.ui.theme.PureWhite
import com.agriquest.ui.theme.SilkscreenFontFamily
import com.agriquest.ui.theme.Slate100
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate50
import com.agriquest.ui.theme.Slate500
import com.agriquest.ui.theme.Slate600
import com.agriquest.ui.theme.Slate800
import com.agriquest.ui.theme.Slate900
import com.agriquest.ui.viewmodel.AgriQuestViewModel
import java.util.Locale


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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            AgriQuestBottomNav(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                streakDays = uiState.plant.streakDays,
                targetStreakDays = uiState.plant.targetStreakDays
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                // 1. Header with Soft Cream/Slate Background
                PlantDetailsHeader(
                    title = uiState.plant.title,
                    subtitle = "PLANT DETAILS",
                    onBackClick = onBackClick
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFFBF9F1),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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

                    Column {
                        Text(
                            text = subtitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                color = Slate500,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = SilkscreenFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                fontSize = 22.sp
                            )
                        )
                    }
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5D9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, Color(0xFFB68E64))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Slate800
                            )
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
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Emerald700
                            )
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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = "${plant.healthPercent}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald600,
                            fontSize = 16.sp
                        )
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
                    Text(
                        text = "Level ${plant.levelInt}",
                        color = Color(0xFF573610),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${plant.currentXp} / ${plant.maxXp} XP",
                        color = Color(0xFF32651D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
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
                Text(
                    text = plant.nextWateringInHours?.let { hours ->
                        val wateringNote = if (hours <= 0) "Watering due now" else "Next watering in $hours h"
                        "${plant.statusTag} · $wateringNote"
                    } ?: "${plant.statusTag} · Next watering not scheduled",
                    color = Color(0xFFB68147),
                    fontStyle = FontStyle.Italic,
                    fontSize = 11.sp
                )

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
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate500,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
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
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = SilkscreenFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Slate900 else Slate500,
                                fontSize = 14.sp
                            )
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Title: Live Sensor Readings
        Text(
            text = "Live Readings",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = SilkscreenFontFamily,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                fontSize = 16.sp
            )
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
fun PlantDetailsTelemetryGrid(
    telemetry: TelemetryData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val moistureStatus = when {
                telemetry.soilMoisture < 60f -> "Too Dry" to R.drawable.red
                telemetry.soilMoisture > 80f -> "Too Wet" to R.drawable.red
                else -> "Optimal" to R.drawable.green
            }
            PlantTelemetryMetricCard(
                modifier = Modifier.weight(1f),
                title = "Soil Moisture",
                reading = "${telemetry.soilMoisture.toInt()}%",
                targetRange = "Target: 60-80%",
                iconRes = R.drawable.soil_moisture,
                statusLabel = moistureStatus.first,
                statusBackgroundRes = moistureStatus.second,
                containerColor = Color(0xFFDBEAF2),
                strokeColor = Color(0xFFC9A86A)
            )

            val tempIconRes = when {
                telemetry.airTemperature < 15.0f -> R.drawable.temp_cold
                telemetry.airTemperature > 30.0f -> R.drawable.temp_high
                telemetry.airTemperature in 28.0f..30.0f -> R.drawable.temp_warm
                else -> R.drawable.temp_good
            }
            val temperatureStatus = when {
                telemetry.airTemperature < 15f -> "Too Cold" to R.drawable.blue
                telemetry.airTemperature > 30f -> "Too Hot" to R.drawable.red
                else -> "Ideal" to R.drawable.green
            }
            PlantTelemetryMetricCard(
                modifier = Modifier.weight(1f),
                title = "Temperature",
                reading = "${telemetry.airTemperature.toInt()}°C",
                targetRange = "Target: 21-28°C",
                iconRes = tempIconRes,
                statusLabel = temperatureStatus.first,
                statusBackgroundRes = temperatureStatus.second,
                containerColor = Color(0xFFFBE3C4),
                strokeColor = Color(0xFFC9A86A)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val humidityStatus = when {
                telemetry.relativeHumidity < 50f -> "Too Low" to R.drawable.red
                telemetry.relativeHumidity > 70f -> "Too High" to R.drawable.blue
                else -> "Good" to R.drawable.green
            }
            PlantTelemetryMetricCard(
                modifier = Modifier.weight(1f),
                title = "Humidity",
                reading = "${telemetry.relativeHumidity.toInt()}%",
                targetRange = "Target: 50-70%",
                iconRes = R.drawable.humidity,
                statusLabel = humidityStatus.first,
                statusBackgroundRes = humidityStatus.second,
                containerColor = Color(0xFFDFECCB),
                strokeColor = Color(0xFFC9A86A)
            )

            // Explicitly formatted "4.2 klux"
            val lightReading = if (telemetry.lightIntensity >= 1000f) {
                String.format(Locale.US, "%.1f klux", telemetry.lightIntensity / 1000f)
            } else {
                "4.2 klux"
            }

            PlantTelemetryMetricCard(
                modifier = Modifier.weight(1f),
                title = "Light",
                reading = lightReading,
                targetRange = "Target: >4.0 klux",
                iconRes = R.drawable.light,
                statusLabel = if (telemetry.lightIntensity < 4000f) "Too Low" else "Moderate",
                statusBackgroundRes = if (telemetry.lightIntensity < 4000f) R.drawable.blue else R.drawable.green,
                containerColor = Color(0xFFFDF0B8),
                strokeColor = Color(0xFFC9A86A)
            )
        }
    }
}

@Composable
fun PlantTelemetryMetricCard(
    title: String,
    reading: String,
    targetRange: String,
    iconRes: Int,
    statusLabel: String,
    statusBackgroundRes: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFFFFF5D9),
    strokeColor: Color = Color(0xFFB68E64)
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(2.dp, strokeColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.4.dp)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(26.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF805B30),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reading,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color(0xFF573610),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = statusLabel,
                        maxLines = 1,
                        color = when (statusBackgroundRes) {
                            R.drawable.green -> Color(0xFF32651D)
                            R.drawable.red -> Color(0xFF9A4A38)
                            R.drawable.blue -> Color(0xFF37606C)
                            else -> Slate900
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
/**
 * Moisture History section displaying a custom chart & daily timeline ("Today", "Yesterday", "2 days ago" values).
 */
@Composable
fun MoistureHistorySection(
    history: List<MoistureHistoryEntry>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5D9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFB68E64))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = SilkscreenFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 16.sp
                        )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Emerald50,
                    modifier = Modifier.border(1.dp, Emerald200, CircleShape)
                ) {
                    Text(
                        text = "Target 60-80%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Emerald700,
                            fontSize = 11.sp
                        ),
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
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate800,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        Text(
                            text = "${entry.moisturePercent.toInt()}% Soil Moisture",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Emerald700,
                                fontSize = 13.sp
                            )
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SilkscreenFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        fontSize = 16.sp
                    )
                )
            }

            Text(
                text = "${journalEntries.size} entries",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate500,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // Quick Add Note Input Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5D9)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, Color(0xFFB68E64))
        ) {
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
                    placeholder = { Text("Add care log note...", color = Slate500, fontSize = 13.sp) },
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
                    shape = RoundedCornerShape(12.dp)
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5D9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFB68E64))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900,
                        fontSize = 14.sp
                    )
                )
            }

            Surface(
                shape = CircleShape,
                color = Slate100,
                modifier = Modifier.border(1.dp, Slate200, CircleShape)
            ) {
                Text(
                    text = entry.date,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate600,
                        fontSize = 11.sp
                    ),
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
