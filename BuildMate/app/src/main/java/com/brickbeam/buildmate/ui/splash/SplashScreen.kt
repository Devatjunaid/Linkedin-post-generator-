package com.brickbeam.buildmate.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brickbeam.buildmate.ui.graphics.drawLogoMark
import com.brickbeam.buildmate.ui.graphics.easeInOut
import com.brickbeam.buildmate.ui.graphics.easeOutBack
import com.brickbeam.buildmate.ui.graphics.easeOutCubic
import com.brickbeam.buildmate.ui.graphics.phase
import com.brickbeam.buildmate.ui.theme.BmType
import com.brickbeam.buildmate.ui.theme.MaterialColors
import com.brickbeam.buildmate.ui.theme.Palettes
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Animated brand splash, shown right after the (instant) system splash. Same dark ground as
 * the system splash so the hand-off is seamless:
 *  1. blueprint grid lines draw across the screen,
 *  2. the logo builds itself (roof, bricks, beam, tiles, paint drop, trowel),
 *  3. the wordmark rises letter by letter,
 *  4. a spirit-level bubble settles in the centre — "level" means ready.
 */
@Composable
fun SplashScreen(motion: Boolean, ready: Boolean, onFinished: () -> Unit) {
    val p = Palettes.Night
    val total = if (motion) 2800 else 700
    val t = remember { Animatable(0f) }
    val finish by rememberUpdatedState(onFinished)
    val isReady by rememberUpdatedState(ready)
    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(total, easing = LinearEasing))
        while (!isReady) kotlinx.coroutines.delay(50)
        finish()
    }
    val v = t.value

    Box(
        Modifier.fillMaxSize().background(p.bg).semantics { contentDescription = "BuildMate is loading" },
        contentAlignment = Alignment.Center,
    ) {
        // 1. Blueprint grid drawing in.
        Canvas(Modifier.fillMaxSize()) {
            val step = 28.dp.toPx()
            val gp = easeInOut(phase(v, 0f, 0.35f))
            val c = p.accent.copy(alpha = 0.08f)
            var x = step / 2
            var i = 0
            while (x < size.width) {
                val d = phase(gp, (i % 7) * 0.05f, 0.7f)
                drawLine(c, Offset(x, 0f), Offset(x, size.height * d), 1.2f)
                x += step; i++
            }
            var y = step / 2
            i = 0
            while (y < size.height) {
                val d = phase(gp, (i % 9) * 0.04f, 0.7f)
                drawLine(c, Offset(0f, y), Offset(size.width * d, y), 1.2f)
                y += step; i++
            }
            // Soft glow behind the logo.
            drawCircle(
                Brush.radialGradient(listOf(p.accent.copy(alpha = 0.18f * easeOutCubic(phase(v, 0.1f, 0.4f))), Color.Transparent), center, size.minDimension * 0.45f),
                radius = size.minDimension * 0.45f, center = Offset(center.x, center.y - 60.dp.toPx()),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 2. Logo build-up (first 70% of the timeline).
            Canvas(Modifier.size(176.dp)) { drawLogoMark((v / 0.7f).coerceIn(0f, 1f), badge = p.surface2, roof = p.accent) }
            Spacer(Modifier.height(18.dp))
            // 3. Wordmark.
            Row {
                "BuildMate".forEachIndexed { i, ch ->
                    val lp = easeOutBack(phase(v, 0.45f + i * 0.03f, 0.18f))
                    Text(
                        ch.toString(),
                        style = BmType.display.copy(fontSize = 40.sp, lineHeight = 44.sp),
                        color = if (i >= 5) p.accent else p.ink,
                        modifier = Modifier.graphicsLayer {
                            alpha = lp.coerceIn(0f, 1f)
                            translationY = (1f - lp) * 24.dp.toPx()
                        },
                    )
                }
            }
            val tag = easeOutCubic(phase(v, 0.7f, 0.2f))
            Text(
                "MEASURE  ·  CALCULATE  ·  BUILD",
                style = BmType.tiny.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                color = p.mute,
                modifier = Modifier.graphicsLayer { alpha = tag },
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.graphicsLayer { alpha = tag }) {
                listOf(MaterialColors.Brick, MaterialColors.Cement, MaterialColors.Steel, MaterialColors.Tile, MaterialColors.Paint).forEach { c ->
                    Box(Modifier.size(width = 18.dp, height = 6.dp).background(c, androidx.compose.foundation.shape.RoundedCornerShape(3.dp)))
                }
            }
        }

        // 4. Spirit level loader.
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp)) {
            Canvas(Modifier.width(184.dp).height(34.dp)) {
                val w = size.width
                val h = size.height
                drawRoundRect(Color(0xFF2A2E37), Offset.Zero, Size(w, h), CornerRadius(h / 2))
                drawRoundRect(Color(0xFF8BC34A).copy(alpha = 0.25f), Offset(6f, 6f), Size(w - 12f, h - 12f), CornerRadius((h - 12f) / 2))
                drawLine(p.ink.copy(alpha = 0.5f), Offset(w / 2 - 16f, 4f), Offset(w / 2 - 16f, h - 4f), 2f)
                drawLine(p.ink.copy(alpha = 0.5f), Offset(w / 2 + 16f, 4f), Offset(w / 2 + 16f, h - 4f), 2f)
                // Damped oscillation that settles in the centre as loading completes.
                val damp = exp(-4.0 * v).toFloat()
                val swing = sin(v * 5 * 2 * PI).toFloat() * damp
                val bx = w / 2 + swing * (w / 2 - 22f) * (1f - v * 0.2f)
                drawOval(Color(0xFFE6F5C9), Offset(bx - 13f, h / 2 - 8f), Size(26f, 16f))
                drawOval(Color.White.copy(alpha = 0.7f), Offset(bx - 7f, h / 2 - 6f), Size(8f, 4f))
                drawRoundRect(p.line, Offset.Zero, Size(w, h), CornerRadius(h / 2), style = Stroke(2f))
            }
        }
    }
}
