package com.devatjunaid.dripdropdraw.engine

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Builds every level from a small set of puzzle templates. Each level is generated together
 * with one working solution (the hint), and the unit tests play every level with that hint to
 * prove it can be solved. Difficulty grows slowly with the level number.
 */
object LevelGenerator {

    enum class Template { SLIDE, WALL, FIRE, BALL, TWO_CUPS, UNDER_WALL, DEFLECT, SEESAW, ZIGZAG, FAN, MOVER, SPINNER, ROOF, TWO_FAUCETS, MOVING_CUP }

    /** First level where each puzzle type appears. That level always uses the new type. */
    private val unlockAt = linkedMapOf(
        Template.SLIDE to 1,
        Template.WALL to 4,
        Template.FIRE to 5,
        Template.BALL to 7,
        Template.TWO_CUPS to 9,
        Template.UNDER_WALL to 11,
        Template.DEFLECT to 13,
        Template.SEESAW to 15,
        Template.ZIGZAG to 17,
        Template.FAN to 20,
        Template.MOVER to 24,
        Template.SPINNER to 29,
        Template.ROOF to 34,
        Template.TWO_FAUCETS to 40,
        Template.MOVING_CUP to 47,
    )

    /** Puzzle types whose answer has a different shape get picked more often. */
    private val extraWeight = mapOf(
        Template.UNDER_WALL to 1.6f,
        Template.ZIGZAG to 1.4f,
        Template.TWO_CUPS to 0.6f,
    )

    private enum class Obstacle { WALL, FIRE, DEFLECT, BALL, SEESAW, FAN, MOVER, SPINNER }

    fun templateFor(number: Int, rnd: Random): Template {
        unlockAt.entries.firstOrNull { it.value == number }?.let { return it.key }
        if (number <= 3) return Template.SLIDE
        val open = unlockAt.filter { it.value <= number }.keys.toList()
        // Newer puzzle types are a little more likely, so later levels feel fresh.
        val weights = open.mapIndexed { i, t -> (1f + i * 0.35f) * (extraWeight[t] ?: 1f) }
        var pick = rnd.nextFloat() * weights.sum()
        for (i in open.indices) {
            pick -= weights[i]
            if (pick <= 0f) return open[i]
        }
        return open.last()
    }

    fun generate(number: Int, attempt: Int = 0): LevelDef = Builder(number, attempt).build()

    private class Box(val x0: Float, val y0: Float, val x1: Float, val y1: Float) {
        fun hits(o: Box) = x0 < o.x1 && x1 > o.x0 && y0 < o.y1 && y1 > o.y0
    }

    private class Builder(val number: Int, attempt: Int) {
        val rnd = Random(number * 1009 + attempt * 7919 + 17)
        val d = (number - 1) / 99f
        val world = ((number - 1) / 20).coerceIn(0, 4)

        val faucets = ArrayList<FaucetDef>()
        val cups = ArrayList<CupDef>()
        val walls = ArrayList<WallDef>()
        val balls = ArrayList<BallDef>()
        val seesaws = ArrayList<SeesawDef>()
        val movers = ArrayList<MoverDef>()
        val spinners = ArrayList<SpinnerDef>()
        val fans = ArrayList<FanDef>()
        val fires = ArrayList<FireDef>()
        val hint = ArrayList<List<Pt>>()

        /** Areas already used by something, so new things do not overlap. */
        val taken = ArrayList<Box>()
        var side = 1

        fun r(a: Float, b: Float) = a + rnd.nextFloat() * (b - a)
        fun lerp(a: Float, b: Float) = a + (b - a) * d

        fun build(): LevelDef {
            val template = templateFor(number, rnd)
            side = when (number) {
                1 -> 1
                2 -> -1
                else -> if (rnd.nextBoolean()) 1 else -1
            }
            when (template) {
                Template.TWO_CUPS -> twoCups()
                Template.TWO_FAUCETS -> twoFaucets()
                Template.UNDER_WALL -> underWall()
                Template.ZIGZAG -> zigzag()
                else -> singleCup(template)
            }
            val gravity = if (world == 4) 0.75f else 1f
            return LevelDef(
                number = number,
                world = world,
                gravityScale = gravity,
                faucets = faucets,
                cups = cups,
                walls = walls,
                balls = balls,
                seesaws = seesaws,
                movers = movers,
                spinners = spinners,
                fans = fans,
                fires = fires,
                hint = hint,
                kind = template.name,
            )
        }

        // ---- building blocks -------------------------------------------------------------

