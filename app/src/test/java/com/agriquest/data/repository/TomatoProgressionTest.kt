package com.agriquest.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.agriquest.data.local.AgriQuestDataStore
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.domain.model.Quest
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TomatoProgressionTest {
    private fun date(value: String) = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)!!

    @Test
    fun stageFollowsSowingDateEvenWithoutXpAndRejectsInvalidDates() = runTest {
        val repo = AgriQuestRepositoryImpl(coroutineScope = backgroundScope, now = { date("2026-05-01") })
        assertTrue(repo.setSowingDate("2026-01-01"))
        assertEquals(120, repo.plantAvatar.value.dayCount)
        assertEquals(PlantGrowthStage.RIPENING, repo.plantAvatar.value.growthStage)
        assertEquals(0, repo.plantAvatar.value.currentXp)
        assertFalse(repo.setSowingDate("2026-02-30"))
        assertFalse(repo.setSowingDate("2026-05-02"))
        assertFalse(repo.setSowingDate("invalid"))
        assertEquals("2026-01-01", repo.plantAvatar.value.sowingDate)
    }

    @Test
    fun allStageBoundariesMatchCalendarAndCareXpBudget() {
        val days = listOf(0, 10, 28, 56, 70, 105)
        PlantGrowthStage.entries.forEachIndexed { index, stage ->
            assertEquals(stage, PlantGrowthStage.fromDays(days[index]))
            if (index > 0) {
                assertEquals(PlantGrowthStage.entries[index - 1], PlantGrowthStage.fromDays(days[index] - 1))
                assertEquals(stage, PlantGrowthStage.fromXp(days[index] * PlantGrowthStage.DAILY_XP_LIMIT))
            }
        }
        assertEquals(12000, PlantGrowthStage.RIPENING.maxXp)
    }

    @Test
    fun dailyLimitRenewsWithoutBankingMissedDaysOrRewardingRepeatedQuiz() = runTest {
        var today = date("2026-01-01")
        val repo = AgriQuestRepositoryImpl(coroutineScope = backgroundScope, now = { today })
        repo.quests.value.forEach { repo.claimQuest(it.id) }
        assertTrue(repo.submitQuizAnswer(repo.currentQuizQuestion.value.correctOptionIndex))
        assertEquals(100, repo.plantAvatar.value.currentXp)
        repo.resetQuizState()
        repo.nextQuizQuestion()
        assertFalse(repo.submitQuizAnswer(repo.currentQuizQuestion.value.correctOptionIndex))
        repo.addXp(Int.MAX_VALUE)
        assertEquals(100, repo.plantAvatar.value.currentXp)
        today = date("2026-01-04")
        repo.checkAndResetMidnightQuests()
        assertEquals(0, repo.plantAvatar.value.dailyXp)
        assertTrue(repo.quests.value.all { !it.isClaimed })
        repo.addXp(Int.MAX_VALUE)
        assertEquals(200, repo.plantAvatar.value.currentXp)
        assertEquals(100, repo.plantAvatar.value.dailyXp)
        repo.addXp(-500)
        assertEquals(200, repo.plantAvatar.value.currentXp)
    }

    @Test
    fun savedLimitQuizClaimsAndSowingDateSurviveRepositoryRestart() = runTest {
        val file = File.createTempFile("tomato-progress-", ".preferences_pb")
        file.delete()
        val preferences = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        val store = AgriQuestDataStore(preferences)
        val first = AgriQuestRepositoryImpl(dataStore = store, coroutineScope = backgroundScope, now = { date("2026-03-01") })
        assertTrue(first.setSowingDate("2026-01-01"))
        val assigned = first.quests.value.map { it.id }
        first.claimQuest(Quest.DAILY_QUESTS.first().id)
        first.claimQuest(first.quests.value.last().id)
        first.submitQuizAnswer(first.currentQuizQuestion.value.correctOptionIndex)
        first.addXp(100)

        val restored = AgriQuestRepositoryImpl(dataStore = store, coroutineScope = backgroundScope, now = { date("2026-03-01") })
        restored.addXp(500) // Also waits for saved state to load before granting anything.
        assertEquals(100, restored.plantAvatar.value.currentXp)
        assertEquals(100, restored.plantAvatar.value.dailyXp)
        assertEquals("2026-01-01", restored.plantAvatar.value.sowingDate)
        assertEquals(assigned, restored.quests.value.map { it.id })
        assertTrue(restored.quests.value.first().isClaimed)
        assertTrue(restored.quests.value.last().isClaimed)
        assertTrue(restored.isQuizCompletedToday.value)
        assertFalse(restored.submitQuizAnswer(restored.currentQuizQuestion.value.correctOptionIndex))
        file.deleteOnExit()
    }

    @Test
    fun midnightChangesMissionAndClearsClaimsButKeepsSameDayAssignment() = runTest {
        var today = date("2026-12-31")
        val repo = AgriQuestRepositoryImpl(coroutineScope = backgroundScope, now = { today })
        val yesterday = repo.quests.value.map { it.id }.toSet()
        repo.quests.value.forEach { repo.claimQuest(it.id) }
        today = date("2027-01-01")
        assertTrue(repo.checkAndResetMidnightQuests())
        val assigned = repo.quests.value
        assertEquals(1, assigned.map { it.id }.toSet().subtract(yesterday).size)
        assertTrue(assigned.all { !it.isClaimed })
        repo.claimQuest(assigned.last().id)
        assertFalse(repo.checkAndResetMidnightQuests())
        assertEquals(assigned.map { it.id }, repo.quests.value.map { it.id })
        assertTrue(repo.quests.value.last().isClaimed)
        assertEquals(30, repo.plantAvatar.value.dailyXp)
    }
}
