package com.devatjunaid.examclock.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.ui.appViewModel
import com.devatjunaid.examclock.ui.components.AppCard
import com.devatjunaid.examclock.ui.components.Format
import com.devatjunaid.examclock.ui.components.ScreenScaffold
import com.devatjunaid.examclock.ui.exams.TimePickerDialog
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun AutoPlanScreen(onBack: () -> Unit, onViewPlan: () -> Unit) {
    val vm = appViewModel { AutoPlanViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editStart by remember { mutableStateOf(false) }
    var editEnd by remember { mutableStateOf(false) }
    val plan = state.plan

    ScreenScaffold(title = stringResource(R.string.autoplan_title), onBack = onBack) { padding ->
        if (!state.loaded) return@ScreenScaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.autoplan_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (state.datedExamCount == 0) {
                AppCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(stringResource(R.string.autoplan_no_dates), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            } else if (state.undatedExamCount > 0) {
                Text(
                    pluralStringResource(R.plurals.autoplan_undated, state.undatedExamCount, state.undatedExamCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            AppCard {
                MinutesStepper(stringResource(R.string.autoplan_weekday), plan.weekdayMinutes) { v -> vm.update { it.copy(weekdayMinutes = v) } }
                MinutesStepper(stringResource(R.string.autoplan_weekend), plan.weekendMinutes) { v -> vm.update { it.copy(weekendMinutes = v) } }
                val weekend = state.weekendDays.sorted().joinToString(", ") { it.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
                Text(stringResource(R.string.autoplan_weekend_note, weekend), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(stringResource(R.string.autoplan_rest_days), style = MaterialTheme.typography.labelLarge)
            val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0L until 7L).map { first.plus(it) }.forEach { day ->
                    FilterChip(
                        selected = day in plan.restDays,
                        onClick = { vm.toggleRestDay(day) },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                    )
                }
            }

            Text(stringResource(R.string.autoplan_session_length), style = MaterialTheme.typography.labelLarge)
            val lengths = listOf(25, 45, 60, 90)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                lengths.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = plan.sessionMinutes == m,
                        onClick = { vm.update { it.copy(sessionMinutes = m) } },
                        shape = SegmentedButtonDefaults.itemShape(i, lengths.size),
                    ) { Text(Format.minutes(context, m)) }
                }
            }

            Text(stringResource(R.string.autoplan_break), style = MaterialTheme.typography.labelLarge)
            val breaks = listOf(5, 10, 15)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                breaks.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = plan.breakMinutes == m,
                        onClick = { vm.update { it.copy(breakMinutes = m) } },
                        shape = SegmentedButtonDefaults.itemShape(i, breaks.size),
                    ) { Text(Format.minutes(context, m)) }
                }
            }

            Text(stringResource(R.string.autoplan_window), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { editStart = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.autoplan_from, Format.time(context, minuteToTime(plan.dayStartMinute))))
                }
                OutlinedButton(onClick = { editEnd = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.autoplan_until, Format.time(context, minuteToTime(plan.dayEndMinute))))
                }
            }

            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.autoplan_papers), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.autoplan_papers_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    CountStepper(plan.pastPaperDays, 0, 7) { v -> vm.update { it.copy(pastPaperDays = v) } }
                }
            }

            Button(
                onClick = vm::generate,
                enabled = state.datedExamCount > 0 && !state.generating && plan.weekdayMinutes + plan.weekendMinutes > 0,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                if (state.generating) {
                    CircularProgressIndicator(Modifier.width(22.dp).height(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.autoplan_generate))
                }
            }
            Text(stringResource(R.string.autoplan_keeps_manual), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }

    if (editStart) {
        TimePickerDialog(minuteToTime(plan.dayStartMinute), onDismiss = { editStart = false }) { t ->
            val minute = t.hour * 60 + t.minute
            vm.update { it.copy(dayStartMinute = minute, dayEndMinute = maxOf(it.dayEndMinute, minute + 60).coerceAtMost(23 * 60 + 59)) }
            editStart = false
        }
    }
    if (editEnd) {
        TimePickerDialog(minuteToTime(plan.dayEndMinute), onDismiss = { editEnd = false }) { t ->
            val minute = t.hour * 60 + t.minute
            vm.update { it.copy(dayEndMinute = maxOf(minute, it.dayStartMinute + 30)) }
            editEnd = false
        }
    }

    state.result?.let { result ->
        AlertDialog(
            onDismissRequest = vm::dismissResult,
            title = { Text(stringResource(R.string.autoplan_done_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        pluralStringResource(R.plurals.autoplan_done_body, result.sessions.size, result.sessions.size, Format.minutes(context, result.totalMinutes)),
                    )
                    result.uncoveredTopics.forEach { (examId, count) ->
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Text(
                                pluralStringResource(R.plurals.autoplan_uncovered, count, count, state.examTitles[examId].orEmpty()),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.dismissResult()
                    onViewPlan()
                }) { Text(stringResource(R.string.action_view_plan)) }
            },
        )
    }
}

private fun minuteToTime(minute: Int): LocalTime = LocalTime.of((minute / 60).coerceIn(0, 23), minute % 60)

@Composable
private fun MinutesStepper(label: String, value: Int, onChange: (Int) -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        OutlinedIconButton(onClick = { onChange((value - 15).coerceAtLeast(0)) }, enabled = value > 0) {
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.cd_less_time, label))
        }
        Text(Format.minutes(context, value), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, modifier = Modifier.width(80.dp))
        OutlinedIconButton(onClick = { onChange((value + 15).coerceAtMost(720)) }, enabled = value < 720) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_more_time, label))
        }
    }
}

@Composable
private fun CountStepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedIconButton(onClick = { onChange(value - 1) }, enabled = value > min) {
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.cd_decrease))
        }
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
        OutlinedIconButton(onClick = { onChange(value + 1) }, enabled = value < max) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_increase))
        }
    }
}
