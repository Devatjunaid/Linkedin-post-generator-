package com.drawtocompletefun.kids.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drawtocompletefun.kids.game.Pictures
import com.drawtocompletefun.kids.ui.components.Clouds
import com.drawtocompletefun.kids.ui.components.KText
import com.drawtocompletefun.kids.ui.components.board
import com.drawtocompletefun.kids.ui.components.drawDashedShape
import com.drawtocompletefun.kids.ui.components.drawPipAt
import com.drawtocompletefun.kids.ui.components.drawShape
import com.drawtocompletefun.kids.ui.components.drawSparkles
import com.drawtocompletefun.kids.ui.components.drawStarShape
import com.drawtocompletefun.kids.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val SPLASH_MS = 2900

/** Animated splash: the logo draws itself, Pip finishes the missing roof, stars pop. */
@Composable
fun SplashScreen(ready: Boolean, onFinished: () -> Unit) {
    val t = remember { Animatable(0f) }
    val isReady by rememberUpdatedState(ready)
    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(SPLASH_MS, easing = LinearEasing))
        // Wait a little for consent / ads start-up, but never keep kids waiting long.
        withTimeoutOrNull(3500) { snapshotFlow { isReady }.first { it } }
        delay(150)
        onFinished()
    }
    val p = t.value

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Sky),
        contentAlignment = Alignment.Center,
    ) {
        Clouds()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoCard(p)
            Box(Modifier.padding(top = 28.dp)) { SplashTitle(p) }
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) { LoadingDots() }
    }
}

private fun seg(p: Float, from: Float, to: Float): Float = ((p - from) / (to - from)).coerceIn(0f, 1f)

private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val y = x - 1f
    return 1f + c3 * y * y * y + c1 * y * y
}

@Composable
private fun LogoCard(p: Float) {
    val house = Pictures.house
    val roof = house.shape("roofR")!!
    val pop = easeOutBack(seg(p, 0f, 0.18f))
    Box(
        Modifier
            .size(250.dp)
            .graphicsLayer {
                scaleX = pop
                scaleY = pop
                rotationZ = (1f - seg(p, 0f, 0.25f)) * -8f
            },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Palette.SkyShadow),
        )
        Box(
            Modifier
                .matchParentSize()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Palette.Paper),
        )
        Canvas(
            Modifier
                .matchParentSize()
                .padding(start = 26.dp, end = 26.dp, top = 20.dp, bottom = 32.dp),
        ) {
            val drawIn = seg(p, 0.12f, 0.38f)
            val others = house.shapes.filter { it.tag != "roofR" }
            val w = size.minDimension * 0.026f
            others.forEach { drawShape(it, Palette.Ink, w, drawIn) }
            val roofP = seg(p, 0.42f, 0.66f)
            if (drawIn >= 1f && roofP < 1f) drawDashedShape(roof, Palette.Ink.copy(alpha = 0.3f), w * 0.8f)
            if (roofP > 0f) drawShape(roof, Palette.Coral, w * 1.3f, roofP)
            // Pip rides along the roof line while drawing it.
            val a = board(roof.points.first())
            val b = board(roof.points.last())
            val tip = Offset(a.x + (b.x - a.x) * roofP, a.y + (b.y - a.y) * roofP)
            val pipIn = seg(p, 0.34f, 0.42f)
            val pipOut = seg(p, 0.7f, 0.8f)
            if (pipIn > 0f && pipOut < 1f) {
                val lift = (1f - pipIn) * size.height * 0.25f + pipOut * size.height * 0.3f
                rotate(28f, pivot = tip) {
                    drawPipAt(Offset(tip.x, tip.y - lift), size.height * 0.62f, happy = true)
                }
            }
            drawSparkles(Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f), seg(p, 0.66f, 0.86f), Palette.Yellow)
        }
        // Three reward stars pop around the card.
        Canvas(Modifier.matchParentSize()) {
            val starsIn = listOf(0.70f, 0.76f, 0.82f).map { easeOutBack(seg(p, it, it + 0.12f)) }
            val r = size.minDimension
            val spots = listOf(Offset(r * 0.02f, r * 0.08f), Offset(r * 0.98f, r * 0.04f), Offset(r * 0.95f, r * 0.9f))
            spots.forEachIndexed { i, o ->
                if (starsIn[i] > 0f) drawStarShape(o, r * 0.085f * starsIn[i], Palette.Yellow, Palette.YellowShadow)
            }
        }
    }
}

@Composable
private fun SplashTitle(p: Float) {
    val words = listOf("Draw", "to", "Complete")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            words.forEachIndexed { i, w ->
                val start = 0.36f + i * 0.08f
                val k = easeOutBack(seg(p, start, start + 0.16f))
                KText(
                    w,
                    size = 38.sp,
                    weight = FontWeight.Black,
                    color = if (i == 2) Palette.Purple else Palette.Ink,
                    modifier = Modifier.graphicsLayer {
                        alpha = seg(p, start, start + 0.05f)
                        translationY = (1f - k) * 60f * density
                    },
                )
            }
        }
        val f = easeOutBack(seg(p, 0.68f, 0.86f))
        Box(
            Modifier
                .padding(top = 10.dp)
                .graphicsLayer {
                    scaleX = f
                    scaleY = f
                    rotationZ = -6f
                }
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.Yellow)
                .padding(horizontal = 26.dp, vertical = 4.dp),
        ) {
            KText("FUN!", size = 34.sp, weight = FontWeight.Black, align = TextAlign.Center)
        }
    }
}

@Composable
private fun LoadingDots() {
    val tr = rememberInfiniteTransition(label = "dots")
    repeat(3) { i ->
        val y by tr.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(500, delayMillis = i * 120, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "dot$i",
        )
        Box(
            Modifier
                .size(14.dp)
                .graphicsLayer { translationY = -y * 10f * density }
                .clip(CircleShape)
                .background(listOf(Palette.Coral, Palette.Yellow, Palette.Purple)[i]),
        )
    }
}
