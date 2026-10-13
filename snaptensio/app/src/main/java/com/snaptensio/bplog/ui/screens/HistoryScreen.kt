package com.snaptensio.bplog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.snaptensio.bplog.ads.NativeAdCard
import com.snaptensio.bplog.data.AppSettings
import com.snaptensio.bplog.data.Arm
import com.snaptensio.bplog.data.Reading
import com.snaptensio.bplog.data.Source
import com.snaptensio.bplog.data.tagList
import com.snaptensio.bplog.domain.BpClassifier
import com.snaptensio.bplog.domain.Severity
import com.snaptensio.bplog.domain.average
import com.snaptensio.bplog.domain.dayKey
import com.snaptensio.bplog.domain.isEvening
import com.snaptensio.bplog.domain.isMorning
import com.snaptensio.bplog.ui.MainViewModel
import com.snaptensio.bplog.ui.Routes
import com.snaptensio.bplog.ui.components.EmptyState
import com.snaptensio.bplog.ui.components.ScreenHeader
import com.snaptensio.bplog.ui.components.Segmented
import com.snaptensio.bplog.ui.components.readableColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryFilter(val label: String) { ALL("All"), MORNING("Morning"), EVENING("Evening"), HIGH("High") }

private sealed interface HistoryRow {
    data class Header(val key: Int, val label: String, val avg: String) : HistoryRow
    data class Item(val reading: Reading, val last: Boolean) : HistoryRow
    data object Ad : HistoryRow
}

@Composable
fun HistoryScreen(vm: MainViewModel, nav: NavHostController) {
    val readings by vm.readings.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: AppSettings()
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val rows = remember(readings, filter, query, s.guideline) {
        val filtered = readings.filter { r ->
            when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.MORNING -> r.isMorning()
                HistoryFilter.EVENING -> r.isEvening()
                HistoryFilter.HIGH -> BpClassifier.classify(r.systolic, r.diastolic, s.guideline).severity.level >= Severity.STAGE2.level
            }
        }.filter { r ->
            query.isBlank() || r.note.contains(query, true) || r.tagList().any { it.contains(query, true) } ||
                "${r.systolic}/${r.diastolic}".contains(query)
        }
        val fmt = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
        val today = dayKey(System.currentTimeMillis())
        val yesterday = dayKey(System.currentTimeMillis() - 86_400_000L)
        buildList {
            var shown = 0
            filtered.groupBy { dayKey(it.timestamp) }.forEach { (key, day) ->
                val label = when (key) { today -> "Today"; yesterday -> "Yesterday"; else -> fmt.format(Date(day.first().timestamp)) }
                add(HistoryRow.Header(key, label, day.average()?.toString() ?: ""))
                day.forEachIndexed { i, r ->
                    add(HistoryRow.Item(r, i == day.lastIndex))
                    shown++
                    // One native ad after the 4th reading, between day groups' rows.
                    if (shown == 4) add(HistoryRow.Ad)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("History") {
            IconButton(onClick = { searching = !searching; if (!searching) query = "" }) { Icon(Icons.Filled.Search, contentDescription = "Search notes and tags") }
            IconButton(onClick = { nav.navigate(Routes.REPORT) }) { Icon(Icons.Filled.FileDownload, contentDescription = "Export report") }
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (searching) {
                OutlinedTextField(query, { query = it }, placeholder = { Text("Search notes, tags or values") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Segmented(HistoryFilter.entries, filter, { it.label }, { filter = it }, Modifier.fillMaxWidth())
        }
        if (readings.isEmpty()) {
            EmptyState("Your log is empty", "Scan your monitor or type a reading in. Everything you save shows up here.") {
                Button(onClick = { nav.navigate(Routes.scan()) }) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan monitor")
                }
            }
            return@Column
        }
        if (rows.isEmpty()) {
            EmptyState("Nothing matches", "Try another filter or search.")
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(rows, key = { row ->
                when (row) {
                    is HistoryRow.Header -> "h${row.key}"
                    is HistoryRow.Item -> "r${row.reading.id}"
                    HistoryRow.Ad -> "ad"
                }
            }) { row ->
                when (row) {
                    is HistoryRow.Header -> Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 6.dp)) {
                        Text(row.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text("Avg ${row.avg}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is HistoryRow.Item -> ReadingRow(row.reading, s, row.last) { nav.navigate(Routes.edit(id = row.reading.id)) }
                    HistoryRow.Ad -> NativeAdCard(Modifier.padding(vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun ReadingRow(r: Reading, s: AppSettings, last: Boolean, onClick: () -> Unit) {
    val cat = BpClassifier.classify(r.systolic, r.diastolic, s.guideline)
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(r.timestamp))
    val meta = buildList {
        if (r.averagedCount > 1) add("Avg of ${r.averagedCount}")
        add(runCatching { Arm.valueOf(r.arm).label }.getOrDefault("Left") + " arm")
        addAll(r.tagList())
        if (r.note.isNotBlank()) add("Note")
    }.joinToString(" · ")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(6.dp).height(40.dp).clip(RoundedCornerShape(3.dp)).background(cat.severity.color))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${r.systolic}/${r.diastolic}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                r.pulse?.let { Text("$it bpm", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (r.source == Source.SCAN.name) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Scanned", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.height(14.dp))
                }
            }
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(time, style = MaterialTheme.typography.titleSmall)
            Text(cat.label, style = MaterialTheme.typography.labelMedium, color = cat.severity.readableColor())
        }
    }
    Spacer(Modifier.height(if (last) 0.dp else 6.dp))
}
