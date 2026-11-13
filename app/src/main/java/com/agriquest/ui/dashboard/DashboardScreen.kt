package com.agriquest.ui.dashboard

import android.net.Uri
import android.widget.Toast
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agriquest.domain.model.ConnectionStatus
import com.agriquest.domain.model.JournalCategory
import com.agriquest.domain.model.PlantSource
import com.agriquest.domain.model.WateringStatus
import com.agriquest.notifications.PlantAlertNotifier
import com.agriquest.ui.ai.AiDiagnosticsScreenContent
import com.agriquest.ui.awards.AwardsScreenContent
import com.agriquest.ui.components.*
import com.agriquest.ui.plant.PlantDetailsScreenContent
import com.agriquest.ui.quests.QuestsScreenContent
import com.agriquest.ui.state.AgriQuestUiState
import com.agriquest.ui.state.LeafScanState
import com.agriquest.ui.theme.*
import com.agriquest.ui.viewmodel.AgriQuestViewModel
import kotlinx.coroutines.launch

/**
 * Main AgriQuest Dashboard & Navigation Switcher Screen.
 *
 * Integrates state-driven 5-tab screen routing (Home, Plant, Quests, AI, Awards)
 * with persistent bottom navigation fixed across all screens and max 430dp width centering.
 */
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: AgriQuestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val leafScanState by viewModel.leafScanState.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val wateringStatus by viewModel.wateringStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEsp32SetupDialog by remember { mutableStateOf(false) }
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) viewModel.startEsp32BleScan()
        else viewModel.showNotification("Bluetooth permission is required to pair your ESP32.")
    }
    val startScan = {
        if (bluetoothPermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) viewModel.startEsp32BleScan()
        else permissionLauncher.launch(bluetoothPermissions)
    }

    // Dry-soil alerts: ask once for notification permission (Android 13+), then post a phone notification.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !PlantAlertNotifier.canNotify(context)) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.dryAlerts.collect { alert ->
            PlantAlertNotifier.showDrySoil(context, alert.plantName, alert.moisturePercent)
        }
    }

    LaunchedEffect(viewModel) {
        while (true) {
            viewModel.checkAndResetMidnightQuests()
            kotlinx.coroutines.delay(30_000L)
        }
    }

    // Listen for notification events (e.g., pump triggers, AI diagnostic results, tab status)
    LaunchedEffect(viewModel) {
        viewModel.notificationEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showEsp32SetupDialog) {
        Esp32HardwareSetupDialog(
            onDismissRequest = { showEsp32SetupDialog = false },
            connectionState = uiState.connection,
            onStartScan = startScan,
            onConnectDevice = { device ->
                viewModel.connectEsp32Device(device.macAddress) { showEsp32SetupDialog = false }
            },
            discoveredDevices = discoveredDevices
        )
    }

    DashboardScreenContent(
        modifier = modifier,
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onPlantSourceSelected = { source ->
            viewModel.setPlantSource(source)
            if (source == PlantSource.ESP32_HARDWARE) {
                showEsp32SetupDialog = true
            }
        },
        onClaimQuest = { questId -> viewModel.claimQuest(questId) },
        onScanForEsp32 = {
            showEsp32SetupDialog = true
            startScan()
        },
        leafScanState = leafScanState,
        onScanPhoto = { photo -> viewModel.performCameraScan(photo) },
        onWakeAiServer = { viewModel.wakeAiServer() },
        onDismissScanError = { viewModel.dismissScanError() },
        onAddJournalEntry = { note -> viewModel.addJournalEntry(note, JournalCategory.GENERAL) },
        onSetSowingDate = { viewModel.setSowingDate(it) },
        onResetStreak = { viewModel.resetStreak() },
        onAnswerQuiz = { optionIndex ->
            coroutineScope.launch { viewModel.submitQuizAnswer(optionIndex) }
        },
        onNextQuizQuestion = { viewModel.nextQuizQuestion() },
        wateringStatus = wateringStatus,
        onWaterPlant = { viewModel.waterPlant() },
        snackbarHostState = snackbarHostState
    )
}

/**
 * State-driven screen switcher rendering the active tab content:
 * - Tab 0 (HOME): HomeDashboardContent
 * - Tab 1 (PLANT): PlantDetailsScreenContent
 * - Tab 2 (QUESTS): QuestsScreenContent
 * - Tab 3 (AI): AiDiagnosticsScreenContent
 * - Tab 4 (AWARDS): AwardsScreenContent
 */
