package app.gigometer.mileage

import android.content.Context
import app.gigometer.mileage.data.AppDatabase
import app.gigometer.mileage.data.SettingsRepository
import app.gigometer.mileage.data.TripDao

/** Tiny service locator — the app is small enough not to need a DI framework. */
object Graph {
    lateinit var appContext: Context
        private set
    val db: AppDatabase by lazy { AppDatabase.get(appContext) }
    val trips: TripDao by lazy { db.trips() }
    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
