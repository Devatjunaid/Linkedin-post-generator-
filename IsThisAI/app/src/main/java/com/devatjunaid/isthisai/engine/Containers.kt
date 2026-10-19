package com.devatjunaid.isthisai.engine

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

enum class ImageFormat(val label: String) {
    JPEG("JPEG"), PNG("PNG"), WEBP("WebP"), HEIF("HEIF/HEIC"), AVIF("AVIF"), GIF("GIF"), TIFF("TIFF/DNG"), UNKNOWN("Unknown")
}

/** A region of the file shown in the "File structure" section of the raw view. */
data class FileSegment(val name: String, val offset: Int, val length: Int)

/** Everything pulled out of the container before interpretation. */
internal class RawFindings(val format: ImageFormat) {
    var width: Int? = null
    var height: Int? = null
    val jumbf = ArrayList<Pair<Int, ByteArray>>()      // offset -> C2PA JUMBF bytes
    var exifTiff: ByteArray? = null
    var exifOffset: Int = -1
    val xmpPackets = ArrayList<String>()
    var xmpOffset: Int = -1
    var iptc = LinkedHashMap<String, String>()
    var iptcOffset: Int = -1
    val textChunks = ArrayList<Pair<String, String>>() // PNG tEXt/iTXt/zTXt keyword -> text
    val comments = ArrayList<String>()
    val segments = ArrayList<FileSegment>()
}

internal object Containers {

    private val C2PA_BMFF_UUID = byteArrayOf(
        0xD8.toByte(), 0xFE.toByte(), 0xC3.toByte(), 0xD6.toByte(), 0x1B, 0x0E, 0x48, 0x3C,
        0x92.toByte(), 0x97.toByte(), 0x58, 0x28, 0x87.toByte(), 0x7E, 0xC4.toByte(), 0x81.toByte(),
    )

    fun detect(b: ByteArray): ImageFormat = when {
        b.size >= 3 && b.u8(0) == 0xFF && b.u8(1) == 0xD8 && b.u8(2) == 0xFF -> ImageFormat.JPEG
        b.size >= 8 && b.u32be(0) == 0x89504E47L && b.u32be(4) == 0x0D0A1A0AL -> ImageFormat.PNG
        b.size >= 12 && b.ascii(0, 4) == "RIFF" && b.ascii(8, 4) == "WEBP" -> ImageFormat.WEBP
        b.size >= 6 && b.ascii(0, 4) == "GIF8" -> ImageFormat.GIF
        b.size >= 12 && b.ascii(4, 4) == "ftyp" -> {
            val brands = b.ascii(8, minOf(b.u32be(0).toInt().coerceAtLeast(12), b.size) - 8)
            if (brands.contains("avif") || brands.contains("avis")) ImageFormat.AVIF else ImageFormat.HEIF
        }
        b.size >= 4 && (b.ascii(0, 4) == "II*\u0000" || b.ascii(0, 4) == "MM\u0000*") -> ImageFormat.TIFF
        else -> ImageFormat.UNKNOWN
    }

    fun read(b: ByteArray): RawFindings {
        val f = RawFindings(detect(b))
        runCatching {
            when (f.format) {
                ImageFormat.JPEG -> readJpeg(b, f)
                ImageFormat.PNG -> readPng(b, f)
                ImageFormat.WEBP -> readWebp(b, f)
                ImageFormat.HEIF, ImageFormat.AVIF -> readBmff(b, f)
                ImageFormat.GIF -> readGif(b, f)
                ImageFormat.TIFF -> { f.exifTiff = b; f.exifOffset = 0 }
                ImageFormat.UNKNOWN -> Unit
            }
        }
        // Formats without a dedicated slot still often carry plain XMP or EXIF somewhere.
        if (f.xmpPackets.isEmpty()) {
            val packets = Xmp.findPackets(b)
            if (packets.isNotEmpty()) {
                f.xmpPackets += packets
                f.xmpOffset = b.indexOf("<x:xmpmeta".asciiBytes())
            }
        }
        if (f.exifTiff == null && f.format != ImageFormat.JPEG) findLooseExif(b, f)
        return f
    }

