package com.drawtocompletefun.kids.game

import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Decides whether a finger stroke "is" one of the missing lines. Both shapes are resampled
 * and compared in two directions:
 *  - precision: how much of the stroke lies on the missing line
 *  - coverage: how much of the missing line the stroke went over
 * Kids are not precise, so tolerances are generous, but tighter for tiny lines.
 */
object StrokeMatcher {

    private const val STEP = 1.5f
    private const val MIN_TOL = 7f

    /** Returns a quality score in 0..1, or 0 when the stroke does not match. */
    fun score(stroke: List<Pt>, target: Shape, tolerance: Float): Float {
        if (stroke.size < 2) return 0f
        val targetLen = length(target.points)
        val strokeLen = length(stroke)
        if (strokeLen < maxOf(5f, targetLen * 0.4f)) return 0f
        if (strokeLen > targetLen * 2.6f + 15f) return 0f

        val tol = effectiveTolerance(target, targetLen, tolerance)
        val s = resample(stroke, STEP)
        val t = resample(target.points, STEP)

        val precision = s.count { distToPolyline(it, target.points) <= tol }.toFloat() / s.size
        if (precision < 0.78f) return 0f
        val coverage = t.count { distToPolyline(it, stroke) <= tol }.toFloat() / t.size
        if (coverage < 0.78f) return 0f
        return (precision + coverage) / 2f
    }

    /** Small lines and small circles get a tighter tolerance so neighbours are not confused. */
    fun effectiveTolerance(target: Shape, targetLen: Float, tolerance: Float): Float {
        val sizeBased = if (target.closed) targetLen / (2f * PI.toFloat()) * 0.8f else targetLen * 0.5f
        return sizeBased.coerceIn(MIN_TOL, tolerance)
    }

    /**
     * Best matching missing line, or null. A stroke that fits a line which is already
     * visible better than any missing line is treated as tracing and does not count.
     */
    fun bestMatch(stroke: List<Pt>, missing: List<Shape>, tolerance: Float, visible: List<Shape> = emptyList()): Shape? {
        val best = missing
            .map { it to score(stroke, it, tolerance) }
            .filter { it.second > 0f }
            .maxByOrNull { it.second }
            ?: return null
        val tracing = visible.maxOfOrNull { score(stroke, it, tolerance) } ?: 0f
        return if (tracing > best.second) null else best.first
    }

    fun length(p: List<Pt>): Float {
        var l = 0f
        for (i in 1 until p.size) l += PictureBuilder.dist(p[i - 1], p[i])
        return l
    }

    fun resample(p: List<Pt>, step: Float): List<Pt> {
        if (p.size < 2) return p
        val out = ArrayList<Pt>()
        out += p[0]
        var carry = 0f
        for (i in 1 until p.size) {
            val a = p[i - 1]
            val b = p[i]
            val seg = PictureBuilder.dist(a, b)
            if (seg == 0f) continue
            var d = step - carry
            while (d <= seg) {
                val f = d / seg
                out += Pt(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f)
                d += step
            }
            carry = seg - (d - step)
        }
        if (out.last() != p.last()) out += p.last()
        return out
    }

    fun distToPolyline(q: Pt, poly: List<Pt>): Float {
        if (poly.size == 1) return PictureBuilder.dist(q, poly[0])
        var best = Float.MAX_VALUE
        for (i in 1 until poly.size) {
            val d = distToSegment(q, poly[i - 1], poly[i])
            if (d < best) best = d
        }
        return best
    }

    private fun distToSegment(q: Pt, a: Pt, b: Pt): Float {
        val vx = b.x - a.x
        val vy = b.y - a.y
        val len2 = vx * vx + vy * vy
        if (len2 == 0f) return PictureBuilder.dist(q, a)
        val t = (((q.x - a.x) * vx + (q.y - a.y) * vy) / len2).coerceIn(0f, 1f)
        val px = a.x + t * vx
        val py = a.y + t * vy
        val dx = q.x - px
        val dy = q.y - py
        return sqrt(dx * dx + dy * dy)
    }
}
