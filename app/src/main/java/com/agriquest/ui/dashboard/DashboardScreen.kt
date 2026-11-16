package com.agriquest.ui.dashboard

import android.net.Uri
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.agriquest.ui.sound.GameSound
import com.agriquest.ui.sound.GameSounds
import com.agriquest.ui.tutorial.Tutorial
import com.agriquest.ui.tutorial.TutorialOverlay
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
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val leafScanState by viewModel.leafScanState.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val wateringStatus by viewModel.wateringStatus.collectAsStateWithLifecycle()
    val fanOn by viewModel.fanOn.collectAsStateWithLifecycle()
    val lightOn by viewModel.lightOn.collectAsStateWithLifecycle()
    val soundMuted by GameSounds.muted.collectAsStateWithLifecycle()
    val wardrobe by viewModel.wardrobe.collectAsStateWithLifecycle()
    val watering = wateringStatus?.phase == WateringStatus.Phase.RUNNING || uiState.actuator.isPumpActive
    LaunchedEffect(watering) { if (watering) GameSounds.play(GameSound.WATER) }
    // Quiet garden ambience while the game is on screen.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    GameSounds.startAmbient()
                    com.agriquest.notifications.MissedDayReminder.markPlayed(context)
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> GameSounds.stopAmbient()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            GameSounds.startAmbient()
            com.agriquest.notifications.MissedDayReminder.markPlayed(context)
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); GameSounds.stopAmbient() }
    }
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
            // One message, one place: the snackbar (a Toast as well showed every message twice).
            snackbarHostState.showSnackbar(message)
        }
    }

    // "Connect" screen at the start of each game, before the plant info is shown.
    var showConnectGate by rememberSaveable { mutableStateOf(true) }
    val usingHardware = uiState.plantSource == PlantSource.ESP32_HARDWARE
    val hardwareConnected = usingHardware && uiState.connection.status == ConnectionStatus.CONNECTED
    LaunchedEffect(hardwareConnected) {
        if (hardwareConnected && showConnectGate) {
            kotlinx.coroutines.delay(1_500L)
            showConnectGate = false
        }
    }
    // "Congratulations!" for each finished mission or quiz, one at a time.
    val pendingRewards = remember { androidx.compose.runtime.mutableStateListOf<com.agriquest.domain.model.Reward>() }
    LaunchedEffect(viewModel) { viewModel.rewards.collect { pendingRewards.add(it) } }
    pendingRewards.firstOrNull()?.let { reward -> RewardDialog(reward) { pendingRewards.removeAt(0) } }

    // First question before the game: how old is the plant? Then the Connect screen.
    val plantAgeConfirmed by viewModel.plantAgeConfirmed.collectAsStateWithLifecycle()
    if (plantAgeConfirmed == false) {
        PlantAgeDialog(initialDays = uiState.plant.dayCount,
            maxDays = com.agriquest.data.repository.AgriQuestRepositoryImpl.MAX_PLANT_AGE_DAYS,
            onConfirm = { viewModel.confirmPlantAge(it) })
    } else if (showConnectGate && plantAgeConfirmed == true) {
        ConnectGateDialog(
            connection = uiState.connection,
            usingHardware = usingHardware,
            onConnectEsp32 = {
                if (!usingHardware) viewModel.setPlantSource(PlantSource.ESP32_HARDWARE)
                showEsp32SetupDialog = true
                startScan()
            },
            onPlaySimulation = {
                viewModel.setPlantSource(PlantSource.EMULATED_OFFLINE)
                showConnectGate = false
            },
            onContinue = { showConnectGate = false }
        )
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

    // Step-by-step tutorial on first play (after the age question and Connect screen); replayable from settings.
    var tutorialStep by rememberSaveable { mutableIntStateOf(if (Tutorial.isDone(context)) -1 else 0) }
    val tutorialVisible = tutorialStep >= 0 && plantAgeConfirmed == true && !showConnectGate
    LaunchedEffect(tutorialStep, tutorialVisible) {
        if (tutorialVisible) selectedTab = Tutorial.STEPS[tutorialStep].tab
    }
    val endTutorial = { Tutorial.markDone(context); tutorialStep = -1; selectedTab = 0 }

    Box(modifier) {
    DashboardScreenContent(
        modifier = Modifier.fillMaxSize(),
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
        onWaterPlant = { GameSounds.play(GameSound.TAP); viewModel.waterPlant() },
        snackbarHostState = snackbarHostState,
        actuators = ActuatorUi(watering = watering, fanOn = fanOn, lightOn = lightOn,
            onFan = { GameSounds.play(GameSound.FAN); viewModel.toggleFan() },
            onLight = { GameSounds.play(GameSound.LIGHT); viewModel.toggleLight() },
            soundMuted = soundMuted, onToggleSound = { GameSounds.setMuted(!soundMuted) },
            onReplayTutorial = { tutorialStep = 0 },
            wardrobe = wardrobe,
            onBuyOutfit = { GameSounds.play(GameSound.SUCCESS); viewModel.buyOutfit(it) },
            onToggleOutfit = { GameSounds.play(GameSound.TAP); viewModel.toggleOutfit(it) })
    )
    if (tutorialVisible) TutorialOverlay(
        step = tutorialStep,
        onNext = { GameSounds.play(GameSound.TAP); if (tutorialStep == Tutorial.STEPS.lastIndex) endTutorial() else tutorialStep++ },
        onBack = { if (tutorialStep > 0) tutorialStep-- },
        onSkip = endTutorial
    )
    }
}

