package com.devatjunaid.isthisai.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.devatjunaid.isthisai.R
import com.devatjunaid.isthisai.ui.theme.Ink
import com.devatjunaid.isthisai.ui.theme.Line
import com.devatjunaid.isthisai.ui.theme.LineStrong
import com.devatjunaid.isthisai.ui.theme.Lime
import com.devatjunaid.isthisai.ui.theme.Mono
import com.devatjunaid.isthisai.ui.theme.Muted
import com.devatjunaid.isthisai.ui.theme.Paper
import com.devatjunaid.isthisai.ui.theme.Surface
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------- Buttons

/** Main call to action: off-white pill with a light glint sweeping across it. */
@Composable
fun PrimaryButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier, glint: Boolean = false) {
    val glintX = if (glint) {
        val t = rememberInfiniteTransition(label = "glint")
        t.animateFloat(-0.6f, 1.8f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "glintX").value
    } else -2f
    Row(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Paper)
            .clickable(role = Role.Button, onClick = onClick)
            .drawWithContent {
                drawContent()
                if (glint && glintX in -0.4f..1.4f) {
                    val x = size.width * glintX
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.6f), Color.Transparent),
                            start = Offset(x - size.width * 0.2f, 0f), end = Offset(x + size.width * 0.2f, size.height),
                        )
                    )
                }
            }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = Ink, style = MaterialTheme.typography.labelLarge, fontSize = 17.sp)
    }
}

@Composable
fun SecondaryButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 50.dp) {
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(BorderStroke(1.dp, Line), RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Paper, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Paper, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun IconSquareButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Paper) {
    Box(
        modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun MonoText(text: String, color: Color = Muted, modifier: Modifier = Modifier, size: Int = 11, bold: Boolean = false) {
    Text(
        text, color = color, modifier = modifier, fontFamily = Mono, fontSize = size.sp, letterSpacing = 0.5.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
    )
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) { MonoText(text.uppercase(), color = color, size = 10, bold = true) }
}

// ---------------------------------------------------------------- Drawing helpers

@Composable
fun rememberFont(resId: Int): Typeface? {
    val context = LocalContext.current
    return remember(resId) { runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull() }
}

@Composable
fun rememberMonoTypeface(): Typeface? = rememberFont(R.font.jetbrains_mono_regular)

/** Text running around a circle, starting at the top. */
fun DrawScope.drawCircularText(text: String, radius: Float, color: Color, textSizePx: Float, typeface: Typeface?, rotation: Float, spacing: Float = 0.25f) {
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val path = android.graphics.Path().apply {
            addCircle(center.x, center.y, radius, android.graphics.Path.Direction.CW)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            this.textSize = textSizePx
            this.typeface = typeface
            letterSpacing = spacing
        }
        nc.save()
        nc.rotate(rotation - 90f, center.x, center.y)
        nc.drawTextOnPath(text, path, 0f, textSizePx * 0.35f, paint)
        nc.restore()
    }
}

/** One aperture blade: an arc on the rim closing into a sharp inner point. */
fun bladePath(c: Offset, r: Float): Path = Path().apply {
    moveTo(c.x, c.y - r)
    arcTo(Rect(c, r), -90f, 60f, false)
    lineTo(c.x + 0.2f * r, c.y + 0.0545f * r)
    lineTo(c.x - 0.0364f * r, c.y - 0.3455f * r)
    close()
}

fun DrawScope.drawAperture(c: Offset, r: Float, rotation: Float, light: Color = Color(0xFF2B2F25), dark: Color = Color(0xFF22251D)) {
    val blade = bladePath(c, r)
    for (k in 0 until 6) {
        rotate(k * 60f + rotation, pivot = c) {
            drawPath(blade, if (k % 2 == 0) light else dark)
            drawPath(blade, Ink, style = Stroke(width = r * 0.025f))
        }
    }
}

fun DrawScope.drawCornerBrackets(color: Color, inset: Float, arm: Float, width: Float, scale: Float = 1f) {
    val w = size.width
    val h = size.height
    withTransform({ scale(scale, scale, pivot = center) }) {
        val pts = listOf(
            Triple(Offset(inset, inset), 1f, 1f), Triple(Offset(w - inset, inset), -1f, 1f),
            Triple(Offset(w - inset, h - inset), -1f, -1f), Triple(Offset(inset, h - inset), 1f, -1f),
        )
        for ((p, dx, dy) in pts) {
            drawLine(color, p, Offset(p.x + dx * arm, p.y), width, StrokeCap.Round)
            drawLine(color, p, Offset(p.x, p.y + dy * arm), width, StrokeCap.Round)
        }
    }
}

fun DrawScope.drawSpark(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x + r * 0.16f, c.y - r * 0.16f, c.x + r, c.y)
        quadraticTo(c.x + r * 0.16f, c.y + r * 0.16f, c.x, c.y + r)
        quadraticTo(c.x - r * 0.16f, c.y + r * 0.16f, c.x - r, c.y)
        quadraticTo(c.x - r * 0.16f, c.y - r * 0.16f, c.x, c.y - r)
        close()
    }
    drawPath(p, color)
}

