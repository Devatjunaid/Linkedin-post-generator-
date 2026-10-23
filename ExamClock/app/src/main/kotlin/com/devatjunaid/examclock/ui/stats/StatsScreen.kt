package com.devatjunaid.examclock.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.ads.NativeAdCard
import com.devatjunaid.examclock.ui.appViewModel
import com.devatjunaid.examclock.ui.components.AppCard
import com.devatjunaid.examclock.ui.components.Format
import com.devatjunaid.examclock.ui.components.RoundedProgress
import com.devatjunaid.examclock.ui.components.ScreenScaffold
import com.devatjunaid.examclock.ui.theme.subjectColor
import java.time.LocalDate
import java.util.Locale

@Composable
fun StatsScreen() {
    val vm = appViewModel { StatsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ScreenScaffold(title = stringResource(R.string.tab_progress), insetBottom = false) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    StatsRange.entries.forEachIndexed { i, r ->
                        SegmentedButton(
                            selected = state.range == r,
                            onClick = { vm.setRange(r) },
                            shape = SegmentedButtonDefaults.itemShape(i, StatsRange.entries.size),
                        ) { Text(stringResource(if (r == StatsRange.WEEK) R.string.range_week else R.string.range_month)) }
                    }
                }
            }
            item { StudyChart(state) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(stringResource(R.string.stat_streak), pluralStringResource(R.plurals.days_short, state.currentStreak, state.currentStreak), Modifier.weight(1f))
                    StatTile(stringResource(R.string.stat_best_streak), pluralStringResource(R.plurals.days_short, state.bestStreak, state.bestStreak), Modifier.weight(1f))
                    StatTile(stringResource(R.string.stat_sessions), state.sessionsDone.toString(), Modifier.weight(1f))
                }
            }
            item { NativeAdCard() }
            if (state.readiness.isNotEmpty()) {
                item {
                    AppCard {
                        Text(stringResource(R.string.stat_readiness), style = MaterialTheme.typography.titleLarge)
                        state.readiness.forEach { s ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row {
                                    val days = s.exam.examAt?.let { com.devatjunaid.examclock.domain.Countdown.calendarDays(LocalDate.now(), Format.toLocalDate(it)) } ?: 0L
                                    Text(
                                        stringResource(R.string.stat_exam_in, s.exam.title, pluralStringResource(R.plurals.days_short, days.toInt(), days.toInt())),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        if (s.topicCount == 0) stringResource(R.string.stat_no_topics) else "${(s.readiness * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                                RoundedProgress(s.readiness, color = subjectColor(s.exam.colorIndex))
                            }
                        }
                    }
                }
            }
            state.warnings.forEach { w ->
                item {
                    AppCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = stringResource(R.string.cd_warning), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(
                                stringResource(R.string.stat_pace_warning, w.examTitle, String.format(Locale.getDefault(), "%.1f", w.topicsPerDay)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }
            if (state.perExamMinutes.isNotEmpty()) {
                item {
                    AppCard {
                        Text(stringResource(R.string.stat_time_per_exam), style = MaterialTheme.typography.titleLarge)
                        val max = state.perExamMinutes.maxOf { it.second }.coerceAtLeast(1)
                        state.perExamMinutes.forEach { (title, minutes) ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row {
                                    Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(Format.minutes(context, minutes), style = MaterialTheme.typography.labelLarge)
                                }
                                RoundedProgress(minutes.toFloat() / max, height = 6.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    AppCard(modifier, contentPadding = 14.dp) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

/**
 * Minutes studied per day: one series in one colour, 4dp rounded tops on a baseline, a dashed
 * daily-goal reference line, and tap-to-read values (each bar is also announced by TalkBack).
 */
@Composable
private fun StudyChart(state: StatsState) {
    val context = LocalContext.current
    var selected by remember(state.range) { mutableStateOf<LocalDate?>(null) }
    val today = LocalDate.now()
    val dailyGoal = if (state.days.isEmpty()) 0 else state.goalMinutes / state.days.size
    val maxValue = (state.days.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(dailyGoal).coerceAtLeast(60)
    val bar = MaterialTheme.colorScheme.primary
    val baseline = MaterialTheme.colorScheme.outline
    val goalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val focus = state.days.firstOrNull { it.date == selected } ?: state.days.firstOrNull { it.date == today }

    AppCard {
        Text(stringResource(R.string.stat_studied), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Format.minutes(context, state.totalMinutes), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.stat_goal, Format.minutes(context, state.goalMinutes)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (focus != null) {
            Text(
                "${Format.longDate(focus.date)} · ${Format.minutes(context, focus.minutes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(Modifier.fillMaxWidth().height(160.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val goalY = size.height * (1f - dailyGoal.toFloat() / maxValue)
                if (dailyGoal > 0) {
                    drawLine(goalColor, Offset(0f, goalY), Offset(size.width, goalY), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                }
                drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(if (state.days.size > 7) 2.dp else 10.dp), verticalAlignment = Alignment.Bottom) {
                state.days.forEach { day ->
                    val isSelected = day == focus
                    val description = "${Format.longDate(day.date)}: ${Format.minutes(context, day.minutes)}"
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(selected = isSelected, role = Role.Tab, onClick = { selected = day.date })
                            .semantics { contentDescription = description },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val h = size.height * (day.minutes.toFloat() / maxValue)
                            if (h > 0f) {
                                val r = minOf(4.dp.toPx(), h)
                                val color = if (isSelected || focus == null) bar else bar.copy(alpha = 0.55f)
                                // Rounded data end on top, square on the baseline.
                                drawRoundRect(color, Offset(0f, size.height - h), Size(size.width, h), CornerRadius(r, r))
                                drawRect(color, Offset(0f, size.height - r), Size(size.width, r))
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (state.days.size > 7) 2.dp else 10.dp)) {
            state.days.forEachIndexed { i, day ->
                val show = state.days.size <= 7 || i % 5 == 0 || day.date == today
                Text(
                    if (!show) "" else if (state.days.size <= 7) Format.weekdayShort(day.date) else day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (day.date == today) FontWeight.Bold else FontWeight.Normal,
                    color = if (day.date == today) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (dailyGoal > 0) {
            Text(stringResource(R.string.stat_goal_line, Format.minutes(context, dailyGoal)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
