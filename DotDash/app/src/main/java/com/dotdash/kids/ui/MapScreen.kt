package com.dotdash.kids.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotdash.kids.game.LEVELS_PER_WORLD
import com.dotdash.kids.game.Level
import com.dotdash.kids.game.Levels
import com.dotdash.kids.game.Pictures
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.sin

private val STEP = 108.dp
private val BOTTOM = 150.dp
private val TOP = 210.dp
private val TRACK = BOTTOM + TOP + STEP * (LEVELS_PER_WORLD - 1)
private fun nodeX(i: Int) = 0.5f + 0.27f * sin(i * 1.05f + 0.5f)
private fun nodeY(i: Int): Dp = TRACK - BOTTOM - STEP * i

/** Winding level map for one world. Level 1 sits at the bottom; the path climbs. */
@Composable
fun MapScreen(state: AppState, world: Int) {
    val game = state.game
    val p = game.progress
    val w = Pictures.worlds[world]
    val levels = Levels.inWorld(world)
    val next = p.nextLevel()
    val currentIndex = if (Levels.all[next].world == world) next else -1
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    var say by remember { mutableStateOf<String?>(null) }
    var sayId by remember { mutableIntStateOf(0) }
    val shakes = remember { mutableStateOf(mapOf<Int, Int>()) }
    var viewport by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        game.audio.music(world)
        // Scroll so the current level is in view (level 1 is at the bottom).
        val target = if (currentIndex >= 0) currentIndex % LEVELS_PER_WORLD
        else levels.lastOrNull { p.isDone(it.index) }?.number?.minus(1) ?: 0
        val height = snapshotFlow { viewport }.first { it > 0 }
        val y = with(density) { nodeY(target).toPx() }
        scroll.scrollTo((y - height * 0.6f).toInt().coerceAtLeast(0))
    }
    LaunchedEffect(sayId) {
        if (sayId == 0) return@LaunchedEffect
        delay(2600)
        say = null
    }

    Box(Modifier.fillMaxSize().background(hex(w.bg))) {
        Sky(18, seed = 11 + world)
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundButton({ game.audio.tap(); state.go(Screen.Home) }, "Back home") { Icon24(Ic.Back, 24.dp, C.White, stroke = 3f) }
                BasicText(w.name, Modifier.weight(1f), style = display(26.sp).copy(textAlign = TextAlign.Center))
                Pill {
                    StarIcon(22.dp)
                    BasicText("${p.worldStars(world)}/${LEVELS_PER_WORLD * 3}", style = display(18.sp))
                }
            }
            BoxWithConstraints(
                Modifier.fillMaxWidth().weight(1f).onSizeChanged { viewport = it.height }.verticalScroll(scroll),
                contentAlignment = Alignment.TopCenter,
            ) {
                val trackW = maxWidth.coerceAtMost(520.dp)
                Box(Modifier.width(trackW).height(TRACK)) {
                    // ground
                    Box(Modifier.offset(x = -trackW * 0.2f, y = TRACK - 110.dp).size(trackW * 1.4f, 260.dp).clip(CircleShape).background(hex(w.bg2)))
                    // decorations on the side away from each node
                    for (i in 0 until LEVELS_PER_WORLD) {
                        val left = nodeX(i) > 0.5f
                        Canvas(
                            Modifier.offset(x = if (left) trackW * 0.04f else trackW * 0.96f - 74.dp, y = nodeY(i) - 30.dp).size(74.dp),
                        ) { drawDecor(w.id, i % 3) }
                    }
                    // the dotted path
                    Canvas(Modifier.width(trackW).height(TRACK)) {
                        val pts = (0 until LEVELS_PER_WORLD).map { Offset(nodeX(it) * size.width, nodeY(it).toPx()) }
                        val doneTo = levels.indexOfLast { p.isDone(it.index) } + 1
                        val dash = PathEffect.dashPathEffect(floatArrayOf(1f, 22.dp.toPx()), 0f)
                        drawPath(smooth(pts), Color.White.copy(alpha = 0.28f), style = Stroke(11.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash))
                        if (doneTo > 0) {
                            drawPath(smooth(pts.take((doneTo + 1).coerceAtMost(pts.size))), C.Sun, style = Stroke(11.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash))
                        }
                    }
                    // sign to the next world
                    Box(Modifier.fillMaxWidth().offset(y = nodeY(LEVELS_PER_WORLD - 1) - 150.dp), contentAlignment = Alignment.TopCenter) {
                        NextWorldSign(state, world) { msg -> say = msg; sayId++ }
                    }
                    levels.forEachIndexed { i, lv ->
                        Box(
                            Modifier.offset(x = trackW * nodeX(i) - 60.dp, y = nodeY(i) - 60.dp).size(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            LevelNode(
                                state, lv, current = lv.index == currentIndex, shake = shakes.value[lv.index] ?: 0,
                                onLocked = {
                                    shakes.value = shakes.value + (lv.index to (shakes.value[lv.index] ?: 0) + 1)
                                    say = "Finish level ${lv.number - 1} first!"
                                    sayId++
                                },
                            )
                        }
                    }
                }
            }
        }
        say?.let {
            Bubble(zipLine(it), Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
        }
    }
}

