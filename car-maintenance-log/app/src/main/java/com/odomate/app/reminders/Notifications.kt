package com.odomate.app.reminders

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
import com.odomate.app.MainActivity
import com.odomate.app.R

object Notifications {
    const val CHANNEL_REMINDERS = "service_reminders"
    const val CHANNEL_NUDGES = "odometer_nudges"
    const val EXTRA_SERVICE_ID = "extra_service_id"
    const val EXTRA_VEHICLE_ID = "extra_vehicle_id"
    const val EXTRA_OPEN_SCAN = "extra_open_scan"
    private const val NUDGE_ID = 900_001

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.channel_reminders_desc) }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_NUDGES, context.getString(R.string.channel_nudges), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_nudges_desc) }
        )
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showServiceReminder(context: Context, serviceId: Long, vehicleId: Long, title: String, text: String) {
        if (!canPost(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SERVICE_ID, serviceId)
            putExtra(EXTRA_VEHICLE_ID, vehicleId)
        }
        val pending = PendingIntent.getActivity(
            context, serviceId.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(0xFFF5A524.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        post(context, serviceId.toInt(), notification)
    }

    fun showScanNudge(context: Context, vehicleName: String) {
        if (!canPost(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SCAN, true)
        }
        val pending = PendingIntent.getActivity(context, NUDGE_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_NUDGES)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(0xFFF5A524.toInt())
            .setContentTitle("Snap your odometer")
            .setContentText("A quick photo of $vehicleName's odometer keeps your service reminders accurate.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("A quick photo of $vehicleName's odometer keeps your service reminders accurate."))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        post(context, NUDGE_ID, notification)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
