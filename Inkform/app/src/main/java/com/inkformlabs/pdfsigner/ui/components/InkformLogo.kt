package com.inkformlabs.pdfsigner.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.inkformlabs.pdfsigner.ui.theme.Bricolage
import com.inkformlabs.pdfsigner.ui.theme.LocalInk
import kotlin.math.cos
import kotlin.math.sin

private fun seg(t: Float, start: Float, end: Float) = ((t - start) / (end - start)).coerceIn(0f, 1f)
private fun easeOut(x: Float) = 1f - (1f - x) * (1f - x) * (1f - x)
private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val p = x - 1f
    return 1f + c3 * p * p * p + c1 * p * p
}

private val PageWhite = Color(0xFFFFFFFF)
private val LineDark = Color(0xFFD9D6CD)
private val LineLight = Color(0xFFECEAE3)
private val FieldLine = Color(0xFFB9B5AA)
private val SignatureInk = Color(0xFF1F3FBF)
private val PenBody = Color(0xFF17181C)
private val PenCone = Color(0xFFF4F2EC)

private fun signaturePath() = Path().apply {
    moveTo(37f, 84f)
    cubicTo(39f, 77f, 42f, 74f, 44f, 79f)
    cubicTo(45f, 82f, 46f, 86f, 49f, 81f)
    cubicTo(51f, 77f, 53f, 74f, 55f, 79f)
    cubicTo(56f, 83f, 58f, 84f, 61f, 80f)
    cubicTo(63f, 77f, 65f, 76f, 67f, 79f)
    cubicTo(68f, 81f, 70f, 82f, 73f, 78f)
}

private fun frontPagePath() = Path().apply {
    moveTo(36f, 34f); lineTo(67f, 34f); lineTo(78f, 45f); lineTo(78f, 88f)
    cubicTo(78f, 90.2f, 76.2f, 92f, 74f, 92f); lineTo(36f, 92f)
    cubicTo(33.8f, 92f, 32f, 90.2f, 32f, 88f); lineTo(32f, 38f)
    cubicTo(32f, 35.8f, 33.8f, 34f, 36f, 34f); close()
}

private fun foldPath() = Path().apply {
    moveTo(67f, 34f); lineTo(67f, 41f); cubicTo(67f, 43.2f, 68.8f, 45f, 71f, 45f); lineTo(78f, 45f); close()
}

private fun checkPath() = Path().apply { moveTo(78.6f, 90f); lineTo(82.6f, 94f); lineTo(90f, 86.2f) }

private fun sparklePath(r: Float) = Path().apply {
    val k = r * 0.26f
    moveTo(0f, -r); lineTo(k, -k); lineTo(r, 0f); lineTo(k, k)
    lineTo(0f, r); lineTo(-k, k); lineTo(-r, 0f); lineTo(-k, -k); close()
}

/**
 * The Inkform mark, drawn and animated in code (no bitmap): an accent tile, two stacked pages,
 * form lines, a signature written live by a fountain pen, a check badge, orbiting ink dots and sparkles.
 *
 * @param playIntro animate the build-up once (splash, exit dialog); false shows the finished mark.
 * @param idle keep the subtle idle motion (orbit, pen bob, sparkle twinkle) running.
 */
