package com.inkformlabs.pdfsigner.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.inkformlabs.pdfsigner.data.db.SignatureDao
import com.inkformlabs.pdfsigner.data.db.SignatureEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/** Saved signatures and initials, kept as transparent PNGs in app-private storage. */
class SignatureRepository(context: Context, private val dao: SignatureDao) {

    private val dir = File(context.filesDir, "signatures").apply { mkdirs() }

    fun observeAll(): Flow<List<SignatureEntity>> = dao.observeAll()

    fun file(entity: SignatureEntity) = File(dir, entity.fileName)

    /** Crops the transparent margin away and stores the signature. */
    suspend fun save(bitmap: Bitmap, kind: String): SignatureEntity = withContext(Dispatchers.Default) {
        val cropped = cropToContent(bitmap)
        val id = UUID.randomUUID().toString()
        val entity = SignatureEntity(
            id = id,
            kind = kind,
            fileName = "$id.png",
            width = cropped.width,
            height = cropped.height,
            createdAt = System.currentTimeMillis(),
        )
        withContext(Dispatchers.IO) {
            File(dir, entity.fileName).outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
            dao.upsert(entity)
        }
        entity
    }

    suspend fun delete(entity: SignatureEntity) = withContext(Dispatchers.IO) {
        dao.delete(entity.id)
        File(dir, entity.fileName).delete()
    }

    suspend fun loadBitmap(id: String): Bitmap? = withContext(Dispatchers.IO) {
        val entity = dao.get(id) ?: return@withContext null
        val f = File(dir, entity.fileName)
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    }

    private fun cropToContent(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                if ((pixels[row + x] ushr 24) > 16) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) return source
        val pad = 6
        val left = max(0, minX - pad)
        val top = max(0, minY - pad)
        val right = min(w - 1, maxX + pad)
        val bottom = min(h - 1, maxY + pad)
        return Bitmap.createBitmap(source, left, top, right - left + 1, bottom - top + 1)
    }
}
