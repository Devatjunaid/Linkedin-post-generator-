package com.sudzy.carwash.scenes

import android.graphics.Canvas
import android.graphics.Color
import com.sudzy.carwash.art.CarArt
import com.sudzy.carwash.art.FaceArt
import com.sudzy.carwash.art.Icon
import com.sudzy.carwash.art.IconArt
import com.sudzy.carwash.art.LogoArt
import com.sudzy.carwash.art.Mood
import com.sudzy.carwash.art.StickerArt
import com.sudzy.carwash.engine.Button
import com.sudzy.carwash.engine.ButtonGroup
import com.sudzy.carwash.engine.Col
import com.sudzy.carwash.engine.Ease
import com.sudzy.carwash.engine.Game
import com.sudzy.carwash.engine.Gfx
import com.sudzy.carwash.engine.PType
import com.sudzy.carwash.engine.Particles
import com.sudzy.carwash.engine.Scene
import com.sudzy.carwash.engine.Touch
import com.sudzy.carwash.game.Levels
import kotlin.math.PI
import kotlin.math.sin

class RewardScene(game: Game, private val index: Int, private val stars: Int, private val coins: Int, private val tip: Int, private val newWorld: Boolean) : Scene(game) {
    override val music = "menu"
    private val lv = Levels.get(index)
    private val model = lv.model()
    private val ui = ButtonGroup()
    private val fx = Particles(600)
    private val starPopped = BooleanArray(3)
    private var shown = 0f
    private val title = arrayOf("SPOTLESS!", "SPARKLING!", "SHINY!", "SQUEAKY CLEAN!", "DAZZLING!")[index % 5]
    private val stickers = game.save.stickers(index).split(";").filter { it.isNotBlank() }.map { it.split(",").map(String::toInt) }

    override fun onEnter() {
        ui.add(Button(40f, H - 280f, 200f, 200f, 0xFF5AA8FF.toInt(), 0xFF2D6CF0.toInt(), round = true,
            art = { c, _, _ -> IconArt.draw(c, Icon.HOME, 0f, 0f, 96f) }) { game.go(HomeScene(game)) })
        if (index < Levels.COUNT - 1) {
            ui.add(Button(290f, H - 280f, W - 330f, 200f, Col.LIME, 0xFF22C13A.toInt(), pulse = true, art = { c, _, _ ->
                Gfx.outlined(c, "NEXT CAR", -30f, 34f, 96f, Color.WHITE, 0xFF0F7A22.toInt())
                IconArt.draw(c, Icon.NEXT, 250f, 0f, 80f)
            }) { game.startLevel(index + 1) })
        } else {
            ui.add(Button(290f, H - 280f, W - 330f, 200f, 0xFFFFE36B.toInt(), 0xFFFFB000.toInt(), pulse = true, art = { c, _, _ ->
                Gfx.outlined(c, "ROAD TRIP", 0f, 34f, 90f, Color.WHITE, 0xFFB05C00.toInt())
            }) { game.go(MapScene(game)) })
        }
        ui.list.forEach { it.appear = -1.6f }
        fx.confetti(W, 160)
    }

    override fun update(dt: Float) {
        super.update(dt)
        fx.update(dt); ui.update(dt)
        ui.list.forEach { it.appear = (it.appear + dt * 2.5f).coerceAtMost(1f) }
        for (i in 0 until 3) if (!starPopped[i] && time > 0.6f + i * 0.35f) {
            starPopped[i] = true
            if (i < stars) {
                val sx = W / 2 + (i - 1) * 260f; val sy = 470f
                game.audio.sfx("star", 1f, 1f + i * 0.12f); game.vibrate(30)
                fx.burst(PType.SPARKLE, sx, sy, 22, 900f, 34f, 0.8f, intArrayOf(Color.WHITE, Col.YELLOW))
                game.shake(0.25f)
            }
        }
        if (time > 1.8f && shown < 1f) {
            if (shown == 0f) game.audio.sfx("coin")
            shown = (shown + dt * 1.2f).coerceAtMost(1f)
            if (((shown * 20).toInt()) != (((shown - dt * 1.2f) * 20).toInt())) game.audio.sfx("coin", 0.4f, 1.3f)
        }
    }

    override fun onTouch(t: Touch): Boolean = ui.touch(t, game)
    override fun onBack(): Boolean { game.go(HomeScene(game)); return true }