@Composable
private fun LevelNode(state: AppState, lv: Level, current: Boolean, shake: Int, onLocked: () -> Unit) {
    val p = state.game.progress
    val done = p.isDone(lv.index)
    val open = p.levelUnlocked(lv.index)
    val best = p.bestStars(lv.index)
    val ring = rememberInfiniteTransition(label = "ring")
    val r by ring.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "r")
    val wobble by ring.animateFloat(-5f, 5f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "w")
    Box(contentAlignment = Alignment.Center) {
        if (current && !done) {
            Box(
                Modifier.size(86.dp).graphicsLayer { scaleX = 1 + 0.7f * r; scaleY = 1 + 0.7f * r; alpha = 0.8f * (1 - r) }
                    .border(5.dp, C.Mint, CircleShape),
            )
        }
        val size = if (open && !done) 86.dp else 74.dp
        val (face, base) = when {
            done -> C.White to C.Muted
            open -> C.Mint to C.MintDark
            else -> Color.White.copy(alpha = 0.14f) to Color.Black.copy(alpha = 0.25f)
        }
        RoundButton(
            {
                if (open) {
                    state.game.audio.pop()
                    state.openLevel(lv.index)
                } else {
                    state.game.audio.wrong()
                    onLocked()
                }
            },
            label = "Level ${lv.number}" + if (done) ", $best stars" else if (!open) ", locked" else "",
            modifier = Modifier.shake(shake),
            size = size, color = face, base = base,
        ) {
            when {
                done -> PictureIcon(lv.picture, 54.dp, hex(p.colorOf(lv.picture)))
                open -> BasicText("${lv.number}", style = display(38.sp, C.Ink))
                else -> Icon24(Ic.Lock, 26.dp, Color.White.copy(alpha = 0.65f))
            }
        }
        if (done) {
            Box(Modifier.align(Alignment.TopStart).offset(14.dp, 12.dp).size(26.dp).clip(CircleShape).background(C.Sun), contentAlignment = Alignment.Center) {
                BasicText("${lv.number}", style = display(14.sp, C.Ink))
            }
            Row(Modifier.align(Alignment.BottomCenter).offset(y = 4.dp)) { repeat(3) { StarIcon(18.dp, it < best) } }
        }
        if (lv.egg) {
            Canvas(
                Modifier.align(Alignment.TopEnd).offset((-4).dp, 8.dp).size(30.dp, 36.dp)
                    .graphicsLayer { rotationZ = if (p.eggHatched(lv.index)) 0f else wobble; alpha = if (p.eggHatched(lv.index)) 0.45f else 1f },
            ) { drawEgg() }
        }
        if (current && !done) {
            ZipView(60.dp, Modifier.align(Alignment.TopCenter).offset(y = (-36).dp).bob(), gold = p.goldenZip())
        }
    }
}

@Composable
private fun NextWorldSign(state: AppState, world: Int, onLocked: (String) -> Unit) {
    val p = state.game.progress
    val nw = Pictures.worlds.getOrNull(world + 1)
    if (nw == null) {
        Pill(Modifier.padding(4.dp)) {
            StarIcon(22.dp)
            BasicText("You found them all!", style = display(18.sp, C.Sun))
        }
        return
    }
    val open = p.worldUnlocked(world + 1)
    ChunkyButton(
        onClick = {
            if (open) {
                state.game.audio.pop()
                state.go(Screen.WorldMap(world + 1))
            } else {
                state.game.audio.wrong()
                onLocked("Collect **${nw.unlock}** stars to open ${nw.name}!")
            }
        },
        color = hex(nw.color).copy(alpha = if (open) 1f else 0.55f), base = Color.Black.copy(alpha = 0.3f),
        modifier = Modifier.widthIn(min = 200.dp),
        height = 52.dp, depth = 6.dp, shape = RoundedCornerShape(18.dp),
        label = nw.name,
    ) {
        Icon24(if (open) Ic.Next else Ic.Lock, 22.dp, C.White)
        BasicText(nw.name, style = display(18.sp, C.White))
        if (!open) {
            StarIcon(14.dp)
            BasicText("${nw.unlock}", style = body(13.sp, C.White, FontWeight.Black))
        }
    }
}

