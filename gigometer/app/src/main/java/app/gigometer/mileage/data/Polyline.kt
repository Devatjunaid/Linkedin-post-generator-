package app.gigometer.mileage.data

import kotlin.math.roundToLong

data class GeoPoint(val lat: Double, val lng: Double)

/** Google's encoded polyline algorithm (5-decimal precision) — compact route storage. */
object Polyline {
    fun encode(points: List<GeoPoint>): String {
        val sb = StringBuilder()
        var prevLat = 0L
        var prevLng = 0L
        for (p in points) {
            val lat = (p.lat * 1e5).roundToLong()
            val lng = (p.lng * 1e5).roundToLong()
            encodeValue(lat - prevLat, sb)
            encodeValue(lng - prevLng, sb)
            prevLat = lat
            prevLng = lng
        }
        return sb.toString()
    }

    private fun encodeValue(value: Long, sb: StringBuilder) {
        var v = if (value < 0) (value shl 1).inv() else value shl 1
        while (v >= 0x20) {
            sb.append(((0x20 or (v and 0x1f).toInt()) + 63).toChar())
            v = v shr 5
        }
        sb.append((v + 63).toInt().toChar())
    }

    fun decode(encoded: String): List<GeoPoint> {
        val out = ArrayList<GeoPoint>()
        var index = 0
        var lat = 0L
        var lng = 0L
        while (index < encoded.length) {
            val dLat = decodeValue(encoded, index).also { index = it.second }.first
            if (index > encoded.length) break
            val dLng = decodeValue(encoded, index).also { index = it.second }.first
            lat += dLat
            lng += dLng
            out += GeoPoint(lat / 1e5, lng / 1e5)
        }
        return out
    }

    private fun decodeValue(s: String, start: Int): Pair<Long, Int> {
        var result = 0L
        var shift = 0
        var i = start
        var b: Int
        do {
            if (i >= s.length) return 0L to s.length + 1
            b = s[i++].code - 63
            result = result or ((b and 0x1f).toLong() shl shift)
            shift += 5
        } while (b >= 0x20)
        val value = if (result and 1L != 0L) (result shr 1).inv() else result shr 1
        return value to i
    }
}
