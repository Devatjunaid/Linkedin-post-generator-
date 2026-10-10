package com.dotdash.kids.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotdash.kids.game.FinishResult
import com.dotdash.kids.game.LEVELS_PER_WORLD
import com.dotdash.kids.game.Level
import com.dotdash.kids.game.Levels
import com.dotdash.kids.game.Pictures
import com.dotdash.kids.game.Pt
import com.dotdash.kids.game.EGG_STARS
import com.dotdash.kids.game.GOLDEN_ZIP_STARS
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val CHEERS = listOf("AMAZING!", "SUPER!", "WOW!", "GREAT JOB!", "AWESOME!", "YOU DID IT!", "BRILLIANT!")

fun article(name: String) = if (name.first().uppercaseChar() in "AEIOU") "an" else "a"

private fun starsFor(wrong: Int, hint: Boolean) = max(1, 3 - (if (wrong > 2) 1 else 0) - (if (hint) 1 else 0))

/** Dots are as big as the picture allows without overlapping. */
private fun dotRadius(dots: List<Pt>): Float {
    var m = Float.MAX_VALUE
    for (i in dots.indices) for (j in i + 1 until dots.size) m = min(m, hypot(dots[i].x - dots[j].x, dots[i].y - dots[j].y))
    return (m * 0.48f).coerceIn(3f, 5.2f)
}

/** The part of the 100-unit box the dots use, so the board zooms in on the picture. */
private class View(val x0: Float, val y0: Float, val w: Float, val h: Float)

private fun viewFor(dots: List<Pt>, r: Float): View {
    val pad = r + 7
    var x0 = dots.minOf { it.x } - pad
    var y0 = dots.minOf { it.y } - pad
    var w = dots.maxOf { it.x } + pad - x0
    var h = dots.maxOf { it.y } + pad - y0
    if (w < 70) { x0 -= (70 - w) / 2; w = 70f }
    if (h < 70) { y0 -= (70 - h) / 2; h = 70f }
    return View(x0, y0, w, h)
}

private class Seg(val a: Pt, val b: Pt, val t0: Long, val color: Color)
private class Spark(val at: Pt, val dx: Float, val dy: Float, val t0: Long, val color: Color)

/** Mutable board state; the canvas redraws every frame, so plain fields are fine. */
private class Board(val level: Level) {
    val dots = level.dots
    val n = dots.size
    val r = dotRadius(dots)
    val view = viewFor(dots, r)
    val litAt = LongArray(n)
    val shakeAt = LongArray(n)
    val segs = ArrayList<Seg>()
    val sparks = ArrayList<Spark>()
    var hintAt = 0L
    var zipFrom: Pt? = null
    var zipTo: Pt? = null
    var zipAt = 0L
    var revealedAt = 0L
    var aliveAt = 0L
    var last = 0L
    var size = IntSize.Zero

    // Board units → pixels.
    fun scale() = min(size.width / view.w, size.height / view.h)
    fun origin(): Offset {
        val s = scale()
        return Offset((size.width - view.w * s) / 2 - view.x0 * s, (size.height - view.h * s) / 2 - view.y0 * s)
    }
    fun toScreen(p: Pt): Offset {
        val o = origin(); val s = scale()
        return Offset(o.x + p.x * s, o.y + p.y * s)
    }
    fun toBoard(o: Offset): Pt {
        val org = origin(); val s = scale()
        return Pt((o.x - org.x) / s, (o.y - org.y) / s)
    }
}

private fun ease(t: Float) = 1 - (1 - t) * (1 - t)
private fun lerp(a: Pt, b: Pt, t: Float) = Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
private fun backOut(t: Float): Float {
    val c = 1.7f
    val x = t - 1
    return 1 + (c + 1) * x * x * x + c * x * x
}

