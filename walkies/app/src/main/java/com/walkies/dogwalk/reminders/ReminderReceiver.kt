package com.walkies.dogwalk.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.walkies.dogwalk.R
import com.walkies.dogwalk.WalkiesApp
import com.walkies.dogwalk.data.LogType
import com.walkies.dogwalk.data.ReminderKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Fires a reminder notification and arms the next occurrence. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as WalkiesApp).container
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = container.settings.current()
                when (intent.action) {
                    ACTION_REMINDER -> {
                        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                        val reminder = container.database.reminderDao().byId(id) ?: return@launch
                        if (!reminder.enabled) return@launch
                        val (title, body) = when (reminder.kind) {
                            ReminderKind.WALK -> context.getString(R.string.notif_walk_title, settings.dogName, reminder.label) to
                                context.getString(R.string.notif_walk_body)
                            ReminderKind.POTTY -> context.getString(R.string.notif_potty_title, settings.dogName, reminder.label) to
                                context.getString(R.string.notif_potty_body, settings.dogName)
                        }
                        Notifications.showReminder(context, reminder.id, title, body)
                        container.scheduler.schedule(reminder)
                    }
                    ACTION_NO_PEE -> {
                        if (!settings.noPeeAlertEnabled) return@launch
                        val last = container.database.logDao().latest(LogType.PEE.name) ?: return@launch
                        val threshold = settings.noPeeAlertHours * 60L * 60L * 1000L
                        // A pee may have been logged after this alarm was armed.
                        if (System.currentTimeMillis() - last.timestamp < threshold - SLACK_MS) return@launch
                        Notifications.showNoPee(
                            context,
                            context.getString(R.string.notif_no_pee_title, settings.noPeeAlertHours),
                            context.getString(R.string.notif_no_pee_body, settings.dogName),
                        )
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMINDER = "com.walkies.dogwalk.action.REMINDER"
        const val ACTION_NO_PEE = "com.walkies.dogwalk.action.NO_PEE"
        const val EXTRA_REMINDER_ID = "reminder_id"
        private const val SLACK_MS = 60_000L
    }
}
