package com.glowdash.warningidentifier.ai

import android.graphics.Bitmap
import android.graphics.Color
import com.glowdash.warningidentifier.data.LightColor

/**
 * Finds which warning-light colours are glowing in a photo of a dashboard.
 *
 * Dashboard symbols are small, bright and saturated against a dark cluster, so
 * counting bright saturated pixels per hue band is a cheap, fully offline signal.
 * It is used on its own when no trained model is bundled, and to back up the model.
 */
object ColorAnalyzer {

    private const val SAMPLE_WIDTH = 160
    private const val MIN_SHARE = 0.0025f

    data class ColorHit(val color: LightColor, val share: Float)

    fun detect(source: Bitmap): List<ColorHit> {
        val scale = SAMPLE_WIDTH.toFloat() / source.width
        val w = SAMPLE_WIDTH.coerceAtMost(source.width)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        val small = if (source.width > SAMPLE_WIDTH) Bitmap.createScaledBitmap(source, w, h, true) else source

        val pixels = IntArray(small.width * small.height)
        small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
        if (small !== source) small.recycle()

        val counts = IntArray(LightColor.entries.size)
        val hsv = FloatArray(3)
        for (p in pixels) {
            Color.colorToHSV(p, hsv)
            if (hsv[2] < 0.55f || hsv[1] < 0.45f) continue
            val bucket = when (hsv[0]) {
                in 0f..18f, in 330f..360f -> LightColor.RED
                in 18f..62f -> LightColor.AMBER
                in 75f..170f -> LightColor.GREEN
                in 185f..260f -> LightColor.BLUE
                else -> null
            } ?: continue
            counts[bucket.ordinal]++
        }

        val total = pixels.size.toFloat()
        return LightColor.entries
            .map { ColorHit(it, counts[it.ordinal] / total) }
            .filter { it.share >= MIN_SHARE }
            .sortedByDescending { it.share }
    }
}
