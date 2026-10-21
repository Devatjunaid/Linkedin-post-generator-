package com.feedthewrongthing.game.game

import com.feedthewrongthing.game.audio.Sfx
import com.feedthewrongthing.game.engine.Camera
import com.feedthewrongthing.game.engine.DrawCtx
import com.feedthewrongthing.game.engine.Meshes
import com.feedthewrongthing.game.engine.RenderFrame
import com.feedthewrongthing.game.models.EnvModels
import com.feedthewrongthing.game.models.ItemModels
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Host services a level needs from the game. */
interface WorldHost : Fx {
    fun hatFor(t: CreatureType): Hat
    /** Records a creature+item discovery. Returns true when it is new in the journal. */
    fun recordDiscovery(c: CreatureType, item: ItemType, r: ReactionType): Boolean
    fun haptic()
    val density: Float
}

class Toast(val title: String, val body: String, val item: ItemType?, val creature: CreatureType?) {
    var age = 0f
}

/** One running level: tray, throws, creatures, goals and stars. */
class PlayWorld(val def: LevelDef, private val host: WorldHost) {
    class Slot(val item: ItemType, var x: Float, var z: Float) {
        var cool = 0f
        var pop = 1f
        var lift = 0f
    }

    class Proj(val item: ItemType) {
        var x = 0f; var y = 0f; var z = 0f
        var vx = 0f; var vy = 0f; var vz = 0f
        var rx = 0f; var ry = 0f; var spinX = 0f; var spinY = 0f
        var age = 0f
        var groundT = 0f
        var onGround = false
        var inWater = false
        var hitSomething = false
        var alive = true
        var noisyDone = false
    }

    val island = def.island
    val goal = def.goal
    val creatures: List<CreatureActor> = def.creatures.map {
        CreatureActor(it.type, it.x, it.z, it.move, it.scale, it.sleep, it.beat).also { a -> a.baseY = it.elev }
    }
    val slots: List<Slot>
    val projectiles = ArrayList<Proj>()
    val popTexts = ArrayList<PopText>()
    val toasts = ArrayList<Toast>()
    val particles get() = host.particles
    val camera = Camera()

    var time = 0f
        private set
    var throws = 0
        private set
    var progress = 0
        private set
    val goalTotal: Int = if (goal is Goal.Everyone) creatures.size else goal.count
    private val everyoneCounts = IntArray(creatures.size)
    private val levelReactions = HashSet<ReactionType>()
    var combo = 0
        private set
    var comboShow = 0f
        private set
    private var lastFeedT = -10f
    var done = false
        private set
    private var doneT = 0f
    val doneElapsed: Float get() = doneT
    var finished = false
        private set
    var newFinds = 0
        private set
    private var shake = 0f
    private var idleT = 0f
    var tutorialStage = if (def.tutorial) 0 else -1
        private set
    var orderIndex = 0
        private set

    // Aiming
    var heldSlot = -1
        private set
    private var downX = 0f
    private var downY = 0f
    private var curX = 0f
    private var curY = 0f
    var aimOk = false
        private set
    private val aimVel = FloatArray(3)
    val preview = FloatArray(3 * 80)
    var previewCount = 0
        private set
    var previewHits = false
        private set

    // Hints
    var hintOn = false
        private set
    var hintSlot = -1
        private set
    var hintDx = 0f
        private set
    var hintDy = 0f
        private set
    val hintDots = FloatArray(3 * 80)
    var hintCount = 0
        private set
    private var hintTimer = 0f

    private val tmp = FloatArray(4)
    private var needInitialHint = def.tutorial
    private val tmp2 = FloatArray(4)
    private var ambientT = 0f
    private val trampSquash = FloatArray(def.gadgets.size)
    private val bumperFlash = FloatArray(def.gadgets.size)

    init {
        val n = def.tray.size
        val spacing = if (def.conveyor) 9.2f / n else min(1.6f, 7.4f / n)
        slots = def.tray.mapIndexed { i, it -> Slot(it, (i - (n - 1) / 2f) * spacing, 1.55f) }
        if (goal is Goal.Requests || goal is Goal.Wrong) creatures.forEach { newRequest(it, null) }
    }

    // ---------------------------------------------------------------- camera

    fun setupCamera(w: Int, h: Int) {
        val sx = if (shake > 0f) (particles.rnd.nextFloat() - 0.5f) * shake else 0f
        val sy = if (shake > 0f) (particles.rnd.nextFloat() - 0.5f) * shake else 0f
        val portrait = h > w
        val ey = if (portrait) 7.6f else 5.9f
        val ez = if (portrait) 10.2f else 8.3f
        camera.setup(sx, ey + sy, ez, 0f, 0.95f, -3.1f, 44f, w, h)
    }

    // ---------------------------------------------------------------- update

