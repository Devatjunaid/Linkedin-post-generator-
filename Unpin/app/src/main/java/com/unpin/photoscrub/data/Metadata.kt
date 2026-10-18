package com.unpin.photoscrub.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.unpin.photoscrub.core.MetadataStripper
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Locale

/** The groups of metadata Unpin shows and removes. */
enum class MetaCategory(
    val code: String,
    val title: String,
    val sensitive: Boolean,
    /** False = always removed; it can't be written back into a clean copy. */
    val optional: Boolean,
    val tags: List<String>,
) {
    LOCATION(
        "GPS", "Location & altitude", true, true,
        listOf(
            "GPSVersionID", "GPSLatitudeRef", "GPSLatitude", "GPSLongitudeRef", "GPSLongitude",
            "GPSAltitudeRef", "GPSAltitude", "GPSTimeStamp", "GPSSatellites", "GPSStatus",
            "GPSMeasureMode", "GPSDOP", "GPSSpeedRef", "GPSSpeed", "GPSTrackRef", "GPSTrack",
            "GPSImgDirectionRef", "GPSImgDirection", "GPSMapDatum", "GPSDestLatitudeRef",
            "GPSDestLatitude", "GPSDestLongitudeRef", "GPSDestLongitude", "GPSDestBearingRef",
            "GPSDestBearing", "GPSDestDistanceRef", "GPSDestDistance", "GPSProcessingMethod",
            "GPSAreaInformation", "GPSDateStamp", "GPSDifferential", "GPSHPositioningError",
        )
    ),
    DEVICE(
        "DEV", "Phone & owner details", true, true,
        listOf(
            "Make", "Model", "BodySerialNumber", "CameraOwnerName", "LensMake", "LensModel",
            "LensSerialNumber", "LensSpecification", "Artist", "Copyright", "ImageUniqueID",
        )
    ),
    DATETIME(
        "TIM", "Date & time taken", false, true,
        listOf(
            "DateTime", "DateTimeOriginal", "DateTimeDigitized", "OffsetTime", "OffsetTimeOriginal",
            "OffsetTimeDigitized", "SubSecTime", "SubSecTimeOriginal", "SubSecTimeDigitized",
        )
    ),
    CAMERA(
        "CAM", "Lens & exposure", false, true,
        listOf(
            "FNumber", "ApertureValue", "MaxApertureValue", "ExposureTime", "ShutterSpeedValue",
            "PhotographicSensitivity", "FocalLength", "FocalLengthIn35mmFilm", "Flash", "WhiteBalance",
            "ExposureProgram", "ExposureMode", "ExposureBiasValue", "MeteringMode", "BrightnessValue",
            "DigitalZoomRatio", "SceneCaptureType", "SubjectDistance", "Contrast", "Saturation",
            "Sharpness", "LightSource", "SensingMethod",
        )
    ),
    SOFTWARE(
        "SW", "Software & notes", false, true,
        listOf("Software", "ImageDescription", "UserComment", "MakerNote")
    ),
    THUMBNAIL("IMG", "Hidden preview image", true, false, emptyList()),
    EXTRAS("XMP", "XMP, IPTC & hidden data", true, false, emptyList());

    companion object {
        val optionalOnes = entries.filter { it.optional }
        val alwaysRemoved = entries.filterNot { it.optional }.toSet()

        /** Tags that can safely be copied back into a cleaned file. */
        fun writableTags(category: MetaCategory) =
            category.tags.filterNot { it == "MakerNote" || it == "UserComment" }
    }
}

data class CategoryInfo(
    val category: MetaCategory,
    val summary: String,
    val fieldCount: Int,
)

data class PhotoInfo(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mime: String,
    val width: Int,
    val height: Int,
    val categories: Map<MetaCategory, CategoryInfo>,
    val latitude: Double?,
    val longitude: Double?,
) {
    val hasLocation get() = MetaCategory.LOCATION in categories
    val hasDevice get() = MetaCategory.DEVICE in categories
    val hasDate get() = MetaCategory.DATETIME in categories
    val totalFields get() = categories.values.sumOf { it.fieldCount }
}

object MetadataReader {

    private const val MAX_BYTES = 80L * 1024 * 1024

