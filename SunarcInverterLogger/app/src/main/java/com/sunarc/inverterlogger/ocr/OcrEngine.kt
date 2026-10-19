package com.sunarc.inverterlogger.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sunarc.inverterlogger.domain.ImageOps
import com.sunarc.inverterlogger.domain.OcrBox
import com.sunarc.inverterlogger.domain.OcrLine
import com.sunarc.inverterlogger.domain.ParseContext
import com.sunarc.inverterlogger.domain.ParseResult
import com.sunarc.inverterlogger.domain.ParsedValue
import com.sunarc.inverterlogger.domain.ReadingParser
import com.sunarc.inverterlogger.domain.Field
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * On-device text recognition with Google ML Kit (bundled Latin model: free, no API key,
 * works offline). Each photo is read several times with different clean-up passes and the
 * most confident value for each field is kept.
 */
class OcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val parser = ReadingParser()

    suspend fun lines(bitmap: Bitmap, rotationDegrees: Int = 0): List<OcrLine> {
        val text = recognizer.process(InputImage.fromBitmap(bitmap, rotationDegrees)).await()
        return text.textBlocks.flatMap { block ->
            block.lines.map { line ->
                OcrLine(line.text, line.boundingBox?.let { OcrBox(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat()) })
            }
        }
    }

    /** Quick single pass, used for the live preview while the camera is pointed at the display. */
    suspend fun quickParse(bitmap: Bitmap, rotationDegrees: Int, ctx: ParseContext): ParseResult =
        parser.parse(lines(bitmap, rotationDegrees), ctx)

    /** Full multi-pass read of a captured photo or a screenshot. */
    suspend fun read(bitmap: Bitmap, ctx: ParseContext, extraFullImage: Bitmap? = null): ParseResult {
        val source = withContext(Dispatchers.Default) { scaleDown(bitmap, 1600) }
        val results = mutableListOf<ParseResult>()
        for (pass in ImageOps.lcdPasses) {
            val img = if (pass == ImageOps.Pass.ORIGINAL) source else withContext(Dispatchers.Default) { processed(source, pass) }
            val result = parser.parse(lines(img), ctx)
            results += result
            // A clean read of a screenshot or a sharp display needs no extra passes.
            if (pass == ImageOps.Pass.ORIGINAL && result.values.size >= 3 && result.values.values.all { it.confidence >= 0.8 }) break
        }
        if (extraFullImage != null && results.maxOf { it.values.size } < 2) {
            results += parser.parse(lines(withContext(Dispatchers.Default) { scaleDown(extraFullImage, 1600) }), ctx)
        }
        return merge(results)
    }

    private fun merge(results: List<ParseResult>): ParseResult {
        if (results.isEmpty()) return ParseResult.EMPTY
        val best = results.maxBy { it.score }
        val merged = LinkedHashMap<Field, ParsedValue>()
        for (r in results) for ((field, value) in r.values) {
            val old = merged[field]
            if (old == null || value.confidence > old.confidence) merged[field] = value
        }
        return ParseResult(
            values = merged,
            faultCode = best.faultCode ?: results.firstNotNullOfOrNull { it.faultCode },
            detectedBrand = best.detectedBrand ?: results.firstNotNullOfOrNull { it.detectedBrand },
            rawText = best.rawText
        )
    }

    private fun processed(src: Bitmap, pass: ImageOps.Pass): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = ImageOps.apply(pass, pixels, w, h)
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    fun close() = recognizer.close()

    companion object {
        fun scaleDown(src: Bitmap, maxSide: Int): Bitmap {
            val side = max(src.width, src.height)
            if (side <= maxSide) return src
            val f = maxSide.toFloat() / side
            return Bitmap.createScaledBitmap(src, (src.width * f).toInt(), (src.height * f).toInt(), true)
        }

        fun rotate(src: Bitmap, degrees: Int): Bitmap {
            if (degrees % 360 == 0) return src
            val m = Matrix().apply { postRotate(degrees.toFloat()) }
            return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        }

        /** Loads a gallery image with sampling so large photos do not run out of memory. */
        fun decode(context: Context, uri: Uri, maxSide: Int = 2000): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }

        /**
         * Cuts the captured photo to the on-screen guide frame (plus a margin), given that the
         * preview shows the camera image centre-cropped to fill the view.
         */
        fun cropToGuide(photo: Bitmap, viewW: Float, viewH: Float, guide: android.graphics.RectF, margin: Float = 0.12f): Bitmap {
            if (viewW <= 0 || viewH <= 0) return photo
            val scale = max(viewW / photo.width, viewH / photo.height)
            val shownW = viewW / scale
            val shownH = viewH / scale
            val offX = (photo.width - shownW) / 2
            val offY = (photo.height - shownH) / 2
            val mx = guide.width() * margin
            val my = guide.height() * margin
            val left = (offX + (guide.left - mx) / scale).toInt().coerceIn(0, photo.width - 1)
            val top = (offY + (guide.top - my) / scale).toInt().coerceIn(0, photo.height - 1)
            val right = (offX + (guide.right + mx) / scale).toInt().coerceIn(left + 1, photo.width)
            val bottom = (offY + (guide.bottom + my) / scale).toInt().coerceIn(top + 1, photo.height)
            return Bitmap.createBitmap(photo, left, top, right - left, bottom - top)
        }
    }
}
