package com.devatjunaid.isthisai.engine

/** Small, bounds-checked helpers for reading binary file formats. */

internal fun ByteArray.u8(i: Int): Int = this[i].toInt() and 0xFF

internal fun ByteArray.u16be(i: Int): Int = (u8(i) shl 8) or u8(i + 1)

internal fun ByteArray.u16le(i: Int): Int = u8(i) or (u8(i + 1) shl 8)

internal fun ByteArray.u24le(i: Int): Int = u8(i) or (u8(i + 1) shl 8) or (u8(i + 2) shl 16)

internal fun ByteArray.u32be(i: Int): Long =
    (u8(i).toLong() shl 24) or (u8(i + 1).toLong() shl 16) or (u8(i + 2).toLong() shl 8) or u8(i + 3).toLong()

internal fun ByteArray.u32le(i: Int): Long =
    u8(i).toLong() or (u8(i + 1).toLong() shl 8) or (u8(i + 2).toLong() shl 16) or (u8(i + 3).toLong() shl 24)

internal fun ByteArray.u64be(i: Int): Long {
    var v = 0L
    for (k in 0 until 8) v = (v shl 8) or u8(i + k).toLong()
    return v
}

internal fun ByteArray.ascii(start: Int, length: Int): String {
    if (start < 0 || length <= 0 || start + length > size) return ""
    return String(this, start, length, Charsets.ISO_8859_1)
}

internal fun ByteArray.startsWithAt(offset: Int, prefix: ByteArray): Boolean {
    if (offset < 0 || offset + prefix.size > size) return false
    for (k in prefix.indices) if (this[offset + k] != prefix[k]) return false
    return true
}

internal fun ByteArray.indexOf(pattern: ByteArray, from: Int = 0, to: Int = size): Int {
    if (pattern.isEmpty()) return -1
    val end = minOf(to, size) - pattern.size
    var i = maxOf(0, from)
    val first = pattern[0]
    while (i <= end) {
        if (this[i] == first && startsWithAt(i, pattern)) return i
        i++
    }
    return -1
}

/** Index of the first zero byte at or after [from], or -1. */
internal fun ByteArray.indexOfZero(from: Int, to: Int = size): Int {
    for (i in from until minOf(to, size)) if (this[i].toInt() == 0) return i
    return -1
}

internal fun ByteArray.slice(start: Int, length: Int): ByteArray {
    val s = start.coerceIn(0, size)
    val e = (start.toLong() + length).coerceIn(s.toLong(), size.toLong()).toInt()
    return copyOfRange(s, e)
}

internal fun ByteArray.toHex(max: Int = size, separator: String = " "): String {
    val n = minOf(max, size)
    val sb = StringBuilder(n * 3)
    for (i in 0 until n) {
        if (i > 0) sb.append(separator)
        val v = u8(i)
        sb.append(HEX[v ushr 4]).append(HEX[v and 0xF])
    }
    return sb.toString()
}

private val HEX = "0123456789ABCDEF".toCharArray()

internal fun String.asciiBytes(): ByteArray = toByteArray(Charsets.ISO_8859_1)

internal fun Int.hexOffset(): String = "0x" + toString(16).uppercase().padStart(6, '0')

/** Trims, collapses whitespace and clips long strings so they fit a UI row. */
internal fun String.clean(max: Int = 400): String {
    val t = replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "").trim()
    return if (t.length > max) t.substring(0, max) + "…" else t
}
