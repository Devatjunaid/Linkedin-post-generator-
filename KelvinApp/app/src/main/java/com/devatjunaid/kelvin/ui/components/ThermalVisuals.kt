package com.devatjunaid.kelvin.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devatjunaid.kelvin.domain.Phase
import com.devatjunaid.kelvin.domain.PhaseType
import com.devatjunaid.kelvin.ui.theme.K
import com.devatjunaid.kelvin.ui.theme.KType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Slowly drifting cold and warm light behind a screen. [warmth] 0 = all ice, 1 = all ember. */
@Composable
fun AmbientBackground(modifier: Modifier = Modifier, warmth: Float = 0.5f) {
    val loop = rememberInfiniteTransition(label = "ambient")
    val t by loop.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(18_000, easing = LinearEasing)), label = "t")
    val w by animateFloatAsState(warmth, tween(1200), label = "warmth")
    Canvas(modifier.fillMaxSize()) {
        val iceC = Offset(size.width * (0.15f + 0.08f * cos(t)), size.height * (0.12f + 0.05f * sin(t)))
        val heatC = Offset(size.width * (0.9f + 0.06f * sin(t * 0.8f)), size.height * (0.35f + 0.06f * cos(t * 0.8f)))
        val r = size.width * 0.9f
        drawCircle(Brush.radialGradient(listOf(K.Ice.copy(alpha = 0.18f * (1.2f - w)), Color.Transparent), iceC, r), r, iceC)
        drawCircle(Brush.radialGradient(listOf(K.Ember.copy(alpha = 0.16f * (0.2f + w)), Color.Transparent), heatC, r), r, heatC)
    }
}

/** Home hero: rotating thermal ring with orbiting particles and the streak in the middle. */
@Composable
fun ThermalOrb(
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val loop = rememberInfiniteTransition(label = "orb")
    val spin by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "spin")
    val orbit by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(9_000, easing = LinearEasing)), label = "orbit")
    val pulse by loop.animateFloat(0.45f, 0.8f, infiniteRepeatable(tween(4_000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = this.size.minDimension / 2f
            val ringW = r * 0.045f
            drawCircle(Brush.sweepGradient(K.thermalSweep.map { it.copy(alpha = 0.55f * pulse) }, c), r * 1.02f, c, alpha = 0.35f)
            rotate(spin, c) {
                drawArc(
                    Brush.sweepGradient(K.thermalSweep, c),
                    startAngle = -90f,
                    sweepAngle = 360f * intro.value,
                    useCenter = false,
                    topLeft = Offset(c.x - r + ringW, c.y - r + ringW),
                    size = Size((r - ringW) * 2, (r - ringW) * 2),
                    style = Stroke(ringW * 2, cap = StrokeCap.Round),
                )
            }
            drawCircle(Brush.radialGradient(listOf(Color(0xFF18212C), K.Ink), c + Offset(0f, -r * 0.3f), r), r - ringW * 2.2f, c)
            repeat(60) { i ->
                val a = i * 6f * (PI / 180f).toFloat()
                val dir = Offset(cos(a), sin(a))
                val outer = r * 0.84f
                drawLine(Color.White.copy(alpha = 0.16f), c + dir * outer, c + dir * (outer - r * 0.035f), r * 0.012f, StrokeCap.Round)
            }
            val aIce = (orbit - 90f) * (PI / 180f).toFloat()
            val aHeat = (-orbit * 1.4f + 90f) * (PI / 180f).toFloat()
            val ice = c + Offset(cos(aIce), sin(aIce)) * (r * 1.04f)
            val heat = c + Offset(cos(aHeat), sin(aHeat)) * (r * 1.04f)
            drawCircle(Brush.radialGradient(listOf(K.Ice.copy(alpha = 0.7f), Color.Transparent), ice, r * 0.12f), r * 0.12f, ice)
            drawCircle(K.Ice, r * 0.045f, ice)
            drawCircle(Brush.radialGradient(listOf(K.Ember.copy(alpha = 0.7f), Color.Transparent), heat, r * 0.12f), r * 0.12f, heat)
            drawCircle(K.Ember, r * 0.04f, heat)
        }
        content()
    }
}

/**
 * Animated water: two phase-shifted waves at [level] (0..1 of the height) with bubbles rising.
 */
