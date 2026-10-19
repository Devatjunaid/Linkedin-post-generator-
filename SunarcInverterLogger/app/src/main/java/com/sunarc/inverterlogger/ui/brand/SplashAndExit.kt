package com.sunarc.inverterlogger.ui.brand

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sunarc.inverterlogger.domain.Countries
import com.sunarc.inverterlogger.domain.InverterBrands
import com.sunarc.inverterlogger.ui.theme.DisplayFont
import com.sunarc.inverterlogger.ui.theme.MonoFont
import com.sunarc.inverterlogger.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Animated splash: horizon rings expand, light motes drift up, the logo assembles, the wordmark
 * rises letter by letter and a sun-coloured progress line runs along the horizon.
 */
@Composable
fun AnimatedSplash(onFinished: () -> Unit) {
    val intro = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val line = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { intro.animateTo(1f, tween(1700, easing = LinearEasing)) }
        launch { delay(650); word.animateTo(1f, tween(900)) }
        launch { delay(300); line.animateTo(1f, tween(1900)) }
        delay(2350)
        onFinished()
    }

    val loop = rememberInfiniteTransition(label = "splash")
    val drift by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "drift")

    Box(Modifier.fillMaxSize().background(Palette.Night), contentAlignment = Alignment.Center) {
        // Background: horizon rings and rising light motes.
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height * 0.62f)
            for (i in 0 until 4) {
                val r = size.width * (0.35f + i * 0.22f) * (0.85f + 0.15f * intro.value)
                drawArc(
                    color = Palette.Sun.copy(alpha = (0.10f - i * 0.02f) * intro.value),
                    startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                    style = Stroke(width = 1.5f)
                )
            }
            for (i in 0 until 26) {
                val seed = i * 37.17f
                val x = (sin(seed) * 0.5f + 0.5f) * size.width
                val speed = 0.4f + (i % 5) * 0.15f
                val t = (drift * speed + i / 26f) % 1f
                val y = size.height * (0.95f - t * 0.9f)
                val a = sin(t * Math.PI.toFloat()) * 0.55f * intro.value
                drawCircle(if (i % 3 == 0) Palette.Sky else Palette.SunSoft, radius = 1.4f + (i % 3), center = Offset(x, y), alpha = a)
            }
            // Horizon line with the progress sweep.
            val y = size.height * 0.62f + size.width * 0.04f
            drawLine(Palette.Line, Offset(size.width * 0.12f, y), Offset(size.width * 0.88f, y), strokeWidth = 2f, cap = StrokeCap.Round)
            val end = size.width * (0.12f + 0.76f * line.value)
            drawLine(
                Brush.horizontalGradient(listOf(Palette.Sun.copy(alpha = 0f), Palette.Sun), startX = size.width * 0.12f, endX = end),
                Offset(size.width * 0.12f, y), Offset(end, y), strokeWidth = 3f, cap = StrokeCap.Round
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SunarcLogo(size = 190.dp, intro = intro.value)
            Spacer(Modifier.height(18.dp))
            Row {
                "Sunarc".forEachIndexed { i, ch ->
                    val t = ((word.value - i * 0.09f) / 0.5f).coerceIn(0f, 1f)
                    Text(
                        ch.toString(),
                        modifier = Modifier.graphicsLayer { alpha = t; translationY = (1f - t) * 28f },
                        fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 44.sp,
                        color = if (i >= 3) Palette.Sun else Palette.Ink, letterSpacing = (-1).sp
                    )
                }
            }
            Text(
                "INVERTER LOGGER",
                modifier = Modifier.alpha(((word.value - 0.45f) / 0.55f).coerceIn(0f, 1f)),
                fontFamily = MonoFont, fontSize = 12.sp, letterSpacing = 4.sp, color = Palette.Muted
            )
        }

        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp).alpha(((line.value - 0.4f) / 0.6f).coerceIn(0f, 1f)),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Offline OCR", "${Countries.all.size} countries", "${InverterBrands.all.size - 1} brands").forEach {
                Text(
                    it,
                    modifier = Modifier.border(1.dp, Palette.Line, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                    fontFamily = MonoFont, fontSize = 11.sp, color = Palette.InkSoft
                )
            }
        }
    }
}

/**
 * Shown when Back is pressed on the main screen. The sun sets behind the panel while the user
 * decides. It carries no ads.
 */
@Composable
fun ExitDialog(todaySummary: String?, onStay: () -> Unit, onExit: () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    val sunset = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(250); sunset.animateTo(1f, tween(2200)) }

    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(220)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), initialScale = 0.82f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            Column(
                Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF1B1A22), Palette.Surface)),
                        RoundedCornerShape(30.dp)
                    )
                    .border(1.dp, Palette.Line, RoundedCornerShape(30.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SunarcLogo(size = 132.dp, sunset = sunset.value)
                Spacer(Modifier.height(8.dp))
                Text("Calling it a day?", fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Palette.Ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    todaySummary ?: "Your log is saved on this phone.",
                    fontSize = 14.sp, color = Palette.InkSoft
                )
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier.weight(1f).height(52.dp),
                        border = BorderStroke(1.dp, Palette.Line),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Exit", color = Palette.InkSoft) }
                    Button(
                        onClick = onStay,
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Palette.Sun, contentColor = Palette.OnSun),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Stay", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}
