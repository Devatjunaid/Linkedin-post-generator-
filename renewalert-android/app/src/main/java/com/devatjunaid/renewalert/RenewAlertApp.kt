package com.devatjunaid.renewalert

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.devatjunaid.renewalert.ads.AdsManager
import com.devatjunaid.renewalert.data.AppDatabase
import com.devatjunaid.renewalert.data.SettingsRepository
import com.devatjunaid.renewalert.data.SubscriptionRepository
import com.devatjunaid.renewalert.reminders.ReminderScheduler
import com.devatjunaid.renewalert.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Hand-rolled dependency container: small app, no DI framework needed. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val database = AppDatabase.build(context)
    val settings = SettingsRepository(context)
    val repository = SubscriptionRepository(database) {
        appScope.launch { WidgetUpdater.update(context) }
    }
    val ads = AdsManager(context as Application)
}

class RenewAlertApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createChannels()
        ReminderScheduler.schedule(this)
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_RENEWALS, getString(R.string.channel_renewals), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_TRIALS, getString(R.string.channel_trials), NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CHANNEL_DIGEST, getString(R.string.channel_digest), NotificationManager.IMPORTANCE_LOW),
            ),
        )
    }

    companion object {
        const val CHANNEL_RENEWALS = "renewals"
        const val CHANNEL_TRIALS = "trials"
        const val CHANNEL_DIGEST = "digest"
    }
}

val Context.container: AppContainer get() = (applicationContext as RenewAlertApp).container
