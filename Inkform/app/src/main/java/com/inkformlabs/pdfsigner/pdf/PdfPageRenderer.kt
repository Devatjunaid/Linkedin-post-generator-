package com.inkformlabs.pdfsigner.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/** Thread-safe wrapper around the platform [PdfRenderer] (which allows only one open page at a time). */
class PdfPageRenderer private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) {
    private val mutex = Mutex()
    private var closed = false

    val pageCount: Int get() = renderer.pageCount

    suspend fun render(index: Int, widthPx: Int, heightPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (closed || index !in 0 until renderer.pageCount) return@withLock null
            renderer.openPage(index).use { page ->
                val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }

    suspend fun close() {
        mutex.withLock {
            if (!closed) {
                closed = true
                runCatching { renderer.close() }
                runCatching { descriptor.close() }
            }
        }
    }

    companion object {
        fun open(file: File): PdfPageRenderer {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            return try {
                PdfPageRenderer(descriptor, PdfRenderer(descriptor))
            } catch (e: Exception) {
                descriptor.close()
                throw e
            }
        }

        /** Number of pages, or -1 if the platform renderer cannot open the file. */
        fun pageCount(file: File): Int = try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { it.pageCount }
            }
        } catch (e: Exception) {
            -1
        }

        /** Renders the first page at [widthPx] wide, or null on failure. */
        fun renderFirstPage(file: File, widthPx: Int): Bitmap? = try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    renderer.openPage(0).use { page ->
                        val height = (widthPx * page.height.toFloat() / page.width).roundToInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
