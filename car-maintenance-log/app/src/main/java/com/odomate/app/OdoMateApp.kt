package com.odomate.app

import android.app.Application
import com.odomate.app.reminders.Notifications
import com.odomate.app.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OdoMateApp : Application() {
    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        appScope.launch {
            val settings = container.settings.current()
            if (settings.notificationsEnabled) ReminderScheduler.schedule(this@OdoMateApp, settings.reminderHour)
            container.settings.incrementLaunchCount()
        }
    }
}
