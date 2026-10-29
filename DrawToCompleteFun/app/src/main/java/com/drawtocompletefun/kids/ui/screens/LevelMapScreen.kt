package com.drawtocompletefun.kids.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtocompletefun.kids.game.Levels
import com.drawtocompletefun.kids.ui.AppState
import com.drawtocompletefun.kids.ui.Sfx
import com.drawtocompletefun.kids.ui.components.Card
import com.drawtocompletefun.kids.ui.components.GameIcon
import com.drawtocompletefun.kids.ui.components.Ico
import com.drawtocompletefun.kids.ui.components.IconTile
import com.drawtocompletefun.kids.ui.components.KText
import com.drawtocompletefun.kids.ui.components.Row3Stars
import com.drawtocompletefun.kids.ui.components.StarIcon
import com.drawtocompletefun.kids.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlin.math.sin

private val NODE_GAP = 112.dp
private val TOP_PAD = 70.dp

@Composable
fun LevelMapScreen(state: AppState, initialWorld: Int) {
    val progress = state.progress
    var worldIndex by remember(initialWorld) { mutableIntStateOf(initialWorld) }
    val world = Levels.worlds[worldIndex]
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) {
            delay(1800)
            message = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.WorldGround[worldIndex]),
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Card(
                Modifier.fillMaxWidth(),
                radius = 28.dp,
                shadow = Palette.WorldAccent[worldIndex].copy(alpha = 0.35f),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Ico.Back, "Back to home", onClick = {
                            state.platform.play(Sfx.Tap)
                            state.goHome()
                        }, color = Palette.Lavender, shadow = Palette.SkyShadow)
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        ) {
                            KText("WORLD ${worldIndex + 1}", size = 13.sp, color = Palette.WorldAccent[worldIndex], weight = FontWeight.Black)
                            KText(world.name, size = 24.sp, weight = FontWeight.Black)
                        }
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Palette.Cream)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            StarIcon(true, 22.dp)
                            KText("${progress.starsIn(world)}/75", size = 16.sp, weight = FontWeight.Black)
                        }
                    }
                    val done = progress.completedIn(world)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xFFE6E8F2)),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(done / 25f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Palette.Purple),
                            )
                        }
                        KText("$done of 25", size = 13.sp, weight = FontWeight.Black)
                    }
                }
            }

            WorldTabs(state, worldIndex) { idx ->
                if (progress.isWorldUnlocked(Levels.worlds[idx])) {
                    state.platform.play(Sfx.Tap)
                    worldIndex = idx
                } else {
                    state.platform.play(Sfx.Oops)
                    message = "Finish World $idx to open ${Levels.worlds[idx].name}!"
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                MapPath(state, worldIndex) { message = it }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Palette.Paper)
                    .navigationBarsPadding()
                    .padding(vertical = 4.dp),
            ) { state.platform.Banner(Modifier.fillMaxWidth()) }
        }

        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
        ) {
            Box(
                Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Palette.Ink)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) { KText(message ?: "", size = 16.sp, color = Palette.Paper, align = TextAlign.Center) }
        }
    }
}

@Composable
private fun WorldTabs(state: AppState, selected: Int, onPick: (Int) -> Unit) {
    val names = listOf("Shapes", "Animals", "Things", "Twisters")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Levels.worlds.forEachIndexed { i, w ->
            val unlocked = state.progress.isWorldUnlocked(w)
            val isSel = i == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSel) Palette.Paper else Palette.Paper.copy(alpha = 0.55f))
                    .then(if (isSel) Modifier.border(3.dp, Palette.Ink, RoundedCornerShape(16.dp)) else Modifier)
                    .clickable(role = Role.Tab) { onPick(i) }
                    .padding(vertical = 8.dp)
                    .semantics { contentDescription = if (unlocked) names[i] else "${names[i]}, locked" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (unlocked) {
                    KText("${i + 1}", size = 18.sp, weight = FontWeight.Black, color = Palette.WorldAccent[i])
                } else {
                    GameIcon(Ico.Lock, Palette.InkSoft, 20.dp)
                }
                KText(names[i], size = 12.sp, weight = FontWeight.Black, color = if (unlocked) Palette.Ink else Palette.InkSoft)
            }
        }
    }
}

