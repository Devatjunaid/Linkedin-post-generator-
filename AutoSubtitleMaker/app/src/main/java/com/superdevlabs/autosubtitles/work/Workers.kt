package com.superdevlabs.autosubtitles.work

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.superdevlabs.autosubtitles.appContainer
import com.superdevlabs.autosubtitles.export.ExportQuality
import com.superdevlabs.autosubtitles.export.MediaSaver
import com.superdevlabs.autosubtitles.telemetry.Telemetry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Starts and cancels the background jobs. One job of each kind per project at a time. */
class Jobs(private val context: Context) {

    private val workManager get() = WorkManager.getInstance(context)

    /** Starts subtitle creation unless it is already queued or running for this project. */
    fun startProcessing(projectId: String, restart: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<ProcessingWorker>()
            .setInputData(workDataOf(KEY_PROJECT to projectId))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(TAG_PROCESSING)
            .build()
        workManager.enqueueUniqueWork(
            processingName(projectId),
            if (restart) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelProcessing(projectId: String) = workManager.cancelUniqueWork(processingName(projectId))

    fun startExport(projectId: String, quality: ExportQuality, keepAudio: Boolean) {
        val request = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(workDataOf(KEY_PROJECT to projectId, KEY_QUALITY to quality.name, KEY_AUDIO to keepAudio))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(TAG_EXPORT)
            .build()
        workManager.enqueueUniqueWork(exportName(projectId), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelExport(projectId: String) = workManager.cancelUniqueWork(exportName(projectId))

    companion object {
        const val KEY_PROJECT = "project"
        const val KEY_QUALITY = "quality"
        const val KEY_AUDIO = "audio"
        const val TAG_PROCESSING = "processing"
        const val TAG_EXPORT = "export"

        fun processingName(projectId: String) = "process-$projectId"
        fun exportName(projectId: String) = "export-$projectId"
    }
}

/**
 * Shared foreground plumbing: Android keeps a job with a visible notification running when the
 * screen turns off or the user switches apps. Uses the "media processing" foreground-service
 * type on Android 15+ and "data sync" below.
 */
abstract class ForegroundJobWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    protected val projectId: String get() = inputData.getString(Jobs.KEY_PROJECT).orEmpty()

    protected abstract val notificationId: Int
    protected abstract fun buildInfo(title: String, text: String, progress: Float): ForegroundInfo

    protected fun info(notification: android.app.Notification): ForegroundInfo = when {
        Build.VERSION.SDK_INT >= 35 ->
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
        Build.VERSION.SDK_INT >= 29 ->
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else -> ForegroundInfo(notificationId, notification)
    }

    /** Becomes a foreground job; if Android refuses (rare background restart), keep going anyway. */
    protected suspend fun goForeground(title: String, text: String, progress: Float) {
        try {
            setForeground(buildInfo(title, text, progress))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            applicationContext.appContainer.telemetry.breadcrumb("foreground refused: ${e::class.simpleName}")
        }
    }
}

class ProcessingWorker(context: Context, params: WorkerParameters) : ForegroundJobWorker(context, params) {

    override val notificationId: Int get() = Notifications.processingId(projectId)

    override suspend fun getForegroundInfo(): ForegroundInfo = buildInfo("Creating subtitles…", "Starting", 0f)

    override fun buildInfo(title: String, text: String, progress: Float): ForegroundInfo {
        val cancel = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        return info(
            Notifications.progress(applicationContext, title, text, progress, cancel, projectId, Notifications.TARGET_EDITOR),
        )
    }

    override suspend fun doWork(): Result {
        if (projectId.isBlank()) return Result.failure()
        val container = applicationContext.appContainer
        Notifications.ensureChannels(applicationContext)
        val name = container.projects.get(projectId)?.name ?: "Your video"
        goForeground("Creating subtitles…", name, 0f)

        val ok = coroutineScope {
            // Mirror progress into the notification, at most every 1.5 s.
            val mirror = launch {
                var last = -1
                while (true) {
                    val ui = container.jobs.processingNow(projectId)
                    if (ui != null) {
                        val percent = (ui.overall * 100).toInt()
                        if (percent != last) {
                            last = percent
                            val running = ui.steps.firstOrNull { it.status == StepStatus.RUNNING }
                            goForeground("Creating subtitles · $percent%", running?.id?.title ?: name, ui.overall)
                        }
                    }
                    delay(1_500)
                }
            }
            try {
                ProcessingPipeline(container).run(projectId)
            } catch (e: CancellationException) {
                // Stopped by the user (screen or notification): forget the half-done progress.
                container.jobs.clearProcessing(projectId)
                throw e
            } finally {
                mirror.cancel()
            }
        }

        val ui = container.jobs.processingNow(projectId)
        if (ok) {
            Notifications.done(
                applicationContext, Notifications.processingDoneId(projectId),
                "Subtitles ready ✨", "$name · ${ui?.captionCount ?: 0} captions. Tap to style and export.",
                projectId, Notifications.TARGET_EDITOR,
            )
        } else {
            Notifications.done(
                applicationContext, Notifications.processingDoneId(projectId),
                "Couldn't create subtitles", ui?.error ?: name, projectId, Notifications.TARGET_EDITOR,
            )
        }
        // Errors are shown in the app; never let WorkManager retry a failed video by itself.
        return if (ok) Result.success() else Result.failure()
    }
}

class ExportWorker(context: Context, params: WorkerParameters) : ForegroundJobWorker(context, params) {

    override val notificationId: Int get() = Notifications.exportId(projectId)

    override suspend fun getForegroundInfo(): ForegroundInfo = buildInfo("Exporting video…", "Starting", 0f)

    override fun buildInfo(title: String, text: String, progress: Float): ForegroundInfo {
        val cancel = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        return info(
            Notifications.progress(applicationContext, title, text, progress, cancel, projectId, Notifications.TARGET_EXPORT),
        )
    }

    override suspend fun doWork(): Result {
        if (projectId.isBlank()) return Result.failure()
        val container = applicationContext.appContainer
        val jobs = container.jobs
        Notifications.ensureChannels(applicationContext)
        val quality = runCatching { ExportQuality.valueOf(inputData.getString(Jobs.KEY_QUALITY).orEmpty()) }
            .getOrDefault(ExportQuality.FULL_HD)
        val keepAudio = inputData.getBoolean(Jobs.KEY_AUDIO, true)
        val project = container.projects.get(projectId) ?: return Result.failure()

        jobs.updateExport(projectId) { ExportJob(phase = ExportPhase.RUNNING) }
        goForeground("Exporting video…", project.name, 0f)
        val started = System.currentTimeMillis()
        return try {
            var lastPercent = -1
            var lastShownAt = 0L
            val file = container.exporter.export(
                project = project,
                captions = project.captions,
                style = project.style,
                quality = quality,
                keepAudio = keepAudio,
            ) { p ->
                jobs.updateExport(projectId) { it.copy(phase = ExportPhase.RUNNING, progress = p) }
                val percent = (p * 100).toInt()
                val now = System.currentTimeMillis()
                if (percent != lastPercent && now - lastShownAt > 1_500) {
                    lastPercent = percent
                    lastShownAt = now
                    // setForeground is a suspend call; progress callbacks come on the main thread.
                    container.appScope.launch { goForeground("Exporting video · $percent%", project.name, p) }
                }
            }
            MediaSaver.saveVideoToGallery(applicationContext, file)
            jobs.updateExport(projectId) { ExportJob(phase = ExportPhase.DONE, progress = 1f, videoFile = file) }
            container.telemetry.event(
                "video_exported",
                "quality" to quality.name,
                "video_length" to Telemetry.durationBucket(project.durationMs),
                "seconds" to (System.currentTimeMillis() - started) / 1000,
                "preset" to project.style.presetId,
                "translated" to project.captions.any { !it.translation.isNullOrBlank() },
            )
            Notifications.done(
                applicationContext, Notifications.exportDoneId(projectId),
                "Video saved to your gallery 🎬", "${project.name} · Movies/AutoSubtitles",
                projectId, Notifications.TARGET_EXPORT,
            )
            Result.success()
        } catch (e: CancellationException) {
            jobs.updateExport(projectId) { ExportJob(phase = ExportPhase.IDLE) }
            throw e
        } catch (e: Throwable) {
            val message = e.message ?: "Export failed"
            jobs.updateExport(projectId) { ExportJob(phase = ExportPhase.ERROR, error = message) }
            container.telemetry.event("export_failed", "reason" to (e::class.simpleName ?: "unknown"), "quality" to quality.name)
            container.telemetry.recordError(e, "export")
            Notifications.done(
                applicationContext, Notifications.exportDoneId(projectId),
                "Export failed", message, projectId, Notifications.TARGET_EXPORT,
            )
            Result.failure()
        }
    }
}
