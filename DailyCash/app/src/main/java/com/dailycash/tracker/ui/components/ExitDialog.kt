package com.dailycash.tracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dailycash.tracker.ui.theme.Palette
import kotlin.math.sin

/** Shown when back is pressed on the wallet screen. Purely a confirmation: it carries no ads. */
@Composable
fun ExitDialog(todaySpent: String?, onStay: () -> Unit, onExit: () -> Unit) {
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)) }
    val loop = rememberInfiniteTransition(label = "exit")
    val wave by loop.animateFloat(-1f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "wave")
    val bob by loop.animateFloat(0f, 6.2832f, infiniteRepeatable(tween(2400)), label = "bob")

    Dialog(onDismissRequest = onStay) {
        Column(
            Modifier
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    alpha = ((pop.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                }
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Palette.Surface)
                .border(1.dp, Palette.Line, RoundedCornerShape(28.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(Modifier.size(150.dp, 110.dp)) {
                val c = center
                // A banknote waving goodbye, with coins bobbing around it.
                rotate(wave * 10f, Offset(c.x, c.y + 30f)) {
                    val w = size.width * 0.62f
                    val h = size.height * 0.48f
                    val tl = Offset(c.x - w / 2f, c.y - h / 2f)
                    drawRoundRect(Palette.notes[0].bg, tl, Size(w, h), CornerRadius(12f))
                    drawRoundRect(Palette.notes[0].line, tl + Offset(8f, 8f), Size(w - 16f, h - 16f), CornerRadius(8f), style = Stroke(2f))
                    drawCircle(Palette.notes[0].line, h * 0.22f, Offset(tl.x + w * 0.74f, c.y), style = Stroke(2f))
                    // little face
                    drawCircle(Palette.Mint, 4f, Offset(tl.x + w * 0.26f, c.y - 6f))
                    drawCircle(Palette.Mint, 4f, Offset(tl.x + w * 0.42f, c.y - 6f))
                    drawArc(Palette.Mint, 20f, 140f, false, Offset(tl.x + w * 0.24f, c.y - 4f), Size(w * 0.2f, 16f), style = Stroke(3f))
                }
                val coins = listOf(Palette.Amber to 0f, Palette.Sky to 2.1f, Palette.Mint to 4.2f)
                coins.forEachIndexed { i, (color, phase) ->
                    val x = size.width * (0.12f + i * 0.38f)
                    val y = size.height * 0.85f + sin(bob + phase) * 8f
                    drawCircle(color, 9f, Offset(x, y))
                    drawCircle(Palette.Ink.copy(alpha = 0.4f), 6f, Offset(x, y), style = Stroke(1.5f))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Leaving already?", color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (todaySpent != null) "You spent $todaySpent today. Everything is saved on this phone."
                else "Your wallets are saved on this phone. See you tomorrow.",
                color = Palette.Muted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onExit,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Exit", color = Palette.Text, fontWeight = FontWeight.Bold) }
                Button(
                    onClick = onStay,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Mint, contentColor = Palette.Ink),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Stay", fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}