    // ---------------- JPEG ----------------

    private fun readJpeg(b: ByteArray, f: RawFindings) {
        val xmpNs = "http://ns.adobe.com/xap/1.0/\u0000".asciiBytes()
        val extXmpNs = "http://ns.adobe.com/xmp/extension/\u0000".asciiBytes()
        val app11 = LinkedHashMap<Int, MutableList<Pair<Long, ByteArray>>>()
        var app11Offset = -1
        val extended = ByteArrayOutputStream()
        var i = 2
        while (i + 4 <= b.size) {
            if (b.u8(i) != 0xFF) break
            val marker = b.u8(i + 1)
            if (marker == 0xFF) { i++; continue }
            if (marker == 0xD8 || marker == 0x01 || marker in 0xD0..0xD7) { i += 2; continue }
            if (marker == 0xD9) break
            val len = b.u16be(i + 2)
            if (len < 2 || i + 2 + len > b.size) break
            val data = i + 4
            val dataLen = len - 2
            val name = segmentName(marker)
            f.segments += FileSegment(name, i, len + 2)
            when (marker) {
                0xE1 -> when {
                    b.startsWithAt(data, "Exif\u0000\u0000".asciiBytes()) && f.exifTiff == null -> {
                        f.exifTiff = b.slice(data + 6, dataLen - 6); f.exifOffset = i
                    }
                    b.startsWithAt(data, xmpNs) -> {
                        f.xmpPackets += String(b, data + xmpNs.size, dataLen - xmpNs.size, Charsets.UTF_8)
                        if (f.xmpOffset < 0) f.xmpOffset = i
                    }
                    b.startsWithAt(data, extXmpNs) -> {
                        // 32-byte GUID, 4-byte full length, 4-byte offset, then the chunk.
                        val chunkAt = data + extXmpNs.size + 40
                        if (chunkAt < data + dataLen) extended.write(b, chunkAt, data + dataLen - chunkAt)
                    }
                }
                0xEB -> if (dataLen > 16 && b.ascii(data, 2) == "JP") {
                    val instance = b.u16be(data + 2)
                    val seq = b.u32be(data + 4)
                    app11.getOrPut(instance) { mutableListOf() } += seq to b.slice(data + 8, dataLen - 8)
                    if (app11Offset < 0) app11Offset = i
                }
                0xED -> {
                    val ps = "Photoshop 3.0\u0000".asciiBytes()
                    if (b.startsWithAt(data, ps)) {
                        val fields = Iptc.parsePhotoshop(b.slice(data + ps.size, dataLen - ps.size))
                        if (fields.isNotEmpty()) { f.iptc.putAll(fields); f.iptcOffset = i }
                    }
                }
                0xFE -> f.comments += String(b, data, dataLen, Charsets.UTF_8).clean(600)
                in 0xC0..0xCF -> if (marker != 0xC4 && marker != 0xC8 && marker != 0xCC && dataLen >= 5) {
                    f.height = b.u16be(data + 1); f.width = b.u16be(data + 3)
                }
            }
            if (marker == 0xDA) { f.segments += FileSegment("Image data", i + 2 + len, b.size - (i + 2 + len)); break }
            i += 2 + len
        }
        if (extended.size() > 0) f.xmpPackets += extended.toString(Charsets.UTF_8.name())
        for ((_, parts) in app11) {
            val sorted = parts.sortedBy { it.first }
            val out = ByteArrayOutputStream()
            sorted.forEachIndexed { idx, (_, payload) ->
                if (idx == 0) {
                    out.write(payload)
                } else {
                    // Continuation segments repeat the box header (LBox, TBox[, XLBox]).
                    val skip = if (payload.size >= 8 && payload.u32be(0) == 1L) 16 else 8
                    if (payload.size > skip) out.write(payload, skip, payload.size - skip)
                }
            }
            f.jumbf += app11Offset to out.toByteArray()
        }
    }

