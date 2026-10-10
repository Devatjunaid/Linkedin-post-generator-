package com.harrycodess.drawingkids.drawing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/** Everything a kid has done on one page: strokes, stamps and bucket fills, with undo/redo. */
class CanvasDocument(initialFills: Map<String, Color> = emptyMap()) {

    private sealed interface Action {
        class AddMark(val mark: Mark) : Action
        class Fill(val group: String, val before: Color?, val after: Color) : Action
        class ClearAll(val marks: List<Mark>, val fills: Map<String, Color>) : Action
    }

    val marks = mutableListOf<Mark>()
    val fills = mutableStateMapOf<String, Color>().apply { putAll(initialFills) }

    /** Bumped when marks are appended; the stroke layer draws only the new ones. */
    var appendVersion by mutableIntStateOf(0)
        private set

    /** Bumped when marks are removed or reordered; the stroke layer redraws everything. */
    var resetVersion by mutableIntStateOf(0)
        private set

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    /** For free drawing: the page size (in dp) captured the first time the canvas is laid out. */
    var pageSize: Size? = null

    private val undoStack = ArrayDeque<Action>()
    private val redoStack = ArrayDeque<Action>()

    val isEmpty get() = marks.isEmpty() && fills.isEmpty()

    fun add(mark: Mark) {
        marks += mark
        appendVersion++
        push(Action.AddMark(mark))
    }

    fun fill(group: String, color: Color) {
        val before = fills[group]
        if (before == color) return
        fills[group] = color
        push(Action.Fill(group, before, color))
    }

    fun clear() {
        if (isEmpty) return
        push(Action.ClearAll(marks.toList(), fills.toMap()))
        marks.clear()
        fills.clear()
        resetVersion++
    }

    fun undo() {
        val action = undoStack.removeLastOrNull() ?: return
        when (action) {
            is Action.AddMark -> marks.remove(action.mark)
            is Action.Fill -> if (action.before == null) fills.remove(action.group) else fills[action.group] = action.before
            is Action.ClearAll -> {
                marks.addAll(action.marks)
                fills.putAll(action.fills)
            }
        }
        if (action !is Action.Fill) resetVersion++
        redoStack.addLast(action)
        refresh()
    }

    fun redo() {
        val action = redoStack.removeLastOrNull() ?: return
        when (action) {
            is Action.AddMark -> {
                marks += action.mark
                appendVersion++
            }
            is Action.Fill -> fills[action.group] = action.after
            is Action.ClearAll -> {
                marks.clear()
                fills.clear()
                resetVersion++
            }
        }
        undoStack.addLast(action)
        refresh()
    }

    private fun push(action: Action) {
        undoStack.addLast(action)
        if (undoStack.size > 200) undoStack.removeFirst()
        redoStack.clear()
        refresh()
    }

    private fun refresh() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }
}
