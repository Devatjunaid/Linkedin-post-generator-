package com.devatjunaid.examclock.ui.exams

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.ads.NativeAdCard
import com.devatjunaid.examclock.domain.Countdown
import com.devatjunaid.examclock.ui.appViewModel
import com.devatjunaid.examclock.ui.components.EmptyState
import com.devatjunaid.examclock.ui.components.Format
import com.devatjunaid.examclock.ui.components.RoundedProgress
import com.devatjunaid.examclock.ui.components.ScreenScaffold
import com.devatjunaid.examclock.ui.components.SubjectBar
import com.devatjunaid.examclock.ui.home.ExamSummary
import com.devatjunaid.examclock.ui.theme.subjectColor
import java.time.LocalDate

@Composable
fun ExamsScreen(onOpenExam: (Long) -> Unit, onAddExam: () -> Unit, onBrowseCatalog: () -> Unit) {
    val vm = appViewModel { ExamListViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = stringResource(R.string.tab_exams),
        insetBottom = false,
        actions = {
            IconButton(onClick = onBrowseCatalog) {
                Icon(Icons.Rounded.Public, contentDescription = stringResource(R.string.action_browse_catalog))
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddExam,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_exam)) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!state.loading && state.upcoming.isEmpty() && state.undated.isEmpty() && state.past.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.EventAvailable,
                        title = stringResource(R.string.home_empty_title),
                        body = stringResource(R.string.exams_empty_body),
                        actionLabel = stringResource(R.string.action_browse_catalog),
                        onAction = onBrowseCatalog,
                    )
                }
            }
            section(R.string.exams_upcoming, state.upcoming, onOpenExam)
            if (state.upcoming.size >= 2) item { NativeAdCard() }
            section(R.string.exams_undated, state.undated, onOpenExam)
            section(R.string.exams_past, state.past, onOpenExam)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(title: Int, list: List<ExamSummary>, onOpen: (Long) -> Unit) {
    if (list.isEmpty()) return
    item(key = "h$title") {
        Text(
            stringResource(title).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    items(list, key = { it.exam.id }) { ExamRow(it) { onOpen(it.exam.id) } }
}

@Composable
fun ExamRow(summary: ExamSummary, onClick: () -> Unit) {
    val context = LocalContext.current
    val exam = summary.exam
    val color = subjectColor(exam.colorIndex)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SubjectBar(color, height = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(exam.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = listOfNotNull(
                    exam.groupName.takeIf { it.isNotBlank() },
                    exam.examAt?.let { "${Format.date(it)} · ${Format.time(context, it)}" } ?: stringResource(R.string.exam_date_not_set),
                ).joinToString(" · ")
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (summary.topicCount > 0) {
                    RoundedProgress(summary.readiness, color = color, height = 5.dp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            exam.examAt?.let { at ->
                val days = Countdown.calendarDays(LocalDate.now(), Format.toLocalDate(at))
                if (days >= 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(days.toString(), style = MaterialTheme.typography.headlineSmall)
                        Text(pluralStringResource(R.plurals.unit_days_plural, days.toInt()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
