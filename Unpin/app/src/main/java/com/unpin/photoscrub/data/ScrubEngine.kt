package com.unpin.photoscrub.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.unpin.photoscrub.core.MetadataStripper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.security.SecureRandom
import kotlin.math.ceil

enum class Preset(val title: String, val description: String) {
    FULL("Full wipe", "Every tag, every field"),
    KEEP_DATE("Keep date", "Albums stay in order"),
    LOCATION_ONLY("Location only", "Keep camera details"),
    CUSTOM("Custom", "Pick fields yourself");

    /** Categories this preset removes. Thumbnails and XMP/IPTC extras always go. */
    fun removes(custom: Set<MetaCategory>): Set<MetaCategory> = when (this) {
        FULL -> MetaCategory.entries.toSet()
        KEEP_DATE -> MetaCategory.entries.toSet() - MetaCategory.DATETIME
        LOCATION_ONLY -> setOf(MetaCategory.LOCATION)
        CUSTOM -> custom
    } + MetaCategory.alwaysRemoved
}

data class ScrubOptions(
    val anonymizeNames: Boolean = false,
    val keepOrientation: Boolean = true,
    val keepColorProfile: Boolean = true,
)

data class ScrubOutcome(
    val sourceName: String,
    val outputUri: Uri?,
    val outputName: String,
    val originalBytes: Long,
    val newBytes: Long,
    val fieldsRemoved: Int,
    val locationErased: Boolean,
    val lossless: Boolean,
    val verified: Boolean,
    val error: String? = null,
)

object ScrubEngine {

    private val random = SecureRandom()

    fun scrub(context: Context, uri: Uri, remove: Set<MetaCategory>, options: ScrubOptions): ScrubOutcome {
        val (name, size) = MetadataReader.displayName(context.contentResolver, uri)
        return try {
            scrubOrThrow(context, uri, name, size, remove + MetaCategory.alwaysRemoved, options)
        } catch (t: Throwable) {
            ScrubOutcome(name, null, "", size.coerceAtLeast(0), 0, 0, false, false, false, t.message ?: "Couldn't clean this photo")
        }
    }

    private fun scrubOrThrow(
        context: Context, uri: Uri, name: String, size: Long,
        remove: Set<MetaCategory>, options: ScrubOptions,
    ): ScrubOutcome {
        val bytes = MetadataReader.readBytes(context, uri)
        val info = MetadataReader.fromBytes(uri, name, bytes.size.toLong(), context.contentResolver.getType(uri), bytes)
        val originalExif = runCatching { ExifInterface(ByteArrayInputStream(bytes)) }.getOrNull()

        val stripped = MetadataStripper.strip(bytes, options.keepColorProfile)
        val lossless = stripped != null
        val cleanBytes = stripped?.bytes ?: reencode(bytes)
        val format = stripped?.format ?: MetadataStripper.Format.OTHER

        val workDir = File(context.cacheDir, "scrub").apply { mkdirs() }
        val temp = File(workDir, "work_${System.nanoTime()}.${format.extension}")
        try {
            temp.writeBytes(cleanBytes)
            writeBack(temp, originalExif, remove, options, lossless)

            val outName = outputName(name, format.extension, options.anonymizeNames)
            val outUri = GallerySaver.save(context, temp, outName, format.mime)
            val verified = verify(context, outUri, remove)
            val removedFields = info.categories.filterKeys { it in remove }.values.sumOf { it.fieldCount }
            return ScrubOutcome(
                sourceName = name,
                outputUri = outUri,
                outputName = outName,
                originalBytes = if (size > 0) size else bytes.size.toLong(),
                newBytes = temp.length(),
                fieldsRemoved = removedFields,
                locationErased = info.hasLocation && MetaCategory.LOCATION in remove,
                lossless = lossless,
                verified = verified,
            )
        } finally {
            temp.delete()
        }
    }

    /** Copies back only what the user chose to keep. */
    private fun writeBack(file: File, original: ExifInterface?, remove: Set<MetaCategory>, options: ScrubOptions, lossless: Boolean) {
        if (original == null) return
        val keep = MetaCategory.optionalOnes.filter { it !in remove }
        val orientation = original.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val writeOrientation = options.keepOrientation && lossless &&
            orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED
        if (keep.isEmpty() && !writeOrientation) return
        runCatching {
            val out = ExifInterface(file)
            for (c in keep) for (tag in MetaCategory.writableTags(c)) {
                original.getAttribute(tag)?.let { out.setAttribute(tag, it) }
            }
            if (writeOrientation) out.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            out.saveAttributes()
        }
    }

    /** Re-reads the saved copy and checks every removed category is really gone. */
    private fun verify(context: Context, uri: Uri, removed: Set<MetaCategory>): Boolean = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
        val exif = ExifInterface(ByteArrayInputStream(bytes))
        val tagsGone = removed.flatMap { it.tags }.none { !exif.getAttribute(it).isNullOrBlank() }
        val findings = MetadataStripper.inspect(bytes)
        tagsGone && !exif.hasThumbnail() && !findings.xmp && !findings.iptc && !findings.comment && findings.trailingBytes == 0
    }.getOrDefault(false)

    private fun outputName(original: String, ext: String, anonymize: Boolean): String {
        if (anonymize) {
            val id = ByteArray(4).also(random::nextBytes).joinToString("") { "%02x".format(it) }
            return "unpin_$id.$ext"
        }
        val base = original.substringBeforeLast('.').ifBlank { "photo" }.take(60)
        return "${base}_clean.$ext"
    }

    /** For HEIC/AVIF/GIF and anything else without a lossless path. */
    private fun reencode(bytes: ByteArray): ByteArray {
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MAX_DIMENSION) decoder.setTargetSampleSize(ceil(longest / MAX_DIMENSION.toDouble()).toInt())
            }
        } else {
            decodeLegacy(bytes)
        }
        val out = ByteArrayOutputStream()
        val flat = if (bitmap.hasAlpha()) flattenAlpha(bitmap) else bitmap
        flat.compress(Bitmap.CompressFormat.JPEG, 95, out)
        if (flat !== bitmap) flat.recycle()
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun decodeLegacy(bytes: ByteArray): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_DIMENSION) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: error("This image format isn't supported on this Android version")
        val exif = runCatching { ExifInterface(ByteArrayInputStream(bytes)) }.getOrNull() ?: return decoded
        val matrix = Matrix()
        if (exif.isFlipped) matrix.postScale(-1f, 1f)
        if (exif.rotationDegrees != 0) matrix.postRotate(exif.rotationDegrees.toFloat())
        if (matrix.isIdentity) return decoded
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded) decoded.recycle()
        return rotated
    }

    private fun flattenAlpha(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawBitmap(src, 0f, 0f, null)
        return out
    }

    private const val MAX_DIMENSION = 8192
}
