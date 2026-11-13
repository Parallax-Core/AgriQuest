package com.agriquest.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.agriquest.domain.model.JournalCategory
import com.agriquest.domain.model.JournalEntry
import com.agriquest.domain.model.PlantGrowthStage
import com.agriquest.domain.model.PlantSource
import com.agriquest.domain.model.TelemetryData
import com.agriquest.domain.model.AccountPlantProgress
import com.agriquest.domain.model.AiScanResult
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Local DataStore / Preferences Persistence for AgriQuest Mobile.
 * Persists user level, current XP, growth stage, claimed quest IDs, care streak, selected plant source,
 * journal notes, and telemetry state so exiting and reopening retains all progress!
 */
class AgriQuestDataStore(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context, accountId: String) : this(AccountStorage.get(context, accountId))

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
        private val KEY_PLANT_ID = stringPreferencesKey("account_plant_id")
        private val KEY_PLANT_NAME = stringPreferencesKey("plant_name")
        private val KEY_REVISION = longPreferencesKey("cloud_revision")
        private val KEY_DIRTY = booleanPreferencesKey("cloud_dirty")
        private val KEY_SCANS = stringPreferencesKey("recent_scans")
    }

    val cloudDirtyFlow = dataStore.data.map { it[KEY_DIRTY] ?: false }
    val plantNameFlow = dataStore.data.map { it[KEY_PLANT_NAME] ?: "Flora" }
    val recentScansFlow = dataStore.data.map { preferences ->
        runCatching {
            val entries = JSONArray(preferences[KEY_SCANS] ?: "[]")
            (0 until entries.length()).map { index -> entries.getJSONObject(index).let {
                AiScanResult(it.getString("id"), it.getString("title"), it.getString("subtext"),
                    it.getString("confidence"), it.optBoolean("healthy", true),
                    it.optString("path").takeIf(String::isNotBlank), it.optBoolean("low"),
                    it.optString("pathogen").takeIf(String::isNotBlank),
                    it.optString("heatmap").takeIf(String::isNotBlank))
            } }
        }.getOrDefault(emptyList())
    }

    suspend fun saveScans(scans: List<AiScanResult>) {
        val json = JSONArray()
        scans.forEach { json.put(JSONObject().put("id", it.id).put("title", it.title)
            .put("subtext", it.subtext).put("confidence", it.confidence).put("healthy", it.isHealthy)
            .put("path", it.imagePath.orEmpty()).put("low", it.lowConfidence).put("pathogen", it.pathogen.orEmpty())
            .put("heatmap", it.heatmapPath.orEmpty())) }
        dataStore.edit { it[KEY_SCANS] = json.toString() }
    }

    /** Mark a complete repository operation for background sync, never transient sensor frames. */
    suspend fun markForSync() { dataStore.edit {
        it.progressChanged()
    } }

    private fun MutablePreferences.progressChanged() {
        this[KEY_REVISION] = (this[KEY_REVISION] ?: 0L) + 1
        this[KEY_DIRTY] = true
    }

    private fun encodeJournal(entry: JournalEntry) = JSONObject().put("id", entry.id).put("date", entry.date)
        .put("note", entry.note).put("category", entry.category.name).toString()

    suspend fun pendingProgress(): Pair<Long, AccountPlantProgress> {
        val p = dataStore.data.first()
        val date = requireNotNull(p[KEY_TRACKING_START])
        return (p[KEY_REVISION] ?: 0L) to AccountPlantProgress(
            plantId = requireNotNull(p[KEY_PLANT_ID]), plantName = p[KEY_PLANT_NAME] ?: "Flora",
            sowingDate = date, totalXp = p[KEY_CURRENT_XP] ?: 0, level = p[KEY_USER_LEVEL] ?: 0,
            stage = PlantGrowthStage.fromName(p[KEY_GROWTH_STAGE]), streakDays = p[KEY_STREAK_DAYS] ?: 0,
            lastResetDate = p[KEY_LAST_RESET_DATE], quizCompletedDate = p[KEY_QUIZ_COMPLETED_DATE],
            xpDate = p[KEY_XP_DATE] ?: date, dailyXp = p[KEY_DAILY_XP] ?: 0,
            claimedQuestIds = p[KEY_CLAIMED_QUEST_IDS] ?: emptySet(), journal = journalNotesFlow.first()
        )
    }

    suspend fun acknowledgeSync(revision: Long) { dataStore.edit {
        if ((it[KEY_REVISION] ?: 0L) == revision) it[KEY_DIRTY] = false
    } }

    /** Bootstrap only the authenticated account. Never import the former shared demo file. */
    suspend fun restoreCloud(progress: AccountPlantProgress) {
        dataStore.edit { p ->
            if (p[KEY_DIRTY] == true && p[KEY_PLANT_ID] == progress.plantId) {
                // Retain unsent edits without showing a lower XP total than the cloud account.
                p[KEY_CURRENT_XP] = maxOf(p[KEY_CURRENT_XP] ?: 0, progress.totalXp)
                p[KEY_USER_LEVEL] = maxOf(p[KEY_USER_LEVEL] ?: 0, progress.level)
                if (p[KEY_XP_DATE] == progress.xpDate) {
                    p[KEY_DAILY_XP] = maxOf(p[KEY_DAILY_XP] ?: 0, progress.dailyXp)
                    p[KEY_CLAIMED_QUEST_IDS] = (p[KEY_CLAIMED_QUEST_IDS] ?: emptySet()) + progress.claimedQuestIds
                }
                return@edit
            }
            p[KEY_PLANT_ID] = progress.plantId
            p[KEY_PLANT_NAME] = progress.plantName
            p[KEY_TRACKING_START] = progress.sowingDate
            p[KEY_CURRENT_XP] = progress.totalXp
            p[KEY_USER_LEVEL] = progress.level
            p[KEY_GROWTH_STAGE] = progress.stage.name
            p[KEY_STREAK_DAYS] = progress.streakDays
            p[KEY_XP_DATE] = progress.xpDate
            p[KEY_DAILY_XP] = progress.dailyXp
            p[KEY_CLAIMED_QUEST_IDS] = progress.claimedQuestIds
            progress.lastResetDate?.let { p[KEY_LAST_RESET_DATE] = it } ?: p.remove(KEY_LAST_RESET_DATE)
            progress.quizCompletedDate?.let { p[KEY_QUIZ_COMPLETED_DATE] = it } ?: p.remove(KEY_QUIZ_COMPLETED_DATE)
            p[KEY_JOURNAL_NOTES] = progress.journal.map(::encodeJournal).toSet()
        }
    }

    val trackingStartFlow = dataStore.data.map { it[KEY_TRACKING_START] }
    val xpDateFlow = dataStore.data.map { it[KEY_XP_DATE] }
    val dailyXpFlow = dataStore.data.map { it[KEY_DAILY_XP] ?: 0 }
    suspend fun saveTrackingStart(date: String) {
        dataStore.edit { it[KEY_TRACKING_START] = date; it.progressChanged() }
    }
    suspend fun saveXpLedger(date: String, dailyXp: Int, totalXp: Int) {
        dataStore.edit {
            it[KEY_XP_DATE] = date
            it[KEY_DAILY_XP] = dailyXp
            it[KEY_CURRENT_XP] = totalXp
            it.progressChanged()
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
            emptyList()
        } else {
            rawSet.mapNotNull { raw ->
                if (raw.startsWith("{")) return@mapNotNull runCatching {
                    val item = JSONObject(raw)
                    JournalEntry(item.getString("id"), item.getString("date"), item.getString("note"),
                        JournalCategory.valueOf(item.getString("category")))
                }.getOrNull()
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
            preferences.progressChanged()
        }
    }

    suspend fun saveClaimedQuestId(questId: String) {
        dataStore.edit { preferences ->
            val currentSet = preferences[KEY_CLAIMED_QUEST_IDS] ?: emptySet()
            preferences[KEY_CLAIMED_QUEST_IDS] = currentSet + questId
            preferences.progressChanged()
        }
    }

    suspend fun saveStreakDays(streakDays: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_STREAK_DAYS] = streakDays
            preferences.progressChanged()
        }
    }

    suspend fun incrementStreak() {
        dataStore.edit { preferences ->
            val current = preferences[KEY_STREAK_DAYS] ?: 0
            preferences[KEY_STREAK_DAYS] = current + 1
            preferences.progressChanged()
        }
    }

    suspend fun resetStreak() {
        dataStore.edit { preferences ->
            preferences[KEY_STREAK_DAYS] = 0
            preferences.progressChanged()
        }
    }

    suspend fun savePlantSource(plantSource: PlantSource) {
        dataStore.edit { preferences ->
            preferences[KEY_PLANT_SOURCE] = plantSource.name
        }
    }

    suspend fun saveJournalEntries(entries: List<JournalEntry>) {
        dataStore.edit { preferences ->
            val rawSet = entries.map(::encodeJournal).toSet()
            preferences[KEY_JOURNAL_NOTES] = rawSet
            preferences.progressChanged()
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
            preferences.progressChanged()
        }
    }

    suspend fun clearClaimedQuestIds() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_CLAIMED_QUEST_IDS)
            preferences.progressChanged()
        }
    }

    suspend fun saveQuizCompletedDate(dateString: String) {
        dataStore.edit { preferences ->
            preferences[KEY_QUIZ_COMPLETED_DATE] = dateString
            preferences.progressChanged()
        }
    }

    suspend fun clearQuizCompletedDate() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_QUIZ_COMPLETED_DATE)
            preferences.progressChanged()
        }
    }
}
