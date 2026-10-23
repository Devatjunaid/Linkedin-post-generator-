package com.devatjunaid.examclock.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devatjunaid.examclock.R

/**
 * Inexact, battery-friendly alarm that tells the user a focus block finished while the app was in
 * the background. Uses setAndAllowWhileIdle, so no exact-alarm permission is needed.
 */
object FocusAlarm {
    private const val REQUEST_CODE = 41

    fun schedule(context: Context, triggerAtMillis: Long, isBreak: Boolean) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent(context, isBreak))
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        manager.cancel(pendingIntent(context, false))
        Notifications.cancel(context, Notifications.ID_FOCUS)
    }

    private fun pendingIntent(context: Context, isBreak: Boolean): PendingIntent {
        val intent = Intent(context, FocusAlarmReceiver::class.java).putExtra(FocusAlarmReceiver.EXTRA_BREAK, isBreak)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

class FocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isBreak = intent.getBooleanExtra(EXTRA_BREAK, false)
        val title = context.getString(if (isBreak) R.string.notif_break_over_title else R.string.notif_focus_done_title)
        val text = context.getString(if (isBreak) R.string.notif_break_over_text else R.string.notif_focus_done_text)
        Notifications.show(context, Notifications.ID_FOCUS, Notifications.CHANNEL_FOCUS, title, text)
    }

    companion object {
        const val EXTRA_BREAK = "is_break"
    }
}