@Composable
fun PlayScreen(state: AppState, index: Int) {
    val game = state.game
    val p = game.progress
    val level = Levels.all[index]
    val world = Pictures.worlds[level.world]
    val board = remember { Board(level) }
    val scope = rememberCoroutineScope()
    val tm = rememberTextMeasurer(cacheSize = 64)
    val dens = LocalDensity.current.density

    var now by remember { mutableLongStateOf(0L) }
    var next by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf(false) }
    var streak by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var line by remember { mutableStateOf("Tap number **1** to start!") }
    var cheer by remember { mutableStateOf<String?>(null) }
    var confetti by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<Pair<Int, ResultExtras>?>(null) }
    val gold = p.goldenZip()

    LaunchedEffect(Unit) {
        game.audio.music(level.world)
        while (true) withFrameMillis { now = it }
    }

    fun trail(i: Int) = if (gold || streak >= 10) Color.hsv((i * 37f) % 360f, 0.62f, 1f) else C.SunLight

    fun finish() {
        done = true
        val starsBefore = p.totalStars()
        val worldsBefore = Pictures.worlds.indices.map { p.worldUnlocked(it) }
        val stars = starsFor(wrong, hint)
        scope.launch {
            delay(220)
            board.segs.add(Seg(board.dots.last(), board.dots.first(), now, trail(board.n)))
            board.zipFrom = board.zipTo; board.zipTo = board.dots.first(); board.zipAt = now
            game.audio.whoosh()
            delay(550)
            board.revealedAt = now
            game.audio.sparkle()
            confetti++
            delay(650)
            board.aliveAt = now
            game.audio.voice(level.picture.voice)
            cheer = CHEERS.random()
            delay(900)
            game.say("It's ${article(level.picture.name)} ${level.picture.name}!")
            val res = p.finishLevel(level.index, stars)
            val goldNow = starsBefore < GOLDEN_ZIP_STARS && p.totalStars() >= GOLDEN_ZIP_STARS
            val newWorld = Pictures.worlds.indices.firstOrNull { !worldsBefore[it] && p.worldUnlocked(it) }
            result = stars to ResultExtras(res, goldNow, newWorld)
            if (res.egg || newWorld != null || goldNow) {
                delay(1200)
                game.audio.unlock()
            }
        }
    }

    fun connect() {
        val i = next
        board.litAt[i] = now
        if (i > 0) board.segs.add(Seg(board.dots[i - 1], board.dots[i], now, trail(i)))
        game.audio.dot(i)
        repeat(6) { k ->
            val a = 2 * PI * k / 6 + (k % 2) * 0.4
            board.sparks.add(Spark(board.dots[i], kotlin.math.cos(a).toFloat() * 8, sin(a).toFloat() * 8, now, listOf(C.SunLight, Color.White, C.Mint)[k % 3]))
        }
        board.zipFrom = board.zipTo ?: board.dots[i]
        board.zipTo = board.dots[i]
        board.zipAt = now
        streak++
        next++
        board.last = now
        if (next == board.n) finish()
        else line = if (i == 0) "Now find **${next + 1}**!" else "Find number **${next + 1}**!"
    }

    fun wrongTap(j: Int) {
        board.shakeAt[j] = now
        game.audio.wrong()
        wrong++
        streak = 0
        board.last = now
        line = "Oops! That's **${j + 1}**. Find **${next + 1}**!"
    }

    fun handle(pos: Offset, isTap: Boolean) {
        if (done || board.size == IntSize.Zero) return
        val pt = board.toBoard(pos)
        val hitR = max(6f, 24f * dens / board.scale()) // at least ~24dp around each dot
        val target = board.dots[next]
        if (hypot(pt.x - target.x, pt.y - target.y) <= hitR) {
            connect()
            return
        }
        if (!isTap) return
        var nearest = -1
        var nd = Float.MAX_VALUE
        board.dots.forEachIndexed { i, d ->
            val dd = hypot(pt.x - d.x, pt.y - d.y)
            if (dd < nd) { nd = dd; nearest = i }
        }
        // Tapping a dot that's already joined is fine; only a wrong new dot counts.
        if (nd <= hitR * 0.85f && nearest > next) wrongTap(nearest)
    }

    // Gentle nudge if the child gets stuck (not counted as a hint).
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (board.last == 0L) board.last = now // start counting from the first frame
            if (!done && now - board.last > 9000) {
                board.last = now
                board.hintAt = now
                line = "Psst… number **${next + 1}** is glowing!"
                game.say("${next + 1}")
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Sky(18, seed = 21)
        Column(Modifier.fillMaxSize()) {
            // header
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundButton({ game.audio.tap(); state.go(Screen.WorldMap(level.world)) }, "Back to map") { Icon24(Ic.Map, 24.dp, C.White) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText("Level ${level.number}", style = display(24.sp))
                    BasicText(world.name, style = body(13.sp, C.Muted))
                }
                val potential = starsFor(wrong, hint)
                Pill(Modifier.semantics { contentDescription = "$potential stars" }) { repeat(3) { StarIcon(22.dp, it < potential) } }
            }
            // progress
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(50)).background(C.Night2)) {
                    Box(Modifier.fillMaxWidth(next / board.n.toFloat()).height(16.dp).clip(RoundedCornerShape(50)).background(C.Sun))
                }
                BasicText("$next/${board.n}", Modifier.widthIn(min = 56.dp).padding(start = 12.dp), style = display(17.sp).copy(textAlign = TextAlign.End))
            }
            // board
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.fillMaxSize().widthIn(max = 620.dp).clip(RoundedCornerShape(30.dp)).background(C.Night2)
                        .onSizeChanged { board.size = it },
                ) {
                    PictureLayer(board, now, hex(p.colorOf(level.picture)))
                    Canvas(
                        Modifier.fillMaxSize().semantics { contentDescription = "Connect the dots puzzle" }.pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                handle(down.position, true)
                                do {
                                    val ev = awaitPointerEvent()
                                    ev.changes.forEach {
                                        if (it.pressed) handle(it.position, false)
                                        it.consume()
                                    }
                                } while (ev.changes.any { it.pressed })
                            }
                        },
                    ) { drawBoard(board, now, next, done, gold, tm) }
                    if (streak >= 5 && !done) {
                        BasicText(
                            "Streak ×$streak",
                            Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(50)).background(C.Coral).padding(horizontal = 12.dp, vertical = 6.dp),
                            style = display(16.sp, C.White),
                        )
                    }
                    cheer?.let { CheerText(it, Modifier.align(Alignment.TopCenter).padding(top = 16.dp)) }
                }
            }
            // bottom: Zip + buttons, or the result card
            val res = result
            if (res == null) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ZipView(58.dp, Modifier.bob(), gold = gold)
                        Bubble(zipLine(line), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ChunkyButton(
                            {
                                if (!done) {
                                    hint = true
                                    board.hintAt = now
                                    board.last = now
                                    game.audio.sparkle()
                                    line = "It's right here: **${next + 1}**!"
                                    game.say("${next + 1}")
                                }
                            },
                            C.Mint, C.MintDark, Modifier.weight(1f), height = 62.dp, label = "Hint",
                        ) {
                            Icon24(Ic.Hint, 26.dp, C.Ink)
                            BasicText("Hint", style = display(22.sp, C.Ink))
                        }
                        ChunkyButton(
                            { if (!done) { game.say("${next + 1}"); game.audio.tap() } },
                            C.Sky, C.SkyDark, Modifier.weight(1f), height = 62.dp, label = "Say the number",
                        ) {
                            Icon24(Ic.Speak, 26.dp, C.Ink)
                            BasicText("Say it", style = display(22.sp, C.Ink))
                        }
                    }
                }
            }
            AnimatedVisibility(res != null, enter = slideInVertically { it / 3 } + fadeIn()) {
                if (res != null) ResultCard(state, level, res.first, res.second)
            }
        }
        Confetti(confetti)
    }
}

