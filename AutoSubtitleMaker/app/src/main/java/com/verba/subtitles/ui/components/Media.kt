package com.verba.subtitles.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.verba.subtitles.ui.theme.Verba
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Loads a small image file off the main thread. */
@Composable
fun rememberFileBitmap(file: File?): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, file?.absolutePath, file?.lastModified()) {
        value = if (file == null || !file.exists()) {
            null
        } else {
            withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull() }
        }
    }
    return bitmap
}

@Composable
fun Thumbnail(file: File?, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val bitmap = rememberFileBitmap(file)
    Box(modifier.background(Verba.SurfaceHigh)) {
        if (bitmap != null) {
            Image(bitmap, contentDescription, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            // Abstract placeholder art while loading / when no frame is available.
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Verba.Coral.copy(alpha = 0.8f), radius = size.minDimension * 0.42f, center = Offset(size.width * 0.85f, size.height * 0.1f))
                rotate(-12f) {
                    drawRect(Verba.Ink.copy(alpha = 0.35f), topLeft = Offset(-size.width * 0.2f, size.height * 0.55f), size = androidx.compose.ui.geometry.Size(size.width * 1.4f, size.height * 0.22f))
                }
            }
        }
    }
}

/**
 * Home hero: a slowly spinning ring of waveform ticks around a pulsing lime button.
 * Tapping the button opens the system video picker.
 */
@Composable
fun WaveDisc(onClick: () -> Unit, size: Dp = 236.dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "disc")
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(48_000, easing = LinearEasing)), label = "spin")
    val orbit by transition.animateFloat(360f, 0f, infiniteRepeatable(tween(9_000, easing = LinearEasing)), label = "orbit")
    val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2_400), RepeatMode.Restart), label = "pulse")
    val wobble by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(3_000, easing = LinearEasing)), label = "wobble")

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val outer = this.size.minDimension / 2f
            rotate(spin) {
                for (i in 0 until 72) {
                    val a = (i * 5f - 90f) * PI.toFloat() / 180f
                    val base = abs(sin(i * 0.7f) * cos(i * 0.23f))
                    val live = 0.75f + 0.25f * sin(wobble + i * 0.5f)
                    val len = (6.dp.toPx() + base * 22.dp.toPx()) * live
                    val r0 = outer - 30.dp.toPx()
                    val dir = Offset(cos(a), sin(a))
                    drawLine(
                        Verba.Lime.copy(alpha = 0.3f + 0.7f * base),
                        start = c + dir * r0,
                        end = c + dir * (r0 + len),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            drawCircle(Verba.Surface, radius = outer - 34.dp.toPx(), center = c)
            drawCircle(Verba.Line, radius = outer - 34.dp.toPx(), center = c, style = Stroke(1.dp.toPx()))
            val orbitR = outer - 46.dp.toPx()
            val oa = (orbit - 90f) * PI.toFloat() / 180f
            drawCircle(Verba.Coral, radius = 5.dp.toPx(), center = c + Offset(cos(oa), sin(oa)) * orbitR)
            drawCircle(
                Verba.Lime.copy(alpha = (1f - pulse) * 0.8f),
                radius = 52.dp.toPx() * (0.92f + 0.63f * pulse),
                center = c,
                style = Stroke(2.dp.toPx()),
            )
        }
        Column(
            Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(Verba.Lime)
                .semantics { contentDescription = "Pick a video" }
                .pressable(onClick = onClick, role = Role.Button, pressedScale = 0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = Verba.Ink, modifier = Modifier.size(30.dp))
            Text("Video", style = MaterialTheme.typography.titleSmall, color = Verba.Ink)
        }
    }
}

/** Live equaliser bars used while the app is listening to a video. */
@Composable
fun EqualizerBars(modifier: Modifier = Modifier, bars: Int = 36, active: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "eq")
    val t by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1_100, easing = LinearEasing)), label = "t")
    Canvas(modifier) {
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val base = 0.25f + 0.75f * abs(sin(i * 1.3f))
            val level = if (active) base * (0.35f + 0.65f * abs(sin(t + i * 0.45f))) else base * 0.2f
            val h = size.height * level
            drawRoundRect(
                Verba.Lime,
                topLeft = Offset(i * (w + gap), (size.height - h) / 2f),
                size = androidx.compose.ui.geometry.Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2f),
            )
        }
    }
}
