package com.devatjunaid.dripdropdraw.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Things that happened during a step, used for sounds and funny reactions. */
enum class EventType { SPLASH_IN_CUP, SIZZLE, BALL_BONK }

class GameEvent(val type: EventType, val x: Float, val y: Float, val cup: Int = -1)

/** A short-lived visual effect (steam puff, splash). */
class Effect(val x: Float, var y: Float, val kind: Int) {
    var age = 0f
}

/**
 * The physics world: water drops, solid shapes, balls, seesaws, fans and fire.
 *
 * Water is a set of small particles moved with Verlet integration. Particles push each other
 * apart (so water piles up and flows) and are pushed out of every solid shape.
 */
class World(val level: LevelDef) {

    companion object {
        const val W = 1000f
        const val H = 1600f
        const val PR = 9f
        const val LINE_R = 7f
        const val CUP_WALL_R = 9f
        const val SUBSTEPS = 3
        const val STEP = 1f / 60f
        const val MAX_P = 900
        const val BASE_GRAVITY = 1700f
        const val EMIT_RATE = 55f

        /** Average floor area one drop takes up once it settles (measured in tests). */
        const val AREA_PER_DROP = 255f

        private const val CELL = 20f
        private const val GRID_X0 = -120f
        private const val GRID_Y0 = -320f
        private val COLS = ((W + 240f) / CELL).toInt() + 1
        private val ROWS = ((H + 480f) / CELL).toInt() + 1
        private const val MIN_DIST = PR * 2f * 0.85f
        private const val MAX_MOVE = 8f
        private const val BALL_MASS = 30f
        private const val SEESAW_K = 5e-7f
        private const val SEESAW_LIMIT = 0.5f
    }

    private val h = STEP / SUBSTEPS
    private val h2 = h * h
    val gravity = BASE_GRAVITY * level.gravityScale
    private val rnd = Random(level.number * 31 + 7)

    var time = 0f
        private set

    // ---- water -------------------------------------------------------------------------
    val px = FloatArray(MAX_P)
    val py = FloatArray(MAX_P)
    private val ox = FloatArray(MAX_P)
    private val oy = FloatArray(MAX_P)
    private val pCup = IntArray(MAX_P)
    var count = 0
        private set

    class FaucetState(val def: FaucetDef) {
        var emitted = 0
        var acc = 0f
        val done get() = emitted >= def.count
        val left get() = def.count - emitted
    }

    val faucets = level.faucets.map { FaucetState(it) }
    var lastEmitTime = 0f
        private set
    var flowing = false

    // ---- cups --------------------------------------------------------------------------
    inner class CupState(val def: CupDef) {
        var offsetX = 0f
        val x get() = def.x + offsetX
        var fill = 0
        val walls = arrayOf(
            Capsule(0f, 0f, 0f, 0f, CUP_WALL_R),
            Capsule(0f, 0f, 0f, 0f, CUP_WALL_R),
            Capsule(0f, 0f, 0f, 0f, CUP_WALL_R),
        )

        fun place(newOffset: Float) {
            val move = newOffset - offsetX
            offsetX = newOffset
            val cx = x
            val top = def.top
            val b = def.bottom
            walls[0].set(cx - def.topHalf, top, cx - def.bottomHalf, b)
            walls[1].set(cx - def.bottomHalf, b, cx + def.bottomHalf, b)
            walls[2].set(cx + def.bottomHalf, b, cx + def.topHalf, top)
            for (w in walls) {
                w.vx = move; w.vy = 0f
            }
        }

        /** True when a point is inside the cup, below the rim. */
        fun contains(x: Float, y: Float, margin: Float = 0f): Boolean {
            if (y > def.bottom + margin || y < def.top - margin) return false
            val t = (def.bottom - y) / def.height
            val half = def.bottomHalf + (def.topHalf - def.bottomHalf) * t.coerceIn(0f, 1f)
            return abs(x - this.x) < half + margin
        }
    }

    val cups = level.cups.map { CupState(it).also { c -> c.place(0f) } }

    // ---- solids ------------------------------------------------------------------------
    val walls = level.walls.map { Capsule(it.ax, it.ay, it.bx, it.by, it.r) }
    private val faucetBodies = level.faucets.map { Capsule(it.x, it.y - 70f, it.x, it.y - 28f, 22f) }

