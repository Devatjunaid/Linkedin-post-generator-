package com.drawtocompletefun.kids.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** A point on the 0..100 drawing board (y grows downwards). */
data class Pt(val x: Float, val y: Float)

/**
 * One drawable line of a picture. Tags that start with "_" are decorations and are
 * never chosen as a missing line.
 */
class Shape(
    val tag: String,
    val points: List<Pt>,
    val closed: Boolean,
    val filled: Boolean = false,
) {
    val isDecor: Boolean get() = tag.startsWith("_")
}

class Picture(val name: String, val shapes: List<Shape>) {
    fun shape(tag: String): Shape? = shapes.firstOrNull { it.tag == tag }
}

fun picture(name: String, build: PictureBuilder.() -> Unit): Picture =
    Picture(name, PictureBuilder().apply(build).build())

class PictureBuilder {
    private val shapes = mutableListOf<Shape>()
    private var decor = 0

    private fun tagOrAuto(tag: String) = tag.ifEmpty { "_${decor++}" }

    private fun add(tag: String, pts: List<Pt>, closed: Boolean, filled: Boolean = false) {
        val t = tagOrAuto(tag)
        require(shapes.none { it.tag == t }) { "Duplicate tag $t" }
        shapes += Shape(t, pts, closed, filled)
    }

    private fun pts(c: Array<out Number>): List<Pt> {
        require(c.size % 2 == 0 && c.size >= 4)
        return (c.indices step 2).map { Pt(c[it].toFloat(), c[it + 1].toFloat()) }
    }

    /** A straight line. */
    fun l(tag: String, x1: Number, y1: Number, x2: Number, y2: Number) =
        add(tag, listOf(Pt(x1.toFloat(), y1.toFloat()), Pt(x2.toFloat(), y2.toFloat())), false)

    /** A single stroke through several points. */
    fun pl(tag: String, vararg c: Number) {
        val p = pts(c)
        val closed = p.size > 2 && dist(p.first(), p.last()) < 0.5f
        add(tag, p, closed)
    }

    /** A closed polygon whose every edge is its own line: prefix1, prefix2, ... */
    fun pg(prefix: String, vararg c: Number) {
        val p = pts(c)
        p.indices.forEach { i -> addEdge(prefix, i, p[i], p[(i + 1) % p.size]) }
    }

    /** An open chain whose every edge is its own line: prefix1, prefix2, ... */
    fun ch(prefix: String, vararg c: Number) {
        val p = pts(c)
        (0 until p.size - 1).forEach { i -> addEdge(prefix, i, p[i], p[i + 1]) }
    }

    private fun addEdge(prefix: String, i: Int, a: Pt, b: Pt) {
        val tag = if (prefix.isEmpty()) "" else "$prefix${i + 1}"
        add(tag, listOf(a, b), false)
    }

    fun c(tag: String, cx: Number, cy: Number, r: Number) = e(tag, cx, cy, r, r)

    fun e(tag: String, cx: Number, cy: Number, rx: Number, ry: Number) =
        add(tag, arcPts(cx.toFloat(), cy.toFloat(), rx.toFloat(), ry.toFloat(), 0f, 360f), true)

    /** Arc in degrees: 0 = right, 90 = down (clockwise on screen). */
    fun a(tag: String, cx: Number, cy: Number, rx: Number, ry: Number, start: Number, sweep: Number) =
        add(
            tag,
            arcPts(cx.toFloat(), cy.toFloat(), rx.toFloat(), ry.toFloat(), start.toFloat(), sweep.toFloat()),
            abs(sweep.toFloat()) >= 360f,
        )

    /** Small filled dot (eyes, noses). Always decoration. */
    fun dot(cx: Number, cy: Number, r: Number) =
        add("", arcPts(cx.toFloat(), cy.toFloat(), r.toFloat(), r.toFloat(), 0f, 360f), true, filled = true)

    /** n lines pointing out from a centre, like sun rays: prefix1..prefixN. */
    fun rays(prefix: String, cx: Number, cy: Number, r1: Number, r2: Number, n: Int, startDeg: Number = -90) {
        repeat(n) { i ->
            val ang = (startDeg.toFloat() + i * 360f / n) * PI.toFloat() / 180f
            val x = cx.toFloat()
            val y = cy.toFloat()
            add(
                if (prefix.isEmpty()) "" else "$prefix${i + 1}",
                listOf(
                    Pt(x + r1.toFloat() * cos(ang), y + r1.toFloat() * sin(ang)),
                    Pt(x + r2.toFloat() * cos(ang), y + r2.toFloat() * sin(ang)),
                ),
                false,
            )
        }
    }

    /** Regular polygon, each edge its own line. */
    fun regular(prefix: String, cx: Number, cy: Number, r: Number, n: Int, rotDeg: Number = -90) {
        val p = (0 until n).map { i ->
            val ang = (rotDeg.toFloat() + i * 360f / n) * PI.toFloat() / 180f
            Pt(cx.toFloat() + r.toFloat() * cos(ang), cy.toFloat() + r.toFloat() * sin(ang))
        }
        p.indices.forEach { i -> addEdge(prefix, i, p[i], p[(i + 1) % n]) }
    }

    /** Five pointed star, 10 edges. */
    fun star(prefix: String, cx: Number, cy: Number, outer: Number, inner: Number) {
        val p = (0 until 10).map { i ->
            val r = if (i % 2 == 0) outer.toFloat() else inner.toFloat()
            val ang = (-90f + i * 36f) * PI.toFloat() / 180f
            Pt(cx.toFloat() + r * cos(ang), cy.toFloat() + r * sin(ang))
        }
        p.indices.forEach { i -> addEdge(prefix, i, p[i], p[(i + 1) % 10]) }
    }

    /** Snowflake arms with two little branches each: arm1..n, brA1..n, brB1..n. */
    fun snowflake(cx: Number, cy: Number, r: Number, n: Int) {
        val x = cx.toFloat()
        val y = cy.toFloat()
        val rr = r.toFloat()
        repeat(n) { i ->
            val ang = (-90f + i * 360f / n) * PI.toFloat() / 180f
            add("arm${i + 1}", listOf(Pt(x, y), Pt(x + rr * cos(ang), y + rr * sin(ang))), false)
            val bx = x + rr * 0.6f * cos(ang)
            val by = y + rr * 0.6f * sin(ang)
            val l = rr * 0.3f
            val a1 = ang + 0.8f
            val a2 = ang - 0.8f
            add("brA${i + 1}", listOf(Pt(bx, by), Pt(bx + l * cos(a1), by + l * sin(a1))), false)
            add("brB${i + 1}", listOf(Pt(bx, by), Pt(bx + l * cos(a2), by + l * sin(a2))), false)
        }
    }

    fun build(): List<Shape> = shapes.toList()

    companion object {
        fun arcPts(cx: Float, cy: Float, rx: Float, ry: Float, start: Float, sweep: Float): List<Pt> {
            val steps = max(8, (abs(sweep) / 7.5f).toInt())
            return (0..steps).map { i ->
                val ang = (start + sweep * i / steps) * PI.toFloat() / 180f
                Pt(cx + rx * cos(ang), cy + ry * sin(ang))
            }
        }

        fun dist(a: Pt, b: Pt): Float {
            val dx = a.x - b.x
            val dy = a.y - b.y
            return kotlin.math.sqrt(dx * dx + dy * dy)
        }
    }
}
