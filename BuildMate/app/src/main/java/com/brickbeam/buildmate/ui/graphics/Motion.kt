package com.brickbeam.buildmate.ui.graphics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import com.brickbeam.buildmate.ui.theme.LocalMotion
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * A 0→1 looping clock read inside draw lambdas (so only the Canvas redraws, not the whole
 * composition). When animations are turned off it is frozen at [stillAt].
 */
@Composable
fun rememberLoop(durationMs: Int, stillAt: Float = 0.6f): State<Float> {
    val motion = LocalMotion.current
    val frozen = remember(stillAt) { mutableFloatStateOf(stillAt) }
    val transition = rememberInfiniteTransition(label = "loop")
    val running = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "loopValue",
    )
    return if (motion) running else frozen
}

/** Progress of a sub-animation that starts at [start] and lasts [length] (both in 0..1). */
fun phase(t: Float, start: Float, length: Float): Float = ((t - start) / length).coerceIn(0f, 1f)

fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}

fun easeOutCubic(x: Float): Float = 1f - (1f - x).pow(3)
fun easeInOut(x: Float): Float = if (x < 0.5f) 4 * x * x * x else 1 - (-2 * x + 2).pow(3) / 2

fun wave(t: Float, cycles: Float = 1f): Float = sin(t * 2 * PI.toFloat() * cycles)
