package com.agriquest.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agriquest.domain.model.ConnectionStatus
import com.agriquest.domain.model.PlantSource
import com.agriquest.ui.ai.AiDiagnosticsScreenContent
import com.agriquest.ui.awards.AwardsScreenContent
import com.agriquest.ui.components.ActuatorControlsCard
import com.agriquest.ui.components.AgriQuestBottomNav
import com.agriquest.ui.components.DailyQuestsDashboardSection
import com.agriquest.ui.components.Esp32HardwareSetupDialog
import com.agriquest.ui.components.PlantAvatarHeroCard
import com.agriquest.ui.components.PlantStagesCard
import com.agriquest.ui.components.QuestBanner
import com.agriquest.ui.components.TelemetryGrid
import com.agriquest.ui.components.TopLevelBanner
import com.agriquest.ui.plant.PlantDetailsScreenContent
import com.agriquest.ui.quests.QuestsScreenContent
import com.agriquest.ui.state.AgriQuestUiState
import com.agriquest.ui.theme.AgriQuestTheme
import com.agriquest.ui.theme.Slate50
import com.agriquest.ui.viewmodel.AgriQuestViewModel

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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEsp32SetupDialog by remember { mutableStateOf(false) }

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
            onStartScan = { viewModel.startEsp32BleScan() },
            onConnectDevice = { device ->
                viewModel.setConnectionStatus(
                    ConnectionStatus.CONNECTED,
                    device.deviceName
                )
                viewModel.showNotification("📡 Connected to BLE device: ${device.deviceName}")
                showEsp32SetupDialog = false
            }
        )
    }

    DashboardScreenContent(
        modifier = modifier,
        uiState = uiState,
        selectedTab = selectedTab,
        onTabSelected = { tabIndex ->
            if (tabIndex == 3 && uiState.plant.streakDays < uiState.plant.targetStreakDays) {
                val message = "🔒 AI Diagnostic Scan unlocks at 7-day streak! Current streak: ${uiState.plant.streakDays}/${uiState.plant.targetStreakDays} days."
                viewModel.showNotification(message)
            }
            selectedTab = tabIndex
        },
        onPlantSourceSelected = { source ->
            viewModel.setPlantSource(source)
            if (source == PlantSource.ESP32_HARDWARE) {
                showEsp32SetupDialog = true
            }
        },
        onClaimQuest = { questId -> viewModel.claimQuest(questId) },
        onSimulateNextDay = { viewModel.simulateNextDay() },
        onTriggerPump = { viewModel.triggerPump(3) },
        onToggleFanMode = { viewModel.toggleFanMode() },
        onScanForEsp32 = {
            showEsp32SetupDialog = true
            viewModel.startEsp32BleScan()
        },
        onPerformScan = { viewModel.performCameraScan() },
        onResetStreak = { viewModel.resetStreak() },
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
    onSimulateNextDay: () -> Unit = {},
    onTriggerPump: () -> Unit = {},
    onToggleFanMode: () -> Unit = {},
    onScanForEsp32: () -> Unit = {},
    onPerformScan: () -> Unit = {},
    onResetStreak: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    when (selectedTab) {
        1 -> PlantDetailsScreenContent(
            modifier = modifier,
            uiState = uiState,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onBackClick = { onTabSelected(0) },
            snackbarHostState = snackbarHostState
        )
        2 -> QuestsScreenContent(
            modifier = modifier,
            uiState = uiState,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onCompleteQuest = onClaimQuest,
            onSimulateNextDay = onSimulateNextDay,
            snackbarHostState = snackbarHostState
        )
        3 -> AiDiagnosticsScreenContent(
            modifier = modifier,
            uiState = uiState,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onPerformScan = onPerformScan,
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
            onTriggerPump = onTriggerPump,
            onToggleFanMode = onToggleFanMode,
            onScanForEsp32 = onScanForEsp32,
            snackbarHostState = snackbarHostState
        )
    }
}

/**
 * Tab 0: Home Dashboard Content displaying TopLevelBanner, PlantAvatarHeroCard,
 * TelemetryGrid, PlantStagesCard, DailyQuestsDashboardSection, QuestBanner, and ActuatorControlsCard.
 */
@Composable
fun HomeDashboardContent(
    modifier: Modifier = Modifier,
    uiState: AgriQuestUiState = AgriQuestUiState(),
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onPlantSourceSelected: (PlantSource) -> Unit = {},
    onClaimQuest: (String) -> Unit = {},
    onTriggerPump: () -> Unit = {},
    onToggleFanMode: () -> Unit = {},
    onScanForEsp32: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate50,
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
                .background(Slate50),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Top Level Banner (Sky Blue Header)
                TopLevelBanner(
                    userLevel = uiState.plant.level,
                    userLevelInt = uiState.plant.levelInt,
                    currentXp = uiState.plant.currentXp,
                    maxXp = uiState.plant.maxXp,
                    streakDays = uiState.plant.streakDays,
                    targetStreakDays = uiState.plant.targetStreakDays,
                    speechBubble = uiState.plant.speechBubble,
                    connectionState = uiState.connection,
                    selectedPlantSource = uiState.plantSource,
                    onPlantSourceSelected = onPlantSourceSelected,
                    onScanForEsp32 = onScanForEsp32
                )

                // 2. Digital Twin / Plant Avatar Hero Card + Active Floating Moodles
                PlantAvatarHeroCard(
                    plant = uiState.plant,
                    activeMoodles = uiState.activeMoodles
                )

                // 3. Environment Conditions Section (4 Real-Time Telemetry Cards)
                TelemetryGrid(
                    telemetry = uiState.telemetry
                )

                // 4. Plant Stages Timeline Section
                PlantStagesCard(
                    currentStage = uiState.plant.growthStage,
                    onViewMoreClick = { onTabSelected(1) }
                )

                // 5. Daily Quests Section
                DailyQuestsDashboardSection(
                    quests = uiState.quests,
                    onClaimQuest = onClaimQuest,
                    onViewAllQuestsClick = { onTabSelected(2) }
                )

                // 6. Active Quest Banner
                QuestBanner(
                    quest = uiState.activeQuest,
                    quests = uiState.quests,
                    onClaimQuest = onClaimQuest
                )

                // 7. Controls Section (2 Dark Teal/Blue Buttons)
                ActuatorControlsCard(
                    actuatorState = uiState.actuator,
                    onTriggerPump = onTriggerPump,
                    onToggleFanMode = onToggleFanMode
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun DashboardScreenPreview() {
    AgriQuestTheme {
        DashboardScreenContent()
    }
}
