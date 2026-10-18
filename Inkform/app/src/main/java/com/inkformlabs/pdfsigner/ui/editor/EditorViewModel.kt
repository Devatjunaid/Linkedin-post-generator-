package com.inkformlabs.pdfsigner.ui.editor

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.inkformlabs.pdfsigner.AppContainer
import com.inkformlabs.pdfsigner.data.Profile
import com.inkformlabs.pdfsigner.pdf.FieldType
import com.inkformlabs.pdfsigner.pdf.FormField
import com.inkformlabs.pdfsigner.pdf.NormRect
import com.inkformlabs.pdfsigner.pdf.Overlay
import com.inkformlabs.pdfsigner.pdf.OverlayKind
import com.inkformlabs.pdfsigner.pdf.PageGeom
import com.inkformlabs.pdfsigner.pdf.PdfPageRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

enum class EditorTool { None, Text, Date, Check, Cross }

data class EditorState(
    val loading: Boolean = true,
    val error: String? = null,
    val title: String = "",
    val pages: List<PageGeom> = emptyList(),
    val fields: List<FormField> = emptyList(),
    val values: Map<String, String> = emptyMap(),
    val overlays: List<Overlay> = emptyList(),
    val activeField: String? = null,
    val selectedOverlay: Long? = null,
    val editingTextOverlay: Long? = null,
    val tool: EditorTool = EditorTool.None,
    val zoom: Float = 1f,
    val saving: Boolean = false,
    val dirty: Boolean = false,
    /** Changes whenever the working file is re-read, so page bitmaps are re-rendered. */
    val renderVersion: Int = 0,
    val message: String? = null,
) {
    val fillable: List<FormField> get() = fields.filter { it.type != FieldType.SIGNATURE }
    val filledCount: Int get() = fillable.count { values[it.name].orEmpty().isNotEmpty() }
    val activeFieldObject: FormField? get() = fields.firstOrNull { it.name == activeField }
}

