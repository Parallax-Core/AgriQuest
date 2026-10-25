package com.agriquest.domain.model

import com.agriquest.ui.state.AgriQuestUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelsTest {

    @Test
    fun plantMoodle_enum_containsAll7Moodles() {
        val moodles = PlantMoodle.entries
        assertEquals(7, moodles.size)

        assertEquals("Wilting", PlantMoodle.WILTING.displayName)
        assertEquals("Thriving", PlantMoodle.THRIVING.displayName)
        assertEquals("Thirsty", PlantMoodle.THIRSTY.displayName)
        assertEquals("Ready to Harvest", PlantMoodle.READY_TO_HARVEST.displayName)
        assertEquals("Heat-Stressed", PlantMoodle.HEAT_STRESSED.displayName)
        assertEquals("Too Cold", PlantMoodle.TOO_COLD.displayName)
        assertEquals("Over-Watered", PlantMoodle.OVER_WATERED.displayName)
    }

    @Test
    fun activeMoodles_computedDynamically_basedOnTelemetry() {
        // Optimal conditions -> THRIVING
        val state1 = AgriQuestUiState(telemetry = TelemetryData(soilMoisture = 68.0f, airTemperature = 24.5f))
        assertTrue(state1.activeMoodles.contains(PlantMoodle.THRIVING))

        // Low moisture < 30% -> THIRSTY
        val state2 = AgriQuestUiState(telemetry = TelemetryData(soilMoisture = 25.0f, airTemperature = 24.5f))
        assertTrue(state2.activeMoodles.contains(PlantMoodle.THIRSTY))

        // Very low moisture < 15% -> WILTING
        val state3 = AgriQuestUiState(telemetry = TelemetryData(soilMoisture = 10.0f, airTemperature = 24.5f))
        assertTrue(state3.activeMoodles.contains(PlantMoodle.WILTING))

        // High temperature > 30°C -> HEAT_STRESSED
        val state4 = AgriQuestUiState(telemetry = TelemetryData(soilMoisture = 68.0f, airTemperature = 32.0f))
        assertTrue(state4.activeMoodles.contains(PlantMoodle.HEAT_STRESSED))

        // Low temperature < 15°C -> TOO_COLD
        val state5 = AgriQuestUiState(telemetry = TelemetryData(soilMoisture = 68.0f, airTemperature = 10.0f))
        assertTrue(state5.activeMoodles.contains(PlantMoodle.TOO_COLD))

        // High moisture > 85% -> OVER_WATERED
        val state6 = AgriQuestUiState(telemetry = TelemetryData(soilMoisture = 90.0f, airTemperature = 24.5f))
        assertTrue(state6.activeMoodles.contains(PlantMoodle.OVER_WATERED))

        // Fruiting stage / level >= 3 -> READY_TO_HARVEST
        val state7 = AgriQuestUiState(
            telemetry = TelemetryData(soilMoisture = 68.0f, airTemperature = 24.5f),
            plant = PlantAvatar(growthStage = PlantGrowthStage.RIPENING, levelInt = 5)
        )
        assertTrue(state7.activeMoodles.contains(PlantMoodle.READY_TO_HARVEST))
    }

    @Test
    fun plantSource_enum_containsExpectedOptions() {
        val emulated = PlantSource.EMULATED_OFFLINE
        val esp32 = PlantSource.ESP32_HARDWARE

        assertEquals("Offline Plant (Emulated)", emulated.displayName)
        assertEquals("ESP32 Hardware (Live IoT)", esp32.displayName)
    }

    @Test
    fun plantGrowthStage_enumAndFromLevel_mapsCorrectly() {
        val stages = listOf(
            PlantGrowthStage.GERMINATION, PlantGrowthStage.SEEDLING_SPROUT,
            PlantGrowthStage.VEGETATIVE_LEAFY, PlantGrowthStage.FLOWERING,
            PlantGrowthStage.FRUITING_HARVEST, PlantGrowthStage.RIPENING
        )
        assertEquals(stages, PlantGrowthStage.entries.toList())
        stages.forEachIndexed { level, stage ->
            assertEquals(stage, PlantGrowthStage.fromLevel(level))
            assertEquals(stage, PlantGrowthStage.fromName(stage.name))
        }
        assertEquals(PlantGrowthStage.GERMINATION, PlantGrowthStage.fromLevel(-1))
        assertEquals(PlantGrowthStage.RIPENING, PlantGrowthStage.fromLevel(99))
        assertEquals(PlantGrowthStage.GERMINATION, PlantGrowthStage.fromName(null))
        val boundaries = listOf(0, 1000, 2800, 5600, 7000, 10500)
        boundaries.forEachIndexed { index, xp ->
            assertEquals(stages[index], PlantGrowthStage.fromXp(xp))
            if (index > 0) assertEquals(stages[index - 1], PlantGrowthStage.fromXp(xp - 1))
        }
        assertEquals(PlantGrowthStage.RIPENING, PlantGrowthStage.fromXp(12000))
    }

    @Test
    fun plantAvatar_defaultValues_matchesLevel0Sprout() {
        val plant = PlantAvatar()
        assertEquals("Flora", plant.name)
        assertEquals("Lvl. 0 Urban Beginner", plant.level)
        assertEquals(0, plant.levelInt)
        assertEquals(0, plant.currentXp)
        assertEquals(1000, plant.maxXp)
        assertEquals(PlantGrowthStage.GERMINATION, plant.growthStage)
        assertEquals("0/1000 XP", plant.xpText)
        assertEquals("Vibrant & Healthy", plant.statusTag)
        assertEquals("0/7", plant.streakText)
        assertEquals(0.0f, plant.xpProgress, 0.01f)
    }

    @Test
    fun quest_defaultValuesAndClaimable_formattedCorrectly() {
        val quest = Quest()
        assertEquals("⚡ Thirsty Roots: Keep soil moisture above 60%", quest.title)
        assertTrue(quest.isCompleted)
        assertFalse(quest.isClaimed)
        assertTrue(quest.isClaimable)
        assertEquals("+50 XP", quest.xpRewardText)
        assertEquals(100, quest.progressPercent)

        val defaultQuests = Quest.DEFAULT_QUESTS
        assertEquals(3, defaultQuests.size)
        assertEquals("quest_review_moisture", defaultQuests[0].id)
        assertEquals("quest_inspect_plant", defaultQuests[1].id)
        assertTrue(Quest.ROTATING_QUESTS.any { it.id == defaultQuests[2].id })

        val dailyQuests = Quest.DAILY_QUESTS
        assertEquals(3, dailyQuests.size)
        assertEquals("quest_review_moisture", dailyQuests[0].id)
        assertEquals("quest_inspect_plant", dailyQuests[1].id)
        assertTrue(Quest.ROTATING_QUESTS.any { it.id == dailyQuests[2].id })

        assertTrue(Quest.QUEST_POOL.containsAll(Quest.DAILY_QUESTS))
    }

    @Test
    fun telemetryData_defaultValues_areOptimal() {
        val telemetry = TelemetryData()
        assertEquals(68.0f, telemetry.soilMoisture, 0.01f)
        assertEquals(24.5f, telemetry.airTemperature, 0.01f)
        assertEquals(62.0f, telemetry.relativeHumidity, 0.01f)
        assertEquals(14200.0f, telemetry.lightIntensity, 0.01f)

        assertTrue(telemetry.isSoilMoistureOptimal)
        assertTrue(telemetry.isAirTemperatureOptimal)
        assertTrue(telemetry.isHumidityOptimal)
        assertTrue(telemetry.isLightIntensityOptimal)
        assertTrue(telemetry.isOverallOptimal)
    }

    @Test
    fun telemetryData_lowMoisture_isNotOptimal() {
        val telemetry = TelemetryData(soilMoisture = 35.0f)
        assertFalse(telemetry.isSoilMoistureOptimal)
        assertFalse(telemetry.isOverallOptimal)
        assertTrue(telemetry.statusSummary.contains("Soil moisture low"))
    }

    @Test
    fun fanMode_cyclesInCorrectOrder() {
        val mode1 = FanMode.AUTO
        val mode2 = mode1.next()
        val mode3 = mode2.next()
        val mode4 = mode3.next()

        assertEquals(FanMode.ON, mode2)
        assertEquals(FanMode.OFF, mode3)
        assertEquals(FanMode.AUTO, mode4)
    }

    @Test
    fun connectionState_connected_returnsGreenIndicator() {
        val connection = ConnectionState()
        assertEquals("ESP32", connection.deviceName)
        assertEquals(ConnectionStatus.CONNECTED, connection.status)
        assertTrue(connection.isIndicatorGreen)
        assertEquals(0xFF4CAF50, connection.indicatorColorArgb)
    }

    @Test
    fun aiScanResult_defaultScans_containsHealthyLeafAndEarlyBlight() {
        val defaultScans = AiScanResult.DEFAULT_SCANS
        assertEquals(2, defaultScans.size)

        val scan1 = defaultScans[0]
        assertEquals("Healthy Leaf", scan1.title)
        assertEquals("Leaf 4 (middle branch) • Sep 12 - 2:14 PM", scan1.subtext)
        assertEquals("97%", scan1.confidence)
        assertTrue(scan1.isHealthy)

        val scan2 = defaultScans[1]
        assertEquals("Early Blight", scan2.title)
        assertEquals("Leaf 7 (lower) • Sep 10 - 11:02 AM", scan2.subtext)
        assertEquals("89%", scan2.confidence)
        assertFalse(scan2.isHealthy)
    }

    @Test
    fun achievement_defaultAchievements_matchesDesignSpecifications() {
        val achievements = Achievement.DEFAULT_ACHIEVEMENTS
        assertEquals(6, achievements.size)

        val unlocked = achievements.filter { it.isUnlocked }
        val locked = achievements.filter { !it.isUnlocked }

        assertEquals(3, unlocked.size)
        assertEquals(3, locked.size)

        // First Plant
        val firstPlant = achievements[0]
        assertEquals("First Plant", firstPlant.title)
        assertEquals("Started your first tomato growing journey.", firstPlant.description)
        assertEquals(100, firstPlant.xpReward)
        assertEquals("+100 XP", firstPlant.xpRewardText)
        assertTrue(firstPlant.isUnlocked)
        assertEquals("Jul 29", firstPlant.earnedDate)
        assertEquals("Earned Jul 29", firstPlant.earnedDateText)
        assertEquals(AchievementIcon.PLANT, firstPlant.iconType)

        // Water Hero
        val waterHero = achievements[1]
        assertEquals("Water Hero", waterHero.title)
        assertEquals("Watered your plant 10 times without missing a day.", waterHero.description)
        assertEquals(150, waterHero.xpReward)
        assertEquals("+150 XP", waterHero.xpRewardText)
        assertTrue(waterHero.isUnlocked)
        assertEquals("Aug 12", waterHero.earnedDate)
        assertEquals("Earned Aug 12", waterHero.earnedDateText)
        assertEquals(AchievementIcon.WATER, waterHero.iconType)

        // Sun Seeker
        val sunSeeker = achievements[2]
        assertEquals("Sun Seeker", sunSeeker.title)
        assertEquals("Maintained optimal light for 5 consecutive days.", sunSeeker.description)
        assertEquals(125, sunSeeker.xpReward)
        assertEquals("+125 XP", sunSeeker.xpRewardText)
        assertTrue(sunSeeker.isUnlocked)
        assertEquals("Aug 20", sunSeeker.earnedDate)
        assertEquals("Earned Aug 20", sunSeeker.earnedDateText)
        assertEquals(AchievementIcon.SUN, sunSeeker.iconType)

        // Verify total unlocked XP
        val totalUnlockedXp = unlocked.sumOf { it.xpReward }
        assertEquals(375, totalUnlockedXp)
    }
}
