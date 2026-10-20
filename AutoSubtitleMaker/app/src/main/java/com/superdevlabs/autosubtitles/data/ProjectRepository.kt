package com.superdevlabs.autosubtitles.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.graphics.scale
import com.superdevlabs.autosubtitles.core.model.Caption
import com.superdevlabs.autosubtitles.core.model.CaptionStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Project(
    val id: String,
    val name: String,
    val videoFile: File?,
    val thumbnail: File?,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val languageId: String,
    val status: String,
    val captions: List<Caption>,
    val style: CaptionStyle,
    val updatedAt: Long,
) {
    val isReady: Boolean get() = status == ProjectStatus.READY
}

class ProjectRepository(private val context: Context, private val dao: ProjectDao) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val captionListSerializer = ListSerializer(Caption.serializer())
    private val projectsDir = File(context.filesDir, "projects").apply { mkdirs() }

    fun observeProjects(): Flow<List<Project>> = dao.observeAll().map { list -> list.map { it.toProject() } }

    fun observe(id: String): Flow<Project?> = dao.observe(id).map { it?.toProject() }

    suspend fun get(id: String): Project? = dao.get(id)?.toProject()

    /** Creates the project row right after the user picks a video. Import happens later. */
    suspend fun create(uri: Uri, languageId: String, style: CaptionStyle): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val name = withContext(Dispatchers.IO) { displayName(uri) }
            ?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: ("Clip " + SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(now)))
        dao.upsert(
            ProjectEntity(
                id = id, name = name.take(60), sourceUri = uri.toString(), videoPath = null, thumbnailPath = null,
                durationMs = 0, width = 0, height = 0, languageId = languageId, status = ProjectStatus.NEW,
                captionsJson = "[]", styleJson = json.encodeToString(CaptionStyle.serializer(), style), createdAt = now, updatedAt = now,
            ),
        )
        return id
    }

    /**
     * Copies the picked video into app storage and reads its size, length and a thumbnail.
     * [onProgress] reports 0..1 while copying.
     */
    suspend fun importVideo(id: String, onProgress: (Float) -> Unit): Project = withContext(Dispatchers.IO) {
        val entity = dao.get(id) ?: throw IOException("Project not found")
        val existing = entity.videoPath?.let(::File)
        if (existing != null && existing.exists() && entity.durationMs > 0) {
            onProgress(1f)
            return@withContext entity.toProject()
        }
        val dir = File(projectsDir, id).apply { mkdirs() }
        val uri = Uri.parse(entity.sourceUri)
        val resolver = context.contentResolver
        val total = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L
        val video = File(dir, "source.mp4")
        resolver.openInputStream(uri)?.use { input ->
            video.outputStream().use { output ->
                val buffer = ByteArray(256 * 1024)
                var copied = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    output.write(buffer, 0, n)
                    copied += n
                    if (total > 0) onProgress((copied.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        } ?: throw IOException("Can't open the selected video")
        onProgress(1f)

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(video.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (duration <= 0 || rawWidth <= 0 || rawHeight <= 0) throw IOException("This file isn't a playable video")
            val swap = rotation % 180 != 0
            val width = if (swap) rawHeight else rawWidth
            val height = if (swap) rawWidth else rawHeight

            val thumb = File(dir, "thumb.jpg")
            retriever.getFrameAtTime(minOf(1_000_000L, duration * 500L), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.let { frame ->
                    val targetWidth = 360
                    val scaled = if (frame.width > targetWidth) {
                        frame.scale(targetWidth, (frame.height * targetWidth / frame.width).coerceAtLeast(1))
                    } else {
                        frame
                    }
                    thumb.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 82, it) }
                }

            val updated = entity.copy(
                videoPath = video.absolutePath,
                thumbnailPath = thumb.takeIf { it.exists() }?.absolutePath,
                durationMs = duration, width = width, height = height,
                updatedAt = System.currentTimeMillis(),
            )
            dao.upsert(updated)
            updated.toProject()
        } finally {
            retriever.release()
        }
    }

    suspend fun saveCaptions(id: String, captions: List<Caption>, status: String = ProjectStatus.READY) {
        val entity = dao.get(id) ?: return
        dao.upsert(
            entity.copy(
                captionsJson = json.encodeToString(captionListSerializer, captions),
                status = status,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun saveEdits(id: String, captions: List<Caption>, style: CaptionStyle) {
        val entity = dao.get(id) ?: return
        dao.upsert(
            entity.copy(
                captionsJson = json.encodeToString(captionListSerializer, captions),
                styleJson = json.encodeToString(CaptionStyle.serializer(), style),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun rename(id: String, name: String) {
        val entity = dao.get(id) ?: return
        val clean = name.trim().take(60)
        if (clean.isEmpty()) return
        dao.upsert(entity.copy(name = clean, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setLanguage(id: String, languageId: String) {
        val entity = dao.get(id) ?: return
        dao.upsert(entity.copy(languageId = languageId))
    }

    suspend fun markFailed(id: String) {
        val entity = dao.get(id) ?: return
        dao.upsert(entity.copy(status = ProjectStatus.FAILED, updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(id: String) {
        dao.delete(id)
        withContext(Dispatchers.IO) { File(projectsDir, id).deleteRecursively() }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    private fun ProjectEntity.toProject(): Project = Project(
        id = id,
        name = name,
        videoFile = videoPath?.let(::File),
        thumbnail = thumbnailPath?.let(::File)?.takeIf { it.exists() },
        durationMs = durationMs,
        width = width,
        height = height,
        languageId = languageId,
        status = status,
        captions = runCatching { json.decodeFromString(captionListSerializer, captionsJson) }.getOrDefault(emptyList()),
        style = runCatching { json.decodeFromString(CaptionStyle.serializer(), styleJson) }.getOrDefault(CaptionStyle()),
        updatedAt = updatedAt,
    )
}