/** The revealed picture: pops in, then wiggles as it comes alive. */
@Composable
private fun PictureLayer(board: Board, now: Long, fill: Color) {
    if (board.revealedAt == 0L || board.size == IntSize.Zero) return
    val tm = rememberTextMeasurer()
    val t = ((now - board.revealedAt) / 700f).coerceIn(0f, 1f)
    val alive = if (board.aliveAt > 0) (now - board.aliveAt) / 1000f else -1f
    val wiggle = if (alive in 0f..2.2f) sin(alive * 2 * PI.toFloat() * 0.9f) * 5f * (1 - alive / 2.2f) else 0f
    val jump = if (alive in 0f..2.2f) abs(sin(alive * PI.toFloat() * 1.8f)) * 4f * (1 - alive / 2.2f) else 0f
    val center = board.toScreen(Pt(50f, 85f))
    Canvas(
        Modifier.fillMaxSize().graphicsLayer {
            val sc = 0.7f + 0.3f * backOut(t)
            scaleX = sc; scaleY = sc
            alpha = t
            rotationZ = wiggle
            translationY = -jump * board.scale()
            transformOrigin = TransformOrigin(center.x / size.width, center.y / size.height)
        },
    ) {
        val o = board.origin()
        val s = board.scale()
        withTransform({
            translate(o.x, o.y)
            scale(s, s, Offset.Zero)
        }) { drawPicture(board.level.picture, fill, tm) }
    }
}

