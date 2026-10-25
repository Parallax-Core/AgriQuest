package com.agriquest.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agriquest.domain.model.JournalCategory
import com.agriquest.domain.model.JournalEntry
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.domain.model.PlantSource
import com.agriquest.domain.model.TelemetryData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "agriquest_preferences")

/**
 * Local DataStore / Preferences Persistence for AgriQuest Mobile.
 * Persists user level, current XP, growth stage, claimed quest IDs, care streak, selected plant source,
 * journal notes, and telemetry state so exiting and reopening retains all progress!
 */
class AgriQuestDataStore(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.dataStore)

    companion object {
        val KEY_TRACKING_START = stringPreferencesKey("growth_tracking_start")
        val KEY_XP_DATE = stringPreferencesKey("xp_earned_date")
        val KEY_DAILY_XP = intPreferencesKey("daily_xp_earned")
        val KEY_USER_LEVEL = intPreferencesKey("user_level")
        val KEY_CURRENT_XP = intPreferencesKey("current_xp")
        val KEY_GROWTH_STAGE = stringPreferencesKey("growth_stage")
        val KEY_CLAIMED_QUEST_IDS = stringSetPreferencesKey("claimed_quest_ids")
        val KEY_STREAK_DAYS = intPreferencesKey("streak_days")
        val KEY_PLANT_SOURCE = stringPreferencesKey("plant_source")
        val KEY_JOURNAL_NOTES = stringSetPreferencesKey("journal_notes")
        val KEY_TELEMETRY_MOISTURE = floatPreferencesKey("telemetry_moisture")
        val KEY_TELEMETRY_TEMP = floatPreferencesKey("telemetry_temp")
        val KEY_TELEMETRY_HUMIDITY = floatPreferencesKey("telemetry_humidity")
        val KEY_TELEMETRY_LIGHT = floatPreferencesKey("telemetry_light")
        val KEY_LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
        val KEY_QUIZ_COMPLETED_DATE = stringPreferencesKey("quiz_completed_date")
    }

    val trackingStartFlow = dataStore.data.map { it[KEY_TRACKING_START] }
    val xpDateFlow = dataStore.data.map { it[KEY_XP_DATE] }
    val dailyXpFlow = dataStore.data.map { it[KEY_DAILY_XP] ?: 0 }
    suspend fun saveTrackingStart(date: String) {
        dataStore.edit { it[KEY_TRACKING_START] = date }
    }
    suspend fun saveXpLedger(date: String, dailyXp: Int, totalXp: Int) {
        dataStore.edit {
            it[KEY_XP_DATE] = date
            it[KEY_DAILY_XP] = dailyXp
            it[KEY_CURRENT_XP] = totalXp
        }
    }

    val userLevelFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_USER_LEVEL] ?: 0
    }

    val currentXpFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_CURRENT_XP] ?: 0
    }

    val growthStageFlow: Flow<PlantGrowthStage> = dataStore.data.map { preferences ->
        PlantGrowthStage.fromName(preferences[KEY_GROWTH_STAGE])
    }

    val claimedQuestIdsFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[KEY_CLAIMED_QUEST_IDS] ?: emptySet()
    }

    val lastResetDateFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_RESET_DATE]
    }

    val quizCompletedDateFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_QUIZ_COMPLETED_DATE]
    }

    val streakDaysFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[KEY_STREAK_DAYS] ?: 0
    }

    val plantSourceFlow: Flow<PlantSource> = dataStore.data.map { preferences ->
        PlantSource.fromName(preferences[KEY_PLANT_SOURCE])
    }

    val journalNotesFlow: Flow<List<JournalEntry>> = dataStore.data.map { preferences ->
        val rawSet = preferences[KEY_JOURNAL_NOTES] ?: emptySet()
        if (rawSet.isEmpty()) {
            JournalEntry.DEFAULT_ENTRIES
        } else {
            rawSet.mapNotNull { raw ->
                val parts = raw.split("|||")
                if (parts.size >= 4) {
                    JournalEntry(
                        id = parts[0],
                        date = parts[1],
                        note = parts[2],
                        category = try { JournalCategory.valueOf(parts[3]) } catch (e: Exception) { JournalCategory.WATERING }
                    )
                } else null
            }.sortedByDescending { it.id }
        }
    }

    val telemetryDataFlow: Flow<TelemetryData?> = dataStore.data.map { preferences ->
        val moisture = preferences[KEY_TELEMETRY_MOISTURE]
        val temp = preferences[KEY_TELEMETRY_TEMP]
        val humidity = preferences[KEY_TELEMETRY_HUMIDITY]
        val light = preferences[KEY_TELEMETRY_LIGHT]
        if (moisture != null && temp != null && humidity != null && light != null) {
            TelemetryData(
                soilMoisture = moisture,
                airTemperature = temp,
                relativeHumidity = humidity,
                lightIntensity = light
            )
        } else null
    }

    suspend fun saveUserProgression(
        level: Int,
        currentXp: Int,
        growthStage: PlantGrowthStage
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_USER_LEVEL] = level
            preferences[KEY_CURRENT_XP] = currentXp
            preferences[KEY_GROWTH_STAGE] = growthStage.name
        }
    }

    suspend fun saveClaimedQuestId(questId: String) {
        dataStore.edit { preferences ->
            val currentSet = preferences[KEY_CLAIMED_QUEST_IDS] ?: emptySet()
            preferences[KEY_CLAIMED_QUEST_IDS] = currentSet + questId
        }
    }

    suspend fun saveStreakDays(streakDays: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_STREAK_DAYS] = streakDays
        }
    }

    suspend fun incrementStreak() {
        dataStore.edit { preferences ->
            val current = preferences[KEY_STREAK_DAYS] ?: 0
            preferences[KEY_STREAK_DAYS] = current + 1
        }
    }

    suspend fun resetStreak() {
        dataStore.edit { preferences ->
            preferences[KEY_STREAK_DAYS] = 0
        }
    }

    suspend fun savePlantSource(plantSource: PlantSource) {
        dataStore.edit { preferences ->
            preferences[KEY_PLANT_SOURCE] = plantSource.name
        }
    }

    suspend fun saveJournalEntries(entries: List<JournalEntry>) {
        dataStore.edit { preferences ->
            val rawSet = entries.map { "${it.id}|||${it.date}|||${it.note}|||${it.category.name}" }.toSet()
            preferences[KEY_JOURNAL_NOTES] = rawSet
        }
    }

    suspend fun saveTelemetryData(telemetry: TelemetryData) {
        dataStore.edit { preferences ->
            preferences[KEY_TELEMETRY_MOISTURE] = telemetry.soilMoisture
            preferences[KEY_TELEMETRY_TEMP] = telemetry.airTemperature
            preferences[KEY_TELEMETRY_HUMIDITY] = telemetry.relativeHumidity
            preferences[KEY_TELEMETRY_LIGHT] = telemetry.lightIntensity
        }
    }

    suspend fun saveLastResetDate(dateString: String) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_RESET_DATE] = dateString
        }
    }

    suspend fun clearClaimedQuestIds() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_CLAIMED_QUEST_IDS)
        }
    }

    suspend fun saveQuizCompletedDate(dateString: String) {
        dataStore.edit { preferences ->
            preferences[KEY_QUIZ_COMPLETED_DATE] = dateString
        }
    }

    suspend fun clearQuizCompletedDate() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_QUIZ_COMPLETED_DATE)
        }
    }
}
