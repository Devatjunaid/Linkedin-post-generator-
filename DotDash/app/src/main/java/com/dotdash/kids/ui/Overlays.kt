package com.dotdash.kids.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotdash.kids.game.DAILY_GIFT_STARS
import com.dotdash.kids.game.Levels
import com.dotdash.kids.game.Picture
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun OverlayHost(state: AppState, overlay: Overlay) {
    when (overlay) {
        Overlay.Exit -> ExitOverlay(state, overlay)
        else -> Dim(onDismiss = { state.close(overlay) }) {
            when (overlay) {
                Overlay.Gift -> GiftCard(state, overlay)
                is Overlay.ColorIt -> ColorCard(state, overlay)
                is Overlay.Sticker -> StickerCard(state, overlay)
                Overlay.Exit -> Unit
            }
        }
    }
}

/** Darkened backdrop; tapping outside the card closes it. */
@Composable
private fun Dim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val appear = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(0.5f, 500f)) }
    Box(
        Modifier.fillMaxSize().background(Color(0xC70C0928))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.graphicsLayer { scaleX = appear.value; scaleY = appear.value }
                // swallow taps on the card itself
                .clickable(remember { MutableInteractionSource() }, indication = null) {},
        ) { content() }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.widthIn(max = 380.dp).fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(C.Night2)
            .verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

// --- Daily gift ------------------------------------------------------------------
@Composable
private fun GiftCard(state: AppState, overlay: Overlay) {
    val game = state.game
    val p = game.progress
    val readyAtOpen = remember { p.giftReady() }
    var opened by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    val lid = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val t = rememberInfiniteTransition(label = "gift")
    val wobble by t.animateFloat(-5f, 5f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "wobble")

    Box {
        Card {
            BasicText("Daily Gift", style = display(32.sp, C.Sun))
            if (readyAtOpen) {
                Canvas(
                    Modifier.size(180.dp)
                        .graphicsLayer { rotationZ = if (opened) 0f else wobble; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.9f) }
                        .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                            if (opened) return@clickable
                            opened = true
                            p.claimGift()
                            game.audio.gift()
                            confetti++
                            game.say("You got $DAILY_GIFT_STARS stars!")
                            scope.launch { lid.animateTo(1f, spring(0.5f, 200f)) }
                        }
                        .semantics { contentDescription = "Open your gift" },
                ) { drawGift(lid.value) }
                if (!opened) BasicText("Tap the present!", style = body(18.sp, C.Cream, FontWeight.Black))
                else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText("+$DAILY_GIFT_STARS ", style = display(44.sp, C.Sun))
                        StarIcon(36.dp)
                    }
                    ChunkyButton({ state.close(overlay) }, C.Sun, C.SunDark, Modifier.widthIn(min = 140.dp), label = "Yay") {
                        BasicText("YAY!", style = display(24.sp, C.Ink))
                    }
                }
            } else {
                ZipView(110.dp, Modifier.bob(), gold = p.goldenZip())
                BasicText(
                    "You opened today's gift.\nCome back tomorrow for another one!",
                    style = body(18.sp, C.Cream, FontWeight.Black).copy(textAlign = TextAlign.Center),
                )
                ChunkyButton({ state.close(overlay) }, C.Sun, C.SunDark, Modifier.widthIn(min = 140.dp), label = "OK") {
                    BasicText("OK", style = display(24.sp, C.Ink))
                }
            }
        }
        Confetti(confetti)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGift(lid: Float) {
    inBox(0f, 0f, 160f, 160f) {
        val ink = Stroke(5f, join = StrokeJoin.Round)
        // box
        drawRoundRect(hex("#FF7A8C"), Offset(28f, 72f), Size(104f, 72f), androidx.compose.ui.geometry.CornerRadius(8f))
        drawRoundRect(C.Ink, Offset(28f, 72f), Size(104f, 72f), androidx.compose.ui.geometry.CornerRadius(8f), style = ink)
        drawRect(C.Sun, Offset(70f, 72f), Size(20f, 72f))
        drawRect(C.Ink, Offset(70f, 72f), Size(20f, 72f), style = ink)
        // lid flies off when opened
        withTransform({
            translate(40f * lid, -120f * lid)
            rotate(35f * lid, Offset(80f, 59f))
        }) {
            drawRoundRect(C.Coral, Offset(18f, 44f), Size(124f, 30f), androidx.compose.ui.geometry.CornerRadius(8f))
            drawRoundRect(C.Ink, Offset(18f, 44f), Size(124f, 30f), androidx.compose.ui.geometry.CornerRadius(8f), style = ink)
            drawRect(C.Sun, Offset(70f, 44f), Size(20f, 30f))
            drawRect(C.Ink, Offset(70f, 44f), Size(20f, 30f), style = ink)
            val bow = Paths.svg("M80 44 C 60 10, 30 22, 46 40 Z M80 44 C 100 10, 130 22, 114 40 Z")
            drawPath(bow, C.Sun)
            drawPath(bow, C.Ink, style = ink)
        }
    }
}

