package com.harrycodess.drawingkids.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File

object Prefs {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("twilio_kids", Context.MODE_PRIVATE)
    }

    var soundOn: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) = prefs.edit().putBoolean("sound", value).apply()

    /** Bucket colors a kid picked for an animal, so a page can be continued later. */
    fun loadFills(animalId: String): Map<String, Color> {
        val raw = prefs.getString("fills_$animalId", null) ?: return emptyMap()
        return raw.split(';').mapNotNull { entry ->
            val parts = entry.split('=')
            val argb = parts.getOrNull(1)?.toLongOrNull()?.toInt() ?: return@mapNotNull null
            parts[0] to Color(argb)
        }.toMap()
    }

    fun saveFills(animalId: String, fills: Map<String, Color>) {
        val raw = fills.entries.joinToString(";") { "${it.key}=${it.value.toArgb().toLong() and 0xFFFFFFFFL}" }
        prefs.edit().putString("fills_$animalId", raw).apply()
    }

    fun coloredAnimals(): Set<String> =
        prefs.all.keys.filter { it.startsWith("fills_") && !prefs.getString(it, null).isNullOrEmpty() }
            .map { it.removePrefix("fills_") }.toSet()
}

data class Artwork(val file: File, val title: String, val createdAt: Long)

object Gallery {
    private lateinit var dir: File

    fun init(context: Context) {
        dir = File(context.filesDir, "gallery").apply { mkdirs() }
    }

    fun list(): List<Artwork> =
        (dir.listFiles { f -> f.extension == "png" } ?: emptyArray())
            .sortedByDescending { it.lastModified() }
            .map { f ->
                val title = f.nameWithoutExtension.substringBefore("__").replace('_', ' ')
                Artwork(f, title, f.lastModified())
            }

    fun save(bitmap: Bitmap, title: String): File {
        val safe = title.filter { it.isLetterOrDigit() || it == ' ' }.replace(' ', '_').ifEmpty { "Drawing" }
        val file = File(dir, "${safe}__${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun delete(artwork: Artwork) {
        artwork.file.delete()
    }

    fun loadThumb(file: File, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSide && bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
