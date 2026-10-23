package com.walkies.dogwalk.ui.log

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.text.format.DateFormat
import com.walkies.dogwalk.AppContainer
import com.walkies.dogwalk.R
import com.walkies.dogwalk.ads.NativeAdCache
import com.walkies.dogwalk.ads.NativeAdCard
import com.walkies.dogwalk.data.Consistency
import com.walkies.dogwalk.data.LogEntry
import com.walkies.dogwalk.data.LogType
import com.walkies.dogwalk.data.Walk
import com.walkies.dogwalk.data.durationMillis
import com.walkies.dogwalk.ui.components.EventBadge
import com.walkies.dogwalk.ui.components.SectionCard
import com.walkies.dogwalk.ui.components.SectionHeader
import com.walkies.dogwalk.ui.components.label
import com.walkies.dogwalk.ui.components.style
import com.walkies.dogwalk.ui.components.walkStyle
import com.walkies.dogwalk.ui.containerFactory
import com.walkies.dogwalk.ui.theme.WalkiesColors
import com.walkies.dogwalk.util.formatClock
import com.walkies.dogwalk.util.formatDay
import com.walkies.dogwalk.util.formatDuration
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun LogScreen(
    container: AppContainer,
    onDeleted: (undo: () -> Unit) -> Unit,
    viewModel: LogViewModel = viewModel(factory = containerFactory { LogViewModel(it) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var walkToDelete by remember { mutableStateOf<Walk?>(null) }

    fun shareReport() {
        scope.launch {
            val text = viewModel.buildReport(context)
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.report_subject, state.dogName))
                .putExtra(Intent.EXTRA_TEXT, text)
            val open = { context.startActivity(Intent.createChooser(send, context.getString(R.string.report_chooser))) }
            // Sharing the report is a major action: interstitial first (if the 30 s cap allows).
            if (activity != null) container.ads.interstitials.showIfAllowed(activity, open) else open()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.log_subtitle, state.dogName),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.log_title), style = MaterialTheme.typography.headlineMedium)
            }
            FilledTonalButton(onClick = ::shareReport, shape = RoundedCornerShape(16.dp)) {
                Icon(painterResource(R.drawable.ic_share), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.log_vet_report))
            }
        }
        Spacer(Modifier.height(16.dp))
        WeekCard(state, onSelect = viewModel::select)
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LogFilter.entries.forEach { f ->
                FilterChip(
                    selected = state.filter == f,
                    onClick = { viewModel.setFilter(f) },
                    label = { Text(stringResource(f.labelRes())) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WalkiesColors.Ink,
                        selectedLabelColor = Color.White,
                    ),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        NativeAdCard(container.ads, NativeAdCache.LOG)
        Spacer(Modifier.height(14.dp))
        SectionCard {
            SectionHeader(
                if (state.selected == state.today) stringResource(R.string.log_today) else formatDay(state.selected),
            ) {
                TextButton(onClick = { showAdd = true }) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.log_add_entry))
                }
            }
            if (state.items.isEmpty()) {
                Text(
                    stringResource(R.string.log_day_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            state.items.forEach { item ->
                when (item) {
                    is LogItem.Entry -> EntryRow(
                        entry = item.entry,
                        onConsistency = { viewModel.setConsistency(item.entry, it) },
                        onDelete = { viewModel.delete(item.entry, onDeleted) },
                    )
                    is LogItem.WalkItem -> WalkLogRow(item, onDelete = { walkToDelete = item.walk })
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showAdd) {
        AddEntryDialog(
            onDismiss = { showAdd = false },
            onAdd = { type, hour, minute, consistency ->
                showAdd = false
                viewModel.addEntry(type, state.selected, hour, minute, consistency)
            },
        )
    }
    walkToDelete?.let { walk ->
        AlertDialog(
            onDismissRequest = { walkToDelete = null },
            title = { Text(stringResource(R.string.log_delete_walk_title)) },
            text = { Text(stringResource(R.string.log_delete_walk_body)) },
            confirmButton = {
                TextButton(onClick = {
                    walkToDelete = null
                    viewModel.deleteWalk(walk)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { walkToDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun LogFilter.labelRes() = when (this) {
    LogFilter.ALL -> R.string.filter_all
    LogFilter.PEE -> R.string.filter_pee
    LogFilter.POO -> R.string.filter_poo
    LogFilter.MEALS -> R.string.filter_meals
    LogFilter.WALKS -> R.string.filter_walks
}

@Composable
private fun WeekCard(state: LogState, onSelect: (java.time.LocalDate) -> Unit) {
    val maxDots = 8
    SectionCard(contentPadding = 12.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendDot(WalkiesColors.Pee, stringResource(R.string.event_pee))
            Spacer(Modifier.size(12.dp))
            LegendDot(WalkiesColors.Poo, stringResource(R.string.event_poo))
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(
                    R.string.log_week_avg,
                    String.format(Locale.getDefault(), "%.1f", state.avgPee),
                    String.format(Locale.getDefault(), "%.1f", state.avgPoo),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            state.days.forEach { day ->
                val selected = day.date == state.selected
                Surface(
                    onClick = { onSelect(day.date) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.background else Color.Transparent,
                ) {
                    Column(
                        Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(
                            Modifier.height(112.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.Bottom),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val pee = day.pee.coerceAtMost(maxDots)
                            val poo = day.poo.coerceAtMost(maxDots - pee)
                            repeat(pee) {
                                Icon(painterResource(R.drawable.ic_pee), contentDescription = null, tint = WalkiesColors.Pee, modifier = Modifier.size(11.dp))
                            }
                            repeat(poo) {
                                Box(
                                    Modifier
                                        .size(11.dp)
                                        .background(WalkiesColors.Poo, CircleShape),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            day.date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EntryRow(entry: LogEntry, onConsistency: (Consistency?) -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EventBadge(entry.type.style())
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.type.label(), style = MaterialTheme.typography.titleSmall)
                if (entry.walkId != null) {
                    Text(
                        stringResource(R.string.on_walk),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                formatClock(context, entry.timestamp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (entry.type == LogType.POO) {
            ConsistencyChips(
                selected = entry.consistency,
                onSelect = onConsistency,
                modifier = Modifier.padding(start = 48.dp),
            )
        }
    }
}

@Composable
fun ConsistencyChips(selected: Consistency?, onSelect: (Consistency?) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Consistency.entries.forEach { c ->
            FilterChip(
                selected = selected == c,
                onClick = { onSelect(if (selected == c) null else c) },
                label = {
                    Text(
                        stringResource(
                            when (c) {
                                Consistency.FIRM -> R.string.consistency_firm
                                Consistency.SOFT -> R.string.consistency_soft
                                Consistency.RUNNY -> R.string.consistency_runny
                            },
                        ),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WalkiesColors.Poo,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

@Composable
private fun WalkLogRow(item: LogItem.WalkItem, onDelete: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EventBadge(walkStyle)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.event_walk), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(
                    R.string.walk_summary,
                    formatDuration(context, item.walk.durationMillis()),
                    item.pee.toString(),
                    item.poo.toString(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            formatClock(context, item.walk.startTime),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onDelete) {
            Icon(
                painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEntryDialog(
    onDismiss: () -> Unit,
    onAdd: (LogType, Int, Int, Consistency?) -> Unit,
) {
    val context = LocalContext.current
    val now = remember { LocalTime.now() }
    val timeState = rememberTimePickerState(now.hour, now.minute, DateFormat.is24HourFormat(context))
    var type by rememberSaveable { mutableStateOf(LogType.PEE) }
    var consistency by rememberSaveable { mutableStateOf<Consistency?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.log_add_entry)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.entry_type), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LogType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label()) })
                    }
                }
                Text(stringResource(R.string.entry_time), style = MaterialTheme.typography.labelLarge)
                TimeInput(state = timeState)
                if (type == LogType.POO) {
                    Text(stringResource(R.string.consistency), style = MaterialTheme.typography.labelLarge)
                    ConsistencyChips(consistency, onSelect = { consistency = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onAdd(type, timeState.hour, timeState.minute, if (type == LogType.POO) consistency else null)
            }) { Text(stringResource(R.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