// --- Exit screen -------------------------------------------------------------------
@Composable
private fun ExitOverlay(state: AppState, overlay: Overlay) {
    val game = state.game
    val p = game.progress
    var leaving by remember { mutableStateOf(false) }
    val fly = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val hiding = remember { Levels.all.filter { !p.isDone(it.index) }.take(3) }
    val t = rememberInfiniteTransition(label = "exit")
    val wave by t.animateFloat(-8f, 8f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "wave")
    val fall by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3400, easing = LinearEasing)), label = "fall")
    val stars = remember { val r = Random(9); List(14) { Triple(r.nextFloat(), r.nextFloat(), 14f + (it % 3) * 6f) } }

    BoxWithConstraints(Modifier.fillMaxSize().background(C.Night).clickable(remember { MutableInteractionSource() }, indication = null) {}) {
        val h = maxHeight
        // falling stars
        stars.forEach { (x, phase, sz) ->
            val y = ((fall + phase) % 1f)
            StarIcon(sz.dp, modifier = Modifier.offset(x = maxWidth * x, y = (h + 80.dp) * y - 40.dp).graphicsLayer { rotationZ = y * 360f; alpha = 0.8f })
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 24.dp).heightIn(min = h - 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            ZipView(
                130.dp,
                Modifier.graphicsLayer {
                    if (leaving) {
                        translationX = size.width * 2.2f * fly.value
                        translationY = -size.height * 4f * fly.value
                        rotationZ = 25f * fly.value
                        alpha = 1 - fly.value
                    } else rotationZ = wave
                },
                gold = p.goldenZip(), sad = !leaving,
            )
            BasicText(if (leaving) "Bye bye!" else "Leaving so soon?", style = display(44.sp, C.Sun).copy(textAlign = TextAlign.Center))
            if (leaving) BasicText("See you soon!", style = body(18.sp, C.Lilac, FontWeight.Black))
            else Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText("Zip will miss you! You have ${p.totalStars()} ", style = body(18.sp, C.Lilac, FontWeight.Black))
                StarIcon(20.dp)
            }
            if (!leaving && hiding.isNotEmpty()) {
                Column(
                    Modifier.widthIn(max = 340.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(C.Night2).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BasicText("Still hiding in the dots…", style = body(15.sp, C.Muted, FontWeight.Black))
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        hiding.forEach {
                            Box(contentAlignment = Alignment.Center) {
                                Silhouette(it.picture, 54.dp, C.Night3)
                                BasicText("?", style = display(26.sp, C.Sun))
                            }
                        }
                    }
                }
            }
            if (!leaving) {
                ChunkyButton(
                    { game.audio.pop(); state.close(overlay) },
                    C.Mint, C.MintDark, Modifier.padding(top = 8.dp).widthIn(max = 320.dp).fillMaxWidth(), height = 74.dp, label = "Keep playing",
                ) {
                    Icon24(Ic.Play, 26.dp, C.Ink, stroke = 2f)
                    BasicText("Keep Playing", style = display(28.sp, C.Ink))
                }
                BasicText(
                    "Exit",
                    Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(50)).clickable(role = Role.Button) {
                        leaving = true
                        state.leaving = true
                        game.audio.bye()
                        game.say("Bye bye! See you soon!")
                        scope.launch {
                            delay(300)
                            fly.animateTo(1f, tween(1100))
                            game.audio.music(null)
                            game.exitApp()
                        }
                    }.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = body(18.sp, C.Muted, FontWeight.Black).copy(textDecoration = TextDecoration.Underline),
                )
            }
        }
    }
}

