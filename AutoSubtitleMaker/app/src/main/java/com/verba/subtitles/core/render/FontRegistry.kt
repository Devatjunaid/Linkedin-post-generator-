package com.verba.subtitles.core.render

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import com.verba.subtitles.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class FontOption(
    val id: String,
    val name: String,
    @param:FontRes val resId: Int? = null,
    val file: File? = null,
) {
    val isCustom: Boolean get() = file != null
}

/**
 * Bundled caption fonts (all SIL Open Font License / Apache 2.0, see assets/licenses) plus any
 * .ttf/.otf the user imports. Typefaces are cached and safe to use from the export thread.
 */
class FontRegistry(private val context: Context) {

    val bundled: List<FontOption> = listOf(
        FontOption("anton", "Anton", R.font.anton),
        FontOption("bebas_neue", "Bebas Neue", R.font.bebas_neue),
        FontOption("poppins_black", "Poppins Black", R.font.poppins_black),
        FontOption("poppins_bold", "Poppins Bold", R.font.poppins_bold),
        FontOption("archivo_black", "Archivo Black", R.font.archivo_black),
        FontOption("bangers", "Bangers", R.font.bangers),
        FontOption("permanent_marker", "Marker", R.font.permanent_marker),
        FontOption("dm_serif_display", "DM Serif", R.font.dm_serif_display),
        FontOption("pacifico", "Pacifico", R.font.pacifico),
        FontOption("caveat", "Caveat", R.font.caveat),
        FontOption("space_mono_bold", "Space Mono", R.font.space_mono_bold),
        FontOption("bricolage", "Bricolage", R.font.bricolage),
    )

    private val customDir = File(context.filesDir, "fonts").apply { mkdirs() }
    private val cache = ConcurrentHashMap<String, Typeface>()
    private val _custom = MutableStateFlow(scanCustom())
    val custom: StateFlow<List<FontOption>> = _custom.asStateFlow()

    fun all(): List<FontOption> = bundled + _custom.value

    fun option(id: String): FontOption = all().firstOrNull { it.id == id } ?: bundled.first()

    fun typeface(id: String): Typeface = cache.getOrPut(id) { load(option(id)) }

    private fun load(option: FontOption): Typeface = runCatching {
        when {
            option.file != null -> Typeface.createFromFile(option.file)
            option.resId != null -> ResourcesCompat.getFont(context, option.resId)
            else -> null
        }
    }.getOrNull() ?: Typeface.DEFAULT_BOLD

    /** Copies a user font into app storage. Returns null if the file is not a readable font. */
    suspend fun import(uri: Uri): FontOption? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "font.ttf"
        val ext = displayName.substringAfterLast('.', "ttf").lowercase()
        if (ext != "ttf" && ext != "otf") return@withContext null
        val base = displayName.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9 _-]"), "").ifBlank { "Custom" }
        val target = File(customDir, "${System.currentTimeMillis()}_$base.$ext")
        resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
            ?: return@withContext null
        val valid = runCatching { Typeface.createFromFile(target) }.isSuccess
        if (!valid) {
            target.delete()
            return@withContext null
        }
        _custom.value = scanCustom()
        _custom.value.firstOrNull { it.file == target }
    }

    fun delete(option: FontOption) {
        option.file?.delete()
        cache.remove(option.id)
        _custom.value = scanCustom()
    }

    private fun scanCustom(): List<FontOption> = (customDir.listFiles() ?: emptyArray())
        .filter { it.extension.lowercase() in setOf("ttf", "otf") }
        .sortedBy { it.name }
        .map { file ->
            FontOption(
                id = "custom:${file.name}",
                name = file.nameWithoutExtension.substringAfter('_').ifBlank { "Custom" },
                file = file,
            )
        }
}
