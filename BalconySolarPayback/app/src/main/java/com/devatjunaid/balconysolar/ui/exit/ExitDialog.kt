package com.devatjunaid.balconysolar.ui.exit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devatjunaid.balconysolar.ui.components.GhostButton
import com.devatjunaid.balconysolar.ui.components.PrimaryButton
import com.devatjunaid.balconysolar.ui.graphics.drawSparkle
import com.devatjunaid.balconysolar.ui.theme.Palette
import com.devatjunaid.balconysolar.ui.theme.Type

/** "Leaving already?" with an animated sunset: the sun sinks behind the balconies and the moon rises. */
@Composable
fun ExitDialog(annualKwh: Double?, onStay: () -> Unit, onExit: () -> Unit) {
    val enter = remember { Animatable(0f) }
    val sunset = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 260f))
    }
    LaunchedEffect(Unit) { sunset.animateTo(1f, tween(2200, easing = FastOutSlowInEasing)) }
    val t = rememberInfiniteTransition(label = "exit")
    val twinkle by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse), label = "tw")

    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val s = 0.85f + 0.15f * enter.value
                    scaleX = s; scaleY = s; alpha = enter.value.coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(28.dp))
                .background(Palette.Surface)
                .border(1.dp, Palette.Line, RoundedCornerShape(28.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val s = sunset.value
            Canvas(
                Modifier.fillMaxWidth().height(170.dp).semantics { contentDescription = "Animated sunset over balconies" },
            ) {
                val w = size.width
                val h = size.height
                drawRect(Brush.verticalGradient(listOf(lerp(Color(0xFF3A4E7A), Palette.NightDeep, s), lerp(Color(0xFFE08A5A), Palette.Dusk, s))))
                // Stars appear as the sun goes down.
                val stars = listOf(0.12f to 0.18f, 0.28f to 0.10f, 0.45f to 0.24f, 0.70f to 0.12f, 0.86f to 0.28f, 0.58f to 0.08f)
                for ((i, st) in stars.withIndex()) {
                    val a = (s * 1.3f - 0.3f).coerceIn(0f, 1f) * (if (i % 2 == 0) twinkle else 1.3f - twinkle)
                    if (i == 2) drawSparkle(Offset(w * st.first, h * st.second), 6f, Color.White.copy(alpha = a))
                    else drawCircle(Color.White.copy(alpha = a), 1.6f, Offset(w * st.first, h * st.second))
                }
                // Moon (crescent) rising on the left.
                val moon = Offset(w * 0.22f, h * (0.62f - 0.34f * s))
                drawCircle(Color(0xFFF2EFD9).copy(alpha = s), 15f, moon)
                drawCircle(lerp(Color(0xFF3A4E7A), Palette.NightDeep, s), 13f, moon + Offset(7f, -4f))
                // Sun sinking on the right, clipped by the horizon.
                val horizon = h * 0.74f
                clipRect(bottom = horizon) {
                    val sun = Offset(w * 0.68f, horizon - 34f + 70f * s)
                    drawCircle(Palette.Amber.copy(alpha = 0.3f), 40f, sun)
                    drawCircle(Palette.Amber, 26f, sun)
                }
                // Balcony buildings in the foreground; panels fade from lit to dark.
                val bc = Color(0xFF0A1420)
                drawRect(bc, Offset(0f, horizon), Size(w, h - horizon))
                val blocks = listOf(0.04f to 0.30f, 0.30f to 0.20f, 0.52f to 0.36f, 0.80f to 0.26f)
                for ((i, b) in blocks.withIndex()) {
                    val left = w * b.first
                    val bw = w * 0.17f
                    val top = horizon - h * b.second
                    drawRect(bc, Offset(left, top), Size(bw, horizon - top + 1f))
                    var y = top + 14f
                    var f = 0
                    while (y < horizon - 6f) {
                        drawRect(Color.White.copy(alpha = 0.08f), Offset(left + 3f, y), Size(bw - 6f, 2f))
                        val panel = lerp(Color(0xFF6FA8F0), Palette.Cell, s)
                        if ((i + f) % 2 == 0) drawRect(panel, Offset(left + bw * 0.5f, y - 7f), Size(bw * 0.34f, 5f))
                        val window = (i * 3 + f) % 4 == 0 && s > 0.6f
                        if (window) drawRect(Palette.AmberSoft.copy(alpha = 0.7f), Offset(left + bw * 0.16f, y - 9f), Size(bw * 0.16f, 7f))
                        y += 18f; f++
                    }
                }
                val ridge = Path().apply { moveTo(0f, horizon); lineTo(w, horizon) }
                drawPath(ridge, Color.White.copy(alpha = 0.1f), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            }
            Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Leaving already?", style = Type.title.copy(fontSize = Type.display.fontSize * 0.8f), color = Palette.Ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                val line = if (annualKwh != null) {
                    "Your setup is saved. The sun will keep making about ${com.devatjunaid.balconysolar.domain.Fmt.number(annualKwh)} kWh a year for you."
                } else "Your setup is saved for next time."
                Text(line, style = Type.body, color = Palette.Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                PrimaryButton("Stay", onClick = onStay)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    GhostButton("Exit app", onClick = onExit, color = Palette.Muted, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
