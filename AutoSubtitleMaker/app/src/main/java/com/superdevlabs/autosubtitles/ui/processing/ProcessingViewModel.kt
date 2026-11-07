package com.superdevlabs.autosubtitles.ui.processing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superdevlabs.autosubtitles.AppContainer
import com.superdevlabs.autosubtitles.work.ProcessingUi
import com.superdevlabs.autosubtitles.work.StepId
import com.superdevlabs.autosubtitles.work.StepStatus
import com.superdevlabs.autosubtitles.work.StepUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Shows the progress of the background subtitle job for one project. The work itself runs
 * in [com.superdevlabs.autosubtitles.work.ProcessingWorker], so it keeps going when the user
 * leaves this screen, switches apps or turns the screen off.
 */
class ProcessingViewModel(private val container: AppContainer, private val projectId: String) : ViewModel() {

    val ui: StateFlow<ProcessingUi> = container.jobs.processing(projectId)
        .map { it ?: ProcessingUi() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, container.jobs.processingNow(projectId) ?: ProcessingUi())

    init {
        val current = container.jobs.processingNow(projectId)
        if (current?.error != null) {
            container.work.startProcessing(projectId, restart = true)
        } else if (current == null) {
            viewModelScope.launch {
                val project = container.projects.get(projectId)
                if (project?.isReady == true) {
                    // Finished earlier (e.g. while the app was closed): just show the result.
                    container.jobs.setProcessing(
                        projectId,
                        ProcessingUi(
                            projectName = project.name,
                            done = true,
                            captionCount = project.captions.size,
                            steps = StepId.entries.map { StepUi(it, StepStatus.DONE, progress = 1f) },
                        ),
                    )
                } else {
                    // Start, or re-attach to a job that is already running (KEEP leaves it alone).
                    container.work.startProcessing(projectId)
                }
            }
        }
    }

    fun retry() = container.work.startProcessing(projectId, restart = true)

    /** Stops the job; the project stays in the list as a draft. */
    fun cancel() {
        container.work.cancelProcessing(projectId)
        container.jobs.clearProcessing(projectId)
    }
}
