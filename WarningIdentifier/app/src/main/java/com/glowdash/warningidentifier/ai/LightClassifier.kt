package com.glowdash.warningidentifier.ai

import android.content.Context
import android.graphics.Bitmap
import com.glowdash.warningidentifier.data.LightColor
import com.glowdash.warningidentifier.data.WarningLight

/**
 * 100% on-device identification — free, unlimited and offline.
 *
 * [ColorAnalyzer] finds which warning colours are lit; the result screen then
 * offers the matching symbols so the user can confirm the exact one.
 *
 * The bundled TensorFlow Lite runtime was removed because its native library is
 * not 16 KB page-size aligned, which Google Play rejects. To add an image model
 * later, use a 16 KB-compatible runtime and return its results as [Prediction]s.
 */
class LightClassifier private constructor() {

    data class Prediction(val light: WarningLight, val confidence: Float)

    data class Analysis(
        val predictions: List<Prediction>,
        val colors: List<LightColor>,
        val usedModel: Boolean,
    ) {
        val best: Prediction? get() = predictions.firstOrNull()
        val isConfident: Boolean get() = (best?.confidence ?: 0f) >= CONFIDENT_THRESHOLD
    }

    /** Full analysis for a captured photo. Safe to call from a background thread. */
    fun analyze(bitmap: Bitmap): Analysis {
        val colors = ColorAnalyzer.detect(bitmap).map { it.color }
        return Analysis(emptyList(), colors, usedModel = false)
    }

    companion object {
        const val CONFIDENT_THRESHOLD = 0.55f

        private val instance by lazy { LightClassifier() }

        @Suppress("UNUSED_PARAMETER")
        fun get(context: Context): LightClassifier = instance
    }
}

/** Hands the latest analysis from the scanner to the result screen. */
object ScanSession {
    @Volatile var latest: LightClassifier.Analysis? = null
}
