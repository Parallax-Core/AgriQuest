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
    fun dailyQuests_defaultList_contains3PlantCareQuestsWithDifficultyAndSubtext() {
        val dailyQuests = Quest.DAILY_QUESTS
        assertEquals(13, dailyQuests.size)

        // Quest 1: Prune Lower Leaves
        val q1 = dailyQuests[0]
        assertEquals("quest_prune_leaves", q1.id)
        assertEquals("Prune Lower Leaves", q1.title)
        assertEquals("Easy", q1.difficulty)
        assertEquals(50, q1.xpReward)
        assertEquals("+50 XP", q1.xpRewardText)
        assertEquals("Remove yellowing bottom leaves to improve airflow and prevent disease.", q1.subtext)
        assertEquals("2 / 2 leaves", q1.progressText)

        // Quest 2: Deep Watering Session
        val q2 = dailyQuests[1]
        assertEquals("quest_deep_watering", q2.id)
        assertEquals("Deep Watering Session", q2.title)
        assertEquals("Easy", q2.difficulty)
        assertEquals(75, q2.xpReward)
        assertEquals("+75 XP", q2.xpRewardText)
        assertEquals("Water thoroughly until soil moisture reaches 75%. Let the roots drink deeply.", q2.subtext)
        assertEquals("75% Moisture Goal", q2.progressText)

        // Quest 3: Stake Main Stem
        val q3 = dailyQuests[2]
        assertEquals("quest_stake_stem", q3.id)
        assertEquals("Stake Main Stem", q3.title)
        assertEquals("Medium", q3.difficulty)
        assertEquals(100, q3.xpReward)
        assertEquals("+100 XP", q3.xpRewardText)
        assertEquals("Add support stake to prevent stem bend as tomatoes grow heavier.", q3.subtext)
        assertEquals("1 / 1 stake placed", q3.progressText)
    }

    @Test
    fun dailyQuests_totalAvailableXp_coversFinalStage() {
        val totalAvailableXp = Quest.DAILY_QUESTS.sumOf { it.xpReward }
        assertEquals(1225, totalAvailableXp)
        assertTrue(totalAvailableXp >= PlantGrowthStage.RIPENING.maxXp)
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

        // Tapping "Mark Complete" on Prune Lower Leaves (+50 XP)
        viewModel.claimQuest("quest_prune_leaves")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val quest = state.quests.find { it.id == "quest_prune_leaves" }
        assertTrue(quest?.isClaimed == true)
        assertEquals(50, state.plant.currentXp)
        assertEquals(0, state.plant.streakDays)
        assertTrue(emittedMessage != null)
        assertTrue(emittedMessage?.contains("🎉 Quest Completed! +50 XP awarded!") == true)

        job.cancel()
    }

    @Test
    fun completeAllDailyQuests_reachesFinalStageAndTriggersStreakIncrement() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        // Initial XP: 0, Level 0 (100 XP max), Streak 0
        assertEquals(0, viewModel.uiState.value.plant.currentXp)
        assertEquals(0, viewModel.uiState.value.plant.streakDays)

        // Complete Quest 1 (+50 XP) - streak remains 0
        viewModel.claimQuest("quest_prune_leaves")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(50, viewModel.uiState.value.plant.currentXp)
        assertEquals(0, viewModel.uiState.value.plant.streakDays)

        // Complete Quest 2 (+75 XP) -> Total 125 XP -> Levels up to Level 1 VEGETATIVE_LEAFY, streak remains 0
        viewModel.claimQuest("quest_deep_watering")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(125, viewModel.uiState.value.plant.currentXp)
        assertEquals(0, viewModel.uiState.value.plant.streakDays)
        assertEquals(PlantGrowthStage.VEGETATIVE_LEAFY, viewModel.uiState.value.plant.growthStage)

        // The first three tasks earn 225 XP; the ten new tasks complete the set.
        viewModel.claimQuest("quest_stake_stem")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(225, viewModel.uiState.value.plant.currentXp)
        assertEquals(0, viewModel.uiState.value.plant.streakDays)
        Quest.ADDITIONAL_QUESTS.forEach { viewModel.claimQuest(it.id) }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1225, viewModel.uiState.value.plant.currentXp)
        assertEquals(PlantGrowthStage.RIPENING, viewModel.uiState.value.plant.growthStage)
        assertEquals(1, viewModel.uiState.value.plant.streakDays)
    }

    @Test
    fun simulateNextDay_resetsAll13Quests() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        viewModel.simulateNextDay()
        testDispatcher.scheduler.advanceUntilIdle()

        val quests = viewModel.uiState.value.quests
        assertEquals(13, quests.size)
        assertTrue(quests.all { !it.isClaimed })
    }
}
