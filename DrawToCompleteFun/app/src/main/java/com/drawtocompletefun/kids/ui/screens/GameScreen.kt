package com.drawtocompletefun.kids.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtocompletefun.kids.game.Level
import com.drawtocompletefun.kids.game.Levels
import com.drawtocompletefun.kids.game.Progress
import com.drawtocompletefun.kids.game.Pt
import com.drawtocompletefun.kids.game.StrokeMatcher
import com.drawtocompletefun.kids.ui.AppState
import com.drawtocompletefun.kids.ui.Sfx
import com.drawtocompletefun.kids.ui.components.ButtonRowLabel
import com.drawtocompletefun.kids.ui.components.Card
import com.drawtocompletefun.kids.ui.components.ChunkyButton
import com.drawtocompletefun.kids.ui.components.Confetti
import com.drawtocompletefun.kids.ui.components.GameIcon
import com.drawtocompletefun.kids.ui.components.Ico
import com.drawtocompletefun.kids.ui.components.IconTile
import com.drawtocompletefun.kids.ui.components.KText
import com.drawtocompletefun.kids.ui.components.PictureView
import com.drawtocompletefun.kids.ui.components.Pip
import com.drawtocompletefun.kids.ui.components.StarIcon
import com.drawtocompletefun.kids.ui.components.board
import com.drawtocompletefun.kids.ui.components.drawDashedShape
import com.drawtocompletefun.kids.ui.components.drawShape
import com.drawtocompletefun.kids.ui.components.drawSparkles
import com.drawtocompletefun.kids.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Outcome of a finished level, shown on the celebration screen. */
class LevelResult(
    val stars: Int,
    val misses: Int,
    val hints: Int,
    val stickerUnlocked: Boolean,
    val colors: Map<String, Color>,
)

fun starsFor(misses: Int, hints: Int): Int = when {
    hints == 0 && misses <= 1 -> 3
    hints <= 1 && misses <= 3 -> 2
    else -> 1
}

private val PRAISE = listOf("Great!", "Yes!", "Super!", "Nice line!", "Wow!", "You got it!", "Perfect!")
private val TRY_AGAIN = listOf("Almost! Find the gap.", "Oops! Try again.", "So close! Look again.")

