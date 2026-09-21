package com.agriquest.ui.ai

import com.agriquest.data.repository.AgriQuestRepositoryImpl
import com.agriquest.domain.model.AiScanResult
import com.agriquest.domain.model.TelemetryData
import com.agriquest.ui.viewmodel.AgriQuestViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class AiDiagnosticsUnitTest {

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
    fun recentScans_defaultList_containsHealthyLeafAndEarlyBlightCards() {
        val defaultScans = AiScanResult.DEFAULT_SCANS
        assertEquals(2, defaultScans.size)

        // Card 1: Healthy Leaf
        val card1 = defaultScans[0]
        assertEquals("Healthy Leaf", card1.title)
        assertEquals("Leaf 4 (middle branch) • Sep 12 - 2:14 PM", card1.subtext)
        assertEquals("97%", card1.confidence)
        assertTrue(card1.isHealthy)

        // Card 2: Early Blight
        val card2 = defaultScans[1]
        assertEquals("Early Blight", card2.title)
        assertEquals("Leaf 7 (lower) • Sep 10 - 11:02 AM", card2.subtext)
        assertEquals("89%", card2.confidence)
        assertFalse(card2.isHealthy)
    }

    @Test
    fun iotConditions_telemetryData_containsExpected2x2CompactGridReadings() {
        val telemetry = TelemetryData(
            soilMoisture = 68.0f,
            airTemperature = 24.0f,
            relativeHumidity = 62.0f,
            lightIntensity = 4200.0f
        )

        assertEquals(68.0f, telemetry.soilMoisture, 0.01f)
        assertEquals(24.0f, telemetry.airTemperature, 0.01f)
        assertEquals(62.0f, telemetry.relativeHumidity, 0.01f)
        assertEquals(4200.0f, telemetry.lightIntensity, 0.01f)

        // Light klux formatting check
        val lightKlux = String.format(Locale.US, "%.1f", telemetry.lightIntensity / 1000f)
        assertEquals("4.2", lightKlux)
    }

    @Test
    fun cameraScanHeroCard_triggerScan_addsNewLogEntryAndAwards25Xp() = runTest {
        val repository = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repository)

        var emittedToast: String? = null
        val job = launch {
            viewModel.notificationEvents.collect { message ->
                emittedToast = message
            }
        }

        val initialScanCount = viewModel.uiState.value.recentScans.size
        val initialXp = viewModel.uiState.value.plant.currentXp

        // Simulate user clicking "Scan Tomato Leaf" camera action
        viewModel.performCameraScan()
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState = viewModel.uiState.value

        // Check new scan entry added
        assertEquals(initialScanCount + 1, updatedState.recentScans.size)
        val latestScan = updatedState.recentScans.first()
        assertEquals("Healthy Leaf", latestScan.title)
        assertEquals("98%", latestScan.confidence)

        // Check +25 XP awarded
        assertEquals(initialXp + 25, updatedState.plant.currentXp)

        // Check notification Toast emitted
        assertTrue(emittedToast != null)
        assertTrue(emittedToast?.contains("+25 XP awarded!") == true)

        job.cancel()
    }
}
