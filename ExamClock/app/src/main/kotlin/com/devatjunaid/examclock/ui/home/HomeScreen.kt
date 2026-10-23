package com.devatjunaid.examclock.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.ads.NativeAdCard
import com.devatjunaid.examclock.data.db.ExamEntity
import com.devatjunaid.examclock.domain.Countdown
import com.devatjunaid.examclock.ui.appViewModel
import com.devatjunaid.examclock.ui.components.AppCard
import com.devatjunaid.examclock.ui.components.EmptyState
import com.devatjunaid.examclock.ui.components.Format
import com.devatjunaid.examclock.ui.components.RoundedProgress
import com.devatjunaid.examclock.ui.components.SectionHeader
import com.devatjunaid.examclock.ui.components.SessionRow
import com.devatjunaid.examclock.ui.components.SubjectDot
import com.devatjunaid.examclock.ui.components.rememberNow
import com.devatjunaid.examclock.ui.theme.Brand
import com.devatjunaid.examclock.ui.theme.CountdownStyle
import com.devatjunaid.examclock.ui.theme.onSubjectColor
import com.devatjunaid.examclock.ui.theme.subjectColor
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    onOpenExam: (Long) -> Unit,
    onEditExam: (Long) -> Unit,
    onAddExam: () -> Unit,
    onBrowseCatalog: () -> Unit,
    onOpenPlanner: () -> Unit,
    onBuildPlan: () -> Unit,
    onStartFocus: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Header(state, onOpenSettings) }

        if (state.loading) return@LazyColumn

        if (!state.hasAnyExam) {
            item {
                EmptyState(
                    icon = Icons.Rounded.EventAvailable,
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                    actionLabel = stringResource(R.string.action_add_exam),
                    onAction = onAddExam,
                )
            }
            item {
                OutlinedButton(onClick = onBrowseCatalog, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_browse_catalog))
                }
            }
            item { NativeAdCard() }
            return@LazyColumn
        }

        val next = state.next
        if (next != null) {
            item { HeroCountdown(next, onClick = { onOpenExam(next.exam.id) }) }
        }
        if (state.upcoming.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.upcoming, key = { it.exam.id }) { summary -> MiniCountdown(summary) { onOpenExam(summary.exam.id) } }
                }
            }
        }
        if (state.undated.isNotEmpty()) {
            item { UndatedCard(state.undated, onEditExam) }
        }

        item { GoalCard(state) }

        item { NativeAdCard() }

        item {
            SectionHeader(
                stringResource(R.string.home_today_plan),
                actionLabel = stringResource(R.string.action_open_planner),
                onAction = onOpenPlanner,
            )
        }
        if (state.today.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.home_nothing_today_title),
                    body = stringResource(R.string.home_nothing_today_body),
                    actionLabel = stringResource(R.string.action_build_plan),
                    onAction = onBuildPlan,
                )
            }
        } else {
            item {
                AppCard(contentPadding = 4.dp) {
                    state.today.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SessionRow(
                            item = item,
                            onToggle = { vm.setCompleted(item, it) },
                            onStart = { onStartFocus(item.session.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: HomeState, onOpenSettings: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = stringResource(
        when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..16 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        },
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(Format.longDate(LocalDate.now()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (state.name.isBlank()) greeting else stringResource(R.string.greeting_named, greeting, state.name),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.streak > 0) {
            AssistChip(
                onClick = {},
                label = { Text(pluralStringResource(R.plurals.streak_days, state.streak, state.streak)) },
                leadingIcon = { Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings_title))
        }
    }
}

@Composable
private fun HeroCountdown(summary: ExamSummary, onClick: () -> Unit) {
    val context = LocalContext.current
    val now by rememberNow()
    val exam = summary.exam
    val target = exam.examAt ?: return
    val c = Countdown.between(now, target)
    val description = pluralStringResource(R.plurals.cd_countdown, c.days.toInt(), exam.title, c.days.toInt(), c.hours, c.minutes)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Brand.Ink,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.next_exam).uppercase(), style = MaterialTheme.typography.labelMedium, color = Brand.OnInkMuted, modifier = Modifier.weight(1f))
                if (exam.groupName.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = subjectColor(exam.colorIndex)) {
                        Text(exam.groupName, style = MaterialTheme.typography.labelMedium, color = onSubjectColor(exam.colorIndex), maxLines = 1, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
            }
            Text(exam.title, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(c.days.toString(), style = CountdownStyle, color = Color.White)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text(pluralStringResource(R.plurals.unit_days_plural, c.days.toInt()), style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(
                        stringResource(R.string.countdown_hms, c.hours, c.minutes, c.seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Brand.OnInkMuted,
                    )
                }
            }
            if (summary.topicCount > 0) {
                Row {
                    Text(stringResource(R.string.syllabus_covered), style = MaterialTheme.typography.bodySmall, color = Brand.OnInkMuted, modifier = Modifier.weight(1f))
                    Text("${(summary.readiness * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
                RoundedProgress(summary.readiness, color = Brand.BlueLight, track = Brand.InkLine, height = 6.dp)
            }
            Text(
                listOf(Format.date(target), Format.time(context, target), exam.location).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = Brand.OnInkMuted,
            )
        }
    }
}

@Composable
private fun MiniCountdown(summary: ExamSummary, onClick: () -> Unit) {
    val target = summary.exam.examAt ?: return
    val days = Countdown.calendarDays(LocalDate.now(), Format.toLocalDate(target))
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(132.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SubjectDot(subjectColor(summary.exam.colorIndex), size = 8.dp)
                Text(summary.exam.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(pluralStringResource(R.plurals.days_short, days.toInt(), days.toInt()), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun UndatedCard(exams: List<ExamEntity>, onEditExam: (Long) -> Unit) {
    AppCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(stringResource(R.string.home_dates_needed_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
        Text(stringResource(R.string.home_dates_needed_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        exams.take(4).forEach { exam ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubjectDot(subjectColor(exam.colorIndex))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(exam.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(exam.groupName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                OutlinedButton(onClick = { onEditExam(exam.id) }) { Text(stringResource(R.string.action_set_date)) }
            }
        }
        if (exams.size > 4) {
            Text(pluralStringResource(R.plurals.more_exams, exams.size - 4, exams.size - 4), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun GoalCard(state: HomeState) {
    val context = LocalContext.current
    val progress = if (state.goalMinutes == 0) 0f else state.todayMinutes.toFloat() / state.goalMinutes
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_daily_goal), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.home_goal_progress, Format.minutes(context, state.todayMinutes), Format.minutes(context, state.goalMinutes)),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Box(Modifier.size(8.dp))
            Text("${(progress * 100).toInt().coerceAtMost(999)}%", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        RoundedProgress(progress)
        if (progress >= 1f) {
            Text(stringResource(R.string.home_goal_reached), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