// --- Color it! --------------------------------------------------------------------------
private val PALETTE = listOf("#FF5D73", "#FF8A3D", "#FFC93C", "#8BD346", "#3EE0B0", "#5CC8FF", "#2F7BFF", "#9B6BFF", "#FFB3C7", "#A0663A", "#F4F6FF", "#3B4A7A")

@Composable
private fun ColorCard(state: AppState, overlay: Overlay.ColorIt) {
    val game = state.game
    val pic = overlay.picture
    var chosen by remember { mutableStateOf(game.progress.colorOf(pic)) }
    val swatches = remember { listOf(pic.fill) + PALETTE.filter { it != pic.fill } }
    Card {
        BasicText("Color it!", style = display(32.sp, C.Sun))
        Box(Modifier.clip(RoundedCornerShape(26.dp)).background(C.Night).padding(6.dp)) { PictureIcon(pic, 220.dp, hex(chosen)) }
        swatches.chunked(6).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { c ->
                    val sel = c == chosen
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                            .border(if (sel) 4.dp else 3.dp, if (sel) C.White else C.Ink, CircleShape)
                            .background(hex(c))
                            .clickable(role = Role.Button) {
                                chosen = c
                                game.audio.dot(Random.nextInt(10))
                            }
                            .semantics { contentDescription = "Colour $c" },
                    )
                }
            }
        }
        ChunkyButton(
            {
                val first = game.progress.setColor(pic, chosen)
                if (first) {
                    game.audio.fanfare()
                    game.say("Beautiful! Here is a bonus star!")
                } else game.audio.pop()
                overlay.onPick(chosen)
                state.close(overlay)
            },
            C.Sun, C.SunDark, Modifier.widthIn(min = 160.dp), label = "Done",
        ) {
            Icon24(Ic.Check, 24.dp, C.Ink, stroke = 3.2f)
            BasicText("Done", style = display(24.sp, C.Ink))
        }
    }
}

// --- Sticker close-up ------------------------------------------------------------------
@Composable
private fun StickerCard(state: AppState, overlay: Overlay.Sticker) {
    val game = state.game
    val pic: Picture = overlay.picture
    var plays by remember { mutableIntStateOf(1) }
    val wiggle = remember { Animatable(0f) }
    LaunchedEffect(plays) {
        game.audio.voice(pic.voice)
        launch {
            wiggle.snapTo(0f)
            wiggle.animateTo(1f, tween(1400, easing = LinearEasing))
        }
        delay(700)
        game.say("It's ${article(pic.name)} ${pic.name}!")
    }
    Card {
        Box(
            Modifier.clip(CircleShape).background(C.Paper).padding(14.dp).graphicsLayer {
                val w = wiggle.value
                rotationZ = kotlin.math.sin(w * 12f) * 6f * (1 - w)
            },
        ) { PictureIcon(pic, 230.dp, hex(game.progress.colorOf(pic))) }
        BasicText(pic.name, style = display(32.sp, C.Sun))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundButton({ plays++ }, "Hear it", size = 64.dp) { Icon24(Ic.Speak, 28.dp, C.White) }
            RoundButton({ state.open(Overlay.ColorIt(pic) {}) }, "Color it in", size = 64.dp, color = C.Coral, base = C.CoralDark) {
                Icon24(Ic.Paint, 28.dp, C.White)
            }
            ChunkyButton({ state.close(overlay) }, C.Sun, C.SunDark, Modifier.widthIn(min = 96.dp), label = "OK") {
                BasicText("OK", style = display(24.sp, C.Ink))
            }
        }
    }
}
