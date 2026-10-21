package com.feedthewrongthing.game.ui

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import com.feedthewrongthing.game.audio.Sfx
import com.feedthewrongthing.game.game.CreatureType
import com.feedthewrongthing.game.game.Game
import com.feedthewrongthing.game.game.Game.Modal
import com.feedthewrongthing.game.game.Game.Screen
import com.feedthewrongthing.game.game.Goal
import com.feedthewrongthing.game.game.Hat
import com.feedthewrongthing.game.game.Island
import com.feedthewrongthing.game.game.ItemType
import com.feedthewrongthing.game.game.Levels
import com.feedthewrongthing.game.game.PlayWorld
import com.feedthewrongthing.game.game.PopText
import com.feedthewrongthing.game.game.Surprises
import com.feedthewrongthing.game.ui.UiKit.CORAL
import com.feedthewrongthing.game.ui.UiKit.CREAM
import com.feedthewrongthing.game.ui.UiKit.GO
import com.feedthewrongthing.game.ui.UiKit.GRAPE
import com.feedthewrongthing.game.ui.UiKit.INK
import com.feedthewrongthing.game.ui.UiKit.MUTED
import com.feedthewrongthing.game.ui.UiKit.SKY
import com.feedthewrongthing.game.ui.UiKit.SUN
import com.feedthewrongthing.game.ui.UiKit.WHITE
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Draws the 2D interface over the 3D view and routes touches. Main thread only. */
class Ui(private val game: Game) {
    private class Btn(val rect: RectF, val action: () -> Unit, val sound: Boolean)

    private val btns = ArrayList<Btn>()
    private var pressedIndex = -1
    private val pressedRect = RectF()
    private var captured = false
    private var sceneTouch = false

    private var w = 1f
    private var h = 1f
    private var u = 1f
    private var t = 0f
    private val tmp = FloatArray(3)
    private val r = RectF()
    private val k = UiKit

    // ------------------------------------------------------------------ input