    override fun draw(c: Canvas) {
        MenuBg.draw(c, W, H, time, 0xFFFFF3A0.toInt(), 0xFFFFB21F.toInt(), 0xFFC2300A.toInt(), H * 0.4f)
        // title ribbon
        c.save(); c.translate(W / 2, 220f); val k = Ease.outElastic(Ease.win(time, 0f, 0.9f)); c.scale(k, k)
        LogoArt.ribbon(c, "", 820f, 170f)
        Gfx.outlined(c, title, 0f, 40f, if (title.length > 10) 92f else 120f, Color.WHITE, 0xFF8C0F47.toInt())
        c.restore()
        // stars
        for (i in 0 until 3) {
            val st = Ease.outBack(Ease.win(time, 0.6f + i * 0.35f, 0.35f), 3f)
            if (st <= 0f) continue
            val sx = W / 2 + (i - 1) * 260f; val sy = 470f + (if (i == 1) -40f else 0f)
            c.save(); c.translate(sx, sy); c.rotate((i - 1) * 14f); c.scale(st, st)
            Gfx.star(c, 0f, 0f, if (i == 1) 130f else 105f, i < stars)
            c.restore()
        }
        // car on podium
        val py = 1080f
        c.drawOval(W / 2 - 380f, py - 40f + 26f, W / 2 + 380f, py + 60f + 26f, Gfx.p(0xFFA85A00.toInt()))
        c.drawOval(W / 2 - 380f, py - 40f, W / 2 + 380f, py + 60f, Gfx.vGrad(py - 40f, py + 60f, 0xFFFFE9B0.toInt(), 0xFFE39A00.toInt()))
        c.drawOval(W / 2 - 380f, py - 40f, W / 2 + 380f, py + 60f, Gfx.s(Color.WHITE, 8f))
        val q = 2.1f
        c.save(); c.translate(W / 2 - 200f * q, py - 180f * q + 10f + sin(time * 2f) * 6f); c.scale(q, q)
        CarArt.shadow(c, model)
        c.save(); c.translate(0f, -model.lift); CarArt.body(c, model, 1f, time)
        for (s in stickers) if (s.size == 4) StickerArt.draw(c, s[0], s[1].toFloat(), s[2].toFloat(), 22f, s[3].toFloat())
        c.restore()
        CarArt.wheels(c, model, 0f, floatArrayOf(1f, 1f))
        c.restore()
        for (i in 0 until 6) {
            val a = time * 1.5f + i * PI.toFloat() / 3
            Gfx.sparkle(c, W / 2 + sin(a) * 420f, 900f + sin(a * 1.3f) * 120f, 26f * (0.5f + 0.5f * sin(time * 5 + i)))
        }
        // customer card
        val cy = 1180f
        val ca = Ease.outBack(Ease.win(time, 1.3f, 0.4f), 2f)
        if (ca > 0f) {
            c.save(); c.scale(ca, ca, W / 2, cy + 110f)
            Gfx.roundRect(c, 50f, cy + 16f, W - 50f, cy + 236f, 60f, Gfx.p(0x55802800))
            Gfx.roundRect(c, 50f, cy, W - 50f, cy + 220f, 60f, Gfx.p(Color.WHITE))
            FaceArt.draw(c, lv.world, lv.customer, Mood.WOW, 170f, cy + 110f, 170f, time)
            Gfx.plain(c, if (lv.vip) "\"VIP shine! You're the best!\"" else "\"WOW! It looks brand new!\"", 280f, cy + 85f, 46f, 0xFFC2410C.toInt(), android.graphics.Paint.Align.LEFT, com.sudzy.carwash.engine.Fonts.title)
            Gfx.plain(c, "Tip:", 280f, cy + 165f, 46f, 0xFF3A2A10.toInt(), android.graphics.Paint.Align.LEFT)
            Gfx.outlined(c, "+$tip", 440f, cy + 168f, 60f, 0xFFFFB000.toInt(), Color.WHITE, strokeMul = 0.08f, depth = 0f)
            Gfx.coin(c, 560f, cy + 148f, 30f)
            for (h in 0 until 3) heart(c, 660f + h * 70f, cy + 145f, 26f)
            c.restore()
        }
        // coins
        val shownCoins = (coins * shown).toInt()
        if (time > 1.7f) Gfx.pill(c, W / 2 - 220f, cy + 260f, 440f, 110f, "+$shownCoins") { cv, x, y, r -> Gfx.coin(cv, x, y, r) }
        if (newWorld) {
            val nw = Ease.outElastic(Ease.win(time, 2.4f, 0.8f))
            if (nw > 0f) {
                c.save(); c.translate(W / 2, 680f); c.rotate(-4f); c.scale(nw * 0.9f, nw * 0.9f)
                Gfx.candy(c, -380f, -70f, 380f, 70f, 0xFFB48BFF.toInt(), 0xFF7A45FF.toInt(), 40f)
                Gfx.outlined(c, "NEW WORLD: ${Levels.worlds[(lv.world + 1).coerceAtMost(5)].name}!", 0f, 18f, 50f, Color.WHITE, 0xFF4A1FB8.toInt(), strokeMul = 0.1f)
                c.restore()
            }
        }
        fx.draw(c)
        ui.draw(c, time)
    }

    private fun heart(c: Canvas, x: Float, y: Float, r: Float) {
        val p = android.graphics.Path()
        p.moveTo(x, y + r); p.cubicTo(x - r * 1.6f, y - r * 0.2f, x - r * 0.6f, y - r * 1.3f, x, y - r * 0.4f); p.cubicTo(x + r * 0.6f, y - r * 1.3f, x + r * 1.6f, y - r * 0.2f, x, y + r)
        c.drawPath(p, Gfx.p(0xFFFF3B6B.toInt()))
    }
}