/** State and actions for the Water / Fan / Light buttons on the home scene. */
data class ActuatorUi(
    val watering: Boolean = false,
    val fanOn: Boolean = false,
    val lightOn: Boolean = false,
    val onFan: () -> Unit = {},
    val onLight: () -> Unit = {},
    val soundMuted: Boolean = false,
    val onToggleSound: () -> Unit = {},
    val onReplayTutorial: () -> Unit = {},
    val wardrobe: com.agriquest.domain.model.Wardrobe = com.agriquest.domain.model.Wardrobe(),
    val onBuyOutfit: (String) -> Unit = {},
    val onToggleOutfit: (String) -> Unit = {}
)

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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    actuators: ActuatorUi = ActuatorUi()
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
            snackbarHostState = snackbarHostState,
            wardrobe = actuators.wardrobe,
            onBuyOutfit = actuators.onBuyOutfit,
            onToggleOutfit = actuators.onToggleOutfit
        )
        else -> HomeDashboardContent(
            modifier = modifier,
            uiState = uiState,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onPlantSourceSelected = onPlantSourceSelected,
            onClaimQuest = onClaimQuest,
            onScanForEsp32 = onScanForEsp32,
            snackbarHostState = snackbarHostState,
            actuators = actuators,
            onWaterPlant = onWaterPlant,
            wateringStatus = wateringStatus
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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    actuators: ActuatorUi = ActuatorUi(),
    onWaterPlant: () -> Unit = {},
    wateringStatus: WateringStatus? = null
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
            PlantScene(dayCount = uiState.plant.dayCount, watering = actuators.watering,
                fanOn = actuators.fanOn, lightOn = actuators.lightOn, plantSize = 245.dp,
                outfits = actuators.wardrobe.equippedOutfits)
        }
        ActuatorBar(watering = actuators.watering, fanOn = actuators.fanOn, lightOn = actuators.lightOn,
            canWater = uiState.telemetry.needsWatering, onWater = onWaterPlant, onFan = actuators.onFan,
            onLight = actuators.onLight,
            note = wateringStatus?.message ?: if (uiState.plantSource == PlantSource.ESP32_HARDWARE)
                "Water runs the real ESP32 pump. Fan and Light are shown in the game only for now." else null)
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
                TextButton(onClick = { showSourceSettings = false; actuators.onReplayTutorial() }) { Text("Replay tutorial") }
                TextButton(onClick = actuators.onToggleSound) {
                    Text(if (actuators.soundMuted) "Sound: Off (tap to turn on)" else "Sound: On (tap to mute)")
                }
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
