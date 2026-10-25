package com.agriquest.ui.plant

import com.agriquest.data.repository.AgriQuestRepositoryImpl
import com.agriquest.domain.model.JournalCategory
import com.agriquest.ui.state.AgriQuestUiState
import com.agriquest.ui.viewmodel.AgriQuestViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class PlantDetailsUnitTest {

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
    fun plantDetails_defaultUiState_matchesTaskObjectives() {
        val uiState = AgriQuestUiState()
        val plant = uiState.plant

        assertEquals("Roma Rosso 🍅", plant.title)
        assertEquals(0, plant.dayCount)
        assertEquals(92, plant.healthPercent)
        assertEquals("Stage 1 - Germinating Tomato Seed", plant.stageTag)
        assertEquals("Observe your plant to assess its condition", plant.healthSubtext)

        // Telemetry grid readings matching Task Objectives
        assertEquals(68.0f, uiState.telemetry.soilMoisture, 0.01f)
        assertEquals(24.5f, uiState.telemetry.airTemperature, 0.01f)
        assertEquals(62.0f, uiState.telemetry.relativeHumidity, 0.01f)
        assertEquals(14200.0f, uiState.telemetry.lightIntensity, 0.01f)

        // Journal Entries list verification
        val journal = uiState.journalEntries
        assertEquals(3, journal.size)
        assertEquals("Aug 12", journal[0].date)
        assertEquals("Watered 250ml", journal[0].note)
        assertEquals("Aug 10", journal[1].date)
        assertEquals("Pruned bottom branches", journal[1].note)
        assertEquals("Aug 5", journal[2].date)
        assertEquals("Planted seedling", journal[2].note)

        // Moisture History timeline verification ("Today", "Yesterday", "2 days ago")
        val history = uiState.moistureHistory
        assertEquals(5, history.size)
        assertTrue(history.any { it.dayLabel == "Today" && it.moisturePercent == 68.0f })
        assertTrue(history.any { it.dayLabel == "Yesterday" && it.moisturePercent == 64.0f })
        assertTrue(history.any { it.dayLabel == "2 days ago" && it.moisturePercent == 70.0f })
    }

    @Test
    fun viewModel_addJournalEntry_updatesJournalInUiState() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        viewModel.addJournalEntry("Added organic compost", JournalCategory.FERTILIZING)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedJournal = viewModel.uiState.value.journalEntries
        assertEquals(4, updatedJournal.size)
        assertEquals("Added organic compost", updatedJournal[0].note)
        assertEquals(JournalCategory.FERTILIZING, updatedJournal[0].category)
    }
}