@Composable
fun GameScreen(state: AppState, levelNumber: Int, run: Int) {
    val level = remember(levelNumber) { Levels.get(levelNumber) }
    val progress = state.progress
    val platform = state.platform
    val scope = rememberCoroutineScope()

    val found = remember(run) { mutableStateMapOf<String, Color>() }
    val reveal = remember(run) { mutableStateMapOf<String, Float>() }
    val stroke = remember(run) { mutableStateListOf<Pt>() }
    var wrongStroke by remember(run) { mutableStateOf<List<Pt>>(emptyList()) }
    val wrongFade = remember(run) { Animatable(0f) }
    var misses by remember(run) { mutableIntStateOf(0) }
    var hints by remember(run) { mutableIntStateOf(0) }
    var hintTag by remember(run) { mutableStateOf<String?>(null) }
    var pen by remember { mutableIntStateOf(0) }
    var bubble by remember(run) { mutableStateOf("Draw the missing lines!") }
    var sparkleAt by remember(run) { mutableStateOf<Pt?>(null) }
    var sparkle by remember(run) { mutableFloatStateOf(0f) }
    var result by remember(run) { mutableStateOf<LevelResult?>(null) }
    var finishing by remember(run) { mutableStateOf(false) }

    val missing = level.missing
    val remaining = missing.filter { it.tag !in found }

    fun finishStroke() {
        if (stroke.size < 2 || finishing) {
            stroke.clear()
            return
        }
        val drawn = stroke.toList()
        stroke.clear()
        val visible = level.picture.shapes.filter { !it.filled && (it.tag !in level.missingTags || it.tag in found) }
        val match = StrokeMatcher.bestMatch(drawn, missing.filter { it.tag !in found }, level.world.tolerance, visible)
        if (match != null) {
            found[match.tag] = Palette.Pens[pen]
            platform.play(Sfx.Pop)
            bubble = PRAISE.random()
            if (hintTag == match.tag) hintTag = null
            sparkleAt = match.points[match.points.size / 2]
            scope.launch { animate(0f, 1f, animationSpec = tween(420)) { v, _ -> reveal[match.tag] = v } }
            scope.launch { animate(0f, 1f, animationSpec = tween(700)) { v, _ -> sparkle = v } }
            if (found.size == missing.size) {
                finishing = true
                scope.launch {
                    delay(750)
                    val stickersBefore = progress.unlockedStickers
                    val stars = starsFor(misses, hints)
                    progress.record(level.number, stars)
                    result = LevelResult(stars, misses, hints, progress.unlockedStickers > stickersBefore, found.toMap())
                }
            }
        } else {
            misses++
            platform.play(Sfx.Oops)
            bubble = TRY_AGAIN.random()
            wrongStroke = drawn
            scope.launch {
                wrongFade.snapTo(1f)
                wrongFade.animateTo(0f, tween(750))
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Cream),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Ico.Close, "Leave level", onClick = {
                    platform.play(Sfx.Tap)
                    state.quitLevel(level.number)
                }, shadow = Palette.CreamShadow)
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    KText(level.world.name.uppercase(), size = 12.sp, color = Palette.WorldAccent[level.world.index], weight = FontWeight.Black)
                    KText("Level ${level.number}", size = 26.sp, weight = FontWeight.Black)
                }
                Box {
                    IconTile(
                        Ico.Bulb,
                        "Show a hint",
                        onClick = {
                            val next = missing.firstOrNull { it.tag !in found }
                            if (next != null && result == null) {
                                if (hintTag != next.tag) hints++
                                hintTag = next.tag
                                bubble = "Look for the dotted line!"
                                platform.play(Sfx.Tap)
                            }
                        },
                        color = Palette.Yellow,
                        shadow = Palette.YellowShadow,
                    )
                }
            }

            // Prompt
            Card(Modifier.fillMaxWidth(), shadow = Palette.CreamShadow, radius = 22.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Pip(Modifier.size(28.dp, 62.dp), happy = misses == 0 || found.isNotEmpty(), bob = true)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    ) {
                        KText(level.title, size = 20.sp, weight = FontWeight.Black)
                        KText(bubble, size = 14.sp, color = Palette.InkSoft)
                    }
                }
            }

            // Drawing board
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    Modifier
                        .widthIn(max = 520.dp)
                        .aspectRatio(1f, matchHeightConstraintsFirst = true),
                    shadow = Palette.CreamShadow,
                    radius = 28.dp,
                ) {
                    DrawingBoard(
                        level = level,
                        found = found,
                        reveal = reveal,
                        stroke = stroke,
                        penColor = Palette.Pens[pen],
                        hintTag = hintTag,
                        wrongStroke = wrongStroke,
                        wrongAlpha = wrongFade.value,
                        sparkleAt = sparkleAt,
                        sparkle = sparkle,
                        enabled = !finishing,
                        onStart = { stroke.clear(); stroke.add(it) },
                        onMove = { stroke.add(it) },
                        onEnd = { finishStroke() },
                    )
                    Row(
                        Modifier
                            .padding(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Palette.Lavender)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Palette.Coral),
                        )
                        KText("Lines: ${found.size} of ${missing.size}", size = 13.sp, weight = FontWeight.Black)
                    }
                }
            }

            // Crayons
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Card(shadow = Palette.CreamShadow, radius = 18.dp) {
                    Row(
                        Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Palette.Pens.forEachIndexed { i, c ->
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(4.dp, if (i == pen) Palette.Ink else Color.White, CircleShape)
                                    .clickable(role = Role.RadioButton) {
                                        pen = i
                                        platform.play(Sfx.Tap)
                                    }
                                    .semantics { contentDescription = "Crayon ${i + 1}" },
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                IconTile(Ico.Replay, "Start again", onClick = {
                    platform.play(Sfx.Tap)
                    state.restart(level.number)
                }, shadow = Palette.CreamShadow, size = 60.dp)
            }
        }

        val r = result
        AnimatedVisibility(
            visible = r != null,
            enter = fadeIn(tween(250)) + slideInVertically(tween(350)) { it / 6 },
        ) {
            if (r != null) LevelComplete(state, level, r)
        }
    }
}

