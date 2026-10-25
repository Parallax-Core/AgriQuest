package com.agriquest.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class DailyMissionRotationTest {
    @Test
    fun everyDayChangesOneMissionAcrossFullYearAndKeepsRewardBudget() {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = formatter.parse("2027-12-01")!!
        }
        var previous = emptySet<String>()
        val seen = mutableSetOf<String>()
        repeat(400) {
            val date = formatter.format(calendar.time)
            val missions = Quest.forDate(date)
            val ids = missions.map { it.id }.toSet()
            assertEquals(3, ids.size)
            assertEquals(90, missions.sumOf { it.xpReward })
            assertEquals(missions, Quest.forDate(date))
            if (previous.isNotEmpty()) {
                assertEquals(2, ids.intersect(previous).size)
                assertEquals(1, ids.subtract(previous).size)
            }
            seen.add(missions.last().id)
            previous = ids
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        assertEquals(Quest.ROTATING_QUESTS.map { it.id }.toSet(), seen)
    }
}
