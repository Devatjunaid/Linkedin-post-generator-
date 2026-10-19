package com.devatjunaid.kelvin

import android.app.Application
import com.devatjunaid.kelvin.reminders.ReminderNotifications

class KelvinApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        ReminderNotifications.createChannel(this)
    }
}
