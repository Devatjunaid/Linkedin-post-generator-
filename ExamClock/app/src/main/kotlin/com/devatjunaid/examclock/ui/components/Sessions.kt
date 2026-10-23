package com.devatjunaid.examclock.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.data.db.SessionWithExam
import com.devatjunaid.examclock.domain.SessionType
import com.devatjunaid.examclock.ui.theme.subjectColor
import kotlinx.coroutines.delay

/** Current time in millis, refreshed every [intervalMillis]. */
@Composable
fun rememberNow(intervalMillis: Long = 1_000L): State<Long> = produceState(System.currentTimeMillis(), intervalMillis) {
    while (true) {
        value = System.currentTimeMillis()
        delay(intervalMillis - (value % intervalMillis))
    }
}

@Composable
fun sessionLabel(type: SessionType, title: String, hasTopic: Boolean): String = when (type) {
    SessionType.STUDY -> stringResource(if (hasTopic) R.string.session_learn else R.string.session_study, title)
    SessionType.REVISION -> stringResource(R.string.session_revise, title)
    SessionType.PAST_PAPER -> stringResource(R.string.session_past_paper, title)
    SessionType.CUSTOM -> title
}

@Composable
fun SessionRow(
    item: SessionWithExam,
    onToggle: (Boolean) -> Unit,
    onStart: (() -> Unit)?,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val s = item.session
    val type = SessionType.fromCode(s.type)
    val title = sessionLabel(type, item.topicTitle ?: s.title, s.topicId != null)
    val color = subjectColor(item.examColor ?: 7)
    Row(
        Modifier
            .toggleable(value = s.completed, role = Role.Checkbox, onValueChange = onToggle)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = s.completed, onCheckedChange = null, modifier = Modifier.padding(8.dp))
        SubjectBar(color)
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            item.examTitle?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                textDecoration = if (s.completed) TextDecoration.LineThrough else null,
                color = if (s.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${Format.time(context, s.startAt)} · ${Format.minutes(context, s.durationMinutes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onStart != null && !s.completed) {
            FilledTonalIconButton(onClick = onStart) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.cd_start_focus))
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.cd_delete_session), modifier = Modifier.size(22.dp))
            }
        }
    }
}