    /** Lines the player drew; they are solid and never move. */
    val drawn = ArrayList<Capsule>()

    class BallState(val def: BallDef) {
        var x = def.x
        var y = def.y
        var ox = def.x
        var oy = def.y
        var rot = 0f
        val r = def.r
    }

    val balls = level.balls.map { BallState(it) }

    class SeesawState(val def: SeesawDef) {
        var angle = def.angle
        var angVel = 0f
        val plank = Capsule(0f, 0f, 0f, 0f, 12f)
        var torque = 0f

        fun place() {
            val c = cos(angle) * def.halfLen
            val s = sin(angle) * def.halfLen
            plank.set(def.x - c, def.y - s, def.x + c, def.y + s)
        }
    }

    val seesaws = level.seesaws.map { SeesawState(it).also { s -> s.place() } }
    private val seesawStands = level.seesaws.map { Capsule(it.x, it.y + 14f, it.x, it.y + 70f, 10f) }

    class MoverState(val def: MoverDef) {
        val body = Capsule(def.ax, def.ay, def.bx, def.by, 14f)
        var ox = 0f
        var oy = 0f

        fun update(t: Float) {
            val k = sin(2f * PI.toFloat() * t / def.period)
            val nx = def.dx * k
            val ny = def.dy * k
            body.set(def.ax + nx, def.ay + ny, def.bx + nx, def.by + ny)
            body.vx = nx - ox
            body.vy = ny - oy
            ox = nx
            oy = ny
        }
    }

    val movers = level.movers.map { MoverState(it).also { m -> m.update(0f); m.body.vx = 0f; m.body.vy = 0f } }

    class SpinnerState(val def: SpinnerDef) {
        var angle = 0f
        val bar = Capsule(0f, 0f, 0f, 0f, 12f)

        fun update(dt: Float) {
            angle += def.speed * dt
            val c = cos(angle) * def.halfLen
            val s = sin(angle) * def.halfLen
            bar.set(def.x - c, def.y - s, def.x + c, def.y + s)
        }
    }

    val spinners = level.spinners.map { SpinnerState(it).also { s -> s.update(0f) } }

    val effects = ArrayList<Effect>()
    val events = ArrayList<GameEvent>()

    // ---- spatial grid ------------------------------------------------------------------
    private val cellHead = IntArray(COLS * ROWS)
    private val nextInCell = IntArray(MAX_P)

    // ---- contact scratch values (avoid allocations in the hot loop) --------------------
    private var cNx = 0f
    private var cNy = 0f
    private var cDepth = 0f
    private var cQx = 0f
    private var cQy = 0f

    /** Drops still waiting inside the faucets. */
    val dropsLeftInFaucets get() = faucets.sumOf { it.left }

    // =====================================================================================

    fun step() {
        repeat(SUBSTEPS) { subStep() }
        countCups()
        for (e in effects) {
            e.age += STEP
            e.y -= 40f * STEP
        }
        effects.removeAll { it.age > 1.2f }
    }

    private fun subStep() {
        time += h
        for (m in movers) m.update(time)
        for (s in spinners) s.update(h)
        for (c in cups) {
            if (c.def.moveAmp > 0f) {
                c.place(c.def.moveAmp * sin(2f * PI.toFloat() * time / c.def.movePeriod))
            }
        }
        if (flowing) emit()
        integrate()
        buildGrid()
        relaxParticles()
        collideParticles()
        collideBalls()
        for (s in seesaws) {
            s.angVel += s.torque * SEESAW_K / (s.def.halfLen / 100f)
            s.torque = 0f
            s.angVel *= 0.992f
            s.angle += s.angVel
            if (s.angle > SEESAW_LIMIT) { s.angle = SEESAW_LIMIT; s.angVel = 0f }
            if (s.angle < -SEESAW_LIMIT) { s.angle = -SEESAW_LIMIT; s.angVel = 0f }
            s.place()
        }
        removeLost()
    }

