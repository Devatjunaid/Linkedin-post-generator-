package com.feedthewrongthing.game.game

import com.feedthewrongthing.game.audio.Sfx
import com.feedthewrongthing.game.engine.DrawCtx
import com.feedthewrongthing.game.engine.Meshes
import com.feedthewrongthing.game.models.CreatureModels
import com.feedthewrongthing.game.models.ItemModels
import com.feedthewrongthing.game.models.Pose
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Effects sink so creatures can make noise, particles and comic words. */
interface Fx {
    val particles: Particles
    fun sfx(s: Sfx, volume: Float = 1f, rate: Float = 1f)
    fun pop(text: String, x: Float, y: Float, z: Float, color: Int)
    fun shake(amount: Float)
}

enum class MoveKind { STATIC, SLIDE, CIRCLE, HOP, PEEK, FLY, BOB, WANDER }

/** How a creature moves around the stage. [a] is the range, [speed] the tempo. */
class Move(val kind: MoveKind, val a: Float = 0f, val speed: Float = 1f, val phase: Float = 0f) {
    companion object {
        val STATIC = Move(MoveKind.STATIC)
    }
}

/** A living creature on stage: movement, mouth, eating and reactions. */
class CreatureActor(
    val type: CreatureType,
    val homeX: Float,
    val homeZ: Float,
    val move: Move,
    val baseScale: Float,
    var sleeper: Boolean,
    val beat: Boolean,
) {
    val pose = Pose()
    var baseY = 0f
    var x = homeX
    var y = 0f
    var z = homeZ
    var sizeMul = 1f
    private var sizeTarget = 1f
    var yOffset = 0f
    private var yVel = 0f
    var visible = 1f
    var sleeping = sleeper
    private var awakeTimer = 0f
    var request: ItemType? = null
    var requestPop = 0f
    var reaction: ReactionType? = null
        private set
    private var rt = 0f
    private var chew = 0f
    private var pendingReaction: ReactionType? = null
    private var pendingItem: ItemType? = null
    private var mouthTarget = 0f
    private var mouthVel = 0f
    private var squash = 1f
    private var squashVel = 0f
    private var blinkTimer = 2f
    private var blinkT = 0f
    private var time = 0f
    private var fxTimer = 0f
    private var jitter = 0f
    var anticipate = 0f
    var lookAtX = 0f
    var lookAtY = 1f
    var lookAtZ = 8f
    var fedCount = 0
    var beatOpen = false
    var celebrating = false
    var meh = 0f
    var hopPhase = 0f
    var shrug = 0f

    val scale: Float get() = baseScale * sizeMul

    init {
        pose.reset()
        if (sleeper) pose.sleepy = 1f
    }

    fun mouthWorld(out: FloatArray) {
        val s = scale
        out[0] = x
        out[1] = y + yOffset + type.mouthY * s * pose.sy + 0.05f * s
        out[2] = z + type.mouthZ * s
    }

    fun bodyCenter(out: FloatArray) {
        val s = scale
        out[0] = x; out[1] = y + yOffset + type.bodyY * s; out[2] = z
    }

    val bodyRadius: Float get() = type.bodyR * scale

    private val mv5 = FloatArray(5)
    private val pv5 = FloatArray(5)
    private var lastTime = 0f

    /** Pure function of time for the movement pattern: x, hover, z, visible (0/1), hop phase. */
    fun moveAt(worldTime: Float, out: FloatArray) {
        val mv = move
        val t = worldTime * mv.speed + mv.phase
        var px = homeX
        var pz = homeZ
        var hover = 0f
        var vis = 1f
        var hop = 0f
        when (mv.kind) {
            MoveKind.STATIC -> {}
            MoveKind.SLIDE -> px = homeX + mv.a * sin(t)
            MoveKind.CIRCLE -> { px = homeX + mv.a * cos(t); pz = homeZ + mv.a * 0.55f * sin(t) }
            MoveKind.HOP -> {
                val cycle = (t / PI.toFloat()) % 2f
                val seg = (t / PI.toFloat()).toInt()
                val dir = if (seg % 2 == 0) 1f else -1f
                val k = cycle % 1f
                px = homeX + dir * mv.a * (k * 2f - 1f)
                hop = k
                hover = abs(sin(k * PI.toFloat() * 3f)) * 0.5f
            }
            MoveKind.PEEK -> vis = if ((t % 4.6f) < 2.8f) 1f else 0f
            MoveKind.FLY -> { px = homeX + mv.a * sin(t); hover = 1.3f + 0.25f * sin(worldTime * 3f) }
            MoveKind.BOB -> { px = homeX + mv.a * sin(t * 0.7f); pz = homeZ + 0.4f * cos(t * 0.5f); hover = 0.12f * sin(worldTime * 2.2f) - 0.25f }
            MoveKind.WANDER -> { px = homeX + mv.a * sin(t) * cos(t * 0.37f); pz = homeZ + mv.a * 0.4f * sin(t * 0.61f) }
        }
        out[0] = px; out[1] = hover; out[2] = pz; out[3] = vis; out[4] = hop
    }

    /** Where the mouth will be at [worldTime], assuming the creature keeps doing what it does. */
    fun mouthAt(worldTime: Float, out: FloatArray) {
        moveAt(worldTime, pv5)
        val s = scale
        val yy = baseY + pv5[1] + (pv5[3] - 1f) * 2.6f * baseScale
        out[0] = pv5[0]
        out[1] = yy + yOffset + type.mouthY * s * pose.sy + 0.05f * s
        out[2] = pv5[2] + type.mouthZ * s
    }

    fun bodyAt(worldTime: Float, out: FloatArray) {
        moveAt(worldTime, pv5)
        val yy = baseY + pv5[1] + (pv5[3] - 1f) * 2.6f * baseScale
        out[0] = pv5[0]; out[1] = yy + yOffset + type.bodyY * scale; out[2] = pv5[2]
    }

    fun visibleAt(worldTime: Float): Boolean {
        moveAt(worldTime, pv5)
        return pv5[3] > 0.5f
    }

    fun beatOpenAt(worldTime: Float): Boolean {
        val period = 60f / 112f * 2f
        return ((worldTime % period) / period) < 0.5f
    }

    /** Prediction of [canEat] at a future moment (used by the aim preview and hints). */
    fun canEatAt(worldTime: Float): Boolean = !sleeping && visibleAt(worldTime) && chew - (worldTime - lastTime) <= 0f &&
        (!beat || beatOpenAt(worldTime)) && reaction != ReactionType.FROZEN && reaction != ReactionType.ROCKET

    fun canEat(): Boolean = !sleeping && visible > 0.7f && chew <= 0f && (!beat || beatOpen) &&
        reaction != ReactionType.FROZEN && reaction != ReactionType.ROCKET

    /** Called when an item lands in the mouth. */
    fun eat(item: ItemType, r: ReactionType?, fx: Fx) {
        chew = 0.5f
        pendingReaction = r
        pendingItem = item
        mouthTarget = 0f
        squashVel -= 3.5f
        fedCount++
        fx.sfx(Sfx.CHOMP, 0.9f, 0.9f + fx.particles.rnd.nextFloat() * 0.25f)
        val m = FloatArray(3)
        mouthWorld(m)
        fx.particles.burst(PKind.CRUMB, m[0], m[1], m[2] + 0.2f, 6, 2.2f, intArrayOf(0xE0A866, 0xFFD23F, 0xFFFFFF), 0.05f, 0.7f, 10f)
        if (reaction != null) endReaction()
    }

    /** The wrong-thing twist: a creature that got exactly what it asked for just shrugs. */
    fun shrugAt(fx: Fx) {
        shrug = 1.4f
        val m = FloatArray(3)
        mouthWorld(m)
        fx.pop("Meh.", m[0], m[1] + 1.1f * scale, m[2], 0x8E6CF0)
        fx.sfx(Sfx.MEH)
    }

    fun bonk(fx: Fx) {
        squashVel -= 2.5f
        jitter = 0.4f
        if (sleeping) {
            wake(fx)
        } else {
            fx.sfx(Sfx.BONK, 0.8f)
            val m = FloatArray(3)
            mouthWorld(m)
            if (fx.particles.rnd.nextFloat() < 0.5f) fx.pop("Hey!", m[0], m[1] + 1.2f * scale, m[2], 0xFF6B6B)
            fx.particles.sparkles(m[0], m[1] + 0.8f * scale, m[2], 4, 0.4f, 0xFFD23F)
        }
    }

    fun wake(fx: Fx) {
        if (!sleeping) { awakeTimer = max(awakeTimer, 7f); return }
        sleeping = false
        awakeTimer = 8f
        squashVel += 4f
        yVel = 4f
        fx.sfx(Sfx.YAY, 0.8f, 1.2f)
        val m = FloatArray(3)
        mouthWorld(m)
        fx.pop("I'm up!", m[0], m[1] + 1.3f * scale, m[2], 0xFFD23F)
    }

    fun celebrate() {
        celebrating = true
        sleeping = false
        if (reaction == null) startReaction(ReactionType.SPARKLE_DANCE, null, null)
    }

    private fun startReaction(r: ReactionType, item: ItemType?, fx: Fx?) {
        endReaction()
        reaction = r
        rt = 0f
        fxTimer = 0f
        if (fx != null) {
            fx.sfx(r.sfx, 1f)
            val m = FloatArray(3)
            mouthWorld(m)
            val word = when (r) {
                ReactionType.GIANT_CHEEKS -> "MMMPH!"
                ReactionType.SPINNY_EYES -> "WOOZY!"
                ReactionType.BUBBLE_BURP -> "BURP!"
                ReactionType.FIRE_BREATH -> "HOT HOT!"
                ReactionType.SPARKLE_DANCE -> "SPARKLY!"
                ReactionType.DUCKIFY -> "QUACK!"
                ReactionType.SPIN_OUT -> "WHEEE!"
                ReactionType.HEART_EYES -> "YUMMY!"
                ReactionType.SUPER_SIZE -> "SUPER!"
                ReactionType.SHRINK -> "teeny!"
                ReactionType.FROZEN -> "BRRR!"
                ReactionType.RAINBOW -> "RAINBOW!"
                ReactionType.STINKY -> "PEE-YEW!"
                ReactionType.BLOCKY -> "BLOCKY!"
                ReactionType.JUMP_SCARE -> "!!!"
                ReactionType.BALLOON_FLOAT -> "FLOATY!"
                ReactionType.TOOT -> "TOOT!"
                ReactionType.GIGGLES -> "HEE HEE!"
                ReactionType.HULA_HOOP -> "HULA!"
                ReactionType.BUNNY_EARS -> "HOP HOP!"
                ReactionType.ROCKET -> "BLAST OFF!"
                ReactionType.SLEEPY -> "Zzz..."
                ReactionType.JELLY_WOBBLE -> "WIBBLE!"
                ReactionType.SEED_SPIT -> "PTOO!"
                ReactionType.CONFETTI_SNEEZE -> "ah... ah..."
            }
            val col = when (r) {
                ReactionType.FIRE_BREATH, ReactionType.HEART_EYES -> 0xFF5A5A
                ReactionType.FROZEN, ReactionType.BUBBLE_BURP -> 0x5B8CFF
                ReactionType.STINKY -> 0x3FC48A
                ReactionType.DUCKIFY, ReactionType.SUPER_SIZE -> 0xFFB000
                else -> 0x8E6CF0
            }
            if (item != null || celebrating) fx.pop(word, m[0], m[1] + 1.25f * scale, m[2], col)
        }
    }

    private fun endReaction() {
        reaction = null
        val keepHat = pose.hat
        val sl = pose.sleepy
        pose.reset()
        pose.hat = keepHat
        if (sleeping) pose.sleepy = sl
        sizeTarget = 1f
    }

    fun update(dt: Float, worldTime: Float, fx: Fx) {
        time += dt
        lastTime = worldTime
        pose.time = worldTime
        // Movement
        moveAt(worldTime, mv5)
        x = mv5[0]; z = mv5[2]
        val hover = mv5[1]
        val targetVisible = mv5[3]
        hopPhase = mv5[4]
        visible += (targetVisible - visible) * min(1f, dt * 6f)
        y = baseY + hover + (visible - 1f) * 2.6f * baseScale
        if (type == CreatureType.CHOMPETTE) pose.flap = worldTime * 14f

        // Sleep cycle for sleepyheads
        if (sleeper && !sleeping && !celebrating) {
            awakeTimer -= dt
            if (awakeTimer <= 0f && reaction == null && chew <= 0f) {
                sleeping = true
                fx.sfx(Sfx.SNORE, 0.5f)
            }
        }
        if (sleeping) {
            pose.sleepy += (1f - pose.sleepy) * min(1f, dt * 3f)
            fxTimer += dt
            if (fxTimer > 1.1f) {
                fxTimer = 0f
                val m = FloatArray(3); mouthWorld(m)
                fx.particles.spawn(PKind.ZZZ, m[0] + 0.4f * scale, m[1] + 0.9f * scale, m[2], 0.4f, 0.9f, 0f, 2f, 0.28f * scale, 0x8E6CF0)
            }
        } else if (reaction == null) {
            pose.sleepy += (0f - pose.sleepy) * min(1f, dt * 5f)
        }

        // Beat mouths
        if (beat && !sleeping) {
            val was = beatOpen
            beatOpen = beatOpenAt(worldTime)
            if (beatOpen && !was) {
                squashVel += 2f
                fx.sfx(Sfx.DRUM, 0.35f)
            }
        }

        // Chewing then reaction
        if (chew > 0f) {
            chew -= dt
            pose.cheek = 0.35f + 0.15f * sin(time * 30f)
            pose.mouth = 0.15f + 0.15f * sin(time * 30f)
            if (chew <= 0f) {
                pose.cheek = 0f
                val r = pendingReaction
                if (r != null) {
                    fx.sfx(Sfx.GULP, 0.7f)
                    startReaction(r, pendingItem, fx)
                }
                pendingReaction = null
            }
        }

        // Mouth target
        val wantOpen = when {
            sleeping -> 0f
            chew > 0f -> -1f
            beat -> if (beatOpen) 1f else 0.05f
            reaction != null -> -1f
            anticipate > 0f -> min(1f, anticipate)
            celebrating -> 0.6f
            else -> 0.08f + 0.06f * sin(time * 1.3f)
        }
        if (wantOpen >= 0f) {
            mouthTarget = wantOpen
            val k = 160f
            val acc = (mouthTarget - pose.mouth) * k - mouthVel * 18f
            mouthVel += acc * dt
            pose.mouth += mouthVel * dt
        }
        anticipate = max(0f, anticipate - dt * 3f)

        // Squash spring + breathing
        val acc = (1f - squash) * 140f - squashVel * 9f
        squashVel += acc * dt
        squash += squashVel * dt
        val breathe = 1f + 0.025f * sin(time * 2.4f)
        val sq = (squash * breathe).coerceIn(0.55f, 1.6f)
        pose.sy = sq
        pose.sx = 1f / sqrt(sq)
        pose.sz = pose.sx

        // Hop / jump physics
        if (yOffset > 0f || yVel != 0f) {
            yVel -= 22f * dt
            yOffset += yVel * dt
            if (yOffset <= 0f) {
                yOffset = 0f
                if (yVel < -3f) { squashVel -= 3f; fx.sfx(Sfx.BONK, 0.3f, 1.4f) }
                yVel = 0f
            }
        }

        // Blink
        blinkTimer -= dt
        if (blinkTimer <= 0f) { blinkT = 0.16f; blinkTimer = 2f + fx.particles.rnd.nextFloat() * 3f }
        if (blinkT > 0f) { blinkT -= dt; pose.blink = if (blinkT > 0.08f) 1f - (blinkT - 0.08f) / 0.08f else blinkT / 0.08f } else pose.blink = 0f

        // Look
        val m = FloatArray(3)
        mouthWorld(m)
        val dx = lookAtX - x
        val dy = lookAtY - (m[1] + 0.4f)
        val dz = lookAtZ - z
        val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.01f)
        pose.lookX = (dx / len * 1.6f).coerceIn(-1f, 1f)
        pose.lookY = (dy / len * 1.6f).coerceIn(-1f, 1f)
        if (reaction == null) pose.yaw = (atan2(dx, dz) * 57.3f * 0.25f).coerceIn(-18f, 18f)

        if (jitter > 0f) { jitter -= dt; pose.tiltZ = sin(time * 60f) * 6f * jitter }
        if (shrug > 0f) {
            shrug -= dt
            pose.tiltZ = sin(time * 6f) * 8f * min(1f, shrug)
            pose.brow = 1f * min(1f, shrug)
            if (shrug <= 0f) pose.brow = 0f
        }

        sizeMul += (sizeTarget - sizeMul) * min(1f, dt * 5f)
        if (requestPop > 0f) requestPop = max(0f, requestPop - dt * 2.5f)

        reaction?.let { updateReaction(it, dt, fx) }
    }

    private fun updateReaction(r: ReactionType, dt: Float, fx: Fx) {
        rt += dt
        fxTimer += dt
        val p = pose
        val P = fx.particles
        val m = FloatArray(3)
        mouthWorld(m)
        val s = scale
        val k = (rt / r.duration).coerceIn(0f, 1f)
        val ease = sin(min(1f, k * 4f) * PI.toFloat() / 2f) * min(1f, (1f - k) * 5f)
        when (r) {
            ReactionType.GIANT_CHEEKS -> {
                p.cheek = 1.1f * ease
                p.sleepy = 0.5f * ease
                if (rt > 1.7f && fxTimer > 0.06f) { fxTimer = 0f; P.spawn(PKind.POOF, m[0], m[1], m[2] + 0.3f, (P.rnd.nextFloat() - 0.5f), 1f, 2f, 0.4f, 0.15f, 0xFFFFFF) }
            }
            ReactionType.SPINNY_EYES -> {
                p.spinEyes = true
                p.spinAngle += dt * 14f
                p.tiltZ = sin(rt * 9f) * 12f * ease
                if (fxTimer > 0.18f) {
                    fxTimer = 0f
                    val a = rt * 5f
                    P.spawn(PKind.STAR, m[0] + cos(a) * 0.8f * s, m[1] + 1.2f * s, m[2] + sin(a) * 0.6f * s, 0f, 0.3f, 0f, 0.7f, 0.18f * s, 0xFFD23F)
                }
            }
            ReactionType.BUBBLE_BURP -> {
                if (rt > 0.25f && rt < 1.4f) {
                    p.mouth = 0.9f
                    if (fxTimer > 0.07f) {
                        fxTimer = 0f
                        P.spawn(PKind.BUBBLE, m[0], m[1], m[2] + 0.2f, (P.rnd.nextFloat() - 0.5f) * 1.6f, 1.4f + P.rnd.nextFloat() * 1.4f,
                            0.6f + P.rnd.nextFloat(), 2.2f, (0.12f + P.rnd.nextFloat() * 0.25f) * s, 0xDDF3FF, -0.3f, 0.4f)
                    }
                } else p.mouth = 0.1f
                p.sy *= 1f + 0.08f * sin(rt * 30f) * ease
            }
            ReactionType.FIRE_BREATH -> {
                p.tint = 0xFF4A3C; p.tintAmt = 0.35f * ease
                if (rt > 0.5f && rt < 1.8f) {
                    p.mouth = 1f
                    if (fxTimer > 0.03f) {
                        fxTimer = 0f
                        P.spawn(PKind.FLAME, m[0], m[1], m[2] + 0.3f, (P.rnd.nextFloat() - 0.5f) * 1.5f, 0.6f + P.rnd.nextFloat(),
                            4.5f + P.rnd.nextFloat() * 2f, 0.6f, 0.22f * s, 0xFF9A2C, -1f, 1.5f)
                    }
                } else if (fxTimer > 0.15f) {
                    fxTimer = 0f
                    P.spawn(PKind.SMOKE, m[0] + (P.rnd.nextFloat() - 0.5f) * 0.6f, m[1] + 1.2f * s, m[2], 0f, 1.2f, 0f, 0.9f, 0.18f * s, 0xFFFFFF)
                }
            }
            ReactionType.SPARKLE_DANCE -> {
                yOffset = abs(sin(rt * 7f)) * 0.45f * s
                yVel = 0f
                p.yaw = sin(rt * 3.5f) * 35f
                p.tiltZ = sin(rt * 7f) * 10f
                p.wiggle = sin(rt * 14f)
                p.mouth = 0.5f
                if (fxTimer > 0.12f) {
                    fxTimer = 0f
                    P.sparkles(x, y + 1.2f * s, z, 2, 0.9f * s)
                    if (P.rnd.nextFloat() < 0.4f) P.spawn(PKind.NOTE, x + (P.rnd.nextFloat() - 0.5f) * 1.6f * s, y + 1.8f * s, z, 0f, 1f, 0f, 1.2f, 0.32f * s,
                        Particles.CONFETTI_COLORS[P.rnd.nextInt(5)])
                }
            }
            ReactionType.DUCKIFY -> {
                p.tint = 0xFFD23F; p.tintAmt = 0.9f * ease; p.beak = ease
                if (fxTimer > 0.6f && k < 0.85f) { fxTimer = 0f; fx.sfx(Sfx.QUACK, 0.6f, 1.1f); squashVel += 1.5f
                    P.burst(PKind.FEATHER, x, y + 1.2f * s, z, 3, 1.5f, intArrayOf(0xFFE14D, 0xFFFFFF), 0.25f, 1.2f, 1.5f) }
            }
            ReactionType.SPIN_OUT -> {
                p.yaw += dt * 900f * (1f - k)
                p.tiltZ = sin(rt * 8f) * 15f * ease
                p.spinEyes = true; p.spinAngle += dt * 10f
                if (fxTimer > 0.2f) { fxTimer = 0f; P.spawn(PKind.STAR, x, y + 2.1f * s, z, cos(rt * 9f) * 1.2f, 0.3f, sin(rt * 9f) * 1.2f, 0.6f, 0.15f * s, 0xFFD23F) }
            }
            ReactionType.HEART_EYES -> {
                p.heartEyes = true
                p.mouth = 0.35f
                yOffset = abs(sin(rt * 5f)) * 0.2f * s * ease
                if (fxTimer > 0.15f) { fxTimer = 0f; P.spawn(PKind.HEART, x + (P.rnd.nextFloat() - 0.5f) * 1.4f * s, y + 1.6f * s, z + 0.3f, 0f, 1.3f, 0f, 1.2f, 0.22f * s, 0xFF4D7A) }
            }
            ReactionType.SUPER_SIZE -> {
                sizeTarget = if (k < 0.8f) 1.65f else 1f
                p.wiggle = sin(rt * 5f) * 0.6f
                p.brow = -1f * ease
                if (rt in 0.6f..0.7f) fx.shake(0.35f)
            }
            ReactionType.SHRINK -> {
                sizeTarget = if (k < 0.8f) 0.45f else 1f
                p.mouth = 0.3f
                if (fxTimer > 0.3f) { fxTimer = 0f; squashVel += 1.2f }
            }
            ReactionType.FROZEN -> {
                p.tint = 0x9FD8FF; p.tintAmt = 0.75f * ease
                p.mouth = 0f
                p.tiltZ = sin(rt * 50f) * 2f * ease
                if (fxTimer > 0.12f && k < 0.8f) { fxTimer = 0f; P.spawn(PKind.ICE, x + (P.rnd.nextFloat() - 0.5f) * 1.6f * s, y + 1.9f * s, z, 0f, -0.5f, 0f, 1.2f, 0.08f * s, 0xFFFFFF, 3f) }
                if (k > 0.82f && fxTimer > 0.05f) { fxTimer = 0f; P.burst(PKind.SNOW, x, y + 1f * s, z, 4, 3f, intArrayOf(0xFFFFFF), 0.07f, 0.8f, 6f) }
            }
            ReactionType.RAINBOW -> {
                p.rainbow = true; p.tint = 0xFFFFFF; p.tintAmt = 0.7f * ease
                if (fxTimer > 0.1f) { fxTimer = 0f; P.spawn(PKind.STAR, x + (P.rnd.nextFloat() - 0.5f) * 2f * s, y + 2f * s, z, 0f, 0.8f, 0f, 1f, 0.16f * s,
                    Particles.CONFETTI_COLORS[P.rnd.nextInt(Particles.CONFETTI_COLORS.size)]) }
            }
            ReactionType.STINKY -> {
                p.tint = 0x7FD46B; p.tintAmt = 0.25f * ease
                p.sleepy = 0.6f * ease
                p.brow = 1f * ease
                if (fxTimer > 0.1f) { fxTimer = 0f; P.spawn(PKind.STINK, x + (P.rnd.nextFloat() - 0.5f) * 1.8f * s, y + 1.4f * s, z,
                    0f, 0.6f, 0f, 1.4f, 0.25f * s, 0x9BE07F) }
            }
            ReactionType.BLOCKY -> {
                p.blocky = ease
                if (rt < 0.1f) P.poof(x, y + 1f * s, z, s)
                if (k > 0.85f && fxTimer > 0.2f) { fxTimer = 0f; P.poof(x, y + 1f * s, z, s) }
            }
            ReactionType.JUMP_SCARE -> {
                if (rt < 0.15f && yVel <= 0f && yOffset <= 0f) { yVel = 11f; yOffset = 0.01f; P.sparkles(m[0], m[1] + 1f, m[2], 6, 0.6f, 0xFFFFFF) }
                p.mouth = 0.9f * ease
                p.brow = -1f * ease
            }
            ReactionType.BALLOON_FLOAT -> {
                p.sx *= 1f + 0.35f * ease; p.sz *= 1f + 0.35f * ease
                yOffset = 2.4f * ease * s + 0.15f * sin(rt * 4f)
                yVel = 0f
                p.mouth = 0.3f
            }
            ReactionType.TOOT -> {
                p.cheek = if (rt < 0.4f) rt * 2f else 0f
                if (rt > 0.4f && rt < 1.6f) {
                    p.mouth = 0.6f
                    if (fxTimer > 0.18f) { fxTimer = 0f; P.spawn(PKind.NOTE, m[0], m[1] + 0.2f, m[2] + 0.3f, (P.rnd.nextFloat() - 0.5f) * 2f, 1.5f, 1.5f, 1.3f, 0.35f * s, 0xFFC21F) }
                }
            }
            ReactionType.GIGGLES -> {
                p.tiltZ = sin(rt * 30f) * 7f * ease
                p.sleepy = 0.7f * ease
                p.mouth = 0.6f + 0.3f * sin(rt * 30f)
                if (fxTimer > 0.25f) { fxTimer = 0f; P.spawn(PKind.HEART, x + (P.rnd.nextFloat() - 0.5f) * 1.4f * s, y + 1.7f * s, z, 0f, 1f, 0f, 1f, 0.15f * s, 0xFF8FC8) }
            }
            ReactionType.HULA_HOOP -> {
                p.hula = ease
                p.hulaAngle += dt * 12f
                p.tiltZ = sin(p.hulaAngle) * 8f
                p.mouth = 0.5f
            }
            ReactionType.BUNNY_EARS -> {
                p.bunny = ease
                if (fxTimer > 0.55f && k < 0.85f) { fxTimer = 0f; yVel = 6f; yOffset = 0.01f; fx.sfx(Sfx.BOING, 0.5f, 1.3f) }
            }
            ReactionType.ROCKET -> {
                if (rt < 0.6f) {
                    p.tiltZ = sin(rt * 60f) * 4f
                    p.cheek = rt
                } else if (rt < 1.6f) {
                    yOffset += (6f - yOffset) * min(1f, dt * 3f)
                    yVel = 0f
                    p.mouth = 0.8f
                    if (fxTimer > 0.03f) { fxTimer = 0f; P.spawn(PKind.FLAME, x, y + yOffset - 0.1f, z, (P.rnd.nextFloat() - 0.5f), -4f, 0f, 0.5f, 0.3f * s, 0xFF9A2C)
                        P.spawn(PKind.SMOKE, x, y + yOffset - 0.2f, z, (P.rnd.nextFloat() - 0.5f) * 2f, -1f, 0f, 1f, 0.3f * s, 0xFFFFFF) }
                } else if (rt < 1.7f) {
                    yVel = -2f
                }
            }
            ReactionType.SLEEPY -> {
                p.sleepy = ease
                p.mouth = 0.15f
                p.tiltZ = 8f * ease
                if (fxTimer > 0.7f) { fxTimer = 0f; P.spawn(PKind.ZZZ, m[0] + 0.4f * s, m[1] + 0.9f * s, m[2], 0.4f, 0.9f, 0f, 1.6f, 0.26f * s, 0x8E6CF0) }
            }
            ReactionType.JELLY_WOBBLE -> {
                val w = 0.28f * exp(-rt * 0.9f) * sin(rt * 16f)
                p.sy *= 1f + w; p.sx *= 1f - w * 0.6f; p.sz *= 1f - w * 0.6f
                p.tint = 0x5BD86E; p.tintAmt = 0.3f * ease
                p.mouth = 0.4f
            }
            ReactionType.SEED_SPIT -> {
                if (rt > 0.3f && rt < 1.9f) {
                    p.mouth = 0.3f + 0.4f * abs(sin(rt * 14f))
                    if (fxTimer > 0.13f) { fxTimer = 0f; fx.sfx(Sfx.SPIT, 0.4f, 1f + P.rnd.nextFloat() * 0.3f)
                        P.spawn(PKind.SEED, m[0], m[1], m[2] + 0.2f, (P.rnd.nextFloat() - 0.5f) * 3f, 2.5f, 6f, 1.4f, 0.09f * s, 0, 9f, 0.2f) }
                }
            }
            ReactionType.CONFETTI_SNEEZE -> {
                if (rt < 0.6f) {
                    p.tiltZ = 0f
                    p.sy *= 1f + rt * 0.25f
                    p.sleepy = rt
                    p.mouth = 0.4f
                } else {
                    if (fxTimer > 0f && rt - dt < 0.6f) {
                        P.confetti(m[0], m[1], m[2] + 0.3f, 50, 7f, 0.9f)
                        fx.pop("ACHOO!", m[0], m[1] + 1.3f * s, m[2], 0xFF6B6B)
                        squashVel -= 4f
                        fx.shake(0.25f)
                    }
                    p.sleepy = 0f
                    p.mouth = 0.3f
                }
            }
        }
        if (rt >= r.duration) {
            endReaction()
            if (celebrating) startReaction(ReactionType.SPARKLE_DANCE, null, null)
        }
    }

    /** Draws the creature, its shadow and a thought bubble with its request. */
    fun draw(ctx: DrawCtx, hat: Hat, silhouette: Boolean = false) {
        if (visible < 0.02f && type != CreatureType.SLURPO) return
        pose.hat = hat
        val s = scale
        val shadowA = (0.42f - (y + yOffset - baseY).coerceAtLeast(0f) * 0.06f).coerceIn(0.08f, 0.42f) * visible
        val above = (y + yOffset - baseY).coerceAtLeast(0f)
        ctx.shadow(x, baseY, z, 1.05f * s * (1f - min(0.5f, above * 0.08f)), shadowA)
        ctx.push()
        ctx.translate(x, y + yOffset, z)
        ctx.scale(s)
        if (silhouette) { ctx.tintColor = 0x2B2340; ctx.tintAmount = 1f }
        CreatureModels.draw(ctx, type, pose)
        if (silhouette) ctx.tintAmount = 0f
        ctx.pop()
        val req = request
        if (req != null && !sleeping && visible > 0.6f) {
            val bob = 0.08f * sin(time * 3f)
            val pop = 1f + requestPop * 0.4f
            val bx = x + 0.9f * s
            val by = y + yOffset + (type.hatY + 0.75f) * s + bob
            ctx.push()
            ctx.translate(bx, by, z + 0.2f)
            ctx.scale(pop)
            ctx.part(Meshes.sphereLo, 0xFFFFFF, -0.55f, -0.6f, 0f, 0.09f, gloss = 0.2f)
            ctx.part(Meshes.sphereLo, 0xFFFFFF, -0.35f, -0.4f, 0f, 0.13f, gloss = 0.2f)
            ctx.part(Meshes.cloud, 0xFFFFFF, 0f, 0f, -0.15f, 0.42f, 0.38f, 0.3f, gloss = 0.15f, rim = 0.4f)
            ctx.push(); ctx.translate(0.02f, 0.05f, 0.25f); ctx.rotateY(sin(time * 2f) * 25f); ctx.scale(0.75f)
            ItemModels.draw(ctx, req)
            ctx.pop()
            ctx.pop()
        }
    }

}
