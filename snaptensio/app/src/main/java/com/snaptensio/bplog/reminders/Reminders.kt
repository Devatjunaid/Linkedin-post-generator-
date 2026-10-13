package com.snaptensio.bplog.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.snaptensio.bplog.MainActivity
import com.snaptensio.bplog.R
import com.snaptensio.bplog.appContainer
import com.snaptensio.bplog.data.Medication
import com.snaptensio.bplog.data.Reminder
import com.snaptensio.bplog.data.timeList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object Notifications {
    const val CHANNEL_REMINDERS = "reminders"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.channel_reminders_desc) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

/**
 * Daily reminders use inexact, battery-friendly alarms (no SCHEDULE_EXACT_ALARM permission,
 * which Play restricts to alarm-clock and calendar apps). They may fire a few minutes late.
 */
object ReminderScheduler {
    private const val EXTRA_CODE = "code"
    private const val EXTRA_HOUR = "hour"
    private const val EXTRA_MINUTE = "minute"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_BODY = "body"
    private const val MED_BASE = 1_000_000
    private const val MAX_TIMES = 12

    private fun measureCode(id: Long) = (id % 900_000).toInt()
    private fun medCode(id: Long, index: Int) = MED_BASE + ((id % 80_000) * MAX_TIMES).toInt() + index

    fun nextTrigger(hour: Int, minute: Int, now: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= now) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    private fun pending(context: Context, code: Int, hour: Int, minute: Int, title: String, body: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_CODE, code); putExtra(EXTRA_HOUR, hour); putExtra(EXTRA_MINUTE, minute)
            putExtra(EXTRA_TITLE, title); putExtra(EXTRA_BODY, body)
        }
        return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun set(context: Context, code: Int, hour: Int, minute: Int, title: String, body: String) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger(hour, minute), pending(context, code, hour, minute, title, body))
    }

    private fun cancel(context: Context, code: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) {
            context.getSystemService(AlarmManager::class.java).cancel(pi)
            pi.cancel()
        }
    }

    fun schedule(context: Context, r: Reminder) {
        if (r.enabled) {
            set(context, measureCode(r.id), r.hour, r.minute, context.getString(R.string.notif_measure_title), context.getString(R.string.notif_measure_body))
        } else {
            cancel(context, measureCode(r.id))
        }
    }

    fun cancel(context: Context, r: Reminder) = cancel(context, measureCode(r.id))

    fun schedule(context: Context, m: Medication) {
        cancel(context, m)
        if (!m.active) return
        m.timeList().take(MAX_TIMES).forEachIndexed { i, t ->
            val (h, min) = t.split(':').map { it.toInt() }
            set(context, medCode(m.id, i), h, min, context.getString(R.string.notif_med_title), "${m.name} ${m.dose}".trim())
        }
    }

    fun cancel(context: Context, m: Medication) {
        repeat(MAX_TIMES) { cancel(context, medCode(m.id, it)) }
    }

    suspend fun rescheduleAll(context: Context) {
        val repo = context.appContainer.repository
        repo.allReminders().forEach { schedule(context, it) }
        repo.activeMedications().forEach { schedule(context, it) }
    }

    internal fun onFired(context: Context, intent: Intent) {
        val code = intent.getIntExtra(EXTRA_CODE, -1)
        val hour = intent.getIntExtra(EXTRA_HOUR, 8)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        if (code < 0) return
        // Re-arm for tomorrow first, so a notification failure never stops the series.
        set(context, code, hour, minute, title, body)
        if (!Notifications.canPost(context)) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, Notifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_heart_pulse)
            .setColor(ContextCompat.getColor(context, R.color.brand_blue))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(code, n)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = ReminderScheduler.onFired(context, intent)
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.rescheduleAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
