package com.devatjunaid.isthisai.engine

import java.util.Locale

/** Parsed EXIF (TIFF IFD) tags, keyed by readable tag name. */
data class ExifData(val tags: LinkedHashMap<String, String>, val hasGps: Boolean) {
    val make get() = tags["Make"]
    val model get() = tags["Model"]
    val software get() = tags["Software"]
    val hasExposure get() = tags.containsKey("ExposureTime") || tags.containsKey("FNumber") || tags.containsKey("ISO")
}

internal object Exif {

    private val IFD0_TAGS = mapOf(
        0x010E to "ImageDescription", 0x010F to "Make", 0x0110 to "Model", 0x0131 to "Software",
        0x0132 to "DateTime", 0x013B to "Artist", 0x8298 to "Copyright",
        0x9C9B to "XPTitle", 0x9C9C to "XPComment", 0x9C9D to "XPAuthor", 0x9C9F to "XPSubject",
    )
    private val EXIF_TAGS = mapOf(
        0x829A to "ExposureTime", 0x829D to "FNumber", 0x8827 to "ISO", 0x9003 to "DateTimeOriginal",
        0x9010 to "OffsetTime", 0x920A to "FocalLength", 0x9286 to "UserComment", 0xA002 to "PixelWidth",
        0xA003 to "PixelHeight", 0xA420 to "ImageUniqueID", 0xA431 to "BodySerialNumber",
        0xA433 to "LensMake", 0xA434 to "LensModel",
    )

    /** [tiff] must start with the TIFF header ("II*\0" or "MM\0*"). */
    fun parse(tiff: ByteArray): ExifData? {
        if (tiff.size < 8) return null
        val le = when (tiff.ascii(0, 2)) { "II" -> true; "MM" -> false; else -> return null }
        val r = Reader(tiff, le)
        if (r.u16(2) != 42) return null
        val tags = LinkedHashMap<String, String>()
        var hasGps = false
        val ifd0 = r.u32(4).toInt()
        val pointers = HashMap<Int, Int>()
        readIfd(r, ifd0, IFD0_TAGS, tags, pointers)
        pointers[0x8769]?.let { readIfd(r, it, EXIF_TAGS, tags, HashMap()) }
        pointers[0x8825]?.let { gps ->
            if (gps in 0 until tiff.size - 2 && r.u16(gps) > 0) hasGps = true
        }
        return if (tags.isEmpty() && !hasGps) null else ExifData(tags, hasGps)
    }

    private class Reader(val b: ByteArray, val le: Boolean) {
        fun u16(i: Int) = if (le) b.u16le(i) else b.u16be(i)
        fun u32(i: Int) = if (le) b.u32le(i) else b.u32be(i)
    }

    private fun readIfd(r: Reader, offset: Int, names: Map<Int, String>, out: MutableMap<String, String>, pointers: MutableMap<Int, Int>) {
        val b = r.b
        if (offset < 8 || offset + 2 > b.size) return
        val count = r.u16(offset)
        if (count > 1000) return
        for (k in 0 until count) {
            val e = offset + 2 + k * 12
            if (e + 12 > b.size) return
            val tag = r.u16(e)
            val type = r.u16(e + 2)
            val n = r.u32(e + 4)
            if (tag == 0x8769 || tag == 0x8825) {
                pointers[tag] = r.u32(e + 8).toInt()
                continue
            }
            val name = names[tag] ?: continue
            val unit = when (type) { 1, 2, 6, 7 -> 1; 3, 8 -> 2; 4, 9, 11 -> 4; 5, 10, 12 -> 8; else -> continue }
            val total = n * unit
            if (total <= 0 || total > 65536) continue
            val dataAt = if (total <= 4) e + 8 else r.u32(e + 8).toInt()
            if (dataAt < 0 || dataAt + total > b.size) continue
            val value = formatValue(r, tag, type, dataAt, n.toInt(), total.toInt()) ?: continue
            if (value.isNotBlank()) out[name] = value.clean(600)
        }
    }

