package com.feedthewrongthing.game.game

import com.feedthewrongthing.game.engine.DrawCtx
import com.feedthewrongthing.game.engine.Meshes
import com.feedthewrongthing.game.engine.RenderFrame
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class PKind { CONFETTI, BUBBLE, STAR, SPARKLE, SMOKE, FLAME, NOTE, HEART, ZZZ, SNOW, STINK, SEED, ICE, CRUMB, DROP, POOF, FEATHER }

/** Fixed-size pool of simple 3D particles. */
class Particles(private val cap: Int = 900) {
    private val kind = IntArray(cap)
    private val px = FloatArray(cap); private val py = FloatArray(cap); private val pz = FloatArray(cap)
    private val vx = FloatArray(cap); private val vy = FloatArray(cap); private val vz = FloatArray(cap)
    private val life = FloatArray(cap); private val maxLife = FloatArray(cap)
    private val size = FloatArray(cap); private val color = IntArray(cap)
    private val rot = FloatArray(cap); private val rotV = FloatArray(cap)
    private val grav = FloatArray(cap); private val drag = FloatArray(cap)
    var count = 0
        private set
    val rnd = Random(1234)

    fun clear() { count = 0 }

    fun spawn(k: PKind, x: Float, y: Float, z: Float, svx: Float, svy: Float, svz: Float,
              lifeSec: Float, s: Float, col: Int, gravity: Float = 0f, dragK: Float = 0.5f) {
        if (count >= cap) return
        val i = count++
        kind[i] = k.ordinal
        px[i] = x; py[i] = y; pz[i] = z
        vx[i] = svx; vy[i] = svy; vz[i] = svz
        life[i] = lifeSec; maxLife[i] = lifeSec
        size[i] = s; color[i] = col
        rot[i] = rnd.nextFloat() * 360f
        rotV[i] = (rnd.nextFloat() - 0.5f) * 720f
        grav[i] = gravity; drag[i] = dragK
    }

    private fun r(a: Float) = (rnd.nextFloat() * 2f - 1f) * a

    fun burst(k: PKind, x: Float, y: Float, z: Float, n: Int, speed: Float, colors: IntArray, size: Float = 0.12f,
              lifeSec: Float = 1.2f, gravity: Float = 9f, up: Float = 0.5f, dirX: Float = 0f, dirZ: Float = 0f) {
        for (q in 0 until n) {
            val c = colors[rnd.nextInt(colors.size)]
            spawn(k, x, y, z, r(speed) + dirX * speed, rnd.nextFloat() * speed * up + speed * 0.3f, r(speed) + dirZ * speed,
                lifeSec * (0.7f + rnd.nextFloat() * 0.6f), size * (0.7f + rnd.nextFloat() * 0.6f), c, gravity, 0.8f)
        }
    }

    fun confetti(x: Float, y: Float, z: Float, n: Int, speed: Float = 6f, dirZ: Float = 0f) =
        burst(PKind.CONFETTI, x, y, z, n, speed, CONFETTI_COLORS, 0.1f, 2.2f, 7f, 1.2f, 0f, dirZ)

    fun poof(x: Float, y: Float, z: Float, s: Float = 1f) {
        for (q in 0 until 8) {
            val a = q / 8f * 6.2832f
            spawn(PKind.POOF, x, y, z, cos(a) * 1.8f * s, 0.6f + rnd.nextFloat(), sin(a) * 1.8f * s, 0.5f, 0.25f * s, 0xFFFFFF, 0f, 3f)
        }
    }

    fun sparkles(x: Float, y: Float, z: Float, n: Int, spread: Float, col: Int = 0xFFF6C2) {
        for (q in 0 until n) spawn(PKind.SPARKLE, x + r(spread), y + r(spread), z + r(spread), r(0.5f), 1f + rnd.nextFloat(), r(0.5f), 0.9f, 0.09f, col, 0f, 1f)
    }

    fun update(dt: Float) {
        var i = 0
        while (i < count) {
            life[i] -= dt
            if (life[i] <= 0f) {
                val last = --count
                if (i != last) copy(last, i)
                continue
            }
            val d = 1f - min(1f, drag[i] * dt)
            vx[i] *= d; vz[i] *= d
            vy[i] = vy[i] * d - grav[i] * dt
            if (kind[i] == PKind.BUBBLE.ordinal || kind[i] == PKind.STINK.ordinal) vx[i] += sin(life[i] * 6f + i) * dt * 2f
            px[i] += vx[i] * dt; py[i] += vy[i] * dt; pz[i] += vz[i] * dt
            if (kind[i] == PKind.CONFETTI.ordinal && py[i] < 0.02f) { py[i] = 0.02f; vy[i] = 0f; vx[i] *= 0.5f; vz[i] *= 0.5f; rotV[i] *= 0.5f }
            rot[i] += rotV[i] * dt
            i++
        }
    }

    private fun copy(a: Int, b: Int) {
        kind[b] = kind[a]; px[b] = px[a]; py[b] = py[a]; pz[b] = pz[a]
        vx[b] = vx[a]; vy[b] = vy[a]; vz[b] = vz[a]; life[b] = life[a]; maxLife[b] = maxLife[a]
        size[b] = size[a]; color[b] = color[a]; rot[b] = rot[a]; rotV[b] = rotV[a]; grav[b] = grav[a]; drag[b] = drag[a]
    }

