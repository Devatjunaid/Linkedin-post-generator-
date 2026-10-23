package com.devatjunaid.examclock

import android.app.Application
import com.devatjunaid.examclock.notifications.Notifications
import com.devatjunaid.examclock.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExamClockApp : Application() {

    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        appScope.launch {
            val prefs = container.preferences.current()
            if (prefs.onboardingDone) ReminderScheduler.schedule(this@ExamClockApp, prefs.remindersEnabled, prefs.reminderMinuteOfDay)
        }
    }
}
