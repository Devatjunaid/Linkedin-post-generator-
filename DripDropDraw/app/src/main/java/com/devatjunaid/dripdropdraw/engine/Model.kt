package com.devatjunaid.dripdropdraw.engine

import kotlin.math.sqrt

/** A point in world units. The world is [World.W] wide and [World.H] tall, y grows downward. */
data class Pt(val x: Float, val y: Float)

/** A thick line segment ("capsule"). Every solid thing in the game is built from these. */
class Capsule(ax: Float, ay: Float, bx: Float, by: Float, val r: Float) {
    var ax = ax; private set
    var ay = ay; private set
    var bx = bx; private set
    var by = by; private set

    /** How far this shape moved during the current physics sub-step (kinematic objects only). */
    var vx = 0f
    var vy = 0f

    var minX = 0f; private set
    var maxX = 0f; private set
    var minY = 0f; private set
    var maxY = 0f; private set

    init {
        updateBounds()
    }

    fun set(ax: Float, ay: Float, bx: Float, by: Float) {
        this.ax = ax; this.ay = ay; this.bx = bx; this.by = by
        updateBounds()
    }

    private fun updateBounds() {
        minX = minOf(ax, bx) - r
        maxX = maxOf(ax, bx) + r
        minY = minOf(ay, by) - r
        maxY = maxOf(ay, by) + r
    }

    /** Distance from a point to the core segment (not counting the radius). */
    fun coreDistance(px: Float, py: Float): Float {
        val dx = bx - ax
        val dy = by - ay
        val l2 = dx * dx + dy * dy
        val t = if (l2 > 1e-6f) (((px - ax) * dx + (py - ay) * dy) / l2).coerceIn(0f, 1f) else 0f
        val qx = ax + dx * t - px
        val qy = ay + dy * t - py
        return sqrt(qx * qx + qy * qy)
    }
}

enum class WallStyle { WOOD, STONE }

data class WallDef(
    val ax: Float, val ay: Float, val bx: Float, val by: Float,
    val r: Float = 14f,
    val style: WallStyle = WallStyle.WOOD,
)

data class FaucetDef(val x: Float, val y: Float, val count: Int)

/**
 * A cup. [x] is the centre, [bottom] the inside floor. The cup is a trapezoid that is
 * [topHalf] wide at the rim and [bottomHalf] at the floor. It is happy once [target]
 * drops are inside it.
 */
data class CupDef(
    val x: Float,
    val bottom: Float,
    val topHalf: Float,
    val bottomHalf: Float,
    val height: Float,
    val target: Int,
    val moveAmp: Float = 0f,
    val movePeriod: Float = 6f,
) {
    val top get() = bottom - height

    /** Inner area between the floor and a given height above it. */
    fun innerArea(h: Float): Float {
        val wall = World.CUP_WALL_R
        val b = bottomHalf - wall
        val tAt = b + (topHalf - bottomHalf) * (h / height)
        return (b + tAt) * (h - wall).coerceAtLeast(0f)
    }

    /** Height (above the floor) the water reaches when [target] drops are inside. */
    fun targetLineHeight(): Float {
        val need = target * World.AREA_PER_DROP
        var lo = 0f
        var hi = height
        repeat(30) {
            val mid = (lo + hi) / 2f
            if (innerArea(mid) < need) lo = mid else hi = mid
        }
        return lo
    }
}

data class BallDef(val x: Float, val y: Float, val r: Float)

data class SeesawDef(val x: Float, val y: Float, val halfLen: Float, val angle: Float = 0f)

/** A platform that slides back and forth by ([dx], [dy]) around its base position. */
data class MoverDef(
    val ax: Float, val ay: Float, val bx: Float, val by: Float,
    val dx: Float, val dy: Float, val period: Float,
)

/** A bar spinning around its centre. [speed] is in radians per second. */
data class SpinnerDef(val x: Float, val y: Float, val halfLen: Float, val speed: Float)

/** A fan that blows inside a rectangle. [fx], [fy] is the push in world units per second². */
data class FanDef(val x: Float, val y: Float, val w: Float, val h: Float, val fx: Float, val fy: Float)

data class FireDef(val x: Float, val y: Float, val r: Float)

class LevelDef(
    val number: Int,
    val world: Int,
    val gravityScale: Float,
    val faucets: List<FaucetDef>,
    val cups: List<CupDef>,
    val walls: List<WallDef>,
    val balls: List<BallDef>,
    val seesaws: List<SeesawDef>,
    val movers: List<MoverDef>,
    val spinners: List<SpinnerDef>,
    val fans: List<FanDef>,
    val fires: List<FireDef>,
    /** One possible solution. It is shown by the Hint button and is checked by tests. */
    val hint: List<List<Pt>>,
    /** Name of the puzzle type, for debugging. */
    val kind: String = "",
) {
    val hintLength: Float = hint.sumOf { polylineLength(it).toDouble() }.toFloat()
    val inkLimit: Float
    val threeStarInk: Float
    val twoStarInk: Float

    init {
        val d = (number - 1) / 99f
        inkLimit = hintLength * (2.4f - 0.5f * d) + 120f
        threeStarInk = hintLength * 1.3f + 40f
        twoStarInk = hintLength * 1.75f + 80f
    }
}

fun polylineLength(points: List<Pt>): Float {
    var len = 0f
    for (i in 1 until points.size) {
        val dx = points[i].x - points[i - 1].x
        val dy = points[i].y - points[i - 1].y
        len += sqrt(dx * dx + dy * dy)
    }
    return len
}
