package com.dotdash.kids.game

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val LEVELS_PER_WORLD = 20
private const val MIN_EDGE = 11f // never split an edge shorter than this, so dots stay tappable

class Level(
    val index: Int,
    val world: Int,
    val number: Int, // 1..20 inside its world
    val picture: Picture,
    val dots: List<Pt>,
    val egg: Boolean, // every 5th level hatches a Mystery Egg
)

private fun dist(a: Pt, b: Pt) = hypot(a.x - b.x, a.y - b.y)

// Later levels get extra dots on the longest edges so the count keeps climbing.
private fun addDots(pts: List<Pt>, target: Int): List<Pt> {
    val out = pts.toMutableList()
    while (out.size < target) {
        var best = -1
        var bestLen = 0f
        for (i in out.indices) {
            val len = dist(out[i], out[(i + 1) % out.size])
            if (len > bestLen) {
                bestLen = len
                best = i
            }
        }
        if (bestLen < MIN_EDGE * 2) break
        val a = out[best]
        val b = out[(best + 1) % out.size]
        out.add(best + 1, Pt(((a.x + b.x) * 5).roundToInt() / 10f, ((a.y + b.y) * 5).roundToInt() / 10f))
    }
    return out
}

object Levels {
    val all: List<Level> = Pictures.worlds.flatMapIndexed { w, world ->
        world.pictures.sortedBy { it.pts.size }.mapIndexed { i, pic ->
            val index = w * LEVELS_PER_WORLD + i
            val target = max(pic.pts.size, min(40, 6 + (index * 0.58).roundToInt()))
            Level(index, w, i + 1, pic, addDots(pic.pts, target), (i + 1) % 5 == 0)
        }
    }

    fun inWorld(w: Int) = all.filter { it.world == w }
}