@Composable
fun InkformLogo(
    modifier: Modifier = Modifier,
    playIntro: Boolean = true,
    idle: Boolean = true,
    introMillis: Int = 1900,
    showOrbit: Boolean = true,
) {
    val p = LocalInk.current
    val intro = remember { Animatable(if (playIntro) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (playIntro) intro.animateTo(1f, tween(introMillis, easing = LinearEasing))
    }
    val infinite = rememberInfiniteTransition(label = "logoIdle")
    val orbit by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "orbit",
    )
    val twinkle by infinite.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "twinkle",
    )
    val bob by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob",
    )

    val signature = remember { signaturePath() }
    val measure = remember { PathMeasure().apply { setPath(signature, false) } }
    val partial = remember { Path() }
    val frontPage = remember { frontPagePath() }
    val fold = remember { foldPath() }
    val check = remember { checkPath() }
    val checkMeasure = remember { PathMeasure().apply { setPath(check, false) } }
    val checkPartial = remember { Path() }
    val sparkle = remember { sparklePath(5f) }
    val tile = remember { Path().apply { addRoundRect(RoundRect(14f, 14f, 106f, 106f, CornerRadius(28f, 28f))) } }

    Canvas(modifier.semantics { contentDescription = "Inkform logo" }) {
        val t = intro.value
        val idleAmount = if (idle) seg(t, 0.85f, 1f) else 0f
        val unit = size.minDimension / 120f
        val ox = (size.width - 120f * unit) / 2f
        val oy = (size.height - 120f * unit) / 2f

        translate(ox, oy) {
            scale(unit, unit, pivot = Offset.Zero) {
                // Orbit ring with ink dots
                if (showOrbit) {
                    val ringAlpha = seg(t, 0.55f, 0.85f)
                    if (ringAlpha > 0f) {
                        drawCircle(
                            color = p.ink.copy(alpha = 0.16f * ringAlpha), radius = 57f, center = Offset(60f, 60f),
                            style = Stroke(width = 0.9f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3.5f), orbit / 6f)),
                        )
                        val dots = listOf(Triple(0f, 3.4f, p.accent2), Triple(125f, 2.6f, p.hi), Triple(235f, 2.1f, p.ink))
                        dots.forEach { (offset, r, color) ->
                            val a = Math.toRadians((orbit * idleAmount + offset - 90f).toDouble())
                            drawCircle(color.copy(alpha = ringAlpha), r, Offset(60f + 57f * cos(a).toFloat(), 60f + 57f * sin(a).toFloat()))
                        }
                    }
                }

                // Tile pops in with overshoot
                val tileScale = easeOutBack(seg(t, 0f, 0.3f))
                scale(tileScale, tileScale, pivot = Offset(60f, 60f)) {
                    drawPath(tile, p.hero)
                    clipPath(tile) {
                        val blob = easeOut(seg(t, 0.12f, 0.4f))
                        drawCircle(p.accent2, radius = 28f * blob, center = Offset(100f, 22f))
                        drawCircle(p.onHero.copy(alpha = 0.18f * blob), radius = 18f, center = Offset(30f, 104f), style = Stroke(1.2f))
                    }
                }

                // Back page slides in from the right, tilted
                val back = easeOut(seg(t, 0.16f, 0.44f))
                if (back > 0f) {
                    rotate(10f, pivot = Offset(66f, 57f)) {
                        drawRoundRect(
                            color = PageWhite.copy(alpha = 0.45f * back),
                            topLeft = Offset(45f + (1f - back) * 26f, 30f), size = Size(42f, 54f),
                            cornerRadius = CornerRadius(4f, 4f),
                        )
                    }
                }

                // Front page rises from below
                val front = easeOut(seg(t, 0.24f, 0.5f))
                if (front > 0f) {
                    translate(0f, (1f - front) * 24f) {
                        drawPath(frontPage, PageWhite.copy(alpha = front))
                        drawPath(fold, LineDark.copy(alpha = front))

                        val lines = easeOut(seg(t, 0.4f, 0.62f))
                        if (lines > 0f) {
                            drawLine(LineDark, Offset(38f, 51f), Offset(38f + 20f * lines, 51f), 3.2f, StrokeCap.Round)
                            drawLine(LineLight, Offset(38f, 58f), Offset(38f + 32f * lines, 58f), 2.6f, StrokeCap.Round)
                            drawRect(
                                color = FieldLine.copy(alpha = lines), topLeft = Offset(38f, 64f), size = Size(33f, 8f),
                                style = Stroke(1.1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 2f))),
                            )
                        }

                        // Signature written stroke by stroke, the pen following the tip
                        val write = seg(t, 0.5f, 0.84f)
                        val length = measure.length
                        if (write > 0f) {
                            partial.reset()
                            measure.getSegment(0f, length * write, partial, true)
                            drawPath(partial, SignatureInk, style = Stroke(2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                        val penAlpha = seg(t, 0.46f, 0.54f)
                        if (penAlpha > 0f) {
                            val tip = measure.getPosition(length * write)
                            val lift = if (write >= 1f) -3f * bob * idleAmount - 2f * seg(t, 0.84f, 0.95f) else 0f
                            drawPen(Offset(tip.x + 1f, tip.y + lift), penAlpha)
                        }
                    }
                }

                // Check badge pops last
                val badge = easeOutBack(seg(t, 0.78f, 0.96f))
                if (badge > 0f) {
                    scale(badge, badge, pivot = Offset(84f, 90f)) {
                        drawCircle(PageWhite, 15.6f, Offset(84f, 90f))
                        drawCircle(p.accent, 13f, Offset(84f, 90f))
                        val c = seg(t, 0.86f, 1f)
                        checkPartial.reset()
                        checkMeasure.getSegment(0f, checkMeasure.length * c, checkPartial, true)
                        drawPath(checkPartial, p.onAccent, style = Stroke(2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }

                // Sparkles
                if (idleAmount > 0f) {
                    val spots = listOf(Offset(20f, 24f) to 1f, Offset(103f, 108f) to 0.7f, Offset(8f, 78f) to 0.55f)
                    spots.forEachIndexed { i, (pos, s) ->
                        val phase = if (i % 2 == 0) twinkle else 1.2f - twinkle
                        val k = s * (0.6f + 0.4f * phase) * idleAmount
                        translate(pos.x, pos.y) {
                            scale(k, k, pivot = Offset.Zero) { drawPath(sparkle, p.accent2.copy(alpha = (0.35f + 0.65f * phase).coerceIn(0f, 1f))) }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawPen(tip: Offset, alpha: Float) {
    rotate(-45f, pivot = tip) {
        translate(tip.x, tip.y) {
            val body = Path().apply { moveTo(7f, -3.3f); lineTo(29f, -3.3f); lineTo(29f, 3.3f); lineTo(7f, 3.3f); close() }
            val cap = Path().apply {
                moveTo(29f, -3.3f); lineTo(34f, -3.3f); cubicTo(36.5f, -3.3f, 36.5f, 3.3f, 34f, 3.3f); lineTo(29f, 3.3f); close()
            }
            val cone = Path().apply { moveTo(0f, 0f); lineTo(7f, -3.3f); lineTo(7f, 3.3f); close() }
            val nib = Path().apply { moveTo(0f, 0f); lineTo(3f, -1.4f); lineTo(3f, 1.4f); close() }
            drawPath(body, PageWhite.copy(alpha = alpha), style = Stroke(1.6f, join = StrokeJoin.Round))
            drawPath(body, PenBody.copy(alpha = alpha))
            drawPath(cap, Color(0xFFFF7A45).copy(alpha = alpha))
            drawPath(cone, PenCone.copy(alpha = alpha))
            drawPath(nib, PenBody.copy(alpha = alpha))
        }
    }
}

/** "Inkform" wordmark whose letters drop in one after another. */
@Composable
fun InkformWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 40.sp,
    animate: Boolean = true,
    delayMillis: Int = 0,
    color: Color = LocalInk.current.ink,
) {
    var visible by remember { mutableStateOf(!animate) }
    LaunchedEffect(Unit) { visible = true }
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "Inkform" }) {
        "Inkform".forEachIndexed { i, ch ->
            val progress by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(durationMillis = 520, delayMillis = delayMillis + i * 70, easing = FastOutSlowInEasing),
                label = "letter$i",
            )
            Text(
                text = ch.toString(),
                style = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, letterSpacing = (-0.5).sp),
                color = if (i < 3) color else LocalInk.current.hi,
                modifier = Modifier.graphicsLayer {
                    alpha = progress
                    translationY = (1f - progress) * 28f * density
                    rotationZ = (1f - progress) * -12f
                },
            )
        }
    }
}
