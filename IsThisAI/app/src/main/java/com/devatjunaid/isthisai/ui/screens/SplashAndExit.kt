package com.devatjunaid.isthisai.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devatjunaid.isthisai.ads.NativeAdCard
import com.devatjunaid.isthisai.ads.NativeStyle
import com.devatjunaid.isthisai.ui.components.MonoText
import com.devatjunaid.isthisai.ui.components.PrimaryButton
import com.devatjunaid.isthisai.ui.components.SecondaryButton
import com.devatjunaid.isthisai.ui.components.rememberMonoTypeface
import com.devatjunaid.isthisai.ui.logo.LensMark
import com.devatjunaid.isthisai.ui.logo.Wordmark
import com.devatjunaid.isthisai.ui.theme.AiOrange
import com.devatjunaid.isthisai.ui.theme.CameraBlue
import com.devatjunaid.isthisai.ui.theme.Ink
import com.devatjunaid.isthisai.ui.theme.Lime
import com.devatjunaid.isthisai.ui.theme.Line
import com.devatjunaid.isthisai.ui.theme.Muted
import com.devatjunaid.isthisai.ui.theme.Paper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Animated splash (about 2.6 s): the lens mark assembles piece by piece over a drifting "byte
 * rain", the wordmark letters rise in, the label formats light up one by one, a calibration
 * bar fills, and finally the lens zooms past the camera to reveal the home screen.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val assemble = remember { Animatable(0f) }
    val words = remember { Animatable(0f) }
    val chips = remember { Animatable(0f) }
    val bar = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { assemble.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
        launch { delay(650); words.animateTo(1f, tween(800, easing = LinearEasing)) }
        launch { delay(1150); chips.animateTo(1f, tween(700, easing = LinearEasing)) }
        bar.animateTo(1f, tween(2200, easing = FastOutSlowInEasing))
        delay(150)
        exit.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .graphicsLayer { alpha = 1f - exit.value },
        contentAlignment = Alignment.Center,
    ) {
        ByteRain(Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(Lime.copy(alpha = 0.10f), Ink.copy(alpha = 0f)), center = center, radius = size.minDimension * 0.6f),
                radius = size.minDimension * 0.6f,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LensMark(
                size = 184.dp,
                assemble = assemble.value,
                modifier = Modifier.graphicsLayer {
                    val z = 1f + exit.value * 2.4f
                    scaleX = z
                    scaleY = z
                },
            )
            Spacer(Modifier.height(28.dp))
            Wordmark(fontSize = 36.sp, reveal = words.value)
            Spacer(Modifier.height(10.dp))
            Text(
                "Reads the hidden labels inside images",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                modifier = Modifier.graphicsLayer { alpha = ((words.value - 0.5f) * 2f).coerceIn(0f, 1f) },
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("C2PA" to AiOrange, "IPTC" to CameraBlue, "XMP" to Lime, "EXIF" to Paper).forEachIndexed { i, (label, color) ->
                    val k = ((chips.value * 5f - i) / 1.5f).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .graphicsLayer { alpha = k; translationY = (1f - k) * 24f; scaleX = 0.8f + 0.2f * k; scaleY = 0.8f + 0.2f * k }
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) { MonoText(label, color = color, size = 11, bold = true) }
                }
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .width(180.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Line)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(bar.value)
                        .height(3.dp)
                        .background(Lime)
                )
            }
            Spacer(Modifier.height(10.dp))
            MonoText(if (bar.value < 1f) "CALIBRATING LENS…" else "READY · OFFLINE", size = 10)
        }
    }
}

/** Faint columns of hex bytes drifting upward behind the splash. */
@Composable
private fun ByteRain(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "rain")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "drift")
    val typeface = rememberMonoTypeface()
    val columns = remember {
        val rnd = Random(7)
        List(14) { col -> List(28) { "%02X".format(rnd.nextInt(256)) } to (rnd.nextFloat() * 0.6f + 0.4f) to col }
    }
    Canvas(modifier) {
        val colW = size.width / columns.size
        val rowH = 22.dp.toPx()
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 10.dp.toPx()
                this.typeface = typeface
            }
            columns.forEach { (pair, col) ->
                val (bytes, speed) = pair
                val offset = (drift * speed * rowH * bytes.size) % (rowH * bytes.size)
                bytes.forEachIndexed { i, b ->
                    var y = i * rowH - offset
                    if (y < -rowH) y += rowH * bytes.size
                    val fade = (1f - kotlin.math.abs(y / size.height - 0.5f) * 2f).coerceIn(0f, 1f)
                    paint.color = (if ((i + col) % 9 == 0) Lime else Muted).copy(alpha = 0.10f * fade).toArgb()
                    canvas.nativeCanvas.drawText(b, col * colW + colW * 0.3f, y, paint)
                }
            }
        }
    }
}

/**
 * Shown when back is pressed on the home screen. The lens closes its aperture as the dialog
 * appears. The native ad sits in its own labelled block, well away from the buttons.
 */
@Composable
fun ExitDialog(onStay: () -> Unit, onExit: () -> Unit) {
    val enter = remember { Animatable(0f) }
    val close = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { enter.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow)) }
        delay(250)
        close.animateTo(0.75f, tween(900, easing = FastOutSlowInEasing))
    }
    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val s = 0.85f + 0.15f * enter.value
                    scaleX = s
                    scaleY = s
                    alpha = enter.value.coerceIn(0f, 1f)
                    translationY = (1f - enter.value) * 80f
                }
                .clip(RoundedCornerShape(28.dp))
                .background(Ink)
                .border(1.dp, Line, RoundedCornerShape(28.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.width(150.dp).height(110.dp)) {
                    drawCircle(
                        Brush.radialGradient(listOf(Lime.copy(alpha = 0.12f), Ink.copy(alpha = 0f)), center = center, radius = size.minDimension),
                        radius = size.minDimension,
                    )
                    // Little "shutter" sparks either side of the lens.
                    for (i in 0 until 3) {
                        val y = size.height * (0.3f + 0.2f * i)
                        drawLine(Line, Offset(0f, y), Offset(size.width * 0.18f, y), 2f)
                        drawLine(Line, Offset(size.width * 0.82f, y), Offset(size.width, y), 2f)
                    }
                }
                LensMark(size = 96.dp, close = close.value)
            }
            Spacer(Modifier.height(10.dp))
            Text("Leaving already?", style = MaterialTheme.typography.headlineMedium, color = Paper, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                "Your scan history stays on this phone, ready for next time.",
                style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            NativeAdCard(NativeStyle.SMALL)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Exit", null, onExit, Modifier.weight(1f), height = 54.dp)
                PrimaryButton("Stay", null, onStay, Modifier.weight(1f))
            }
        }
    }
}
