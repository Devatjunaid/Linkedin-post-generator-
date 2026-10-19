package com.verba.subtitles.ui.processing

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.verba.subtitles.AppContainer
import com.verba.subtitles.ads.NativeAdCard
import com.verba.subtitles.ui.components.EqualizerBars
import com.verba.subtitles.ui.components.PillButton
import com.verba.subtitles.ui.components.SectionLabel
import com.verba.subtitles.ui.components.Thumbnail
import com.verba.subtitles.ui.components.TopBar
import com.verba.subtitles.ui.theme.Verba

@Composable
fun ProcessingScreen(container: AppContainer, projectId: String, onBack: () -> Unit, onDone: () -> Unit) {
    val vm: ProcessingViewModel = viewModel(
        key = "processing-$projectId",
        factory = viewModelFactory { initializer { ProcessingViewModel(container, projectId) } },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmLeave by remember { mutableStateOf(false) }
    var continued by remember { mutableStateOf(false) }
    val continueOnce = {
        if (!continued) {
            continued = true
            onDone()
        }
    }

    // Keep the screen awake while the phone is working.
    val view = LocalView.current
    DisposableEffect(ui.done, ui.error) {
        view.keepScreenOn = !ui.done && ui.error == null
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(enabled = !ui.done && ui.error == null) { confirmLeave = true }

    Column(Modifier.fillMaxSize().background(Verba.Ink).statusBarsPadding().navigationBarsPadding()) {
        TopBar(
            title = when {
                ui.error != null -> "Couldn't finish"
                ui.done -> "Subtitles ready"
                else -> "Creating subtitles…"
            },
            subtitle = ui.projectName.ifBlank { null },
            onBack = { if (ui.done || ui.error != null) onBack() else confirmLeave = true },
        ) {
            if (ui.language.isNotBlank()) {
                Text(
                    ui.language,
                    style = MaterialTheme.typography.labelMedium,
                    color = Verba.Muted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Verba.Surface)
                        .border(1.dp, Verba.Line, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ClipScanner(ui)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ProgressRing(ui.overall, ui.done, Modifier.size(132.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ui.steps.forEach { StepRow(it) }
                }
            }
            TranscriptCard(ui)
            if (ui.error != null) {
                ErrorCard(ui.error.orEmpty(), onRetry = vm::retry, onBack = onBack)
            }
            NativeAdCard(container.ads)
            Spacer(Modifier.height(4.dp))
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Verba.Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                SectionLabel("On-device · no uploads · no credits")
            }
            AnimatedVisibility(
                visible = ui.done,
                enter = fadeIn() + scaleIn(spring(dampingRatio = 0.55f), initialScale = 0.8f) + slideInVertically { it / 2 },
            ) {
                PillButton(
                    text = "Open editor · ${ui.captionCount} captions",
                    onClick = continueOnce,
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                )
            }
        }
    }

    // Auto-continue shortly after finishing so the flow feels instant.
    LaunchedEffect(ui.done) {
        if (ui.done) {
            kotlinx.coroutines.delay(1_400)
            continueOnce()
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Stop creating subtitles?") },
            text = { Text("The project stays in your list as a draft. You can run it again later.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    onBack()
                }) { Text("Stop", color = Verba.Coral) }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Keep going") } },
            containerColor = Verba.Surface,
        )
    }
}

@Composable
private fun ClipScanner(ui: ProcessingUi) {
    val transition = rememberInfiniteTransition(label = "scan")
    val scan by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2_600, easing = LinearEasing)), label = "x")
    val working = !ui.done && ui.error == null
    Box(
        Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Verba.Line, RoundedCornerShape(24.dp)),
    ) {
        Thumbnail(ui.thumbnail, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Verba.Ink.copy(alpha = 0.35f)))
        if (working) {
            Canvas(Modifier.fillMaxSize()) {
                val x = -40f + (size.width + 80f) * scan
                drawRect(Verba.Lime.copy(alpha = 0.12f), topLeft = Offset(x - 60f, 0f), size = androidx.compose.ui.geometry.Size(60f, size.height))
                drawLine(Verba.Lime, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
            }
        }
        EqualizerBars(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            active = working,
        )
    }
}

@Composable
private fun ProgressRing(progress: Float, done: Boolean, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress, tween(400), label = "ring")
    val transition = rememberInfiniteTransition(label = "ringSpin")
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "spin")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            rotate(spin) {
                drawCircle(
                    Verba.Coral,
                    radius = size.minDimension / 2f - 2.dp.toPx(),
                    style = Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 14f))),
                )
            }
            val stroke = 10.dp.toPx()
            val inset = 12.dp.toPx()
            val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(Verba.SurfaceTop, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(Verba.Lime, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (done) {
                Icon(Icons.Rounded.Check, contentDescription = "Done", tint = Verba.Lime, modifier = Modifier.size(40.dp))
            } else {
                Text("${(animated * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium)
            }
            SectionLabel("on-device")
        }
    }
}

@Composable
private fun StepRow(step: StepUi) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            when (step.status) {
                StepStatus.DONE -> Box(Modifier.size(22.dp).clip(CircleShape).background(Verba.Lime), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Verba.Ink, modifier = Modifier.size(14.dp))
                }
                StepStatus.RUNNING -> Spinner()
                StepStatus.ERROR -> Box(Modifier.size(22.dp).clip(CircleShape).background(Verba.Coral), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PriorityHigh, contentDescription = null, tint = Verba.Ink, modifier = Modifier.size(14.dp))
                }
                StepStatus.PENDING -> Box(Modifier.size(18.dp).border(2.dp, Verba.SurfaceTop, CircleShape))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(step.id.title, style = MaterialTheme.typography.titleSmall)
            if (step.detail.isNotBlank()) {
                Text(step.detail, style = MaterialTheme.typography.labelSmall, color = Verba.Muted)
            }
        }
    }
}

@Composable
private fun Spinner() {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "a")
    Canvas(Modifier.size(18.dp)) {
        drawArc(Verba.SurfaceTop, 0f, 360f, false, style = Stroke(2.5.dp.toPx()))
        drawArc(Verba.Lime, angle, 90f, false, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun TranscriptCard(ui: ProcessingUi) {
    val words = remember(ui.transcript) { ui.transcript.split(' ').filter { it.isNotBlank() }.takeLast(60) }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Verba.Surface)
            .border(1.dp, Verba.Line, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel("Live transcript")
        if (words.isEmpty() && ui.partial.isBlank()) {
            Text(
                if (ui.done || ui.error != null) "—" else "Words appear here as Verba hears them…",
                style = MaterialTheme.typography.bodyLarge,
                color = Verba.Muted,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                words.forEach { word ->
                    Text(word, style = MaterialTheme.typography.titleMedium)
                }
                if (ui.partial.isNotBlank()) {
                    Text(
                        ui.partial,
                        style = MaterialTheme.typography.titleMedium,
                        color = Verba.Ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Verba.Lime)
                            .padding(horizontal = 7.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Verba.Coral.copy(alpha = 0.12f))
            .border(1.dp, Verba.Coral.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Back", onBack, Modifier.weight(1f), primary = false, height = 46.dp)
            PillButton("Try again", onRetry, Modifier.weight(1f), height = 46.dp)
        }
    }
}
