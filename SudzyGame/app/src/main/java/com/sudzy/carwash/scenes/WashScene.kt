package com.sudzy.carwash.scenes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.sudzy.carwash.art.CarArt
import com.sudzy.carwash.art.FaceArt
import com.sudzy.carwash.art.Icon
import com.sudzy.carwash.art.IconArt
import com.sudzy.carwash.art.Mood
import com.sudzy.carwash.art.StickerArt
import com.sudzy.carwash.art.WorldArt
import com.sudzy.carwash.engine.Button
import com.sudzy.carwash.engine.ButtonGroup
import com.sudzy.carwash.engine.Col
import com.sudzy.carwash.engine.Ease
import com.sudzy.carwash.engine.FloatText
import com.sudzy.carwash.engine.Game
import com.sudzy.carwash.engine.Gfx
import com.sudzy.carwash.engine.PType
import com.sudzy.carwash.engine.Particles
import com.sudzy.carwash.engine.Rng
import com.sudzy.carwash.engine.Scene
import com.sudzy.carwash.engine.Touch
import com.sudzy.carwash.game.CoverLayer
import com.sudzy.carwash.game.DirtPainter
import com.sudzy.carwash.game.Levels
import com.sudzy.carwash.game.Spot
import com.sudzy.carwash.game.SpotArt
import com.sudzy.carwash.game.Step
import com.sudzy.carwash.game.Tool
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class WashScene(game: Game, val index: Int) : Scene(game) {
    override val music = "wash"
    private val lv = Levels.get(index)
    private val world = lv.world
    private val wd = lv.w
    private val model = lv.model()
    private val rng = Rng(index * 131L + 17)
    private val q = 960f / 420f

    private val floorY get() = H - 550f
    private val carX get() = W / 2 - 200f * q
    private val carY get() = floorY - 180f * q
    private val trayTop get() = H - 300f

    private lateinit var dirt: CoverLayer
    private lateinit var foam: CoverLayer
    private lateinit var wax: CoverLayer
    private val spots = mutableListOf<Spot>()
    private val tough = mutableListOf<Spot>()
    private class Scratch(val x: Float, val y: Float, val len: Float, val ang: Float, var hp: Float = 1f)
    private val scratches = mutableListOf<Scratch>()
    private class Drop(var x: Float, var y: Float, val r: Float, var alive: Boolean = true)
    private val drops = mutableListOf<Drop>()
    private var dropsTotal = 0
    private val tires = FloatArray(2)
    private class Sticker(val kind: Int, val x: Float, val y: Float, val rot: Float, var t: Float = 0f)
    private val stickers = mutableListOf<Sticker>()
    private var stickerChoice = 0
    private var shine = 0f

    private enum class State { INTRO, WORK, STEP_DONE, FINALE }
    private var state = State.INTRO
    private var stateT = 0f
    private var stepIdx = 0
    private val step get() = lv.steps[stepIdx.coerceAtMost(lv.steps.size - 1)]
    private val tools = lv.steps.map { it.tool }.distinct()

    private val fx = Particles()
    private val floats = mutableListOf<FloatText>()
    private var combo = 0; private var comboMeter = 0f; private var lastWork = -10f
    private var bonusCoins = 0
    private var elapsed = 0f
    private var touching = false
    private var px = 0f; private var py = 0f
    private var lastLx = Float.NaN; private var lastLy = Float.NaN
    private var touchedThisStep = false
    private var loopOn: String? = null
    private var squeakT = 0f
    private var carBounce = 0f
    private var praise = ""
    private var goldX = 0f; private var goldY = -1f; private var goldT = 4f
    private val rushTime = 20f + lv.steps.size * 7f
    private val ui = ButtonGroup()
    private val path = Path()

    override fun onEnter() {
        val bq = min(3f, q * game.scale)
        dirt = CoverLayer(model, bq, true)
        DirtPainter.paint(dirt, world, wd, index * 7L + 3)
        foam = CoverLayer(model, bq, false)
        wax = CoverLayer(model, bq, false)
        repeat(lv.spots) { spots += randomSpot(wd.spot, false, it) }
        repeat(lv.toughSpots) { tough += randomSpot(wd.tough, true, it + 50) }
        repeat(lv.scratches) {
            val (x, y) = randomPoint()
            scratches += Scratch(x, y, rng.range(28f, 60f), rng.range(-0.5f, 0.5f))
        }
        ui.add(Button(30f, 30f, 120f, 120f, 0xFF5AA8FF.toInt(), 0xFF2D6CF0.toInt(), round = true,
            art = { c, _, _ -> IconArt.draw(c, Icon.HOME, 0f, 0f, 64f) }) { game.go(MapScene(game)) })
        game.audio.sfx("honk")
    }

    override fun onExit() {
        loopOn?.let { game.audio.stopLoop(it) }; loopOn = null
        dirt.recycle(); foam.recycle(); wax.recycle()
    }

    private fun randomPoint(): Pair<Float, Float> {
        repeat(80) {
            val x = rng.range(50f, 350f); val y = rng.range(55f, 140f)
            if (model.inside(x, y) && model.inside(x + 12f, y) && model.inside(x - 12f, y) && model.inside(x, y + 12f) && model.inside(x, y - 12f)) return x to y
        }
        return 200f to 120f
    }

    private fun randomSpot(kind: com.sudzy.carwash.game.SpotKind, isTough: Boolean, seed: Int): Spot {
        val (x, y) = randomPoint()
        return Spot(kind, x, y, rng.range(13f, 20f) * (if (isTough) 1.2f else 1f), isTough, seed)
    }

    // ------------------------------------------------------------------ progress
    private fun progress(): Float = when (step) {
        Step.RINSE_DIRT -> dirt.cleared()
        Step.FOAM -> foam.filled()
        Step.SCRUB -> spotsProgress(spots)
        Step.SCRUB_TOUGH -> spotsProgress(tough)
        Step.RINSE_FOAM -> foam.cleared()
        Step.DRY -> if (dropsTotal == 0) 1f else 1f - drops.count { it.alive }.toFloat() / dropsTotal
        Step.POLISH -> if (scratches.isEmpty()) 1f else 1f - scratches.sumOf { it.hp.toDouble() }.toFloat() / scratches.size
        Step.WAX -> wax.filled()
        Step.TIRES -> (tires[0] + tires[1]) / 2f
        Step.STICKERS -> stickers.size / 3f
    }

    private fun spotsProgress(list: List<Spot>) = if (list.isEmpty()) 1f else 1f - list.sumOf { it.hp.coerceAtLeast(0f).toDouble() }.toFloat() / list.size

    private fun threshold() = when (step) {
        Step.RINSE_DIRT, Step.FOAM, Step.RINSE_FOAM, Step.WAX -> 0.92f
        Step.TIRES -> 0.98f
        else -> 0.999f
    }

    private fun completeStep() {
        stopLoop()
        when (step) {
            Step.FOAM -> foam.completeFill { cv, x, y -> DirtPainter.foamBlob(cv, x, y, rng, 0.8f) }
            Step.SCRUB -> spots.forEach { it.hp = 0f }
            Step.SCRUB_TOUGH -> tough.forEach { it.hp = 0f }
            Step.RINSE_FOAM -> if (Step.DRY in lv.steps) {
                repeat(lv.drops) { val (x, y) = randomPoint(); drops += Drop(x, y, rng.range(6f, 11f)) }
                dropsTotal = drops.size
            }
            Step.DRY -> drops.forEach { it.alive = false }
            Step.POLISH -> scratches.forEach { it.hp = 0f }
            Step.TIRES -> { tires[0] = 1f; tires[1] = 1f }
            else -> {}
        }
        praise = PRAISE[(stepIdx + index) % PRAISE.size]
        state = State.STEP_DONE; stateT = 0f
        carBounce = 1f
        game.audio.sfx("star"); game.vibrate(40)
        val cx = carX + 200f * q; val cy = carY + 90f * q
        fx.burst(PType.SPARKLE, cx, cy, 30, 900f, 30f, 0.9f, intArrayOf(Color.WHITE, Col.YELLOW, Col.CYAN))
        fx.burst(PType.STAR, cx, cy - 100f, 6, 700f, 26f, 1f, gravity = 900f)
    }

    private fun nextStep() {
        stepIdx++
        touchedThisStep = false
        lastLx = Float.NaN
        if (stepIdx >= lv.steps.size) startFinale() else { state = State.WORK; stateT = 0f; game.audio.sfx("pop") }
    }

    private fun startFinale() {
        state = State.FINALE; stateT = 0f
        game.audio.sfx("tada"); game.vibrate(80, 200)
        fx.confetti(W, 140)
    }

    private fun finish() {
        val stars = run {
            val par = lv.steps.size * 8f + 6f
            when { elapsed <= par -> 3; elapsed <= par * 1.7f -> 2; else -> 1 }
        }
        var tip = 30 + 10 * world
        if (lv.rush) tip += (60 * ((rushTime - elapsed) / rushTime).coerceIn(0f, 1f)).toInt()
        if (lv.vip) tip *= 2
        val coins = 50 + 10 * index + stars * 20 + bonusCoins
        val save = game.save
        val firstTime = save.unlocked <= index
        save.setStars(index, stars)
        if (firstTime) save.unlocked = index + 1
        save.coins = save.coins + coins + tip
        save.levelsPlayed = save.levelsPlayed + 1
        if (stickers.isNotEmpty()) save.setStickers(index, stickers.joinToString(";") { "${it.kind},${it.x.toInt()},${it.y.toInt()},${it.rot.toInt()}" })
        game.go(RewardScene(game, index, stars, coins, tip, newWorld = firstTime && index % 5 == 4 && index < 29), wipe = true)
    }

    // ------------------------------------------------------------------ update
    override fun update(dt: Float) {
        super.update(dt)
        stateT += dt
        ui.update(dt)
        fx.update(dt)
        floats.forEach { it.t += dt }; floats.removeAll { !it.alive() }
        carBounce = (carBounce - dt * 2.5f).coerceAtLeast(0f)
        stickers.forEach { it.t += dt }
        if (stepIdx > 0 || state == State.STEP_DONE) dirt.alpha = (dirt.alpha - dt * 2.5f).coerceAtLeast(0f)
        val foamGone = stepIdx > lv.steps.indexOf(Step.RINSE_FOAM) || (step == Step.RINSE_FOAM && state == State.STEP_DONE)
        if (foamGone) foam.alpha = (foam.alpha - dt * 2.5f).coerceAtLeast(0f)
        val waxDone = Step.WAX in lv.steps && (stepIdx > lv.steps.indexOf(Step.WAX) || (step == Step.WAX && state == State.STEP_DONE))
        if (waxDone) { wax.alpha = (wax.alpha - dt * 1.5f).coerceAtLeast(0f); shine = (shine + dt * 1.2f).coerceAtMost(1f) }

        when (state) {
            State.INTRO -> if (stateT > 1.9f) { state = State.WORK; stateT = 0f; game.audio.sfx("pop") }
            State.WORK -> {
                elapsed += dt
                if (time - lastWork > 0.7f) combo = 0
                if (progress() >= threshold()) completeStep()
                updateGold(dt)
                if (touching && step.tool.loop != null && time - lastWork < 0.15f) startLoop(step.tool.loop!!) else stopLoop()
            }
            State.STEP_DONE -> if (stateT > 1.0f) nextStep()
            State.FINALE -> {
                if (stateT > 0.6f && stateT < 1.8f && rng.chance(0.4f)) {
                    fx.spawn(PType.SPARKLE, carX + rng.range(40f, 360f) * q, carY + rng.range(40f, 150f) * q, 0f, -40f, rng.range(18f, 34f), 0.8f, Color.WHITE)
                }
                if (stateT > 3.0f) { state = State.INTRO; stateT = -100f; finish() }
            }
        }
    }

    private fun updateGold(dt: Float) {
        if (!lv.goldenBubbles) return
        if (goldY < 0f) {
            goldT -= dt
            if (goldT <= 0f) { goldX = rng.range(120f, W - 120f); goldY = trayTop - 40f }
        } else {
            goldY -= dt * 120f
            if (goldY < 200f) { goldY = -1f; goldT = rng.range(5f, 9f) }
        }
    }

    private fun startLoop(name: String) { if (loopOn != name) { stopLoop(); game.audio.startLoop(name); loopOn = name } }
    private fun stopLoop() { loopOn?.let { game.audio.stopLoop(it) }; loopOn = null }

    // ------------------------------------------------------------------ input
    override fun onTouch(t: Touch): Boolean {
        if (ui.touch(t, game)) return true
        if (t.action == Touch.DOWN && goldY > 0f && hypot(t.x - goldX, t.y - goldY) < 90f) {
            goldY = -1f; goldT = rng.range(5f, 9f); bonusCoins += 15
            fx.burst(PType.COIN, t.x, t.y, 8, 700f, 26f, 1f, gravity = 1400f)
            fx.burst(PType.SPARKLE, t.x, t.y, 14, 600f, 24f, 0.6f, intArrayOf(Col.YELLOW))
            floats += FloatText("+15", t.x, t.y - 40f, 70f, Col.YELLOW, 0xFF8A5A00.toInt())
            game.audio.sfx("coin"); game.vibrate(25)
            return true
        }
        if (state != State.WORK) return false
        if (step == Step.STICKERS) return stickerTouch(t)
        when (t.action) {
            Touch.DOWN -> { if (t.y > trayTop - 10f) return false; touching = true; px = t.x; py = t.y; lastLx = Float.NaN; touchedThisStep = true; work() }
            Touch.MOVE -> if (touching) { px = t.x; py = t.y.coerceAtMost(trayTop - 10f); work() }
            Touch.UP -> { touching = false; stopLoop() }
        }
        return true
    }

    private fun stickerTouch(t: Touch): Boolean {
        if (t.action != Touch.UP) return true
        if (t.y > trayTop) {
            val slot = ((t.x - 30f) / ((W - 60f) / 5)).toInt().coerceIn(0, 4)
            stickerChoice = slot; game.audio.sfx("click"); return true
        }
        val lx = (t.x - carX) / q; val ly = (t.y - carY) / q + model.lift
        if (model.inside(lx, ly) && stickers.size < 3) {
            stickers += Sticker(stickerChoice, lx, ly, rng.range(-20f, 20f))
            fx.burst(PType.SPARKLE, t.x, t.y, 16, 600f, 26f, 0.7f, intArrayOf(Color.WHITE, Col.YELLOW, Col.PINK))
            floats += FloatText("COOL!", t.x, t.y - 60f, 70f, Col.CYAN, Col.NAVY)
            game.audio.sfx("sticker"); game.vibrate(20)
            touchedThisStep = true
        } else game.audio.sfx("boing")
        return true
    }

    /** Effect point is a little above the finger so kids can see what they clean. */
    private fun work() {
        val ex = px; val ey = py - 90f
        val lx = (ex - carX) / q; val ly = (ey - carY) / q + model.lift
        if (lastLx.isNaN()) { stamp(lx, ly, ex, ey); lastLx = lx; lastLy = ly; return }
        val d = hypot(lx - lastLx, ly - lastLy)
        val n = max(1, (d / 6f).toInt())
        for (i in 1..n) {
            val k = i.toFloat() / n
            val sx = lastLx + (lx - lastLx) * k; val sy = lastLy + (ly - lastLy) * k
            stamp(sx, sy, carX + sx * q, carY + (sy - model.lift) * q)
        }
        lastLx = lx; lastLy = ly
    }

    private fun stamp(lx: Float, ly: Float, vx: Float, vy: Float) {
        val hp = wd.hp
        var gain = 0f
        when (step) {
            Step.RINSE_DIRT -> {
                gain = dirt.eraseAt(lx, ly, 26f, 0.16f / hp)
                if (rng.chance(0.5f)) fx.spawn(PType.DROP, vx, vy, rng.range(-500f, 500f), rng.range(-700f, -100f), rng.range(12f, 20f), 0.7f, 0xFF8FDFFF.toInt(), 2200f)
                if (gain > 0f && rng.chance(0.6f)) fx.spawn(PType.CHUNK, vx, vy, rng.range(-300f, 300f), rng.range(-400f, 0f), rng.range(8f, 16f), 0.8f, wd.dirt2, 2000f)
            }
            Step.FOAM -> {
                gain = foam.fillAt(lx, ly, 26f, 0.3f) { cv -> DirtPainter.foamBlob(cv, lx, ly, rng) }
                if (rng.chance(0.3f)) fx.spawn(PType.BUBBLE, vx, vy, rng.range(-150f, 150f), rng.range(-260f, -80f), rng.range(14f, 30f), 1.2f)
            }
            Step.SCRUB, Step.SCRUB_TOUGH -> {
                val list = if (step == Step.SCRUB) spots else tough
                for (s in list) if (s.hp > 0f && SpotArt.dist(s.x, s.y, lx, ly) < s.r + 26f) {
                    val before = s.hp; s.hp -= if (s.tough) 0.03f else 0.055f
                    gain += before - s.hp.coerceAtLeast(0f)
                    if (s.hp <= 0f) {
                        fx.burst(PType.BUBBLE, carX + s.x * q, carY + (s.y - model.lift) * q, 6, 300f, 24f, 1f)
                        game.audio.sfx("pop", 0.8f, 1f + rng.range(-0.1f, 0.2f))
                    }
                }
                if (rng.chance(0.35f)) fx.spawn(PType.FOAM, vx + rng.range(-30f, 30f), vy + rng.range(-30f, 30f), rng.range(-60f, 60f), rng.range(-90f, 0f), rng.range(6f, 14f), 0.6f)
                squeakT -= 1f
                if (gain > 0f && squeakT <= 0f) { game.audio.sfx("squeak", 0.6f, rng.range(0.9f, 1.3f)); squeakT = 22f }
            }
            Step.RINSE_FOAM -> {
                gain = foam.eraseAt(lx, ly, 28f, 0.28f)
                if (rng.chance(0.5f)) fx.spawn(PType.DROP, vx, vy, rng.range(-500f, 500f), rng.range(-700f, -100f), rng.range(12f, 20f), 0.7f, 0xFF8FDFFF.toInt(), 2200f)
                if (gain > 0f && rng.chance(0.4f)) fx.spawn(PType.FOAM, vx, vy, rng.range(-300f, 300f), rng.range(-400f, 0f), rng.range(6f, 12f), 0.7f, gravity = 1500f)
            }
            Step.DRY -> {
                for (d in drops) if (d.alive && SpotArt.dist(d.x, d.y, lx, ly) < 50f) {
                    d.alive = false; gain += 1f
                    val a = atan2(d.y - ly, d.x - lx)
                    fx.spawn(PType.DROP, carX + d.x * q, carY + (d.y - model.lift) * q, cos(a) * 900f, sin(a) * 900f - 200f, d.r * 2f, 0.6f, 0xFF8FDFFF.toInt(), 1500f)
                    game.audio.sfx("drip", 0.5f, rng.range(0.9f, 1.4f))
                }
                if (rng.chance(0.5f)) fx.spawn(PType.WIND, vx, vy, rng.range(-900f, 900f), rng.range(-300f, 300f), 16f, 0.35f, 0xFFE0F6FF.toInt())
            }
            Step.POLISH -> {
                for (s in scratches) if (s.hp > 0f && SpotArt.dist(s.x, s.y, lx, ly) < s.len / 2 + 22f) { val b = s.hp; s.hp = (s.hp - 0.05f).coerceAtLeast(0f); gain += b - s.hp }
                if (rng.chance(0.3f)) fx.spawn(PType.SPARKLE, vx + rng.range(-40f, 40f), vy + rng.range(-40f, 40f), 0f, -60f, rng.range(14f, 26f), 0.5f, Color.WHITE)
            }
            Step.WAX -> {
                gain = wax.fillAt(lx, ly, 30f, 0.25f) { cv -> DirtPainter.waxSmear(cv, lx, ly, rng) }
                if (rng.chance(0.25f)) fx.spawn(PType.SPARKLE, vx + rng.range(-40f, 40f), vy + rng.range(-40f, 40f), 0f, -60f, rng.range(14f, 26f), 0.5f, 0xFFFFF6B0.toInt())
            }
            Step.TIRES -> {
                val wy = model.wheelY() + model.lift
                for (i in 0..1) if (hypot(lx - model.wheelX[i], ly - wy) < model.wheelR + 16f && tires[i] < 1f) {
                    val b = tires[i]; tires[i] = (tires[i] + 0.035f).coerceAtMost(1f); gain += tires[i] - b
                    if (rng.chance(0.4f)) fx.spawn(PType.SPARKLE, vx + rng.range(-30f, 30f), vy + rng.range(-30f, 30f), 0f, -40f, rng.range(14f, 24f), 0.5f)
                }
                if (rng.chance(0.3f)) fx.spawn(PType.BUBBLE, vx, vy, rng.range(-100f, 100f), rng.range(-200f, -60f), rng.range(6f, 12f), 0.6f)
            }
            Step.STICKERS -> {}
        }
        if (gain > 0f) {
            lastWork = time
            comboMeter += gain
            val need = when (step) { Step.SCRUB, Step.SCRUB_TOUGH, Step.POLISH -> 0.6f; Step.DRY -> 2f; Step.TIRES -> 0.25f; else -> 45f }
            if (comboMeter >= need) {
                comboMeter = 0f; combo++
                if (floats.size >= 2) floats.removeAt(0)
                if (combo >= 2) floats += FloatText("${step.tool.word} x$combo!", W / 2 + rng.range(-160f, 160f), carY - 30f, 78f, comboColor(), Col.shade(comboColor(), -0.55f))
                if (combo % 5 == 0) {
                    bonusCoins += 5; game.shake(0.35f); game.audio.sfx("combo"); game.vibrate(30)
                    fx.burst(PType.COIN, vx, vy, 5, 600f, 22f, 1f, gravity = 1400f)
                }
            }
        }
    }

    private fun comboColor() = intArrayOf(Col.YELLOW, Col.CYAN, 0xFFFF9AD0.toInt(), Col.LIME)[combo % 4]

    override fun onBack(): Boolean { game.go(MapScene(game)); return true }

    // ------------------------------------------------------------------ draw
    override fun draw(c: Canvas) {
        WorldArt.bay(c, world, W, H, floorY, time)
        drawCar(c)
        fx.draw(c)
        drawGold(c)
        floats.forEach { it.draw(c) }
        drawHud(c)
        drawTray(c)
        if (state == State.WORK) {
            if (!touchedThisStep) drawHint(c)
            if (touching) drawToolCursor(c)
        }
        if (state == State.STEP_DONE) {
            val k = Ease.outBack(Ease.win(stateT, 0f, 0.35f), 3f)
            val a = if (stateT > 0.75f) 1f - (stateT - 0.75f) / 0.25f else 1f
            c.save(); c.scale(k, k, W / 2, carY - 60f)
            Gfx.outlined(c, praise, W / 2, carY - 40f, 130f, Col.YELLOW, 0xFFC2410C.toInt(), alpha = a)
            c.restore()
        }
        if (state == State.FINALE) {
            val k = Ease.outElastic(Ease.win(stateT, 0.2f, 0.8f))
            c.save(); c.scale(k, k, W / 2, carY - 60f)
            Gfx.outlined(c, "SPARKLING!", W / 2, carY - 40f, 140f, 0xFFFF9AD0.toInt(), 0xFFA1135A.toInt())
            c.restore()
        }
        ui.draw(c, time)
    }

    private fun drawCar(c: Canvas) {
        val drive = if (state == State.INTRO) Ease.outCubic(Ease.win(stateT, 0f, 1.3f)) else 1f
        val ox = (1f - drive) * -W * 1.3f
        val rot = (1f - drive) * -1500f
        val bounceY = if (state == State.INTRO) sin(Ease.win(stateT, 1.2f, 0.5f) * Math.PI.toFloat()) * 0.05f else carBounce * 0.06f * sin(carBounce * 12f)
        c.save()
        c.translate(carX + ox, carY)
        c.scale(q, q)
        c.scale(1f + bounceY * 0.5f, 1f - bounceY, 200f, 180f)
        CarArt.shadow(c, model)
        c.save(); c.translate(0f, -model.lift)
        CarArt.body(c, model, shine, time)
        // under-the-dirt details
        for (s in scratches) if (s.hp > 0f) {
            c.save(); c.rotate(Math.toDegrees(s.ang.toDouble()).toFloat(), s.x, s.y)
            val pp = Gfx.s(Col.alpha(Color.WHITE, 0.75f * s.hp), 2.5f)
            path.reset(); path.moveTo(s.x - s.len / 2, s.y); path.quadTo(s.x, s.y - 6f, s.x + s.len / 2, s.y + 2f); c.drawPath(path, pp)
            path.reset(); path.moveTo(s.x - s.len / 3, s.y + 6f); path.lineTo(s.x + s.len / 4, s.y + 4f); c.drawPath(path, pp)
            c.restore()
        }
        stickers.forEach { StickerArt.draw(c, it.kind, it.x, it.y, 22f * Ease.outBack(Ease.win(it.t, 0f, 0.3f), 3f), it.rot) }
        val spotsOnTop = dirt.alpha <= 0f
        if (!spotsOnTop) { spots.forEach { SpotArt.draw(c, it) }; tough.forEach { SpotArt.draw(c, it) } }
        dirt.draw(c)
        foam.draw(c)
        if (spotsOnTop) { spots.forEach { SpotArt.draw(c, it) }; tough.forEach { SpotArt.draw(c, it) } }
        wax.draw(c)
        for (d in drops) if (d.alive) {
            c.drawOval(d.x - d.r * 0.8f, d.y - d.r, d.x + d.r * 0.8f, d.y + d.r, Gfx.p(0x998FDFFF.toInt()))
            c.drawOval(d.x - d.r * 0.8f, d.y - d.r, d.x + d.r * 0.8f, d.y + d.r, Gfx.s(0xCCFFFFFF.toInt(), 1.5f))
            c.drawCircle(d.x - d.r * 0.3f, d.y - d.r * 0.4f, d.r * 0.25f, Gfx.p(Color.WHITE))
        }
        if (state == State.FINALE) {
            // a gleam sweeps across the freshly cleaned body
            val k = Ease.win(stateT, 0.3f, 1.1f)
            if (k in 0.001f..0.999f) {
                c.save(); c.clipPath(model.body)
                val gx = -80f + k * 560f
                path.reset(); path.moveTo(gx, 0f); path.lineTo(gx + 50f, 0f); path.lineTo(gx - 30f, 200f); path.lineTo(gx - 80f, 200f); path.close()
                c.drawPath(path, Gfx.p(0xAAFFFFFF.toInt()))
                path.reset(); path.moveTo(gx + 70f, 0f); path.lineTo(gx + 86f, 0f); path.lineTo(gx + 6f, 200f); path.lineTo(gx - 10f, 200f); path.close()
                c.drawPath(path, Gfx.p(0x88FFFFFF.toInt()))
                c.restore()
            }
        }
        c.restore()
        val wheelDirt = if (stepIdx == 0 && state != State.STEP_DONE) 1f - progress() else 0f
        CarArt.wheels(c, model, rot, tires, wheelDirt, wd.dirt2)
        if (shine > 0f) {
            val tw = { o: Float -> 0.5f + 0.5f * sin(time * 4f + o) }
            Gfx.sparkle(c, 330f, 70f - model.lift, 18f * shine * tw(0f))
            Gfx.sparkle(c, 92f, 96f - model.lift, 13f * shine * tw(1.3f))
            Gfx.sparkle(c, 250f, 40f - model.lift, 11f * shine * tw(2.6f))
        }
        c.restore()
    }

    private fun drawGold(c: Canvas) {
        if (goldY < 0f) return
        val x = goldX + sin(time * 3f) * 30f
        c.drawCircle(x, goldY, 70f, Gfx.radial(x - 20f, goldY - 20f, 80f, 0xEEFFF3A0.toInt(), 0x99FFB000.toInt()))
        c.drawCircle(x, goldY, 70f, Gfx.s(0xFFFFE14D.toInt(), 6f))
        Gfx.coin(c, x, goldY, 34f)
        c.drawArc(x - 50f, goldY - 50f, x + 50f, goldY + 50f, 200f, 60f, false, Gfx.s(Color.WHITE, 8f))
        Gfx.sparkle(c, x + 50f, goldY - 50f, 16f * (0.5f + 0.5f * sin(time * 8f)))
    }

    private fun drawHud(c: Canvas) {
        val bx = 180f; val bw = W - 180f - 300f; val by = 58f; val bh = 56f
        Gfx.roundRect(c, bx, by, bx + bw, by + bh, bh / 2, Gfx.p(0x80001440.toInt()))
        val total = lv.steps.size
        val p = when (state) {
            State.WORK -> (stepIdx + progress().coerceIn(0f, 1f)) / total
            State.STEP_DONE -> (stepIdx + 1f) / total
            State.FINALE -> 1f
            else -> 0f
        }
        if (p > 0.01f) Gfx.roundRect(c, bx + 6f, by + 6f, bx + 6f + (bw - 12f) * p, by + bh - 6f, (bh - 12f) / 2, Gfx.vGrad(by, by + bh, 0xFF7DFFE0.toInt(), 0xFF22C17A.toInt()))
        Gfx.roundRect(c, bx, by, bx + bw, by + bh, bh / 2, Gfx.s(Color.WHITE, 7f))
        for (i in 0 until total) {
            val nx = bx + bw * (i + 1f) / total - 6f
            val done = i < stepIdx || (i == stepIdx && state == State.STEP_DONE) || state == State.FINALE
            val cur = i == stepIdx && state == State.WORK
            val slotW = bw / total
            val r = (if (cur) 40f else 30f).coerceAtMost(slotW * (if (cur) 0.6f else 0.45f))
            c.drawCircle(nx, by + bh / 2, r, Gfx.p(if (done) 0xFF22C17A.toInt() else if (cur) 0xFFFFC21A.toInt() else 0xFF5D6789.toInt()))
            c.drawCircle(nx, by + bh / 2, r, Gfx.s(Color.WHITE, 5f))
            if (done) IconArt.draw(c, Icon.CHECK, nx, by + bh / 2, r * 1.3f) else IconArt.draw(c, lv.steps[i].tool.icon, nx, by + bh / 2, r * 1.5f)
        }
        Gfx.pill(c, W - 230f, 52f, 200f, 70f, "${game.save.coins + bonusCoins}") { cv, x, y, r -> Gfx.coin(cv, x, y, r) }
        // customer speech
        val mood = when (state) { State.INTRO -> Mood.WORRIED; State.FINALE -> Mood.WOW; else -> Mood.HAPPY }
        val text = when (state) {
            State.INTRO -> wd.intro[index % 5]
            State.FINALE -> if (lv.vip) "WOW! VIP shine! Thank you!" else "WOW! It looks brand new!"
            else -> step.hint
        }
        FaceArt.speech(c, world, lv.customer, mood, text, 30f, 170f, W - 60f, time, Ease.win(time, 0.3f, 0.4f))
        if (lv.vip) {
            Gfx.candy(c, W - 200f, 196f, W - 40f, 266f, 0xFFFFE36B.toInt(), 0xFFFFB000.toInt(), 30f, border = 5f, depth = 8f)
            Gfx.outlined(c, "VIP", W - 120f, 248f, 50f, Color.WHITE, 0xFFB36B00.toInt(), strokeMul = 0.1f, depth = 0.04f)
        }
        if (lv.rush && state == State.WORK) {
            val left = (rushTime - elapsed).coerceAtLeast(0f)
            val col = if (left < 8f) 0xFFFF3B3B.toInt() else 0xFFFF8A2B.toInt()
            val pulse = if (left < 8f) 1f + 0.08f * sin(time * 14f) else 1f
            c.save(); c.scale(pulse, pulse, W - 130f, 330f)
            Gfx.candy(c, W - 250f, 300f, W - 30f, 370f, Col.shade(col, 0.3f), col, 35f, border = 5f, depth = 8f)
            Gfx.outlined(c, if (left > 0f) "RUSH ${left.toInt()}" else "RUSH!", W - 140f, 352f, 44f, Color.WHITE, Col.shade(col, -0.5f), strokeMul = 0.1f, depth = 0.04f)
            c.restore()
        }
    }

    private fun drawTray(c: Canvas) {
        val t = trayTop
        Gfx.roundRect(c, 24f, t + 24f, W - 24f, H - 20f, 60f, Gfx.p(0xFF8FA6DC.toInt()))
        Gfx.roundRect(c, 24f, t, W - 24f, H - 40f, 60f, Gfx.vGrad(t, H - 40f, Color.WHITE, 0xFFDDE7FF.toInt()))
        if (state == State.WORK && step == Step.STICKERS) {
            val sw = (W - 60f) / 5
            for (i in 0 until 5) {
                val cx = 30f + sw * (i + 0.5f); val cy = t + (H - 40f - t) / 2 - 10f
                val on = i == stickerChoice
                if (on) Gfx.candy(c, cx - 82f, cy - 82f, cx + 82f, cy + 82f, 0xFFFFE36B.toInt(), 0xFFFFB000.toInt(), 40f, border = 6f, depth = 8f)
                StickerArt.draw(c, i, cx, cy, if (on) 62f else 50f, 0f)
            }
            Gfx.plain(c, "${stickers.size} / 3", W / 2, t - 20f, 44f, Color.WHITE)
            return
        }
        val n = tools.size
        val slot = (W - 80f) / n.coerceAtLeast(5)
        val start = W / 2 - slot * n / 2
        for ((i, tool) in tools.withIndex()) {
            val cx = start + slot * (i + 0.5f); val cy = t + 120f
            val active = state == State.WORK && step.tool == tool
            val done = lv.steps.withIndex().filter { it.value.tool == tool }.all { it.index < stepIdx }
            val sz = slot * 0.78f
            val lift = if (active) -40f + sin(time * 6f) * 6f else 0f
            c.save(); c.translate(0f, lift)
            if (active) {
                c.drawCircle(cx, cy, sz * 0.75f, Gfx.radial(cx, cy, sz * 0.8f, 0xAAFFE14D.toInt(), 0x00FFE14D))
                Gfx.candy(c, cx - sz / 2, cy - sz / 2, cx + sz / 2, cy + sz / 2, 0xFFFFE36B.toInt(), 0xFFFFB000.toInt(), sz * 0.3f, border = 6f, depth = 10f)
            } else Gfx.candy(c, cx - sz / 2, cy - sz / 2, cx + sz / 2, cy + sz / 2, 0xFFF3F7FF.toInt(), 0xFFC9D6F5.toInt(), sz * 0.3f, border = 0f, depth = 10f)
            IconArt.draw(c, tool.icon, cx, cy, sz * 0.72f)
            if (done && !active) {
                c.drawCircle(cx + sz * 0.38f, cy - sz * 0.38f, 24f, Gfx.p(0xFF22C17A.toInt())); IconArt.draw(c, Icon.CHECK, cx + sz * 0.38f, cy - sz * 0.38f, 34f)
            }
            c.restore()
            Gfx.outlined(c, tool.label, cx, cy + sz / 2 + 52f, (slot * 0.26f).coerceAtMost(36f), if (active) 0xFFFF8A00.toInt() else 0xFF3554A8.toInt(), Color.WHITE, strokeMul = 0.14f, depth = 0f)
        }
    }

    private fun drawHint(c: Canvas) {
        val tt = time * 2.2f
        val hx: Float; val hy: Float
        if (step == Step.TIRES) { hx = carX + model.wheelX[(tt / 3.14f).toInt() % 2] * q + sin(tt * 3f) * 30f; hy = carY + model.wheelY() * q + 90f }
        else { hx = carX + (200f + 140f * sin(tt)) * q; hy = carY + 100f * q + 90f + cos(tt * 2f) * 20f }
        c.drawCircle(hx, hy - 90f, 44f + 8f * sin(time * 8f), Gfx.s(0xAAFFFFFF.toInt(), 8f))
        drawHand(c, hx, hy)
    }

    private fun drawHand(c: Canvas, x: Float, y: Float) {
        c.save(); c.translate(x, y); c.rotate(-15f)
        val skin = 0xFFFFDDB8.toInt(); val out = 0xFF7A4A22.toInt()
        Gfx.roundRect(c, -18f, -100f, 18f, 0f, 18f, Gfx.p(skin)); Gfx.roundRect(c, -18f, -100f, 18f, 0f, 18f, Gfx.s(out, 5f))
        Gfx.roundRect(c, -40f, -30f, 50f, 70f, 34f, Gfx.p(skin)); Gfx.roundRect(c, -40f, -30f, 50f, 70f, 34f, Gfx.s(out, 5f))
        Gfx.roundRect(c, -14f, -96f, 14f, -70f, 10f, Gfx.p(0xFFFFF2E6.toInt()))
        c.restore()
    }

    private fun drawToolCursor(c: Canvas) {
        val tool = step.tool
        val ex = px; val ey = py - 90f
        val wob = sin(time * 30f) * 4f
        when (tool) {
            Tool.HOSE, Tool.TIRE, Tool.FOAM -> {
                // hold the nozzle on whichever side keeps it on screen
                val side = if (ex > W * 0.62f) -1f else 1f
                val gx = ex + 150f * side; val gy = ey + 170f
                val col = if (tool == Tool.FOAM) 0xCCFFFFFF.toInt() else 0xAADFF7FF.toInt()
                path.reset(); path.moveTo(gx - 60f * side, gy - 70f); path.lineTo(ex - 40f * side, ey - 20f); path.lineTo(ex + 30f * side, ey + 40f); path.close()
                c.drawPath(path, Gfx.p(col))
                c.drawCircle(ex, ey, 34f + wob, Gfx.s(0xCCFFFFFF.toInt(), 5f))
                c.save(); c.translate(gx, gy); c.scale(side, 1f); c.rotate(-130f + wob * 0.3f)
                IconArt.draw(c, tool.icon, 0f, 0f, 200f)
                c.restore()
            }
            Tool.DRYER -> {
                val side = if (ex > W * 0.62f) -1f else 1f
                c.save(); c.translate(ex + 120f * side, ey + 120f); c.scale(side, 1f); c.rotate(-135f + wob)
                IconArt.draw(c, Icon.DRYER, 0f, 0f, 170f); c.restore()
                c.drawCircle(ex, ey, 60f, Gfx.s(0x88FFFFFF.toInt(), 5f))
            }
            else -> {
                c.save(); c.translate(ex, ey); c.rotate(sin(time * 18f) * (if (touching) 12f else 4f))
                IconArt.draw(c, tool.icon, 0f, 0f, 170f); c.restore()
            }
        }
    }

    companion object {
        val PRAISE = arrayOf("AWESOME!", "SUPER!", "WOW!", "SPARKLY!", "GREAT JOB!", "AMAZING!", "SO CLEAN!", "YAY!")
    }
}