    fun draw(ctx: DrawCtx) {
        for (i in 0 until count) {
            val t = 1f - life[i] / maxLife[i]
            val fadeIn = min(1f, t * 8f)
            val fadeOut = min(1f, life[i] / (maxLife[i] * 0.3f))
            val s = size[i]
            ctx.push()
            ctx.translate(px[i], py[i], pz[i])
            when (PKind.entries[kind[i]]) {
                PKind.CONFETTI -> {
                    ctx.rotate(rot[i], 0.3f, 1f, 0.5f)
                    ctx.scale(s, s * 0.12f, s * 0.6f)
                    ctx.draw(Meshes.cube, color[i], alpha = fadeOut, gloss = 0.4f, rim = 0.2f)
                }
                PKind.BUBBLE -> {
                    val g = s * (0.6f + 0.4f * fadeIn) * (1f + 0.1f * sin(t * 20f))
                    ctx.scale(g)
                    ctx.draw(Meshes.sphere, color[i], alpha = 0.45f * fadeOut, gloss = 0.95f, rim = 0.9f, mode = RenderFrame.GLASS)
                }
                PKind.STAR, PKind.SPARKLE -> {
                    ctx.rotateZ(rot[i])
                    val g = s * fadeIn * (if (kind[i] == PKind.SPARKLE.ordinal) (0.5f + 0.5f * sin(t * 18f)) else 1f)
                    ctx.scale(g)
                    ctx.draw(Meshes.star, color[i], alpha = fadeOut, gloss = 0.8f, emissive = 0.55f)
                }
                PKind.SMOKE, PKind.POOF -> {
                    ctx.scale(s * (0.6f + t * 1.4f))
                    ctx.draw(Meshes.sphereLo, color[i], alpha = 0.55f * fadeOut, gloss = 0f, rim = 0.3f)
                }
                PKind.STINK -> {
                    ctx.scale(s * (0.6f + t * 1.2f))
                    ctx.draw(Meshes.sphereLo, color[i], alpha = 0.5f * fadeOut, gloss = 0.1f, rim = 0.5f)
                }
                PKind.FLAME -> {
                    val c = if (t < 0.3f) 0xFFE45C else if (t < 0.6f) 0xFF9A2C else 0xFF4A2C
                    ctx.scale(s * (1f - t * 0.6f), s * 1.6f * (1f - t * 0.5f), s * (1f - t * 0.6f))
                    ctx.draw(Meshes.teardrop, c, alpha = fadeOut, gloss = 0f, rim = 0f, emissive = 0.9f, mode = RenderFrame.UNLIT)
                }
                PKind.NOTE -> {
                    ctx.rotateZ(15f * sin(t * 8f))
                    ctx.scale(s * fadeIn)
                    ctx.draw(Meshes.note, color[i], alpha = fadeOut, gloss = 0.6f, emissive = 0.3f)
                }
                PKind.HEART -> {
                    ctx.rotateZ(12f * sin(t * 6f))
                    ctx.scale(s * fadeIn * (1f + 0.15f * sin(t * 16f)))
                    ctx.draw(Meshes.heart, color[i], alpha = fadeOut, gloss = 0.8f, emissive = 0.3f)
                }
                PKind.ZZZ -> {
                    ctx.rotateZ(-10f)
                    ctx.scale(s * fadeIn)
                    ctx.draw(Meshes.zee, color[i], alpha = fadeOut, gloss = 0.5f, emissive = 0.4f)
                }
                PKind.SNOW -> {
                    ctx.scale(s)
                    ctx.draw(Meshes.sphereLo, 0xFFFFFF, alpha = fadeOut, gloss = 0.3f, emissive = 0.3f)
                }
                PKind.SEED -> {
                    ctx.rotate(rot[i], 1f, 0f, 0f)
                    ctx.scale(s * 0.6f, s * 0.4f, s)
                    ctx.draw(Meshes.egg, 0x1A1428, gloss = 0.8f)
                }
                PKind.ICE -> {
                    ctx.rotate(rot[i], 1f, 1f, 0f)
                    ctx.scale(s)
                    ctx.draw(Meshes.roundCube, 0xCFEFFF, alpha = 0.6f * fadeOut, gloss = 0.95f, rim = 0.8f, mode = RenderFrame.GLASS)
                }
                PKind.CRUMB -> {
                    ctx.rotate(rot[i], 1f, 0.5f, 0f)
                    ctx.scale(s)
                    ctx.draw(Meshes.roundCube, color[i], alpha = fadeOut, gloss = 0.3f)
                }
                PKind.DROP -> {
                    ctx.scale(s)
                    ctx.draw(Meshes.sphereLo, color[i], alpha = 0.7f * fadeOut, gloss = 0.95f, rim = 0.7f, mode = RenderFrame.GLASS)
                }
                PKind.FEATHER -> {
                    ctx.rotate(rot[i], 0f, 0f, 1f)
                    ctx.scale(s * 0.4f, s, s * 0.1f)
                    ctx.draw(Meshes.teardrop, color[i], alpha = fadeOut, gloss = 0.3f)
                }
            }
            ctx.pop()
        }
    }

    companion object {
        val CONFETTI_COLORS = intArrayOf(0xFFD23F, 0xFF6B8B, 0x4FD1BA, 0x8E6CF0, 0x5B8CFF, 0xFFFFFF, 0x3FC48A)
    }
}

/** Floating comic words like "ACHOO!" drawn by the 2D overlay. */
class PopText(val text: String, val x: Float, val y: Float, val z: Float, val color: Int, val life: Float = 1.4f) {
    var age = 0f
}
