package com.superdevlabs.autosubtitles.work

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.File

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
    /** Weighted overall progress for the big ring and the notification. */
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

    val running: Boolean get() = !done && error == null
}

enum class ExportPhase { IDLE, RUNNING, DONE, ERROR }

data class ExportJob(
    val phase: ExportPhase = ExportPhase.IDLE,
    val progress: Float = 0f,
    val videoFile: File? = null,
    val error: String? = null,
)

/**
 * Live progress of background jobs, keyed by project id. Workers write, screens read.
 * Lives in the app process, so it survives leaving a screen but not process death
 * (WorkManager then restarts the job and it fills this in again).
 */
class JobState {
    private val _processing = MutableStateFlow<Map<String, ProcessingUi>>(emptyMap())
    private val _exports = MutableStateFlow<Map<String, ExportJob>>(emptyMap())

    val processingAll: StateFlow<Map<String, ProcessingUi>> = _processing.asStateFlow()

    fun processing(projectId: String): Flow<ProcessingUi?> = _processing.map { it[projectId] }.distinctUntilChanged()
    fun export(projectId: String): Flow<ExportJob?> = _exports.map { it[projectId] }.distinctUntilChanged()

    fun processingNow(projectId: String): ProcessingUi? = _processing.value[projectId]
    fun exportNow(projectId: String): ExportJob? = _exports.value[projectId]

    fun updateProcessing(projectId: String, transform: (ProcessingUi) -> ProcessingUi) {
        _processing.update { all -> all + (projectId to transform(all[projectId] ?: ProcessingUi())) }
    }

    fun setProcessing(projectId: String, ui: ProcessingUi) = _processing.update { it + (projectId to ui) }

    fun clearProcessing(projectId: String) = _processing.update { it - projectId }

    fun updateExport(projectId: String, transform: (ExportJob) -> ExportJob) {
        _exports.update { all -> all + (projectId to transform(all[projectId] ?: ExportJob())) }
    }

    fun clearExport(projectId: String) = _exports.update { it - projectId }
}
