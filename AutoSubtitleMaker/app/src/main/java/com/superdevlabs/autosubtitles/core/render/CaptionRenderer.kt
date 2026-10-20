package com.superdevlabs.autosubtitles.core.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import com.superdevlabs.autosubtitles.core.captions.SubtitleFormatter
import com.superdevlabs.autosubtitles.core.model.BackgroundMode
import com.superdevlabs.autosubtitles.core.model.Caption
import com.superdevlabs.autosubtitles.core.model.CaptionAnimation
import com.superdevlabs.autosubtitles.core.model.CaptionStyle
import com.superdevlabs.autosubtitles.core.model.HighlightMode
import com.superdevlabs.autosubtitles.core.model.ShadowMode
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws one frame of styled, animated captions onto an [android.graphics.Canvas].
 *
 * The same renderer is used by the editor preview (Compose canvas) and by the video export
 * (bitmap overlay), so what the user sees is exactly what gets burned into the video.
 * Not thread-safe: give each thread its own instance.
 */
class CaptionRenderer(private val fonts: FontRegistry) {

    private val fill = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val stroke = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val box = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val metrics = Paint.FontMetrics()

    /** The caption visible at [timeMs], or null. [captions] must be sorted by start time. */
    fun activeCaption(captions: List<Caption>, timeMs: Long): Caption? {
        var lo = 0
        var hi = captions.lastIndex
        var found = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (captions[mid].startMs <= timeMs) {
                found = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        val caption = captions.getOrNull(found) ?: return null
        return if (timeMs < caption.endMs) caption else null
    }

    fun draw(canvas: Canvas, width: Int, height: Int, timeMs: Long, captions: List<Caption>, style: CaptionStyle) {
        val caption = activeCaption(captions, timeMs) ?: return
        drawCaption(canvas, width, height, timeMs, caption, style)
    }

    fun drawCaption(canvas: Canvas, width: Int, height: Int, timeMs: Long, caption: Caption, style: CaptionStyle) {
        if (width <= 0 || height <= 0 || caption.words.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        val size = (style.textSize * min(w, h)).coerceAtLeast(6f)

        val typeface = fonts.typeface(style.fontId)
        for (paint in arrayOf(fill, stroke)) {
            paint.typeface = typeface
            paint.textSize = size
            paint.letterSpacing = style.letterSpacing
        }
        stroke.strokeWidth = size * style.outlineWidth

        val words = caption.words.map { SubtitleFormatter.applyCase(it.text, style.textCase) }
        val widths = FloatArray(words.size) { fill.measureText(words[it]) }
        val space = fill.measureText(" ")

        // Greedy line wrapping inside 86% of the frame width.
        val maxLineWidth = w * 0.86f
        val lines = mutableListOf<IntRange>()
        val lineWidths = mutableListOf<Float>()
        var lineStart = 0
        var lineWidth = 0f
        for (i in words.indices) {
            val candidate = if (i == lineStart) widths[i] else lineWidth + space + widths[i]
            if (i > lineStart && candidate > maxLineWidth) {
                lines += lineStart until i
                lineWidths += lineWidth
                lineStart = i
                lineWidth = widths[i]
            } else {
                lineWidth = candidate
            }
        }
        lines += lineStart until words.size
        lineWidths += lineWidth

        fill.getFontMetrics(metrics)
        val glyphHeight = metrics.descent - metrics.ascent
        val lineHeight = glyphHeight * 1.06f
        val blockHeight = lineHeight * lines.size
        val margin = h * 0.04f
        val minCenter = margin + blockHeight / 2f
        val maxCenter = h - margin - blockHeight / 2f
        val centerY = if (minCenter < maxCenter) (style.positionY * h).coerceIn(minCenter, maxCenter) else h / 2f
        val top = centerY - blockHeight / 2f
        val widest = lineWidths.maxOrNull() ?: 0f
        val fitScale = if (widest > maxLineWidth) maxLineWidth / widest else 1f

        // Whole-caption entrance animation.
        val elapsed = (timeMs - caption.startMs).coerceAtLeast(0L).toFloat()
        var alpha = 1f
        var scale = fitScale
        var offsetY = 0f
        when (style.animation) {
            CaptionAnimation.POP -> {
                scale *= 0.72f + 0.28f * easeOutBack((elapsed / 240f).coerceIn(0f, 1f))
                alpha = (elapsed / 110f).coerceIn(0f, 1f)
            }
            CaptionAnimation.SLIDE_UP -> {
                val p = (elapsed / 280f).coerceIn(0f, 1f)
                offsetY = (1f - easeOutCubic(p)) * size * 0.6f
                alpha = p
            }
            CaptionAnimation.FADE -> alpha = (elapsed / 260f).coerceIn(0f, 1f)
            else -> Unit
        }
        if (alpha <= 0.01f) return

        val activeIndex = caption.words.indexOfLast { it.startMs <= timeMs }.coerceAtLeast(0)
        val padX = size * 0.32f
        val padY = size * 0.12f

        canvas.save()
        if (scale != 1f) canvas.scale(scale, scale, w / 2f, centerY)
        if (offsetY != 0f) canvas.translate(0f, offsetY)

        if (style.backgroundMode == BackgroundMode.BLOCK) {
            rect.set(w / 2f - widest / 2f - padX, top - padY, w / 2f + widest / 2f + padX, top + blockHeight + padY)
            box.color = withAlpha(style.backgroundColor, style.backgroundOpacity * alpha)
            canvas.drawRoundRect(rect, size * 0.28f, size * 0.28f, box)
        }

        lines.forEachIndexed { lineIndex, range ->
            val lineW = lineWidths[lineIndex]
            val baseline = top + lineIndex * lineHeight + (lineHeight - glyphHeight) / 2f - metrics.ascent
            var x = (w - lineW) / 2f

            if (style.backgroundMode == BackgroundMode.LINE_BOX) {
                rect.set(x - padX, baseline + metrics.ascent - padY, x + lineW + padX, baseline + metrics.descent + padY)
                box.color = withAlpha(style.backgroundColor, style.backgroundOpacity * alpha)
                canvas.drawRoundRect(rect, size * 0.22f, size * 0.22f, box)
            }

            for (i in range) {
                val word = caption.words[i]
                val wordWidth = widths[i]
                val wordElapsed = (timeMs - word.startMs).toFloat()
                var wordAlpha = alpha
                var wordDy = 0f
                var wordScale = 1f

                when (style.animation) {
                    CaptionAnimation.TYPEWRITER -> if (i > 0) {
                        wordAlpha *= (wordElapsed / 90f).coerceIn(0f, 1f)
                    }
                    CaptionAnimation.BOUNCE -> if (wordElapsed in 0f..300f) {
                        wordDy = -sin(PI.toFloat() * wordElapsed / 300f) * size * 0.28f
                    }
                    CaptionAnimation.POP -> if (i == activeIndex) {
                        wordScale = 1f + 0.1f * (1f - ((wordElapsed / 220f).coerceIn(0f, 1f) - 1f).pow(2))
                    }
                    CaptionAnimation.KARAOKE -> if (i > activeIndex) wordAlpha *= 0.5f
                    else -> Unit
                }

                if (wordAlpha > 0.01f) {
                    val isActive = i == activeIndex && style.highlightMode != HighlightMode.NONE
                    var color = style.textColor
                    if (isActive && style.highlightMode == HighlightMode.BOX) {
                        rect.set(
                            x - size * 0.12f, baseline + metrics.ascent - size * 0.02f,
                            x + wordWidth + size * 0.08f, baseline + metrics.descent + size * 0.02f,
                        )
                        rect.offset(0f, wordDy)
                        box.color = withAlpha(style.highlightColor, wordAlpha)
                        canvas.drawRoundRect(rect, size * 0.18f, size * 0.18f, box)
                        color = contrastOn(style.highlightColor)
                    } else if (isActive) {
                        color = style.highlightColor
                    }
                    drawWord(canvas, words[i], x, baseline, wordDy, wordScale, wordWidth, size, color, wordAlpha, style, isActive)
                }
                x += wordWidth + space
            }
        }
        canvas.restore()
    }

    private fun drawWord(
        canvas: Canvas,
        text: String,
        x: Float,
        baseline: Float,
        dy: Float,
        scale: Float,
        width: Float,
        size: Float,
        color: Int,
        alpha: Float,
        style: CaptionStyle,
        boxed: Boolean,
    ) {
        canvas.save()
        if (dy != 0f) canvas.translate(0f, dy)
        if (scale != 1f) canvas.scale(scale, scale, x + width / 2f, baseline - size * 0.35f)

        val hasOutline = style.outlineWidth > 0.001f && !boxed
        val shadowPaint = if (hasOutline) stroke else fill
        fill.clearShadowLayer()
        stroke.clearShadowLayer()
        when (style.shadowMode) {
            ShadowMode.NONE -> Unit
            ShadowMode.DROP -> shadowPaint.setShadowLayer(size * 0.09f, 0f, size * 0.05f, withAlpha(Color.BLACK, 0.7f * alpha))
            ShadowMode.GLOW -> shadowPaint.setShadowLayer(size * 0.38f, 0f, 0f, withAlpha(style.highlightColor, alpha))
        }
        if (hasOutline) {
            stroke.color = withAlpha(style.outlineColor, alpha)
            canvas.drawText(text, x, baseline, stroke)
        }
        fill.color = withAlpha(color, alpha)
        canvas.drawText(text, x, baseline, fill)
        canvas.restore()
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (Color.alpha(color) * alpha.coerceIn(0f, 1f)).roundToInt()
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    private fun contrastOn(color: Int): Int {
        val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
        return if (luminance > 0.55) 0xFF0E0E12.toInt() else Color.WHITE
    }

    private fun easeOutCubic(t: Float): Float = 1f - (1f - t).pow(3)

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        return 1f + c3 * (t - 1f).pow(3) + c1 * (t - 1f).pow(2)
    }
}
