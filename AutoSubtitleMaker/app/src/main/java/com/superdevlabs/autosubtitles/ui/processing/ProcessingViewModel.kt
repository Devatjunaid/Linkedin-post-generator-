package com.superdevlabs.autosubtitles.ui.processing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superdevlabs.autosubtitles.AppContainer
import com.superdevlabs.autosubtitles.core.captions.CaptionSegmenter
import com.superdevlabs.autosubtitles.data.ProjectStatus
import com.superdevlabs.autosubtitles.speech.ModelCatalog
import com.superdevlabs.autosubtitles.speech.ModelState
import com.superdevlabs.autosubtitles.speech.NoAudioTrackException
import com.superdevlabs.autosubtitles.speech.TranscriptionListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

enum class StepId(val title: String) {
    IMPORT("Import video"),
    MODEL("Prepare speech model"),
    LISTEN("Listen & transcribe"),
    CAPTIONS("Time the subtitles"),
}

enum class StepStatus { PENDING, RUNNING, DONE, ERROR }

data class StepUi(val id: StepId, val status: StepStatus = StepStatus.PENDING, val detail: String = "", val progress: Float = 0f)

data class ProcessingUi(
    val projectName: String = "",
    val thumbnail: File? = null,
    val language: String = "",
    val steps: List<StepUi> = StepId.entries.map { StepUi(it) },
    val transcript: String = "",
    val partial: String = "",
    val error: String? = null,
    val done: Boolean = false,
    val captionCount: Int = 0,
) {
    /** Weighted overall progress for the big ring. */
    val overall: Float
        get() {
            val weights = mapOf(StepId.IMPORT to 0.1f, StepId.MODEL to 0.15f, StepId.LISTEN to 0.7f, StepId.CAPTIONS to 0.05f)
            return steps.sumOf { step ->
                val w = weights.getValue(step.id)
                when (step.status) {
                    StepStatus.DONE -> w.toDouble()
                    StepStatus.RUNNING -> (w * step.progress).toDouble()
                    else -> 0.0
                }
            }.toFloat().coerceIn(0f, 1f)
        }
}

class ProcessingViewModel(private val container: AppContainer, private val projectId: String) : ViewModel() {

    private val _ui = MutableStateFlow(ProcessingUi())
    val ui: StateFlow<ProcessingUi> = _ui.asStateFlow()
    private var job: Job? = null

    init {
        start()
    }

    fun retry() = start()

    private fun start() {
        job?.cancel()
        _ui.value = ProcessingUi()
        job = viewModelScope.launch {
            try {
                run()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val message = when (e) {
                    is NoAudioTrackException -> e.message
                    is IOException -> e.message ?: "Something went wrong reading the video."
                    else -> e.message ?: "Something went wrong."
                }
                _ui.update { ui ->
                    ui.copy(
                        error = message,
                        steps = ui.steps.map { if (it.status == StepStatus.RUNNING) it.copy(status = StepStatus.ERROR) else it },
                    )
                }
                container.projects.markFailed(projectId)
            }
        }
    }

    private suspend fun run() {
        val repo = container.projects
        val initial = repo.get(projectId) ?: throw IOException("Project not found")
        val model = ModelCatalog.byId(initial.languageId)
        _ui.update { it.copy(projectName = initial.name, language = model.language, thumbnail = initial.thumbnail) }

        // 1. Copy the video into app storage and read its metadata.
        step(StepId.IMPORT, StepStatus.RUNNING, "Copying")
        val project = repo.importVideo(projectId) { p -> step(StepId.IMPORT, StepStatus.RUNNING, "Copying ${(p * 100).toInt()}%", p) }
        _ui.update { it.copy(thumbnail = project.thumbnail) }
        step(StepId.IMPORT, StepStatus.DONE, "${project.width}×${project.height}", 1f)

        // 2. Make sure the offline model for the chosen language is on the device.
        if (!container.models.isInstalled(model.id)) {
            step(StepId.MODEL, StepStatus.RUNNING, "Downloading ${model.sizeMb} MB (one time)")
            val watcher = viewModelScope.launch {
                container.models.states.collect { states ->
                    when (val s = states[model.id]) {
                        is ModelState.Downloading -> step(
                            StepId.MODEL, StepStatus.RUNNING,
                            "Downloading ${(s.progress * 100).toInt()}% of ${model.sizeMb} MB", s.progress,
                        )
                        ModelState.Unpacking -> step(StepId.MODEL, StepStatus.RUNNING, "Unpacking", 0.98f)
                        else -> Unit
                    }
                }
            }
            try {
                container.models.install(model.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                throw IOException("Couldn't download the ${model.language} model. Check your connection and retry.", e)
            } finally {
                watcher.cancel()
            }
        }
        step(StepId.MODEL, StepStatus.DONE, "${model.language} · offline", 1f)

        // 3. Decode the sound track and run speech recognition with word timestamps.
        val video = project.videoFile ?: throw IOException("The video is missing")
        step(StepId.LISTEN, StepStatus.RUNNING, "0%")
        val words = container.transcriber.transcribe(
            video,
            container.models.modelDir(model.id),
            object : TranscriptionListener {
                override fun onProgress(progress: Float) {
                    step(StepId.LISTEN, StepStatus.RUNNING, "${(progress * 100).toInt()}%", progress)
                }

                override fun onTranscript(confirmed: String, partial: String) {
                    _ui.update { it.copy(transcript = confirmed, partial = partial) }
                }
            },
        )
        if (words.isEmpty()) {
            throw IOException("No speech was found. Check the video has people talking and that the language (${model.language}) is right.")
        }
        step(StepId.LISTEN, StepStatus.DONE, "${words.size} words", 1f)

        // 4. Group words into timed captions.
        step(StepId.CAPTIONS, StepStatus.RUNNING, "", 0.5f)
        val captions = CaptionSegmenter.build(words, project.style.wordsPerCaption)
        repo.saveCaptions(projectId, captions, ProjectStatus.READY)
        step(StepId.CAPTIONS, StepStatus.DONE, "${captions.size} captions", 1f)
        _ui.update { it.copy(done = true, captionCount = captions.size, partial = "") }
    }

    private fun step(id: StepId, status: StepStatus, detail: String, progress: Float = 0f) {
        _ui.update { ui ->
            ui.copy(steps = ui.steps.map { if (it.id == id) StepUi(id, status, detail, progress.coerceIn(0f, 1f)) else it })
        }
    }
}
