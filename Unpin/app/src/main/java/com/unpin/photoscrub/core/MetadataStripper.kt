package com.unpin.photoscrub.core

import java.io.ByteArrayOutputStream

/**
 * Lossless metadata removal for JPEG, PNG and WebP.
 *
 * The image data itself is copied byte-for-byte; only the containers that carry
 * metadata (EXIF, XMP, IPTC, comments, text chunks, trailing payloads such as
 * motion-photo videos or gain maps) are dropped. Formats this class can't handle
 * return null from [strip] and are re-encoded by the caller instead.
 *
 * Pure Kotlin on purpose: no Android dependencies, so it is unit-testable on the JVM.
 */
object MetadataStripper {

    enum class Format(val mime: String, val extension: String) {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp"),
        OTHER("image/jpeg", "jpg"),
    }

    /** What was found (and dropped) while stripping. */
    data class Findings(
        val exif: Boolean = false,
        val xmp: Boolean = false,
        val iptc: Boolean = false,
        val comment: Boolean = false,
        val trailingBytes: Int = 0,
        val removedBlocks: Int = 0,
    ) {
        /** Blocks that aren't plain EXIF: XMP packets, IPTC, comments and hidden trailers. */
        val extrasCount: Int
            get() = listOf(xmp, iptc, comment, trailingBytes > 0).count { it }
    }

    class Result(val bytes: ByteArray, val format: Format, val findings: Findings)

    class MalformedImageException(message: String) : Exception(message)

