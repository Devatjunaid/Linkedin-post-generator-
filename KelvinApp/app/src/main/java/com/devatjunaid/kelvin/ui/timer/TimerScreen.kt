package com.devatjunaid.kelvin.ui.timer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devatjunaid.kelvin.data.Settings
import com.devatjunaid.kelvin.domain.Phase
import com.devatjunaid.kelvin.domain.PhaseType
import com.devatjunaid.kelvin.domain.SessionKind
import com.devatjunaid.kelvin.domain.SessionPlan
import com.devatjunaid.kelvin.domain.SessionRecord
import com.devatjunaid.kelvin.domain.Units
import com.devatjunaid.kelvin.ui.components.CircleIconButton
import com.devatjunaid.kelvin.ui.components.HeatFill
import com.devatjunaid.kelvin.ui.components.IconBadge
import com.devatjunaid.kelvin.ui.components.KIcons
import com.devatjunaid.kelvin.ui.components.RestFill
import com.devatjunaid.kelvin.ui.components.WaterFill
import com.devatjunaid.kelvin.ui.components.pressable
import com.devatjunaid.kelvin.ui.theme.K
import com.devatjunaid.kelvin.ui.theme.KType

@Composable
fun TimerScreen(
    plan: SessionPlan,
    sessionId: Long,
    settings: Settings,
    onSave: (SessionRecord) -> Unit,
    onDiscard: () -> Unit,
    vm: TimerViewModel = viewModel(),
) {
    LaunchedEffect(sessionId) { vm.begin(sessionId, plan, settings.sound, settings.vibration) }
    val rawUi by vm.ui.collectAsStateWithLifecycle()
    // Ignore state left over from a previous session until this one has published.
    val ui = if (rawUi.sessionId == sessionId) rawUi else TimerUi()
    var soundOn by remember { mutableStateOf(settings.sound) }
    LaunchedEffect(soundOn, settings.vibration) { vm.updateCuePrefs(soundOn, settings.vibration) }

    var confirmEnd by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(ui.finished) { if (ui.finished) showSummary = true }

    // The screen stays awake for the whole session so the timer is always visible.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    BackHandler {
        when {
            showSummary -> Unit
            ui.finished -> showSummary = true
            else -> confirmEnd = true
        }
    }

    val phase = ui.phase ?: plan.phases.first()
    val accent = accentFor(phase.type)

    // Full-screen flash + label when a new phase begins.
    val flash = remember { Animatable(0f) }
    LaunchedEffect(ui.index) {
        if (ui.index > 0) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(1100, easing = FastOutSlowInEasing))
        }
    }

    Box(Modifier.fillMaxSize().background(K.InkDeep)) {
        Crossfade(targetState = phase.type, animationSpec = tween(900), label = "phaseBg") { type ->
            when (type) {
                PhaseType.COLD -> WaterFill(level = 0.22f + 0.5f * ui.phaseProgress)
                PhaseType.HEAT -> HeatFill(level = 0.25f + 0.45f * ui.phaseProgress)
                PhaseType.REST -> RestFill()
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(KIcons.Close, "End session", { if (ui.finished) showSummary = true else confirmEnd = true })
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(phaseLabel(phase, ui.roundsTotal, plan.kind), style = KType.Label.copy(color = accent))
                    Text(plan.title, style = KType.Heading.copy(fontSize = 17.sp))
                }
                CircleIconButton(
                    if (soundOn) KIcons.Volume else KIcons.Vibrate,
                    if (soundOn) "Mute sound cues" else "Turn sound cues on",
                    { soundOn = !soundOn },
                )
            }
            Spacer(Modifier.height(16.dp))
            SegmentBar(ui.phases, ui.segments)

            Spacer(Modifier.weight(1f))
            TimerRing(ui = ui, phase = phase, fahrenheit = settings.fahrenheit)
            Spacer(Modifier.height(18.dp))
            if (settings.breathingGuide && !ui.finished) BreathingGuide(phase.type, ui.phaseElapsedMs)
            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundControl(onClick = vm::addThirtySeconds, label = "Add 30 seconds", enabled = !ui.finished) {
                    Text("+30s", style = KType.Label.copy(color = K.Text, fontSize = 13.sp, letterSpacing = 0.sp))
                }
                Box(
                    Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(K.Text)
                        .pressable(onClick = { if (ui.finished) showSummary = true else vm.toggle() }, label = if (ui.running) "Pause" else "Resume"),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = when {
                            ui.finished -> 2
                            ui.running -> 0
                            else -> 1
                        },
                        transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
                        label = "playPause",
                    ) { state ->
                        Icon(
                            when (state) {
                                0 -> KIcons.Pause
                                1 -> KIcons.Play
                                else -> KIcons.Check
                            },
                            contentDescription = null,
                            tint = K.InkDeep,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                RoundControl(onClick = vm::skipPhase, label = "Finish this phase", enabled = !ui.finished) {
                    Icon(KIcons.Skip, contentDescription = null, tint = K.Text, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            UpNext(ui.nextPhase, settings.fahrenheit)
        }

        if (flash.value > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = flash.value }
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.55f), Color.Transparent))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    phaseWord(phase.type),
                    style = KType.Hero.copy(fontSize = 56.sp, color = K.Text),
                    modifier = Modifier.graphicsLayer {
                        val s = 1.4f - 0.4f * flash.value
                        scaleX = s
                        scaleY = s
                    },
                )
            }
        }
    }

    if (confirmEnd) {
        EndSessionDialog(
            hasProgress = vm.actualTotalSeconds() > 0 || ui.phaseElapsedMs > 1000,
            onKeepGoing = { confirmEnd = false },
            onSave = {
                confirmEnd = false
                vm.finishNow()
                showSummary = true
            },
            onDiscard = {
                confirmEnd = false
                vm.end()
                onDiscard()
            },
        )
    }

    if (showSummary) {
        SessionSummaryDialog(
            totalSec = vm.actualTotalSeconds(),
            coldSec = vm.actualColdSeconds(),
            heatSec = vm.actualHeatSeconds(),
            rounds = vm.roundsCompleted(),
            initialColdTemp = plan.coldTempC,
            initialHeatTemp = plan.heatTempC,
            fahrenheit = settings.fahrenheit,
            onSave = { mood, note, coldT, heatT ->
                if (saved) return@SessionSummaryDialog
                saved = true
                val record = vm.record(mood, note, coldT, heatT)
                vm.end()
                if (record != null) onSave(record) else onDiscard()
            },
            onDiscard = {
                vm.end()
                onDiscard()
            },
        )
    }
}

