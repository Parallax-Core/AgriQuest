package com.agriquest.ui.dashboard

import com.agriquest.domain.model.ConnectionStatus
import com.agriquest.domain.model.FanMode
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.domain.model.PlantSource
import com.agriquest.ui.components.AgriQuestTab
import com.agriquest.ui.state.AgriQuestUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardUnitTest {

    @Test
    fun defaultUiState_matchesTask1Level0AndTelemetryObjectives() {
        val uiState = AgriQuestUiState()

        val telemetry = uiState.telemetry
        assertEquals(68.0f, telemetry.soilMoisture, 0.01f)
        assertEquals(24.5f, telemetry.airTemperature, 0.01f)
        assertEquals(62.0f, telemetry.relativeHumidity, 0.01f)
        assertEquals(14200.0f, telemetry.lightIntensity, 0.01f)

        assertTrue(telemetry.isSoilMoistureOptimal)
        assertTrue(telemetry.isAirTemperatureOptimal)
        assertTrue(telemetry.isHumidityOptimal)
        assertTrue(telemetry.isLightIntensityOptimal)

        val plant = uiState.plant
        assertEquals("Lvl. 0 Urban Beginner", plant.level)
        assertEquals(0, plant.currentXp)
        assertEquals(50, plant.maxXp)
        assertEquals(PlantGrowthStage.GERMINATION, plant.growthStage)
        assertEquals("Vibrant & Healthy", plant.statusTag)
        assertEquals(0, plant.streakDays)
        assertEquals(7, plant.targetStreakDays)

        assertEquals(PlantSource.EMULATED_OFFLINE, uiState.plantSource)

        val connection = uiState.connection
        assertEquals("ESP32", connection.deviceName)
        assertEquals(ConnectionStatus.CONNECTED, connection.status)
        assertEquals("Connected", connection.statusText)
    }

    @Test
    fun defaultUiState_matchesTask1QuestsAndControlsObjectives() {
        val uiState = AgriQuestUiState()

        // 1. Active Quest Banner Objectives
        val quest = uiState.activeQuest
        assertEquals("⚡ Thirsty Roots: Keep soil moisture above 60%", quest.title)
        assertEquals(1.0f, quest.progress, 0.01f)
        assertEquals(100, quest.progressPercent)
        assertTrue(quest.isCompleted)
        assertFalse(quest.isClaimed)
        assertTrue(quest.isClaimable)
        assertEquals("+50 XP", quest.xpRewardText)

        // 2. Default Quests List
        assertEquals(3, uiState.quests.size)

        // 3. Actuator Controls Objectives
        val actuator = uiState.actuator
        assertFalse(actuator.isPumpActive)
        assertEquals(0, actuator.pumpRemainingSeconds)
        assertEquals(FanMode.AUTO, actuator.fanMode)

        // 4. Bottom Nav AI Scan Lock Objectives (streak < 7 -> locked)
        val isAiScanLocked = uiState.plant.streakDays < uiState.plant.targetStreakDays
        assertTrue(isAiScanLocked)
    }

    @Test
    fun agriQuestTab_5TabEnumStructure_matchesDesignObjectives() {
        val tabs = AgriQuestTab.entries
        assertEquals(5, tabs.size)

        assertEquals(AgriQuestTab.HOME, tabs[0])
        assertEquals("Home", tabs[0].title)

        assertEquals(AgriQuestTab.PLANT, tabs[1])
        assertEquals("Plant", tabs[1].title)

        assertEquals(AgriQuestTab.QUESTS, tabs[2])
        assertEquals("Quests", tabs[2].title)

        assertEquals(AgriQuestTab.AI_SCAN, tabs[3])
        assertEquals("AI", tabs[3].title)

        assertEquals(AgriQuestTab.AWARDS, tabs[4])
        assertEquals("Awards", tabs[4].title)
    }

    @Test
    fun agriQuestTab_aiScanLock_isLockedWhenStreakUnder7() {
        val uiState = AgriQuestUiState()
        // Default streak = 0, target streak = 7 -> AI_SCAN tab (index 3) is locked
        val isAiLocked = uiState.plant.streakDays < uiState.plant.targetStreakDays
        assertTrue(isAiLocked)
    }
}