    private fun emit() {
        for (f in faucets) {
            if (f.done) continue
            f.acc += EMIT_RATE * h
            while (f.acc >= 1f && !f.done && count < MAX_P) {
                f.acc -= 1f
                val i = count++
                // Two lanes side by side, fast enough that new drops never overlap.
                val lane = if (f.emitted % 2 == 0) -8.5f else 8.5f
                val x = f.def.x + lane + (rnd.nextFloat() - 0.5f) * 1.5f
                val y = f.def.y + 7f
                px[i] = x
                py[i] = y
                ox[i] = x
                oy[i] = y - 480f * h
                pCup[i] = -1
                f.emitted++
                lastEmitTime = time
            }
        }
    }

    private fun integrate() {
        val g = gravity * h2
        for (i in 0 until count) {
            var vx = (px[i] - ox[i]) * 0.999f
            var vy = (py[i] - oy[i]) * 0.999f + g
            for (fan in level.fans) {
                if (px[i] > fan.x && px[i] < fan.x + fan.w && py[i] > fan.y && py[i] < fan.y + fan.h) {
                    vx += fan.fx * h2
                    vy += fan.fy * h2
                }
            }
            val sp = vx * vx + vy * vy
            if (sp > MAX_MOVE * MAX_MOVE) {
                val k = MAX_MOVE / sqrt(sp)
                vx *= k; vy *= k
            }
            ox[i] = px[i]
            oy[i] = py[i]
            px[i] += vx
            py[i] += vy
        }
        for (b in balls) {
            var vx = (b.x - b.ox) * 0.998f
            var vy = (b.y - b.oy) * 0.998f + g
            for (fan in level.fans) {
                if (b.x > fan.x && b.x < fan.x + fan.w && b.y > fan.y && b.y < fan.y + fan.h) {
                    vx += fan.fx * h2 * 0.2f
                    vy += fan.fy * h2 * 0.2f
                }
            }
            val sp = vx * vx + vy * vy
            if (sp > MAX_MOVE * MAX_MOVE) {
                val k = MAX_MOVE / sqrt(sp)
                vx *= k; vy *= k
            }
            b.ox = b.x; b.oy = b.y
            b.x += vx; b.y += vy
            b.rot += vx / b.r
        }
    }

    private fun cellIndex(x: Float, y: Float): Int {
        val cx = ((x - GRID_X0) / CELL).toInt().coerceIn(0, COLS - 1)
        val cy = ((y - GRID_Y0) / CELL).toInt().coerceIn(0, ROWS - 1)
        return cy * COLS + cx
    }

    private fun buildGrid() {
        cellHead.fill(-1)
        for (i in 0 until count) {
            val c = cellIndex(px[i], py[i])
            nextInCell[i] = cellHead[c]
            cellHead[c] = i
        }
    }

    private fun relaxParticles() {
        val minD2 = MIN_DIST * MIN_DIST
        for (i in 0 until count) {
            val gx = ((px[i] - GRID_X0) / CELL).toInt().coerceIn(0, COLS - 1)
            val gy = ((py[i] - GRID_Y0) / CELL).toInt().coerceIn(0, ROWS - 1)
            for (yy in (gy - 1).coerceAtLeast(0)..(gy + 1).coerceAtMost(ROWS - 1)) {
                for (xx in (gx - 1).coerceAtLeast(0)..(gx + 1).coerceAtMost(COLS - 1)) {
                    var j = cellHead[yy * COLS + xx]
                    while (j != -1) {
                        if (j > i) {
                            var dx = px[j] - px[i]
                            var dy = py[j] - py[i]
                            val d2 = dx * dx + dy * dy
                            if (d2 < minD2) {
                                var d = sqrt(d2)
                                if (d < 1e-4f) {
                                    dx = if ((i + j) % 2 == 0) 0.01f else -0.01f
                                    dy = 0.01f
                                    d = sqrt(dx * dx + dy * dy)
                                }
                                val push = (MIN_DIST - d) * 0.3f / d
                                val mx = dx * push
                                val my = dy * push
                                px[i] -= mx; py[i] -= my
                                px[j] += mx; py[j] += my
                                // Damp the velocity the push would otherwise add (keeps water calm).
                                ox[i] -= mx * 0.35f; oy[i] -= my * 0.35f
                                ox[j] += mx * 0.35f; oy[j] += my * 0.35f
                            }
                        }
                        j = nextInCell[j]
                    }
                }
            }
        }
    }

