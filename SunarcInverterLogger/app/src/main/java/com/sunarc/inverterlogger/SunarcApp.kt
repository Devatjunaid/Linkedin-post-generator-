package com.sunarc.inverterlogger

import android.app.Application
import com.sunarc.inverterlogger.reminders.ReminderScheduler

class SunarcApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderScheduler.createChannel(this)
    }
}
