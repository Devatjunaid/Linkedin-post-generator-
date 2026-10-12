package com.sudzy.carwash.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import kotlin.math.sin

/**
 * Bouncy candy button. Coordinates are virtual (1080-wide) units.
 * [art] draws the content centred at (0,0) inside a w*h box.
 */
class Button(
    var x: Float, var y: Float, var w: Float, var h: Float,
    var top: Int, var bottom: Int,
    var round: Boolean = false,
    var pulse: Boolean = false,
    var badge: Boolean = false,
    var visible: Boolean = true,
    val art: (Canvas, Float, Float) -> Unit,
    val onClick: () -> Unit,
) {
    private var pressed = false
    private var press = 0f
    private var pop = 0f
    var appear = 1f

    fun contains(px: Float, py: Float) = visible && px >= x && px <= x + w && py >= y && py <= y + h + 14f

    fun touch(t: Touch, game: Game): Boolean {
        if (!visible) return false
        when (t.action) {
            Touch.DOWN -> if (contains(t.x, t.y)) { pressed = true; game.audio.sfx("click", 0.7f); return true }
            Touch.MOVE -> if (pressed) { if (!contains(t.x, t.y)) pressed = false; return true }
            Touch.UP -> if (pressed) {
                pressed = false
                if (contains(t.x, t.y)) { pop = 1f; game.vibrate(15); onClick() }
                return true
            }
        }
        return false
    }

    fun update(dt: Float) {
        press += ((if (pressed) 1f else 0f) - press) * (dt * 25f).coerceAtMost(1f)
        pop = (pop - dt * 4f).coerceAtLeast(0f)
    }

    fun draw(c: Canvas, time: Float) {
        if (!visible || appear <= 0f) return
        val cx = x + w / 2; val cy = y + h / 2
        var sc = (1f - press * 0.08f + pop * 0.08f) * Ease.outBack(appear, 2.2f)
        if (pulse) sc *= 1f + 0.045f * sin(time * 6f)
        c.save(); c.scale(sc, sc, cx, cy)
        val sink = press * 8f
        c.translate(0f, sink)
        if (round) {
            val r = w / 2
            c.drawCircle(cx, cy + 10f - sink, r, Gfx.p(Col.shade(bottom, -0.45f)))
            c.drawCircle(cx, cy, r, Gfx.vGrad(cy - r, cy + r, top, bottom))
            c.drawCircle(cx, cy - r * 0.35f, r * 0.55f, Gfx.p(0x33FFFFFF))
            c.drawCircle(cx, cy, r, Gfx.s(Color.WHITE, 7f))
        } else {
            Gfx.candy(c, x, y, x + w, y + h, top, bottom, h * 0.3f, depth = 14f - sink)
        }
        c.save(); c.translate(cx, cy); art(c, w, h); c.restore()
        if (badge) {
            val bx = x + w - 10f; val by = y + 6f
            c.drawCircle(bx, by, 26f, Gfx.p(0xFFFF2D2D.toInt())); c.drawCircle(bx, by, 26f, Gfx.s(Color.WHITE, 5f))
            Gfx.outlined(c, "!", bx, by + 13f, 36f, Color.WHITE, 0xFF9A0000.toInt(), strokeMul = 0.05f, depth = 0f)
        }
        c.restore()
    }
}

class ButtonGroup {
    val list = mutableListOf<Button>()
    fun add(b: Button): Button { list += b; return b }
    fun touch(t: Touch, game: Game): Boolean { for (b in list.asReversed()) if (b.touch(t, game)) return true; return false }
    fun update(dt: Float) = list.forEach { it.update(dt) }
    fun draw(c: Canvas, time: Float) = list.forEach { it.draw(c, time) }
}

/** Persistent progress. Players never see level numbers; this is internal only. */
class Save(context: Context) {
    private val sp = context.getSharedPreferences("sudzy_save", Context.MODE_PRIVATE)

    var unlocked: Int
        get() = sp.getInt("unlocked", 0)
        set(v) = sp.edit().putInt("unlocked", v).apply()
    var coins: Int
        get() = sp.getInt("coins", 0)
        set(v) = sp.edit().putInt("coins", v).apply()
    var music: Boolean
        get() = sp.getBoolean("music", true)
        set(v) = sp.edit().putBoolean("music", v).apply()
    var sound: Boolean
        get() = sp.getBoolean("sound", true)
        set(v) = sp.edit().putBoolean("sound", v).apply()
    var vibration: Boolean
        get() = sp.getBoolean("vibration", true)
        set(v) = sp.edit().putBoolean("vibration", v).apply()
    var lastSpinDay: Long
        get() = sp.getLong("spin_day", -1L)
        set(v) = sp.edit().putLong("spin_day", v).apply()
    var levelsPlayed: Int
        get() = sp.getInt("played", 0)
        set(v) = sp.edit().putInt("played", v).apply()

    fun stars(level: Int): Int = sp.getInt("stars_$level", 0)
    fun setStars(level: Int, s: Int) { if (s > stars(level)) sp.edit().putInt("stars_$level", s).apply() }
    fun stickers(level: Int): String = sp.getString("stk_$level", "") ?: ""
    fun setStickers(level: Int, v: String) = sp.edit().putString("stk_$level", v).apply()
    fun totalStars(): Int = (0 until 30).sumOf { stars(it) }
}