/** Catmull-Rom style smoothing through the node centres. */
private fun smooth(points: List<Offset>): Path = Path().apply {
    moveTo(points[0].x, points[0].y)
    for (i in 0 until points.size - 1) {
        val p0 = points.getOrElse(i - 1) { points[i] }
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points.getOrElse(i + 2) { p2 }
        cubicTo(
            p1.x + (p2.x - p0.x) / 6, p1.y + (p2.y - p0.y) / 6,
            p2.x - (p3.x - p1.x) / 6, p2.y - (p3.y - p1.y) / 6,
            p2.x, p2.y,
        )
    }
}

fun DrawScope.drawEgg(fill: Color = hex("#FFF1C9")) {
    inBox(0f, 0f, 100f, 120f) {
        drawOval(fill, Offset(10f, 16f), Size(80f, 100f))
        drawOval(C.Ink, Offset(10f, 16f), Size(80f, 100f), style = Stroke(6f))
        drawCircle(C.Mint, 8f, Offset(34f, 48f))
        drawCircle(hex("#FF8A3D"), 6f, Offset(64f, 38f))
        drawCircle(hex("#9B6BFF"), 10f, Offset(64f, 80f))
    }
}

/** Little scenery per world: trees and flowers, clouds and buildings, ferns and volcanoes. */
private fun DrawScope.drawDecor(world: String, kind: Int) {
    inBox(0f, 0f, 80f, 90f) {
        when (world) {
            "animals" -> when (kind) {
                0 -> {
                    drawRect(hex("#7A4A24"), Offset(35f, 50f), Size(10f, 36f))
                    drawCircle(hex("#3FAE4A"), 26f, Offset(40f, 38f))
                    drawCircle(hex("#3FAE4A"), 16f, Offset(24f, 48f))
                    drawCircle(hex("#2F9442"), 16f, Offset(56f, 48f))
                }
                1 -> {
                    drawCircle(hex("#2F9442"), 18f, Offset(24f, 68f))
                    drawCircle(hex("#3FAE4A"), 22f, Offset(44f, 60f))
                    drawCircle(hex("#2F9442"), 15f, Offset(62f, 70f))
                    drawCircle(C.Coral, 3f, Offset(40f, 54f))
                    drawCircle(C.Sun, 3f, Offset(58f, 64f))
                }
                else -> {
                    drawLine(hex("#3FAE4A"), Offset(40f, 88f), Offset(40f, 50f), 4f)
                    for (o in listOf(Offset(40f, 32f), Offset(51f, 44f), Offset(29f, 44f), Offset(40f, 56f))) drawCircle(C.Coral, 8f, o)
                    drawCircle(C.Sun, 7f, Offset(40f, 44f))
                }
            }
            "vehicles" -> when (kind) {
                0 -> {
                    val cloud = Color(0xFFE6EAFF)
                    drawCircle(cloud, 14f, Offset(24f, 50f))
                    drawCircle(cloud, 18f, Offset(42f, 42f))
                    drawCircle(cloud, 12f, Offset(60f, 52f))
                    drawRect(cloud, Offset(16f, 50f), Size(52f, 12f))
                }
                1 -> {
                    drawRect(hex("#2457E6"), Offset(16f, 14f), Size(48f, 76f))
                    for ((x, y) in listOf(22f to 22f, 46f to 22f, 22f to 42f, 46f to 42f)) drawRect(C.SunLight, Offset(x, y), Size(11f, 11f))
                    drawRect(hex("#16399E"), Offset(33f, 66f), Size(14f, 24f))
                }
                else -> {
                    drawPath(Paths.svg("M40 20 L56 80 H24 Z"), hex("#FF8A3D"))
                    drawPath(Paths.svg("M33 46 H47 L50 56 H30 Z"), Color.White)
                    drawRect(hex("#FF8A3D"), Offset(16f, 78f), Size(48f, 8f))
                }
            }
            else -> when (kind) {
                0 -> {
                    val fern = Stroke(7f, cap = StrokeCap.Round)
                    drawPath(Paths.svg("M40 88 Q36 56 10 38 M40 88 Q43 52 40 16 M40 88 Q47 56 72 36"), hex("#1FA97A"), style = fern)
                    drawPath(Paths.svg("M40 88 Q28 72 14 68 M40 88 Q54 72 68 66"), hex("#3FAE4A"), style = Stroke(6f, cap = StrokeCap.Round))
                }
                1 -> {
                    drawPath(Paths.svg("M40 24 L12 86 H70 Z"), hex("#0B3A36"))
                    drawPath(Paths.svg("M32 40 Q40 32 48 40 L44 50 Q40 44 36 50 Z"), hex("#FF8A3D"))
                    drawCircle(hex("#9FB8B4").copy(alpha = 0.6f), 5f, Offset(46f, 22f))
                }
                else -> {
                    val bone = hex("#FFF1C9")
                    drawRect(bone, Offset(14f, 56f), Size(52f, 9f))
                    for (o in listOf(Offset(14f, 54f), Offset(14f, 67f), Offset(66f, 54f), Offset(66f, 67f))) drawCircle(bone, 7f, o)
                }
            }
        }
    }
}