    fun update(dt: Float) {
        time += dt
        if (needInitialHint) { needInitialHint = false; computeHint(true) }
        if (shake > 0f) shake = max(0f, shake - dt * 1.5f)
        if (comboShow > 0f) comboShow -= dt
        for (i in trampSquash.indices) { trampSquash[i] = max(0f, trampSquash[i] - dt * 3f); bumperFlash[i] = max(0f, bumperFlash[i] - dt * 3f) }

        // Conveyor / slot refill
        val n = slots.size
        for ((i, s) in slots.withIndex()) {
            if (def.conveyor && heldSlot != i) {
                val span = 9.2f
                val base = i * (span / n) + time * 0.8f
                s.x = ((base % span) + span) % span - span / 2f
            }
            if (s.cool > 0f) {
                s.cool -= dt
                if (s.cool <= 0f) { s.pop = 0f; host.sfx(Sfx.POP, 0.35f, 1.4f) }
            }
            s.pop = min(1f, s.pop + dt * 4f)
            val wantLift = if (heldSlot == i) 1f else 0f
            s.lift += (wantLift - s.lift) * min(1f, dt * 12f)
        }

        // Creatures look at the most interesting thing
        val focus = projectiles.lastOrNull { it.alive }
        for (c in creatures) {
            if (focus != null) { c.lookAtX = focus.x; c.lookAtY = focus.y; c.lookAtZ = focus.z }
            else if (heldSlot >= 0) { val s = slots[heldSlot]; c.lookAtX = s.x; c.lookAtY = 1f; c.lookAtZ = s.z }
            else { c.lookAtX = camera.eye[0]; c.lookAtY = camera.eye[1] * 0.5f; c.lookAtZ = camera.eye[2] }
            c.update(dt, time, host)
        }

        // Projectiles
        val h = dt / SUBSTEPS
        for (p in projectiles) {
            if (!p.alive) continue
            repeat(SUBSTEPS) { if (p.alive) stepProj(p, h, false) }
            p.age += dt
            p.rx += p.spinX * dt; p.ry += p.spinY * dt
            if (p.onGround && hypot(p.vx, p.vz) < 0.7f && abs(p.vy) < 1f) p.groundT += dt else p.groundT = 0f
            if (p.groundT > 0.7f || p.age > 7f || (p.inWater && p.age > 2.5f)) {
                p.alive = false
                particles.poof(p.x, p.y, p.z, 0.6f)
                host.sfx(Sfx.POOF, 0.35f)
            }
            if (p.y < -6f) p.alive = false
        }
        projectiles.removeAll { !it.alive }

        // Anticipation: open mouths when something flies close
        for (p in projectiles) for (c in creatures) {
            if (!c.canEat()) continue
            c.mouthWorld(tmp)
            val d = dist(p.x, p.y, p.z, tmp[0], tmp[1], tmp[2])
            if (d < 4.5f && p.vz < 0.5f) c.anticipate = max(c.anticipate, (1.2f - d / 4.5f).coerceIn(0f, 1f) * 1.4f)
        }

        particles.update(dt)
        ambient(dt)

        val it = popTexts.iterator()
        while (it.hasNext()) { val t = it.next(); t.age += dt; if (t.age > t.life) it.remove() }
        val tt = toasts.iterator()
        while (tt.hasNext()) { val t = tt.next(); t.age += dt; if (t.age > 3.2f) tt.remove() }

        if (heldSlot >= 0) computeAim()

        // Hints
        if (!done) {
            idleT += dt
            if (!hintOn && idleT > 16f) computeHint(true)
            if (hintOn) {
                hintTimer += dt
                if (hintTimer > 1.5f) { hintTimer = 0f; computeHint(false) }
                stepHintSearch(1600)
            }
        }

        // Completion sequence
        if (done) {
            doneT += dt
            if (doneT > 1.3f && doneT - dt <= 1.3f) {
                creatures.forEach { it.celebrate() }
                host.sfx(Sfx.FANFARE)
                for (c in creatures) particles.confetti(c.x, c.y + 2.5f, c.z, 40, 6f)
                host.haptic()
            }
            if (doneT > 1.3f && ((doneT * 3f).toInt() != ((doneT - dt) * 3f).toInt())) {
                particles.confetti((particles.rnd.nextFloat() - 0.5f) * 8f, 5f, -4f, 15, 4f)
            }
            if (doneT > 3.6f) finished = true
        }
    }

    private fun ambient(dt: Float) {
        ambientT += dt
        when (island) {
            Island.SNOW -> if (ambientT > 0.08f) {
                ambientT = 0f
                particles.spawn(PKind.SNOW, (particles.rnd.nextFloat() - 0.5f) * 16f, 7f, -10f + particles.rnd.nextFloat() * 12f,
                    def.wind * 0.8f, -1.2f, 0f, 6f, 0.07f, 0xFFFFFF, 0f, 0f)
            }
            Island.BATH -> if (ambientT > 0.5f) {
                ambientT = 0f
                particles.spawn(PKind.BUBBLE, (particles.rnd.nextFloat() - 0.5f) * 9f, 0.1f, -7f + particles.rnd.nextFloat() * 7f,
                    0f, 0.8f, 0f, 3f, 0.15f + particles.rnd.nextFloat() * 0.15f, 0xE8F8FF, -0.2f, 0.2f)
            }
            Island.MOON -> if (ambientT > 0.4f) {
                ambientT = 0f
                particles.sparkles((particles.rnd.nextFloat() - 0.5f) * 12f, 1f + particles.rnd.nextFloat() * 3f, -8f + particles.rnd.nextFloat() * 6f, 1, 0.1f, 0xFFE68A)
            }
            Island.SOCKS -> if (ambientT > 0.6f) {
                ambientT = 0f
                particles.sparkles((particles.rnd.nextFloat() - 0.5f) * 14f, 1f + particles.rnd.nextFloat() * 4f, -9f + particles.rnd.nextFloat() * 4f, 1, 0.1f, 0xC2B0FF)
            }
            else -> {}
        }
        // Fans puff a little wind streak
        for (g in def.gadgets) if (g.kind == GadgetKind.FAN && particles.rnd.nextFloat() < dt * 8f) {
            particles.spawn(PKind.POOF, g.x + (particles.rnd.nextFloat() - 0.5f) * g.w, 0.6f, g.z + (particles.rnd.nextFloat() - 0.5f) * g.d,
                g.fx * 0.3f, g.fy * 0.3f, g.fz * 0.3f, 0.8f, 0.12f, 0xFFFFFF, 0f, 0.5f)
        }
    }

