package com.inkformlabs.pdfsigner.ui.signature

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.inkformlabs.pdfsigner.AppContainer
import com.inkformlabs.pdfsigner.data.db.SignatureEntity
import com.inkformlabs.pdfsigner.ui.theme.Paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

enum class PadTab { Draw, Type, Image }

data class InkStroke(val points: List<Offset>, val color: Color, val widthDp: Float)

class SignaturePadViewModel(private val container: AppContainer, initials: Boolean) : ViewModel() {

    val strokes = mutableStateListOf<InkStroke>()
    val currentPoints = mutableStateListOf<Offset>()

    var tab by mutableStateOf(PadTab.Draw)
    var isInitials by mutableStateOf(initials)
    var penColor by mutableStateOf(Paper.inkBlue)
    var penWidthDp by mutableStateOf(3.4f)

    var typedText by mutableStateOf("")
    var typeStyle by mutableStateOf(0)

    private var importedSource: Bitmap? = null
    var processedImage by mutableStateOf<ImageBitmap?>(null)
        private set
    private var processedBitmap: Bitmap? = null
    var threshold by mutableStateOf(0.62f)
    var isProcessing by mutableStateOf(false)
        private set
    private var processJob: Job? = null

    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    val canSave: Boolean
        get() = when (tab) {
            PadTab.Draw -> strokes.isNotEmpty()
            PadTab.Type -> typedText.isNotBlank()
            PadTab.Image -> processedBitmap != null
        }

    // ------------------------------------------------------------ drawing

    fun startStroke(point: Offset) {
        currentPoints.clear()
        currentPoints += point
    }

    fun extendStroke(point: Offset) {
        val last = currentPoints.lastOrNull()
        if (last == null || (last - point).getDistance() > 1.2f) currentPoints += point
    }

    fun endStroke() {
        if (currentPoints.isNotEmpty()) strokes += InkStroke(currentPoints.toList(), penColor, penWidthDp)
        currentPoints.clear()
    }

    fun undo() {
        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
    }

    fun clear() {
        strokes.clear()
        currentPoints.clear()
    }

    // ------------------------------------------------------------ image import

    fun importImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            isProcessing = true
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1400) sample *= 2
                    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                }.getOrNull()
            }
            isProcessing = false
            if (bitmap == null) {
                error = "Couldn't read that image."
                return@launch
            }
            importedSource = bitmap
            reprocess()
        }
    }

    /** Removes the paper background: light pixels become transparent, ink is recoloured to the pen colour. */
    fun reprocess() {
        val source = importedSource ?: return
        val cutoff = threshold
        val ink = penColor.toArgb() and 0x00FFFFFF
        processJob?.cancel()
        processJob = viewModelScope.launch {
            isProcessing = true
            val result = withContext(Dispatchers.Default) {
                val w = source.width
                val h = source.height
                val pixels = IntArray(w * h)
                source.getPixels(pixels, 0, w, 0, 0, w, h)
                for (i in pixels.indices) {
                    val c = pixels[i]
                    val r = (c shr 16) and 0xFF
                    val g = (c shr 8) and 0xFF
                    val b = c and 0xFF
                    val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                    pixels[i] = if (lum >= cutoff) {
                        0
                    } else {
                        val alpha = (((cutoff - lum) / cutoff) * 255f * 1.8f).toInt().coerceIn(0, 255)
                        (alpha shl 24) or ink
                    }
                }
                Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
            }
            processedBitmap = result
            processedImage = result.asImageBitmap()
            isProcessing = false
        }
    }

    // ------------------------------------------------------------ saving

    fun save(canvasWidthPx: Int, canvasHeightPx: Int, density: Float, onSaved: (SignatureEntity) -> Unit) {
        if (saving || !canSave) return
        saving = true
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                when (tab) {
                    PadTab.Draw -> renderStrokes(canvasWidthPx, canvasHeightPx, density)
                    PadTab.Type -> renderTyped()
                    PadTab.Image -> processedBitmap
                }
            }
            if (bitmap == null) {
                saving = false
                error = "Nothing to save yet."
                return@launch
            }
            val entity = container.signatures.save(bitmap, if (isInitials) "initials" else "signature")
            saving = false
            onSaved(entity)
        }
    }

    private fun renderStrokes(width: Int, height: Int, density: Float): Bitmap? {
        if (width <= 0 || height <= 0 || strokes.isEmpty()) return null
        val scale = max(1f, 1000f / width)
        val bitmap = Bitmap.createBitmap((width * scale).toInt(), (height * scale).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (stroke in strokes.toList()) {
            paint.color = stroke.color.toArgb()
            paint.strokeWidth = stroke.widthDp * density
            if (stroke.points.size == 1) {
                dot.color = paint.color
                canvas.drawCircle(stroke.points[0].x, stroke.points[0].y, paint.strokeWidth / 2f, dot)
                continue
            }
            val path = android.graphics.Path()
            val pts = stroke.points
            path.moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val prev = pts[i - 1]
                val mid = Offset((prev.x + pts[i].x) / 2f, (prev.y + pts[i].y) / 2f)
                path.quadTo(prev.x, prev.y, mid.x, mid.y)
            }
            path.lineTo(pts.last().x, pts.last().y)
            canvas.drawPath(path, paint)
        }
        return bitmap
    }

    private fun renderTyped(): Bitmap? {
        val text = typedText.trim()
        if (text.isEmpty()) return null
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = typefaceFor(typeStyle)
            textSize = 180f
            color = penColor.toArgb()
        }
        val width = paint.measureText(text).toInt() + 60
        val metrics = paint.fontMetrics
        val height = (metrics.descent - metrics.ascent).toInt() + 60
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawText(text, 30f, 30f - metrics.ascent, paint)
        return bitmap
    }

    companion object {
        val TypeStyleNames = listOf("Script", "Classic", "Casual")

        fun typefaceFor(style: Int): Typeface = when (style) {
            1 -> Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            2 -> Typeface.create("casual", Typeface.NORMAL)
            else -> Typeface.create("cursive", Typeface.NORMAL)
        }

        fun factory(container: AppContainer, initials: Boolean): ViewModelProvider.Factory = viewModelFactory {
            initializer { SignaturePadViewModel(container, initials) }
        }
    }
}