@Composable
private fun DrawingBoard(
    level: Level,
    found: Map<String, Color>,
    reveal: Map<String, Float>,
    stroke: List<Pt>,
    penColor: Color,
    hintTag: String?,
    wrongStroke: List<Pt>,
    wrongAlpha: Float,
    sparkleAt: Pt?,
    sparkle: Float,
    enabled: Boolean,
    onStart: (Pt) -> Unit,
    onMove: (Pt) -> Unit,
    onEnd: () -> Unit,
) {
    val blink = rememberInfiniteTransition(label = "hint")
    val hintAlpha by blink.animateFloat(0.25f, 0.95f, infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
    val missingTags = level.missingTags.toSet()

    Canvas(
        Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Drawing board. ${level.title}" }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                fun toBoard(o: Offset): Pt {
                    val s = minOf(size.width, size.height).toFloat()
                    val ox = (size.width - s) / 2f
                    val oy = (size.height - s) / 2f
                    return Pt((o.x - ox) / s * 100f, (o.y - oy) / s * 100f)
                }
                detectDragGestures(
                    onDragStart = { onStart(toBoard(it)) },
                    onDragEnd = { onEnd() },
                    onDragCancel = { onEnd() },
                    onDrag = { change, _ ->
                        change.consume()
                        onMove(toBoard(change.position))
                    },
                )
            },
    ) {
        // Dotted paper
        val step = size.minDimension / 16f
        var y = step / 2f
        while (y < size.height) {
            var x = step / 2f
            while (x < size.width) {
                drawCircle(Palette.PaperDot, 1.8.dp.toPx(), Offset(x, y))
                x += step
            }
            y += step
        }

        val w = size.minDimension * 0.022f
        level.picture.shapes.forEach { s ->
            when {
                s.tag !in missingTags -> drawShape(s, Palette.Ink, w)
                s.tag in found -> drawShape(s, found.getValue(s.tag), w * 1.3f, reveal[s.tag] ?: 1f)
                s.tag == hintTag -> drawDashedShape(s, Palette.Purple.copy(alpha = hintAlpha), w)
            }
        }

        fun polyline(points: List<Pt>, color: Color, width: Float) {
            if (points.size < 2) return
            val path = Path()
            points.forEachIndexed { i, p ->
                val o = board(p)
                if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            }
            drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        if (wrongAlpha > 0f) polyline(wrongStroke, Palette.Locked.copy(alpha = wrongAlpha), w * 1.1f)
        polyline(stroke, penColor, w * 1.3f)
        sparkleAt?.let { drawSparkles(board(it), sparkle, Palette.Yellow) }
    }
}

// ---------------------------------------------------------------- Level complete

private val CHEERS = listOf("Awesome!", "Super star!", "Fantastic!", "You did it!", "Amazing!")

