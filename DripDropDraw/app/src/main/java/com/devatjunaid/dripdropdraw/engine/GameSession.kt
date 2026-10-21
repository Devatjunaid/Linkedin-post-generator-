package com.devatjunaid.dripdropdraw.engine

import kotlin.math.sqrt
import kotlin.random.Random

/** A little speech bubble ("Wheee!") shown next to a cup or an object. */
class Bubble(val text: String, val x: Float, val y: Float) {
    var age = 0f
}

/**
 * One play of one level: drawing, ink, the water flow and the win / try-again decision.
 * Pure Kotlin so it can be tested on the JVM.
 */
class GameSession(val level: LevelDef) {

    enum class State { READY, RUNNING, WON, FAILED }

    val world = World(level)
    var state = State.READY
        private set
    var inkUsed = 0f
        private set
    val inkLeftFraction get() = (1f - inkUsed / level.inkLimit).coerceIn(0f, 1f)
    var stars = 0
        private set

    /** Lines for drawing on screen. The last one may still be growing. */
    val strokes = ArrayList<ArrayList<Pt>>()
    private var drawing = false
    private var lastX = 0f
    private var lastY = 0f
    private var activeStroke: ArrayList<Pt>? = null
    var isTouching = false
        private set
    var touchX = 0f
        private set
    var touchY = 0f
        private set

    private var startCountdown = -1f
    private var winTimer = 0f
    private var endTimer = 0f
    var stateTime = 0f
        private set

    val bubbles = ArrayList<Bubble>()

    /** Gets every physics event (for sounds). */
    var listener: ((GameEvent) -> Unit)? = null
    private val rnd = Random(level.number)
    private val cupHasWater = BooleanArray(level.cups.size)
    private var sizzleCooldown = 0f

    /** Seconds since water last splashed into each cup (drives the "O" mouth). */
    val lastSplashAge = FloatArray(level.cups.size) { 99f }

    val canDraw get() = (state == State.READY || state == State.RUNNING) && inkUsed < level.inkLimit

    // ---- input ----------------------------------------------------------------------------

    fun touchDown(x: Float, y: Float) {
        isTouching = true
        touchX = x; touchY = y
        if (!canDraw) return
        drawing = true
        activeStroke = null
        lastX = x; lastY = y
        if (world.canDrawAt(x, y)) startStroke(x, y)
    }

