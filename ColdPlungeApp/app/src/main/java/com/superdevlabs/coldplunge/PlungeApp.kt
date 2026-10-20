package com.superdevlabs.coldplunge

import android.app.Application
import com.superdevlabs.coldplunge.reminders.ReminderNotifications

class PlungeApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        ReminderNotifications.createChannel(this)
    }
}
