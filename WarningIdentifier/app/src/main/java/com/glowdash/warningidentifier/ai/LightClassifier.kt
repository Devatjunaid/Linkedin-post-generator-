package com.glowdash.warningidentifier.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.glowdash.warningidentifier.data.LightColor
import com.glowdash.warningidentifier.data.LightsCatalog
import com.glowdash.warningidentifier.data.WarningLight
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.min

/**
 * 100% on-device identification — free, unlimited and offline.
 *
 * 1. If `assets/model/warning_lights.tflite` + `labels.txt` are bundled, an image
 *    classifier (e.g. exported from Google Teachable Machine) names the symbol.
 * 2. [ColorAnalyzer] always runs as well, so even without a model the app knows
 *    which colours are lit and can offer the matching symbols to pick from.
 */
class LightClassifier private constructor(context: Context) {

    data class Prediction(val light: WarningLight, val confidence: Float)

    data class Analysis(
        val predictions: List<Prediction>,
        val colors: List<LightColor>,
        val usedModel: Boolean,
    ) {
        val best: Prediction? get() = predictions.firstOrNull()
        val isConfident: Boolean get() = (best?.confidence ?: 0f) >= CONFIDENT_THRESHOLD
    }

    private val interpreter: Interpreter?
    private val labels: List<WarningLight?>

    val hasModel: Boolean get() = interpreter != null

    init {
        var interp: Interpreter? = null
        var lbls: List<WarningLight?> = emptyList()
        try {
            val files = context.assets.list(MODEL_DIR).orEmpty()
            if (MODEL_FILE in files && LABELS_FILE in files) {
                interp = Interpreter(loadModel(context), Interpreter.Options().setNumThreads(2))
                lbls = context.assets.open("$MODEL_DIR/$LABELS_FILE").bufferedReader().readLines()
                    .filter { it.isNotBlank() }
                    .map { matchLabel(it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Model not loaded, using colour analysis only", e)
            interp?.close()
            interp = null
        }
        interpreter = interp
        labels = lbls
    }

    /** Full analysis for a captured photo. Safe to call from a background thread. */
    fun analyze(bitmap: Bitmap): Analysis {
        val hits = ColorAnalyzer.detect(bitmap)
        val colors = hits.map { it.color }
        val modelPredictions = runModel(bitmap)
        if (modelPredictions.isNotEmpty()) {
            return Analysis(modelPredictions, colors, usedModel = true)
        }
        return Analysis(emptyList(), colors, usedModel = false)
    }

    @Synchronized
    private fun runModel(bitmap: Bitmap): List<Prediction> {
        val interp = interpreter ?: return emptyList()
        return try {
            val inputTensor = interp.getInputTensor(0)
            val shape = inputTensor.shape() // [1, h, w, 3]
            val height = shape[1]
            val width = shape[2]
            val isFloat = inputTensor.dataType() == DataType.FLOAT32

            val square = centerCropSquare(bitmap)
            val scaled = Bitmap.createScaledBitmap(square, width, height, true)
            val pixels = IntArray(width * height)
            scaled.getPixels(pixels, 0, width, 0, 0, width, height)
            if (scaled !== square) scaled.recycle()
            if (square !== bitmap) square.recycle()

            val input = ByteBuffer.allocateDirect(width * height * 3 * if (isFloat) 4 else 1)
                .order(ByteOrder.nativeOrder())
            for (p in pixels) {
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                if (isFloat) {
                    // Teachable Machine / MobileNet normalisation: [-1, 1]
                    input.putFloat(r / 127.5f - 1f)
                    input.putFloat(g / 127.5f - 1f)
                    input.putFloat(b / 127.5f - 1f)
                } else {
                    input.put(r.toByte()); input.put(g.toByte()); input.put(b.toByte())
                }
            }
            input.rewind()

            val outTensor = interp.getOutputTensor(0)
            val count = outTensor.shape().last()
            val outFloat = outTensor.dataType() == DataType.FLOAT32
            val output = ByteBuffer.allocateDirect(count * if (outFloat) 4 else 1)
                .order(ByteOrder.nativeOrder())
            interp.run(input, output)
            output.rewind()

            val scores = FloatArray(count) {
                if (outFloat) output.float else (output.get().toInt() and 0xFF) / 255f
            }
            scores.indices
                .mapNotNull { i -> labels.getOrNull(i)?.let { Prediction(it, scores[i]) } }
                .sortedByDescending { it.confidence }
                .take(4)
        } catch (e: Exception) {
            Log.w(TAG, "Inference failed", e)
            emptyList()
        }
    }

    private fun centerCropSquare(src: Bitmap): Bitmap {
        val side = min(src.width, src.height)
        if (src.width == src.height) return src
        return Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
    }

    private fun loadModel(context: Context): MappedByteBuffer {
        context.assets.openFd("$MODEL_DIR/$MODEL_FILE").use { fd ->
            FileInputStream(fd.fileDescriptor).use { stream ->
                return stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
    }

    /** Maps a label like "0 Check Engine" or "check_engine" to a catalog entry. */
    private fun matchLabel(raw: String): WarningLight? {
        val cleaned = raw.trim().replace(Regex("^\\d+\\s+"), "")
        val key = normalize(cleaned)
        return LightsCatalog.find(key)
            ?: LightsCatalog.all.firstOrNull { normalize(it.name) == key }
            ?: run { Log.w(TAG, "Label '$raw' has no matching light in LightsCatalog"); null }
    }

    private fun normalize(s: String) = s.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    companion object {
        private const val TAG = "LightClassifier"
        private const val MODEL_DIR = "model"
        private const val MODEL_FILE = "warning_lights.tflite"
        private const val LABELS_FILE = "labels.txt"
        const val CONFIDENT_THRESHOLD = 0.55f

        @Volatile private var instance: LightClassifier? = null

        fun get(context: Context): LightClassifier =
            instance ?: synchronized(this) {
                instance ?: LightClassifier(context.applicationContext).also { instance = it }
            }
    }
}

/** Hands the latest analysis from the scanner to the result screen. */
object ScanSession {
    @Volatile var latest: LightClassifier.Analysis? = null
}
