package com.walkies.dogwalk

import android.app.Application
import com.walkies.dogwalk.ads.AdsManager
import com.walkies.dogwalk.data.SettingsRepository
import com.walkies.dogwalk.data.WalkiesDatabase
import com.walkies.dogwalk.data.WalkiesRepository
import com.walkies.dogwalk.reminders.ReminderScheduler
import com.walkies.dogwalk.walk.StepCounter

/** Hand-wired app-wide objects. */
class AppContainer(app: Application) {
    val database: WalkiesDatabase = WalkiesDatabase.build(app)
    val settings = SettingsRepository(app, app.getString(R.string.default_dog_name))
    val scheduler = ReminderScheduler(app, database, settings)
    val repository = WalkiesRepository(database, scheduler)
    val stepCounter = StepCounter(app)
    val ads = AdsManager(app)
}