    fun displayName(resolver: ContentResolver, uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "photo"
        var size = -1L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) {
                        val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val s = c.getColumnIndex(OpenableColumns.SIZE)
                        if (n >= 0 && !c.isNull(n)) name = c.getString(n)
                        if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
                    }
                }
        }
        return name to size
    }

    /**
     * Asks MediaStore for the un-redacted file when the user granted
     * ACCESS_MEDIA_LOCATION; otherwise Android hides GPS from us (it is still removed).
     */
    fun originalUri(context: Context, uri: Uri): Uri {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return uri
        if (uri.authority != MediaStore.AUTHORITY) return uri
        if (uri.pathSegments.firstOrNull() == "picker") return uri
        if (!Permissions.hasMediaLocation(context)) return uri
        return runCatching { MediaStore.setRequireOriginal(uri) }.getOrDefault(uri)
    }

    fun readBytes(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver
        val attempt = { u: Uri ->
            resolver.openInputStream(u)?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > MAX_BYTES) throw IllegalStateException("Photo is larger than 80 MB")
                    out.write(buf, 0, n)
                }
                out.toByteArray()
            } ?: throw IllegalStateException("Can't open photo")
        }
        val original = originalUri(context, uri)
        return if (original == uri) attempt(uri) else runCatching { attempt(original) }.getOrElse { attempt(uri) }
    }

    fun read(context: Context, uri: Uri): PhotoInfo {
        val (name, size) = displayName(context.contentResolver, uri)
        val bytes = readBytes(context, uri)
        return fromBytes(uri, name, if (size > 0) size else bytes.size.toLong(), context.contentResolver.getType(uri), bytes)
    }

    fun fromBytes(uri: Uri, name: String, size: Long, mimeHint: String?, bytes: ByteArray): PhotoInfo {
        val format = MetadataStripper.detect(bytes)
        val mime = if (format == MetadataStripper.Format.OTHER) (mimeHint ?: "image/*") else format.mime
        val exif = runCatching { ExifInterface(ByteArrayInputStream(bytes)) }.getOrNull()
        val findings = MetadataStripper.inspect(bytes)
        val cats = LinkedHashMap<MetaCategory, CategoryInfo>()
        var lat: Double? = null
        var lon: Double? = null
        var w = 0
        var h = 0

        if (exif != null) {
            w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                .takeIf { it > 0 } ?: exif.getAttributeInt(ExifInterface.TAG_PIXEL_X_DIMENSION, 0)
            h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                .takeIf { it > 0 } ?: exif.getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION, 0)

            fun present(c: MetaCategory) = c.tags.filter { !exif.getAttribute(it).isNullOrBlank() }

            exif.latLong?.let { lat = it[0]; lon = it[1] }
            for (c in MetaCategory.optionalOnes) {
                val found = present(c)
                if (found.isEmpty()) continue
                cats[c] = CategoryInfo(c, summarize(c, exif, lat, lon), found.size)
            }
            if (exif.hasThumbnail()) {
                cats[MetaCategory.THUMBNAIL] = CategoryInfo(MetaCategory.THUMBNAIL, "Embedded thumbnail inside the file", 1)
            }
        }
        if (findings.extrasCount > 0) {
            val parts = buildList {
                if (findings.xmp) add("XMP packet")
                if (findings.iptc) add("IPTC")
                if (findings.comment) add("comments")
                if (findings.trailingBytes > 0) add("hidden ${formatBytes(findings.trailingBytes.toLong())} trailer")
            }
            cats[MetaCategory.EXTRAS] = CategoryInfo(MetaCategory.EXTRAS, parts.joinToString(" · "), findings.extrasCount)
        }
        return PhotoInfo(uri, name, size, mime, w, h, cats, lat, lon)
    }

    private fun summarize(c: MetaCategory, exif: ExifInterface, lat: Double?, lon: Double?): String {
        fun a(tag: String) = exif.getAttribute(tag)?.trim().orEmpty()
        return when (c) {
            MetaCategory.LOCATION -> {
                if (lat != null && lon != null) {
                    val alt = exif.getAltitude(Double.NaN)
                    buildString {
                        append(formatCoord(lat, lon))
                        if (!alt.isNaN()) append(" · ${alt.toInt()} m alt")
                    }
                } else "GPS tags present"
            }
            MetaCategory.DEVICE -> listOf(
                listOf(a("Make"), a("Model")).filter { it.isNotEmpty() }.distinct().joinToString(" "),
                a("BodySerialNumber").takeIf { it.length > 3 }?.let { "serial ••••" + it.takeLast(4) }.orEmpty(),
                a("Artist").takeIf { it.isNotEmpty() }?.let { "by $it" }.orEmpty(),
                a("LensModel"),
            ).filter { it.isNotEmpty() }.joinToString(" · ").ifEmpty { "Device identifiers" }
            MetaCategory.DATETIME -> {
                val raw = a("DateTimeOriginal").ifEmpty { a("DateTime") }.ifEmpty { a("DateTimeDigitized") }
                prettyDate(raw).let { d -> a("OffsetTimeOriginal").takeIf { it.isNotEmpty() }?.let { "$d (UTC$it)" } ?: d }
            }
            MetaCategory.CAMERA -> buildList {
                exif.getAttributeDouble("FNumber", 0.0).takeIf { it > 0 }?.let { add("f/" + trim1(it)) }
                exif.getAttributeDouble("ExposureTime", 0.0).takeIf { it > 0 }?.let {
                    add(if (it < 1) "1/${(1 / it).toInt()} s" else "${trim1(it)} s")
                }
                a("PhotographicSensitivity").takeIf { it.isNotEmpty() }?.let { add("ISO $it") }
                exif.getAttributeDouble("FocalLength", 0.0).takeIf { it > 0 }?.let { add("${trim1(it)} mm") }
            }.joinToString(" · ").ifEmpty { "Camera settings" }
            MetaCategory.SOFTWARE -> listOf(a("Software"), a("ImageDescription"))
                .filter { it.isNotEmpty() }.joinToString(" · ")
                .ifEmpty { if (exif.getAttribute("MakerNote") != null) "Manufacturer notes" else "Notes" }
            else -> ""
        }
    }

    fun formatCoord(lat: Double, lon: Double): String {
        val ns = if (lat >= 0) "N" else "S"
        val ew = if (lon >= 0) "E" else "W"
        return String.format(Locale.US, "%.5f° %s, %.5f° %s", kotlin.math.abs(lat), ns, kotlin.math.abs(lon), ew)
    }

    private fun trim1(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.1f", v)

    private fun prettyDate(raw: String): String = runCatching {
        val parsed = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(raw)!!
        SimpleDateFormat("EEE d MMM yyyy · HH:mm:ss", Locale.getDefault()).format(parsed)
    }.getOrDefault(raw.ifEmpty { "Timestamp" })
}

fun formatBytes(bytes: Long): String {
    val abs = kotlin.math.abs(bytes)
    return when {
        abs >= 1024L * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1048576.0)
        abs >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