@Composable
fun WaterFill(level: Float, modifier: Modifier = Modifier, bubbles: Int = 9) {
    val loop = rememberInfiniteTransition(label = "water")
    val phase by loop.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(4_000, easing = LinearEasing)), label = "phase")
    val rise by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "rise")
    val animatedLevel by animateFloatAsState(level.coerceIn(0f, 1f), tween(1000, easing = LinearEasing), label = "level")
    Canvas(modifier.fillMaxSize()) {
        val top = size.height * (1f - animatedLevel)
        fun wavePath(amplitude: Float, wavelength: Float, shift: Float): Path = Path().apply {
            moveTo(0f, size.height)
            lineTo(0f, top)
            var x = 0f
            while (x <= size.width + 8f) {
                lineTo(x, top + sin(x / wavelength + shift) * amplitude)
                x += 8f
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(wavePath(14f, size.width / 5f, phase * 0.6f + 1.5f), Brush.verticalGradient(listOf(K.IceDeep.copy(alpha = 0.28f), Color(0xCC08203C)), top, size.height))
        drawPath(wavePath(9f, size.width / 3.2f, phase), Brush.verticalGradient(listOf(Color(0x5550AAFF), Color(0xE0061A30)), top, size.height))
        repeat(bubbles) { i ->
            val seed = (i * 0.618f) % 1f
            val p = (rise + seed) % 1f
            val x = size.width * (0.08f + 0.84f * ((i * 0.37f) % 1f)) + sin(p * 6f + i) * 10f
            val travel = (size.height - top).coerceAtLeast(1f)
            val y = size.height - p * travel
            val radius = 3f + (i % 4) * 2.2f
            if (y > top + 6f) {
                drawCircle(Color(0xB3BEF0FF), radius, Offset(x, y), alpha = (1f - p).coerceIn(0f, 1f), style = Stroke(1.6f))
            }
        }
    }
}

/** Sauna visual: a warm glow rising from the bottom with drifting heat shimmer and sparks. */
@Composable
fun HeatFill(level: Float, modifier: Modifier = Modifier) {
    val loop = rememberInfiniteTransition(label = "heat")
    val rise by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(3_800, easing = LinearEasing)), label = "rise")
    val shimmer by loop.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(3_000, easing = LinearEasing)), label = "shimmer")
    val animatedLevel by animateFloatAsState(level.coerceIn(0f, 1f), tween(1000), label = "level")
    Canvas(modifier.fillMaxSize()) {
        val top = size.height * (1f - animatedLevel)
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, K.EmberDeep.copy(alpha = 0.22f), Color(0xCC3A1208)), top - 120f, size.height))
        repeat(5) { i ->
            val x = size.width * (0.15f + i * 0.18f)
            val p = Path().apply {
                moveTo(x, size.height)
                var y = size.height
                while (y > top) {
                    lineTo(x + sin(y / 40f + shimmer + i) * 10f, y)
                    y -= 12f
                }
            }
            drawPath(p, Color(0x26FFD9C4), style = Stroke(6f, cap = StrokeCap.Round))
        }
        repeat(14) { i ->
            val p = (rise + (i * 0.61f) % 1f) % 1f
            val x = size.width * ((i * 0.43f) % 1f) + sin(p * 8f + i) * 14f
            val y = size.height - p * (size.height - top + 80f)
            drawCircle(if (i % 3 == 0) K.Gold else K.Ember, 2f + (i % 3), Offset(x, y), alpha = (1f - p) * 0.9f)
        }
    }
}

/** Neutral recovery visual for rest phases: slow concentric pulses. */
@Composable
fun RestFill(modifier: Modifier = Modifier) {
    val loop = rememberInfiniteTransition(label = "rest")
    val p by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(5_000, easing = LinearEasing)), label = "p")
    Canvas(modifier.fillMaxSize()) {
        repeat(3) { i ->
            val local = (p + i / 3f) % 1f
            drawCircle(Color.White.copy(alpha = 0.08f * (1f - local)), size.minDimension * (0.2f + local * 0.6f), Offset(size.width / 2f, size.height * 0.62f), style = Stroke(2f))
        }
    }
}

/**
 * Horizontal timeline of a protocol: one block per phase, width proportional to duration,
 * ice / ember / rest colours, with a staggered grow-in.
 */
@Composable
fun ThermalRibbon(
    phases: List<Phase>,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    showLabels: Boolean = true,
) {
    val intro = remember(phases) { Animatable(0f) }
    LaunchedEffect(phases) { intro.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        phases.forEachIndexed { i, phase ->
            val local = ((intro.value * phases.size) - i * 0.6f).coerceIn(0f, 1f)
            val brush = when (phase.type) {
                PhaseType.HEAT -> Brush.verticalGradient(listOf(Color(0xFFFF9A5C), K.EmberDeep))
                PhaseType.COLD -> Brush.verticalGradient(listOf(K.IceLight, K.IceDeep))
                PhaseType.REST -> Brush.verticalGradient(listOf(Color(0x24FFFFFF), Color(0x14FFFFFF)))
            }
            val textColor = when (phase.type) {
                PhaseType.HEAT -> Color(0xFF2A0E08)
                PhaseType.COLD -> Color(0xFF06223A)
                PhaseType.REST -> Color(0xFFB6C2CE)
            }
            Box(
                Modifier
                    .weight(phase.durationSec.coerceAtLeast(45).toFloat())
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleY = local
                        alpha = local
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(if (height > 20.dp) 10.dp else 5.dp))
                    .background(brush),
                contentAlignment = Alignment.BottomCenter,
            ) {
                if (showLabels && height > 30.dp) {
                    val minutes = phase.durationSec / 60f
                    val label = if (minutes >= 1f && phase.durationSec % 60 == 0) "${minutes.toInt()}" else "${phase.durationSec}s"
                    Text(
                        label,
                        style = KType.Label.copy(color = textColor, fontSize = 10.sp, letterSpacing = 0.sp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Legend row for ribbons. */
@Composable
fun RibbonLegend(heat: String, cold: String, rest: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        LegendItem(K.Ember, "Heat", heat)
        LegendItem(K.Ice, "Cold", cold)
        LegendItem(K.Rest, "Rest", rest)
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
            Text(label, style = KType.Small)
        }
        Text(value, style = KType.Mono.copy(color = if (color == K.Rest) K.Text else color, fontSize = 16.sp))
    }
}
