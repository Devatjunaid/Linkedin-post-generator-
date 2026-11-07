package com.superdevlabs.autosubtitles.ui.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superdevlabs.autosubtitles.AppContainer
import com.superdevlabs.autosubtitles.core.captions.SubtitleFormat
import com.superdevlabs.autosubtitles.core.captions.SubtitleFormatter
import com.superdevlabs.autosubtitles.data.Project
import com.superdevlabs.autosubtitles.export.ExportQuality
import com.superdevlabs.autosubtitles.export.MediaSaver
import com.superdevlabs.autosubtitles.export.VideoExporter
import com.superdevlabs.autosubtitles.work.ExportJob
import com.superdevlabs.autosubtitles.work.ExportPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class OutputKind(val label: String, val wantsVideo: Boolean, val wantsSubtitles: Boolean) {
    VIDEO("Burn into video", true, false),
    SUBTITLES("Subtitle file", false, true),
    BOTH("Both", true, true),
}

data class ExportUi(
    val project: Project? = null,
    val output: OutputKind = OutputKind.VIDEO,
    val quality: ExportQuality = ExportQuality.FULL_HD,
    val format: SubtitleFormat = SubtitleFormat.SRT,
    val keepAudio: Boolean = true,
    val phase: ExportPhase = ExportPhase.IDLE,
    val progress: Float = 0f,
    val videoFile: File? = null,
    val savedVideo: Boolean = false,
    val savedSubtitle: Boolean = false,
    val error: String? = null,
) {
    /** Rough output size for the chosen quality (H.264 at typical phone bitrates). */
    val estimate: String
        get() {
            val p = project ?: return "—"
            if (!output.wantsVideo) return "≈ ${(p.captions.size * 60 / 1024).coerceAtLeast(1)} KB"
            val shortSide = quality.shortSide ?: minOf(p.width, p.height)
            val mbps = when {
                shortSide <= 720 -> 4.0
                shortSide <= 1080 -> 8.0
                else -> 16.0
            }
            val mb = p.durationMs / 1000.0 * mbps / 8.0
            return "≈ ${"%.0f".format(mb.coerceAtLeast(1.0))} MB"
        }
}

class ExportViewModel(private val container: AppContainer, private val projectId: String) : ViewModel() {

    private val _ui = MutableStateFlow(ExportUi())
    val ui: StateFlow<ExportUi> = _ui.asStateFlow()
    private var countedFinish = false

    init {
        viewModelScope.launch { _ui.update { it.copy(project = container.projects.get(projectId)) } }
        // The video itself renders in ExportWorker (keeps going in the background); mirror its state.
        viewModelScope.launch {
            container.jobs.export(projectId).collect { job ->
                if (job == null) return@collect
                _ui.update { ui ->
                    ui.copy(
                        phase = job.phase,
                        progress = job.progress,
                        videoFile = job.videoFile ?: ui.videoFile,
                        savedVideo = job.phase == ExportPhase.DONE,
                        error = job.error,
                    )
                }
            }
        }
    }

    fun setOutput(kind: OutputKind) = _ui.update { it.copy(output = kind) }
    fun setQuality(quality: ExportQuality) = _ui.update { it.copy(quality = quality) }
    fun setFormat(format: SubtitleFormat) = _ui.update { it.copy(format = format) }
    fun setKeepAudio(keep: Boolean) = _ui.update { it.copy(keepAudio = keep) }

    fun suggestedSubtitleName(): String {
        val ui = _ui.value
        return VideoExporter.safeFileName(ui.project?.name ?: "subtitles") + "." + ui.format.extension
    }

    /** Writes the subtitle file into the document the user created. */
    fun writeSubtitle(context: Context, uri: Uri) {
        val ui = _ui.value
        val project = ui.project ?: return
        viewModelScope.launch {
            runCatching {
                // Use the latest saved edits from the editor.
                val latest = container.projects.get(projectId) ?: project
                val text = SubtitleFormatter.format(
                    ui.format, latest.captions, latest.style,
                    fontName = container.fonts.option(latest.style.fontId).name,
                    videoWidth = latest.width, videoHeight = latest.height,
                )
                MediaSaver.writeText(context, uri, text)
            }.onSuccess {
                container.telemetry.event("subtitle_file_saved", "format" to ui.format.name)
                _ui.update { s ->
                    s.copy(savedSubtitle = true, phase = if (s.output.wantsVideo) s.phase else ExportPhase.DONE)
                }
            }.onFailure { e -> _ui.update { it.copy(error = e.message ?: "Couldn't save the subtitle file") } }
        }
    }

    fun startVideoExport() {
        val start = _ui.value
        countedFinish = false
        container.jobs.updateExport(projectId) { ExportJob(phase = ExportPhase.RUNNING) }
        container.work.startExport(projectId, start.quality, start.keepAudio)
    }

    /** Called once per finished export; returns the total number of successful exports. */
    suspend fun countFinishedExport(): Int? {
        if (countedFinish) return null
        countedFinish = true
        return container.settings.incrementExports()
    }

    fun cancel() {
        container.work.cancelExport(projectId)
        container.jobs.clearExport(projectId)
        _ui.update { it.copy(phase = ExportPhase.IDLE, progress = 0f, savedSubtitle = false) }
    }

    fun reset() {
        container.jobs.clearExport(projectId)
        countedFinish = false
        _ui.update { it.copy(phase = ExportPhase.IDLE, progress = 0f, error = null, savedSubtitle = false, savedVideo = false) }
    }
}