private fun DrawScope.drawBoard(b: Board, now: Long, next: Int, done: Boolean, gold: Boolean, tm: androidx.compose.ui.text.TextMeasurer) {
    if (b.size == IntSize.Zero) return
    val s = b.scale()
    val fade = if (b.revealedAt > 0) 1 - ((now - b.revealedAt) / 600f).coerceIn(0f, 1f) else 1f
    if (fade <= 0f) return

    // lines draw themselves in
    for (seg in b.segs) {
        val t = ((now - seg.t0) / 220f).coerceIn(0f, 1f)
        val a = b.toScreen(seg.a)
        val e = b.toScreen(lerp(seg.a, seg.b, ease(t)))
        drawLine(seg.color.copy(alpha = 0.3f * fade), a, e, 4.6f * s, StrokeCap.Round)
        drawLine(C.SunLight.copy(alpha = fade), a, e, 1.5f * s, StrokeCap.Round)
    }

    // dots
    val r = b.r
    for (i in 0 until b.n) {
        var c = b.toScreen(b.dots[i])
        val lit = i < next
        val isNext = i == next && !done
        var scale = 1f
        if (lit) {
            val t = (now - b.litAt[i]) / 350f
            if (t < 1f) scale = 1 + 0.6f * sin(PI.toFloat() * t)
        }
        val sh = (now - b.shakeAt[i]) / 400f
        val shaking = b.shakeAt[i] > 0 && sh < 1f
        if (shaking) c += Offset(sin(sh * 25f) * 1.6f * s * (1 - sh), 0f)
        val hinting = isNext && b.hintAt > 0 && now - b.hintAt < 2400
        if (hinting) scale = 1 + 0.9f * abs(sin(PI.toFloat() * (now - b.hintAt) / 600f))
        if (isNext) {
            val pr = (now % 1300) / 1300f
            val ringColor = if (hinting) C.Sun else C.Mint
            drawCircle(ringColor.copy(alpha = 0.9f * (1 - pr) * fade), r * s * (1 + 1.3f * pr), c, style = Stroke((if (hinting) 2f else 1.2f) * s))
        }
        val fill = when {
            shaking -> hex("#FF9AA2")
            lit -> C.Sun
            isNext -> C.Mint
            else -> Color.White
        }
        drawCircle(fill.copy(alpha = fade), r * s * scale, c)
        val label = "${i + 1}"
        val fs = (if (i + 1 >= 10) 0.92f else 1.08f) * r * s * scale
        val layout = tm.measure(label, TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = fs.toSp(), color = C.Ink.copy(alpha = fade)))
        drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
    }

    // sparkles
    b.sparks.removeAll { now - it.t0 > 600 }
    for (sp in b.sparks) {
        val t = (now - sp.t0) / 600f
        val pos = b.toScreen(Pt(sp.at.x + sp.dx * ease(t), sp.at.y + sp.dy * ease(t)))
        drawCircle(sp.color.copy(alpha = (1 - t) * fade), 0.9f * s, pos)
    }

    // Zip follows the end of the line
    val to = b.zipTo
    if (to != null && b.revealedAt == 0L) {
        val from = b.zipFrom ?: to
        val t = ((now - b.zipAt) / 260f).coerceIn(0f, 1f)
        val pos = b.toScreen(lerp(from, to, backOut(t)))
        val zs = 14f * s
        val wing = 0.8f + 0.2f * sin(now / 40f)
        withTransform({
            translate(pos.x - 0.5f * zs, pos.y - 1.07f * zs)
            scale(zs / 120f, zs / 120f, Offset.Zero)
        }) { drawZip(gold, wing = wing) }
    }
}