        fun addCup(x: Float, bottom: Float, fillFrac: Float, moveAmp: Float = 0f): CupDef {
            val top = lerp(100f, 80f) + r(-4f, 4f)
            val bottomHalf = top * 0.72f
            val height = lerp(185f, 165f)
            var cup = CupDef(x, bottom, top, bottomHalf, height, 1, moveAmp, r(5.5f, 7.5f))
            val capacity = cup.innerArea(height) / World.AREA_PER_DROP
            cup = cup.copy(target = (capacity * fillFrac).roundToInt().coerceAtLeast(8))
            cups.add(cup)
            // A shelf under the cup.
            val shelfHalf = top + 40f + moveAmp
            walls.add(WallDef(x - shelfHalf, bottom + 24f, x + shelfHalf, bottom + 24f, 14f))
            taken.add(Box(x - shelfHalf - 70f, cup.top - 140f, x + shelfHalf + 70f, World.H + 50f))
            return cup
        }

        fun addFaucet(x: Float, y: Float, count: Int) {
            faucets.add(FaucetDef(x, y, count))
            taken.add(Box(x - 60f, 0f, x + 60f, y + 60f))
        }

        fun fillFrac() = lerp(0.42f, 0.68f) + r(-0.03f, 0.03f)
        fun supplyFactor() = lerp(2.3f, 1.65f)

        /** Lowest point of the hint lines at x (or null if no hint line covers x). */
        fun lineY(x: Float): Float? {
            var best: Float? = null
            for (s in hint) for (i in 1 until s.size) {
                val a = s[i - 1]
                val b = s[i]
                val lo = min(a.x, b.x)
                val hi = max(a.x, b.x)
                if (x < lo || x > hi || hi - lo < 1e-3f) continue
                val y = a.y + (b.y - a.y) * (x - a.x) / (b.x - a.x)
                best = if (best == null) y else max(best, y)
            }
            return best
        }

        /** Lowest hint line y anywhere between x0 and x1 (sampled). */
        fun lineYMax(x0: Float, x1: Float): Float? {
            var best: Float? = null
            var x = x0
            while (x <= x1) {
                lineY(x)?.let { y -> best = best?.let { max(it, y) } ?: y }
                x += 10f
            }
            lineY(x1)?.let { y -> best = best?.let { max(it, y) } ?: y }
            return best
        }

        fun free(b: Box) = b.x0 > 20f && b.x1 < World.W - 20f && b.y1 < World.H + 60f && taken.none { it.hits(b) }

        // ---- templates ---------------------------------------------------------------------

        fun singleCup(t: Template) {
            val fx = 500f - side * r(170f, 290f)
            val fy = r(250f, 340f)
            val cx = (500f + side * r(170f, 270f)).coerceIn(150f, 850f)
            val bottom = r(1390f, 1470f)
            val cup = addCup(cx, bottom, fillFrac(), if (t == Template.MOVING_CUP) r(45f, 70f) else 0f)
            addFaucet(fx, fy, (cup.target * supplyFactor()).roundToInt() + 15)

            val start = Pt(fx - side * 75f, fy + r(140f, 175f))
            val endX = if (t == Template.MOVING_CUP) cx - side * cup.topHalf * 0.3f else cx - side * cup.topHalf * 0.7f
            val end = Pt(endX, cup.top - 45f)
            hint.add(listOf(start, end))

            if (t == Template.ROOF) {
                val y = cup.top - 105f
                walls.add(WallDef(cx - side * cup.topHalf * 0.05f, y, cx + side * (cup.topHalf + 80f), y, 14f, WallStyle.STONE))
            }

            val main = when (t) {
                Template.WALL -> Obstacle.WALL
                Template.FIRE -> Obstacle.FIRE
                Template.BALL -> Obstacle.BALL
                Template.DEFLECT -> Obstacle.DEFLECT
                Template.SEESAW -> Obstacle.SEESAW
                Template.FAN -> Obstacle.FAN
                Template.MOVER -> Obstacle.MOVER
                Template.SPINNER -> Obstacle.SPINNER
                else -> null
            }
            val wanted = ArrayList<Obstacle>()
            if (main != null) wanted.add(main)
            val extra = (d * 2.6f + rnd.nextFloat() * 0.8f).toInt()
            val pool = Obstacle.entries.filter { obstacleUnlocked(it) }
            repeat(extra) { wanted.add(pool.random(rnd)) }
            for (o in wanted) place(o, fx, cx)
            addFloatingBlocks(listOf(fx))
        }

