package com.odomate.app

import android.app.Application
import com.odomate.app.ads.AdsManager
import com.odomate.app.data.BackupManager
import com.odomate.app.data.CarRepository
import com.odomate.app.data.SettingsRepository
import com.odomate.app.data.db.AppDatabase

/** Hand-rolled dependency container; one per process. */
class AppContainer(app: Application) {
    val db: AppDatabase = AppDatabase.build(app)
    val settings = SettingsRepository(app)
    val repository = CarRepository(app, db, settings)
    val backup = BackupManager(app, db, settings)
    val ads = AdsManager(app)
}
