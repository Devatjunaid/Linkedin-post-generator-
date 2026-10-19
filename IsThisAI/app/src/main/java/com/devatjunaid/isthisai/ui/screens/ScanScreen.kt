package com.devatjunaid.isthisai.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devatjunaid.isthisai.engine.Analyzer
import com.devatjunaid.isthisai.engine.ScanStep
import com.devatjunaid.isthisai.engine.StepStatus
import com.devatjunaid.isthisai.ui.ScanState
import com.devatjunaid.isthisai.ui.components.AppIcons
import com.devatjunaid.isthisai.ui.components.HexRibbon
import com.devatjunaid.isthisai.ui.components.IconSquareButton
import com.devatjunaid.isthisai.ui.components.MonoText
import com.devatjunaid.isthisai.ui.components.PrimaryButton
import com.devatjunaid.isthisai.ui.components.ScanOverlay
import com.devatjunaid.isthisai.ui.components.SecondaryButton
import com.devatjunaid.isthisai.ui.components.drawAperture
import com.devatjunaid.isthisai.ui.theme.AiOrange
import com.devatjunaid.isthisai.ui.theme.Danger
import com.devatjunaid.isthisai.ui.theme.Dim
import com.devatjunaid.isthisai.ui.theme.Ink
import com.devatjunaid.isthisai.ui.theme.Lime
import com.devatjunaid.isthisai.ui.theme.Line
import com.devatjunaid.isthisai.ui.theme.Muted
import com.devatjunaid.isthisai.ui.theme.Paper
import com.devatjunaid.isthisai.ui.theme.Surface
import com.devatjunaid.isthisai.ui.theme.SurfaceHigh
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Iris transition: the screen is revealed through a rotating hexagonal aperture that opens from
 * the centre, with the edge of the opening traced in lime.
 */
fun Modifier.irisReveal(progress: Float): Modifier = drawWithContent {
    if (progress >= 1f) {
        drawContent()
        return@drawWithContent
    }
    val c = center
    val maxR = hypot(size.width / 2f, size.height / 2f) * 1.25f
    val r = maxR * progress
    val rot = Math.toRadians((1f - progress) * 90.0)
    val hex = Path().apply {
        for (i in 0 until 6) {
            val a = rot + Math.toRadians(60.0 * i)
            val p = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    // Closed blades fill the screen behind the opening.
    drawRect(Ink)
    drawAperture(c, maxR * 0.9f, (1f - progress) * 90f, light = SurfaceHigh, dark = Surface)
    clipPath(hex) { this@drawWithContent.drawContent() }
    drawPath(hex, Lime.copy(alpha = 1f - progress), style = Stroke(3.dp.toPx()))
}

@Composable
fun ScanScreen(state: ScanState, onCancel: () -> Unit, onFinished: () -> Unit, onRetry: () -> Unit) {
    val iris = remember { Animatable(0f) }
    LaunchedEffect(Unit) { iris.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }

    val ready = state as? ScanState.Ready
    var revealed by remember { mutableIntStateOf(0) }
    val total = ScanStep.entries.size

    // Reveal each step in turn once the analysis is done, then hand over to the verdict.
    LaunchedEffect(ready != null) {
        if (ready == null) return@LaunchedEffect
        delay(350)
        while (revealed < total) {
            delay(380)
            revealed++
        }
        delay(450)
        onFinished()
    }

    val fileName = when (state) {
        is ScanState.Reading -> state.fileName
        is ScanState.Ready -> state.report.fileName
        else -> "image"
    }
    val preview = (state as? ScanState.Reading)?.preview ?: ready?.preview
    val progress by animateFloatAsState(revealed / total.toFloat(), tween(350), label = "progress")

    Column(
        Modifier
            .fillMaxSize()
            .irisReveal(iris.value)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconSquareButton(AppIcons.Close, "Cancel", onCancel)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                MonoText(fileName.uppercase(), size = 11)
                ready?.report?.let { r ->
                    MonoText(
                        listOfNotNull(Analyzer.formatSize(r.sizeBytes), if (r.width != null && r.height != null) "${r.width}×${r.height}" else null, r.format.label)
                            .joinToString(" · "),
                        size = 11,
                    )
                }
            }
        }

        if (state is ScanState.Failed) {
            FailedCard(state.message, onRetry)
        } else {
            ScanBody(ready, preview, revealed, total, progress) { revealed = total }
        }
    }
}