    fun onTouch(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedIndex = -1
                captured = false
                sceneTouch = false
                for (i in btns.indices.reversed()) {
                    if (grow(btns[i].rect).contains(x, y)) {
                        pressedIndex = i
                        pressedRect.set(btns[i].rect)
                        captured = true
                        break
                    }
                }
                if (!captured && game.modal == Modal.NONE) {
                    sceneTouch = true
                    game.sceneDown(x, y)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (captured) {
                    if (!grow(pressedRect).contains(x, y)) { captured = false; pressedIndex = -1 }
                } else if (sceneTouch) game.sceneMove(x, y)
            }
            MotionEvent.ACTION_UP -> {
                if (captured && pressedIndex in btns.indices && grow(pressedRect).contains(x, y)) {
                    val b = btns[pressedIndex]
                    if (b.sound) game.audio.play(Sfx.CLICK, 0.8f)
                    b.action()
                } else if (sceneTouch) game.sceneUp(x, y)
                captured = false
                pressedIndex = -1
                sceneTouch = false
            }
            MotionEvent.ACTION_CANCEL -> {
                captured = false; pressedIndex = -1
                if (sceneTouch) game.sceneCancel()
                sceneTouch = false
            }
        }
        return true
    }

    private val growRect = RectF()
    private fun grow(rr: RectF): RectF {
        val g = 6f * u
        growRect.set(rr.left - g, rr.top - g, rr.right + g, rr.bottom + g)
        return growRect
    }

    /** Registers a button for this frame. Returns whether it is currently held down. */
    private fun btn(rect: RectF, sound: Boolean = true, action: () -> Unit): Boolean {
        val idx = btns.size
        btns.add(Btn(RectF(rect), action, sound))
        return captured && idx == pressedIndex
    }

    private fun circleBtn(cx: Float, cy: Float, rad: Float, action: () -> Unit): Boolean =
        btn(RectF(cx - rad, cy - rad, cx + rad, cy + rad), true, action)

    // ------------------------------------------------------------------ frame

    fun draw(c: Canvas) {
        w = c.width.toFloat()
        h = c.height.toFloat()
        u = min(w, h) / 360f
        t = game.time
        btns.clear()
        when (game.screen) {
            Screen.SPLASH -> drawSplash(c)
            Screen.HOME -> drawHome(c)
            Screen.MAP -> drawMap(c)
            Screen.PLAY -> drawPlay(c)
            Screen.RESULTS -> drawResults(c)
            Screen.COLLECTION -> drawCollection(c)
        }
        drawPops(c, if (game.screen == Screen.PLAY) game.world?.popTexts else game.menuPops)
        if (game.modal != Modal.NONE) drawModal(c)
        if (game.fade > 0f) {
            k.fill.shader = null
            k.fill.color = k.withAlpha(0xFF3B2A6E.toInt(), game.fade)
            c.drawRect(0f, 0f, w, h, k.fill)
        }
    }

    private val left get() = game.safeLeft + 16f * u
    private val right get() = w - game.safeRight - 16f * u
    private val top get() = game.safeTop + 14f * u
    private val bottomSafe get() = h - game.safeBottom - 10f * u

    // ------------------------------------------------------------------ logo

    private fun pop(age: Float): Float {
        if (age <= 0f) return 0f
        if (age > 0.5f) return 1f
        val x = age / 0.5f
        return 1f + sin(x * Math.PI.toFloat()) * 0.35f * (1f - x) - (1f - x) * (1f - x) * 1f
    }

    private fun drawLogo(c: Canvas, cx: Float, cy: Float, s: Float, age: Float) {
        c.save()
        c.rotate(-4f, cx, cy)
        val a1 = pop(age)
        val a2 = pop(age - 0.25f)
        val a3 = pop(age - 0.5f)
        if (a1 > 0f) {
            c.save(); c.scale(a1, a1, cx, cy - 58f * s)
            k.title(c, "Feed the", cx, cy - 58f * s, 34f * s, WHITE, 4)
            c.restore()
        }
        if (a2 > 0f) {
            c.save(); c.scale(a2, a2, cx, cy + 8f * s)
            k.title(c, "WRONG", cx, cy + 20f * s, 76f * s, SUN, 7)
            c.restore()
        }
        if (a3 > 0f) {
            c.save(); c.scale(a3, a3, cx, cy + 80f * s)
            k.title(c, "THING!", cx + 18f * s, cy + 92f * s, 76f * s, CORAL, 7)
            c.restore()
        }
        c.restore()
    }

    private fun drawSplash(c: Canvas) {
        val st = game.screenT
        val landscape = w >= h
        val cx = w / 2f
        val cy = if (landscape) h * 0.2f else h * 0.18f
        drawLogo(c, cx, cy + 20f * u, if (landscape) 0.82f * u else 1f * u, st - 1.3f)
        if (st > 2.6f && (t * 2f).toInt() % 2 == 0) {
            k.label(c, "Tap to start!", cx, bottomSafe - 20f * u, 20f * u, WHITE, INK)
        }
    }

    // ------------------------------------------------------------------ home

    private fun drawHome(c: Canvas) {
        val landscape = w >= h
        val banner = game.bannerHeight().toFloat()
        val bottom = bottomSafe - banner
        val lx = if (landscape) w * 0.69f else w / 2f
        val ly = if (landscape) h * 0.27f else h * 0.16f
        drawLogo(c, lx, ly, if (landscape) 0.95f * u else 1.0f * u, game.screenT + 2f)

        // Stats chip
        chip(c, left, top, "${game.save.totalStars()}", Icon.STAR, SUN)
        chip(c, left, top + 46f * u, "${game.save.totalDiscoveries()}", Icon.BOOK, GRAPE)

        val playW = 230f * u
        val playH = 76f * u
        val py = if (landscape) h * 0.6f else bottom - 170f * u
        r.set(lx - playW / 2f, py - playH / 2f, lx + playW / 2f, py + playH / 2f)
        val pulse = 1f + 0.03f * sin(t * 4f)
        c.save(); c.scale(pulse, pulse, lx, py)
        val pressed = btn(r) { game.openMap() }
        k.button(c, r, SUN, pressed, "PLAY", Icon.PLAY, INK, 36f * u)
        c.restore()

        val rad = 30f * u
        val rowY = min(py + playH / 2f + 58f * u, bottom - rad - 18f * u)
        val items = ArrayList<Triple<Icon, Int, () -> Unit>>()
        items.add(Triple(Icon.BOOK, GRAPE) { game.openCollection() })
        items.add(Triple(Icon.GEAR, SKY) { game.openSettings() })
        if (game.dailyGiftReady) items.add(Triple(Icon.GIFT, CORAL) { game.openDailyGift() })
        items.add(Triple(if (game.save.musicOn) Icon.MUSIC else Icon.MUTE, GO) { game.toggleMusic() })
        val gap = 84f * u
        var x = lx - gap * (items.size - 1) / 2f
        for ((icon, col, act) in items) {
            val bounce = if (icon == Icon.GIFT) abs(sin(t * 4f)) * 8f * u else 0f
            val p = circleBtn(x, rowY - bounce, rad, act)
            k.roundButton(c, x, rowY - bounce, rad, col, icon, p)
            if (icon == Icon.GIFT) k.label(c, "Daily gift!", x, rowY + rad + 22f * u, 13f * u, WHITE, INK)
            x += gap
        }
    }

    private fun chip(c: Canvas, x: Float, y: Float, label: String, icon: Icon, col: Int) {
        k.text.textSize = 20f * u
        val tw = k.text.measureText(label)
        r.set(x, y, x + tw + 58f * u, y + 38f * u)
        k.panel(c, r, 0xEEFFFFFF.toInt(), 19f * u, 3f * u)
        k.drawIcon(c, icon, x + 20f * u, y + 19f * u, 12f * u, col)
        k.label(c, label, x + 38f * u, y + 26f * u, 20f * u, INK, 0, Paint.Align.LEFT)
    }

    // ------------------------------------------------------------------ map

    private fun drawMap(c: Canvas) {
        val banner = game.bannerHeight().toFloat()
        val bottom = bottomSafe - banner
        val back = circleBtn(left + 26f * u, top + 26f * u, 26f * u) { game.goHome() }
        k.roundButton(c, left + 26f * u, top + 26f * u, 26f * u, WHITE, Icon.LEFT, back, INK)
        k.title(c, "Snack Sky", w / 2f, top + 40f * u, 34f * u, WHITE, 5)
        chipRight(c, right, top + 6f * u, "${game.save.totalStars()}", Icon.STAR, SUN)

        val cam = game.camera
        val islands = Levels.islandCount + 1
        for (i in 0 until islands) {
            val x = i * Game.ISLAND_GAP
            if (!cam.project(x, -2.6f, 0f, tmp)) continue
            if (tmp[0] < -200f * u || tmp[0] > w + 200f * u) continue
            val island = if (i >= Levels.islandCount) Island.FEAST else Island.entries[i]
            val unlocked = game.save.isIslandUnlocked(i)
            val sel = i == game.selectedIsland
            val cy = min(tmp[1] + 20f * u, bottom - 70f * u)
            val name = if (unlocked) island.title else "???"
            k.text.textSize = 17f * u
            val tw = max(k.text.measureText(name), 90f * u)
            r.set(tmp[0] - tw / 2f - 18f * u, cy - 22f * u, tmp[0] + tw / 2f + 18f * u, cy + 22f * u)
            k.panel(c, r, if (sel) 0xF5FFFFFF.toInt() else 0xCCFFFFFF.toInt(), 22f * u, 3f * u)
            k.label(c, name, tmp[0], cy - 2f * u, 17f * u, INK)
            val sub = when {
                !unlocked -> "Locked"
                i >= Levels.islandCount -> if (game.save.endlessBest > 0) "Best: Feast #${game.save.endlessBest}" else "Endless fun!"
                else -> "★ ${game.save.islandStars(i)} / ${Levels.PER_ISLAND * 3}"
            }
            k.label(c, sub, tmp[0], cy + 15f * u, 13f * u, if (unlocked) MUTED else 0xFF8C86A6.toInt())
            if (!unlocked) k.drawIcon(c, Icon.LOCK, r.left + 4f * u, r.top + 4f * u, 11f * u, MUTED)
        }
        // Step + GO controls
        val sel = game.selectedIsland
        val goW = 170f * u
        val goH = 62f * u
        val gy = bottom - goH / 2f - 8f * u
        r.set(w / 2f - goW / 2f, gy - goH / 2f, w / 2f + goW / 2f, gy + goH / 2f)
        val unlocked = game.save.isIslandUnlocked(sel)
        val p = btn(r) { game.openIsland(sel) }
        k.button(c, r, if (unlocked) GO else 0xFFB9B0D3.toInt(), p, if (unlocked) "GO!" else "Locked", if (unlocked) Icon.PLAY else Icon.LOCK)
        val ar = 26f * u
        if (sel > 0) {
            val pl = circleBtn(w / 2f - goW / 2f - 46f * u, gy, ar) { stepMap(-1) }
            k.roundButton(c, w / 2f - goW / 2f - 46f * u, gy, ar, WHITE, Icon.LEFT, pl, INK)
        }
        if (sel < islands - 1) {
            val pr = circleBtn(w / 2f + goW / 2f + 46f * u, gy, ar) { stepMap(1) }
            k.roundButton(c, w / 2f + goW / 2f + 46f * u, gy, ar, WHITE, Icon.RIGHT, pr, INK)
        }
    }

    private fun stepMap(d: Int) = game.nudgeMap(d)

    private fun chipRight(c: Canvas, rx: Float, y: Float, label: String, icon: Icon, col: Int) {
        k.text.textSize = 20f * u
        val tw = k.text.measureText(label)
        chip(c, rx - (tw + 58f * u), y, label, icon, col)
    }

    // ------------------------------------------------------------------ play HUD

    private fun goalText(world: PlayWorld): String {
        val cs = world.creatures
        fun name(i: Int) = if (i in cs.indices) cs[i].type.title else if (cs.size == 1) cs[0].type.title else "someone"
        return when (val g = world.goal) {
            is Goal.Feed -> if (g.target >= 0 || cs.size == 1) "Feed ${name(g.target.coerceAtLeast(0))} ${g.count} snacks!" else "Feed ${g.count} snacks!"
            is Goal.FeedItem -> "Feed ${name(g.target)} the ${g.item.title}!"
            is Goal.Requests -> "Feed them what they're dreaming of!"
            is Goal.Wrong -> "Feed the WRONG thing!"
            is Goal.React -> "Make ${name(g.target)} ${g.type.verb}!"
            is Goal.Discover -> "Find ${g.count} different silly reactions!"
            is Goal.Combo -> "Get a ${g.count}-snack COMBO!"
            is Goal.Everyone -> if (g.count > 1) "Feed everyone ${g.count} times!" else "Feed everyone!"
            is Goal.Order -> "Feed in this order:"
            is Goal.Wake -> "Wake the sleepyheads and feed them!"
            is Goal.Beat -> "Feed on the beat!"
        }
    }

    private fun levelLabel(): String {
        val i = game.levelIndex
        return if (i >= Levels.all.size) "Feast #${i - Levels.all.size + 1}"
        else "${i / Levels.PER_ISLAND + 1}-${i % Levels.PER_ISLAND + 1}"
    }

    private fun drawPlay(c: Canvas) {
        val world = game.world ?: return
        // Pause
        val pr = 27f * u
        val pause = circleBtn(left + pr, top + pr, pr) { game.openModal(Modal.PAUSE) }
        k.roundButton(c, left + pr, top + pr, pr, WHITE, Icon.PAUSE, pause, INK)
        // Hint
        val hint = circleBtn(right - pr, top + pr, pr) { world.requestHint() }
        k.roundButton(c, right - pr, top + pr, pr, SUN, Icon.BULB, hint, WHITE)

        // Objective card
        val cardW = min(w - (left + pr * 2f + 14f * u) * 2f + left * 0f, 430f * u).coerceAtLeast(240f * u)
        val cardX = w / 2f - cardW / 2f
        val cardH = 64f * u
        r.set(cardX, top, cardX + cardW, top + cardH)
        k.panel(c, r, 0xF2FFF6E5.toInt(), 22f * u, 4f * u)
        val g = world.goal
        val iconSize = 52f * u
        val iconX = cardX + 12f * u + iconSize / 2f
        val iconY = top + cardH / 2f
        when (g) {
            is Goal.FeedItem -> k.bitmap(c, game.thumb(g.item), iconX, iconY, iconSize)
            is Goal.React -> {
                val tgt = if (g.target >= 0) world.creatures.getOrNull(g.target)?.type else world.creatures.firstOrNull()?.type
                k.bitmap(c, tgt?.let { game.thumb(it) }, iconX, iconY, iconSize)
            }
            else -> k.bitmap(c, world.creatures.firstOrNull()?.type?.let { game.thumb(it) }, iconX, iconY, iconSize)
        }
        val textX = iconX + iconSize / 2f + 8f * u
        val progW = progressWidth(world)
        val maxText = cardX + cardW - progW - 14f * u - textX
        k.label(c, levelLabel(), textX, top + 20f * u, 12f * u, MUTED, 0, Paint.Align.LEFT)
        fitLabel(c, goalText(world), textX, top + 44f * u, 18f * u, maxText)
        drawProgress(c, world, cardX + cardW - 12f * u, top + cardH / 2f)

        // Order strip
        if (g is Goal.Order) {
            val n = g.items.size
            val s = 40f * u
            val sx = w / 2f - (n - 1) * (s + 6f * u) / 2f
            val sy = top + cardH + 30f * u
            for ((i, item) in g.items.withIndex()) {
                val x = sx + i * (s + 6f * u)
                r.set(x - s / 2f, sy - s / 2f, x + s / 2f, sy + s / 2f)
                val doneItem = i < world.orderIndex
                val next = i == world.orderIndex
                k.panel(c, r, if (next) 0xFFFFF0B3.toInt() else 0xD9FFFFFF.toInt(), 12f * u, 2f * u)
                k.bitmap(c, game.thumb(item), x, sy, s * 0.95f, alpha = if (doneItem) 110 else 255)
                if (doneItem) k.drawIcon(c, Icon.CHECK, x, sy, 12f * u, GO)
            }
        }

        // Tip banner
        val def = world.def
        if (game.tipTimer > 0f && def.tip != null && game.modal == Modal.NONE) {
            val a = min(1f, game.tipTimer)
            val ty = top + cardH + (if (g is Goal.Order) 70f else 20f) * u
            k.text.textSize = 16f * u
            val tw = min(k.text.measureText(def.tip), w * 0.8f)
            r.set(w / 2f - tw / 2f - 16f * u, ty, w / 2f + tw / 2f + 16f * u, ty + 36f * u)
            k.panel(c, r, k.withAlpha(0xFF8E6CF0.toInt(), 0.92f * a), 18f * u, 3f * u)
            fitLabel(c, def.tip, w / 2f, ty + 24f * u, 16f * u, tw, k.withAlpha(WHITE, a), Paint.Align.CENTER)
        }

        // Level intro text
        if (game.screenT < 2.2f && game.modal == Modal.NONE) {
            val a = (1f - (game.screenT - 1.4f).coerceAtLeast(0f) / 0.8f).coerceIn(0f, 1f)
            val s = pop(game.screenT)
            c.save(); c.scale(s, s, w / 2f, h * 0.45f)
            k.title(c, def.name, w / 2f, h * 0.45f, 38f * u, k.withAlpha(WHITE, a), 5)
            c.restore()
        }

        // Combo
        if (world.comboShow > 0f && world.combo >= 2) {
            val age = 1.6f - world.comboShow
            val s = pop(age) * (1f + 0.05f * sin(t * 20f))
            c.save(); c.scale(s, s, w / 2f, h * 0.36f)
            k.title(c, "COMBO ×${world.combo}!", w / 2f, h * 0.36f, 40f * u, 0xFFFF8A3C.toInt(), 6)
            c.restore()
        }

        // Toasts
        var ty = top + pr * 2f + 14f * u
        for (toast in world.toasts) {
            val slide = min(1f, toast.age * 4f) * min(1f, (3.2f - toast.age) * 4f)
            val tw = 250f * u
            val x0 = right - tw * slide
            r.set(x0, ty, x0 + tw, ty + 58f * u)
            k.panel(c, r, 0xF28E6CF0.toInt(), 18f * u, 3f * u)
            toast.item?.let { k.bitmap(c, game.thumb(it), x0 + 32f * u, ty + 29f * u, 48f * u) }
            k.label(c, toast.title, x0 + 62f * u, ty + 22f * u, 12f * u, 0xFFFFE68A.toInt(), 0, Paint.Align.LEFT)
            fitLabel(c, toast.body, x0 + 62f * u, ty + 44f * u, 14f * u, tw - 72f * u, WHITE)
            ty += 66f * u
        }

        // Hint hand
        if (world.hintOn && world.heldSlot < 0 && world.hintSlot >= 0 && game.modal == Modal.NONE) {
            val s = world.slots[world.hintSlot]
            if (world.camera.project(s.x, 0.6f, s.z, tmp)) {
                val cyc = (t % 1.8f) / 1.8f
                val m = ((cyc - 0.15f) / 0.6f).coerceIn(0f, 1f)
                val e = m * m * (3f - 2f * m)
                val hx = tmp[0] + world.hintDx * e
                val hy = tmp[1] - world.hintDy * e
                val alpha = if (cyc > 0.9f) (1f - cyc) / 0.1f else 1f
                if (alpha > 0.05f) k.hand(c, hx, hy, 46f * u, cyc in 0.1f..0.8f)
            }
        }

        if (world.done) {
            val s = pop(world.doneElapsed - 1.2f)
            if (s > 0f) {
                c.save(); c.scale(s, s, w / 2f, h * 0.32f)
                k.title(c, "GREAT JOB!", w / 2f, h * 0.32f, 46f * u, SUN, 6)
                c.restore()
            }
        }
    }

    private fun progressWidth(world: PlayWorld): Float {
        val n = world.goalTotal
        return if (n <= 6) n * 22f * u else 70f * u
    }

    private fun drawProgress(c: Canvas, world: PlayWorld, rx: Float, cy: Float) {
        val n = world.goalTotal
        val p = world.progress.coerceAtMost(n)
        if (n <= 6) {
            for (i in 0 until n) {
                val x = rx - (n - 1 - i) * 22f * u - 9f * u
                k.fill.shader = null
                k.fill.color = if (i < p) GO else 0xFFE6E0F2.toInt()
                c.drawCircle(x, cy, 9f * u, k.fill)
                k.stroke.color = if (i < p) 0xFF1C7A50.toInt() else 0xFFB9B0D3.toInt()
                k.stroke.strokeWidth = 2.5f * u
                c.drawCircle(x, cy, 9f * u, k.stroke)
            }
        } else {
            k.label(c, "$p/$n", rx - 30f * u, cy + 8f * u, 22f * u, INK)
        }
    }

    private fun fitLabel(c: Canvas, s: String, x: Float, y: Float, size: Float, maxW: Float, color: Int = INK,
                         align: Paint.Align = Paint.Align.LEFT) {
        var sz = size
        k.text.textSize = sz
        while (k.text.measureText(s) > maxW && sz > size * 0.55f) { sz *= 0.93f; k.text.textSize = sz }
        k.label(c, s, x, y, sz, color, 0, align)
    }

    private fun drawPops(c: Canvas, list: List<PopText>?) {
        if (list == null) return
        val cam = if (game.screen == Screen.PLAY) game.world?.camera ?: return else game.camera
        for (p in list) {
            if (!cam.project(p.x, p.y + p.age * 0.8f, p.z, tmp)) continue
            val s = pop(p.age) * u
            val a = min(1f, (p.life - p.age) * 3f)
            c.save()
            c.rotate(-6f + 12f * ((p.text.length % 3) / 2f), tmp[0], tmp[1])
            k.label(c, p.text, tmp[0], tmp[1], 30f * s, k.withAlpha(0xFF000000.toInt() or p.color, a), k.withAlpha(WHITE, a))
            c.restore()
        }
    }

    // ------------------------------------------------------------------ results

    private fun drawResults(c: Canvas) {
        val landscape = w >= h
        val pw = if (landscape) min(400f * u, w * 0.48f) else w - 40f * u
        val ph = if (landscape) h - top - 30f * u - game.safeBottom else h * 0.52f
        val px = if (landscape) right - pw else w / 2f - pw / 2f
        val py = if (landscape) top + 18f * u else h - ph - game.safeBottom - 20f * u
        r.set(px, py, px + pw, py + ph)
        k.panel(c, r, 0xF7FFF6E5.toInt(), 30f * u, 6f * u)
        val cx = px + pw / 2f
        k.title(c, game.resultWord, cx, py + 52f * u, min(44f * u, pw / game.resultWord.length * 1.7f), CORAL, 6)
        k.label(c, if (game.isEndless) "Feast #${game.levelIndex - Levels.all.size + 1} complete" else "Level ${levelLabel()} complete",
            cx, py + 80f * u, 15f * u, MUTED)
        val shown = game.starsShown(game.screenT)
        for (i in 0 until 3) {
            val sx = cx + (i - 1) * 66f * u
            val sy = py + 130f * u - (if (i == 1) 10f * u else 0f)
            val appear = game.screenT - (0.6f + 0.4f * i)
            val sc = if (i < shown) pop(appear) else 1f
            c.save(); c.scale(sc, sc, sx, sy); c.rotate((i - 1) * 12f, sx, sy)
            k.star(c, sx, sy, (if (i == 1) 34f else 28f) * u, i < shown)
            c.restore()
        }
        var ly = py + 196f * u
        if (game.resultNewFinds > 0) { infoRow(c, px + 22f * u, ly, pw - 44f * u, "New reactions found", "+${game.resultNewFinds}", GRAPE); ly += 40f * u }
        if (game.resultBest) { infoRow(c, px + 22f * u, ly, pw - 44f * u, "New best!", "★", SUN); ly += 40f * u }
        if (game.resultNewIsland >= 0) {
            val name = if (game.resultNewIsland >= Levels.islandCount) Island.FEAST.title else Island.entries[game.resultNewIsland].title
            infoRow(c, px + 22f * u, ly, pw - 44f * u, "New island: $name!", "", GO); ly += 40f * u
        }
        val giftWaiting = game.giftEligible && !game.save.giftClaimed(game.levelIndex)
        if (giftWaiting) { infoRow(c, px + 22f * u, ly, pw - 44f * u, "A bonus gift is waiting!", "", CORAL); ly += 40f * u }

        val by = py + ph - 46f * u
        val rad = 28f * u
        val replay = circleBtn(px + 44f * u, by, rad) { game.restartLevel() }
        k.roundButton(c, px + 44f * u, by, rad, SKY, Icon.REPLAY, replay)
        val map = circleBtn(px + 108f * u, by, rad) { game.resultsToMap() }
        k.roundButton(c, px + 108f * u, by, rad, GRAPE, Icon.GRID, map)
        r.set(px + 150f * u, by - 30f * u, px + pw - 22f * u, by + 30f * u)
        val next = btn(r) { game.nextLevel() }
        k.button(c, r, GO, next, if (giftWaiting) "GIFT!" else "NEXT", if (giftWaiting) Icon.GIFT else Icon.PLAY)
    }

    private fun infoRow(c: Canvas, x: Float, y: Float, width: Float, label: String, value: String, col: Int) {
        r.set(x, y, x + width, y + 32f * u)
        k.panel(c, r, k.lighten(col, 0.78f), 12f * u, 0f)
        fitLabel(c, label, x + 14f * u, y + 22f * u, 15f * u, width - 60f * u)
        if (value.isNotEmpty()) k.label(c, value, x + width - 14f * u, y + 23f * u, 18f * u, k.darken(col, 0.7f), 0, Paint.Align.RIGHT)
    }

    // ------------------------------------------------------------------ collection

    private fun drawCollection(c: Canvas) {
        val landscape = w >= h
        val banner = game.bannerHeight().toFloat()
        val bottom = bottomSafe - banner
        val back = circleBtn(left + 26f * u, top + 26f * u, 26f * u) { game.goHome() }
        k.roundButton(c, left + 26f * u, top + 26f * u, 26f * u, WHITE, Icon.LEFT, back, INK)
        val ct = CreatureType.entries[game.colIndex]
        val met = game.save.creatureMet(ct)
        val nameX = left + 64f * u
        k.title(c, if (met) ct.title else "???", nameX, top + 36f * u, 30f * u, WHITE, 5, Paint.Align.LEFT)
        k.label(c, if (met) ct.blurb else "Meet it on a new island!", nameX, top + 60f * u, 14f * u, WHITE, INK, Paint.Align.LEFT)

        // Arrows near the creature
        val ax = if (landscape) w * 0.22f else w / 2f
        val ay = if (landscape) bottom - 36f * u else h * 0.42f
        val la = circleBtn(ax - 110f * u, ay, 26f * u) { game.collectionNext(-1) }
        k.roundButton(c, ax - 110f * u, ay, 26f * u, WHITE, Icon.LEFT, la, INK)
        val ra = circleBtn(ax + 110f * u, ay, 26f * u) { game.collectionNext(1) }
        k.roundButton(c, ax + 110f * u, ay, 26f * u, WHITE, Icon.RIGHT, ra, INK)

        // Journal panel
        val pw = if (landscape) min(470f * u, w * 0.55f) else w - 32f * u
        val px = if (landscape) right - pw else w / 2f - pw / 2f
        val py = if (landscape) top else h * 0.47f
        val pb = bottom
        r.set(px, py, px + pw, pb)
        k.panel(c, r, 0xF2FFFFFF.toInt(), 26f * u, 5f * u)
        val found = game.save.discoveredCount(ct)
        k.label(c, "Reaction Journal", px + 18f * u, py + 30f * u, 18f * u, INK, 0, Paint.Align.LEFT)
        k.label(c, "$found/${ItemType.entries.size}", px + pw - 18f * u, py + 30f * u, 18f * u, GRAPE, 0, Paint.Align.RIGHT)
        val cols = 5
        val hatsH = 70f * u
        val infoH = 30f * u
        val gridTop = py + 44f * u
        val gridH = pb - gridTop - hatsH - infoH - 12f * u
        val cell = min((pw - 36f * u) / cols, gridH / 5f)
        val gx0 = px + (pw - cell * cols) / 2f
        for ((i, item) in ItemType.entries.withIndex()) {
            val cx = gx0 + (i % cols) * cell + cell / 2f
            val cy = gridTop + (i / cols) * cell + cell / 2f
            val d = met && game.save.discovered(ct, item)
            r.set(cx - cell * 0.45f, cy - cell * 0.45f, cx + cell * 0.45f, cy + cell * 0.45f)
            val sel = game.colSelected == item
            val p = btn(r, false) { game.collectionTapItem(item) }
            k.panel(c, r, if (sel) 0xFFFFF0B3.toInt() else if (d) 0xFFF3EFFA.toInt() else 0xFFE6E0F2.toInt(), cell * 0.2f, 0f)
            k.bitmap(c, game.thumb(item), cx, cy + (if (p) 2f * u else 0f), cell * 0.82f, silhouette = !d)
        }
        val sel = game.colSelected
        val infoY = gridTop + 5 * cell + 22f * u
        val info = when {
            sel == null -> if (met) "Tap a snack to replay its reaction!" else "Keep playing to meet this friend!"
            game.save.discovered(ct, sel) -> "${sel.title} → ${Surprises.reaction(ct, sel).title}!"
            else -> "${sel.title} → ??? Feed it to find out!"
        }
        fitLabel(c, info, px + pw / 2f, infoY, 15f * u, pw - 30f * u, MUTED, Paint.Align.CENTER)

        // Hats row
        val hats = Hat.entries
        val hs = min(54f * u, (pw - 20f * u) / hats.size * 1.6f)
        val rowY = pb - hatsH / 2f - 6f * u
        val visible = ((pw - 20f * u) / (hs + 4f * u)).toInt().coerceAtLeast(4)
        val owned = hats.filter { game.save.ownsHat(it) }
        val list = (owned + hats.filter { !game.save.ownsHat(it) }).take(visible)
        val hx0 = px + pw / 2f - (list.size - 1) * (hs + 4f * u) / 2f
        val eq = game.save.hatOf(ct)
        for ((i, hat) in list.withIndex()) {
            val cx = hx0 + i * (hs + 4f * u)
            r.set(cx - hs / 2f, rowY - hs / 2f, cx + hs / 2f, rowY + hs / 2f)
            val own = game.save.ownsHat(hat)
            val p = btn(r) { if (own) game.equipHat(hat) }
            k.panel(c, r, if (hat == eq) SUN else if (own) 0xFFF3EFFA.toInt() else 0xFFD8D1EA.toInt(), hs * 0.25f, 0f)
            if (hat == Hat.NONE) k.drawIcon(c, Icon.CLOSE, cx, rowY, hs * 0.22f, MUTED)
            else if (own) k.bitmap(c, game.thumb(hat), cx, rowY + (if (p) 2f * u else 0f), hs * 0.95f)
            else k.drawIcon(c, Icon.LOCK, cx, rowY, hs * 0.22f, MUTED)
        }
    }

    // ------------------------------------------------------------------ modals

    private fun dim(c: Canvas) {
        k.fill.shader = null
        k.fill.color = k.withAlpha(0xFF1A1235.toInt(), min(0.62f, game.modalT * 3f))
        c.drawRect(0f, 0f, w, h, k.fill)
    }

    private fun modalPanel(c: Canvas, pw: Float, ph: Float, color: Int = 0xFFFFF6E5.toInt()): RectF {
        val s = pop(game.modalT * 1.4f).coerceAtLeast(0.01f)
        val px = w / 2f - pw / 2f
        val py = h / 2f - ph / 2f
        c.save()
        c.scale(s, s, w / 2f, h / 2f)
        val rect = RectF(px, py, px + pw, py + ph)
        k.panel(c, rect, color, 30f * u, 7f * u)
        return rect
    }

    private fun closeX(c: Canvas, p: RectF, action: () -> Unit) {
        val cx = p.right - 10f * u
        val cy = p.top + 10f * u
        val pr = circleBtn(cx, cy, 22f * u, action)
        k.roundButton(c, cx, cy, 22f * u, CORAL, Icon.CLOSE, pr)
    }

    private fun wideButton(c: Canvas, cx: Float, cy: Float, bw: Float, bh: Float, col: Int, label: String, icon: Icon?, textCol: Int = WHITE, action: () -> Unit) {
        r.set(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f)
        val p = btn(r, true, action)
        k.button(c, r, col, p, label, icon, textCol, bh * 0.38f)
    }

    private fun drawModal(c: Canvas) {
        dim(c)
        val bw = 220f * u
        val bh = 56f * u
        when (game.modal) {
            Modal.NONE -> {}
            Modal.PAUSE -> {
                val p = modalPanel(c, 320f * u, 330f * u)
                k.title(c, "Paused", p.centerX(), p.top + 52f * u, 36f * u, GRAPE, 5)
                wideButton(c, p.centerX(), p.top + 104f * u, bw, bh, GO, "Keep playing", Icon.PLAY) { game.closeModal() }
                wideButton(c, p.centerX(), p.top + 170f * u, bw, bh, SKY, "Start over", Icon.REPLAY) { game.closeModal(); game.restartLevel() }
                val y = p.top + 252f * u
                val rad = 27f * u
                val items = listOf(
                    Triple(Icon.GRID, GRAPE) { game.closeModal(); game.quitLevelToMap() },
                    Triple(Icon.HOME, CORAL) { game.closeModal(); game.quitLevelToHome() },
                    Triple(if (game.save.musicOn) Icon.MUSIC else Icon.MUTE, 0xFF4FD1BA.toInt()) { game.toggleMusic() },
                    Triple(if (game.save.sfxOn) Icon.SOUND else Icon.MUTE, SUN) { game.toggleSfx() },
                )
                var x = p.centerX() - 69f * u * 1.5f
                for ((icon, col, act) in items) {
                    val pr = circleBtn(x, y, rad, act)
                    k.roundButton(c, x, y, rad, col, icon, pr)
                    x += 69f * u
                }
                c.restore()
            }
            Modal.SETTINGS -> {
                val p = modalPanel(c, 340f * u, 330f * u, WHITE)
                k.title(c, "Settings", p.centerX(), p.top + 50f * u, 32f * u, SKY, 5)
                var y = p.top + 96f * u
                toggleRow(c, p, y, Icon.MUSIC, "Music", game.save.musicOn) { game.toggleMusic() }; y += 52f * u
                toggleRow(c, p, y, Icon.SOUND, "Silly sounds", game.save.sfxOn) { game.toggleSfx() }; y += 52f * u
                toggleRow(c, p, y, Icon.VIBRATE, "Buzzy shakes", game.save.hapticsOn) { game.toggleHaptics() }; y += 56f * u
                r.set(p.left + 18f * u, y - 4f * u, p.right - 18f * u, y + 46f * u)
                k.panel(c, r, 0xFFF3EFFA.toInt(), 16f * u, 0f)
                k.label(c, "FOR GROWN-UPS", p.left + 32f * u, y + 16f * u, 11f * u, MUTED, 0, Paint.Align.LEFT)
                k.label(c, "Progress is saved on this device only", p.left + 32f * u, y + 34f * u, 11f * u, MUTED, 0, Paint.Align.LEFT, UiKit.bold)
                r.set(p.right - 128f * u, y + 4f * u, p.right - 28f * u, y + 38f * u)
                val rp = btn(r) { game.resetProgress() }
                k.button(c, r, 0xFFB9B0D3.toInt(), rp, "Reset", null, INK, 14f * u)
                closeX(c, p) { game.closeModal() }
                c.restore()
            }
            Modal.EXIT -> {
                val p = modalPanel(c, 360f * u, 300f * u)
                k.bitmap(c, game.thumb(CreatureType.MUNCHY), p.centerX(), p.top + 64f * u, 100f * u)
                k.title(c, "Leaving so soon?", p.centerX(), p.top + 140f * u, 28f * u, CORAL, 5)
                k.label(c, "Munchy will miss you!", p.centerX(), p.top + 166f * u, 15f * u, MUTED)
                val y = p.bottom - 48f * u
                val third = (p.width() - 36f * u) / 3f
                wideButton(c, p.left + 18f * u + third * 0.5f, y, third - 8f * u, bh, SKY, "Cancel", null) { game.closeModal() }
                wideButton(c, p.left + 18f * u + third * 1.5f, y, third - 8f * u, bh, CORAL, "Exit", null) { game.exitApp() }
                wideButton(c, p.left + 18f * u + third * 2.5f, y, third - 8f * u, bh, SUN, "Rate us", Icon.STAR, INK) { game.rateUs() }
                c.restore()
            }
            Modal.GATE -> {
                val p = modalPanel(c, 360f * u, 290f * u, WHITE)
                k.title(c, "Grown-ups only", p.centerX(), p.top + 48f * u, 28f * u, GRAPE, 4)
                k.label(c, "Ask a grown-up to tap the answer:", p.centerX(), p.top + 80f * u, 15f * u, MUTED)
                k.title(c, game.gateQuestion, p.centerX(), p.top + 140f * u, 44f * u, INK, 3)
                val y = p.bottom - 52f * u
                val third = (p.width() - 36f * u) / 3f
                for ((i, v) in game.gateAnswers.withIndex()) {
                    wideButton(c, p.left + 18f * u + third * (i + 0.5f), y, third - 10f * u, bh, SKY, "$v", null) { game.answerGate(v) }
                }
                closeX(c, p) { game.cancelGate() }
                c.restore()
            }
            Modal.RESET -> {
                val p = modalPanel(c, 340f * u, 230f * u, WHITE)
                k.title(c, "Erase everything?", p.centerX(), p.top + 56f * u, 26f * u, CORAL, 4)
                k.label(c, "Stars, hats and the journal will be gone.", p.centerX(), p.top + 90f * u, 14f * u, MUTED)
                wideButton(c, p.centerX() - 78f * u, p.bottom - 50f * u, 140f * u, bh, GO, "Keep it", null) { game.closeModal() }
                wideButton(c, p.centerX() + 78f * u, p.bottom - 50f * u, 140f * u, bh, CORAL, "Erase", null) { game.confirmReset() }
                c.restore()
            }
            Modal.GIFT -> drawGift(c)
            Modal.REWARD_OFFER -> {
                val p = modalPanel(c, 360f * u, 320f * u)
                k.title(c, "Mystery Gift!", p.centerX(), p.top + 50f * u, 32f * u, CORAL, 5)
                giftBox(c, p.centerX(), p.top + 138f * u, 64f * u, t)
                k.label(c, "Watch a short video to open it.", p.centerX(), p.top + 218f * u, 15f * u, MUTED)
                wideButton(c, p.centerX() - 82f * u, p.bottom - 46f * u, 150f * u, bh, GO, "Watch", Icon.VIDEO) { game.watchRewardedForGift() }
                wideButton(c, p.centerX() + 82f * u, p.bottom - 46f * u, 140f * u, bh, 0xFFB9B0D3.toInt(), "No thanks", null, INK) { game.skipRewardOffer() }
                c.restore()
            }
            Modal.ISLAND -> drawIslandPicker(c)
            Modal.INTRO -> {
                val world = game.world
                val island = world?.def?.island ?: Island.PICNIC
                val p = modalPanel(c, min(420f * u, w - 40f * u), 330f * u)
                k.title(c, island.title, p.centerX(), p.top + 50f * u, 30f * u, GRAPE, 5)
                k.bitmap(c, game.thumb(island.creature), p.centerX(), p.top + 128f * u, 120f * u)
                k.label(c, "New friend: ${island.creature.title}!", p.centerX(), p.top + 206f * u, 20f * u, CORAL)
                fitLabel(c, island.twist, p.centerX(), p.top + 236f * u, 15f * u, p.width() - 40f * u, MUTED, Paint.Align.CENTER)
                wideButton(c, p.centerX(), p.bottom - 42f * u, 200f * u, bh, GO, "Let's go!", Icon.PLAY) { game.closeModal() }
                c.restore()
            }
        }
    }

    private fun toggleRow(c: Canvas, p: RectF, y: Float, icon: Icon, label: String, on: Boolean, action: () -> Unit) {
        k.drawIcon(c, icon, p.left + 40f * u, y + 18f * u, 14f * u, INK)
        k.label(c, label, p.left + 66f * u, y + 26f * u, 19f * u, INK, 0, Paint.Align.LEFT)
        r.set(p.right - 96f * u, y, p.right - 28f * u, y + 36f * u)
        btn(r, true, action)
        k.toggle(c, r, on)
    }

    private fun giftBox(c: Canvas, cx: Float, cy: Float, s: Float, time: Float, open: Float = 0f) {
        val bounce = abs(sin(time * 3f)) * 6f * u * (1f - open)
        val y = cy - bounce
        k.fill.shader = LinearGradient(0f, y - s, 0f, y + s, 0xFFFF8F8F.toInt(), 0xFFE0405A.toInt(), Shader.TileMode.CLAMP)
        r.set(cx - s, y - s * 0.45f, cx + s, y + s)
        c.drawRoundRect(r, s * 0.14f, s * 0.14f, k.fill)
        k.fill.shader = null
        k.fill.color = SUN
        r.set(cx - s * 0.16f, y - s * 0.45f, cx + s * 0.16f, y + s); c.drawRect(r, k.fill)
        val lid = y - s * 0.45f - open * s * 0.9f
        c.save(); c.rotate(-open * 25f, cx - s, lid)
        k.fill.shader = LinearGradient(0f, lid - s * 0.3f, 0f, lid, 0xFFFFA3A3.toInt(), 0xFFF0506A.toInt(), Shader.TileMode.CLAMP)
        r.set(cx - s * 1.1f, lid - s * 0.32f, cx + s * 1.1f, lid + s * 0.05f)
        c.drawRoundRect(r, s * 0.1f, s * 0.1f, k.fill)
        k.fill.shader = null
        k.fill.color = SUN
        r.set(cx - s * 0.16f, lid - s * 0.32f, cx + s * 0.16f, lid + s * 0.05f); c.drawRect(r, k.fill)
        k.stroke.color = SUN; k.stroke.strokeWidth = s * 0.14f
        r.set(cx - s * 0.62f, lid - s * 0.78f, cx, lid - s * 0.3f); c.drawOval(r, k.stroke)
        r.set(cx, lid - s * 0.78f, cx + s * 0.62f, lid - s * 0.3f); c.drawOval(r, k.stroke)
        c.restore()
    }

    private fun drawGift(c: Canvas) {
        val p = modalPanel(c, 360f * u, 340f * u)
        if (!game.giftOpened) {
            k.title(c, if (game.giftFromDaily) "Daily Gift!" else "Mystery Gift!", p.centerX(), p.top + 50f * u, 32f * u, CORAL, 5)
            giftBox(c, p.centerX(), p.top + 150f * u, 70f * u, t)
            r.set(p.centerX() - 90f * u, p.top + 80f * u, p.centerX() + 90f * u, p.top + 240f * u)
            btn(r, false) { game.openGiftBox() }
            k.label(c, "Tap the box to open it!", p.centerX(), p.bottom - 40f * u, 17f * u, MUTED)
        } else {
            val hat = game.giftHat
            if (hat != null) {
                k.title(c, "You got...", p.centerX(), p.top + 44f * u, 24f * u, GRAPE, 4)
                val s = pop(game.modalT * 1.2f)
                c.save(); c.scale(s, s, p.centerX(), p.top + 130f * u)
                k.bitmap(c, game.thumb(hat), p.centerX(), p.top + 130f * u, 130f * u)
                c.restore()
                k.title(c, hat.title + "!", p.centerX(), p.top + 222f * u, 28f * u, CORAL, 5)
                wideButton(c, p.centerX() - 80f * u, p.bottom - 44f * u, 150f * u, 54f * u, GO, "Try it on!", Icon.HANGER) { game.tryOnGift() }
                wideButton(c, p.centerX() + 80f * u, p.bottom - 44f * u, 130f * u, 54f * u, SKY, "Yay!", null) { game.closeModal() }
            } else {
                k.title(c, "Wow!", p.centerX(), p.top + 60f * u, 34f * u, CORAL, 5)
                fitLabel(c, "You already have every single hat!", p.centerX(), p.top + 150f * u, 18f * u, p.width() - 40f * u, INK, Paint.Align.CENTER)
                wideButton(c, p.centerX(), p.bottom - 44f * u, 160f * u, 54f * u, GO, "Hooray!", null) { game.closeModal() }
            }
        }
        c.restore()
    }

    private fun drawIslandPicker(c: Canvas) {
        val i = game.selectedIsland
        val island = Island.entries[i]
        val p = modalPanel(c, min(500f * u, w - 30f * u), min(340f * u, h - 30f * u))
        k.bitmap(c, game.thumb(island.creature), p.left + 54f * u, p.top + 50f * u, 78f * u)
        k.title(c, island.title, p.left + 100f * u, p.top + 52f * u, 26f * u, GRAPE, 4, Paint.Align.LEFT)
        k.label(c, "★ ${game.save.islandStars(i)} / ${Levels.PER_ISLAND * 3}", p.left + 102f * u, p.top + 76f * u, 15f * u, MUTED, 0, Paint.Align.LEFT)
        val cols = 4
        val gw = p.width() - 40f * u
        val cell = gw / cols
        val gy0 = p.top + 104f * u
        val rowH = (p.bottom - 20f * u - gy0) / 2f
        for (k2 in 0 until Levels.PER_ISLAND) {
            val idx = i * Levels.PER_ISLAND + k2
            val cx = p.left + 20f * u + (k2 % cols) * cell + cell / 2f
            val cy = gy0 + (k2 / cols) * rowH + rowH * 0.42f
            val unlocked = game.save.isUnlocked(idx)
            val stars = game.save.stars(idx)
            val current = unlocked && stars == 0
            val rad = min(cell, rowH) * 0.34f
            val pulse = if (current) 1f + 0.06f * sin(t * 6f) else 1f
            val pr = circleBtn(cx, cy, rad) { if (unlocked) { game.closeModal(); game.startLevel(idx) } else game.audio.play(Sfx.BONK, 0.5f) }
            c.save(); c.scale(pulse, pulse, cx, cy)
            if (unlocked) {
                k.roundButton(c, cx, cy, rad, if (current) CORAL else SUN, Icon.STAR, pr)
                k.fill.shader = null
                k.label(c, "${k2 + 1}", cx, cy + rad * 0.32f, rad * 0.95f, if (current) WHITE else INK, if (current) k.darken(CORAL, 0.5f) else 0)
            } else {
                k.roundButton(c, cx, cy, rad, 0xFFD8D1EA.toInt(), Icon.LOCK, pr, 0xFF8A80A8.toInt())
            }
            c.restore()
            for (s in 0 until 3) k.star(c, cx + (s - 1) * rad * 0.62f, cy + rad + 12f * u, rad * 0.26f, s < stars)
        }
        closeX(c, p) { game.closeModal() }
        c.restore()
    }

    @Suppress("unused")
    private fun creamPanel() = CREAM
}