    /** Finds the contact between a circle and a capsule. Fills the c* scratch fields. */
    private fun contact(x: Float, y: Float, rad: Float, c: Capsule): Boolean {
        if (x < c.minX - rad || x > c.maxX + rad || y < c.minY - rad || y > c.maxY + rad) return false
        val dx = c.bx - c.ax
        val dy = c.by - c.ay
        val l2 = dx * dx + dy * dy
        val t = if (l2 > 1e-6f) (((x - c.ax) * dx + (y - c.ay) * dy) / l2).coerceIn(0f, 1f) else 0f
        val qx = c.ax + dx * t
        val qy = c.ay + dy * t
        var nx = x - qx
        var ny = y - qy
        val d2 = nx * nx + ny * ny
        val minD = rad + c.r
        if (d2 >= minD * minD) return false
        val d = sqrt(d2)
        if (d > 1e-5f) {
            nx /= d; ny /= d
        } else {
            val l = sqrt(l2)
            if (l > 1e-5f) {
                nx = dy / l; ny = -dx / l
                if (ny > 0f) { nx = -nx; ny = -ny }
            } else {
                nx = 0f; ny = -1f
            }
        }
        cNx = nx; cNy = ny; cDepth = minD - d; cQx = qx; cQy = qy
        return true
    }

    /** Pushes particle [i] out of the last contact and applies friction/bounce. */
    private fun resolveParticle(i: Int, svx: Float, svy: Float, friction: Float) {
        val vx = px[i] - ox[i] - svx
        val vy = py[i] - oy[i] - svy
        px[i] += cNx * cDepth
        py[i] += cNy * cDepth
        val vn = vx * cNx + vy * cNy
        var nvx = vx
        var nvy = vy
        if (vn < 0f) {
            val tx = vx - vn * cNx
            val ty = vy - vn * cNy
            nvx = tx * (1f - friction) - vn * 0.05f * cNx
            nvy = ty * (1f - friction) - vn * 0.05f * cNy
        }
        ox[i] = px[i] - nvx - svx
        oy[i] = py[i] - nvy - svy
    }

    private fun collideParticles() {
        for (i in 0 until count) {
            for (c in walls) if (contact(px[i], py[i], PR, c)) resolveParticle(i, 0f, 0f, 0.01f)
            for (c in drawn) if (contact(px[i], py[i], PR, c)) resolveParticle(i, 0f, 0f, 0.01f)
            for (c in faucetBodies) if (contact(px[i], py[i], PR, c)) resolveParticle(i, 0f, 0f, 0.01f)
            for (c in seesawStands) if (contact(px[i], py[i], PR, c)) resolveParticle(i, 0f, 0f, 0.01f)
            for (cup in cups) for (c in cup.walls) {
                if (contact(px[i], py[i], PR, c)) resolveParticle(i, c.vx, c.vy, 0.03f)
            }
            for (m in movers) if (contact(px[i], py[i], PR, m.body)) resolveParticle(i, m.body.vx, m.body.vy, 0.05f)
            for (s in spinners) {
                if (contact(px[i], py[i], PR, s.bar)) {
                    val w = s.def.speed * h
                    resolveParticle(i, -w * (cQy - s.def.y), w * (cQx - s.def.x), 0.05f)
                }
            }
            for (s in seesaws) {
                if (contact(px[i], py[i], PR, s.plank)) {
                    s.torque += cross(cQx - s.def.x, cQy - s.def.y, -cNx, -cNy) * cDepth
                    val w = s.angVel
                    resolveParticle(i, -w * (cQy - s.def.y), w * (cQx - s.def.x), 0.02f)
                }
            }
            for (f in level.fires) {
                val dx = px[i] - f.x
                val dy = py[i] - f.y
                if (dx * dx + dy * dy < f.r * f.r) {
                    // Mark for removal by throwing it far away; removeLost() cleans it up.
                    if (rnd.nextFloat() < 0.35f) effects.add(Effect(px[i], py[i], 0))
                    events.add(GameEvent(EventType.SIZZLE, px[i], py[i]))
                    py[i] = H + 1000f
                }
            }
        }
    }

