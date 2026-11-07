package com.superdevlabs.autosubtitles.work

import com.superdevlabs.autosubtitles.AppContainer
import com.superdevlabs.autosubtitles.core.captions.CaptionSegmenter
import com.superdevlabs.autosubtitles.core.model.Word
import com.superdevlabs.autosubtitles.data.ProjectStatus
import com.superdevlabs.autosubtitles.speech.ModelCatalog
import com.superdevlabs.autosubtitles.speech.ModelState
import com.superdevlabs.autosubtitles.speech.NoAudioTrackException
import com.superdevlabs.autosubtitles.speech.TranscriptionListener
import com.superdevlabs.autosubtitles.speech.WhisperCatalog
import com.superdevlabs.autosubtitles.telemetry.Telemetry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Video → timed captions: import, make sure the speech model is installed, transcribe,
 * group words into captions. Runs inside [ProcessingWorker]; progress goes to [JobState].
 */
class ProcessingPipeline(private val container: AppContainer) {

    private val jobs get() = container.jobs

    /** Returns true on success. Errors are reported in the job state, not thrown. */
    suspend fun run(projectId: String): Boolean {
        jobs.setProcessing(projectId, ProcessingUi())
        val started = System.currentTimeMillis()
        return try {
            execute(projectId)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val message = when (e) {
                is NoAudioTrackException -> e.message
                is IOException -> e.message ?: "Something went wrong reading the video."
                is OutOfMemoryError -> "Your phone ran out of memory. Try the Lite or Fast speech model in Languages."
                else -> e.message ?: "Something went wrong."
            }
            jobs.updateProcessing(projectId) { ui ->
                ui.copy(
                    error = message,
                    steps = ui.steps.map { if (it.status == StepStatus.RUNNING) it.copy(status = StepStatus.ERROR) else it },
                )
            }
            container.projects.markFailed(projectId)
            container.telemetry.event(
                "subtitles_failed",
                "reason" to (e::class.simpleName ?: "unknown"),
                "seconds" to (System.currentTimeMillis() - started) / 1000,
            )
            if (e !is NoAudioTrackException) container.telemetry.recordError(e, "processing")
            false
        }
    }

    private suspend fun execute(projectId: String) {
        val repo = container.projects
        val initial = repo.get(projectId) ?: throw IOException("Project not found")
        val whisper = initial.isWhisper
        val languageLabel = if (whisper) {
            WhisperCatalog.language(initial.languageId).let { if (it.code == WhisperCatalog.AUTO) "Auto-detect" else it.name }
        } else {
            ModelCatalog.byId(initial.languageId).language
        }
        jobs.updateProcessing(projectId) {
            it.copy(projectName = initial.name, language = languageLabel, thumbnail = initial.thumbnail)
        }
        container.telemetry.breadcrumb("processing engine=${initial.engine} lang=${initial.languageId}")

        // 1. Copy the video into app storage and read its metadata.
        step(projectId, StepId.IMPORT, StepStatus.RUNNING, "Copying")
        val project = repo.importVideo(projectId) { p ->
            step(projectId, StepId.IMPORT, StepStatus.RUNNING, "Copying ${(p * 100).toInt()}%", p)
        }
        jobs.updateProcessing(projectId) { it.copy(thumbnail = project.thumbnail) }
        step(projectId, StepId.IMPORT, StepStatus.DONE, "${project.width}×${project.height}", 1f)

        // 2. Make sure the offline model is on the device.
        val modelId = if (whisper) project.engine else ModelCatalog.byId(project.languageId).id
        val modelName = if (whisper) "${WhisperCatalog.byId(modelId).name} model" else "${languageLabel} model"
        val sizeMb = if (whisper) WhisperCatalog.byId(modelId).sizeMb else ModelCatalog.byId(modelId).sizeMb
        if (!container.models.isInstalled(modelId)) {
            step(projectId, StepId.MODEL, StepStatus.RUNNING, "Downloading $sizeMb MB (one time)")
            coroutineScope {
                val watcher = launch {
                    container.models.states.collect { states ->
                        when (val s = states[modelId]) {
                            is ModelState.Downloading -> step(
                                projectId, StepId.MODEL, StepStatus.RUNNING,
                                "Downloading ${(s.progress * 100).toInt()}% of $sizeMb MB", s.progress,
                            )
                            ModelState.Unpacking -> step(projectId, StepId.MODEL, StepStatus.RUNNING, "Unpacking", 0.98f)
                            else -> Unit
                        }
                    }
                }
                try {
                    container.models.install(modelId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    throw IOException("Couldn't download the $modelName. Check your connection and retry.", e)
                } finally {
                    watcher.cancel()
                }
            }
        }
        step(projectId, StepId.MODEL, StepStatus.DONE, if (whisper) "$modelName · offline" else "$languageLabel · offline", 1f)

        // 3. Decode the sound track and run speech recognition.
        val video = project.videoFile ?: throw IOException("The video is missing")
        step(projectId, StepId.LISTEN, StepStatus.RUNNING, "0%")
        val listener = object : TranscriptionListener {
            override fun onProgress(progress: Float) {
                step(projectId, StepId.LISTEN, StepStatus.RUNNING, "${(progress * 100).toInt()}%", progress)
            }

            override fun onTranscript(confirmed: String, partial: String) {
                jobs.updateProcessing(projectId) { it.copy(transcript = confirmed, partial = partial) }
            }
        }
        val listenStarted = System.currentTimeMillis()
        val words: List<Word>
        val spoken: String
        if (whisper) {
            val model = WhisperCatalog.byId(modelId)
            val result = container.whisper.transcribe(video, container.models.modelDir(modelId), model, project.languageId, listener)
            words = result.words
            spoken = result.language
        } else {
            words = container.transcriber.transcribe(video, container.models.modelDir(modelId), listener)
            spoken = ModelCatalog.languageCode(modelId)
        }
        if (words.isEmpty()) {
            throw IOException("No speech was found. Check the video has people talking and that the language ($languageLabel) is right.")
        }
        repo.setSourceLanguage(projectId, spoken)
        val detected = if (whisper && project.languageId == WhisperCatalog.AUTO) " · ${WhisperCatalog.language(spoken).english}" else ""
        step(projectId, StepId.LISTEN, StepStatus.DONE, "${words.size} words$detected", 1f)

        // 4. Group words into timed captions.
        step(projectId, StepId.CAPTIONS, StepStatus.RUNNING, "", 0.5f)
        val captions = CaptionSegmenter.build(words, project.style.wordsPerCaption)
        repo.saveCaptions(projectId, captions, ProjectStatus.READY)
        step(projectId, StepId.CAPTIONS, StepStatus.DONE, "${captions.size} captions", 1f)
        jobs.updateProcessing(projectId) { it.copy(done = true, captionCount = captions.size, partial = "") }

        container.telemetry.event(
            "subtitles_created",
            "engine" to project.engine,
            "language" to spoken,
            "auto_language" to (whisper && project.languageId == WhisperCatalog.AUTO),
            "video_length" to Telemetry.durationBucket(project.durationMs),
            "words" to words.size,
            "speed_x" to project.durationMs.toFloat() / (System.currentTimeMillis() - listenStarted).coerceAtLeast(1),
        )
    }

    private fun step(projectId: String, id: StepId, status: StepStatus, detail: String, progress: Float = 0f) {
        jobs.updateProcessing(projectId) { ui ->
            ui.copy(steps = ui.steps.map { if (it.id == id) StepUi(id, status, detail, progress.coerceIn(0f, 1f)) else it })
        }
    }
}