    private fun formatValue(r: Reader, tag: Int, type: Int, at: Int, n: Int, total: Int): String? {
        val b = r.b
        return when {
            tag == 0x9286 -> userComment(b.slice(at, total))
            tag in 0x9C9B..0x9C9F -> String(b, at, total, Charsets.UTF_16LE).trimEnd('\u0000')
            type == 2 -> String(b, at, total, Charsets.UTF_8).trimEnd('\u0000')
            type == 3 -> r.u16(at).toString()
            type == 4 -> r.u32(at).toString()
            type == 5 || type == 10 -> {
                val num = r.u32(at)
                val den = r.u32(at + 4)
                if (den == 0L) null else when (tag) {
                    0x829A -> if (num < den && num > 0) "1/${den / num}s" else "${num.toDouble() / den}s"
                    0x829D -> "f/" + "%.1f".format(Locale.US, num.toDouble() / den)
                    0x920A -> "%.1f".format(Locale.US, num.toDouble() / den) + " mm"
                    else -> "%.2f".format(Locale.US, num.toDouble() / den)
                }
            }
            type == 7 -> String(b, at, total, Charsets.ISO_8859_1).trimEnd('\u0000').takeIf { s -> s.all { it.code in 32..126 } }
            else -> null
        }
    }

    /** UserComment starts with an 8-byte character-code prefix. */
    private fun userComment(v: ByteArray): String? {
        if (v.size <= 8) return null
        val prefix = v.ascii(0, 8)
        val body = v.copyOfRange(8, v.size)
        return when {
            prefix.startsWith("UNICODE") -> {
                // Writers disagree on byte order; pick the one whose zero bytes look like ASCII text.
                val evenZeros = body.indices.count { it % 2 == 0 && body[it].toInt() == 0 }
                val oddZeros = body.indices.count { it % 2 == 1 && body[it].toInt() == 0 }
                String(body, if (evenZeros >= oddZeros) Charsets.UTF_16BE else Charsets.UTF_16LE)
            }
            prefix.startsWith("ASCII") -> String(body, Charsets.UTF_8)
            else -> String(body, Charsets.UTF_8).takeIf { s -> s.count { it.isLetterOrDigit() } > s.length / 3 }
        }?.trimEnd('\u0000', ' ')
    }
}

/** Fields read from an XMP packet. Values keep their original wording. */
data class XmpData(val fields: LinkedHashMap<String, List<String>>, val history: List<String>, val rawLength: Int) {
    fun first(name: String): String? = fields[name]?.firstOrNull()
}

internal object Xmp {

    /** Local property names we surface, mapped to display names. */
    private val PROPS = listOf(
        "DigitalSourceType" to "DigitalSourceType",
        "AISystemUsed" to "AISystemUsed",
        "AISystemVersionUsed" to "AISystemVersionUsed",
        "AIPromptInformation" to "AIPromptInformation",
        "AIPromptWriterName" to "AIPromptWriterName",
        "CreatorTool" to "CreatorTool",
        "Credit" to "Credit",
        "creator" to "Creator",
        "title" to "Title",
        "description" to "Description",
        "rights" to "Rights",
        "Make" to "Make",
        "Model" to "Model",
        "Software" to "Software",
        "CreateDate" to "CreateDate",
        "DateCreated" to "DateCreated",
        "provenance" to "Provenance",
        "AIGC" to "AIGC",
    )

    fun findPackets(b: ByteArray): List<String> {
        val start = "<x:xmpmeta".asciiBytes()
        val end = "</x:xmpmeta>".asciiBytes()
        val out = ArrayList<String>()
        var from = 0
        while (out.size < 4) {
            val s = b.indexOf(start, from)
            if (s < 0) break
            val e = b.indexOf(end, s)
            if (e < 0) break
            out += String(b, s, e + end.size - s, Charsets.UTF_8)
            from = e + end.size
        }
        return out
    }

    fun parse(packets: List<String>): XmpData? {
        if (packets.isEmpty()) return null
        val xml = packets.joinToString("\n")
        val fields = LinkedHashMap<String, List<String>>()
        for ((local, display) in PROPS) {
            val values = values(xml, local)
            if (values.isNotEmpty()) fields[display] = values
        }
        val history = historyEntries(xml)
        return XmpData(fields, history, xml.length)
    }

