package com.devatjunaid.examclock.ui.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.ui.appViewModel
import com.devatjunaid.examclock.ui.components.Format
import com.devatjunaid.examclock.ui.components.LightSystemBarIcons
import com.devatjunaid.examclock.ui.theme.Brand
import com.devatjunaid.examclock.ui.theme.Display

@Composable
fun FocusScreen(sessionId: Long?, onFinished: () -> Unit) {
    val context = LocalContext.current
    val vm = appViewModel(key = "focus-${sessionId ?: 0L}") { FocusViewModel(it, context, sessionId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val completed by vm.blockCompleted.collectAsStateWithLifecycle()
    LightSystemBarIcons()

    if (state.running && state.prefs.keepScreenOn) KeepScreenOn()

    val ringColor = if (state.isBreak) Brand.BlueLight else Brand.OrangeLight
    val progress by animateFloatAsState(
        if (state.totalMillis == 0L) 0f else state.remainingMillis.toFloat() / state.totalMillis,
        label = "ring",
    )
    val remainingSeconds = (state.remainingMillis + 999) / 1000
    val clock = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
    val idle = !state.running && !state.paused

    Column(
        Modifier
            .fillMaxSize()
            .background(Brand.Ink)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            pluralStringResource(R.plurals.focus_blocks_done, state.timer.completedBlocks, state.timer.completedBlocks),
            style = MaterialTheme.typography.labelLarge,
            color = Brand.OnInkMuted,
        )
        Surface(shape = RoundedCornerShape(12.dp), color = if (state.isBreak) Color(0xFF22305F) else Color(0xFF3B2A1E)) {
            Text(
                stringResource(if (state.isBreak) R.string.focus_phase_break else R.string.focus_phase_focus),
                color = ringColor,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Text(
            state.linkedLabel ?: stringResource(R.string.focus_free_study),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        val description = stringResource(R.string.cd_timer_remaining, remainingSeconds / 60, remainingSeconds % 60)
        Box(
            Modifier
                .size(270.dp)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 14.dp.toPx()
                val inset = stroke / 2
                val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                drawArc(Brand.InkLine, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                drawArc(ringColor, -90f, 360f * progress, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(clock, fontFamily = Display, fontSize = 64.sp, color = Color.White)
                Text(
                    stringResource(if (state.paused) R.string.focus_paused else R.string.focus_remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Brand.OnInkMuted,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = { vm.finish(onFinished) },
                enabled = !idle || state.timer.completedBlocks > 0,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Brand.InkRaised, contentColor = Color.White),
                modifier = Modifier.size(56.dp),
            ) { Icon(Icons.Rounded.Stop, contentDescription = stringResource(R.string.focus_finish)) }
            FilledIconButton(
                onClick = {
                    when {
                        state.running -> vm.pause()
                        state.paused -> vm.resume()
                        else -> vm.start()
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Brand.Ink),
                modifier = Modifier.size(84.dp),
            ) {
                Icon(
                    if (state.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(
                        when {
                            state.running -> R.string.focus_pause
                            state.paused -> R.string.focus_resume
                            state.isBreak -> R.string.focus_start_break
                            else -> R.string.focus_start
                        },
                    ),
                    modifier = Modifier.size(36.dp),
                )
            }
            FilledIconButton(
                onClick = vm::skip,
                enabled = !idle || state.isBreak,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Brand.InkRaised, contentColor = Color.White),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = stringResource(if (state.isBreak) R.string.focus_skip_break else R.string.focus_end_block),
                )
            }
        }

        if (idle && !state.isBreak && state.options.isNotEmpty()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.focus_what), style = MaterialTheme.typography.labelLarge, color = Brand.OnInkMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        containerColor = Brand.InkRaised,
                        labelColor = Color.White,
                        selectedContainerColor = Brand.BlueLight,
                        selectedLabelColor = Brand.Ink,
                    )
                    FilterChip(
                        selected = state.selectedSessionId == 0L,
                        onClick = { vm.selectSession(null) },
                        label = { Text(stringResource(R.string.focus_free_study)) },
                        colors = chipColors,
                    )
                    state.options.forEach { option ->
                        FilterChip(
                            selected = state.selectedSessionId == option.sessionId,
                            onClick = { vm.selectSession(option) },
                            label = { Text(option.label, maxLines = 1) },
                            colors = chipColors,
                        )
                    }
                }
            }
        }

        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1E2027), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(
                        R.string.focus_settings_summary,
                        Format.minutes(context, state.prefs.focusMinutes),
                        Format.minutes(context, state.prefs.shortBreakMinutes),
                        Format.minutes(context, state.prefs.longBreakMinutes),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
                Text(stringResource(R.string.focus_settings_hint), style = MaterialTheme.typography.bodySmall, color = Brand.OnInkMuted)
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    completed?.let { blocks ->
        AlertDialog(
            onDismissRequest = vm::consumeEvent,
            icon = { Box(Modifier.size(12.dp).background(Brand.OrangeLight, CircleShape)) },
            title = { Text(stringResource(R.string.focus_block_done_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.focus_block_done_body,
                        Format.minutes(context, state.prefs.focusMinutes),
                        Format.minutes(context, if (blocks % state.prefs.blocksBeforeLongBreak.coerceAtLeast(1) == 0) state.prefs.longBreakMinutes else state.prefs.shortBreakMinutes),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.consumeEvent()
                    vm.start()
                }) { Text(stringResource(R.string.focus_start_break)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.consumeEvent()
                    vm.finish(onFinished)
                }) { Text(stringResource(R.string.focus_finish)) }
            },
        )
    }
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
