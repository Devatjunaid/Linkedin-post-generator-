package com.dailycash.tracker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailycash.tracker.data.Money
import com.dailycash.tracker.ui.theme.Mono
import com.dailycash.tracker.ui.theme.Palette

/** A currency wallet drawn as a banknote with live guilloche print and a passing sheen. */
@Composable
fun Banknote(
    code: String,
    balanceMinor: Long,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Palette.noteFor(code)
    val lift by animateDpAsState(
        if (selected) (-8).dp else 0.dp,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "lift",
    )
    val loop = rememberInfiniteTransition(label = "note")
    val spin by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "spin")
    val sheen by loop.animateFloat(-0.6f, 1.6f, infiniteRepeatable(tween(4_500, easing = LinearEasing)), label = "sheen")
    val shape = RoundedCornerShape(18.dp)
    val serial = serialFor(code)

    Box(
        modifier
            .offset(y = lift)
            .size(width = 214.dp, height = 128.dp)
            .border(2.dp, if (selected) Palette.Mint else Color.Transparent, RoundedCornerShape(21.dp))
            .padding(3.dp)
            .clip(shape)
            .background(colors.bg)
            .border(1.dp, colors.line, shape)
            .clickable(role = Role.Button, onClickLabel = "Select $code wallet", onClick = onClick),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val pivot = Offset(size.width * 0.88f, size.height * 0.15f)
            rotate(spin, pivot) {
                for (i in 0 until 8) {
                    rotate(i * 22.5f, pivot) {
                        drawOval(
                            colors.line.copy(alpha = 0.55f),
                            topLeft = Offset(pivot.x - size.height * 0.95f, pivot.y - size.height * 0.36f),
                            size = Size(size.height * 1.9f, size.height * 0.72f),
                            style = Stroke(1.dp.toPx()),
                        )
                    }
                }
            }
            drawCircle(colors.line, size.height * 0.22f, pivot, style = Stroke(1.dp.toPx()))
            drawLine(
                colors.line.copy(alpha = 0.6f),
                Offset(14.dp.toPx(), size.height - 10.dp.toPx()),
                Offset(size.width - 14.dp.toPx(), size.height - 10.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
            )
            val x = size.width * sheen
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.09f), Color.Transparent),
                    start = Offset(x - 60f, 0f),
                    end = Offset(x + 60f, size.height),
                ),
            )
        }
        Text(
            serial,
            color = colors.ink,
            fontFamily = Mono,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 12.dp),
        )
        Text(
            code,
            color = colors.ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 14.dp, top = 10.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 16.dp)) {
            Text(
                Money.format(balanceMinor, code),
                color = Palette.Text,
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
            )
            Text(caption, color = colors.ink, fontSize = 11.sp, maxLines = 1)
        }
    }
}

private fun serialFor(code: String): String {
    val h = Math.floorMod(code.hashCode() * 7919, 100_000_000)
    return "%s %04d %04d".format(code.take(2), h / 10_000, h % 10_000)
}

/** Dashed "empty slot" note that starts the add-cash flow. */
@Composable
fun AddWalletNote(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .padding(3.dp)
            .size(width = 150.dp, height = 122.dp)
            .clip(shape)
            .clickable(role = Role.Button, onClickLabel = "Add cash to a wallet", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                Palette.Muted.copy(alpha = 0.6f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = Palette.Mint)
            Text("Add cash", color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("any currency", color = Palette.Muted, fontSize = 11.sp)
        }
    }
}
