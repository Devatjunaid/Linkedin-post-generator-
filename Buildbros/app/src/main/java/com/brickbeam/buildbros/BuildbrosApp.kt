package com.brickbeam.buildbros

import android.app.Application
import android.content.Context
import com.brickbeam.buildbros.ads.AdsManager
import com.brickbeam.buildbros.data.ProjectRepository
import com.brickbeam.buildbros.data.SettingsRepository
import com.brickbeam.buildbros.data.db.AppDatabase

/** Tiny manual DI container: one instance of each long-lived dependency. */
class AppContainer(context: Context) {
    val settings = SettingsRepository(context)
    val projects = ProjectRepository(AppDatabase.create(context).projectDao())
    val ads = AdsManager(context)
}

class BuildbrosApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as BuildbrosApp).container