@Composable
private fun LevelComplete(state: AppState, level: Level, result: LevelResult) {
    val platform = state.platform
    val progress = state.progress
    var shown by remember { mutableIntStateOf(0) }
    val cheer = remember { CHEERS.random() }
    val starScales = remember { List(3) { Animatable(0f) } }

    LaunchedEffect(Unit) {
        platform.play(Sfx.Fanfare)
        delay(450)
        for (i in 0 until result.stars) {
            shown = i + 1
            platform.play(listOf(Sfx.Star1, Sfx.Star2, Sfx.Star3)[i])
            launch { starScales[i].animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 300f)) }
            delay(420)
        }
        if (result.stickerUnlocked) platform.play(Sfx.Unlock)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.PurpleDeep)
            // Swallow taps so nothing underneath can be pressed.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Confetti()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Card(color = Palette.Yellow, shadow = Palette.YellowShadow, radius = 20.dp) {
                KText(cheer, size = 40.sp, weight = FontWeight.Black, modifier = Modifier.padding(horizontal = 30.dp, vertical = 4.dp))
            }
            KText("Level ${level.number} complete", size = 17.sp, color = Palette.Paper, modifier = Modifier.padding(top = 14.dp))

            Row(
                Modifier
                    .height(130.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(86.dp, 112.dp, 86.dp).forEachIndexed { i, sz ->
                    Box(
                        Modifier
                            .size(sz)
                            .offset(y = if (i == 1) (-14).dp else 0.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        StarIcon(false, sz * 0.92f, emptyColor = Palette.PurpleNight)
                        val k = starScales[i].value
                        Box(
                            Modifier.graphicsLayer {
                                scaleX = k
                                scaleY = k
                                alpha = if (k <= 0.01f) 0f else 1f
                            },
                        ) { StarIcon(true, sz) }
                    }
                }
            }
            KText(
                if (shown > 0) "+${result.stars} stars" else " ",
                size = 20.sp,
                color = Palette.Paper,
                weight = FontWeight.Black,
            )

            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth(), shadow = Palette.PurpleNight, radius = 26.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Palette.Cream),
                    ) {
                        PictureView(
                            level.picture,
                            Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            colored = result.colors,
                            widthFactor = 1.6f,
                        )
                    }
                    Column(
                        Modifier.padding(start = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CheckLine("Picture complete!")
                        if (result.misses <= 1) CheckLine("Super neat lines")
                        if (result.hints == 0) CheckLine("No hints used")
                        if (result.misses == 0) CheckLine("First try!")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            StickerProgress(state, result)

            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconTile(Ico.Replay, "Play again", onClick = {
                    platform.play(Sfx.Tap)
                    state.replay(level.number)
                }, shadow = Palette.PurpleNight, size = 64.dp)
                ChunkyButton(
                    onClick = {
                        platform.play(Sfx.Whoosh)
                        state.next(level.number)
                    },
                    color = Palette.Yellow,
                    shadow = Palette.YellowShadow,
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp),
                    shape = RoundedCornerShape(26.dp),
                    depth = 7.dp,
                    label = if (level.number < Levels.COUNT) "Next level" else "Back to map",
                ) {
                    ButtonRowLabel(
                        Ico.Next,
                        if (level.number < Levels.COUNT) "Next level" else "All done!",
                        Palette.Ink,
                    )
                }
                IconTile(Ico.Map, "Level map", onClick = {
                    platform.play(Sfx.Tap)
                    state.backToMapFromResult(level.number)
                }, shadow = Palette.PurpleNight, size = 64.dp)
            }
        }
    }
}

@Composable
private fun CheckLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GameIcon(Ico.Check, Palette.GreenShadow, 20.dp)
        KText(text, size = 14.sp)
    }
}

@Composable
private fun StickerProgress(state: AppState, result: LevelResult) {
    val progress = state.progress
    val toNext = progress.starsToNextSticker
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.PurpleShadow)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Palette.Pink),
            contentAlignment = Alignment.Center,
        ) { GameIcon(Ico.Sticker, Palette.Ink, 28.dp) }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            val text = when {
                result.stickerUnlocked -> "New sticker unlocked! Open your sticker book."
                toNext == null -> "You collected every sticker!"
                else -> "$toNext more star${if (toNext == 1) "" else "s"} to the next sticker!"
            }
            KText(text, size = 14.sp, color = Palette.Paper, weight = FontWeight.Black)
            if (toNext != null) {
                val into = STICKER_STEP - toNext
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Palette.PurpleNight),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((into.toFloat() / STICKER_STEP).coerceIn(0.04f, 1f))
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Palette.Yellow),
                    )
                }
            }
        }
    }
}

private const val STICKER_STEP = Progress.STARS_PER_STICKER
