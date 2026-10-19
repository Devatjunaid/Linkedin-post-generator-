package com.devatjunaid.isthisai.engine

/**
 * A JUMBF box (ISO/IEC 19566-5). Superboxes ("jumb") carry a [label] from their description box
 * and [children]; content boxes ("cbor", "json", "bidb", ...) carry a payload range in [source].
 */
internal class JumbfBox(
    val type: String,
    val label: String?,
    val typeUuid: ByteArray?,
    val children: List<JumbfBox>,
    val source: ByteArray,
    val payloadStart: Int,
    val payloadLength: Int,
) {
    val isSuper get() = type == "jumb"

    fun payload(): ByteArray = source.copyOfRange(payloadStart, payloadStart + payloadLength)

    /** First content box of the given type inside this superbox. */
    fun content(type: String): JumbfBox? = children.firstOrNull { !it.isSuper && it.type == type }

    /** First content box that is not a description box. */
    fun firstContent(): JumbfBox? = children.firstOrNull { !it.isSuper && it.type != "jumd" }

    fun child(label: String): JumbfBox? = children.firstOrNull { it.isSuper && it.label == label }

    fun superChildren(): List<JumbfBox> = children.filter { it.isSuper }
}

internal object Jumbf {

    /** Parses consecutive boxes in [b] from [start] to [end]. Malformed tails are ignored. */
    fun parseBoxes(b: ByteArray, start: Int = 0, end: Int = b.size, depth: Int = 0): List<JumbfBox> {
        val out = ArrayList<JumbfBox>()
        var p = start
        while (p + 8 <= end) {
            var len = b.u32be(p)
            val type = b.ascii(p + 4, 4)
            var header = 8
            if (len == 1L) {
                if (p + 16 > end) break
                len = b.u64be(p + 8)
                header = 16
            } else if (len == 0L) {
                len = (end - p).toLong()
            }
            if (len < header || p + len > end) break
            val boxEnd = (p + len).toInt()
            val payloadStart = p + header
            if (type == "jumb" && depth < 16) {
                out.add(parseSuperbox(b, payloadStart, boxEnd, depth))
            } else {
                out.add(JumbfBox(type, null, null, emptyList(), b, payloadStart, boxEnd - payloadStart))
            }
            p = boxEnd
        }
        return out
    }

    private fun parseSuperbox(b: ByteArray, start: Int, end: Int, depth: Int): JumbfBox {
        val children = parseBoxes(b, start, end, depth + 1)
        var label: String? = null
        var uuid: ByteArray? = null
        val desc = children.firstOrNull { it.type == "jumd" }
        if (desc != null && desc.payloadLength >= 17) {
            val d = desc.payloadStart
            uuid = b.copyOfRange(d, d + 16)
            val toggles = b.u8(d + 16)
            if (toggles and 0x02 != 0) {
                val z = b.indexOfZero(d + 17, d + desc.payloadLength)
                val stop = if (z < 0) d + desc.payloadLength else z
                label = String(b, d + 17, stop - (d + 17), Charsets.UTF_8)
            }
        }
        return JumbfBox("jumb", label, uuid, children, b, start, end - start)
    }

    /** Short ASCII type code held in the first four bytes of a JUMBF description UUID. */
    fun uuidCode(uuid: ByteArray?): String = uuid?.ascii(0, 4) ?: ""
}
