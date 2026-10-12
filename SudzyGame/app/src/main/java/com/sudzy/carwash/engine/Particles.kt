package com.sudzy.carwash.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

enum class PType { DROP, BUBBLE, SPARKLE, CHUNK, CONFETTI, WIND, COIN, STAR, RING, FOAM }

class Particle {
    var type = PType.DROP
    var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
    var life = 0f; var maxLife = 1f
    var size = 10f; var color = Color.WHITE
    var rot = 0f; var vr = 0f; var gravity = 0f; var drag = 0f
    var alive = false
}

/** Pooled particle system used for splashes, bubbles, sparkles, confetti and coins. */
class Particles(capacity: Int = 900) {
    private val pool = Array(capacity) { Particle() }
    private var cursor = 0
    private val rng = Rng(99)

    fun spawn(type: PType, x: Float, y: Float, vx: Float, vy: Float, size: Float, life: Float,
              color: Int = Color.WHITE, gravity: Float = 0f, drag: Float = 0f, vr: Float = 0f): Particle {
        val p = pool[cursor]; cursor = (cursor + 1) % pool.size
        p.type = type; p.x = x; p.y = y; p.vx = vx; p.vy = vy; p.size = size
        p.life = life; p.maxLife = life; p.color = color; p.gravity = gravity; p.drag = drag
        p.rot = rng.range(0f, 360f); p.vr = vr; p.alive = true
        return p
    }

    fun burst(type: PType, x: Float, y: Float, n: Int, speed: Float, size: Float, life: Float,
              colors: IntArray = intArrayOf(Color.WHITE), gravity: Float = 0f, spread: Float = 6.283f, dir: Float = 0f) {
        repeat(n) {
            val a = dir + rng.range(-spread / 2, spread / 2)
            val sp = speed * rng.range(0.35f, 1f)
            spawn(type, x, y, cos(a) * sp, sin(a) * sp, size * rng.range(0.6f, 1.3f), life * rng.range(0.6f, 1.2f),
                colors[rng.int(colors.size)], gravity, 1.2f, rng.range(-400f, 400f))
        }
    }

    fun confetti(w: Float, n: Int) {
        val cs = intArrayOf(Col.RED, Col.GREEN, Col.CYAN, Col.PURPLE, Col.YELLOW, Color.WHITE, Col.ORANGE)
        repeat(n) {
            spawn(PType.CONFETTI, rng.range(0f, w), rng.range(-400f, -20f), rng.range(-120f, 120f), rng.range(150f, 420f),
                rng.range(18f, 30f), rng.range(2.5f, 4.5f), cs[rng.int(cs.size)], 120f, 0.4f, rng.range(-500f, 500f))
        }
    }

    fun clear() { pool.forEach { it.alive = false } }

    fun update(dt: Float) {
        for (p in pool) {
            if (!p.alive) continue
            p.life -= dt
            if (p.life <= 0f) { p.alive = false; continue }
            val d = 1f / (1f + p.drag * dt)
            p.vx *= d; p.vy = p.vy * d + p.gravity * dt
            p.x += p.vx * dt; p.y += p.vy * dt; p.rot += p.vr * dt
            if (p.type == PType.CONFETTI) p.vx += sin(p.life * 6f) * 40f * dt
        }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    fun draw(c: Canvas) {
        for (p in pool) {
            if (!p.alive) continue
            val k = p.life / p.maxLife
            when (p.type) {
                PType.DROP -> {
                    paint.style = Paint.Style.FILL; paint.color = p.color; paint.alpha = (255 * k.coerceAtMost(1f)).toInt()
                    c.save(); c.rotate(Math.toDegrees(kotlin.math.atan2(p.vy, p.vx).toDouble()).toFloat() + 90f, p.x, p.y)
                    c.drawOval(p.x - p.size * 0.45f, p.y - p.size, p.x + p.size * 0.45f, p.y + p.size * 0.6f, paint)
                    paint.color = Color.WHITE; paint.alpha = (200 * k).toInt()
                    c.drawCircle(p.x - p.size * 0.12f, p.y - p.size * 0.3f, p.size * 0.15f, paint)
                    c.restore()
                }
                PType.BUBBLE -> Gfx.bubble(c, p.x, p.y, p.size * (1.2f - 0.2f * k), (k * 2f).coerceAtMost(1f))
                PType.FOAM -> {
                    paint.style = Paint.Style.FILL; paint.color = Color.WHITE; paint.alpha = (255 * (k * 2f).coerceAtMost(1f)).toInt()
                    c.drawCircle(p.x, p.y, p.size, paint)
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = p.size * 0.15f; paint.color = 0xFFBFE6FF.toInt(); paint.alpha = paint.alpha
                    c.drawCircle(p.x, p.y, p.size, paint)
                }
                PType.SPARKLE -> Gfx.sparkle(c, p.x, p.y, p.size * (0.4f + 0.6f * sin(k * 3.14f)), p.color, 1f)
                PType.STAR -> { c.save(); c.rotate(p.rot, p.x, p.y); Gfx.star(c, p.x, p.y, p.size, true); c.restore() }
                PType.CHUNK -> {
                    paint.style = Paint.Style.FILL; paint.color = p.color; paint.alpha = (255 * (k * 1.5f).coerceAtMost(1f)).toInt()
                    c.drawCircle(p.x, p.y, p.size * (0.5f + 0.5f * k), paint)
                }
                PType.CONFETTI -> {
                    paint.style = Paint.Style.FILL; paint.color = p.color; paint.alpha = (255 * (k * 3f).coerceAtMost(1f)).toInt()
                    c.save(); c.rotate(p.rot, p.x, p.y)
                    val sq = kotlin.math.abs(cos(p.rot * 0.03f))
                    c.drawRect(p.x - p.size * 0.35f, p.y - p.size * 0.5f * sq, p.x + p.size * 0.35f, p.y + p.size * 0.5f * sq, paint)
                    c.restore()
                }
                PType.WIND -> {
                    paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND; paint.strokeWidth = p.size * 0.25f
                    paint.color = p.color; paint.alpha = (200 * k).toInt()
                    c.drawLine(p.x, p.y, p.x - p.vx * 0.06f, p.y - p.vy * 0.06f, paint)
                }
                PType.COIN -> { c.save(); c.scale(kotlin.math.abs(cos(p.rot * 0.02f)) * 0.7f + 0.3f, 1f, p.x, p.y); Gfx.coin(c, p.x, p.y, p.size); c.restore() }
                PType.RING -> {
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = p.size * 0.12f * k + 1f
                    paint.color = p.color; paint.alpha = (255 * k).toInt()
                    c.drawCircle(p.x, p.y, p.size * (1.6f - k), paint)
                }
            }
        }
    }
}

/** Floating "SPLASH x5!" style text. */
class FloatText(val text: String, var x: Float, var y: Float, val size: Float, val color: Int, val outline: Int, val life: Float = 1.1f) {
    var t = 0f
    val rot = (Math.random().toFloat() - 0.5f) * 16f
    fun alive() = t < life
    fun draw(c: Canvas) {
        val k = t / life
        val sc = Ease.outBack(Ease.win(t, 0f, 0.25f), 3f)
        val a = if (k > 0.7f) 1f - (k - 0.7f) / 0.3f else 1f
        c.save(); c.translate(x, y - k * 90f); c.rotate(rot); c.scale(sc, sc)
        Gfx.outlined(c, text, 0f, 0f, size, color, outline, alpha = a)
        c.restore()
    }
}
