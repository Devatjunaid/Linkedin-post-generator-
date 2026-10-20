package com.superdevlabs.coldplunge.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.superdevlabs.coldplunge.MainActivity
import com.superdevlabs.coldplunge.R
import com.superdevlabs.coldplunge.domain.Stats
import com.superdevlabs.coldplunge.graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object ReminderNotifications {
    const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 4101

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "A once-a-day nudge if you haven't logged a session yet"
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        val permissionOk = android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permissionOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, streak: Int) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (streak > 0) "Keep your $streak-day streak alive — even a 1-minute plunge counts."
        else "A short session today? Your timer is ready."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time for your session")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the call.
        }
    }
}

/**
 * Daily reminder using an inexact repeating alarm: no exact-alarm permission is needed
 * (exact alarms are restricted by Google Play policy), and Android may shift it by a few minutes.
 */
object ReminderScheduler {
    private const val REQUEST_CODE = 4100

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun schedule(context: Context, hour: Int, minute: Int) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val zone = ZoneId.systemDefault()
        var next = LocalDate.now(zone).atTime(hour, minute)
        if (!next.isAfter(LocalDateTime.now(zone))) next = next.plusDays(1)
        val triggerAt = next.atZone(zone).toInstant().toEpochMilli()
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAt, AlarmManager.INTERVAL_DAY, pendingIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }
}

/** Posts the reminder only if nothing has been logged today. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = app.graph.settings.current()
                if (settings.reminderEnabled) {
                    val zone = ZoneId.systemDefault()
                    val today = LocalDate.now(zone)
                    val sessions = app.graph.sessions.all()
                    val days = sessions.map { Stats.dateOf(it.startedAt, zone) }.toSet()
                    if (today !in days) ReminderNotifications.show(app, Stats.currentStreak(days, today))
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms are cleared on reboot; re-arm the reminder if the user enabled it. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val s = app.graph.settings.current()
                if (s.reminderEnabled) ReminderScheduler.schedule(app, s.reminderHour, s.reminderMinute)
            } finally {
                pending.finish()
            }
        }
    }
}
