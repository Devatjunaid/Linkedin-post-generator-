package com.sudzy.carwash.scenes

import android.graphics.Canvas
import android.graphics.Color
import com.sudzy.carwash.art.LogoArt
import com.sudzy.carwash.engine.Col
import com.sudzy.carwash.engine.Ease
import com.sudzy.carwash.engine.Game
import com.sudzy.carwash.engine.Gfx
import com.sudzy.carwash.engine.PType
import com.sudzy.carwash.engine.Particles
import com.sudzy.carwash.engine.Rng
import com.sudzy.carwash.engine.Scene
import com.sudzy.carwash.engine.Touch
import kotlin.math.sin

/**
 * Animated splash: a bubble wells up from a puddle of foam, pops open to reveal Sudzy,
 * the letters bounce in one by one, the ribbon snaps in, sparkles burst, then a foam wipe
 * carries us to the home screen.
 */
class SplashScene(game: Game) : Scene(game) {
    override val music: String? = null
    private val fx = Particles(400)
    private var popped = BooleanArray(7)
    private var left = false
    private val rng = Rng(3)

    private fun letterT(i: Int) = Ease.win(time, 1.05f + i * 0.14f, 0.55f)

    override fun update(dt: Float) {
        super.update(dt)
        fx.update(dt)
        val cx = W / 2; val cy = H * 0.42f
        if (!popped[0] && time > 0.75f) {
            popped[0] = true; game.audio.sfx("pop", 1f, 0.8f)
            fx.burst(PType.BUBBLE, cx, cy - 200f, 18, 900f, 30f, 1.2f)
            fx.burst(PType.SPARKLE, cx, cy - 200f, 20, 1000f, 30f, 0.8f, intArrayOf(Color.WHITE, Col.CYAN, Col.YELLOW))
        }
        for (i in 0 until 6) if (!popped[i + 1] && letterT(i) > 0.45f) {
            popped[i + 1] = true
            game.audio.sfx("boing", 0.6f, 0.9f + i * 0.08f)
            fx.burst(PType.FOAM, cx - 330f + i * 130f, cy + 110f, 6, 400f, 16f, 0.6f, gravity = 900f)
        }
        if (time > 2.3f && time - dt <= 2.3f) {
            game.audio.sfx("tada", 0.8f)
            fx.burst(PType.STAR, cx, cy, 10, 1200f, 30f, 1.1f, gravity = 1200f)
            fx.burst(PType.SPARKLE, cx, cy, 40, 1400f, 34f, 1f, intArrayOf(Color.WHITE, Col.YELLOW, Col.PINK, Col.CYAN))
        }
        if (time > 3.4f && !left) { left = true; game.go(HomeScene(game)) }
        if (time < 2.5f && rng.chance(0.3f)) fx.spawn(PType.BUBBLE, rng.range(0f, W), H + 40f, 0f, -rng.range(200f, 500f), rng.range(14f, 44f), 4f)
    }

    override fun onTouch(t: Touch): Boolean {
        if (t.action == Touch.UP && time > 1.2f && !left) { left = true; game.go(HomeScene(game)) }
        return true
    }

    override fun draw(c: Canvas) {
        val bgIn = Ease.win(time, 0f, 0.5f)
        MenuBg.draw(c, W, H, time, Col.lerp(0xFF0B2A6E.toInt(), 0xFF7FE3FF.toInt(), bgIn), Col.SKY, Col.lerp(0xFF050F30.toInt(), 0xFF1053E0.toInt(), bgIn), H * 0.4f)
        val cx = W / 2; val cy = H * 0.45f
        // foam puddle that the bubble rises from
        val puddle = 1f - Ease.win(time, 0.6f, 0.5f)
        if (puddle > 0f) {
            for (i in 0 until 9) {
                val r = (60f + 30f * sin(i * 1.7f)) * puddle
                c.drawCircle(cx - 260f + i * 65f, cy + 330f + sin(i * 2.1f) * 16f, r, Gfx.p(Color.WHITE))
            }
        }
        val rise = Ease.win(time, 0.1f, 0.7f)
        if (time < 0.8f) {
            val by = cy + 330f - Ease.outCubic(rise) * 530f
            Gfx.bubble(c, cx, by, 40f + rise * 130f)
        }
        c.save(); c.translate(cx, cy)
        val pulse = 1.22f * (1f + 0.02f * sin(time * 4f))
        c.scale(pulse, pulse)
        LogoArt.full(c, time, emblem = Ease.win(time, 0.75f, 0.9f), letters = ::letterT, ribbon = Ease.win(time, 2.0f, 0.4f))
        c.restore()
        fx.draw(c)
        val a = Ease.win(time, 2.4f, 0.4f) * (0.6f + 0.4f * sin(time * 5f))
        Gfx.plain(c, "Loading the bubbles...", W / 2, H - 140f, 42f, Col.alpha(Color.WHITE, a))
    }
}
