package com.dotdash.kids.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Twinkling night sky behind every screen. */
@Composable
fun Sky(count: Int = 26, seed: Int = 1, modifier: Modifier = Modifier) {
    val stars = remember(seed) {
        val r = Random(seed)
        List(count) { floatArrayOf(r.nextFloat(), r.nextFloat(), 1.5f + r.nextFloat() * 2.5f, r.nextFloat()) }
    }
    val t = rememberInfiniteTransition(label = "sky")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "twinkle")
    Canvas(modifier.fillMaxSize()) {
        for (s in stars) {
            val a = 0.15f + 0.7f * (0.5f + 0.5f * sin(2 * PI.toFloat() * (p + s[3])))
            drawCircle(C.SunLight.copy(alpha = a), s[2] * density / 2f, Offset(s[0] * size.width, s[1] * size.height))
        }
    }
}

/** Chunky toy-like button: a coloured face sitting on a darker base; pressing pushes it down. */
@Composable
fun ChunkyButton(
    onClick: () -> Unit,
    color: Color,
    base: Color,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    depth: Dp = 7.dp,
    shape: Shape = RoundedCornerShape(50),
    label: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val push = if (pressed) depth - 2.dp else 0.dp
    Box(
        modifier
            .height(height + depth)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { if (label != null) contentDescription = label },
    ) {
        Box(Modifier.fillMaxWidth().height(height).offset(y = depth).clip(shape).background(base))
        Row(
            Modifier.fillMaxWidth().height(height).offset(y = push).clip(shape).background(color).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun RoundButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = C.Night3,
    base: Color = C.Deep,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val depth = 5.dp
    Box(
        modifier
            .size(size, size + depth)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Box(Modifier.size(size).offset(y = depth).clip(RoundedCornerShape(50)).background(base))
        Box(
            Modifier.size(size).offset(y = if (pressed) depth - 1.dp else 0.dp).clip(RoundedCornerShape(50)).background(color),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
fun Pill(modifier: Modifier = Modifier, color: Color = C.Night2, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50)).background(color).padding(start = 9.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
fun StarCount(n: Int, size: Dp = 26.dp) {
    Pill {
        StarIcon(size)
        BasicText("$n", style = display(20.sp))
    }
}

/** Speech bubble with Zip's line. Numbers in **bold** are highlighted. */
@Composable
fun Bubble(text: AnnotatedString, modifier: Modifier = Modifier) {
    BasicText(
        text,
        modifier
            .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
            .background(C.White)
            .padding(horizontal = 15.dp, vertical = 11.dp),
        style = body(16.sp, C.Ink, FontWeight.Black),
    )
}

/** Builds "Find number **13**!" style lines: segments between ** are highlighted. */
fun zipLine(s: String): AnnotatedString = buildAnnotatedString {
    s.split("**").forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(color = C.MintDark, fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 20.sp)) { append(part) }
        else append(part)
    }
}

/** Gentle up-and-down float, used for Zip everywhere. */
@Composable
fun Modifier.bob(amount: Dp = 9.dp, millis: Int = 1100, delayMs: Int = 0): Modifier {
    val t = rememberInfiniteTransition(label = "bob")
    val y by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(millis), RepeatMode.Reverse, StartOffset(delayMs)),
        label = "bobY",
    )
    return this.graphicsLayer { translationY = -amount.toPx() * y }
}

/** Slow "breathing" pulse that invites a tap. */
@Composable
fun Modifier.breathe(): Modifier {
    val t = rememberInfiniteTransition(label = "breathe")
    val s by t.animateFloat(1f, 1.05f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "scale")
    return this.graphicsLayer { scaleX = s; scaleY = s }
}

/** Side-to-side shake for a "nope" moment; restarts every time [trigger] changes. */
@Composable
fun Modifier.shake(trigger: Int): Modifier {
    val x = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        for (v in listOf(-14f, 14f, -10f, 8f, 0f)) x.animateTo(v, tween(70))
    }
    return this.graphicsLayer { translationX = x.value }
}

private val CONFETTI = listOf(C.Sun, C.Coral, C.Mint, C.Sky, C.Grape, Color.White)

/** A burst of falling confetti; a new [key] starts a new burst. */
@Composable
fun Confetti(key: Int, modifier: Modifier = Modifier) {
    if (key == 0) return
    val progress = remember(key) { Animatable(0f) }
    val pieces = remember(key) {
        val r = Random(key)
        List(70) { floatArrayOf(r.nextFloat(), r.nextFloat() * 0.25f, 0.55f + r.nextFloat() * 0.45f, r.nextFloat() * 720 - 360, r.nextFloat() * 120 - 60, it.toFloat()) }
    }
    LaunchedEffect(key) { progress.animateTo(1f, tween(3200, easing = LinearEasing)) }
    if (progress.value >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        for (c in pieces) {
            val t = ((p - c[1]) / c[2]).coerceIn(0f, 1f)
            if (t <= 0f || t >= 1f) continue
            val x = c[0] * size.width + c[4] * density * t
            val y = -20 * density + (size.height + 40 * density) * t
            val color = CONFETTI[c[5].toInt() % CONFETTI.size]
            rotate(c[3] * t, Offset(x, y)) {
                if (c[5].toInt() % 3 == 0) drawCircle(color, 5 * density, Offset(x, y))
                else drawRect(color, Offset(x - 5 * density, y - 7 * density), Size(10 * density, 14 * density))
            }
        }
    }
}
