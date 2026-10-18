package com.inkformlabs.pdfsigner.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import com.inkformlabs.pdfsigner.data.db.DocumentDao
import com.inkformlabs.pdfsigner.data.db.DocumentEntity
import com.inkformlabs.pdfsigner.pdf.FieldType
import com.inkformlabs.pdfsigner.pdf.ImagesToPdf
import com.inkformlabs.pdfsigner.pdf.PdfEngine
import com.inkformlabs.pdfsigner.pdf.PdfPageRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Stores every document in app-private storage:
 *   files/docs/<id>/original.pdf  – the file as imported (used by "Revert")
 *   files/docs/<id>/working.pdf   – the file being filled and signed
 *   files/docs/<id>/thumb.jpg     – first-page preview
 */
class DocumentRepository(
    private val context: Context,
    private val dao: DocumentDao,
    private val engine: PdfEngine,
) {
    private val root = File(context.filesDir, "docs")

    sealed interface ImportResult {
        data class Success(val id: String) : ImportResult
        data object NeedsPassword : ImportResult
        data class Failed(val message: String) : ImportResult
    }

    fun observeAll(): Flow<List<DocumentEntity>> = dao.observeAll()
    fun observe(id: String): Flow<DocumentEntity?> = dao.observe(id)
    suspend fun get(id: String): DocumentEntity? = dao.get(id)

    fun directory(id: String) = File(root, id)
    fun originalFile(id: String) = File(directory(id), "original.pdf")
    fun workingFile(id: String) = File(directory(id), "working.pdf")
    fun thumbnailFile(id: String) = File(directory(id), "thumb.jpg")

    suspend fun importPdf(uri: Uri, password: String?): ImportResult = withContext(Dispatchers.IO) {
        val name = displayName(uri) ?: "Document"
        val temp = File(context.cacheDir, "import_${UUID.randomUUID()}.pdf")
        try {
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { input.copyTo(it) }
                true
            } ?: false
            if (!copied) {
                ImportResult.Failed("Couldn't read this file.")
            } else {
                importFileInternal(temp, name, password)
            }
        } catch (e: SecurityException) {
            ImportResult.Failed("Access to this file was lost. Please choose it again.")
        } catch (e: Exception) {
            ImportResult.Failed(e.message ?: "Couldn't import this file.")
        } finally {
            temp.delete()
        }
    }

    /** Converts photos (gallery or camera) into a new PDF document. */
    suspend fun createFromImages(images: List<Uri>, title: String): ImportResult = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "images_${UUID.randomUUID()}.pdf")
        try {
            val pages = ImagesToPdf.build(context, images, temp)
            if (pages == 0) {
                ImportResult.Failed("None of the selected images could be read.")
            } else {
                importFileInternal(temp, title, null)
            }
        } catch (e: OutOfMemoryError) {
            ImportResult.Failed("Not enough memory. Try fewer images at a time.")
        } catch (e: Exception) {
            ImportResult.Failed(e.message ?: "Couldn't create the PDF.")
        } finally {
            temp.delete()
        }
    }

    private suspend fun importFileInternal(source: File, rawName: String, password: String?): ImportResult {
        val id = UUID.randomUUID().toString()
        val dir = directory(id).apply { mkdirs() }
        return when (val prepared = engine.prepareImported(source, password, originalFile(id))) {
            PdfEngine.PrepareResult.NeedsPassword -> {
                dir.deleteRecursively()
                ImportResult.NeedsPassword
            }

            is PdfEngine.PrepareResult.Failed -> {
                dir.deleteRecursively()
                ImportResult.Failed(prepared.message)
            }

            PdfEngine.PrepareResult.Ok -> {
                val pages = PdfPageRenderer.pageCount(originalFile(id))
                if (pages <= 0) {
                    dir.deleteRecursively()
                    return ImportResult.Failed("This PDF can't be displayed on this device.")
                }
                originalFile(id).copyTo(workingFile(id), overwrite = true)
                val (total, filled) = countFields(workingFile(id))
                writeThumbnail(id)
                val now = System.currentTimeMillis()
                dao.upsert(
                    DocumentEntity(
                        id = id,
                        name = cleanName(rawName),
                        pageCount = pages,
                        sizeBytes = workingFile(id).length(),
                        fieldCount = total,
                        filledCount = filled,
                        signed = false,
                        exported = false,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
                ImportResult.Success(id)
            }
        }
    }

    private fun countFields(file: File): Pair<Int, Int> {
        val fields = runCatching { engine.readDocument(file).fields }.getOrDefault(emptyList())
            .filter { it.type != FieldType.SIGNATURE }
        return fields.size to fields.count { it.value.isNotEmpty() }
    }

    suspend fun afterSave(id: String, fieldCount: Int, filledCount: Int, signedNow: Boolean) = withContext(Dispatchers.IO) {
        val current = dao.get(id) ?: return@withContext
        writeThumbnail(id)
        dao.upsert(
            current.copy(
                fieldCount = fieldCount,
                filledCount = filledCount,
                signed = current.signed || signedNow,
                sizeBytes = workingFile(id).length(),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun revert(id: String) = withContext(Dispatchers.IO) {
        val current = dao.get(id) ?: return@withContext
        originalFile(id).copyTo(workingFile(id), overwrite = true)
        val (total, filled) = countFields(workingFile(id))
        writeThumbnail(id)
        dao.upsert(
            current.copy(
                fieldCount = total, filledCount = filled, signed = false, exported = false,
                sizeBytes = workingFile(id).length(), updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun rename(id: String, newName: String) = withContext(Dispatchers.IO) {
        val current = dao.get(id) ?: return@withContext
        val name = cleanName(newName)
        if (name.isNotBlank()) dao.upsert(current.copy(name = name, updatedAt = System.currentTimeMillis()))
    }

    suspend fun markExported(id: String) = withContext(Dispatchers.IO) {
        val current = dao.get(id) ?: return@withContext
        dao.upsert(current.copy(exported = true, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        dao.delete(id)
        directory(id).deleteRecursively()
    }

    /** Copy of the working file with a friendly name, inside cache/share (exposed via FileProvider). */
    suspend fun shareableCopy(id: String): File? = withContext(Dispatchers.IO) {
        val doc = dao.get(id) ?: return@withContext null
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, safeFileName(doc.name) + ".pdf")
        workingFile(id).copyTo(out, overwrite = true)
        out
    }

    private fun writeThumbnail(id: String) {
        val bitmap = PdfPageRenderer.renderFirstPage(workingFile(id), 320) ?: return
        try {
            thumbnailFile(id).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        } finally {
            bitmap.recycle()
        }
    }

    private fun displayName(uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment
    } catch (e: Exception) {
        uri.lastPathSegment
    }

    companion object {
        fun cleanName(raw: String): String =
            raw.substringAfterLast('/').removeSuffix(".pdf").removeSuffix(".PDF").trim().take(120).ifBlank { "Document" }

        fun safeFileName(name: String): String =
            name.replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), "_").trim().take(100).ifBlank { "Document" }
    }
}
