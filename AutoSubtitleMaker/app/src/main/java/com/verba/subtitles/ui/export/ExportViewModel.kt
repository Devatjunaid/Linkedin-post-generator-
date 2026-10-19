package com.verba.subtitles.ui.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verba.subtitles.AppContainer
import com.verba.subtitles.core.captions.SubtitleFormat
import com.verba.subtitles.core.captions.SubtitleFormatter
import com.verba.subtitles.data.Project
import com.verba.subtitles.export.ExportQuality
import com.verba.subtitles.export.MediaSaver
import com.verba.subtitles.export.VideoExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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

enum class ExportPhase { IDLE, RUNNING, DONE, ERROR }

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
    private var job: Job? = null

    init {
        viewModelScope.launch { _ui.update { it.copy(project = container.projects.get(projectId)) } }
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
                val text = SubtitleFormatter.format(
                    ui.format, project.captions, project.style,
                    fontName = container.fonts.option(project.style.fontId).name,
                    videoWidth = project.width, videoHeight = project.height,
                )
                MediaSaver.writeText(context, uri, text)
            }.onSuccess {
                _ui.update { s ->
                    s.copy(savedSubtitle = true, phase = if (s.output.wantsVideo) s.phase else ExportPhase.DONE)
                }
            }.onFailure { e -> _ui.update { it.copy(error = e.message ?: "Couldn't save the subtitle file") } }
        }
    }

    fun startVideoExport(context: Context) {
        val start = _ui.value
        val initial = start.project ?: return
        job?.cancel()
        _ui.update { it.copy(phase = ExportPhase.RUNNING, progress = 0f, error = null, savedVideo = false) }
        job = viewModelScope.launch {
            try {
                // Use the latest saved edits from the editor.
                val project = container.projects.get(projectId) ?: initial
                val file = container.exporter.export(
                    project = project,
                    captions = project.captions,
                    style = project.style,
                    quality = start.quality,
                    keepAudio = start.keepAudio,
                ) { p -> _ui.update { it.copy(progress = p) } }
                MediaSaver.saveVideoToGallery(context.applicationContext, file)
                _ui.update { it.copy(phase = ExportPhase.DONE, progress = 1f, videoFile = file, savedVideo = true) }
            } catch (e: CancellationException) {
                _ui.update { it.copy(phase = ExportPhase.IDLE, progress = 0f) }
                throw e
            } catch (e: Throwable) {
                _ui.update { it.copy(phase = ExportPhase.ERROR, error = e.message ?: "Export failed") }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        _ui.update { it.copy(phase = ExportPhase.IDLE, progress = 0f, savedSubtitle = false) }
    }

    fun reset() = _ui.update { it.copy(phase = ExportPhase.IDLE, progress = 0f, error = null, savedSubtitle = false, savedVideo = false) }
}