@Composable
private fun SegmentBar(phases: List<Phase>, segments: List<Float>) {
    Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        phases.forEachIndexed { i, p ->
            val color = accentFor(p.type)
            Box(
                Modifier
                    .weight(p.durationSec.coerceAtLeast(30).toFloat())
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color.copy(alpha = 0.2f)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(segments.getOrElse(i) { 0f })
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun TimerRing(ui: TimerUi, phase: Phase, fahrenheit: Boolean) {
    val loop = rememberInfiniteTransition(label = "ring")
    val ripple by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(3_000, easing = LinearEasing)), label = "ripple")
    val spin by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(30_000, easing = LinearEasing)), label = "spin")
    val progress by animateFloatAsState(1f - ui.phaseProgress, tween(150, easing = LinearEasing), label = "progress")
    val accent = accentFor(phase.type)
    val brush = when (phase.type) {
        PhaseType.COLD -> listOf(K.IceLight, K.Ice, K.IceDeep)
        PhaseType.HEAT -> listOf(K.Gold, K.Ember, K.EmberDeep)
        PhaseType.REST -> listOf(Color(0xFFE6ECF2), K.Muted, K.Rest)
    }
    val remainingSec = ((ui.remainingMs + 999) / 1000).toInt()
    Box(
        Modifier
            .size(280.dp)
            .semantics {
                contentDescription = "${phaseWord(phase.type)} phase, ${Units.clock(remainingSec)} remaining"
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension / 2f
            if (ui.running) {
                repeat(2) { i ->
                    val local = (ripple + i * 0.5f) % 1f
                    drawCircle(accent.copy(alpha = 0.45f * (1f - local)), r * (0.85f + 0.3f * local), c, style = Stroke(1.5.dp.toPx()))
                }
            }
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.18f), Color(0xF2070B10)), c + Offset(0f, -r * 0.3f), r), r * 0.86f, c)
            val trackW = 10.dp.toPx()
            val arcR = r - trackW
            drawCircle(Color.White.copy(alpha = 0.08f), arcR, c, style = Stroke(trackW))
            rotate(spin, c) {
                repeat(60) { i ->
                    val a = Math.toRadians(i * 6.0)
                    val dir = Offset(kotlin.math.cos(a).toFloat(), kotlin.math.sin(a).toFloat())
                    val outer = arcR - trackW * 1.6f
                    drawLine(Color.White.copy(alpha = if (i % 5 == 0) 0.3f else 0.12f), c + dir * outer, c + dir * (outer - if (i % 5 == 0) 12f else 6f), 2f, StrokeCap.Round)
                }
            }
            drawArc(
                Brush.sweepGradient(brush + brush.first(), c),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(c.x - arcR, c.y - arcR),
                size = Size(arcR * 2, arcR * 2),
                style = Stroke(trackW, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                when {
                    ui.finished -> "COMPLETE"
                    ui.running -> "REMAINING"
                    else -> "PAUSED"
                },
                style = KType.Label.copy(color = Color(0xFF9FB6C6)),
            )
            Text(Units.clock(remainingSec), style = KType.MonoHuge.copy(fontSize = 66.sp))
            Text(
                buildString {
                    append("of ${Units.clock((ui.phaseDurationMs / 1000).toInt())}")
                    phase.tempC?.let { append(" · ${Units.temp(it, fahrenheit)}") }
                },
                style = KType.Small.copy(color = Color(0xFF9FB6C6)),
            )
        }
    }
}

