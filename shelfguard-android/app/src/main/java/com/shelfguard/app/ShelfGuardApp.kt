package com.shelfguard.app

import android.app.Application
import com.shelfguard.app.ads.AppOpenAdManager
import com.shelfguard.app.data.db.AppDatabase
import com.shelfguard.app.data.prefs.SettingsRepository
import com.shelfguard.app.data.repo.InventoryRepository
import com.shelfguard.app.notifications.AlertScheduler
import com.shelfguard.app.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Simple manual dependency container shared by the whole app. */
class AppContainer(app: Application) {
    val database: AppDatabase = AppDatabase.create(app)
    val inventory = InventoryRepository(database)
    val settings = SettingsRepository(app)
}

class ShelfGuardApp : Application() {
    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
        AppOpenAdManager(this)

        // Keep the daily alert scheduled at the user's chosen time.
        appScope.launch {
            val s = container.settings.current()
            AlertScheduler.schedule(this@ShelfGuardApp, s.remindersEnabled, s.reminderHour, s.reminderMinute)
        }
    }
}
