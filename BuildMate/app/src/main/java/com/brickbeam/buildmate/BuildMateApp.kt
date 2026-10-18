package com.brickbeam.buildmate

import android.app.Application
import android.content.Context
import com.brickbeam.buildmate.ads.AdsManager
import com.brickbeam.buildmate.data.ProjectRepository
import com.brickbeam.buildmate.data.SettingsRepository
import com.brickbeam.buildmate.data.db.AppDatabase

/** Tiny manual DI container: one instance of each long-lived dependency. */
class AppContainer(context: Context) {
    val settings = SettingsRepository(context)
    val projects = ProjectRepository(AppDatabase.create(context).projectDao())
    val ads = AdsManager(context)
}

class BuildMateApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as BuildMateApp).container