    private fun cross(ax: Float, ay: Float, bx: Float, by: Float) = ax * by - ay * bx

    private fun resolveBall(b: BallState, svx: Float, svy: Float) {
        val vx = b.x - b.ox - svx
        val vy = b.y - b.oy - svy
        b.x += cNx * cDepth
        b.y += cNy * cDepth
        val vn = vx * cNx + vy * cNy
        var nvx = vx
        var nvy = vy
        if (vn < 0f) {
            if (vn < -4f) events.add(GameEvent(EventType.BALL_BONK, b.x, b.y))
            val tx = vx - vn * cNx
            val ty = vy - vn * cNy
            nvx = tx * 0.995f - vn * 0.25f * cNx
            nvy = ty * 0.995f - vn * 0.25f * cNy
        }
        b.ox = b.x - nvx - svx
        b.oy = b.y - nvy - svy
    }

    private fun collideBalls() {
        for (b in balls) {
            for (c in walls) if (contact(b.x, b.y, b.r, c)) resolveBall(b, 0f, 0f)
            for (c in drawn) if (contact(b.x, b.y, b.r, c)) resolveBall(b, 0f, 0f)
            for (c in faucetBodies) if (contact(b.x, b.y, b.r, c)) resolveBall(b, 0f, 0f)
            for (c in seesawStands) if (contact(b.x, b.y, b.r, c)) resolveBall(b, 0f, 0f)
            for (cup in cups) for (c in cup.walls) if (contact(b.x, b.y, b.r, c)) resolveBall(b, c.vx, c.vy)
            for (m in movers) if (contact(b.x, b.y, b.r, m.body)) resolveBall(b, m.body.vx, m.body.vy)
            for (s in spinners) {
                if (contact(b.x, b.y, b.r, s.bar)) {
                    val w = s.def.speed * h
                    resolveBall(b, -w * (cQy - s.def.y), w * (cQx - s.def.x))
                }
            }
            for (s in seesaws) {
                if (contact(b.x, b.y, b.r, s.plank)) {
                    s.torque += cross(cQx - s.def.x, cQy - s.def.y, -cNx, -cNy) * cDepth * BALL_MASS
                    val w = s.angVel
                    resolveBall(b, -w * (cQy - s.def.y), w * (cQx - s.def.x))
                }
            }
            // Balls push water (and water pushes balls a little).
            val gx0 = ((b.x - b.r - PR - GRID_X0) / CELL).toInt().coerceIn(0, COLS - 1)
            val gx1 = ((b.x + b.r + PR - GRID_X0) / CELL).toInt().coerceIn(0, COLS - 1)
            val gy0 = ((b.y - b.r - PR - GRID_Y0) / CELL).toInt().coerceIn(0, ROWS - 1)
            val gy1 = ((b.y + b.r + PR - GRID_Y0) / CELL).toInt().coerceIn(0, ROWS - 1)
            val minD = b.r + PR
            for (yy in gy0..gy1) for (xx in gx0..gx1) {
                var j = cellHead[yy * COLS + xx]
                while (j != -1) {
                    val dx = px[j] - b.x
                    val dy = py[j] - b.y
                    val d2 = dx * dx + dy * dy
                    if (d2 < minD * minD && d2 > 1e-6f) {
                        val d = sqrt(d2)
                        val corr = (minD - d) / d
                        val share = 1f / (1f + BALL_MASS)
                        px[j] += dx * corr * (1f - share)
                        py[j] += dy * corr * (1f - share)
                        b.x -= dx * corr * share
                        b.y -= dy * corr * share
                    }
                    j = nextInCell[j]
                }
            }
        }
        // Ball against ball.
        for (a in 0 until balls.size) for (c in a + 1 until balls.size) {
            val p = balls[a]
            val q = balls[c]
            val dx = q.x - p.x
            val dy = q.y - p.y
            val d = sqrt(dx * dx + dy * dy)
            val minD = p.r + q.r
            if (d < minD && d > 1e-4f) {
                val k = (minD - d) / d * 0.5f
                p.x -= dx * k; p.y -= dy * k
                q.x += dx * k; q.y += dy * k
            }
        }
    }