@Composable
private fun MapPath(state: AppState, worldIndex: Int, onMessage: (String) -> Unit) {
    val progress = state.progress
    val world = Levels.worlds[worldIndex]
    val levels = (world.firstLevel..world.lastLevel).toList()
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val current = progress.currentLevel

    LaunchedEffect(worldIndex) {
        val idx = (current - world.firstLevel).coerceIn(0, levels.lastIndex)
        val y = with(density) { (TOP_PAD + NODE_GAP * idx).toPx() } - with(density) { 220.dp.toPx() }
        scroll.scrollTo(y.toInt().coerceAtLeast(0))
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
    ) {
        val width = maxWidth
        val total = TOP_PAD * 2 + NODE_GAP * (levels.size - 1)
        fun xFor(i: Int): Dp = width * (0.5f + 0.27f * sin(i * 1.05f))
        fun yFor(i: Int): Dp = TOP_PAD + NODE_GAP * i

        Box(
            Modifier
                .fillMaxWidth()
                .height(total),
        ) {
            val track = Palette.WorldAccent[worldIndex].copy(alpha = 0.18f)
            Canvas(Modifier.matchParentSize()) {
                val path = Path()
                levels.indices.forEach { i ->
                    val p = Offset(xFor(i).toPx(), yFor(i).toPx())
                    if (i == 0) {
                        path.moveTo(p.x, p.y)
                    } else {
                        val prev = Offset(xFor(i - 1).toPx(), yFor(i - 1).toPx())
                        val midY = (prev.y + p.y) / 2f
                        path.cubicTo(prev.x, midY, p.x, midY, p.x, p.y)
                    }
                }
                drawPath(path, track, style = Stroke(34.dp.toPx(), cap = StrokeCap.Round))
                drawPath(
                    path,
                    Color.White,
                    style = Stroke(
                        5.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 14.dp.toPx())),
                    ),
                )
                // A few bushes to make the world feel alive.
                levels.indices.filter { it % 3 == 1 }.forEach { i ->
                    val left = xFor(i) > width / 2
                    val bx = if (left) 34.dp.toPx() else size.width - 34.dp.toPx()
                    val by = yFor(i).toPx() + 20.dp.toPx()
                    drawOval(Palette.Leaf, Offset(bx - 26.dp.toPx(), by - 14.dp.toPx()), Size(52.dp.toPx(), 28.dp.toPx()))
                    drawCircle(Palette.Pink, 4.dp.toPx(), Offset(bx - 8.dp.toPx(), by - 6.dp.toPx()))
                    drawCircle(Palette.Yellow, 4.dp.toPx(), Offset(bx + 9.dp.toPx(), by - 8.dp.toPx()))
                }
            }

            levels.forEachIndexed { i, level ->
                val size = if (level == current && !progress.isCompleted(level)) 84.dp else 66.dp
                Box(
                    Modifier
                        .offset(x = xFor(i) - size / 2, y = yFor(i) - size / 2)
                        .width(size),
                ) {
                    LevelNode(state, level, size, onMessage)
                }
            }
        }
    }
}

@Composable
private fun LevelNode(state: AppState, level: Int, size: Dp, onMessage: (String) -> Unit) {
    val progress = state.progress
    val unlocked = progress.isUnlocked(level)
    val done = progress.isCompleted(level)
    val isCurrent = unlocked && !done
    val treasure = Levels.get(level).isTreasureLevel && !done
    val noRipple = remember { MutableInteractionSource() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size)) {
            if (isCurrent) PulseRing(Modifier.matchParentSize())
            val face = when {
                isCurrent -> Palette.Yellow
                done -> Palette.Purple
                else -> Palette.Locked
            }
            val shadow = when {
                isCurrent -> Palette.YellowShadow
                done -> Palette.PurpleShadow
                else -> Palette.LockedShadow
            }
            Box(
                Modifier
                    .matchParentSize()
                    .offset(y = 5.dp)
                    .clip(CircleShape)
                    .background(shadow),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(face)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable(interactionSource = noRipple, indication = null, role = Role.Button) {
                        if (unlocked) {
                            state.platform.play(Sfx.Whoosh)
                            state.openLevel(level)
                        } else {
                            state.platform.play(Sfx.Oops)
                            onMessage("Finish level ${level - 1} first!")
                        }
                    }
                    .semantics {
                        contentDescription = when {
                            done -> "Level $level, ${progress.starsFor(level)} stars"
                            unlocked -> "Play level $level"
                            else -> "Level $level, locked"
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (unlocked) {
                    KText(
                        "$level",
                        size = if (isCurrent) 30.sp else 24.sp,
                        weight = FontWeight.Black,
                        color = if (done) Palette.Paper else Palette.Ink,
                    )
                } else {
                    GameIcon(Ico.Lock, Palette.Paper, 26.dp)
                }
            }
            if (treasure) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-6).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF5F6368))
                        .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                        .semantics { contentDescription = "Opens after an ad" },
                ) { KText("Ad", size = 12.sp, color = Color.White, weight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (done) {
            Row3Stars(progress.starsFor(level), 18.dp)
        } else if (isCurrent) {
            Bubble("Play!")
        }
    }
}

@Composable
private fun PulseRing(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "ring")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "p")
    Box(
        modifier
            .graphicsLayer {
                scaleX = 1f + p * 0.6f
                scaleY = 1f + p * 0.6f
                alpha = (1f - p) * 0.7f
            }
            .clip(CircleShape)
            .background(Palette.Yellow),
    )
}

@Composable
private fun Bubble(text: String) {
    val t = rememberInfiniteTransition(label = "bubble")
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    Box(
        Modifier
            .graphicsLayer { translationY = -y * 6f * density }
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.Ink)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) { KText(text, size = 13.sp, color = Palette.Paper, weight = FontWeight.Black) }
}