    fun touchMove(x: Float, y: Float) {
        touchX = x; touchY = y
        if (!drawing) return
        val dx = x - lastX
        val dy = y - lastY
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 2f) return
        // Walk towards the finger in small steps so lines never jump through things.
        val steps = (dist / STEP_LEN).toInt().coerceAtLeast(1)
        for (s in 1..steps) {
            val t = s.toFloat() / steps
            val sx = lastX + dx * t
            val sy = lastY + dy * t
            if (!addPoint(sx, sy)) {
                drawing = false
                break
            }
        }
        lastX = x; lastY = y
    }

    fun touchUp() {
        isTouching = false
        drawing = false
        val stroke = activeStroke
        activeStroke = null
        if (stroke != null && stroke.size < 2) strokes.remove(stroke)
        if (state == State.READY && inkUsed > 0f && startCountdown < 0f) startCountdown = 0.35f
    }

    private fun startStroke(x: Float, y: Float) {
        val s = arrayListOf(Pt(x, y))
        strokes.add(s)
        activeStroke = s
    }

    /** Returns false when the ink has run out. */
    private fun addPoint(x: Float, y: Float): Boolean {
        if (!world.canDrawAt(x, y)) {
            // Blocked: break the line here and carry on when the finger is in a free spot again.
            val s = activeStroke
            if (s != null && s.size < 2) strokes.remove(s)
            activeStroke = null
            return true
        }
        val s = activeStroke
        if (s == null) {
            startStroke(x, y)
            return true
        }
        val last = s.last()
        val dx = x - last.x
        val dy = y - last.y
        val d = sqrt(dx * dx + dy * dy)
        if (d < MIN_SEGMENT) return true
        if (inkUsed + d > level.inkLimit) return false
        world.addLine(last.x, last.y, x, y)
        s.add(Pt(x, y))
        inkUsed += d
        return true
    }

    /** Used by tests and the demo on the splash screen: draws a whole line at once. */
    fun drawStroke(points: List<Pt>) {
        if (points.isEmpty()) return
        touchDown(points[0].x, points[0].y)
        for (i in 1 until points.size) touchMove(points[i].x, points[i].y)
        touchUp()
    }

    /** Starts the water without drawing (the Play button). */
    fun startWater() {
        if (state == State.READY) startCountdown = 0f
    }

    // ---- update ---------------------------------------------------------------------------

    /** Advances one fixed step of [World.STEP] seconds. */
    fun step() {
        val dt = World.STEP
        stateTime += dt
        for (i in lastSplashAge.indices) lastSplashAge[i] += dt
        for (b in bubbles) b.age += dt
        bubbles.removeAll { it.age > 1.6f }
        sizzleCooldown -= dt

        if (startCountdown >= 0f && state == State.READY) {
            startCountdown -= dt
            if (startCountdown <= 0f) {
                state = State.RUNNING
                stateTime = 0f
                world.flowing = true
            }
        }
        if (state == State.READY) return

        world.events.clear()
        world.step()
        handleEvents()

        if (state != State.RUNNING) return

        val allFull = world.cups.all { it.fill >= it.def.target }
        if (allFull) {
            winTimer += dt
            if (winTimer >= WIN_HOLD) win()
            return
        } else {
            winTimer = (winTimer - dt * 2f).coerceAtLeast(0f)
        }

        // Not enough water left anywhere to fill the cups: it is over.
        val possible = world.count + world.dropsLeftInFaucets
        val need = level.cups.sumOf { it.target }
        val emptied = world.dropsLeftInFaucets == 0
        if (emptied || possible < need) {
            endTimer += dt
            val wait = if (possible < need) 2.0f else 4.5f
            if (endTimer > wait) fail()
        }
    }

    private fun handleEvents() {
        for (e in world.events) {
            listener?.invoke(e)
            when (e.type) {
                EventType.SPLASH_IN_CUP -> {
                    lastSplashAge[e.cup] = 0f
                    if (!cupHasWater[e.cup]) {
                        cupHasWater[e.cup] = true
                        val c = world.cups[e.cup]
                        say(FIRST_DROP.random(rnd), c.x, c.def.top - 70f)
                    }
                }
                EventType.SIZZLE -> {
                    if (sizzleCooldown <= 0f) {
                        sizzleCooldown = 2.5f
                        say(SIZZLE.random(rnd), e.x, e.y - 90f)
                    }
                }
                EventType.BALL_BONK -> Unit
            }
        }
    }

    private fun say(text: String, x: Float, y: Float) {
        if (bubbles.size < 3) bubbles.add(Bubble(text, x, y))
    }

    private fun win() {
        state = State.WON
        stateTime = 0f
        stars = when {
            inkUsed <= level.threeStarInk -> 3
            inkUsed <= level.twoStarInk -> 2
            else -> 1
        }
        for (c in world.cups) say(WIN.random(rnd), c.x, c.def.top - 70f)
    }

    private fun fail() {
        state = State.FAILED
        stateTime = 0f
        for (c in world.cups) if (c.fill < c.def.target) say(FAIL.random(rnd), c.x, c.def.top - 70f)
    }

    companion object {
        const val STEP_LEN = 8f
        const val MIN_SEGMENT = 12f
        const val WIN_HOLD = 1.2f

        val FIRST_DROP = listOf("Yay!", "Wheee!", "Splash!", "Yummy!", "More!", "Ooh!")
        val WIN = listOf("Hee hee!", "So full!", "Thank you!", "Ahhh~", "Burp!", "Yippee!")
        val FAIL = listOf("Aww...", "Still thirsty...", "Try again?", "So close!")
        val SIZZLE = listOf("Hot hot!", "Tsss!", "Ouch!")
    }
}
