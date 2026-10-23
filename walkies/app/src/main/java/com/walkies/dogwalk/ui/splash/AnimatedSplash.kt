package com.walkies.dogwalk.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walkies.dogwalk.R
import com.walkies.dogwalk.ui.theme.WalkiesColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated splash. It opens on the static logo, sized and centred like the system launch icon so
 * the hand-over is seamless, then the person and dog walk while the path scrolls under them,
 * the title rises in, and they settle back into the logo pose.
 *
 * @param ready true once start-up work (the consent check) has finished.
 */
@Composable
fun AnimatedSplash(ready: Boolean, onFinished: () -> Unit) {
    val stride = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    var distance by remember { mutableFloatStateOf(0f) }
    var animationDone by remember { mutableStateOf(false) }
    val finish by rememberUpdatedState(onFinished)

    // One clock drives both the stride and the scrolling path, so feet and ground stay in step.
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) distance += (now - last) / 1_000_000_000f / CYCLE_SECONDS * stride.value
                last = now
            }
        }
    }
    LaunchedEffect(Unit) {
        delay(150)
        launch { stride.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        launch { lift.animateTo(1f, tween(900, delayMillis = 200, easing = FastOutSlowInEasing)) }
        title.animateTo(1f, tween(650, delayMillis = 700, easing = LinearOutSlowInEasing))
        delay(500)
        stride.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
        delay(250)
        animationDone = true
    }
    LaunchedEffect(animationDone, ready) {
        if (animationDone && ready) finish()
    }
    // Never hold the user on the splash for a slow network: the consent form, if one is needed,
    // still appears over the app when it is ready.
    LaunchedEffect(animationDone) {
        if (animationDone) {
            delay(MAX_EXTRA_WAIT_MS)
            finish()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(WalkiesColors.Forest),
    ) {
        Canvas(
            Modifier
                .align(Alignment.Center)
                .size(LOGO_BOX)
                .graphicsLayer {
                    val scale = 1f + 0.35f * lift.value
                    scaleX = scale
                    scaleY = scale
                    translationY = -56.dp.toPx() * lift.value
                },
        ) {
            val unit = size.width / 110f
            // Logo content is centred on (57, 50.5) in its 100-unit box, as in the launcher icon.
            val origin = Offset(size.width / 2f - 57f * unit, size.height / 2f - 50.5f * unit)
            val groundY = origin.y + 92.5f * unit
            val dash = 6f * unit
            drawLine(
                color = WalkiesColors.Cream.copy(alpha = 0.28f * stride.value.coerceAtLeast(lift.value)),
                start = Offset(-size.width, groundY),
                end = Offset(size.width * 2f, groundY),
                strokeWidth = 1.6f * unit,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(dash, dash * 2f),
                    phase = distance * STEP_LENGTH_UNITS * unit,
                ),
            )
            drawWalkers(
                phase = distance % 1f,
                stride = stride.value,
                figure = WalkiesColors.Cream,
                leash = WalkiesColors.Walk,
                origin = origin,
                unit = unit,
            )
        }
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(top = 220.dp)
                .offset(y = (18 * (1f - title.value)).dp)
                .graphicsLayer { alpha = title.value },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.app_name),
                color = WalkiesColors.Cream,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            Text(
                stringResource(R.string.splash_tagline),
                color = WalkiesColors.Cream.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 110 logo units ≈ the system splash icon (288dp canvas, logo scaled to 55%). */
private val LOGO_BOX = 161.dp
private const val CYCLE_SECONDS = 0.76f
private const val STEP_LENGTH_UNITS = 30f
private const val MAX_EXTRA_WAIT_MS = 2_500L
