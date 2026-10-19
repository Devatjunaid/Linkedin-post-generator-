package com.devatjunaid.isthisai.data

import android.content.Context
import android.graphics.Bitmap
import com.devatjunaid.isthisai.engine.ReportJson
import com.devatjunaid.isthisai.engine.ScanReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ScanRepository(private val context: Context, private val dao: ScanDao) {

    fun observeAll() = dao.observeAll()

    fun observeRecent(limit: Int) = dao.observeRecent(limit)

    suspend fun save(report: ScanReport, preview: Bitmap?): Long = withContext(Dispatchers.IO) {
        val path = preview?.let { ImageSource.savePreview(context, it) }
        dao.insert(
            ScanEntity(
                fileName = report.fileName,
                verdict = report.verdict.name,
                strength = report.strength.name,
                aiTool = report.aiTool,
                scannedAt = report.scannedAt,
                sizeBytes = report.sizeBytes,
                previewPath = path,
                reportJson = ReportJson.encode(report),
            )
        )
    }

    suspend fun load(id: Long): Pair<ScanEntity, ScanReport>? = withContext(Dispatchers.IO) {
        val e = dao.get(id) ?: return@withContext null
        val report = runCatching { ReportJson.decode(e.reportJson) }.getOrNull() ?: return@withContext null
        e to report
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dao.get(id)?.previewPath?.let { File(it).delete() }
        dao.delete(id)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dao.allPreviewPaths().forEach { File(it).delete() }
        dao.clear()
        File(context.filesDir, "previews").listFiles()?.forEach { it.delete() }
    }
}