    private fun segmentName(marker: Int): String = when (marker) {
        0xE0 -> "APP0 (JFIF)"; 0xE1 -> "APP1 (EXIF/XMP)"; 0xE2 -> "APP2 (ICC/MPF)"
        0xEB -> "APP11 (JUMBF)"; 0xED -> "APP13 (IPTC)"; 0xEE -> "APP14 (Adobe)"
        0xDB -> "Quantization tables"; 0xC4 -> "Huffman tables"; 0xDA -> "Start of scan"
        0xFE -> "Comment"; 0xDD -> "Restart interval"
        in 0xC0..0xCF -> "Frame header"
        in 0xE0..0xEF -> "APP${marker - 0xE0}"
        else -> "Marker 0x" + marker.toString(16).uppercase()
    }

    // ---------------- PNG ----------------

    private fun readPng(b: ByteArray, f: RawFindings) {
        var p = 8
        while (p + 12 <= b.size) {
            val len = b.u32be(p)
            val type = b.ascii(p + 4, 4)
            if (len < 0 || p + 12 + len > b.size) break
            val data = p + 8
            val n = len.toInt()
            f.segments += FileSegment("$type chunk", p, n + 12)
            when (type) {
                "IHDR" -> if (n >= 8) { f.width = b.u32be(data).toInt(); f.height = b.u32be(data + 4).toInt() }
                "tEXt" -> {
                    val z = b.indexOfZero(data, data + n)
                    if (z > data) f.textChunks += String(b, data, z - data, Charsets.ISO_8859_1) to
                        String(b, z + 1, data + n - z - 1, Charsets.ISO_8859_1)
                }
                "zTXt" -> {
                    val z = b.indexOfZero(data, data + n)
                    if (z > data && z + 2 <= data + n) {
                        val text = inflate(b.slice(z + 2, data + n - z - 2))
                        if (text != null) f.textChunks += String(b, data, z - data, Charsets.ISO_8859_1) to String(text, Charsets.ISO_8859_1)
                    }
                }
                "iTXt" -> readItxt(b, data, n, f)
                "eXIf" -> if (f.exifTiff == null) { f.exifTiff = b.slice(data, n); f.exifOffset = p }
                "caBX" -> f.jumbf += p to b.slice(data, n)
                "IEND" -> break
            }
            p += 12 + n
        }
    }

    private fun readItxt(b: ByteArray, data: Int, n: Int, f: RawFindings) {
        val end = data + n
        val z1 = b.indexOfZero(data, end)
        if (z1 < 0 || z1 + 3 > end) return
        val keyword = String(b, data, z1 - data, Charsets.ISO_8859_1)
        val compressed = b.u8(z1 + 1) == 1
        val z2 = b.indexOfZero(z1 + 3, end)
        if (z2 < 0) return
        val z3 = b.indexOfZero(z2 + 1, end)
        if (z3 < 0) return
        val raw = b.slice(z3 + 1, end - z3 - 1)
        val textBytes = if (compressed) inflate(raw) ?: return else raw
        val text = String(textBytes, Charsets.UTF_8)
        if (keyword == "XML:com.adobe.xmp") {
            f.xmpPackets += text
            if (f.xmpOffset < 0) f.xmpOffset = data - 8
        } else {
            f.textChunks += keyword to text
        }
    }

    private fun inflate(data: ByteArray): ByteArray? = runCatching {
        val inflater = Inflater()
        inflater.setInput(data)
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (!inflater.finished()) {
            val count = inflater.inflate(buf)
            if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
            out.write(buf, 0, count)
            if (out.size() > 4_000_000) break
        }
        inflater.end()
        out.toByteArray()
    }.getOrNull()

    // ---------------- WebP ----------------