class EditorViewModel(
    private val documentId: String,
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _signatureBitmaps = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val signatureBitmaps: StateFlow<Map<String, ImageBitmap>> = _signatureBitmaps.asStateFlow()

    private var renderer: PdfPageRenderer? = null
    private var initialValues: Map<String, String> = emptyMap()
    private var nextOverlayId = 1L

    /** When the user taps a signature field, the next signature goes into that rectangle. */
    private var signatureTarget: Pair<Int, NormRect>? = null

    private val pageCache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 6).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val doc = container.documents.get(documentId)
            if (doc == null) {
                _state.update { it.copy(loading = false, error = "This document no longer exists.") }
                return@launch
            }
            val file = container.documents.workingFile(documentId)
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val model = container.pdfEngine.readDocument(file)
                    model to PdfPageRenderer.open(file)
                }
            }
            loaded.onSuccess { (model, newRenderer) ->
                renderer?.close()
                renderer = newRenderer
                pageCache.evictAll()
                initialValues = model.fields.associate { it.name to it.value }
                _state.update {
                    it.copy(
                        loading = false,
                        title = doc.name,
                        pages = model.pages,
                        fields = model.fields,
                        values = initialValues,
                        overlays = emptyList(),
                        activeField = null,
                        selectedOverlay = null,
                        editingTextOverlay = null,
                        tool = EditorTool.None,
                        dirty = false,
                        saving = false,
                        renderVersion = it.renderVersion + 1,
                    )
                }
            }.onFailure {
                _state.update { s -> s.copy(loading = false, error = "This PDF couldn't be opened.") }
            }
        }
    }

    // ---------------------------------------------------------------- rendering

    suspend fun renderPage(index: Int, widthPx: Int): Bitmap? {
        val geom = _state.value.pages.getOrNull(index) ?: return null
        val version = _state.value.renderVersion
        val key = "$version:$index:$widthPx"
        pageCache.get(key)?.let { return it }
        val heightPx = (widthPx * geom.displayH / geom.displayW).roundToInt()
        val bitmap = try {
            renderer?.render(index, widthPx, heightPx)
        } catch (e: Exception) {
            null
        } ?: return null
        pageCache.put(key, bitmap)
        return bitmap
    }

    // ---------------------------------------------------------------- form fields

    fun activateField(name: String?) {
        _state.update { it.copy(activeField = name, selectedOverlay = null, tool = EditorTool.None) }
    }

    fun setValue(name: String, value: String) {
        _state.update { it.copy(values = it.values + (name to value), dirty = true) }
    }

    fun toggleCheckbox(field: FormField) {
        val current = _state.value.values[field.name].orEmpty()
        val on = field.widgets.firstNotNullOfOrNull { it.onValue } ?: "Yes"
        setValue(field.name, if (current.isEmpty()) on else "")
        activateField(field.name)
    }

    fun selectRadio(field: FormField, onValue: String?) {
        if (onValue == null) return
        setValue(field.name, onValue)
        activateField(field.name)
    }

    /** Moves to the next (or previous) fillable field in reading order. */
    fun stepField(forward: Boolean) {
        val s = _state.value
        val list = s.fillable
        if (list.isEmpty()) return
        val current = list.indexOfFirst { it.name == s.activeField }
        val next = when {
            current < 0 -> 0
            forward -> current + 1
            else -> current - 1
        }
        activateField(list.getOrNull(next)?.name)
    }

    /** Jumps to the first empty field after the current one (wrapping around). */
    fun nextEmptyField(): Boolean {
        val s = _state.value
        val list = s.fillable
        if (list.isEmpty()) return false
        val start = list.indexOfFirst { it.name == s.activeField }.coerceAtLeast(-1)
        for (i in 1..list.size) {
            val f = list[(start + i).mod(list.size)]
            if (s.values[f.name].isNullOrEmpty()) {
                activateField(f.name)
                return true
            }
        }
        return false
    }

    fun autoFill(profile: Profile): Int {
        var filled = 0
        val s = _state.value
        val updates = mutableMapOf<String, String>()
        for (f in s.fields) {
            if (f.type != FieldType.TEXT || s.values[f.name].orEmpty().isNotEmpty()) continue
            val value = AutoFill.valueFor(f.name, f.label, profile) ?: continue
            updates[f.name] = if (f.maxLength > 0) value.take(f.maxLength) else value
            filled++
        }
        if (updates.isNotEmpty()) _state.update { it.copy(values = it.values + updates, dirty = true) }
        return filled
    }

    // ---------------------------------------------------------------- placed elements

    fun setTool(tool: EditorTool) {
        _state.update {
            it.copy(tool = if (it.tool == tool) EditorTool.None else tool, activeField = null, selectedOverlay = null)
        }
    }

    fun placeAt(page: Int, u: Float, v: Float) {
        val s = _state.value
        val geom = s.pages.getOrNull(page) ?: return
        val tool = s.tool
        if (tool == EditorTool.None) {
            _state.update { it.copy(selectedOverlay = null, activeField = null) }
            return
        }
        val id = nextOverlayId++
        val overlay = when (tool) {
            EditorTool.Text, EditorTool.Date -> {
                val fontPt = 12f
                val h = fontPt / Overlay.TEXT_SIZE_RATIO / geom.displayH
                val text = if (tool == EditorTool.Date) AutoFill.today() else ""
                val w = (text.length.coerceAtLeast(6) * fontPt * 0.55f) / geom.displayW
                Overlay(
                    id = id, kind = if (tool == EditorTool.Date) OverlayKind.DATE else OverlayKind.TEXT, page = page,
                    x = u.coerceIn(0f, 1f - w), y = (v - h / 2f).coerceIn(0f, 1f - h), w = w, h = h, text = text,
                )
            }

            EditorTool.Check, EditorTool.Cross -> {
                val sizePt = 14f
                val w = sizePt / geom.displayW
                val h = sizePt / geom.displayH
                Overlay(
                    id = id, kind = if (tool == EditorTool.Check) OverlayKind.CHECK else OverlayKind.CROSS, page = page,
                    x = (u - w / 2f).coerceIn(0f, 1f - w), y = (v - h / 2f).coerceIn(0f, 1f - h), w = w, h = h,
                )
            }

            EditorTool.None -> return
        }
        _state.update {
            it.copy(
                overlays = it.overlays + overlay,
                selectedOverlay = id,
                editingTextOverlay = if (overlay.kind == OverlayKind.TEXT) id else null,
                tool = EditorTool.None,
                dirty = true,
            )
        }
    }

    fun selectOverlay(id: Long?) {
        _state.update { it.copy(selectedOverlay = id, activeField = null, tool = EditorTool.None) }
    }

    fun startEditingText(id: Long) {
        _state.update { it.copy(editingTextOverlay = id, selectedOverlay = id) }
    }

    fun finishEditingText(id: Long, text: String?) {
        _state.update { s ->
            val overlay = s.overlays.firstOrNull { it.id == id }
            val updated = when {
                overlay == null -> s.overlays
                text == null && overlay.text.isBlank() -> s.overlays.filterNot { it.id == id }
                text == null -> s.overlays
                text.isBlank() -> s.overlays.filterNot { it.id == id }
                else -> s.overlays.map { if (it.id == id) it.copy(text = text.trim()) else it }
            }
            s.copy(overlays = updated, editingTextOverlay = null, dirty = true)
        }
    }

    fun moveOverlay(id: Long, du: Float, dv: Float) {
        _state.update { s ->
            s.copy(
                overlays = s.overlays.map {
                    if (it.id != id) it else it.copy(
                        x = (it.x + du).coerceIn(0f, (1f - it.w).coerceAtLeast(0f)),
                        y = (it.y + dv).coerceIn(0f, (1f - it.h).coerceAtLeast(0f)),
                    )
                },
                dirty = true,
            )
        }
    }

    /** Scales the element around its top-left corner, keeping its aspect ratio. */
    fun resizeOverlay(id: Long, factor: Float) {
        _state.update { s ->
            s.copy(
                overlays = s.overlays.map {
                    if (it.id != id) it else {
                        val f = factor.coerceIn(0.5f, 2f)
                        val newH = (it.h * f).coerceIn(0.008f, 0.5f)
                        val k = newH / it.h
                        val newW = (it.w * k).coerceAtMost(1f - it.x)
                        it.copy(w = newW, h = newH)
                    }
                },
                dirty = true,
            )
        }
    }

    /** Text boxes size themselves to their text; the UI reports the measured width. */
    fun reportOverlayWidth(id: Long, w: Float) {
        _state.update { s ->
            s.copy(overlays = s.overlays.map { if (it.id == id && kotlin.math.abs(it.w - w) > 0.001f) it.copy(w = w) else it })
        }
    }

    fun setOverlayColor(id: Long, color: Int) {
        _state.update { s -> s.copy(overlays = s.overlays.map { if (it.id == id) it.copy(color = color) else it }, dirty = true) }
    }

    fun deleteOverlay(id: Long) {
        _state.update { s ->
            s.copy(overlays = s.overlays.filterNot { it.id == id }, selectedOverlay = null, editingTextOverlay = null, dirty = true)
        }
    }

    fun targetSignatureField(field: FormField) {
        val widget = field.firstWidget
        signatureTarget = widget.page to widget.rect
        activateField(null)
    }

    fun clearSignatureTarget() {
        signatureTarget = null
    }

    /** Places a saved signature: into the tapped signature field, or centred low on [visiblePage]. */
    fun placeSignature(signatureId: String, visiblePage: Int) {
        viewModelScope.launch {
            val bitmap = container.signatures.loadBitmap(signatureId)
            if (bitmap == null) {
                _state.update { it.copy(message = "That signature could not be loaded.") }
                return@launch
            }
            _signatureBitmaps.update { it + (signatureId to bitmap.asImageBitmap()) }
            val s = _state.value
            val target = signatureTarget
            signatureTarget = null
            val page = target?.first ?: visiblePage.coerceIn(0, (s.pages.size - 1).coerceAtLeast(0))
            val geom = s.pages.getOrNull(page) ?: return@launch
            val aspect = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)

            val (x, y, w, h) = if (target != null) {
                val rect = target.second
                val boxWpt = rect.width * geom.displayW
                val boxHpt = rect.height * geom.displayH
                var wPt = boxWpt
                var hPt = wPt / aspect
                if (hPt > boxHpt) {
                    hPt = boxHpt
                    wPt = hPt * aspect
                }
                val w = wPt / geom.displayW
                val h = hPt / geom.displayH
                listOf(rect.left + (rect.width - w) / 2f, rect.top + (rect.height - h) / 2f, w, h)
            } else {
                val w = 0.36f
                val h = (w * geom.displayW / aspect) / geom.displayH
                listOf((1f - w) / 2f, (0.72f - h / 2f).coerceIn(0f, 1f - h), w, h)
            }
            val id = nextOverlayId++
            _state.update {
                it.copy(
                    overlays = it.overlays + Overlay(id, OverlayKind.SIGNATURE, page, x, y, w, h, signatureId = signatureId),
                    selectedOverlay = id,
                    activeField = null,
                    tool = EditorTool.None,
                    dirty = true,
                )
            }
        }
    }

    // ---------------------------------------------------------------- misc

    fun zoom(delta: Float) {
        _state.update { it.copy(zoom = (it.zoom + delta).coerceIn(1f, 2.5f)) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    fun showMessage(text: String) {
        _state.update { it.copy(message = text) }
    }

    fun revertToOriginal() {
        viewModelScope.launch {
            renderer?.close()
            renderer = null
            container.documents.revert(documentId)
            load()
            _state.update { it.copy(message = "Restored the original file") }
        }
    }

    /** Writes field values and placed elements into the working PDF. [onSaved] runs on success. */
    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (s.saving || s.loading) return
        val changed = s.values.filter { (name, value) -> initialValues[name].orEmpty() != value }
        if (changed.isEmpty() && s.overlays.isEmpty()) {
            onSaved()
            return
        }
        _state.update { it.copy(saving = true, activeField = null, selectedOverlay = null, editingTextOverlay = null) }
        viewModelScope.launch {
            val bitmaps = s.overlays.mapNotNull { it.signatureId }.toSet()
                .associateWith { container.signatures.loadBitmap(it) }
            val working = container.documents.workingFile(documentId)
            val temp = File(working.parentFile, "working.saving.pdf")
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    container.pdfEngine.applyEdits(working, temp, s.pages, changed, s.overlays) { id -> bitmaps[id] }
                }
            }
            if (result.isFailure) {
                temp.delete()
                _state.update { it.copy(saving = false, message = "Couldn't save: ${result.exceptionOrNull()?.message ?: "unknown error"}") }
                return@launch
            }
            renderer?.close()
            renderer = null
            withContext(Dispatchers.IO) {
                if (!temp.renameTo(working)) {
                    temp.copyTo(working, overwrite = true)
                    temp.delete()
                }
            }
            val signedNow = s.overlays.any { it.kind == OverlayKind.SIGNATURE }
            container.documents.afterSave(documentId, s.fillable.size, s.filledCount, signedNow)
            load()
            onSaved()
        }
    }

    override fun onCleared() {
        val r = renderer
        renderer = null
        if (r != null) runBlocking { r.close() }
        pageCache.evictAll()
    }

    companion object {
        fun factory(documentId: String, container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { EditorViewModel(documentId, container) }
        }
    }
}
