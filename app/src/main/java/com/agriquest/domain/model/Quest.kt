package com.agriquest.domain.model

/** Which sensor a quest needs. The current ESP32 only measures soil moisture; the others may be missing (NaN). */
enum class SensorKind { MOISTURE, TEMPERATURE, HUMIDITY, LIGHT;

    companion object {
        /** Sensors that report a real value in [telemetry]. */
        fun availableIn(telemetry: TelemetryData): Set<SensorKind> = buildSet {
            if (telemetry.soilMoisture.isFinite()) add(MOISTURE)
            if (telemetry.airTemperature.isFinite()) add(TEMPERATURE)
            if (telemetry.relativeHumidity.isFinite()) add(HUMIDITY)
            if (telemetry.lightIntensity.isFinite()) add(LIGHT)
        }
    }
}

/**
 * How a quest is completed. Every goal is checked from sensor readings or a confirmed pump run,
 * never by the player tapping "done".
 *
 * @param sensor the sensor the goal needs (null = any reading counts)
 * @param minStageLevel the earliest growth stage the goal makes sense for
 * @param isTimed true when [Quest.target] is minutes of readings in the zone
 */
enum class QuestGoal(val sensor: SensorKind?, val minStageLevel: Int = 0, val isTimed: Boolean = true) {
    MOISTURE_ZONE(SensorKind.MOISTURE),
    STAY_ONLINE(null),
    WATER_WHEN_DRY(SensorKind.MOISTURE, isTimed = false),
    RECOVER_FROM_DRY(SensorKind.MOISTURE, isTimed = false),
    TEMP_ZONE(SensorKind.TEMPERATURE),
    HUMIDITY_ZONE(SensorKind.HUMIDITY),
    LIGHT_TIME(SensorKind.LIGHT, minStageLevel = 1),
}

/**
 * A daily plant-care quest. It becomes claimable only when the readings show the goal was met.
 *
 * @param progress 0..1, from [QuestEvaluator]
 * @param target minutes needed for timed goals (e.g. 20 minutes in the moisture zone)
 * @param progressText live status, e.g. "Moisture 64% · 12/20 min"
 */
data class Quest(
    val id: String = "quest_moisture_zone",
    val title: String = "Keep the Soil in the Comfort Zone",
    val difficulty: String = "Easy",
    val progress: Float = 0f,
    val xpReward: Int = 30,
    val subtext: String = "",
    val progressText: String = "Waiting for sensor readings",
    val subtextBadge: String = "",
    val isClaimed: Boolean = false,
    val isClaimable: Boolean = false,
    val goal: QuestGoal = QuestGoal.MOISTURE_ZONE,
    val target: Float = 20f
) {
    val isCompleted: Boolean
        get() = progress >= 1.0f || isClaimed

    val xpRewardText: String
        get() = "+$xpReward XP"

    val progressPercent: Int
        get() = (progress * 100).toInt().coerceIn(0, 100)

    companion object {
        /** Quests per day (the daily quiz is extra: one per day). */
        const val MIN_DAILY_QUESTS = 3
        const val MAX_DAILY_QUESTS = 5

        private fun quest(goal: QuestGoal, id: String, title: String, xp: Int, difficulty: String, target: Float, description: String) =
            Quest(id = id, title = title, xpReward = xp, difficulty = difficulty, subtext = description, goal = goal, target = target)

        val ALL_QUESTS = listOf(
            quest(QuestGoal.MOISTURE_ZONE, "quest_moisture_zone", "Keep the Soil in the Comfort Zone", 30, "Easy", 20f,
                "Keep soil moisture between 60% and 80% for 20 minutes. The sensor checks it for you."),
            quest(QuestGoal.STAY_ONLINE, "quest_stay_online", "Keep Your Garden Online", 20, "Easy", 15f,
                "Keep the sensor sending readings for 15 minutes today."),
            quest(QuestGoal.WATER_WHEN_DRY, "quest_water_when_dry", "Water Only When Thirsty", 30, "Medium", 1f,
                "When the soil drops below 45%, water it with the Water button. Completes when the pump run is confirmed."),
            quest(QuestGoal.RECOVER_FROM_DRY, "quest_recover_from_dry", "Bring the Roots Back", 30, "Medium", 1f,
                "After the soil gets dry (below 45%), bring it back above 60%."),
            quest(QuestGoal.TEMP_ZONE, "quest_temp_zone", "Cozy Temperature", 25, "Easy", 30f,
                "Keep the air between 21 and 28 °C for 30 minutes."),
            quest(QuestGoal.HUMIDITY_ZONE, "quest_humidity_zone", "Balanced Humidity", 25, "Easy", 30f,
                "Keep humidity between 50% and 70% for 30 minutes."),
            quest(QuestGoal.LIGHT_TIME, "quest_light_time", "Sunshine Time", 30, "Medium", 60f,
                "Give your plant 60 minutes of bright light (10,000 lux or more)."),
        )

        private val CORE_GOALS = setOf(QuestGoal.MOISTURE_ZONE, QuestGoal.STAY_ONLINE)

        /**
         * Today's quests: always the two core quests, then a date-based rotation of the others that fit the
         * growth stage and the sensors present. 3 to 5 quests; the same date always gives the same list.
         */
        fun forDate(
            date: String,
            stage: PlantGrowthStage = PlantGrowthStage.GERMINATION,
            sensors: Set<SensorKind> = SensorKind.entries.toSet()
        ): List<Quest> {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                isLenient = false
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val day = runCatching { requireNotNull(parser.parse(date)).time / 86_400_000L }.getOrDefault(0L)
            val eligible = ALL_QUESTS.filter { quest ->
                quest.goal.minStageLevel <= stage.stageLevel && (quest.goal.sensor == null || quest.goal.sensor in sensors)
            }
            val core = eligible.filter { it.goal in CORE_GOALS }
            val others = eligible.filter { it.goal !in CORE_GOALS }
            val rotated = if (others.isEmpty()) others else {
                val shift = Math.floorMod(day, others.size.toLong()).toInt()
                others.drop(shift) + others.take(shift)
            }
            val wanted = MIN_DAILY_QUESTS + Math.floorMod(day, (MAX_DAILY_QUESTS - MIN_DAILY_QUESTS + 1).toLong()).toInt()
            return (core + rotated).take(wanted.coerceAtLeast(MIN_DAILY_QUESTS))
        }

        val DAILY_QUESTS: List<Quest>
            get() = forDate(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()))
        val DEFAULT_QUESTS: List<Quest> get() = DAILY_QUESTS
    }
}
