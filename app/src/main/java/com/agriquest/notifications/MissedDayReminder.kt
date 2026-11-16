package com.agriquest.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * "Your tomato misses you!" when the game hasn't been opened for a day.
 * A background check runs every few hours; it reminds at most once per day of absence.
 */
object MissedDayReminder {
    private const val PREFS = "agriquest_activity"
    private const val KEY_LAST_PLAYED = "last_played"
    private const val KEY_LAST_REMINDED = "last_reminded"
    private const val WORK_NAME = "missed_day_reminder"
    const val DAY_MS = 24L * 60 * 60 * 1000

    /** Call whenever the game is on screen. */
    fun markPlayed(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_PLAYED, System.currentTimeMillis()).apply()
    }

    /** Schedules the background check once (kept across app restarts). */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<MissedDayWorker>(4, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** True when the player has been away a day and hasn't been reminded in the last day. */
    fun shouldRemind(lastPlayed: Long, lastReminded: Long, now: Long): Boolean =
        lastPlayed > 0L && now - lastPlayed >= DAY_MS && now - lastReminded >= DAY_MS

    internal fun check(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastPlayed = prefs.getLong(KEY_LAST_PLAYED, 0L)
        if (!shouldRemind(lastPlayed, prefs.getLong(KEY_LAST_REMINDED, 0L), now)) return
        val days = ((now - lastPlayed) / DAY_MS).toInt().coerceAtLeast(1)
        if (PlantAlertNotifier.showMissedDay(context, days)) {
            prefs.edit().putLong(KEY_LAST_REMINDED, now).apply()
        }
    }
}

class MissedDayWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        MissedDayReminder.check(applicationContext)
        return Result.success()
    }
}