        fun obstacleUnlocked(o: Obstacle): Boolean {
            val t = when (o) {
                Obstacle.WALL -> Template.WALL
                Obstacle.FIRE -> Template.FIRE
                Obstacle.BALL -> Template.BALL
                Obstacle.DEFLECT -> Template.DEFLECT
                Obstacle.SEESAW -> Template.SEESAW
                Obstacle.FAN -> Template.FAN
                Obstacle.MOVER -> Template.MOVER
                Obstacle.SPINNER -> Template.SPINNER
            }
            return unlockAt.getValue(t) < number
        }

        fun twoCups() {
            val fx = 500f + r(-15f, 15f)
            val fy = r(250f, 320f)
            val spread = r(270f, 320f)
            val c1 = addCup(500f - spread, r(1390f, 1460f), fillFrac() * 0.8f)
            val c2 = addCup(500f + spread, r(1390f, 1460f), fillFrac() * 0.8f)
            addFaucet(fx, fy, ((c1.target + c2.target) * supplyFactor() * 1.1f).roundToInt() + 20)
            val apex = Pt(fx, fy + r(150f, 190f))
            val left = Pt(c1.x + c1.topHalf * 0.55f, c1.top - 45f)
            val right = Pt(c2.x - c2.topHalf * 0.55f, c2.top - 45f)
            hint.add(listOf(left, apex, right))
            val extra = (d * 2.2f + rnd.nextFloat() * 0.6f).toInt()
            val pool = Obstacle.entries.filter { obstacleUnlocked(it) && it != Obstacle.SEESAW }
            repeat(extra) { place(pool.random(rnd), fx, if (rnd.nextBoolean()) c1.x else c2.x) }
        }

        /** A wall hangs from the top and blocks the easy path, so the line has to dip under it. */
        fun underWall() {
            val fx = 500f - side * r(230f, 300f)
            val fy = r(230f, 280f)
            val cx = (500f + side * r(220f, 290f)).coerceIn(150f, 850f)
            val cup = addCup(cx, r(1400f, 1470f), fillFrac())
            addFaucet(fx, fy, (cup.target * supplyFactor()).roundToInt() + 15)
            val start = Pt(fx - side * 75f, fy + r(130f, 150f))
            val end = Pt(cx - side * cup.topHalf * 0.7f, cup.top - 45f)
            val wx = start.x + (end.x - start.x) * r(0.42f, 0.52f)
            val straightY = start.y + (end.y - start.y) * (wx - start.x) / (end.x - start.x)
            val bottom = straightY + r(70f, 120f)
            walls.add(WallDef(wx, -40f, wx, bottom, 18f, WallStyle.STONE))
            taken.add(Box(wx - 45f, 0f, wx + 45f, bottom + 45f))
            // One bend that only ever gets flatter, so the water hugs the line instead of jumping off.
            hint.add(listOf(start, Pt(wx - side * 30f, bottom + 60f), end))
            val extra = (d * 2.2f + rnd.nextFloat() * 0.6f).toInt()
            val pool = Obstacle.entries.filter { obstacleUnlocked(it) }
            repeat(extra) { place(pool.random(rnd), fx, cx) }
            addFloatingBlocks(listOf(fx))
        }

        /**
         * The cup is right under the faucet but a lid covers it. Water has to be led out to the
         * side and then back in underneath the lid: two lines.
         */
        fun zigzag() {
            val cx = 500f - side * r(-50f, 230f)
            val fx = cx + r(-35f, 35f)
            val fy = r(215f, 250f)
            val cup = addCup(cx, r(1430f, 1475f), fillFrac() * 0.9f)
            addFaucet(fx, fy, (cup.target * supplyFactor() * 1.15f).roundToInt() + 20)
            val lidY = cup.top - r(220f, 245f)
            val lidHalf = cup.topHalf + 40f
            walls.add(WallDef(cx - lidHalf, lidY, cx + lidHalf, lidY, 16f, WallStyle.STONE))
            taken.add(Box(cx - lidHalf - 30f, lidY - 40f, cx + lidHalf + 30f, lidY + 40f))
            val edge = cx + side * lidHalf
            val s1 = Pt(fx - side * 70f, fy + 135f)
            val e1 = Pt(edge + side * 45f, lidY - r(140f, 170f))
            val s2 = Pt(edge + side * r(230f, 250f), lidY - 40f)
            val e2 = Pt(cx + side * cup.topHalf * 0.5f, cup.top - 45f)
            hint.add(listOf(s1, e1))
            hint.add(listOf(s2, e2))
        }