    fun detect(bytes: ByteArray): Format = when {
        bytes.size >= 3 && u8(bytes, 0) == 0xFF && u8(bytes, 1) == 0xD8 && u8(bytes, 2) == 0xFF -> Format.JPEG
        bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE) -> Format.PNG
        bytes.size >= 12 && ascii(bytes, 0, 4) == "RIFF" && ascii(bytes, 8, 4) == "WEBP" -> Format.WEBP
        else -> Format.OTHER
    }

    /** Scans without modifying anything. */
    fun inspect(bytes: ByteArray): Findings =
        try {
            strip(bytes, keepIcc = true)?.findings ?: Findings()
        } catch (_: MalformedImageException) {
            Findings()
        }

    /** Returns stripped bytes, or null when [bytes] isn't JPEG, PNG or WebP. */
    fun strip(bytes: ByteArray, keepIcc: Boolean): Result? = when (detect(bytes)) {
        Format.JPEG -> stripJpeg(bytes, keepIcc)
        Format.PNG -> stripPng(bytes, keepIcc)
        Format.WEBP -> stripWebp(bytes, keepIcc)
        Format.OTHER -> null
    }

    // ---------------------------------------------------------------- JPEG

    private fun stripJpeg(src: ByteArray, keepIcc: Boolean): Result {
        val out = ByteArrayOutputStream(src.size)
        out.write(0xFF); out.write(0xD8)
        var exif = false; var xmp = false; var iptc = false; var comment = false
        var removed = 0
        var trailing = 0
        var i = 2
        var sawEoi = false

        loop@ while (i < src.size) {
            if (u8(src, i) != 0xFF) throw MalformedImageException("Expected marker at $i")
            var j = i
            while (j < src.size && u8(src, j) == 0xFF) j++ // fill bytes
            if (j >= src.size) break
            val marker = u8(src, j)
            i = j + 1

            if (marker == 0xD9) { // EOI: anything after it is a hidden payload
                out.write(0xFF); out.write(0xD9)
                sawEoi = true
                trailing = src.size - i
                break@loop
            }
            if (marker in 0xD0..0xD7 || marker == 0x01) {
                out.write(0xFF); out.write(marker)
                continue
            }
            if (i + 2 > src.size) throw MalformedImageException("Truncated segment")
            val len = u16be(src, i)
            if (len < 2 || i + len > src.size) throw MalformedImageException("Bad segment length")
            val payloadStart = i + 2
            val payloadLen = len - 2

            var keep = true
            var replacement: ByteArray? = null
            when (marker) {
                0xE0 -> {
                    if (startsWith(src, payloadStart, payloadLen, JFIF)) {
                        // Keep JFIF but drop its optional embedded thumbnail.
                        if (payloadLen > 14 && (u8(src, payloadStart + 12) != 0 || u8(src, payloadStart + 13) != 0)) {
                            replacement = src.copyOfRange(payloadStart, payloadStart + 14).also {
                                it[12] = 0; it[13] = 0
                            }
                            removed++
                        }
                    } else { // JFXX (thumbnail extension) or anything else
                        keep = false
                    }
                }
                0xE1 -> {
                    keep = false
                    if (startsWith(src, payloadStart, payloadLen, EXIF_ID)) exif = true
                    if (startsWith(src, payloadStart, payloadLen, XMP_ID) ||
                        startsWith(src, payloadStart, payloadLen, XMP_EXT_ID)
                    ) xmp = true
                }
                0xE2 -> keep = keepIcc && startsWith(src, payloadStart, payloadLen, ICC_ID)
                0xED -> { keep = false; iptc = true }
                0xEE -> keep = startsWith(src, payloadStart, payloadLen, ADOBE_ID) // colour transform
                in 0xE3..0xEF -> keep = false
                0xFE -> { keep = false; comment = true }
            }

            if (keep) {
                out.write(0xFF); out.write(marker)
                if (replacement != null) {
                    val newLen = replacement.size + 2
                    out.write(newLen ushr 8); out.write(newLen and 0xFF)
                    out.write(replacement)
                } else {
                    out.write(src, i, len)
                }
            } else {
                removed++
            }
            i += len

            if (marker == 0xDA) { // Start of scan: copy entropy-coded data up to the next real marker
                val start = i
                var k = i
                while (k < src.size) {
                    if (u8(src, k) == 0xFF && k + 1 < src.size) {
                        val n = u8(src, k + 1)
                        if (n == 0x00 || n in 0xD0..0xD7) { k += 2; continue }
                        if (n == 0xFF) { k += 1; continue }
                        break
                    }
                    k++
                }
                out.write(src, start, k - start)
                i = k
            }
        }
        if (!sawEoi) { out.write(0xFF); out.write(0xD9) }
        if (trailing > 0) removed++

        return Result(
            out.toByteArray(), Format.JPEG,
            Findings(exif, xmp, iptc, comment, trailing, removed)
        )
    }

    // ---------------------------------------------------------------- PNG

    private val PNG_KEEP = setOf(
        "tRNS", "gAMA", "cHRM", "sRGB", "sBIT", "bKGD", "pHYs", "hIST", "sPLT",
        "acTL", "fcTL", "fdAT", "cICP", "mDCV", "cLLI"
    )

    private fun stripPng(src: ByteArray, keepIcc: Boolean): Result {
        val out = ByteArrayOutputStream(src.size)
        out.write(PNG_SIGNATURE)
        var exif = false; var xmp = false; var comment = false
        var removed = 0
        var trailing = 0
        var i = 8
        while (i + 12 <= src.size) {
            val len = u32be(src, i)
            if (len < 0 || i + 12 + len > src.size) throw MalformedImageException("Bad PNG chunk")
            val type = ascii(src, i + 4, 4)
            val critical = type[0].isUpperCase()
            val keep = critical || type in PNG_KEEP || (type == "iCCP" && keepIcc)
            if (keep) out.write(src, i, len + 12) else removed++
            when (type) {
                "eXIf" -> exif = true
                "iTXt" -> if (startsWith(src, i + 8, len, "XML:com.adobe.xmp".toByteArray())) xmp = true else comment = true
                "tEXt", "zTXt" -> {
                    if (startsWith(src, i + 8, len, "Raw profile type exif".toByteArray())) exif = true else comment = true
                }
            }
            i += len + 12
            if (type == "IEND") {
                trailing = src.size - i
                break
            }
        }
        if (trailing > 0) removed++
        return Result(out.toByteArray(), Format.PNG, Findings(exif, xmp, false, comment, trailing, removed))
    }

    // ---------------------------------------------------------------- WebP

    private val WEBP_KEEP = setOf("VP8 ", "VP8L", "VP8X", "ALPH", "ANIM", "ANMF")

    private fun stripWebp(src: ByteArray, keepIcc: Boolean): Result {
        val riffSize = u32le(src, 4)
        val end = minOf(src.size, 8 + riffSize)
        val body = ByteArrayOutputStream(src.size)
        var exif = false; var xmp = false
        var removed = 0
        var keptIcc = false
        var vp8xOffsetInBody = -1
        var i = 12
        while (i + 8 <= end) {
            val type = ascii(src, i, 4)
            val size = u32le(src, i + 4)
            val padded = size + (size and 1)
            if (size < 0 || i + 8 + size > end) throw MalformedImageException("Bad WebP chunk")
            val keep = type in WEBP_KEEP || (type == "ICCP" && keepIcc)
            when (type) {
                "EXIF" -> exif = true
                "XMP " -> xmp = true
            }
            if (keep) {
                if (type == "VP8X") vp8xOffsetInBody = body.size()
                if (type == "ICCP") keptIcc = true
                body.write(src, i, minOf(8 + padded, end - i))
            } else {
                removed++
            }
            i += 8 + padded
        }
        val bodyBytes = body.toByteArray()
        if (vp8xOffsetInBody >= 0) {
            val flagsIndex = vp8xOffsetInBody + 8
            var flags = u8(bodyBytes, flagsIndex)
            flags = flags and 0x08.inv() and 0x04.inv() // EXIF, XMP
            if (!keptIcc) flags = flags and 0x20.inv()
            bodyBytes[flagsIndex] = flags.toByte()
        }
        val out = ByteArrayOutputStream(bodyBytes.size + 12)
        out.write("RIFF".toByteArray())
        writeU32le(out, bodyBytes.size + 4)
        out.write("WEBP".toByteArray())
        out.write(bodyBytes)
        val trailing = src.size - end
        if (trailing > 0) removed++
        return Result(out.toByteArray(), Format.WEBP, Findings(exif, xmp, false, false, trailing, removed))
    }

    // ---------------------------------------------------------------- helpers

    private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val JFIF = "JFIF\u0000".toByteArray(Charsets.ISO_8859_1)
    private val EXIF_ID = "Exif\u0000\u0000".toByteArray(Charsets.ISO_8859_1)
    private val XMP_ID = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(Charsets.ISO_8859_1)
    private val XMP_EXT_ID = "http://ns.adobe.com/xmp/extension/\u0000".toByteArray(Charsets.ISO_8859_1)
    private val ICC_ID = "ICC_PROFILE\u0000".toByteArray(Charsets.ISO_8859_1)
    private val ADOBE_ID = "Adobe".toByteArray(Charsets.ISO_8859_1)

    private fun u8(b: ByteArray, i: Int) = b[i].toInt() and 0xFF
    private fun u16be(b: ByteArray, i: Int) = (u8(b, i) shl 8) or u8(b, i + 1)
    private fun u32be(b: ByteArray, i: Int) =
        (u8(b, i) shl 24) or (u8(b, i + 1) shl 16) or (u8(b, i + 2) shl 8) or u8(b, i + 3)
    private fun u32le(b: ByteArray, i: Int) =
        u8(b, i) or (u8(b, i + 1) shl 8) or (u8(b, i + 2) shl 16) or (u8(b, i + 3) shl 24)

    private fun writeU32le(out: ByteArrayOutputStream, v: Int) {
        out.write(v and 0xFF); out.write((v ushr 8) and 0xFF)
        out.write((v ushr 16) and 0xFF); out.write((v ushr 24) and 0xFF)
    }

    private fun ascii(b: ByteArray, i: Int, n: Int) = String(b, i, n, Charsets.ISO_8859_1)

    private fun startsWith(b: ByteArray, start: Int, len: Int, prefix: ByteArray): Boolean {
        if (len < prefix.size || start + prefix.size > b.size) return false
        for (k in prefix.indices) if (b[start + k] != prefix[k]) return false
        return true
    }
}
