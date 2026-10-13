package com.snaptensio.bplog

import android.app.Application
import android.content.Context
import com.snaptensio.bplog.ads.AdsManager
import com.snaptensio.bplog.data.AppDatabase
import com.snaptensio.bplog.data.Repository
import com.snaptensio.bplog.data.SettingsStore
import com.snaptensio.bplog.reminders.Notifications

class AppContainer(context: Context) {
    val database = AppDatabase.build(context)
    val repository = Repository(database)
    val settings = SettingsStore(context)
    val ads = AdsManager(context.applicationContext as Application)
}

class SnapTensioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
    }
}

val Context.appContainer: AppContainer get() = (applicationContext as SnapTensioApp).container
