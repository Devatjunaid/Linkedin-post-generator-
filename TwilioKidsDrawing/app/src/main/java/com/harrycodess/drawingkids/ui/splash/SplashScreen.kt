package com.harrycodess.drawingkids.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.harrycodess.drawingkids.art.inkStroke
import com.harrycodess.drawingkids.drawing.Tool
import com.harrycodess.drawingkids.ui.components.ArtThumb
import com.harrycodess.drawingkids.ui.components.Showcase
import com.harrycodess.drawingkids.ui.components.TwilioLogo
import com.harrycodess.drawingkids.ui.components.drawToolIcon
import com.harrycodess.drawingkids.ui.theme.Kid
import com.harrycodess.drawingkids.ui.theme.kidText
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private const val DURATION = 2800

/** Overshooting "pop" curve: 0 -> 1 with a little bounce past 1. */
private fun pop(x: Float): Float {
    if (x <= 0f) return 0f
    if (x >= 1f) return 1f
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}

private fun window(t: Float, start: Float, length: Float) = ((t - start) / length).coerceIn(0f, 1f)

/**
 * Animated splash: a crayon zooms across the screen drawing a rainbow, the logo pops in letter
 * by letter and animal friends bounce into the corners.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        clock.animateTo(1f, tween(DURATION, easing = LinearEasing))
        onFinished()
    }
    val t = clock.value

    BoxWithConstraints(Modifier.fillMaxSize().background(Kid.Cream)) {
        val small = min(maxWidth.value, maxHeight.value)
        val critter = (small * 0.26f).dp

        Canvas(Modifier.fillMaxSize()) {
            rainbow(progress = window(t, 0.02f, 0.5f), yFraction = 0.80f, flip = false)
            rainbow(progress = window(t, 0.18f, 0.5f), yFraction = 0.17f, flip = true)
        }

        Friend("cat", critter, Alignment.TopStart, pop(window(t, 0.30f, 0.18f)), -10f)
        Friend("fish", critter, Alignment.TopEnd, pop(window(t, 0.38f, 0.18f)), 8f)
        Friend("turtle", critter, Alignment.BottomStart, pop(window(t, 0.46f, 0.18f)), -6f)
        Friend("butterfly", critter, Alignment.BottomEnd, pop(window(t, 0.54f, 0.18f)), 10f + 8f * sin(t * 20f))

        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val emblemPop = pop(window(t, 0.05f, 0.22f))
            Box(
                Modifier.graphicsLayer {
                    scaleX = emblemPop
                    scaleY = emblemPop
                    rotationZ = (1f - emblemPop) * -90f
                },
            ) {
                com.harrycodess.drawingkids.ui.components.Emblem(Modifier.size((small * 0.24f).dp))
            }
            TwilioLogo(letterSize = (small * 0.15f).toInt().coerceIn(44, 96), reveal = window(t, 0.2f, 0.45f))
            val tagline = window(t, 0.62f, 0.15f)
            Text(
                "Let's draw & color!",
                style = kidText(20, color = Kid.Muted),
                modifier = Modifier.graphicsLayer { alpha = tagline },
            )
            LoadingDots(t)
        }
    }
}

@Composable
private fun Friend(id: String, size: Dp, align: Alignment, scale: Float, angle: Float) {
    Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = align) {
        ArtThumb(
            Showcase.art(id), Showcase.fills(id),
            Modifier.size(size).graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = angle
            },
        )
    }
}

@Composable
private fun LoadingDots(t: Float) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(Kid.Pink, Kid.Yellow, Kid.Blue).forEachIndexed { i, c ->
            val phase = sin((t * 18f) - i * 0.9f)
            Box(
                Modifier
                    .offset(y = (phase * -6f).dp)
                    .size(14.dp)
                    .background(c, CircleShape),
            )
        }
    }
}

/** A wavy rainbow being drawn by a crayon. */
private fun DrawScope.rainbow(progress: Float, yFraction: Float, flip: Boolean) {
    if (progress <= 0f) return
    val w = size.width
    val y = size.height * yFraction
    val amp = size.height * 0.05f
    val path = Path().apply {
        if (!flip) {
            moveTo(-40f, y)
            cubicTo(w * 0.2f, y - amp * 2, w * 0.3f, y + amp * 2, w * 0.5f, y)
            cubicTo(w * 0.7f, y - amp * 2, w * 0.8f, y + amp * 2, w + 40f, y)
        } else {
            moveTo(w + 40f, y)
            cubicTo(w * 0.8f, y + amp * 2, w * 0.7f, y - amp * 2, w * 0.5f, y)
            cubicTo(w * 0.3f, y + amp * 2, w * 0.2f, y - amp * 2, -40f, y)
        }
    }
    val measure = PathMeasure().apply { setPath(path, false) }
    val len = measure.length * progress
    val segment = Path()
    measure.getSegment(0f, len, segment, true)
    val band = 9.dp.toPx()
    Kid.Rainbow.forEachIndexed { i, c ->
        translate(top = (i - 2.5f) * band * 0.95f) {
            drawPath(segment, c.copy(alpha = 0.55f), style = inkStroke(band))
        }
    }
    if (progress < 1f) {
        val pos: Offset = measure.getPosition(len)
        val tan: Offset = measure.getTangent(len)
        val deg = Math.toDegrees(atan2(tan.y, tan.x).toDouble()).toFloat()
        val s = 64.dp.toPx()
        withTransform({
            translate(pos.x, pos.y)
            rotate(deg + 190f, Offset.Zero)
            translate(-s * 0.1875f, -s * 0.8125f)
            scale(s / 64f, s / 64f, Offset.Zero)
        }) {
            drawToolIcon(Tool.CRAYON, Kid.Pink)
        }
    }
}
