package com.superdevlabs.autosubtitles.ui.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.superdevlabs.autosubtitles.AppContainer
import com.superdevlabs.autosubtitles.core.captions.CaptionEditing
import com.superdevlabs.autosubtitles.core.captions.CaptionSegmenter
import com.superdevlabs.autosubtitles.core.captions.StylePresets
import com.superdevlabs.autosubtitles.core.model.Caption
import com.superdevlabs.autosubtitles.core.model.CaptionStyle
import com.superdevlabs.autosubtitles.data.Project
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorUi(
    val loading: Boolean = true,
    val project: Project? = null,
    val captions: List<Caption> = emptyList(),
    val style: CaptionStyle = CaptionStyle(),
    val selectedId: Long? = null,
    val canUndo: Boolean = false,
    val message: String? = null,
)

class EditorViewModel(private val container: AppContainer, private val projectId: String) : ViewModel() {

    private val _ui = MutableStateFlow(EditorUi())
    val ui: StateFlow<EditorUi> = _ui.asStateFlow()

    private val undoStack = ArrayDeque<Pair<List<Caption>, CaptionStyle>>()
    private var saveJob: Job? = null
    private var lastCommitAt = 0L

    init {
        viewModelScope.launch {
            val project = container.projects.get(projectId)
            _ui.value = EditorUi(
                loading = false,
                project = project,
                captions = project?.captions.orEmpty(),
                style = project?.style ?: CaptionStyle(),
            )
        }
    }

    fun select(id: Long?) = _ui.update { it.copy(selectedId = id) }

    fun updateStyle(transform: (CaptionStyle) -> CaptionStyle) = commit(style = transform(_ui.value.style))

    /** Snapshot for undo at the start of a continuous gesture (dragging captions on the video). */
    fun beginGesture() {
        val ui = _ui.value
        undoStack.addLast(ui.captions to ui.style)
        while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        _ui.update { it.copy(canUndo = true) }
    }

    /** Style change during a gesture: no undo entry per frame, saved when the gesture settles. */
    fun updateStyleLive(transform: (CaptionStyle) -> CaptionStyle) {
        _ui.update { it.copy(style = transform(it.style)) }
        scheduleSave()
    }

    fun applyPreset(id: String) {
        val preset = StylePresets.byId(id)
        commit(style = StylePresets.apply(preset, _ui.value.style))
        viewModelScope.launch { container.settings.setPreset(id) }
    }

    /** Re-groups every word into captions of [count] words. */
    fun setWordsPerCaption(count: Int) {
        val ui = _ui.value
        val words = CaptionSegmenter.flatten(ui.captions)
        commit(
            captions = CaptionSegmenter.build(words, count),
            style = ui.style.copy(wordsPerCaption = count),
            clearSelection = true,
        )
    }

    fun editCaption(id: Long, text: String) = commit(captions = CaptionEditing.editText(_ui.value.captions, id, text))

    fun deleteCaption(id: Long) = commit(captions = CaptionEditing.delete(_ui.value.captions, id), clearSelection = true)

    fun nudge(id: Long, deltaStartMs: Long, deltaEndMs: Long) =
        commit(captions = CaptionEditing.retime(_ui.value.captions, id, deltaStartMs, deltaEndMs))

    fun undo() {
        lastCommitAt = 0L
        val (captions, style) = undoStack.removeLastOrNull() ?: return
        _ui.update { it.copy(captions = captions, style = style, canUndo = undoStack.isNotEmpty()) }
        scheduleSave()
    }

    fun rename(name: String) {
        viewModelScope.launch {
            container.projects.rename(projectId, name)
            val project = container.projects.get(projectId)
            _ui.update { it.copy(project = project) }
        }
    }

    fun importFont(uri: Uri) {
        viewModelScope.launch {
            val font = container.fonts.import(uri)
            if (font == null) {
                _ui.update { it.copy(message = "That file isn't a .ttf or .otf font.") }
            } else {
                updateStyle { it.copy(fontId = font.id) }
                _ui.update { it.copy(message = "Added ${font.name}") }
            }
        }
    }

    fun consumeMessage() = _ui.update { it.copy(message = null) }

    /** Writes any pending edit immediately (called when leaving the editor). */
    fun flush() {
        saveJob?.cancel()
        val ui = _ui.value
        viewModelScope.launch {
            withContext(NonCancellable) { container.projects.saveEdits(projectId, ui.captions, ui.style) }
        }
    }

    private fun commit(
        captions: List<Caption> = _ui.value.captions,
        style: CaptionStyle = _ui.value.style,
        clearSelection: Boolean = false,
    ) {
        val before = _ui.value
        if (captions == before.captions && style == before.style) return
        // Rapid changes (slider drags) collapse into a single undo step.
        val now = System.currentTimeMillis()
        if (now - lastCommitAt > UNDO_COALESCE_MS || undoStack.isEmpty()) {
            undoStack.addLast(before.captions to before.style)
            while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        }
        lastCommitAt = now
        _ui.update {
            it.copy(
                captions = captions,
                style = style,
                canUndo = true,
                selectedId = if (clearSelection) null else it.selectedId,
            )
        }
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            val ui = _ui.value
            container.projects.saveEdits(projectId, ui.captions, ui.style)
        }
    }

    override fun onCleared() {
        val ui = _ui.value
        if (!ui.loading) {
            container.appScope.launch { container.projects.saveEdits(projectId, ui.captions, ui.style) }
        }
    }

    private companion object {
        const val MAX_UNDO = 40
        const val UNDO_COALESCE_MS = 600L
    }
}
