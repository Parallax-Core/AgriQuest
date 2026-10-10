package com.agriquest.data.repository

import com.agriquest.domain.model.AgroQuizQuestion
import com.agriquest.domain.model.ConnectionState
import com.agriquest.domain.model.ConnectionStatus
import com.agriquest.domain.model.FanMode
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.domain.model.PlantSource
import com.agriquest.domain.model.Quest
import com.agriquest.domain.model.TelemetryData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgriQuestRepositoryTest {

    @Test
    fun repository_initialState_matchesLevel0AndDomainRequirements() {
        val repo = AgriQuestRepositoryImpl()

        val telemetry = repo.telemetryData.value
        assertEquals(68.0f, telemetry.soilMoisture, 0.01f)

        val plant = repo.plantAvatar.value
        assertEquals("Flora", plant.name)
        assertEquals("Lvl. 0 Urban Beginner", plant.level)
        assertEquals(0, plant.currentXp)
        assertEquals(100, plant.maxXp)
        assertEquals(PlantGrowthStage.SEEDLING_SPROUT, plant.growthStage)
        assertEquals("Vibrant & Healthy", plant.statusTag)
        assertEquals(0, plant.streakDays)

        val activeQuest = repo.activeQuest.value
        assertEquals("quest_prune_leaves", activeQuest.id)
        assertEquals("Prune Lower Leaves", activeQuest.title)

        val plantSource = repo.plantSource.value
        assertEquals(PlantSource.EMULATED_OFFLINE, plantSource)

        val connection = repo.connectionState.value
        assertEquals("ESP32", connection.deviceName)
        assertEquals(ConnectionStatus.CONNECTED, connection.status)

        val actuator = repo.actuatorState.value
        assertFalse(actuator.isPumpActive)
        assertEquals(FanMode.AUTO, actuator.fanMode)
    }

    @Test
    fun addXp_progressionAndLevelingUp_updatesAvatarAndStage() = runTest {
        val repo = AgriQuestRepositoryImpl()

        // 1. Initial State: Level 0, 0/100 XP
        assertEquals(0, repo.plantAvatar.value.currentXp)
        assertEquals(100, repo.plantAvatar.value.maxXp)
        assertEquals(PlantGrowthStage.SEEDLING_SPROUT, repo.plantAvatar.value.growthStage)

        // 2. Add 50 XP -> 50/100 XP, stays Level 0
        repo.addXp(50)
        assertEquals(50, repo.plantAvatar.value.currentXp)
        assertEquals(100, repo.plantAvatar.value.maxXp)
        assertEquals(PlantGrowthStage.SEEDLING_SPROUT, repo.plantAvatar.value.growthStage)

        // 3. Add 50 XP -> 100/250 XP, Levels up to Level 1 VEGETATIVE_LEAFY!
        repo.addXp(50)
        assertEquals(100, repo.plantAvatar.value.currentXp)
        assertEquals(250, repo.plantAvatar.value.maxXp)
        assertEquals(PlantGrowthStage.VEGETATIVE_LEAFY, repo.plantAvatar.value.growthStage)
        assertEquals("Lvl. 1 Grower", repo.plantAvatar.value.level)

        // 4. Add 150 XP -> 250/450 XP, Levels up to Level 2 FLOWERING!
        repo.addXp(150)
        assertEquals(250, repo.plantAvatar.value.currentXp)
        assertEquals(450, repo.plantAvatar.value.maxXp)
        assertEquals(PlantGrowthStage.FLOWERING, repo.plantAvatar.value.growthStage)
        assertEquals("Lvl. 2 Master", repo.plantAvatar.value.level)

        // 5. Add 200 XP -> 450/700 XP, Levels up to Level 3 FRUITING_HARVEST!
        repo.addXp(200)
        assertEquals(450, repo.plantAvatar.value.currentXp)
        assertEquals(700, repo.plantAvatar.value.maxXp)
        assertEquals(PlantGrowthStage.FRUITING_HARVEST, repo.plantAvatar.value.growthStage)
        assertEquals("Lvl. 3 Harvest Master", repo.plantAvatar.value.level)
    }

    @Test
    fun claimQuest_individualQuestDoesNotIncrementStreak_completingAllIncrementsStreak() = runTest {
        val repo = AgriQuestRepositoryImpl()

        val activeQuestId = repo.activeQuest.value.id
        assertEquals("quest_prune_leaves", activeQuestId)

        // Claim Quest 1 (+50 XP) - 1 of 3 complete -> Streak remains 0!
        val completedAll1 = repo.claimQuest("quest_prune_leaves")
        assertFalse(completedAll1)

        val q1 = repo.quests.value.find { it.id == "quest_prune_leaves" }
        assertTrue(q1?.isClaimed == true)
        assertEquals(50, repo.plantAvatar.value.currentXp)
        assertEquals(0, repo.plantAvatar.value.streakDays)

        // Claim Quest 2 (+75 XP) - 2 of 3 complete -> Streak remains 0!
        val completedAll2 = repo.claimQuest("quest_deep_watering")
        assertFalse(completedAll2)
        assertEquals(125, repo.plantAvatar.value.currentXp)
        assertEquals(0, repo.plantAvatar.value.streakDays)

        // Claim Quest 3 (+100 XP) - 3 of 3 complete -> ALL completed! Streak increments to 1!
        val completedAll3 = repo.claimQuest("quest_stake_stem")
        assertTrue(completedAll3)
        assertEquals(225, repo.plantAvatar.value.currentXp)
        assertEquals(1, repo.plantAvatar.value.streakDays)
    }

    @Test
    fun setPlantSource_updatesSourceStateFlow() = runTest {
        val repo = AgriQuestRepositoryImpl()

        assertEquals(PlantSource.EMULATED_OFFLINE, repo.plantSource.value)

        repo.setPlantSource(PlantSource.ESP32_HARDWARE)

        assertEquals(PlantSource.ESP32_HARDWARE, repo.plantSource.value)
    }

    @Test
    fun triggerPump_restoresMoistureBy15PercentAndAwards15XpWithoutIncrementingStreak() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val initialMoisture = repo.telemetryData.value.soilMoisture

        repo.triggerPump(durationSeconds = 1)

        val updatedMoisture = repo.telemetryData.value.soilMoisture
        assertEquals((initialMoisture + 15.0f).coerceAtMost(100.0f), updatedMoisture, 0.01f)
        assertEquals(15, repo.plantAvatar.value.currentXp)
        assertEquals(0, repo.plantAvatar.value.streakDays)
    }

    @Test
    fun resetDailyQuests_shuffles3QuestsAndResetsCompletionStatus() = runTest {
        val repo = AgriQuestRepositoryImpl()

        // Complete all initial quests
        repo.claimQuest("quest_prune_leaves")
        repo.claimQuest("quest_deep_watering")
        repo.claimQuest("quest_stake_stem")
        assertEquals(1, repo.plantAvatar.value.streakDays)

        // Reset & shuffle daily quests
        val newQuests = repo.resetDailyQuests()
        assertEquals(3, newQuests.size)
        assertTrue(newQuests.all { !it.isClaimed })
        assertEquals(newQuests, repo.quests.value)
        assertTrue(Quest.QUEST_POOL.containsAll(newQuests))
    }

    @Test
    fun toggleFanMode_cyclesCorrectly() {
        val repo = AgriQuestRepositoryImpl()

        assertEquals(FanMode.AUTO, repo.actuatorState.value.fanMode)

        repo.toggleFanMode()
        assertEquals(FanMode.ON, repo.actuatorState.value.fanMode)

        repo.toggleFanMode()
        assertEquals(FanMode.OFF, repo.actuatorState.value.fanMode)

        repo.toggleFanMode()
        assertEquals(FanMode.AUTO, repo.actuatorState.value.fanMode)
    }

    @Test
    fun updateTelemetryFromBle_updatesStateFlow() {
        val repo = AgriQuestRepositoryImpl()
        val newTelemetry = TelemetryData(
            soilMoisture = 72.0f,
            airTemperature = 26.0f,
            relativeHumidity = 60.0f,
            lightIntensity = 9000.0f
        )

        repo.updateTelemetryFromBle(newTelemetry)

        assertEquals(newTelemetry, repo.telemetryData.value)
    }

    @Test
    fun updateConnectionStatus_updatesState() {
        val repo = AgriQuestRepositoryImpl()
        val newConnection = ConnectionState(
            deviceName = "ESP32",
            status = ConnectionStatus.DISCONNECTED,
            statusText = "Disconnected"
        )

        repo.updateConnectionStatus(newConnection)

        assertEquals(newConnection, repo.connectionState.value)
    }

    @Test
    fun performCameraScan_addsNewScanEntryAndAwards25Xp() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val initialXp = repo.plantAvatar.value.currentXp
        val initialScanCount = repo.recentScans.value.size

        val newScan = repo.performCameraScan()

        assertEquals(initialScanCount + 1, repo.recentScans.value.size)
        assertEquals(newScan, repo.recentScans.value.first())
        assertEquals("Healthy Leaf", newScan.title)
        assertEquals(initialXp + 25, repo.plantAvatar.value.currentXp)
    }

    @Test
    fun streakOperations_incrementAndReset_updatesStreakDays() = runTest {
        val repo = AgriQuestRepositoryImpl()

        // Initial streak
        assertEquals(0, repo.plantAvatar.value.streakDays)
        assertEquals(0, repo.streakDays.value)

        // Increment streak
        repo.incrementStreak()
        assertEquals(1, repo.plantAvatar.value.streakDays)
        assertEquals(1, repo.streakDays.value)

        // Set streak days directly
        repo.setStreakDays(5)
        assertEquals(5, repo.plantAvatar.value.streakDays)
        assertEquals(5, repo.streakDays.value)

        // Reset streak to 0
        repo.resetStreak()
        assertEquals(0, repo.plantAvatar.value.streakDays)
        assertEquals(0, repo.streakDays.value)
    }

    @Test
    fun checkAndResetMidnightQuests_firstCall_resetsQuestsAndSavesDate() = runTest {
        val repo = AgriQuestRepositoryImpl()

        // First call with explicit date
        val resetOccurred = repo.checkAndResetMidnightQuests("2026-03-30")
        assertTrue(resetOccurred)
        assertEquals(3, repo.quests.value.size)
        assertTrue(repo.quests.value.all { !it.isClaimed })

        // Second call on same date should not reset
        val secondResetOccurred = repo.checkAndResetMidnightQuests("2026-03-30")
        assertFalse(secondResetOccurred)
    }

    @Test
    fun checkAndResetMidnightQuests_consecutiveNextDay_resetsQuestsAndPreservesStreak() = runTest {
        val repo = AgriQuestRepositoryImpl()

        // Set initial reset date
        repo.checkAndResetMidnightQuests("2026-03-30")

        // User completes quests and builds streak
        repo.setStreakDays(3)
        repo.claimQuest(repo.quests.value.first().id)

        // Advance to next consecutive calendar day (1 day difference)
        val resetOccurred = repo.checkAndResetMidnightQuests("2026-03-31")
        assertTrue(resetOccurred)
        assertEquals(3, repo.plantAvatar.value.streakDays)
        assertTrue(repo.quests.value.all { !it.isClaimed })
    }

    @Test
    fun checkAndResetMidnightQuests_missedDay_resetsStreakToZeroAndResetsQuests() = runTest {
        val repo = AgriQuestRepositoryImpl()

        // Set initial reset date
        repo.checkAndResetMidnightQuests("2026-03-30")

        // User has a 5-day streak
        repo.setStreakDays(5)

        // Advance date by 3 days (missed days between March 30 and April 2)
        val resetOccurred = repo.checkAndResetMidnightQuests("2026-04-02")
        assertTrue(resetOccurred)
        assertEquals(0, repo.plantAvatar.value.streakDays)
        assertTrue(repo.quests.value.all { !it.isClaimed })
    }

    @Test
    fun submitQuizAnswer_correctOption_awards30XpAndMarksQuizCompleted() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val question = repo.currentQuizQuestion.value
        assertEquals("quiz_soil_moisture", question.id)
        assertEquals(1, question.correctOptionIndex)

        // Submit correct answer (option index 1 = "60-80%")
        val isCorrect = repo.submitQuizAnswer(1)

        assertTrue(isCorrect)
        assertEquals(30, repo.plantAvatar.value.currentXp)
        assertTrue(repo.isQuizCompletedToday.value)
        assertEquals(1, repo.selectedQuizOptionIndex.value)
        assertEquals(true, repo.isQuizAnswerCorrect.value)
    }

    @Test
    fun submitQuizAnswer_incorrectOption_returnsFalseAndMarksQuizCompletedWithoutXp() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val question = repo.currentQuizQuestion.value
        assertEquals("quiz_soil_moisture", question.id)

        // Submit incorrect answer (option index 0 = "10-30%")
        val isCorrect = repo.submitQuizAnswer(0)

        assertFalse(isCorrect)
        assertEquals(0, repo.plantAvatar.value.currentXp)
        assertTrue(repo.isQuizCompletedToday.value)
        assertEquals(0, repo.selectedQuizOptionIndex.value)
        assertEquals(false, repo.isQuizAnswerCorrect.value)
    }

    @Test
    fun resetQuizState_forcesCleanUnansweredQuizState() = runTest {
        val repo = AgriQuestRepositoryImpl()
        repo.submitQuizAnswer(1)

        assertEquals(1, repo.selectedQuizOptionIndex.value)
        assertTrue(repo.isQuizCompletedToday.value)

        repo.resetQuizState()

        assertEquals(-1, repo.selectedQuizOptionIndex.value)
        assertFalse(repo.isQuizCompletedToday.value)
        assertNull(repo.isQuizAnswerCorrect.value)
    }

    @Test
    fun nextQuizQuestion_cyclesThroughQuizPoolAndResetsAnswerState() = runTest {
        val repo = AgriQuestRepositoryImpl()
        assertEquals("quiz_soil_moisture", repo.currentQuizQuestion.value.id)
        repo.submitQuizAnswer(1)

        assertEquals(1, repo.selectedQuizOptionIndex.value)
        assertTrue(repo.isQuizCompletedToday.value)

        // Cycle to next question
        repo.nextQuizQuestion()

        val nextQuestion = repo.currentQuizQuestion.value
        assertEquals("quiz_light_spectrum", nextQuestion.id)
        assertEquals(-1, repo.selectedQuizOptionIndex.value)
        assertFalse(repo.isQuizCompletedToday.value)
        assertNull(repo.isQuizAnswerCorrect.value)
    }
}
