package com.agriquest.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.agriquest.R

/**
 * Phone notifications for plant alerts (shown in the status bar, even when the app is in the background).
 * Note: readings are only checked while the app is running.
 */
object PlantAlertNotifier {

    private const val CHANNEL_ID = "plant_alerts"
    private const val DRY_SOIL_ID = 1001

    /** True when the app may post notifications (always true before Android 13). */
    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** "Flora is too dry (38%). Water it now." Tapping it opens the app. */
    fun showDrySoil(context: Context, plantName: String, moisturePercent: Int) {
        if (!canNotify(context)) return
        createChannel(context)

        val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapAction = openApp?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_water_drop)
            .setContentTitle("$plantName needs water")
            .setContentText("Soil moisture is $moisturePercent%. Open AgriQuest and tap Water Plant.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .apply { if (tapAction != null) setContentIntent(tapAction) }
            .build()

        try {
            NotificationManagerCompat.from(context).notify(DRY_SOIL_ID, notification)
        } catch (e: SecurityException) {
            // Permission was turned off in the meantime; the in-app banner still shows the alert.
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Plant alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Alerts when your plant needs watering"
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