    private fun readWebp(b: ByteArray, f: RawFindings) {
        var p = 12
        while (p + 8 <= b.size) {
            val type = b.ascii(p, 4)
            val len = b.u32le(p + 4).toInt()
            if (len < 0 || p + 8 + len > b.size) break
            val data = p + 8
            f.segments += FileSegment("${type.trim()} chunk", p, len + 8)
            when (type) {
                "VP8X" -> if (len >= 10) { f.width = b.u24le(data + 4) + 1; f.height = b.u24le(data + 7) + 1 }
                "VP8 " -> if (f.width == null && len >= 10) {
                    f.width = b.u16le(data + 6) and 0x3FFF; f.height = b.u16le(data + 8) and 0x3FFF
                }
                "VP8L" -> if (f.width == null && len >= 5) {
                    val bits = b.u32le(data + 1)
                    f.width = ((bits and 0x3FFF) + 1).toInt(); f.height = (((bits shr 14) and 0x3FFF) + 1).toInt()
                }
                "EXIF" -> {
                    val skip = if (b.startsWithAt(data, "Exif\u0000\u0000".asciiBytes())) 6 else 0
                    f.exifTiff = b.slice(data + skip, len - skip); f.exifOffset = p
                }
                "XMP " -> { f.xmpPackets += String(b, data, len, Charsets.UTF_8); f.xmpOffset = p }
                "C2PA" -> f.jumbf += p to b.slice(data, len)
            }
            p += 8 + len + (len and 1)
        }
    }

    // ---------------- HEIF / AVIF (ISO BMFF) ----------------

    private fun readBmff(b: ByteArray, f: RawFindings) {
        var p = 0
        while (p + 8 <= b.size) {
            var size = b.u32be(p)
            val type = b.ascii(p + 4, 4)
            var header = 8
            if (size == 1L && p + 16 <= b.size) { size = b.u64be(p + 8); header = 16 }
            if (size == 0L) size = (b.size - p).toLong()
            if (size < header || p + size > b.size) break
            f.segments += FileSegment("$type box", p, size.toInt())
            if (type == "uuid" && size >= header + 16 && b.startsWithAt(p + header, C2PA_BMFF_UUID)) {
                var q = p + header + 16 + 4 // uuid + version/flags
                val z = b.indexOfZero(q, (p + size).toInt())
                if (z > 0) {
                    val purpose = b.ascii(q, z - q)
                    q = z + 1
                    if (purpose == "manifest") {
                        q += 8 // merkle offset
                        f.jumbf += p to b.slice(q, (p + size - q).toInt())
                    }
                }
            }
            p += size.toInt()
        }
    }

    // ---------------- GIF ----------------

    private fun readGif(b: ByteArray, f: RawFindings) {
        if (b.size >= 10) { f.width = b.u16le(6); f.height = b.u16le(8) }
        var p = b.indexOf(byteArrayOf(0x21, 0xFE.toByte()))
        var guard = 0
        while (p >= 0 && guard++ < 8) {
            val out = ByteArrayOutputStream()
            var q = p + 2
            while (q < b.size) {
                val n = b.u8(q)
                if (n == 0 || q + 1 + n > b.size) break
                out.write(b, q + 1, n)
                q += 1 + n
            }
            val text = out.toString(Charsets.UTF_8.name()).clean(600)
            if (text.isNotBlank()) f.comments += text
            p = b.indexOf(byteArrayOf(0x21, 0xFE.toByte()), q)
        }
    }

    private fun findLooseExif(b: ByteArray, f: RawFindings) {
        val marker = "Exif\u0000\u0000".asciiBytes()
        val at = b.indexOf(marker)
        if (at >= 0 && at + 14 < b.size) {
            val h = b.ascii(at + 6, 4)
            if (h == "II*\u0000" || h == "MM\u0000*") {
                f.exifTiff = b.copyOfRange(at + 6, b.size)
                f.exifOffset = at
            }
        }
    }
}
