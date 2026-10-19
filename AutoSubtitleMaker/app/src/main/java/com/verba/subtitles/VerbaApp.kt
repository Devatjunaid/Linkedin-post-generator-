package com.verba.subtitles

import android.app.Application
import android.content.Context
import com.verba.subtitles.ads.AdsManager
import com.verba.subtitles.ads.ConsentManager
import com.verba.subtitles.core.render.FontRegistry
import com.verba.subtitles.data.ProjectRepository
import com.verba.subtitles.data.SettingsRepository
import com.verba.subtitles.data.VerbaDatabase
import com.verba.subtitles.export.VideoExporter
import com.verba.subtitles.speech.ModelManager
import com.verba.subtitles.speech.Transcriber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hand-wired dependencies (small app, no DI framework needed). */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val database: VerbaDatabase = VerbaDatabase.create(context)
    val projects = ProjectRepository(context, database.projects())
    val settings = SettingsRepository(context)
    val fonts = FontRegistry(context)
    val models = ModelManager(context, appScope)
    val transcriber = Transcriber()
    val exporter = VideoExporter(context, fonts)
    val consent = ConsentManager(context)
    val ads = AdsManager(context, appScope)
}

class VerbaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer get() = (applicationContext as VerbaApp).container