        fun twoFaucets() {
            val f1x = 500f - side * r(300f, 340f)
            val f2x = 500f - side * r(120f, 160f)
            val cx = (500f + side * r(220f, 280f)).coerceIn(150f, 850f)
            val cup = addCup(cx, r(1400f, 1470f), fillFrac())
            val total = (cup.target * supplyFactor()).roundToInt() + 20
            val start = Pt(f1x - side * 70f, r(400f, 450f))
            val end = Pt(cx - side * cup.topHalf * 0.7f, cup.top - 45f)
            hint.add(listOf(start, end))
            val f1y = start.y - r(140f, 170f)
            val f2y = (lineY(f2x) ?: 600f) - r(150f, 200f)
            addFaucet(f1x, f1y, total / 2)
            addFaucet(f2x, f2y, total - total / 2)
            val extra = (d * 2.2f + rnd.nextFloat() * 0.6f).toInt()
            val pool = Obstacle.entries.filter { obstacleUnlocked(it) }
            repeat(extra) { place(pool.random(rnd), f2x, cx) }
            addFloatingBlocks(listOf(f1x, f2x))
        }

        // ---- obstacles -------------------------------------------------------------------

        /** Tries a few spots under the hint line, between the faucet and the cup. */
        fun place(o: Obstacle, fx: Float, cx: Float) {
            val lo = min(fx, cx)
            val hi = max(fx, cx)
            repeat(30) { tryNo ->
                // The first try goes right under the falling water, which makes the obstacle matter.
                val x = if (tryNo == 0) fx + (if (cx > fx) 1f else -1f) * 20f else r(lo - 60f, hi + 60f)
                if (tryPlace(o, x, if (cx > fx) 1 else -1)) return
            }
        }

        /**
         * Floating stone blocks above the solution line. They never touch the water's path,
         * but they leave less room for drawing, so later levels need a steadier hand.
         */
        fun addFloatingBlocks(streams: List<Float>) {
            val n = (d * 3.2f + rnd.nextFloat() * 0.7f - 0.4f).toInt().coerceAtLeast(0)
            var placed = 0
            var tries = 0
            while (placed < n && tries < 60) {
                tries++
                val x = r(80f, 920f)
                if (streams.any { abs(it - x) < 110f }) continue
                val ly = lineYMin(x - 90f, x + 90f) ?: continue
                val len = r(45f, 75f)
                val y = ly - r(85f, 170f)
                if (y < 200f) continue
                val tilt = r(-25f, 25f)
                val box = Box(x - len - 25f, y - 45f, x + len + 25f, y + 45f)
                if (!free(box)) continue
                walls.add(WallDef(x - len, y - tilt, x + len, y + tilt, 16f, WallStyle.STONE))
                taken.add(box)
                placed++
            }
        }

        /** Highest hint line y between x0 and x1 (sampled). */
        fun lineYMin(x0: Float, x1: Float): Float? {
            var best: Float? = null
            var x = x0
            while (x <= x1) {
                lineY(x)?.let { y -> best = best?.let { min(it, y) } ?: y }
                x += 10f
            }
            return best
        }

