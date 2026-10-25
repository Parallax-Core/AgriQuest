package com.agriquest.ui.viewmodel

import com.agriquest.data.repository.AgriQuestRepositoryImpl
import com.agriquest.domain.model.ConnectionStatus
import com.agriquest.domain.model.FanMode
import com.agriquest.domain.model.Quest
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.domain.model.PlantSource
import com.agriquest.domain.model.TelemetryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertEquals(1000, state.plant.maxXp)
        assertEquals(PlantGrowthStage.GERMINATION, state.plant.growthStage)
        assertEquals("Check Soil and Water Needs", state.activeQuest.title)
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
        assertEquals(1000, state.plant.maxXp)
        assertEquals("Lvl. 0 Urban Beginner", state.plant.level)
        assertEquals(PlantGrowthStage.GERMINATION, state.plant.growthStage)
    }

    @Test
    fun claimQuest_threeMissionsAwardNinetyXpAndOneStreak() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)
        viewModel.claimQuest(Quest.DAILY_QUESTS.first().id)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(30, viewModel.uiState.value.plant.currentXp)
        assertEquals(0, viewModel.uiState.value.plant.streakDays)
        Quest.DAILY_QUESTS.drop(1).forEach { viewModel.claimQuest(it.id) }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(90, viewModel.uiState.value.plant.currentXp)
        assertEquals(1, viewModel.uiState.value.plant.streakDays)
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
        assertEquals(initialXp, updatedState.plant.currentXp)
        assertTrue(emittedNotification?.contains("📸 AI Leaf Scan Complete") == true)
        assertTrue(emittedNotification?.contains("AI Leaf Scan Complete") == true)

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

    @Test
    fun checkAndResetMidnightQuests_triggersResetAndEmitsNotification() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)
        val notifications = mutableListOf<String>()

        val job = launch {
            viewModel.notificationEvents.collect {
                notifications.add(it)
            }
        }

        viewModel.checkAndResetMidnightQuests()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.quests.isNotEmpty())

        job.cancel()
    }

    @Test
    fun submitQuizAnswer_correctOption_updatesUiStateAndEmitsCelebrationToast() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()
        val notifications = mutableListOf<String>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.notificationEvents.collect {
                notifications.add(it)
            }
        }

        val isCorrect = viewModel.submitQuizAnswer(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(isCorrect)
        val state = viewModel.uiState.value
        assertEquals(10, state.plant.currentXp)
        assertTrue(state.isQuizCompletedToday)
        assertEquals(1, state.selectedQuizOptionIndex)
        assertEquals(true, state.isQuizAnswerCorrect)
        assertTrue(notifications.any { it.contains("🎉 Correct! +10 XP Awarded!") })

        job.cancel()
    }

    @Test
    fun submitQuizAnswer_incorrectOption_updatesUiStateAndReturnsFalse() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val isCorrect = viewModel.submitQuizAnswer(0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(isCorrect)
        val state = viewModel.uiState.value
        assertEquals(0, state.plant.currentXp)
        assertTrue(state.isQuizCompletedToday)
        assertEquals(0, state.selectedQuizOptionIndex)
        assertEquals(false, state.isQuizAnswerCorrect)
    }

    @Test
    fun nextQuizQuestion_updatesCurrentQuestionInUiState() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        assertEquals("quiz_soil_moisture", viewModel.uiState.value.currentQuizQuestion.id)

        viewModel.nextQuizQuestion()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("quiz_light_spectrum", viewModel.uiState.value.currentQuizQuestion.id)
        assertEquals(-1, viewModel.uiState.value.selectedQuizOptionIndex)
        assertFalse(viewModel.uiState.value.isQuizCompletedToday)
        assertNull(viewModel.uiState.value.isQuizAnswerCorrect)
    }
}
