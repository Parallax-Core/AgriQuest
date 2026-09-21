package com.agriquest.ui.ai

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agriquest.domain.model.AiScanResult
import com.agriquest.domain.model.TelemetryData
import com.agriquest.ui.components.AgriQuestBottomNav
import com.agriquest.ui.state.AgriQuestUiState
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Amber100
import com.agriquest.ui.theme.Amber50
import com.agriquest.ui.theme.Amber500
import com.agriquest.ui.theme.Amber600
import com.agriquest.ui.theme.Emerald100
import com.agriquest.ui.theme.Emerald50
import com.agriquest.ui.theme.Emerald500
import com.agriquest.ui.theme.Emerald600
import com.agriquest.ui.theme.Emerald700
import com.agriquest.ui.theme.PureWhite
import com.agriquest.ui.theme.Slate100
import com.agriquest.ui.theme.Slate200
import com.agriquest.ui.theme.Slate500
import com.agriquest.ui.theme.Slate600
import com.agriquest.ui.theme.Slate700
import com.agriquest.ui.theme.Slate900
import com.agriquest.ui.viewmodel.AgriQuestViewModel
import kotlinx.coroutines.delay

/**
 * AI Agro-Assistant Diagnostics Screen for AgriQuest Mobile.
 *
 * Features:
 * 1. Clean serif typography header ("DIAGNOSTICS", "AI Agro-Assistant") with cream background.
 * 2. AI Model Status Bar ("● Offline AI Ready" green chip badge & "PlantNet-Tomato v1.2" muted info tag).
 * 3. Camera Scan Hero Card (Emerald green card, camera icon, "Scan Tomato Leaf", interactive click action opening camera scanner simulation dialog & awarding +25 XP!).
 * 4. Current IoT Conditions Summary Card (2x2 compact grid with Soil Moisture 68% ✓, Temp 24°C ✓, Humidity 62% ⚠, Light 4.2 klux ✓).
 * 5. Recent Scans List Section ("Healthy Leaf" 97% green check badge, "Early Blight" 89% amber warning badge).
 */
@Composable
fun AiDiagnosticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AgriQuestViewModel = viewModel(),
    onTabSelected: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.notificationEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            snackbarHostState.showSnackbar(message)
        }
    }

    AiDiagnosticsScreenContent(
        modifier = modifier,
        uiState = uiState,
        selectedTab = 3,
        onTabSelected = onTabSelected,
        onPerformScan = { viewModel.performCameraScan() },
        snackbarHostState = snackbarHostState
    )
}

@Composable
fun AiDiagnosticsScreenContent(
    modifier: Modifier = Modifier,
    uiState: AgriQuestUiState = AgriQuestUiState(),
    selectedTab: Int = 3,
    onTabSelected: (Int) -> Unit = {},
    onPerformScan: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var showScannerDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isLocked = uiState.plant.streakDays < uiState.plant.targetStreakDays

    if (showScannerDialog) {
        InteractiveLeafScannerDialog(
            onDismissRequest = { showScannerDialog = false },
            onScanTriggered = {
                onPerformScan()
                showScannerDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFAF6EE),
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
                .background(Color(0xFFFAF6EE)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Header: "DIAGNOSTICS" subtitle & "AI Agro-Assistant" title in clean serif typography
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "DIAGNOSTICS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                            color = Color(0xFF8B7E66)
                        )
                    )
                    Text(
                        text = "AI Agro-Assistant",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = Slate900
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 2. AI Model Status Bar: "● Offline AI Ready" & "PlantNet-Tomato v1.2"
                    AiModelStatusBar()

                    // Lock Banner Overlay when streak < 7
                    if (isLocked) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Amber50),
                            border = BorderStroke(1.dp, Amber100),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Amber500,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = "Locked",
                                            tint = PureWhite,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "🔒 AI Diagnostic Scan Unlocks at 7-Day Streak!",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Slate900
                                        )
                                    )
                                    Text(
                                        text = "Current streak: ${uiState.plant.streakDays} / ${uiState.plant.targetStreakDays} days",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Amber600
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 3. Camera Scan Hero Card: Emerald green card with camera icon, title, subtitle & click action
                    CameraScanHeroCard(
                        isLocked = isLocked,
                        streakDays = uiState.plant.streakDays,
                        targetStreakDays = uiState.plant.targetStreakDays,
                        onOpenScanner = {
                            if (isLocked) {
                                Toast.makeText(
                                    context,
                                    "🔒 AI Diagnostic Scan unlocks at 7-day streak! Current streak: ${uiState.plant.streakDays}/${uiState.plant.targetStreakDays} days",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showScannerDialog = true
                            }
                        }
                    )

                    // 4. Current IoT Conditions Summary Card: 2x2 compact row grid
                    IotConditionsSummaryCard(
                        telemetry = uiState.telemetry
                    )

                    // 5. Recent Scans List Section
                    RecentScansSection(
                        recentScans = uiState.recentScans
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * AI Model Status Bar displaying green badge chip "● Offline AI Ready" and muted tag "PlantNet-Tomato v1.2".
 */
@Composable
private fun AiModelStatusBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status badge chip: "● Offline AI Ready" (green chip)
        Surface(
            shape = RoundedCornerShape(50),
            color = Emerald50,
            border = BorderStroke(1.dp, Emerald100)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pulse Green Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Emerald600)
                )
                Text(
                    text = "Offline AI Ready",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Emerald700
                    )
                )
            }
        }

        // Model info tag: "PlantNet-Tomato v1.2" (muted text)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Memory,
                contentDescription = "Model",
                tint = Slate500,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "PlantNet-Tomato v1.2",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Slate500
                )
            )
        }
    }
}

