package com.inkformlabs.pdfsigner.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** Builds a PDF from photos or camera captures: one image per page, auto-rotated and fitted to A4. */
object ImagesToPdf {

    private const val A4_W = 595
    private const val A4_H = 842
    private const val MARGIN = 18f
    private const val MAX_EDGE = 1800

    /** @return number of pages written. */
    fun build(context: Context, images: List<Uri>, output: File): Int {
        val pdf = PdfDocument()
        var pageNumber = 0
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        try {
            for (uri in images) {
                val bitmap = decode(context, uri) ?: continue
                val landscape = bitmap.width > bitmap.height
                val pageW = if (landscape) A4_H else A4_W
                val pageH = if (landscape) A4_W else A4_H
                pageNumber++
                val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create()
                val page = pdf.startPage(info)
                val scale = min((pageW - 2 * MARGIN) / bitmap.width, (pageH - 2 * MARGIN) / bitmap.height)
                val drawW = bitmap.width * scale
                val drawH = bitmap.height * scale
                val matrix = Matrix().apply {
                    postScale(scale, scale)
                    postTranslate((pageW - drawW) / 2f, (pageH - drawH) / 2f)
                }
                page.canvas.drawBitmap(bitmap, matrix, paint)
                pdf.finishPage(page)
            }
            if (pageNumber > 0) {
                output.outputStream().use { pdf.writeTo(it) }
            }
        } finally {
            pdf.close()
        }
        return pageNumber
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

        val orientation = try {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation == 0f) return decoded
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true)
        if (rotated != decoded) decoded.recycle()
        return rotated
    }
}
