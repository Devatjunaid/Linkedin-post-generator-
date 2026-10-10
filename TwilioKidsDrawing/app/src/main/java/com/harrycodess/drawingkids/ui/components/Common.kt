package com.harrycodess.drawingkids.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.harrycodess.drawingkids.audio.Sfx
import com.harrycodess.drawingkids.ui.theme.Kid
import com.harrycodess.drawingkids.ui.theme.kidText
import kotlin.random.Random

/**
 * The app's signature "chunky sticker" surface: rounded, with a solid shadow lip underneath,
 * and a squishy press animation.
 */
@Composable
fun Chunky(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    shadow: Color = Kid.Line,
    corner: Dp = 20.dp,
    depth: Dp = 5.dp,
    border: Color? = null,
    borderWidth: Dp = 3.dp,
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press",
    )
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                val d = depth.toPx()
                val r = CornerRadius(corner.toPx())
                drawRoundRect(shadow, topLeft = Offset(0f, d), size = Size(size.width, size.height - d), cornerRadius = r)
                drawRoundRect(color, size = Size(size.width, size.height - d), cornerRadius = r)
                if (border != null) {
                    val bw = borderWidth.toPx()
                    drawRoundRect(
                        border, topLeft = Offset(bw / 2, bw / 2),
                        size = Size(size.width - bw, size.height - d - bw), cornerRadius = r, style = Stroke(bw),
                    )
                }
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null) {
                        Sfx.pop()
                        onClick()
                    }
                } else {
                    Modifier
                },
            )
            .padding(bottom = depth),
        contentAlignment = contentAlignment,
        content = content,
    )
}

@Composable
fun IconButton32(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    enabled: Boolean = true,
    draw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
) {
    Chunky(
        modifier.size(size),
        corner = 18.dp,
        onClick = if (enabled) onClick else null,
    ) {
        Icon32(Modifier.size(size * 0.5f), draw)
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Chunky(
        modifier,
        color = if (selected) Kid.Ink else Color.White,
        shadow = if (selected) Color(0xFF120D1F) else Kid.Line,
        corner = 50.dp,
        depth = 4.dp,
        onClick = onClick,
    ) {
        Text(
            text,
            style = kidText(17, color = if (selected) Color.White else Kid.Ink),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun Swatch(color: Color, selected: Boolean, size: Dp, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.12f else 1f, spring(dampingRatio = 0.45f), label = "swatch")
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (selected) Modifier.border(3.dp, Kid.Ink, CircleShape) else Modifier)
            .padding(if (selected) 4.dp else 0.dp)
            .shadow(if (selected) 0.dp else 2.dp, CircleShape)
            .background(color, CircleShape)
            .border(3.dp, if (color == Color.White) Kid.Line else Color.White, CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                Sfx.tick()
                onClick()
            },
    )
}

/** Big friendly yes/no dialog for destructive actions (kids tap things by accident). */
@Composable
fun ConfirmDialog(title: String, yes: String, onYes: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Chunky(corner = 32.dp, depth = 8.dp, shadow = Kid.Ink, border = Kid.Ink) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(title, style = kidText(26))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Chunky(color = Kid.Line, shadow = Color(0xFFD9CFC0), corner = 22.dp, onClick = onDismiss) {
                        Text("Keep it", style = kidText(20), modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
                    }
                    Chunky(color = Kid.Red, shadow = Color(0xFFC92F2F), corner = 22.dp, onClick = onYes) {
                        Text(yes, style = kidText(20, color = Color.White), modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
                    }
                }
            }
        }
    }
}

/** A short burst of confetti; re-runs whenever [trigger] changes to a new positive value. */
@Composable
fun Confetti(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger <= 0) return
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { progress.animateTo(1f, tween(1800, easing = LinearEasing)) }
    val pieces = remember(trigger) {
        val r = Random(trigger)
        List(70) {
            ConfettiPiece(
                x = r.nextFloat(), delay = r.nextFloat() * 0.35f, speed = 0.7f + r.nextFloat() * 0.6f,
                drift = (r.nextFloat() - 0.5f) * 0.25f, spin = r.nextFloat() * 720f,
                color = Kid.Rainbow[r.nextInt(Kid.Rainbow.size)], round = r.nextBoolean(),
            )
        }
    }
    val p = progress.value
    if (p >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        pieces.forEach { c ->
            val t = ((p - c.delay) / (1f - c.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val x = (c.x + c.drift * t) * size.width
            val y = -20f + t * c.speed * size.height * 1.1f
            val s = 9.dp.toPx()
            withTransform({ rotate(c.spin * t, Offset(x, y)) }) {
                if (c.round) {
                    drawCircle(c.color, s * 0.55f, Offset(x, y))
                } else {
                    drawRect(c.color, topLeft = Offset(x - s / 2, y - s / 4), size = Size(s, s / 2))
                }
            }
        }
    }
}

private class ConfettiPiece(
    val x: Float, val delay: Float, val speed: Float, val drift: Float, val spin: Float,
    val color: Color, val round: Boolean,
)

/** Small rotated sticker label ("Saved!", etc.). */
@Composable
fun StickerLabel(text: String, color: Color, modifier: Modifier = Modifier, angle: Float = -4f) {
    Box(
        modifier
            .rotate(angle)
            .shadow(6.dp, RoundedCornerShape(50))
            .background(color, RoundedCornerShape(50))
            .border(3.dp, Kid.Ink, RoundedCornerShape(50))
            .padding(horizontal = 26.dp, vertical = 10.dp),
    ) {
        Text(text, style = kidText(28, FontWeight.Black))
    }
}

@Composable
fun FullCenter(content: @Composable BoxScope.() -> Unit) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center, content = content)
