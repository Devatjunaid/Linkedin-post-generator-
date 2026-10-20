package com.superdevlabs.autosubtitles.export

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.superdevlabs.autosubtitles.core.model.Caption
import com.superdevlabs.autosubtitles.core.model.CaptionStyle
import com.superdevlabs.autosubtitles.core.render.FontRegistry
import com.superdevlabs.autosubtitles.data.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

enum class ExportQuality(val label: String, val shortSide: Int?) {
    HD("720p", 720),
    FULL_HD("1080p", 1080),
    ORIGINAL("Original", null),
}

/**
 * Burns captions into the video with Media3 Transformer: hardware decode → (optional resize)
 * → caption overlay → H.264 encode. Audio is passed through untouched unless removed.
 */
@OptIn(UnstableApi::class)
class VideoExporter(private val context: Context, private val fonts: FontRegistry) {

    suspend fun export(
        project: Project,
        captions: List<Caption>,
        style: CaptionStyle,
        quality: ExportQuality,
        keepAudio: Boolean,
        onProgress: (Float) -> Unit,
    ): File {
        val source = project.videoFile?.takeIf { it.exists() } ?: throw IOException("The original video is missing")
        val output = withContext(Dispatchers.IO) {
            File(context.cacheDir, "exports").apply { mkdirs() }
                .listFiles()?.forEach { it.delete() }
            File(File(context.cacheDir, "exports"), safeFileName(project.name) + "_subtitled.mp4")
        }
        val (outWidth, outHeight) = targetSize(project.width, project.height, quality)

        val videoEffects = mutableListOf<Effect>()
        if (outWidth != project.width || outHeight != project.height) {
            videoEffects += Presentation.createForWidthAndHeight(outWidth, outHeight, Presentation.LAYOUT_SCALE_TO_FIT)
        }
        if (captions.isNotEmpty()) {
            val overlay = CaptionBitmapOverlay(fonts, captions, style, outWidth, outHeight)
            videoEffects += OverlayEffect(listOf<TextureOverlay>(overlay))
        }
        val editedItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(source)))
            .setRemoveAudio(!keepAudio)
            .setEffects(Effects(emptyList<AudioProcessor>(), videoEffects))
            .build()

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val handler = Handler(Looper.getMainLooper())
                val progressHolder = ProgressHolder()
                lateinit var transformer: Transformer
                val poll = object : Runnable {
                    override fun run() {
                        if (transformer.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                            onProgress(progressHolder.progress / 100f)
                        }
                        handler.postDelayed(this, 200)
                    }
                }
                transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            handler.removeCallbacks(poll)
                            onProgress(1f)
                            if (continuation.isActive) continuation.resume(output)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException,
                        ) {
                            handler.removeCallbacks(poll)
                            output.delete()
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    IOException("Export failed (${exportException.errorCodeName})", exportException),
                                )
                            }
                        }
                    })
                    .build()
                continuation.invokeOnCancellation {
                    handler.post {
                        handler.removeCallbacks(poll)
                        transformer.cancel()
                        output.delete()
                    }
                }
                transformer.start(editedItem, output.absolutePath)
                handler.post(poll)
            }
        }
    }

    companion object {
        /** Output size for [quality], keeping aspect ratio and even dimensions (encoder requirement). */
        fun targetSize(width: Int, height: Int, quality: ExportQuality): Pair<Int, Int> {
            val target = quality.shortSide
            val shortSide = minOf(width, height)
            if (target == null || shortSide <= target) return even(width) to even(height)
            val scale = target.toFloat() / shortSide
            return even((width * scale).roundToInt()) to even((height * scale).roundToInt())
        }

        private fun even(value: Int): Int = (value / 2 * 2).coerceAtLeast(2)

        fun safeFileName(name: String): String =
            name.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().replace(' ', '_').ifBlank { "subtitles" }.take(40)
    }
}