@Composable
private fun BreathingGuide(type: PhaseType, phaseElapsedMs: Long) {
    val sec = phaseElapsedMs / 1000
    val text = when (type) {
        PhaseType.COLD -> if (sec < 30) "Settle in · long, slow exhales" else if ((phaseElapsedMs % 10_000) < 4_000) "Inhale · 4s" else "Exhale · 6s"
        PhaseType.HEAT -> "Breathe easy · stay relaxed"
        PhaseType.REST -> "Recover · let your heart rate settle"
    }
    val t = (phaseElapsedMs % 10_000) / 10_000f
    val target = if (type != PhaseType.COLD) 0.9f else if (t < 0.4f) 0.7f + 0.45f * (t / 0.4f) else 1.15f - 0.45f * ((t - 0.4f) / 0.6f)
    val scale by animateFloatAsState(target, tween(200), label = "breath")
    val accent = accentFor(type)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(24.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White, accent, accent.copy(alpha = 0f)))),
        )
        AnimatedContent(targetState = text, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) }, label = "breathText") {
            Text(it, style = KType.BodyStrong)
        }
    }
}

@Composable
private fun RoundControl(onClick: () -> Unit, label: String, enabled: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(60.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(CircleShape)
            .background(Color(0x99101C28))
            .border(1.dp, Color(0x24FFFFFF), CircleShape)
            .pressable(onClick = onClick, enabled = enabled, label = label),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun UpNext(next: Phase?, fahrenheit: Boolean) {
    val accent = next?.let { accentFor(it.type) } ?: K.Gold
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xB3101820))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(
            when (next?.type) {
                PhaseType.COLD -> KIcons.Snowflake
                PhaseType.HEAT -> KIcons.Flame
                PhaseType.REST -> KIcons.Breath
                null -> KIcons.Star
            },
            accent,
        )
        Column(Modifier.weight(1f)) {
            Text("UP NEXT", style = KType.Label.copy(color = accent))
            Text(
                if (next == null) "Finish line — log how you feel"
                else buildString {
                    append(phaseWord(next.type).lowercase().replaceFirstChar { it.uppercase() })
                    next.tempC?.let { append(" · ${Units.temp(it, fahrenheit)}") }
                    append(" · ${Units.clock(next.durationSec)}")
                },
                style = KType.BodyStrong,
            )
        }
    }
}

internal fun accentFor(type: PhaseType): Color = when (type) {
    PhaseType.COLD -> K.Ice
    PhaseType.HEAT -> K.Ember
    PhaseType.REST -> Color(0xFFB6C2CE)
}

private fun phaseWord(type: PhaseType): String = when (type) {
    PhaseType.COLD -> "COLD"
    PhaseType.HEAT -> "HEAT"
    PhaseType.REST -> "REST"
}

private fun phaseLabel(phase: Phase, rounds: Int, kind: SessionKind): String =
    if (kind == SessionKind.CONTRAST) "ROUND ${phase.round} / $rounds · ${phaseWord(phase.type)}" else "${phaseWord(phase.type)} SESSION"