    // ---------------------------------------------------------------- physics

    private fun dist(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float): Float {
        val dx = ax - bx; val dy = ay - by; val dz = az - bz
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private var lastCatch = -1
    private var simSteps = 0

    private fun eatable(c: CreatureActor, preview: Boolean, t: Float) = if (preview) c.canEatAt(t) else c.canEat()

    private fun mouthOf(c: CreatureActor, preview: Boolean, t: Float, out: FloatArray) {
        if (preview) c.mouthAt(t, out) else c.mouthWorld(out)
    }

    private fun onTable(x: Float, z: Float) = x in EnvModels.MIN_X..EnvModels.MAX_X && z in EnvModels.MIN_Z..EnvModels.MAX_Z

    /**
     * Advances one projectile. In preview mode nothing in the world changes and the
     * return value tells why the flight ended: 0 still flying, 1 hit something, 2 would be eaten.
     */
    private fun stepProj(p: Proj, dt: Float, preview: Boolean, simT: Float = time): Int {
        val r = p.item.radius
        p.vy -= G * dt
        if (def.wind != 0f && p.y > 0.4f && !p.onGround) p.vx += def.wind * dt
        // Fans
        for (g in def.gadgets) if (g.kind == GadgetKind.FAN) {
            if (abs(p.x - g.x) < g.w && abs(p.z - g.z) < g.d && p.y < g.h) {
                p.vx += g.fx * dt; p.vy += g.fy * dt; p.vz += g.fz * dt
            }
        }
        // Gentle aim assist toward open mouths
        if (!p.onGround) for (c in creatures) {
            if (!eatable(c, preview, simT)) continue
            mouthOf(c, preview, simT, tmp)
            val dx = tmp[0] - p.x; val dy = tmp[1] - p.y; val dz = tmp[2] - p.z
            val d = sqrt(dx * dx + dy * dy + dz * dz)
            val reach = 1.6f * c.scale
            if (d < reach && d > 0.01f) {
                val k = 32f * (1f - d / reach)
                p.vx += dx / d * k * dt; p.vy += dy / d * k * dt; p.vz += dz / d * k * dt
            }
        }
        p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt

        // Mouths
        for ((ci, c) in creatures.withIndex()) {
            if (!eatable(c, preview, simT)) continue
            mouthOf(c, preview, simT, tmp)
            val d = dist(p.x, p.y, p.z, tmp[0], tmp[1], tmp[2])
            if (d < 0.55f * c.scale + r * 0.7f) {
                if (preview) { lastCatch = ci; return 2 }
                p.alive = false
                feed(c, ci, p.item)
                return 2
            }
        }
        var hit = 0
        // Creature bodies
        for (c in creatures) {
            // Near an open mouth the snack goes in instead of bouncing off the body.
            if (eatable(c, preview, simT)) {
                mouthOf(c, preview, simT, tmp)
                if (dist(p.x, p.y, p.z, tmp[0], tmp[1], tmp[2]) < 1.35f * (0.55f * c.scale + r * 0.7f)) continue
            }
            if (preview) {
                if (!c.visibleAt(simT)) continue
                c.bodyAt(simT, tmp)
            } else {
                if (c.visible < 0.5f) continue
                c.bodyCenter(tmp)
            }
            val br = c.bodyRadius + r
            val dx = p.x - tmp[0]; val dy = p.y - tmp[1]; val dz = p.z - tmp[2]
            val d = sqrt(dx * dx + dy * dy + dz * dz)
            if (d < br && d > 0.001f) {
                if (preview) return 1
                val nx = dx / d; val ny = dy / d; val nz = dz / d
                p.x = tmp[0] + nx * br; p.y = tmp[1] + ny * br; p.z = tmp[2] + nz * br
                val vn = p.vx * nx + p.vy * ny + p.vz * nz
                if (vn < 0f) {
                    p.vx -= 1.5f * vn * nx; p.vy -= 1.5f * vn * ny; p.vz -= 1.5f * vn * nz
                    if (!p.hitSomething || abs(vn) > 2f) c.bonk(host)
                }
                p.hitSomething = true
                hit = 1
            }
        }
        // Gadgets
        for ((gi, g) in def.gadgets.withIndex()) {
            when (g.kind) {
                GadgetKind.WALL, GadgetKind.PLATFORM -> {
                    val hx = g.w; val hz = g.d
                    val cx = p.x.coerceIn(g.x - hx, g.x + hx)
                    val cy = p.y.coerceIn(0f, g.h)
                    val cz = p.z.coerceIn(g.z - hz, g.z + hz)
                    val dx = p.x - cx; val dy = p.y - cy; val dz = p.z - cz
                    val d = sqrt(dx * dx + dy * dy + dz * dz)
                    if (d < r) {
                        val nx: Float; val ny: Float; val nz: Float
                        if (d > 0.0001f) { nx = dx / d; ny = dy / d; nz = dz / d } else { nx = 0f; ny = 1f; nz = 0f }
                        p.x = cx + nx * r; p.y = cy + ny * r; p.z = cz + nz * r
                        val vn = p.vx * nx + p.vy * ny + p.vz * nz
                        if (vn < 0f) {
                            p.vx -= (1f + p.item.bounce) * vn * nx; p.vy -= (1f + p.item.bounce) * vn * ny; p.vz -= (1f + p.item.bounce) * vn * nz
                            if (abs(vn) > 2f && !preview) host.sfx(Sfx.BONK, 0.4f, 1.3f)
                        }
                        if (ny > 0.7f) {
                            if (preview) return 1
                            p.onGround = true; p.vx *= 0.96f; p.vz *= 0.96f
                        }
                        hit = if (preview) 3 else 1
                    }
                }
                GadgetKind.TRAMPOLINE -> {
                    val d2 = hypot(p.x - g.x, p.z - g.z)
                    val top = g.h
                    if (d2 < g.w && p.vy < 0f && p.y - r < top && p.y - r > top - 0.6f) {
                        if (preview) {
                            p.y = top + r; p.vy = max(-p.vy * 1.05f, 11.5f)
                        } else {
                            p.y = top + r; p.vy = max(-p.vy * 1.05f, 11.5f)
                            trampSquash[gi] = 1f
                            host.sfx(Sfx.BOING, 0.7f, 1f + particles.rnd.nextFloat() * 0.2f)
                            particles.sparkles(g.x, top + 0.2f, g.z, 4, 0.5f)
                        }
                    }
                }
                GadgetKind.BUMPER -> {
                    val dx = p.x - g.x; val dz = p.z - g.z
                    val d2 = hypot(dx, dz)
                    if (d2 < g.w + r && p.y < g.h && d2 > 0.001f) {
                        val nx = dx / d2; val nz = dz / d2
                        p.x = g.x + nx * (g.w + r); p.z = g.z + nz * (g.w + r)
                        val vn = p.vx * nx + p.vz * nz
                        if (vn < 0f) {
                            p.vx -= 2.2f * vn * nx; p.vz -= 2.2f * vn * nz
                            p.vx += nx * 1.5f; p.vz += nz * 1.5f
                            if (!preview) {
                                bumperFlash[gi] = 1f
                                host.sfx(Sfx.BOING, 0.6f, 1.5f)
                            }
                        }
                        hit = if (preview) 3 else 1
                    }
                }
                GadgetKind.FAN -> {}
            }
        }
        // Ground / water
        if (onTable(p.x, p.z) && p.y - r < 0f) {
            if (island == Island.BATH) {
                if (preview) return 1
                if (!p.inWater) {
                    p.inWater = true
                    host.sfx(Sfx.SPLASH, 0.6f)
                    particles.burst(PKind.DROP, p.x, 0.1f, p.z, 10, 2.5f, intArrayOf(0xBFE8FF, 0xFFFFFF), 0.08f, 0.7f, 9f, 1.5f)
                    p.vx *= 0.3f; p.vz *= 0.3f
                }
                p.vy += (-(p.y - r * 0.3f) * 40f - p.vy * 4f) * dt + G * dt
                p.onGround = false
                noisyLanding(p)
                return 1
            }
            if (preview) return 1
            p.y = r
            val impact = -p.vy
            p.vy = if (impact > 1.4f) impact * p.item.bounce else 0f
            val fr = if (island == Island.SNOW) 0.985f else 0.86f
            p.vx *= fr; p.vz *= fr
            p.spinX *= 0.7f; p.spinY *= 0.7f
            p.onGround = true
            if (impact > 2.5f) {
                host.sfx(Sfx.BONK, 0.25f + min(0.35f, impact * 0.03f), 1.6f)
                particles.burst(PKind.POOF, p.x, 0.05f, p.z, 3, 1f, intArrayOf(0xFFFFFF), 0.12f, 0.4f, 0f, 0.3f)
            }
            noisyLanding(p)
            hit = 1
        } else if (p.y - r > 0.05f) {
            p.onGround = p.onGround && hit == 1
        }
        return hit
    }

    private fun noisyLanding(p: Proj) {
        if (p.noisyDone) return
        p.noisyDone = true
        if (p.item.noisy) {
            host.sfx(when (p.item) { ItemType.ALARM_CLOCK -> Sfx.ALARM; ItemType.TRUMPET -> Sfx.TOOT; else -> Sfx.QUACK }, 0.7f)
            for (c in creatures) if (c.sleeping && hypot(c.x - p.x, c.z - p.z) < 4.8f) c.wake(host)
        }
    }

    // ---------------------------------------------------------------- feeding & goals

    private fun newRequest(c: CreatureActor, previous: ItemType?) {
        val choices = slots.map { it.item }.distinct().filter { it != previous }
        c.request = if (choices.isEmpty()) previous else choices[particles.rnd.nextInt(choices.size)]
        c.requestPop = 1f
    }

    private fun feed(c: CreatureActor, ci: Int, item: ItemType) {
        val requested = c.request
        val wrongGoal = goal is Goal.Wrong
        val reaction = Surprises.reaction(c.type, item)
        val shrug = wrongGoal && requested == item
        c.eat(item, if (shrug) null else reaction, host)
        if (shrug) c.shrugAt(host)
        host.haptic()
        idleT = 0f
        if (hintOn && tutorialStage != 0) hintOn = false

        // Combo
        combo = if (time - lastFeedT <= COMBO_WINDOW) combo + 1 else 1
        lastFeedT = time
        if (combo >= 2) {
            comboShow = 1.6f
            host.sfx(Sfx.DING, 0.8f, (1f + 0.12f * (combo - 2)).coerceAtMost(1.9f))
        }

        if (!shrug) {
            val isNew = host.recordDiscovery(c.type, item, reaction)
            if (isNew) {
                newFinds++
                toasts.add(Toast("NEW REACTION!", "${c.type.title} + ${item.title} = ${reaction.title}", item, c.type))
                host.sfx(Sfx.SPARKLE, 0.6f)
            }
        }

        if (done) return
        val before = progress
        when (val g = goal) {
            is Goal.Feed -> if (g.target < 0 || g.target == ci) progress++
            is Goal.FeedItem -> if (item == g.item && (g.target < 0 || g.target == ci)) progress++
            is Goal.Requests -> if (requested == item) progress++
            is Goal.Wrong -> if (requested != null && requested != item) progress++
            is Goal.React -> if (reaction == g.type && (g.target < 0 || g.target == ci)) progress++
            is Goal.Discover -> { levelReactions.add(reaction); progress = levelReactions.size }
            is Goal.Combo -> progress = max(progress, combo)
            is Goal.Everyone -> {
                everyoneCounts[ci]++
                progress = everyoneCounts.count { it >= g.count }
            }
            is Goal.Order -> {
                if (orderIndex < g.items.size && item == g.items[orderIndex]) { orderIndex++; progress = orderIndex }
                else if (orderIndex < g.items.size) {
                    c.mouthWorld(tmp)
                    popTexts.add(PopText("Next: ${g.items[orderIndex].title}!", tmp[0], tmp[1] + 2.2f, tmp[2], 0x5B8CFF, 1.8f))
                }
            }
            is Goal.Wake -> if (c.sleeper) progress++
            is Goal.Beat -> if (c.beat) progress++
        }
        if (goal is Goal.Requests || goal is Goal.Wrong) newRequest(c, requested)
        if (progress > before) {
            host.sfx(Sfx.STAR, 0.5f, 1f + 0.1f * progress)
            if (tutorialStage == 0) { tutorialStage = 1; hintOn = false }
        }
        if (progress >= goalTotal && !done) {
            done = true
            doneT = 0f
            hintOn = false
        }
    }

    /** 1-3 stars based on how many throws it took. */
    fun stars(): Int {
        val p = def.par
        return when {
            throws <= p -> 3
            throws <= p + max(2, p / 2) -> 2
            else -> 1
        }
    }

    // ---------------------------------------------------------------- input

    fun touchDown(sx: Float, sy: Float): Boolean {
        if (done) return false
        var best = -1
        var bestD = Float.MAX_VALUE
        for ((i, s) in slots.withIndex()) {
            if (s.cool > 0f) continue
            if (!camera.project(s.x, 0.5f, s.z, tmp)) continue
            val ppu = camera.pixelsPerUnit(tmp[2])
            val d = hypot(tmp[0] - sx, tmp[1] - sy)
            val reach = max(56f * host.density, ppu * 0.85f)
            if (d < reach && d < bestD) { best = i; bestD = d }
        }
        if (best < 0) return false
        heldSlot = best
        downX = sx; downY = sy; curX = sx; curY = sy
        aimOk = false
        previewCount = 0
        host.sfx(Sfx.POP, 0.5f, 1.2f)
        return true
    }

    fun touchMove(sx: Float, sy: Float) {
        if (heldSlot < 0) return
        curX = sx; curY = sy
    }

    fun touchUp(sx: Float, sy: Float) {
        if (heldSlot < 0) return
        curX = sx; curY = sy
        computeAim()
        val s = slots[heldSlot]
        if (aimOk && s.cool <= 0f) {
            val p = Proj(s.item)
            p.x = s.x; p.y = START_Y; p.z = s.z
            p.vx = aimVel[0]; p.vy = aimVel[1]; p.vz = aimVel[2]
            p.spinX = 300f + particles.rnd.nextFloat() * 300f
            p.spinY = (particles.rnd.nextFloat() - 0.5f) * 400f
            projectiles.add(p)
            throws++
            s.cool = 0.55f
            s.pop = 0f
            host.sfx(Sfx.WHOOSH, 0.7f, 0.9f + particles.rnd.nextFloat() * 0.3f)
            if (tutorialStage == 0) hintOn = true
            if (tutorialStage != 0) hintOn = false
            idleT = 0f
        }
        heldSlot = -1
        aimOk = false
        previewCount = 0
    }

    fun cancelTouch() { heldSlot = -1; aimOk = false; previewCount = 0 }

    private fun aimFromDrag(dx: Float, dyUp: Float, out: FloatArray): Boolean {
        val dead = 14f * host.density
        if (dyUp < dead) return false
        val h = camera.height.toFloat()
        val p = ((dyUp - dead) / (0.42f * h)).coerceIn(0f, 1f)
        val yaw = atan2(dx, dyUp).coerceIn(-0.8f, 0.8f)
        launch(p, yaw, out)
        return true
    }

    private fun launch(power: Float, yaw: Float, out: FloatArray) {
        val dist = 1.6f + power * 11f
        val th = Math.toRadians(52.0).toFloat()
        val v0 = sqrt(G * dist / sin(2f * th))
        out[0] = sin(yaw) * v0 * cos(th)
        out[1] = v0 * sin(th)
        out[2] = -cos(yaw) * v0 * cos(th)
    }

    private fun computeAim() {
        val s = slots.getOrNull(heldSlot) ?: return
        aimOk = aimFromDrag(curX - downX, downY - curY, aimVel)
        if (!aimOk) { previewCount = 0; return }
        previewHits = simulate(s.x, START_Y, s.z, aimVel, s.item, preview) == 2
    }

    /** Simulates a throw without side effects, filling [out] with dots. Returns the end reason. */
    private fun simulate(x: Float, y: Float, z: Float, vel: FloatArray, item: ItemType, out: FloatArray): Int {
        val p = Proj(item)
        p.x = x; p.y = y; p.z = z
        p.vx = vel[0]; p.vy = vel[1]; p.vz = vel[2]
        var count = 0
        var result = 0
        val dt = 1f / 60f
        lastCatch = -1
        for (step in 0 until 200) {
            simSteps += SUBSTEPS
            var r = 0
            // Same sub-step size as the real simulation so the preview never lies.
            for (k in 0 until SUBSTEPS) {
                r = stepProj(p, dt / SUBSTEPS, true, time + (step * SUBSTEPS + k + 1) * dt / SUBSTEPS)
                if (r == 1 || r == 2) break
            }
            if (step % 3 == 0 && count < out.size / 3) {
                out[count * 3] = p.x; out[count * 3 + 1] = p.y; out[count * 3 + 2] = p.z
                count++
            }
            if (r == 1 || r == 2) { result = r; break }
            if (p.y < -3f) break
        }
        if (out === preview) previewCount = count else if (out === hintDots) hintCount = count
        return result
    }

    // ---------------------------------------------------------------- hints

    fun requestHint() {
        computeHint(true)
        host.sfx(Sfx.SPARKLE, 0.6f)
    }

    private fun targetCreature(): Int {
        val g = goal
        if (g is Goal.React && g.target >= 0) return g.target
        if (g is Goal.React) {
            val tray = slots.map { it.item }
            val able = creatures.indices.filter { i -> tray.any { Surprises.reaction(creatures[i].type, it) == g.type } }
            val idx = able.firstOrNull { creatures[it].canEat() } ?: able.firstOrNull()
            if (idx != null) return idx
        }
        if (g is Goal.Feed && g.target >= 0) return g.target
        if (g is Goal.Everyone) {
            val idx = everyoneCounts.indices.firstOrNull { everyoneCounts[it] < g.count && creatures[it].canEat() }
            if (idx != null) return idx
        }
        if (g is Goal.Wake) {
            val idx = creatures.indices.firstOrNull { creatures[it].sleeper && !creatures[it].sleeping }
            if (idx != null) return idx
        }
        val awake = creatures.indices.filter { creatures[it].canEat() }
        return awake.minByOrNull { abs(creatures[it].x) } ?: 0
    }

    private fun hintItem(ci: Int): ItemType? {
        val c = creatures.getOrNull(ci) ?: return null
        val tray = slots.map { it.item }
        return when (val g = goal) {
            is Goal.FeedItem -> g.item
            is Goal.Requests -> c.request
            is Goal.Wrong -> tray.firstOrNull { it != c.request }
            is Goal.React -> tray.firstOrNull { Surprises.reaction(c.type, it) == g.type }
                ?: tray.firstOrNull { it.reaction == g.type }
            is Goal.Order -> g.items.getOrNull(orderIndex)
            is Goal.Wake -> if (creatures.any { it.sleeping }) tray.firstOrNull { it.noisy } else null
            is Goal.Discover -> tray.firstOrNull { Surprises.reaction(c.type, it) !in levelReactions }
            else -> null
        }
    }

    private val scratchDots = FloatArray(3 * 80)

    /** True when the current hint is a verified throw (useful for tests). */
    var hintVerified = false
        private set

    // Incremental search state: a few test throws per frame so hints never cause a hitch.
    private var searchActive = false
    private var searchYi = 0
    private var searchP = 0f
    private var searchSlot = -1
    private var searchTarget = -1
    private var searchYaw = 0f
    private val searchVel = FloatArray(3)

    private fun computeHint(start: Boolean) {
        if (done) { hintOn = false; return }
        var ci = targetCreature()
        val sleeper = creatures.indexOfFirst { it.sleeping }
        val item = hintItem(ci)
        var slotIdx = if (item != null) slots.indexOfFirst { it.item == item && it.cool <= 0f } else -1
        if (slotIdx < 0) slotIdx = slots.indexOfFirst { it.cool <= 0f }
        if (slotIdx < 0) return
        val s = slots[slotIdx]
        // Noisy items should land next to sleepers; otherwise aim for the mouth.
        val aimAtSleeper = goal is Goal.Wake && sleeper >= 0 && s.item.noisy
        if (aimAtSleeper) ci = sleeper
        val c = creatures.getOrNull(ci) ?: return
        c.mouthWorld(tmp2)
        val tx = tmp2[0]
        val tz = if (aimAtSleeper) c.z + 1.2f else tmp2[2]
        val ty = if (aimAtSleeper) 0f else tmp2[1]
        val baseYaw = atan2(tx - s.x, -(tz - s.z))
        // Quick ballistic estimate first, so a hint shows immediately.
        var bestP = 0.5f
        var bestD = Float.MAX_VALUE
        val v = searchVel
        var p = 0f
        while (p <= 1f) {
            launch(p, baseYaw, v)
            var x = s.x; var y = START_Y; var z = s.z
            var vx = v[0]; var vy = v[1]; var vz = v[2]
            var md = Float.MAX_VALUE
            for (k in 0 until 200) {
                val dt = 1f / 60f
                vy -= G * dt; vx += def.wind * dt
                x += vx * dt; y += vy * dt; z += vz * dt
                val d = dist(x, y, z, tx, ty, tz)
                if (d < md) md = d
                if (y < -1f) break
            }
            if (md < bestD) { bestD = md; bestP = p }
            p += 0.02f
        }
        applyHint(slotIdx, bestP, baseYaw, false)
        if (start) hintOn = true
        // Then refine with real simulated throws over the next frames.
        searchActive = !aimAtSleeper
        searchYi = 0
        searchP = 0f
        searchSlot = slotIdx
        searchTarget = ci
        searchYaw = baseYaw
    }

    private fun applyHint(slotIdx: Int, power: Float, yaw: Float, verified: Boolean) {
        val s = slots[slotIdx]
        hintVerified = verified
        launch(power, yaw, searchVel)
        simulate(s.x, START_Y, s.z, searchVel, s.item, hintDots)
        val dyUp = 14f * host.density + power * 0.42f * camera.height
        hintSlot = slotIdx
        hintDy = dyUp
        hintDx = tan(yaw) * dyUp
    }

    /** Runs test throws until roughly [budget] physics sub-steps have been spent this frame. */
    private fun stepHintSearch(budget: Int) {
        if (!searchActive) return
        val s = slots.getOrNull(searchSlot) ?: run { searchActive = false; return }
        if (s.cool > 0f) { searchActive = false; return }
        simSteps = 0
        while (simSteps < budget && searchYi <= 24) {
            val dy = ((searchYi + 1) / 2) * 0.04f * (if (searchYi % 2 == 0) 1f else -1f)
            launch(searchP, searchYaw + dy, searchVel)
            if (simulate(s.x, START_Y, s.z, searchVel, s.item, scratchDots) == 2 && lastCatch == searchTarget) {
                applyHint(searchSlot, searchP, searchYaw + dy, true)
                searchActive = false
                return
            }
            searchP += 0.05f
            if (searchP > 1.0001f) { searchP = 0f; searchYi++ }
        }
        if (searchYi > 24) searchActive = false
    }

    // ---------------------------------------------------------------- effects host shortcuts

    fun addShake(a: Float) { shake = max(shake, a) }

    // ---------------------------------------------------------------- drawing

    fun draw(ctx: DrawCtx, frame: RenderFrame) {
        EnvModels.lighting(frame, island)
        camera.copyInto(frame)
        EnvModels.drawStage(ctx, island, time)
        drawGadgets(ctx)
        drawTray(ctx)
        for (c in creatures) c.draw(ctx, host.hatFor(c.type))
        for ((i, s) in slots.withIndex()) {
            if (s.cool > 0f) continue
            val pop = easeBack(s.pop)
            val bob = 0.05f * sin(time * 3f + i)
            val lift = s.lift
            val wig = if (hintOn && hintSlot == i) sin(time * 12f) * 12f else 0f
            ctx.shadow(s.x, 0.25f, s.z, 0.4f, 0.3f)
            ctx.push()
            ctx.translate(s.x, 0.62f + bob + lift * 0.45f, s.z)
            ctx.rotateY(time * 25f + i * 50f)
            ctx.rotateZ(wig)
            ctx.scale(pop * (1f + lift * 0.12f))
            ItemModels.draw(ctx, s.item)
            ctx.pop()
            if (hintOn && hintSlot == i) {
                ctx.part(Meshes.torusThin, 0xFFE15C, s.x, 0.3f, s.z, 0.55f + 0.06f * sin(time * 8f), 1f, 0.55f + 0.06f * sin(time * 8f),
                    emissive = 0.9f, mode = RenderFrame.UNLIT)
            }
        }
        for (p in projectiles) {
            val h = p.y.coerceAtLeast(0f)
            if (onTable(p.x, p.z)) ctx.shadow(p.x, 0f, p.z, p.item.radius * (1f - min(0.6f, h * 0.06f)), (0.35f - h * 0.03f).coerceIn(0.06f, 0.35f))
            ctx.push()
            ctx.translate(p.x, p.y, p.z)
            ctx.rotateY(p.ry); ctx.rotateX(p.rx)
            ItemModels.draw(ctx, p.item)
            ctx.pop()
        }
        particles.draw(ctx)
        if (heldSlot >= 0 && aimOk) drawDots(ctx, preview, previewCount, if (previewHits) 0xFFD23F else 0xFFFFFF, 1f)
        if (hintOn && heldSlot < 0) drawDots(ctx, hintDots, hintCount, 0x7FE0FF, 0.45f + 0.25f * sin(time * 6f))
    }

    private fun easeBack(t: Float): Float {
        val c = 1.70158f
        val x = t - 1f
        return 1f + (c + 1f) * x * x * x + c * x * x
    }

    private fun drawDots(ctx: DrawCtx, pts: FloatArray, n: Int, color: Int, alpha: Float) {
        for (i in 1 until n) {
            val s = 0.09f * (1f - i / (n + 6f))
            ctx.part(Meshes.sphereLo, color, pts[i * 3], pts[i * 3 + 1], pts[i * 3 + 2], s, alpha = alpha * (1f - i * 0.6f / n),
                gloss = 0f, rim = 0f, emissive = 0.8f, mode = RenderFrame.UNLIT)
        }
        if (n > 1) {
            val i = n - 1
            ctx.part(Meshes.torusThin, color, pts[i * 3], max(0.03f, pts[i * 3 + 1] - 0.2f), pts[i * 3 + 2], 0.35f, 1f, 0.35f,
                alpha = alpha, emissive = 0.8f, mode = RenderFrame.UNLIT)
        }
    }

    private fun drawTray(ctx: DrawCtx) {
        if (def.conveyor) {
            ctx.part(Meshes.roundCube, 0x3A3A4E, 0f, 0.12f, 1.55f, 4.9f, 0.12f, 0.62f, gloss = 0.6f)
            val n = 12
            for (k in 0 until n) {
                val span = 9.6f
                val x = ((k * span / n + time * 0.8f) % span) - span / 2f
                ctx.part(Meshes.roundCube, 0x5D6A86, x, 0.25f, 1.55f, 0.06f, 0.02f, 0.55f, gloss = 0.5f)
            }
            ctx.part(Meshes.cylinder, 0x9AA6BF, -4.9f, 0.12f, 1.55f, 0.16f, 0.62f, 0.16f, rx = 90f, gloss = 0.9f)
            ctx.part(Meshes.cylinder, 0x9AA6BF, 4.9f, 0.12f, 1.55f, 0.16f, 0.62f, 0.16f, rx = 90f, gloss = 0.9f)
        } else {
            val half = (slots.size * 0.8f + 0.4f).coerceAtMost(4.7f)
            ctx.part(Meshes.roundCube, 0xC98A4B, 0f, 0.12f, 1.55f, half, 0.12f, 0.62f, gloss = 0.35f)
            ctx.part(Meshes.roundCube, 0xE0A866, 0f, 0.2f, 1.55f, half - 0.12f, 0.06f, 0.5f, gloss = 0.3f, pattern = 6, color2 = 0xC98A4B, patternScale = 4f)
        }
    }

    private fun drawGadgets(ctx: DrawCtx) {
        val cols = intArrayOf(0xFF5A5A, 0xFFD23F, 0x3FC48A, 0x5B8CFF, 0x8E6CF0)
        for ((gi, g) in def.gadgets.withIndex()) {
            when (g.kind) {
                GadgetKind.WALL -> {
                    val blocks = max(1, (g.w * 2f / 0.8f).toInt())
                    val rows = max(1, (g.h / 0.8f).toInt())
                    val bw = g.w * 2f / blocks
                    val bh = g.h / rows
                    for (ry in 0 until rows) for (bx in 0 until blocks) {
                        val col = if (island == Island.TOYS) cols[(bx + ry * 2) % cols.size] else if (island == Island.FACTORY) 0x9AA6BF else 0xE0D6C2
                        ctx.part(Meshes.roundCube, col, g.x - g.w + bw * (bx + 0.5f), bh * (ry + 0.5f), g.z, bw * 0.48f, bh * 0.48f, g.d, gloss = 0.45f)
                    }
                    ctx.shadow(g.x, 0f, g.z + 0.3f, g.w * 1.1f, 0.25f)
                }
                GadgetKind.PLATFORM -> {
                    ctx.part(Meshes.roundCube, if (island == Island.TOYS) 0x8E6CF0 else 0xB9A88A, g.x, g.h / 2f, g.z, g.w, g.h / 2f, g.d, gloss = 0.35f)
                    ctx.part(Meshes.roundCube, 0xFFD23F, g.x, g.h - 0.02f, g.z, g.w * 0.9f, 0.04f, g.d * 0.9f, gloss = 0.5f)
                    ctx.shadow(g.x, 0f, g.z, g.w * 1.4f, 0.3f)
                }
                GadgetKind.TRAMPOLINE -> {
                    val sq = trampSquash[gi]
                    for (k in 0 until 4) {
                        val a = k * 1.5708f + 0.785f
                        ctx.part(Meshes.cylinder, 0x5D6A86, g.x + cos(a) * g.w * 0.8f, g.h / 2f, g.z + sin(a) * g.w * 0.8f, 0.05f, g.h / 2f, 0.05f)
                    }
                    ctx.part(Meshes.torus, 0x5B8CFF, g.x, g.h, g.z, g.w, 0.5f, g.w, gloss = 0.6f)
                    ctx.part(Meshes.sphere, 0xFF6BB5, g.x, g.h - 0.04f - sq * 0.15f, g.z, g.w * 0.92f, 0.06f + sq * 0.05f, g.w * 0.92f, gloss = 0.8f)
                    ctx.shadow(g.x, 0f, g.z, g.w * 1.2f, 0.3f)
                }
                GadgetKind.BUMPER -> {
                    val f = bumperFlash[gi]
                    ctx.part(Meshes.cylinder, 0xFF5A5A, g.x, g.h / 2f, g.z, g.w, g.h / 2f, g.w, gloss = 0.8f,
                        pattern = 2, color2 = 0xFFFFFF, patternScale = 1.5f, emissive = f * 0.5f)
                    ctx.part(Meshes.torus, 0xFFD23F, g.x, g.h, g.z, g.w * 1.05f, 0.6f, g.w * 1.05f, gloss = 0.8f, emissive = 0.2f + f)
                    ctx.shadow(g.x, 0f, g.z, g.w * 1.5f, 0.3f)
                }
                GadgetKind.FAN -> {
                    ctx.part(Meshes.cylinder, 0x5D6A86, g.x, 0.12f, g.z, g.w * 0.6f, 0.12f, g.w * 0.6f, gloss = 0.7f)
                    ctx.push()
                    ctx.translate(g.x, 0.5f, g.z)
                    val ang = Math.toDegrees(atan2(g.fx.toDouble(), g.fy.toDouble())).toFloat()
                    ctx.rotateZ(-ang)
                    ctx.part(Meshes.cylinder, 0x5B8CFF, 0f, 0f, 0f, g.w * 0.55f, 0.08f, g.w * 0.55f, gloss = 0.7f)
                    ctx.rotateY(time * 900f)
                    for (k in 0 until 3) {
                        ctx.part(Meshes.roundCube, 0xD7E3FF, 0f, 0.12f, 0f, g.w * 0.48f, 0.015f, 0.12f, ry = k * 120f, gloss = 0.6f)
                    }
                    ctx.pop()
                    ctx.shadow(g.x, 0f, g.z, g.w, 0.25f)
                }
            }
        }
    }

    companion object {
        const val G = 13f
        const val SUBSTEPS = 4
        const val COMBO_WINDOW = 3.5f
        const val START_Y = 0.95f
    }
}
