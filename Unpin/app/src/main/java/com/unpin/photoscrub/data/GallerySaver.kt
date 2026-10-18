package com.unpin.photoscrub.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** Writes clean copies into Pictures/Unpin. Originals are never modified. */
object GallerySaver {

    const val FOLDER = "Unpin"

    fun save(context: Context, source: File, displayName: String, mime: String): Uri {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$FOLDER")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values) ?: error("Couldn't create the clean copy")
            try {
                resolver.openOutputStream(uri, "w")?.use { out -> source.inputStream().use { it.copyTo(out) } }
                    ?: error("Couldn't write the clean copy")
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            } catch (t: Throwable) {
                resolver.delete(uri, null, null)
                throw t
            }
            uri
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER)
            if (!dir.exists() && !dir.mkdirs()) error("Couldn't create Pictures/$FOLDER")
            var target = File(dir, displayName)
            var n = 1
            while (target.exists()) {
                target = File(dir, displayName.substringBeforeLast('.') + "_" + n++ + "." + displayName.substringAfterLast('.'))
            }
            source.copyTo(target)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, target.name)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                @Suppress("DEPRECATION")
                put(MediaStore.Images.Media.DATA, target.absolutePath)
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Couldn't add the clean copy to the gallery")
        }
    }

    fun exists(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
    }.getOrDefault(false)
}