/**
 * Camera Scan Hero Card: Emerald green container card (0xFF059669 / 0xFF10B981) with camera icon,
 * title ("Scan Tomato Leaf"), subtitle ("Point camera at a leaf to detect disease"), and interactive action.
 */
@Composable
private fun CameraScanHeroCard(
    isLocked: Boolean = false,
    streakDays: Int = 0,
    targetStreakDays: Int = 7,
    onOpenScanner: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onOpenScanner() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLocked) Color(0xFF64748B) else Color(0xFF059669)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isLocked) {
                            listOf(Color(0xFF64748B), Color(0xFF475569))
                        } else {
                            listOf(Color(0xFF059669), Color(0xFF10B981))
                        }
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Camera / Lock Icon Container
                Surface(
                    shape = CircleShape,
                    color = PureWhite.copy(alpha = 0.22f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.CameraAlt,
                            contentDescription = if (isLocked) "Locked Scan" else "Camera Scan",
                            tint = PureWhite,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Text details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isLocked) "Scan Locked (Requires 7-Day Streak)" else "Scan Tomato Leaf",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = PureWhite
                        )
                    )
                    Text(
                        text = if (isLocked) "Current streak: $streakDays / $targetStreakDays days" else "Point camera at a leaf to detect disease",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = PureWhite.copy(alpha = 0.9f)
                        )
                    )
                }

                // Small Action Arrow / XP pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PureWhite,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = if (isLocked) "🔒 Locked" else "+25 XP",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = if (isLocked) Slate700 else Emerald700
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Current IoT Conditions Summary Card:
 * Title "Current IoT Conditions" and 2x2 compact row grid showing:
 * - Soil Moisture: 68% ✓
 * - Temperature: 24°C ✓
 * - Humidity: 62% ⚠ (Amber warning icon)
 * - Light: 4.2 klux ✓
 */
@Composable
private fun IotConditionsSummaryCard(
    telemetry: TelemetryData
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Current IoT Conditions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Slate900
                )
            )

            // 2x2 Compact Row Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Soil Moisture & Temperature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IotConditionTile(
                        modifier = Modifier.weight(1f),
                        label = "Soil Moisture",
                        value = "${telemetry.soilMoisture.toInt()}%",
                        isWarning = false,
                        statusSymbol = "✓"
                    )
                    IotConditionTile(
                        modifier = Modifier.weight(1f),
                        label = "Temperature",
                        value = "${telemetry.airTemperature.toInt()}°C",
                        isWarning = false,
                        statusSymbol = "✓"
                    )
                }

                // Row 2: Humidity & Light
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Humidity 62% shows Amber Warning Icon per specification
                    IotConditionTile(
                        modifier = Modifier.weight(1f),
                        label = "Humidity",
                        value = "${telemetry.relativeHumidity.toInt()}%",
                        isWarning = true,
                        statusSymbol = "⚠"
                    )
                    val lightKlux = String.format("%.1f", telemetry.lightIntensity / 1000f)
                    IotConditionTile(
                        modifier = Modifier.weight(1f),
                        label = "Light",
                        value = "$lightKlux klux",
                        isWarning = false,
                        statusSymbol = "✓"
                    )
                }
            }
        }
    }
}

