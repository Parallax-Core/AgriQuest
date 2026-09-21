package com.agriquest.ui.viewmodel

import com.agriquest.data.repository.AgriQuestRepositoryImpl
import com.agriquest.domain.model.ConnectionStatus
import com.agriquest.domain.model.FanMode
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.domain.model.PlantSource
import com.agriquest.domain.model.TelemetryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgriQuestViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun viewModel_initialUiState_containsDefaultDomainModels() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        val state = viewModel.uiState.value
        assertEquals(68.0f, state.telemetry.soilMoisture, 0.01f)
        assertEquals("Flora", state.plant.name)
        assertEquals("Lvl. 0 Urban Beginner", state.plant.level)
        assertEquals(0, state.plant.currentXp)
        assertEquals(100, state.plant.maxXp)
        assertEquals(PlantGrowthStage.SEEDLING_SPROUT, state.plant.growthStage)
        assertEquals("⚡ Thirsty Roots: Keep soil moisture above 60%", state.activeQuest.title)
        assertEquals(PlantSource.EMULATED_OFFLINE, state.plantSource)
        assertEquals("ESP32", state.connection.deviceName)
        assertEquals(FanMode.AUTO, state.actuator.fanMode)
    }

    @Test
    fun addXp_updatesUiStateAndLeveling() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        viewModel.addXp(100)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(100, state.plant.currentXp)
        assertEquals(250, state.plant.maxXp)
        assertEquals("Lvl. 1 Grower", state.plant.level)
        assertEquals(PlantGrowthStage.VEGETATIVE_LEAFY, state.plant.growthStage)
    }

    @Test
    fun claimQuest_individualQuestAwardsXpAndCompletingAllIncrementsStreak() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)
        val notifications = mutableListOf<String>()

        val job = launch {
            viewModel.notificationEvents.collect {
                notifications.add(it)
            }
        }

        // Claim Quest 1 (+50 XP) - streak remains 0
        viewModel.claimQuest("quest_prune_leaves")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val quest = state.quests.find { it.id == "quest_prune_leaves" }
        assertTrue(quest?.isClaimed == true)
        assertEquals(50, state.plant.currentXp)
        assertEquals(0, state.plant.streakDays)
        assertTrue(notifications.last().contains("🎉 Quest Completed! +50 XP awarded!"))

        // Claim Quest 2 (+75 XP) - streak remains 0
        viewModel.claimQuest("quest_deep_watering")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.plant.streakDays)

        // Claim Quest 3 (+100 XP) - ALL daily quests complete -> streak increments to 1 & celebration toast emitted!
        viewModel.claimQuest("quest_stake_stem")
        testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertEquals(225, finalState.plant.currentXp)
        assertEquals(1, finalState.plant.streakDays)
        assertTrue(notifications.last().contains("🎉 All Daily Quests Completed! Care Streak: 1 / 7 Days 🔥"))

        job.cancel()
    }

    @Test
    fun simulateNextDay_resetsQuestsAndEmitsNotification() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)
        var notification: String? = null

        val job = launch {
            viewModel.notificationEvents.collect {
                notification = it
            }
        }

        viewModel.simulateNextDay()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.quests.size)
        assertTrue(notification?.contains("🌅 Next Day Simulated!") == true)

        job.cancel()
    }

    @Test
    fun setPlantSource_updatesUiStatePlantSource() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        viewModel.setPlantSource(PlantSource.ESP32_HARDWARE)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PlantSource.ESP32_HARDWARE, viewModel.uiState.value.plantSource)
    }

    @Test
    fun toggleFanMode_updatesUiStateFanMode() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        viewModel.toggleFanMode()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(FanMode.ON, repo.actuatorState.value.fanMode)
    }

    @Test
    fun setConnectionStatus_updatesUiStateConnection() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        viewModel.setConnectionStatus(ConnectionStatus.DISCONNECTED)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ConnectionStatus.DISCONNECTED, repo.connectionState.value.status)
        assertEquals("Disconnected", repo.connectionState.value.statusText)
    }

    @Test
    fun updateTelemetry_updatesUiStateTelemetry() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        val newTelemetry = TelemetryData(
            soilMoisture = 75.0f,
            airTemperature = 25.0f,
            relativeHumidity = 62.0f,
            lightIntensity = 8000.0f
        )
        viewModel.updateTelemetry(newTelemetry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(75.0f, repo.telemetryData.value.soilMoisture, 0.01f)
    }

    @Test
    fun showNotification_emitsMessageToNotificationEvents() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)
        var receivedEvent: String? = null

        val job = launch {
            receivedEvent = viewModel.notificationEvents.first()
        }

        viewModel.showNotification("Test notification message")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Test notification message", receivedEvent)
        job.cancel()
    }

    @Test
    fun performCameraScan_addsScanAndAwards25XpAndEmitsNotification() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)
        var emittedNotification: String? = null

        val job = launch {
            viewModel.notificationEvents.collect {
                emittedNotification = it
            }
        }

        val initialScanCount = viewModel.uiState.value.recentScans.size
        val initialXp = viewModel.uiState.value.plant.currentXp

        viewModel.performCameraScan()
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertEquals(initialScanCount + 1, updatedState.recentScans.size)
        assertEquals(initialXp + 25, updatedState.plant.currentXp)
        assertTrue(emittedNotification?.contains("📸 AI Leaf Scan Complete") == true)
        assertTrue(emittedNotification?.contains("+25 XP awarded!") == true)

        job.cancel()
    }

    @Test
    fun streakActions_incrementAndReset_updatesStateAndEmitsNotifications() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)
        val notifications = mutableListOf<String>()

        val job = launch {
            viewModel.notificationEvents.collect {
                notifications.add(it)
            }
        }

        // Initial streak is 0
        assertEquals(0, viewModel.uiState.value.plant.streakDays)

        // Increment streak
        viewModel.incrementStreak()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.plant.streakDays)

        // Reset streak
        viewModel.resetStreak()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.plant.streakDays)

        assertTrue(notifications.any { it.contains("Care streak reset to 0 days") })

        job.cancel()
    }
}