@Composable
private fun CheerText(text: String, modifier: Modifier) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(text) { pop.animateTo(1f, spring(0.4f, 400f)) }
    Box(modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = -3f }) {
        val st = display(52.sp)
        BasicText(text, style = st.copy(color = C.Ink, drawStyle = Stroke(14f)))
        BasicText(text, style = st.copy(color = C.Sun))
    }
}

class ResultExtras(val finish: FinishResult, val goldNow: Boolean, val newWorld: Int?)

@Composable
private fun ResultCard(state: AppState, level: Level, stars: Int, extras: ResultExtras) {
    val game = state.game
    val p = game.progress
    val pops = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        pops.forEachIndexed { k, a ->
            launch {
                delay(250L + k * 280)
                if (k < stars) game.audio.star(k)
                a.animateTo(1f, spring(0.45f, 400f))
            }
        }
    }
    Column(
        Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(C.Night2).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            for (k in 0 until 3) {
                StarIcon(
                    if (k == 1) 64.dp else 50.dp, k < stars,
                    Modifier.padding(bottom = if (k == 1) 10.dp else 0.dp).graphicsLayer {
                        scaleX = pops[k].value; scaleY = pops[k].value; rotationZ = (1 - pops[k].value) * -40f
                    },
                )
            }
        }
        BasicText(
            zipLine("It's ${article(level.picture.name)} **${level.picture.name}**!"),
            style = body(18.sp, C.Cream),
        )
        if (extras.finish.firstTime) ResultRow { PictureIcon(level.picture, 44.dp, hex(p.colorOf(level.picture))); BasicText("New sticker for your book!", style = body(15.sp, C.Cream, FontWeight.Black)) }
        if (extras.finish.egg) ResultRow {
            Canvas(Modifier.size(40.dp, 44.dp)) { drawEgg(C.Sun) }
            BasicText("Mystery Egg hatched! +$EGG_STARS", style = body(15.sp, C.Cream, FontWeight.Black))
            StarIcon(16.dp)
        }
        extras.newWorld?.let { w ->
            ResultRow {
                Icon24(Ic.Next, 26.dp, C.Mint)
                BasicText(zipLine("New world open: **${Pictures.worlds[w].name}**!"), style = body(15.sp, C.Cream, FontWeight.Black))
            }
        }
        if (extras.goldNow) ResultRow {
            ZipView(44.dp, gold = true)
            BasicText("Zip turned GOLDEN!", style = body(15.sp, C.Cream, FontWeight.Black))
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundButton({ game.audio.pop(); state.openLevel(level.index) }, "Play again", size = 64.dp) { Icon24(Ic.Replay, 28.dp, C.White) }
            RoundButton(
                { game.audio.pop(); state.open(Overlay.ColorIt(level.picture) {}) },
                "Color it in", size = 64.dp, color = C.Coral, base = C.CoralDark,
            ) { Icon24(Ic.Paint, 28.dp, C.White) }
            val last = level.index == Levels.all.lastIndex
            ChunkyButton(
                {
                    game.audio.pop()
                    val ni = level.index + 1
                    when {
                        ni >= Levels.all.size -> state.go(Screen.Home)
                        !p.levelUnlocked(ni) -> state.go(Screen.WorldMap(level.world))
                        Levels.all[ni].world != level.world && ni % LEVELS_PER_WORLD == 0 -> state.go(Screen.WorldMap(Levels.all[ni].world))
                        else -> state.openLevel(ni)
                    }
                },
                C.Mint, C.MintDark, Modifier.weight(1f), height = 66.dp, label = if (last) "Done" else "Next",
            ) {
                BasicText(if (last) "DONE" else "NEXT", style = display(28.sp, C.Ink))
                Icon24(Ic.Next, 26.dp, C.Ink, stroke = 3.2f)
            }
        }
    }
}

@Composable
private fun ResultRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Night).padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