/**
 * Compact tile representation for individual IoT sensor condition.
 */
@Composable
private fun IotConditionTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    isWarning: Boolean,
    statusSymbol: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Slate100,
        border = BorderStroke(1.dp, if (isWarning) Amber100 else Slate200)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Slate500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Slate900
                    )
                )
            }

            // Status Badge Icon
            if (isWarning) {
                Surface(
                    shape = CircleShape,
                    color = Amber50,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Warning",
                            tint = Amber600,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = Emerald50,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Optimal",
                            tint = Emerald600,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recent Scans List Section displaying section title "Recent Scans" and scan cards:
 * 1. "Healthy Leaf", "Leaf 4 (middle branch) • Sep 12 - 2:14 PM", "97%" (green check badge)
 * 2. "Early Blight", "Leaf 7 (lower) • Sep 10 - 11:02 AM", "89%" (amber warning badge)
 */
@Composable
private fun RecentScansSection(
    recentScans: List<AiScanResult>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Recent Scans",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Slate900
            )
        )

        recentScans.forEach { scan ->
            ScanResultCard(scan = scan)
        }
    }
}

/**
 * Individual Scan Result Card displaying title, subtext, confidence tag, and green check or amber warning badge.
 */
@Composable
private fun ScanResultCard(
    scan: AiScanResult
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = scan.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Slate900
                    )
                )
                Text(
                    text = scan.subtext,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Slate500
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Confidence Tag Badge Chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (scan.isHealthy) Emerald50 else Amber50,
                border = BorderStroke(1.dp, if (scan.isHealthy) Emerald100 else Amber100)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (scan.isHealthy) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        contentDescription = if (scan.isHealthy) "Healthy Check" else "Warning Badge",
                        tint = if (scan.isHealthy) Emerald600 else Amber600,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = scan.confidence,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (scan.isHealthy) Emerald700 else Amber600
                        )
                    )
                }
            }
        }
    }
}

/**
 * Interactive Leaf Scanner Simulation Dialog simulating live camera viewfinder scan.
 */
@Composable
private fun InteractiveLeafScannerDialog(
    onDismissRequest: () -> Unit,
    onScanTriggered: () -> Unit
) {
    var scanStatusText by remember { mutableStateOf("Positioning camera over leaf...") }
    var isScanning by remember { mutableStateOf(false) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            scanStatusText = "Analyzing leaf texture with PlantNet-Tomato v1.2..."
            delay(1200L)
            scanStatusText = "Inference complete! Processing health score..."
            delay(500L)
            onScanTriggered()
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Header
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
                            imageVector = Icons.Rounded.CenterFocusStrong,
                            contentDescription = "Scanner",
                            tint = Emerald500,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "AI Leaf Scanner",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = PureWhite
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Slate500,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { onDismissRequest() }
                    )
                }

                // Camera Viewfinder Simulation Box
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Emerald500.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Viewfinder Target Box
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(if (isScanning) pulseScale else 1.0f)
                            .border(2.dp, Emerald500, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Target Leaf",
                            tint = Emerald500.copy(alpha = 0.8f),
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // Model Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "PlantNet-Tomato v1.2",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Emerald500
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Scan Status Text
                Text(
                    text = scanStatusText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Slate200
                    )
                )

                // Trigger Button
                Button(
                    onClick = { isScanning = true },
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald600,
                        disabledContainerColor = Emerald700.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Scan",
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isScanning) "Scanning Leaf..." else "Run Diagnostic Scan (+25 XP)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PureWhite
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AiDiagnosticsScreenPreview() {
    AgriQuestTheme {
        AiDiagnosticsScreenContent()
    }
}
