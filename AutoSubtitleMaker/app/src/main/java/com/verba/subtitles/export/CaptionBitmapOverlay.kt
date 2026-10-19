package com.verba.subtitles.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.annotation.OptIn
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import com.verba.subtitles.core.model.Caption
import com.verba.subtitles.core.model.CaptionStyle
import com.verba.subtitles.core.render.CaptionRenderer
import com.verba.subtitles.core.render.FontRegistry

/**
 * Media3 overlay that paints the captions for each video frame with [CaptionRenderer].
 *
 * A frame-sized bitmap is redrawn only while something is animating or the highlighted word
 * changes; otherwise the previous bitmap is returned unchanged so it isn't re-uploaded to the
 * GPU. Two bitmaps are alternated because the overlay re-uploads only when the instance changes.
 */
@OptIn(UnstableApi::class)
class CaptionBitmapOverlay(
    fonts: FontRegistry,
    private val captions: List<Caption>,
    private val style: CaptionStyle,
    fallbackWidth: Int,
    fallbackHeight: Int,
) : BitmapOverlay() {

    private val renderer = CaptionRenderer(fonts)
    private val canvas = Canvas()
    private var width = fallbackWidth.coerceAtLeast(2)
    private var height = fallbackHeight.coerceAtLeast(2)
    private var buffers: Array<Bitmap>? = null
    private var empty: Bitmap? = null
    private var index = 0
    private var lastKey: Any? = null

    override fun configure(videoSize: Size) {
        super.configure(videoSize)
        if (videoSize.width > 0 && videoSize.height > 0 &&
            (videoSize.width != width || videoSize.height != height)
        ) {
            width = videoSize.width
            height = videoSize.height
            buffers = null
            lastKey = null
        }
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timeMs = presentationTimeUs / 1_000
        val caption = renderer.activeCaption(captions, timeMs) ?: return emptyBitmap()

        val activeWord = caption.words.indexOfLast { it.startMs <= timeMs }.coerceAtLeast(0)
        val wordStart = caption.words.getOrNull(activeWord)?.startMs ?: caption.startMs
        val animating = timeMs - caption.startMs < CAPTION_ANIMATION_MS || timeMs - wordStart < WORD_ANIMATION_MS
        val key = Triple(caption.id, activeWord, if (animating) timeMs else -1L)
        val current = buffers?.get(index)
        if (key == lastKey && current != null) return current

        val pool = buffers ?: Array(2) { Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) }.also { buffers = it }
        index = index xor 1
        val bitmap = pool[index]
        bitmap.eraseColor(Color.TRANSPARENT)
        canvas.setBitmap(bitmap)
        renderer.drawCaption(canvas, width, height, timeMs, caption, style)
        canvas.setBitmap(null)
        lastKey = key
        return bitmap
    }

    private fun emptyBitmap(): Bitmap {
        lastKey = null
        return empty ?: Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(Color.TRANSPARENT)
            empty = it
        }
    }

    private companion object {
        const val CAPTION_ANIMATION_MS = 420L
        const val WORD_ANIMATION_MS = 340L
    }
}
