package com.agriquest.ui.quests

import com.agriquest.data.repository.AgriQuestRepositoryImpl
import com.agriquest.domain.model.Quest
import com.agriquest.domain.model.PlantGrowthStage
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuestsScreenUnitTest {

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
    fun dailyQuests_offerThreeSafeObservations() {
        val quests = Quest.DAILY_QUESTS
        assertEquals(3, quests.size)
        assertEquals(3, quests.map { it.id }.distinct().size)
        assertTrue(quests.all { it.xpReward == 30 && it.subtext.isNotBlank() })
        assertTrue(quests.first().subtext.contains("only if needed"))
    }

    @Test
    fun dailyQuests_leaveTenXpForQuiz() {
        assertEquals(90, Quest.DAILY_QUESTS.sumOf { it.xpReward })
        assertTrue(90 < PlantGrowthStage.GERMINATION.maxXp)
    }

    @Test
    fun completeQuest_individualQuestClaim_awardsXpWithoutIncrementingStreak() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        var emittedMessage: String? = null
        val job = launch {
            viewModel.notificationEvents.collect {
                emittedMessage = it
            }
        }

        // Tapping "Mark Complete" on Check Soil and Water Needs (+50 XP)
        viewModel.claimQuest("quest_review_moisture")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val quest = state.quests.find { it.id == "quest_review_moisture" }
        assertTrue(quest?.isClaimed == true)
        assertEquals(30, state.plant.currentXp)
        assertEquals(0, state.plant.streakDays)
        assertTrue(emittedMessage != null)
        assertTrue(emittedMessage?.contains("🎉 Quest Completed! +30 XP awarded!") == true)

        job.cancel()
    }

    @Test
    fun completeAllDailyQuests_recordsCareWithoutRushingGrowth() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)
        Quest.DAILY_QUESTS.forEach { viewModel.claimQuest(it.id) }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(90, viewModel.uiState.value.plant.currentXp)
        assertEquals(PlantGrowthStage.GERMINATION, viewModel.uiState.value.plant.growthStage)
        assertEquals(1, viewModel.uiState.value.plant.streakDays)
    }

    @Test
    fun simulateNextDay_resetsAll13Quests() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        viewModel.simulateNextDay()
        testDispatcher.scheduler.advanceUntilIdle()

        val quests = viewModel.uiState.value.quests
        assertEquals(3, quests.size)
        assertTrue(quests.all { !it.isClaimed })
    }
}
