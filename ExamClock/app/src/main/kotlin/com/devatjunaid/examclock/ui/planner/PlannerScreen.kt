package com.devatjunaid.examclock.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.ads.NativeAdCard
import com.devatjunaid.examclock.data.db.ExamEntity
import com.devatjunaid.examclock.domain.SessionType
import com.devatjunaid.examclock.ui.appViewModel
import com.devatjunaid.examclock.ui.components.AppCard
import com.devatjunaid.examclock.ui.components.EmptyState
import com.devatjunaid.examclock.ui.components.Format
import com.devatjunaid.examclock.ui.components.ScreenScaffold
import com.devatjunaid.examclock.ui.components.SessionRow
import com.devatjunaid.examclock.ui.exams.TimePickerDialog
import com.devatjunaid.examclock.ui.theme.subjectColor
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun PlannerScreen(onAutoPlan: () -> Unit, onStartFocus: (Long) -> Unit) {
    val vm = appViewModel { PlannerViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var adding by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = stringResource(R.string.tab_planner),
        insetBottom = false,
        actions = {
            IconButton(onClick = vm::today) { Icon(Icons.Rounded.Today, contentDescription = stringResource(R.string.action_today)) }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_add_session))
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.shiftWeek(-1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.action_previous_week))
                    }
                    Text(
                        Format.monthYear(state.selected),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalButton(onClick = onAutoPlan) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.action_auto_plan), modifier = Modifier.padding(start = 6.dp))
                    }
                    IconButton(onClick = { vm.shiftWeek(1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.action_next_week))
                    }
                }
            }
            item { WeekStrip(state, vm::select) }
            item {
                AppCard(contentPadding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.planner_day_summary, Format.longDate(state.selected), Format.minutes(context, state.plannedMinutes)),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.doneMinutes > 0) {
                            Text(
                                stringResource(R.string.planner_done, Format.minutes(context, state.doneMinutes)),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            if (!state.loading && state.selectedSessions.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.CalendarMonth,
                        title = stringResource(R.string.planner_empty_title),
                        body = stringResource(R.string.planner_empty_body),
                        actionLabel = stringResource(R.string.action_build_plan),
                        onAction = onAutoPlan,
                    )
                }
            } else if (state.selectedSessions.isNotEmpty()) {
                item {
                    AppCard(contentPadding = 4.dp) {
                        state.selectedSessions.forEachIndexed { index, item ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SessionRow(
                                item = item,
                                onToggle = { vm.setCompleted(item, it) },
                                onStart = if (state.selected == LocalDate.now()) ({ onStartFocus(item.session.id) }) else null,
                                onDelete = { vm.delete(item) },
                            )
                        }
                    }
                }
            }
            item { NativeAdCard() }
        }
    }

    if (adding) {
        AddSessionDialog(
            date = state.selected,
            exams = state.exams,
            onDismiss = { adding = false },
            onAdd = { examId, title, type, time, minutes ->
                vm.add(examId, title, type, state.selected, time, minutes)
                adding = false
            },
        )
    }
}

@Composable
private fun WeekStrip(state: PlannerState, onSelect: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        state.week.forEach { day ->
            val selected = day == state.selected
            val sessions = state.byDay[day].orEmpty()
            val allDone = sessions.isNotEmpty() && sessions.all { it.session.completed }
            val scheme = MaterialTheme.colorScheme
            val label = "${Format.longDate(day)}, ${sessions.size}"
            Surface(
                onClick = { onSelect(day) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) scheme.secondary else scheme.background,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = label
                        this.selected = selected
                    },
            ) {
                Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        Format.weekdayShort(day),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) scheme.onSecondary else scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (day == today) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (selected) scheme.onSecondary else if (day == today) scheme.primary else scheme.onSurface,
                    )
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    sessions.isEmpty() -> scheme.background.copy(alpha = 0f)
                                    allDone -> scheme.primary
                                    else -> scheme.tertiary
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSessionDialog(
    date: LocalDate,
    exams: List<ExamEntity>,
    onDismiss: () -> Unit,
    onAdd: (examId: Long?, title: String, type: SessionType, time: LocalTime, minutes: Int) -> Unit,
) {
    val context = LocalContext.current
    var examId by remember { mutableStateOf(exams.firstOrNull()?.id) }
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(SessionType.STUDY) }
    var time by remember { mutableStateOf(LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)) }
    var minutes by remember { mutableIntStateOf(45) }
    var pickTime by remember { mutableStateOf(false) }
    val selectedExam = exams.firstOrNull { it.id == examId }
    val canAdd = title.isNotBlank() || selectedExam != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_session_title, Format.date(date))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (exams.isNotEmpty()) {
                    Text(stringResource(R.string.add_session_exam), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        exams.forEach { exam ->
                            FilterChip(
                                selected = exam.id == examId,
                                onClick = { examId = if (examId == exam.id) null else exam.id },
                                label = { Text(exam.title) },
                                leadingIcon = { Box(Modifier.size(10.dp).clip(CircleShape).background(subjectColor(exam.colorIndex))) },
                            )
                        }
                    }
                }
                Text(stringResource(R.string.add_session_type), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        SessionType.STUDY to R.string.type_study,
                        SessionType.REVISION to R.string.type_revision,
                        SessionType.PAST_PAPER to R.string.type_past_paper,
                        SessionType.CUSTOM to R.string.type_other,
                    ).forEach { (t, label) ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(stringResource(label)) })
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text(stringResource(R.string.add_session_what)) },
                    placeholder = { Text(stringResource(R.string.add_session_what_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.add_session_starts, Format.time(context, time)), modifier = Modifier.padding(start = 8.dp))
                }
                Text(stringResource(R.string.add_session_length), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(25, 45, 60, 90, 120).forEach { m ->
                        FilterChip(selected = minutes == m, onClick = { minutes = m }, label = { Text(Format.minutes(context, m)) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canAdd,
                onClick = {
                    val finalTitle = title.ifBlank { selectedExam?.title.orEmpty() }
                    onAdd(selectedExam?.id, finalTitle, type, time, minutes)
                },
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )

    if (pickTime) {
        TimePickerDialog(initial = time, onDismiss = { pickTime = false }, onConfirm = {
            time = it
            pickTime = false
        })
    }
}
