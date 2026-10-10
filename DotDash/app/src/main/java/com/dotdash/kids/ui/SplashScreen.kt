package com.dotdash.kids.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TRAIL = "M24 150 C 70 196, 104 86, 160 112 S 246 166, 296 70"
private val DOT_AT = floatArrayOf(0f, 0.24f, 0.5f, 0.74f, 1f)

/**
 * Animated splash: five numbered dots pop in, Zip flies through them drawing a
 * glowing line (each dot chimes), then the logo letters drop in and bounce.
 */
@Composable
fun SplashScreen(state: AppState) {
    val game = state.game
    val draw = remember { Animatable(0f) }
    val dotPop = remember { List(5) { Animatable(0f) } }
    var lit by remember { mutableIntStateOf(0) }
    val tag = remember { Animatable(0f) }
    var canSkip by remember { mutableStateOf(false) }
    val tm = rememberTextMeasurer()

    LaunchedEffect(Unit) {
        game.audio.music(null)
        dotPop.forEachIndexed { i, a -> launch { delay(150L + i * 100); a.animateTo(1f, spring(0.45f, 500f)) } }
        launch { delay(900); canSkip = true }
        launch { delay(2450); tag.animateTo(1f, tween(500)) }
        delay(750)
        draw.animateTo(1f, tween(1300, easing = FastOutSlowInEasing)) {
            while (lit < DOT_AT.size && value >= DOT_AT[lit] - 0.001f) {
                game.audio.dot(lit * 2)
                lit++
            }
        }
        delay(1850)
        state.go(Screen.Home)
    }

    val trail = remember { Paths.svg(TRAIL) }
    val measure = remember { PathMeasure().apply { setPath(trail, false) } }

    Box(
        Modifier
            .fillMaxSize()
            .background(C.Night)
            .clickable(remember { MutableInteractionSource() }, indication = null) { if (canSkip) state.go(Screen.Home) },
    ) {
        Sky(40, seed = 3)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Canvas(Modifier.fillMaxWidth().widthIn(max = 420.dp).aspectRatio(320f / 210f)) {
                inBox(0f, 0f, 320f, 210f) {
                    val len = measure.length
                    val seg = Path()
                    measure.getSegment(0f, len * draw.value, seg, true)
                    drawPath(seg, C.Sun.copy(alpha = 0.25f), style = Stroke(16f, cap = StrokeCap.Round))
                    drawPath(seg, C.SunLight, style = Stroke(5f, cap = StrokeCap.Round))
                    DOT_AT.forEachIndexed { i, f ->
                        val pos = measure.getPosition(len * f)
                        val s = dotPop[i].value
                        if (s > 0.01f) {
                            drawCircle(if (i < lit) C.Sun else C.White, 12f * s, pos)
                            val layout = tm.measure("${i + 1}", TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = (15f * s).toSp(), color = C.Ink))
                            drawText(layout, topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f))
                        }
                    }
                    if (draw.value > 0f) {
                        val z = measure.getPosition(len * draw.value)
                        withTransform({
                            translate(z.x - 23f, z.y - 34f)
                            scale(46f / 120f, 46f / 120f, Offset.Zero)
                        }) { drawZip(game.progress.goldenZip()) }
                    }
                }
            }
            Logo(104.dp, LogoMotion.Intro)
            BasicText(
                "Connect the dots. Light up the picture!",
                Modifier.padding(top = 18.dp).graphicsLayer { alpha = tag.value; translationY = (1 - tag.value) * 30f },
                style = body(18.sp, C.Lilac, FontWeight.Black).copy(textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(26.dp))
            LoadingDots(Modifier.graphicsLayer { alpha = tag.value })
        }
    }
}

@Composable
private fun LoadingDots(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "load")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(C.Sun, C.Coral, C.Mint).forEachIndexed { i, c ->
            val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(450), RepeatMode.Reverse, StartOffset(i * 150)), label = "d$i")
            Box(Modifier.graphicsLayer { translationY = -12f * density * y }.size(14.dp).clip(CircleShape).background(c))
        }
    }
}
