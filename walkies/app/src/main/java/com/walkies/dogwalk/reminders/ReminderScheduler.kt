package com.walkies.dogwalk.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.walkies.dogwalk.data.LogType
import com.walkies.dogwalk.data.Reminder
import com.walkies.dogwalk.data.SettingsRepository
import com.walkies.dogwalk.data.WalkiesDatabase
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Arms reminders with inexact, Doze-friendly alarms. A dog walk does not need to-the-second
 * precision, so no exact-alarm permission is requested (Play restricts those to alarm-clock and
 * calendar apps).
 */
class ReminderScheduler(
    private val context: Context,
    private val db: WalkiesDatabase,
    private val settings: SettingsRepository,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun rescheduleAll() {
        db.reminderDao().all().forEach { schedule(it) }
        scheduleNoPeeAlert()
    }

    fun schedule(reminder: Reminder) {
        cancel(reminder.id)
        if (!reminder.enabled || reminder.daysMask == 0) return
        val trigger = nextTrigger(reminder, System.currentTimeMillis()) ?: return
        alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, reminderIntent(reminder.id))
    }

    fun cancel(reminderId: Long) {
        alarmManager?.cancel(reminderIntent(reminderId))
    }

    /** Arms (or clears) the "no pee logged for N hours" alert from the latest pee entry. */
    suspend fun scheduleNoPeeAlert() {
        val pending = noPeeIntent()
        alarmManager?.cancel(pending)
        val s = settings.current()
        if (!s.noPeeAlertEnabled) return
        val lastPee = db.logDao().latest(LogType.PEE.name) ?: return
        val trigger = lastPee.timestamp + s.noPeeAlertHours * HOUR
        if (trigger <= System.currentTimeMillis()) return
        alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
    }

    private fun reminderIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMINDER)
            .putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun noPeeIntent(): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_NO_PEE)
        return PendingIntent.getBroadcast(
            context,
            NO_PEE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val HOUR = 60L * 60L * 1000L
        private const val NO_PEE_REQUEST_CODE = Int.MAX_VALUE

        /** Next time [reminder] should fire after [now], honouring its days of the week. */
        fun nextTrigger(reminder: Reminder, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
            if (reminder.daysMask == 0) return null
            val start = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone)
            for (offset in 0..7) {
                val day = start.toLocalDate().plusDays(offset.toLong())
                val dayBit = 1 shl (day.dayOfWeek.value - 1)
                if (reminder.daysMask and dayBit == 0) continue
                val candidate = day.atTime(reminder.hour, reminder.minute).atZone(zone).toInstant().toEpochMilli()
                if (candidate > now) return candidate
            }
            return null
        }
    }
}
