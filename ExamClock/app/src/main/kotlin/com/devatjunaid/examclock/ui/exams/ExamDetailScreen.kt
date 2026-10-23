package com.devatjunaid.examclock.ui.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.ads.NativeAdCard
import com.devatjunaid.examclock.data.db.ExamEntity
import com.devatjunaid.examclock.data.db.TopicEntity
import com.devatjunaid.examclock.domain.Countdown
import com.devatjunaid.examclock.domain.Readiness
import com.devatjunaid.examclock.domain.TopicStatus
import com.devatjunaid.examclock.ui.appViewModel
import com.devatjunaid.examclock.ui.components.AppCard
import com.devatjunaid.examclock.ui.components.Format
import com.devatjunaid.examclock.ui.components.RoundedProgress
import com.devatjunaid.examclock.ui.components.ScreenScaffold
import com.devatjunaid.examclock.ui.components.StatusChip
import com.devatjunaid.examclock.ui.components.rememberNow
import com.devatjunaid.examclock.ui.theme.Brand
import java.time.LocalDate

@Composable
fun ExamDetailScreen(
    examId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPlanRevision: () -> Unit,
    onFocus: () -> Unit,
) {
    val vm = appViewModel(key = "exam-$examId") { ExamDetailViewModel(it.repository, examId) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    val exam = state.exam
    val missing = !state.loading && exam == null
    LaunchedEffect(missing) {
        // Deleted (here or elsewhere): leave the screen.
        if (missing) onBack()
    }

    ScreenScaffold(
        title = exam?.title ?: "",
        onBack = onBack,
        actions = {
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.action_edit_exam)) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.cd_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete_exam)) },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                        onClick = {
                            menu = false
                            confirmDelete = true
                        },
                    )
                }
            }
        },
    ) { padding ->
        if (exam == null) return@ScreenScaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (exam.groupName.isNotBlank()) {
                item { Text(exam.groupName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            item { CountdownBlocks(exam, onEdit) }
            item { InfoCard(exam) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onFocus, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.action_focus_now), modifier = Modifier.padding(start = 6.dp))
                    }
                    Button(onClick = onPlanRevision, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.action_plan_revision), modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
            item { SyllabusHeader(state, onAdd = { showAdd = true }) }
            if (state.topics.isEmpty()) {
                item {
                    AppCard {
                        Text(stringResource(R.string.topics_empty_title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.topics_empty_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { showAdd = true }) { Text(stringResource(R.string.action_add_topics)) }
                    }
                }
            } else {
                items(state.topics, key = { it.id }) { topic ->
                    TopicRow(
                        topic = topic,
                        onCycle = { vm.cycleStatus(topic) },
                        onConfidence = { vm.setConfidence(topic, it) },
                        onRename = { vm.rename(topic, it) },
                        onDelete = { vm.deleteTopic(topic) },
                    )
                }
            }
            if (exam.notes.isNotBlank()) {
                item {
                    AppCard {
                        Text(stringResource(R.string.exam_notes), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(exam.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { NativeAdCard() }
        }
    }

    if (showAdd) {
        AddTopicsDialog(onDismiss = { showAdd = false }, onAdd = {
            vm.addTopics(it)
            showAdd = false
        })
    }
    if (confirmDelete && exam != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_exam_title)) },
            text = { Text(stringResource(R.string.delete_exam_body, exam.title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteExam()
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun CountdownBlocks(exam: ExamEntity, onSetDate: () -> Unit) {
    val target = exam.examAt
    if (target == null) {
        AppCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
            Text(stringResource(R.string.home_dates_needed_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Text(stringResource(R.string.exam_set_date_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Button(onClick = onSetDate) { Text(stringResource(R.string.action_set_date)) }
        }
        return
    }
    val now by rememberNow()
    val c = Countdown.between(now, target)
    if (target <= now) {
        AppCard { Text(stringResource(R.string.exam_finished), style = MaterialTheme.typography.titleMedium) }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            c.days.toString() to pluralStringResource(R.plurals.unit_days_plural, c.days.toInt()),
            c.hours.toString().padStart(2, '0') to stringResource(R.string.unit_hours),
            c.minutes.toString().padStart(2, '0') to stringResource(R.string.unit_minutes),
            c.seconds.toString().padStart(2, '0') to stringResource(R.string.unit_seconds),
        ).forEach { (value, label) ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brand.Ink)
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(value, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(label, style = MaterialTheme.typography.labelSmall, color = Brand.OnInkMuted)
            }
        }
    }
}

@Composable
private fun InfoCard(exam: ExamEntity) {
    val context = LocalContext.current
    AppCard {
        val at = exam.examAt
        Row {
            InfoCell(stringResource(R.string.exam_date), at?.let { Format.date(it) } ?: stringResource(R.string.exam_date_not_set), Modifier.weight(1f))
            InfoCell(stringResource(R.string.exam_time), at?.let { Format.time(context, it) } ?: "—", Modifier.weight(1f))
        }
        Row {
            InfoCell(stringResource(R.string.exam_duration), Format.minutes(context, exam.durationMinutes), Modifier.weight(1f))
            InfoCell(stringResource(R.string.exam_location), exam.location.ifBlank { "—" }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SyllabusHeader(state: ExamDetailState, onAdd: () -> Unit) {
    val statuses = state.statuses
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.exam_syllabus), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onAdd) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.action_add_topics), modifier = Modifier.padding(start = 4.dp))
            }
        }
        if (statuses.isNotEmpty()) {
            val done = statuses.count { it == TopicStatus.DONE }
            Text(
                stringResource(R.string.topics_progress, done, statuses.size, (Readiness.score(statuses) * 100).toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RoundedProgress(Readiness.score(statuses))
            val exam = state.exam
            val perDay = exam?.examAt?.let { Readiness.topicsPerDayNeeded(statuses, LocalDate.now(), Format.toLocalDate(it)) }
            if (perDay != null && perDay > 0f) {
                Text(
                    stringResource(R.string.topics_pace, String.format(java.util.Locale.getDefault(), "%.1f", perDay)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (perDay > 1.5f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(stringResource(R.string.topics_tap_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TopicRow(
    topic: TopicEntity,
    onCycle: () -> Unit,
    onConfidence: (Int) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    val status = TopicStatus.fromCode(topic.status)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(topic.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Surface(onClick = onCycle, shape = RoundedCornerShape(10.dp), color = Color.Transparent, modifier = Modifier.heightIn(min = 40.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) { StatusChip(status) }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.cd_topic_options, topic.title)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_rename)) }, onClick = {
                            menu = false
                            renaming = true
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = {
                            menu = false
                            onDelete()
                        })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.topic_confidence), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                (1..5).forEach { value ->
                    IconToggleButton(checked = value <= topic.confidence, onCheckedChange = { onConfidence(value) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (value <= topic.confidence) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = stringResource(R.string.cd_confidence, value),
                            tint = if (value <= topic.confidence) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
    if (renaming) {
        var text by remember { mutableStateOf(topic.title) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text(stringResource(R.string.action_rename)) },
            text = { OutlinedTextField(value = text, onValueChange = { text = it.take(120) }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    onRename(text)
                    renaming = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun AddTopicsDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_topics)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.add_topics_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(4000) },
                    placeholder = { Text(stringResource(R.string.add_topics_placeholder)) },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.action_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
