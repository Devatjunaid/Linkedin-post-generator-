package com.walkies.dogwalk

import android.app.Application
import com.walkies.dogwalk.data.Reminder
import com.walkies.dogwalk.data.ReminderKind
import com.walkies.dogwalk.reminders.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WalkiesApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        appScope.launch {
            if (container.settings.claimReminderSeeding()) {
                container.repository.seedDefaultReminders(defaultReminders())
            } else {
                // Alarms are dropped when the app is force-stopped; re-arm them on every start.
                container.scheduler.rescheduleAll()
            }
        }
    }

    private fun defaultReminders() = listOf(
        Reminder(kind = ReminderKind.WALK, label = getString(R.string.default_morning_walk), hour = 7, minute = 0, durationMinutes = 30),
        Reminder(
            kind = ReminderKind.WALK,
            label = getString(R.string.default_midday_walk),
            hour = 13,
            minute = 0,
            daysMask = Reminder.WEEKDAYS,
            durationMinutes = 20,
        ),
        Reminder(kind = ReminderKind.WALK, label = getString(R.string.default_evening_walk), hour = 18, minute = 30, durationMinutes = 45),
        Reminder(kind = ReminderKind.POTTY, label = getString(R.string.default_last_pee), hour = 22, minute = 30, enabled = false),
    )
}
