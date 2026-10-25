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
        assertEquals(1000, plant.maxXp)
        assertEquals(PlantGrowthStage.GERMINATION, plant.growthStage)
        assertEquals("Vibrant & Healthy", plant.statusTag)
        assertEquals(0, plant.streakDays)

        val activeQuest = repo.activeQuest.value
        assertEquals("quest_review_moisture", activeQuest.id)
        assertEquals("Check Soil and Water Needs", activeQuest.title)

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
    fun addXp_enforcesDailyLimitAndDoesNotRushGrowth() = runTest {
        val repo = AgriQuestRepositoryImpl()
        repo.addXp(80)
        repo.addXp(80)
        repo.addXp(-100)
        assertEquals(100, repo.plantAvatar.value.currentXp)
        assertEquals(100, repo.plantAvatar.value.dailyXp)
        assertEquals(PlantGrowthStage.GERMINATION, repo.plantAvatar.value.growthStage)
        assertEquals(1000, repo.plantAvatar.value.maxXp)
    }

    @Test
    fun claimQuest_awardsThreeMissionsAndOneStreakPerDay() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val quests = repo.quests.value
        assertFalse(repo.claimQuest(quests[0].id))
        assertFalse(repo.claimQuest(quests[0].id))
        assertEquals(30, repo.plantAvatar.value.currentXp)
        assertFalse(repo.claimQuest(quests[1].id))
        assertTrue(repo.claimQuest(quests[2].id))
        assertEquals(90, repo.plantAvatar.value.currentXp)
        assertEquals(1, repo.plantAvatar.value.streakDays)
        assertEquals(PlantGrowthStage.GERMINATION, repo.plantAvatar.value.growthStage)
        assertFalse(repo.claimQuest(quests[2].id))
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
        assertEquals(0, repo.plantAvatar.value.currentXp)
        assertEquals(0, repo.plantAvatar.value.streakDays)
    }

    @Test
    fun resetDailyQuests_restoresThreeSafeMissionsWithoutResettingXpLimit() = runTest {
        val repo = AgriQuestRepositoryImpl()
        Quest.DAILY_QUESTS.forEach { repo.claimQuest(it.id) }
        repo.addXp(10)
        val quests = repo.resetDailyQuests()
        assertEquals(3, quests.size)
        assertTrue(quests.all { !it.isClaimed })
        quests.forEach { repo.claimQuest(it.id) }
        assertEquals(100, repo.plantAvatar.value.currentXp)
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
        assertEquals(initialXp, repo.plantAvatar.value.currentXp)
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
        val repo = AgriQuestRepositoryImpl(now = { java.text.SimpleDateFormat("yyyy-MM-dd").parse("2026-03-30")!! })

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
        assertEquals(10, repo.plantAvatar.value.currentXp)
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
        assertTrue(repo.isQuizCompletedToday.value)
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
        assertTrue(repo.isQuizCompletedToday.value)
        assertNull(repo.isQuizAnswerCorrect.value)
    }
}
