package com.devatjunaid.isthisai.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class ImageTooLargeException : IOException("This file is larger than 100 MB")

/** Reads picked/shared images. Files are only read on the device; nothing is uploaded. */
object ImageSource {
    const val MAX_BYTES = 100L * 1024 * 1024

    data class Info(val name: String, val size: Long?, val mimeType: String?)

    fun info(context: Context, uri: Uri): Info {
        val cr = context.contentResolver
        var name: String? = null
        var size: Long? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            runCatching {
                cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val s = c.getColumnIndex(OpenableColumns.SIZE)
                        if (n >= 0) name = c.getString(n)
                        if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
                    }
                }
            }
        }
        return Info(name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "image", size, cr.getType(uri))
    }

    fun readBytes(context: Context, uri: Uri): ByteArray {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open this file")
        input.use { stream ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = stream.read(buf)
                if (n < 0) break
                total += n
                if (total > MAX_BYTES) throw ImageTooLargeException()
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }
    }

    /** Downsampled bitmap for on-screen previews, or null for formats the phone can't decode. */
    fun decodePreview(bytes: ByteArray, maxSide: Int): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(java.nio.ByteBuffer.wrap(bytes))
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > maxSide) {
                    val scale = maxSide.toFloat() / longest
                    decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull()

    fun dimensions(bytes: ByteArray): Pair<Int, Int>? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        return if (o.outWidth > 0 && o.outHeight > 0) o.outWidth to o.outHeight else null
    }

    fun savePreview(context: Context, bitmap: Bitmap): String? = runCatching {
        val dir = File(context.filesDir, "previews").apply { mkdirs() }
        val file = File(dir, "p_${System.currentTimeMillis()}_${(0..9999).random()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        file.absolutePath
    }.getOrNull()

    fun loadPreview(path: String?): Bitmap? = path?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
}
