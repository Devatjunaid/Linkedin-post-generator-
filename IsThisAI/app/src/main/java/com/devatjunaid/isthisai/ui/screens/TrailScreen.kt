package com.devatjunaid.isthisai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devatjunaid.isthisai.ads.NativeAdCard
import com.devatjunaid.isthisai.ads.NativeStyle
import com.devatjunaid.isthisai.engine.RawSection
import com.devatjunaid.isthisai.engine.TrailKind
import com.devatjunaid.isthisai.engine.TrailStep
import com.devatjunaid.isthisai.ui.OpenScan
import com.devatjunaid.isthisai.ui.components.AppIcons
import com.devatjunaid.isthisai.ui.components.IconSquareButton
import com.devatjunaid.isthisai.ui.components.MonoText
import com.devatjunaid.isthisai.ui.components.Pill
import com.devatjunaid.isthisai.ui.components.PrimaryButton
import com.devatjunaid.isthisai.ui.components.SecondaryButton
import com.devatjunaid.isthisai.ui.theme.AiOrange
import com.devatjunaid.isthisai.ui.theme.CameraBlue
import com.devatjunaid.isthisai.ui.theme.Danger
import com.devatjunaid.isthisai.ui.theme.Dim
import com.devatjunaid.isthisai.ui.theme.Ink
import com.devatjunaid.isthisai.ui.theme.Lime
import com.devatjunaid.isthisai.ui.theme.Line
import com.devatjunaid.isthisai.ui.theme.LineStrong
import com.devatjunaid.isthisai.ui.theme.Muted
import com.devatjunaid.isthisai.ui.theme.Paper
import com.devatjunaid.isthisai.ui.theme.Surface
import com.devatjunaid.isthisai.ui.theme.SurfaceHigh

@Composable
fun TrailScreen(
    scan: OpenScan?,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    if (scan == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MonoText("LOADING…") }
    } else {
        TrailContent(scan, onBack, onCopy, onSave, onShare)
    }
}

@Composable
private fun TrailContent(scan: OpenScan, onBack: () -> Unit, onCopy: () -> Unit, onSave: () -> Unit, onShare: () -> Unit) {
    val report = scan.report
    var rawTab by rememberSaveable { mutableStateOf(false) }

    // "Card expands into a sheet" entrance.
    val sheet = remember { Animatable(0f) }
    LaunchedEffect(Unit) { sheet.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }

    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val k = sheet.value
                scaleX = 0.92f + 0.08f * k
                scaleY = 0.92f + 0.08f * k
                translationY = (1f - k) * 120f
                alpha = 0.3f + 0.7f * k
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconSquareButton(AppIcons.Back, "Back to verdict", onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Provenance trail", style = MaterialTheme.typography.headlineMedium, color = Paper)
                MonoText("${report.fileName} · ${report.trail.size} steps", size = 11)
            }
            IconSquareButton(AppIcons.Share, "Share report", onShare)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Surface)
                .border(1.dp, Line, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Tab("Trail", !rawTab, Modifier.weight(1f)) { rawTab = false }
            Tab("Raw fields", rawTab, Modifier.weight(1f)) { rawTab = true }
        }

        if (!rawTab) {
            Timeline(report.trail)
        } else {
            report.raw.forEachIndexed { i, s -> RawCard(s, initiallyOpen = i < 2) }
        }

        NativeAdCard(NativeStyle.SMALL)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Copy", AppIcons.Copy, onCopy, Modifier.weight(1f), height = 58.dp)
            PrimaryButton("Save report", AppIcons.Download, onSave, Modifier.weight(1.4f))
        }
    }
}

@Composable
private fun Tab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Paper else Color.Transparent, tween(250), label = "tabBg")
    val fg by animateColorAsState(if (selected) Ink else Muted, tween(250), label = "tabFg")
    Box(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = fg)
    }
}

private fun trailColor(k: TrailKind): Color = when (k) {
    TrailKind.AI -> AiOrange
    TrailKind.CAPTURE -> CameraBlue
    TrailKind.SIGNED -> Lime
    TrailKind.WARNING -> Danger
    TrailKind.EDIT -> Muted
    TrailKind.DEVICE, TrailKind.INFO -> Dim
}

/** The timeline line draws downward, and each step fades in after the one before it. */
@Composable
private fun Timeline(steps: List<TrailStep>) {
    val progress = remember(steps) { Animatable(0f) }
    LaunchedEffect(steps) { progress.animateTo(1f, tween(400 + 320 * steps.size, easing = FastOutSlowInEasing)) }
    Column {
        steps.forEachIndexed { i, step ->
            val start = i / steps.size.toFloat()
            val k = ((progress.value - start) * steps.size).coerceIn(0f, 1f)
            val color = trailColor(step.kind)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(Modifier.width(34.dp).fillMaxHeight()) {
                    Canvas(Modifier.fillMaxSize()) {
                        val x = 12.dp.toPx()
                        val top = 18.dp.toPx()
                        if (i < steps.lastIndex) {
                            val endY = top + (size.height - top) * k
                            drawLine(LineStrong, Offset(x, top), Offset(x, endY), 2.dp.toPx())
                        }
                        if (i > 0) drawLine(LineStrong, Offset(x, 0f), Offset(x, top), 2.dp.toPx())
                        val r = 11.dp.toPx() * (0.6f + 0.4f * k)
                        drawCircle(Ink, r, Offset(x, top))
                        drawCircle(color.copy(alpha = k), r, Offset(x, top), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                        if (step.kind != TrailKind.DEVICE && step.kind != TrailKind.INFO) drawCircle(color.copy(alpha = k), 4.dp.toPx(), Offset(x, top))
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(bottom = 12.dp)
                        .graphicsLayer { alpha = k; translationX = (1f - k) * -24f }
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (step.kind == TrailKind.DEVICE) Ink else Surface)
                        .border(1.dp, if (step.kind == TrailKind.WARNING) Danger.copy(alpha = 0.6f) else Line, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(step.title, style = MaterialTheme.typography.titleMedium, color = Paper, modifier = Modifier.weight(1f))
                        when (step.kind) {
                            TrailKind.AI -> Pill("AI", AiOrange)
                            TrailKind.SIGNED -> Pill("Signed", Lime)
                            TrailKind.CAPTURE -> Pill("Capture", CameraBlue)
                            TrailKind.WARNING -> Pill("Check", Danger)
                            else -> Unit
                        }
                    }
                    if (step.detail.isNotBlank()) MonoText(step.detail, size = 11)
                    step.tool?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
    }
}

@Composable
private fun RawCard(section: RawSection, initiallyOpen: Boolean) {
    var open by rememberSaveable(section.title) { mutableStateOf(initiallyOpen) }
    val rotation by androidx.compose.animation.core.animateFloatAsState(if (open) 90f else 0f, tween(250), label = "chev")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { open = !open }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Chevron, null, tint = CameraBlue, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = rotation })
            Spacer(Modifier.width(8.dp))
            Text(section.title, style = MaterialTheme.typography.titleMedium, color = Paper, modifier = Modifier.weight(1f))
            MonoText("${section.entries.size}", size = 11)
        }
        AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(SurfaceHigh.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                section.entries.forEach { (k, v) ->
                    Column {
                        MonoText(k, color = Muted, size = 10, bold = true)
                        MonoText(v, color = Paper, size = 12)
                    }
                }
            }
        }
    }
}
