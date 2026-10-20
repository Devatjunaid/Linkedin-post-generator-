package com.superdevlabs.autosubtitles

import android.app.Application
import android.content.Context
import com.superdevlabs.autosubtitles.ads.AdsManager
import com.superdevlabs.autosubtitles.ads.ConsentManager
import com.superdevlabs.autosubtitles.core.render.FontRegistry
import com.superdevlabs.autosubtitles.data.ProjectRepository
import com.superdevlabs.autosubtitles.data.SettingsRepository
import com.superdevlabs.autosubtitles.data.AppDatabase
import com.superdevlabs.autosubtitles.export.VideoExporter
import com.superdevlabs.autosubtitles.speech.ModelManager
import com.superdevlabs.autosubtitles.speech.Transcriber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hand-wired dependencies (small app, no DI framework needed). */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val database: AppDatabase = AppDatabase.create(context)
    val projects = ProjectRepository(context, database.projects())
    val settings = SettingsRepository(context)
    val fonts = FontRegistry(context)
    val models = ModelManager(context, appScope)
    val transcriber = Transcriber()
    val exporter = VideoExporter(context, fonts)
    val consent = ConsentManager(context)
    val ads = AdsManager(context, appScope)
}

class SubtitleApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer get() = (applicationContext as SubtitleApp).container