@Composable
fun DashboardScreenContent(
    modifier: Modifier = Modifier,
    uiState: AgriQuestUiState = AgriQuestUiState(),
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onPlantSourceSelected: (PlantSource) -> Unit = {},
    onClaimQuest: (String) -> Unit = {},
    onScanForEsp32: () -> Unit = {},
    leafScanState: LeafScanState = LeafScanState(),
    onScanPhoto: (Uri) -> Unit = {},
    onWakeAiServer: () -> Unit = {},
    onDismissScanError: () -> Unit = {},
    onAddJournalEntry: (String) -> Unit = {},
    onSetSowingDate: (String) -> Unit = {},
    onResetStreak: () -> Unit = {},
    onAnswerQuiz: (Int) -> Unit = {},
    onNextQuizQuestion: () -> Unit = {},
    wateringStatus: WateringStatus? = null,
    onWaterPlant: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    when (selectedTab) {
        1 -> PlantDetailsScreenContent(
            modifier = modifier,
            uiState = uiState,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onAddJournalEntry = onAddJournalEntry,
            onSetSowingDate = onSetSowingDate,
            snackbarHostState = snackbarHostState,
            wateringStatus = wateringStatus,
            onWaterPlant = onWaterPlant
        )
        2 -> QuestsScreenContent(
            modifier = modifier,
            uiState = uiState,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onCompleteQuest = onClaimQuest,
            onAnswerQuiz = onAnswerQuiz,
            onNextQuizQuestion = onNextQuizQuestion,
            snackbarHostState = snackbarHostState
        )
        3 -> AiDiagnosticsScreenContent(
            modifier = modifier,
            uiState = uiState,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            scanState = leafScanState,
            onScanPhoto = onScanPhoto,
            onWakeServer = onWakeAiServer,
            onDismissScanError = onDismissScanError,
            onAddJournalEntry = onAddJournalEntry,
            snackbarHostState = snackbarHostState
        )
        4 -> AwardsScreenContent(
            modifier = modifier,
            uiState = uiState,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onResetStreak = onResetStreak,
            snackbarHostState = snackbarHostState
        )
        else -> HomeDashboardContent(
            modifier = modifier,
            uiState = uiState,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onPlantSourceSelected = onPlantSourceSelected,
            onClaimQuest = onClaimQuest,
            onScanForEsp32 = onScanForEsp32,
            snackbarHostState = snackbarHostState
        )
    }
}

/** Home plant scene and daily overview. */
@Composable
fun HomeDashboardContent(
    modifier: Modifier = Modifier,
    uiState: AgriQuestUiState = AgriQuestUiState(),
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onPlantSourceSelected: (PlantSource) -> Unit = {},
    onClaimQuest: (String) -> Unit = {},
    onScanForEsp32: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var showSourceSettings by remember { mutableStateOf(false) }
    AppScreen(modifier = modifier, uiState = uiState, selectedTab = selectedTab,
        onTabSelected = onTabSelected, snackbarHostState = snackbarHostState) {
        Box(Modifier.fillMaxWidth().height(300.dp)
            .clip(AppLayout.plantBackgroundShape)
            .border(AppLayout.cardBorderWidth, GardenBorder, AppLayout.plantBackgroundShape)) {
            GardenBackground(Modifier.matchParentSize())
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                PixelChip(uiState.plant.growthStage.stageLabel)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { showSourceSettings = true }) {
                        Icon(Icons.Rounded.Settings, "Plant connection settings", tint = AppAccentBackground)
                    }
                    if (uiState.plantSource == PlantSource.ESP32_HARDWARE) {
                        PixelChip("ESP32: " + uiState.connection.statusText)
                    }
                }
            }
            PlantAvatarCanvas(growthStage = uiState.plant.growthStage, size = 245.dp,
                modifier = Modifier.align(Alignment.BottomCenter))
        }
        PixelPanel {
            PlantLevelBar(uiState)
            HorizontalDivider(color = AppCardBorder)
            TelemetryGrid(telemetry = uiState.telemetry, compact = true, embedded = true)
            HorizontalDivider(color = AppCardBorder)
            PixelHeading("Tomato Stages")
            PlantStagesTimeline()
            HorizontalDivider(color = AppCardBorder)
            PixelHeading("Today's Quests", trailing = {
                PixelChip("${uiState.quests.count { it.isClaimed }}/${uiState.quests.size} Completed")
            })
            Text("Complete your daily plant care tasks", style = MaterialTheme.typography.labelSmall, color = AppTextColor)
            uiState.quests.take(3).forEach { quest ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(quest.isClaimed, onCheckedChange = { if (!quest.isClaimed && quest.isClaimable) onClaimQuest(quest.id) },
                        enabled = quest.isClaimed || quest.isClaimable)
                    Text(quest.title, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = AppTextColor)
                    Text(quest.xpRewardText, style = MaterialTheme.typography.labelMedium, color = PixelGreen)
                }
            }
            PixelButton("View More →", { onTabSelected(2) }, Modifier.align(Alignment.End))
        }
    }
    if (showSourceSettings) AlertDialog(
        onDismissRequest = { showSourceSettings = false },
        title = { Text("Plant source") },
        text = {
            Column {
                PlantSource.entries.forEach { source ->
                    TextButton(onClick = { showSourceSettings = false; onPlantSourceSelected(source) }) {
                        Text(source.displayName)
                    }
                }
                if (uiState.plantSource == PlantSource.ESP32_HARDWARE)
                    TextButton(onClick = { showSourceSettings = false; onScanForEsp32() }) { Text("Connect ESP32") }
            }
        },
        confirmButton = { TextButton({ showSourceSettings = false }) { Text("Done") } }
    )
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun DashboardScreenPreview() {
    AgriQuestTheme {
        DashboardScreenContent()
    }
}