@Composable
private fun ScanBody(
    ready: ScanState.Ready?,
    preview: android.graphics.Bitmap?,
    revealed: Int,
    total: Int,
    progress: Float,
    onSkip: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(290.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceHigh)
        ) {
            if (preview != null) {
                Image(preview.asImageBitmap(), contentDescription = "Image being checked", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MonoText("NO PREVIEW FOR THIS FORMAT", size = 11)
                }
            }
            ScanOverlay(active = revealed < total)
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Ink.copy(alpha = 0.8f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val blink = rememberInfiniteTransition(label = "blink")
                val a by blink.animateFloat(0.25f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "a")
                Box(Modifier.size(7.dp).graphicsLayer { alpha = a }.clip(RoundedCornerShape(4.dp)).background(Lime))
                Spacer(Modifier.width(6.dp))
                MonoText(if (revealed < total) "READING FILE…" else "DONE", color = Lime, size = 11)
            }
        }

        HexRibbon(ready?.report?.headerHex ?: "", Modifier.fillMaxWidth())

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Reading hidden labels", style = MaterialTheme.typography.headlineMedium, color = Paper, modifier = Modifier.weight(1f))
                MonoText("$revealed / $total", color = Lime, size = 12)
            }
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Line)) {
                Box(Modifier.fillMaxWidth(progress).height(4.dp).background(Lime))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScanStep.entries.forEachIndexed { i, step ->
                val result = ready?.report?.steps?.firstOrNull { it.step == step }
                StepRow(step, i, revealed, result?.status, result?.detail)
            }
        }

        if (ready != null && revealed < total) {
            SecondaryButton("Show result now", AppIcons.Arrow, onSkip, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StepRow(step: ScanStep, index: Int, revealed: Int, status: StepStatus?, detail: String?) {
    val done = index < revealed && status != null
    val current = index == revealed
    val k by animateFloatAsState(if (index <= revealed) 1f else 0.55f, tween(300), label = "row")
    val border = if (current) Lime else Line
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = k }
            .clip(RoundedCornerShape(16.dp))
            .background(if (index <= revealed) Surface else Ink)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            done && status == StepStatus.FOUND -> Icon(AppIcons.Check, null, tint = Lime, modifier = Modifier.size(20.dp))
            done -> Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { Box(Modifier.width(10.dp).height(2.dp).background(Dim)) }
            current -> Spinner()
            else -> Box(Modifier.size(20.dp).border(1.5.dp, Dim, RoundedCornerShape(10.dp)))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(step.title, style = MaterialTheme.typography.titleMedium, color = if (index <= revealed) Paper else Muted)
            MonoText(if (done && detail != null) detail else step.subtitle, size = 11, modifier = Modifier)
        }
        Spacer(Modifier.width(8.dp))
        val (label, color) = when {
            done && status == StepStatus.FOUND -> "FOUND" to AiOrange
            done -> "NONE" to Muted
            current -> "READING" to Lime
            else -> "QUEUED" to Muted
        }
        MonoText(label, color = color, size = 11)
    }
}

@Composable
private fun Spinner() {
    val t = rememberInfiniteTransition(label = "spin")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "angle")
    androidx.compose.foundation.Canvas(Modifier.size(20.dp)) {
        drawArc(Lime, angle, 270f, false, style = Stroke(2.2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

@Composable
private fun FailedCard(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, Danger.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Warning, null, tint = Danger, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Couldn't read this file", style = MaterialTheme.typography.titleLarge, color = Paper)
        }
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Muted, maxLines = 6, overflow = TextOverflow.Ellipsis)
        PrimaryButton("Try another image", AppIcons.Image, onRetry, Modifier.fillMaxWidth())
    }
}
