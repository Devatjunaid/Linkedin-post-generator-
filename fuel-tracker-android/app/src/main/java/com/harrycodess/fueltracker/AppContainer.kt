package com.harrycodess.fueltracker

import android.content.Context
import com.harrycodess.fueltracker.ads.AdsManager
import com.harrycodess.fueltracker.data.FuelRepository
import com.harrycodess.fueltracker.data.local.AppDatabase
import com.harrycodess.fueltracker.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** Manual dependency container, created once in [FuelTrackerApp]. */
class AppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob())
    val repository = FuelRepository(AppDatabase.build(context))
    val settingsRepository = SettingsRepository(context)
    val adsManager = AdsManager(context, applicationScope)
}
