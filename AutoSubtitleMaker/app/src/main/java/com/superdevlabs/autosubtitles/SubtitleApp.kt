package com.superdevlabs.autosubtitles

import android.app.Application
import android.content.Context
import com.superdevlabs.autosubtitles.ads.AdsManager
import com.superdevlabs.autosubtitles.ads.ConsentManager
import com.superdevlabs.autosubtitles.core.render.FontRegistry
import com.superdevlabs.autosubtitles.data.AppDatabase
import com.superdevlabs.autosubtitles.data.ProjectRepository
import com.superdevlabs.autosubtitles.data.SettingsRepository
import com.superdevlabs.autosubtitles.export.VideoExporter
import com.superdevlabs.autosubtitles.speech.ModelCatalog
import com.superdevlabs.autosubtitles.speech.ModelManager
import com.superdevlabs.autosubtitles.speech.Transcriber
import com.superdevlabs.autosubtitles.speech.WhisperTranscriber
import com.superdevlabs.autosubtitles.telemetry.Telemetry
import com.superdevlabs.autosubtitles.translate.CaptionTranslator
import com.superdevlabs.autosubtitles.work.JobState
import com.superdevlabs.autosubtitles.work.Jobs
import com.superdevlabs.autosubtitles.work.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow

/** Hand-wired dependencies (small app, no DI framework needed). */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val telemetry = Telemetry(context)
    val database: AppDatabase = AppDatabase.create(context)
    val projects = ProjectRepository(context, database.projects())
    val models = ModelManager(context, appScope)
    val settings = SettingsRepository(context) { ModelCatalog.models.any { models.isInstalled(it.id) } }
    val fonts = FontRegistry(context)
    val transcriber = Transcriber()
    val whisper = WhisperTranscriber(context)
    val translator = CaptionTranslator()
    val exporter = VideoExporter(context, fonts)
    val consent = ConsentManager(context)
    val ads = AdsManager(context, appScope)
    val jobs = JobState()
    val work = Jobs(context)

    /** Project to open after the user taps a "done" notification (consumed by navigation). */
    val pendingOpen = MutableStateFlow<Pair<String, String>?>(null)
}

class SubtitleApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.ensureChannels(this)
        // Respect the "Share usage & crash reports" switch from the first launch on.
        container.appScope.launch {
            container.settings.settings.map { it.shareDiagnostics }.distinctUntilChanged().collect {
                container.telemetry.setCollectionEnabled(it)
            }
        }
    }
}

val Context.appContainer: AppContainer get() = (applicationContext as SubtitleApp).container