    private fun removeLost() {
        var i = 0
        while (i < count) {
            val x = px[i]
            val y = py[i]
            if (y > H + 60f || x < -80f || x > W + 80f) {
                val last = count - 1
                px[i] = px[last]; py[i] = py[last]
                ox[i] = ox[last]; oy[i] = oy[last]
                pCup[i] = pCup[last]
                count--
            } else {
                i++
            }
        }
        for (b in balls) {
            if (b.y > H + 300f) {
                b.y = H + 300f; b.oy = b.y
            }
        }
    }

    private fun countCups() {
        for (c in cups) c.fill = 0
        for (i in 0 until count) {
            var inside = -1
            for (k in cups.indices) {
                if (cups[k].contains(px[i], py[i])) {
                    inside = k
                    cups[k].fill++
                    break
                }
            }
            if (inside >= 0 && pCup[i] != inside) {
                events.add(GameEvent(EventType.SPLASH_IN_CUP, px[i], py[i], inside))
            }
            pCup[i] = inside
        }
    }

    // =====================================================================================
    // Drawing support

    /** True when the player is allowed to draw a line point here. */
    fun canDrawAt(x: Float, y: Float): Boolean {
        if (x < LINE_R + 2f || x > W - LINE_R - 2f || y < LINE_R + 2f || y > H - LINE_R - 2f) return false
        val clear = LINE_R + 4f
        for (c in walls) if (c.coreDistance(x, y) < c.r + clear) return false
        for (c in faucetBodies) if (c.coreDistance(x, y) < c.r + clear + 10f) return false
        for (f in faucets) {
            // Keep the spout itself clear so water always comes out.
            if (abs(x - f.def.x) < 30f && y > f.def.y - 80f && y < f.def.y + 40f) return false
        }
        for (c in seesawStands) if (c.coreDistance(x, y) < c.r + clear) return false
        for (cup in cups) {
            if (cup.contains(x, y, 4f)) return false
            for (c in cup.walls) if (c.coreDistance(x, y) < c.r + clear + 2f) return false
            if (cup.def.moveAmp > 0f) {
                val left = cup.def.x - cup.def.moveAmp - cup.def.topHalf - clear - 10f
                val right = cup.def.x + cup.def.moveAmp + cup.def.topHalf + clear + 10f
                if (x in left..right && y > cup.def.top - 20f && y < cup.def.bottom + 20f) return false
            }
        }
        for (s in seesaws) {
            val dx = x - s.def.x
            val dy = y - s.def.y
            val r = s.def.halfLen + 12f + clear
            if (dx * dx + dy * dy < r * r) return false
        }
        for (s in spinners) {
            val dx = x - s.def.x
            val dy = y - s.def.y
            val r = s.def.halfLen + 12f + clear
            if (dx * dx + dy * dy < r * r) return false
        }
        for (m in movers) {
            val d = m.def
            val minX = minOf(d.ax, d.bx) - abs(d.dx) - 14f - clear
            val maxX = maxOf(d.ax, d.bx) + abs(d.dx) + 14f + clear
            val minY = minOf(d.ay, d.by) - abs(d.dy) - 14f - clear
            val maxY = maxOf(d.ay, d.by) + abs(d.dy) + 14f + clear
            if (x in minX..maxX && y in minY..maxY) return false
        }
        for (b in balls) {
            val dx = x - b.x
            val dy = y - b.y
            val r = b.r + clear
            if (dx * dx + dy * dy < r * r) return false
        }
        for (f in level.fires) {
            val dx = x - f.x
            val dy = y - f.y
            val r = f.r + 20f
            if (dx * dx + dy * dy < r * r) return false
        }
        return true
    }

    fun addLine(ax: Float, ay: Float, bx: Float, by: Float) {
        drawn.add(Capsule(ax, ay, bx, by, LINE_R))
    }

    /** Average position of moving water, used to make the cups look at it. */
    fun waterFocus(out: FloatArray): Boolean {
        if (count == 0) return false
        var sx = 0f
        var sy = 0f
        var n = 0
        var i = count - 1
        while (i >= 0 && n < 40) {
            sx += px[i]; sy += py[i]; n++
            i -= 3
        }
        out[0] = sx / n
        out[1] = sy / n
        return true
    }
}