// ---------------------------------------------------------------- Hero aperture

/**
 * The home screen lens: a tick-marked barrel with orbiting label names, six breathing aperture
 * blades and a pulsing lime iris. Tapping it opens the photo picker.
 */
@Composable
fun ApertureHero(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 280.dp) {
    val t = rememberInfiniteTransition(label = "hero")
    val ring by t.animateFloat(0f, 360f, infiniteRepeatable(tween(48_000, easing = LinearEasing)), label = "ring")
    val breathe by t.animateFloat(0f, 14f, infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "breathe")
    val pulse by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse")
    val orbit by t.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "orbit")
    val typeface = rememberMonoTypeface()

    Canvas(
        modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Lens. Tap to choose an image" }
    ) {
        val c = center
        val r = this.size.minDimension / 2f
        drawCircle(Line, r * 0.985f, c, style = Stroke(1.dp.toPx()))
        // Tick marks around the barrel.
        for (i in 0 until 72) {
            val a = Math.toRadians(i * 5.0)
            val long = i % 6 == 0
            val r1 = r * 0.90f
            val r2 = r * if (long) 0.965f else 0.94f
            drawLine(
                if (long) LineStrong.copy(alpha = 1f) else LineStrong.copy(alpha = 0.7f),
                Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                Offset(c.x + r2 * cos(a).toFloat(), c.y + r2 * sin(a).toFloat()),
                strokeWidth = if (long) 2f else 1.2f,
            )
        }
        // Orbiting lime satellite.
        val oa = Math.toRadians(orbit.toDouble() - 90)
        drawCircle(Lime, r * 0.022f, Offset(c.x + r * 0.925f * cos(oa).toFloat(), c.y + r * 0.925f * sin(oa).toFloat()))
        drawCircularText(
            "C2PA · IPTC · XMP · EXIF · PNG TEXT · JUMBF · CONTENT CREDENTIALS · ON-DEVICE · ",
            r * 0.83f, Muted, r * 0.072f, typeface, ring, 0.18f,
        )
        drawCircle(Surface, r * 0.745f, c)
        drawCircle(Line, r * 0.745f, c, style = Stroke(1f))
        drawAperture(c, r * 0.73f, breathe)
        drawCircle(Ink, r * 0.2f, c)
        drawCircle(LineStrong, r * 0.2f, c, style = Stroke(1.5f))
        drawCircle(Lime.copy(alpha = 0.18f * pulse), r * 0.13f, c)
        drawSpark(c, r * (0.07f + 0.02f * pulse), Lime)
    }
}

// ---------------------------------------------------------------- Scan visuals

