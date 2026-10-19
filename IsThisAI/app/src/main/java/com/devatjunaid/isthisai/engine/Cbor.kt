package com.devatjunaid.isthisai.engine

import java.io.ByteArrayOutputStream

/** A tagged CBOR value (major type 6). */
internal data class CborTag(val tag: Long, val value: Any?)

/** CBOR "undefined" (simple value 23). */
internal object CborUndefined

/**
 * Minimal CBOR (RFC 8949) decoder. Maps become [LinkedHashMap] with String or Long keys,
 * arrays become [List], byte strings [ByteArray], text [String], integers [Long].
 */
internal class CborReader(private val b: ByteArray, private var pos: Int = 0, private val end: Int = b.size) {

    fun read(): Any? = readItem(0)

    private fun need(n: Int) {
        if (n < 0 || pos + n > end) throw IllegalArgumentException("CBOR truncated at $pos")
    }

    private fun readArgument(info: Int): Long {
        return when {
            info < 24 -> info.toLong()
            info == 24 -> { need(1); b.u8(pos).toLong().also { pos += 1 } }
            info == 25 -> { need(2); b.u16be(pos).toLong().also { pos += 2 } }
            info == 26 -> { need(4); b.u32be(pos).also { pos += 4 } }
            info == 27 -> { need(8); b.u64be(pos).also { pos += 8 } }
            info == 31 -> -1L // indefinite length
            else -> throw IllegalArgumentException("Bad CBOR additional info $info")
        }
    }

    private fun readItem(depth: Int): Any? {
        if (depth > 64) throw IllegalArgumentException("CBOR nesting too deep")
        need(1)
        val initial = b.u8(pos++)
        val major = initial ushr 5
        val info = initial and 0x1F
        return when (major) {
            0 -> readArgument(info)
            1 -> -1L - readArgument(info)
            2 -> readBytes(info, depth)
            3 -> String(readBytes(info, depth), Charsets.UTF_8)
            4 -> {
                val n = readArgument(info)
                val list = ArrayList<Any?>()
                if (n < 0) {
                    while (true) {
                        need(1)
                        if (b.u8(pos) == 0xFF) { pos++; break }
                        list.add(readItem(depth + 1))
                    }
                } else {
                    if (n > end - pos) throw IllegalArgumentException("CBOR array too long")
                    repeat(n.toInt()) { list.add(readItem(depth + 1)) }
                }
                list
            }
            5 -> {
                val n = readArgument(info)
                val map = LinkedHashMap<Any?, Any?>()
                if (n < 0) {
                    while (true) {
                        need(1)
                        if (b.u8(pos) == 0xFF) { pos++; break }
                        val k = readItem(depth + 1)
                        map[k] = readItem(depth + 1)
                    }
                } else {
                    if (n > end - pos) throw IllegalArgumentException("CBOR map too long")
                    repeat(n.toInt()) {
                        val k = readItem(depth + 1)
                        map[k] = readItem(depth + 1)
                    }
                }
                map
            }
            6 -> CborTag(readArgument(info), readItem(depth + 1))
            7 -> when (info) {
                20 -> false
                21 -> true
                22 -> null
                23 -> CborUndefined
                24 -> { need(1); pos += 1; null }
                25 -> { need(2); halfToDouble(b.u16be(pos)).also { pos += 2 } }
                26 -> { need(4); java.lang.Float.intBitsToFloat(b.u32be(pos).toInt()).toDouble().also { pos += 4 } }
                27 -> { need(8); java.lang.Double.longBitsToDouble(b.u64be(pos)).also { pos += 8 } }
                else -> null
            }
            else -> throw IllegalArgumentException("Bad CBOR major type")
        }
    }

    private fun readBytes(info: Int, depth: Int): ByteArray {
        val n = readArgument(info)
        if (n < 0) {
            val out = ByteArrayOutputStream()
            while (true) {
                need(1)
                if (b.u8(pos) == 0xFF) { pos++; break }
                val chunk = readItem(depth + 1)
                when (chunk) {
                    is ByteArray -> out.write(chunk)
                    is String -> out.write(chunk.toByteArray(Charsets.UTF_8))
                    else -> throw IllegalArgumentException("Bad CBOR chunk")
                }
            }
            return out.toByteArray()
        }
        if (n > end - pos) throw IllegalArgumentException("CBOR string too long")
        val len = n.toInt()
        val r = b.copyOfRange(pos, pos + len)
        pos += len
        return r
    }

    private fun halfToDouble(h: Int): Double {
        val exp = (h shr 10) and 0x1F
        val mant = h and 0x3FF
        val v = when (exp) {
            0 -> mant * Math.pow(2.0, -24.0)
            31 -> if (mant == 0) Double.POSITIVE_INFINITY else Double.NaN
            else -> (mant + 1024) * Math.pow(2.0, exp - 25.0)
        }
        return if ((h and 0x8000) != 0) -v else v
    }

    companion object {
        fun decode(bytes: ByteArray): Any? = CborReader(bytes).read()
    }
}

/** Just enough CBOR encoding to build a COSE Sig_structure. */
internal object CborWriter {
    private fun header(out: ByteArrayOutputStream, major: Int, length: Long) {
        val m = major shl 5
        when {
            length < 24 -> out.write(m or length.toInt())
            length < 0x100 -> { out.write(m or 24); out.write(length.toInt()) }
            length < 0x10000 -> { out.write(m or 25); out.write((length shr 8).toInt() and 0xFF); out.write(length.toInt() and 0xFF) }
            length < 0x100000000L -> {
                out.write(m or 26)
                for (s in intArrayOf(24, 16, 8, 0)) out.write((length shr s).toInt() and 0xFF)
            }
            else -> {
                out.write(m or 27)
                for (s in 56 downTo 0 step 8) out.write((length shr s).toInt() and 0xFF)
            }
        }
    }

    fun array(vararg items: Any): ByteArray {
        val out = ByteArrayOutputStream()
        header(out, 4, items.size.toLong())
        for (item in items) {
            when (item) {
                is String -> {
                    val t = item.toByteArray(Charsets.UTF_8)
                    header(out, 3, t.size.toLong()); out.write(t)
                }
                is ByteArray -> { header(out, 2, item.size.toLong()); out.write(item) }
                else -> throw IllegalArgumentException("Unsupported CBOR item")
            }
        }
        return out.toByteArray()
    }
}

// ---- Convenience accessors for decoded CBOR maps ----

internal fun Any?.asMap(): Map<Any?, Any?>? = when (this) {
    is Map<*, *> -> @Suppress("UNCHECKED_CAST") (this as Map<Any?, Any?>)
    is CborTag -> value.asMap()
    else -> null
}

internal fun Any?.asList(): List<Any?>? = when (this) {
    is List<*> -> this
    is CborTag -> value.asList()
    else -> null
}

internal fun Any?.asText(): String? = when (this) {
    is String -> this
    is CborTag -> value.asText()
    is Long, is Double, is Boolean -> toString()
    else -> null
}

internal fun Map<Any?, Any?>.text(key: String): String? = this[key].asText()
internal fun Map<Any?, Any?>.map(key: String): Map<Any?, Any?>? = this[key].asMap()
internal fun Map<Any?, Any?>.list(key: String): List<Any?>? = this[key].asList()
internal fun Map<Any?, Any?>.long(key: Any): Long? = this[key] as? Long