    private fun values(xml: String, local: String): List<String> {
        val out = LinkedHashSet<String>()
        val name = "[A-Za-z][\\w.-]*:$local"
        Regex("\\s$name\\s*=\\s*\"([^\"]*)\"").findAll(xml).forEach { out += it.groupValues[1] }
        Regex("\\s$name\\s*=\\s*'([^']*)'").findAll(xml).forEach { out += it.groupValues[1] }
        Regex("<$name\\s+rdf:resource\\s*=\\s*\"([^\"]*)\"").findAll(xml).forEach { out += it.groupValues[1] }
        Regex("<$name(?:\\s[^>]*)?>([^<]+)</$name>").findAll(xml).forEach { out += it.groupValues[1] }
        Regex("<$name(?:\\s[^>]*)?>\\s*<rdf:(?:Alt|Seq|Bag)>(.*?)</rdf:(?:Alt|Seq|Bag)>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml).forEach { m ->
                Regex("<rdf:li[^>]*>([^<]*)</rdf:li>").findAll(m.groupValues[1]).forEach { out += it.groupValues[1] }
            }
        return out.map { unescape(it).clean(600) }.filter { it.isNotBlank() }
    }

    /** xmpMM:History events as "action · softwareAgent · when". */
    private fun historyEntries(xml: String): List<String> {
        val block = Regex("<xmpMM:History>(.*?)</xmpMM:History>", RegexOption.DOT_MATCHES_ALL).find(xml)?.groupValues?.get(1)
            ?: return emptyList()
        val items = Regex("<rdf:li(.*?)(?:/>|</rdf:li>)", RegexOption.DOT_MATCHES_ALL).findAll(block)
        return items.map { li ->
            val t = li.groupValues[1]
            fun prop(p: String) = Regex("stEvt:$p\\s*=\\s*\"([^\"]*)\"").find(t)?.groupValues?.get(1)
                ?: Regex("<stEvt:$p>([^<]*)</stEvt:$p>").find(t)?.groupValues?.get(1)
            listOfNotNull(prop("action"), prop("softwareAgent"), prop("when")).joinToString(" · ")
        }.filter { it.isNotBlank() }.take(20).toList()
    }

    private fun unescape(s: String) = s.replace("&quot;", "\"").replace("&apos;", "'").replace("&lt;", "<")
        .replace("&gt;", ">").replace("&#xA;", "\n").replace("&#10;", "\n").replace("&amp;", "&")
}

/** IPTC IIM fields from a Photoshop image resource block. */
internal object Iptc {
    private val NAMES = mapOf(
        5 to "ObjectName", 80 to "By-line", 105 to "Headline", 110 to "Credit",
        115 to "Source", 116 to "CopyrightNotice", 120 to "Caption",
    )

    /** [irb] is the APP13 payload after "Photoshop 3.0\0". */
    fun parsePhotoshop(irb: ByteArray): LinkedHashMap<String, String> {
        val out = LinkedHashMap<String, String>()
        var p = 0
        while (p + 12 <= irb.size && irb.ascii(p, 4) == "8BIM") {
            val id = irb.u16be(p + 4)
            val nameLen = irb.u8(p + 6)
            var q = p + 7 + nameLen
            if ((nameLen + 1) % 2 != 0) q++
            if (q + 4 > irb.size) break
            val size = irb.u32be(q).toInt()
            val dataAt = q + 4
            if (size < 0 || dataAt + size > irb.size) break
            if (id == 0x0404) parseIim(irb, dataAt, dataAt + size, out)
            p = dataAt + size + (size and 1)
        }
        return out
    }

    private fun parseIim(b: ByteArray, start: Int, end: Int, out: MutableMap<String, String>) {
        var p = start
        while (p + 5 <= end && b.u8(p) == 0x1C) {
            val record = b.u8(p + 1)
            val dataset = b.u8(p + 2)
            val len = b.u16be(p + 3)
            if (len and 0x8000 != 0) break
            val at = p + 5
            if (at + len > end) break
            if (record == 2) NAMES[dataset]?.let { name ->
                val v = String(b, at, len, Charsets.UTF_8).clean(400)
                if (v.isNotBlank()) out[name] = out[name]?.let { "$it; $v" } ?: v
            }
            p = at + len
        }
    }
}