        fun tryPlace(o: Obstacle, x: Float, dir: Int): Boolean {
            when (o) {
                Obstacle.WALL -> {
                    val ly = lineY(x) ?: return false
                    val top = ly + r(75f, 120f)
                    if (top > 1350f) return false
                    val box = Box(x - 30f, top - 30f, x + 30f, World.H)
                    if (!free(box)) return false
                    walls.add(WallDef(x, top, x, World.H + 40f, 16f, WallStyle.STONE))
                    taken.add(box)
                }
                Obstacle.FIRE -> {
                    val ly = lineYMax(x - 110f, x + 110f) ?: return false
                    val y = 1470f
                    if (ly > y - 160f) return false
                    val box = Box(x - 110f, y - 120f, x + 110f, World.H)
                    if (!free(box)) return false
                    fires.add(FireDef(x, y - 10f, 62f))
                    walls.add(WallDef(x - 90f, y + 45f, x + 90f, y + 45f, 16f, WallStyle.STONE))
                    taken.add(box)
                }
                Obstacle.DEFLECT -> {
                    val ly = lineYMax(x - 110f, x + 110f) ?: return false
                    val cy = ly + r(150f, 190f)
                    if (cy > 1400f) return false
                    val box = Box(x - 120f, cy - 60f, x + 120f, cy + 60f)
                    if (!free(box)) return false
                    // Low end points away from the cup, so falling water is thrown the wrong way.
                    walls.add(WallDef(x - dir * 100f, cy + 40f, x + dir * 100f, cy - 40f, 14f))
                    taken.add(box)
                }
                Obstacle.BALL -> {
                    val ly = lineYMax(x - 90f, x + 90f) ?: return false
                    val ballR = r(26f, 34f)
                    val shelfY = ly + 2f * ballR + 85f
                    if (shelfY > 1450f) return false
                    val box = Box(x - 100f, shelfY - 2f * ballR - 40f, x + 100f, shelfY + 30f)
                    if (!free(box)) return false
                    walls.add(WallDef(x - 80f, shelfY, x + 80f, shelfY, 14f))
                    // Little bumps keep the ball on its shelf until something pushes it.
                    walls.add(WallDef(x - 80f, shelfY - 18f, x - 80f, shelfY, 8f))
                    walls.add(WallDef(x + 80f, shelfY - 18f, x + 80f, shelfY, 8f))
                    balls.add(BallDef(x + r(-30f, 30f), shelfY - 14f - ballR - 1f, ballR))
                    taken.add(box)
                }
                Obstacle.SEESAW -> {
                    val half = r(100f, 125f)
                    val ly = lineYMax(x - half - 20f, x + half + 20f) ?: return false
                    val py = ly + half + 12f + 70f
                    if (py > 1420f) return false
                    val box = Box(x - half - 20f, py - half - 20f, x + half + 20f, py + 80f)
                    if (!free(box)) return false
                    seesaws.add(SeesawDef(x, py, half, 0f))
                    taken.add(box)
                    if (rnd.nextBoolean()) {
                        val bx = x - dir * half * 0.6f
                        balls.add(BallDef(bx, py - 12f - 26f - 2f, 26f))
                    }
                }
                Obstacle.FAN -> {
                    val w = r(150f, 200f)
                    val x0 = x - w / 2f
                    val ly = lineYMax(x0, x0 + w) ?: return false
                    val top = ly + 60f
                    val bottom = 1500f
                    if (bottom - top < 250f) return false
                    val box = Box(x0 - 50f, top, x0 + w + 50f, bottom)
                    if (!free(box)) return false
                    fans.add(FanDef(x0, top, w, bottom - top, -dir * lerp(2400f, 3200f), 0f))
                    taken.add(box)
                }
                Obstacle.MOVER -> {
                    val half = 70f
                    val amp = r(70f, 110f)
                    val ly = lineYMax(x - half - amp - 30f, x + half + amp + 30f) ?: return false
                    val y = ly + r(110f, 150f)
                    if (y > 1420f) return false
                    val box = Box(x - half - amp - 30f, y - 40f, x + half + amp + 30f, y + 40f)
                    if (!free(box)) return false
                    movers.add(MoverDef(x - half, y, x + half, y, amp, 0f, r(2.6f, 3.6f)))
                    taken.add(box)
                }
                Obstacle.SPINNER -> {
                    val half = r(75f, 95f)
                    val ly = lineYMax(x - half - 20f, x + half + 20f) ?: return false
                    val y = ly + half + 12f + 60f
                    if (y > 1400f) return false
                    val box = Box(x - half - 20f, y - half - 20f, x + half + 20f, y + half + 20f)
                    if (!free(box)) return false
                    val speed = r(1.6f, 2.6f) * (if (rnd.nextBoolean()) 1f else -1f)
                    spinners.add(SpinnerDef(x, y, half, speed))
                    walls.add(WallDef(x, y + 14f, x, y + 15f, 10f, WallStyle.STONE))
                    taken.add(box)
                }
            }
            return true
        }
    }

    /** Used by the tests: how many points a level's hint line has outside the drawable area. */
    fun blockedHintPoints(level: LevelDef): Int {
        val w = World(level)
        var blocked = 0
        for (s in level.hint) for (i in 1 until s.size) {
            val a = s[i - 1]
            val b = s[i]
            val dist = abs(b.x - a.x) + abs(b.y - a.y)
            val n = ceil(dist / 6f).toInt().coerceAtLeast(1)
            for (k in 0..n) {
                val t = k.toFloat() / n
                if (!w.canDrawAt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)) blocked++
            }
        }
        return blocked
    }
}

/** All levels of the game. */
object Levels {
    const val COUNT = 100

    /**
     * For each level, which generator attempt to use. Found by LevelSolvabilityTest so every
     * level is proven solvable with its hint and impossible without drawing.
     */
    private val ATTEMPT = mapOf(25 to 1, 39 to 1, 65 to 1, 66 to 1, 80 to 1)

    fun attemptFor(number: Int) = ATTEMPT[number] ?: 0

    fun get(number: Int): LevelDef = LevelGenerator.generate(number, attemptFor(number))
}