/** A slow marquee of the file's real bytes. */
@Composable
fun HexRibbon(hex: String, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "hex")
    val shift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "shift")
    val text = remember(hex) { (hex.ifBlank { "FF D8 FF E1 00 00 45 78 69 66" } + "   ").repeat(3) }
    Box(
        modifier
            .height(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, Line, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text,
            maxLines = 1,
            softWrap = false,
            fontFamily = Mono,
            fontSize = 11.sp,
            color = Color(0xFF6E7265),
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .graphicsLayer { translationX = -shift * size.width / 3f },
        )
    }
}

/** Animated lime scanline and measuring grid laid over the image being read. */
@Composable
fun ScanOverlay(modifier: Modifier = Modifier, active: Boolean = true) {
    val t = rememberInfiniteTransition(label = "scan")
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "y")
    Canvas(modifier.fillMaxSize()) {
        val step = 24.dp.toPx()
        var x = 0f
        while (x < size.width) { drawLine(Lime.copy(alpha = 0.07f), Offset(x, 0f), Offset(x, size.height), 1f); x += step }
        var gy = 0f
        while (gy < size.height) { drawLine(Lime.copy(alpha = 0.07f), Offset(0f, gy), Offset(size.width, gy), 1f); gy += step }
        if (active) {
            val ly = size.height * y
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, Lime.copy(alpha = 0.28f), Color.Transparent), startY = ly - 40f, endY = ly + 40f),
                topLeft = Offset(0f, ly - 40f), size = Size(size.width, 80f),
            )
            drawLine(Lime, Offset(0f, ly), Offset(size.width, ly), 3f)
        }
        drawCornerBrackets(Lime, 16.dp.toPx(), 26.dp.toPx(), 3.dp.toPx())
    }
}

// ---------------------------------------------------------------- Verdict stamp

/** The round verdict seal. [drop] runs 0→1 for the stamp-down animation. */
@Composable
fun VerdictStamp(word: String, sub: String, ringText: String, color: Color, drop: Float, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "stamp")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(30_000, easing = LinearEasing)), label = "spin")
    val typeface = rememberMonoTypeface()
    val display = rememberFont(R.font.bricolage_extrabold)
    Canvas(modifier.semantics { contentDescription = "$word. $sub" }) {
        val c = center
        val r = size.minDimension / 2f
        // Shockwave ring once the stamp lands.
        if (drop > 0.55f) {
            val k = ((drop - 0.55f) / 0.45f).coerceIn(0f, 1f)
            drawCircle(color.copy(alpha = (1f - k) * 0.7f), r * (0.75f + 0.45f * k), c, style = Stroke(2.dp.toPx()))
        }
        val scale = when {
            drop < 0.55f -> 1.9f - 1.0f * (drop / 0.55f)
            drop < 0.75f -> 0.9f + 0.14f * ((drop - 0.55f) / 0.2f)
            else -> 1.04f - 0.04f * ((drop - 0.75f) / 0.25f)
        }
        val tilt = -28f + 20f * drop.coerceIn(0f, 1f)
        val alpha = (drop / 0.4f).coerceIn(0f, 1f)
        withTransform({
            scale(scale, scale, pivot = c)
            rotate(tilt, pivot = c)
        }) {
            drawCircle(color.copy(alpha = alpha), r * 0.94f, c, style = Stroke(3.dp.toPx()))
            drawCircle(color.copy(alpha = alpha), r * 0.64f, c, style = Stroke(1.5.dp.toPx()))
            drawCircularText(ringText, r * 0.79f, color.copy(alpha = alpha), r * 0.1f, typeface, spin, 0.3f)
            drawIntoCanvas { canvas ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color.copy(alpha = alpha).toArgb()
                    textAlign = Paint.Align.CENTER
                    this.typeface = display ?: Typeface.DEFAULT_BOLD
                }
                paint.textSize = r * if (word.length > 8) 0.2f else 0.25f
                canvas.nativeCanvas.drawText(word, c.x, c.y + r * 0.02f, paint)
                paint.typeface = typeface
                paint.textSize = r * 0.095f
                paint.letterSpacing = 0.15f
                canvas.nativeCanvas.drawText(sub, c.x, c.y + r * 0.22f, paint)
            }
        }
    }
}
