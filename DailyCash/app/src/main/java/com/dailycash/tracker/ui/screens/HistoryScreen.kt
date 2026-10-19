package com.dailycash.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailycash.tracker.data.CashEntry
import com.dailycash.tracker.data.Money
import com.dailycash.tracker.data.Stats
import com.dailycash.tracker.data.isSpend
import com.dailycash.tracker.ui.MainViewModel
import com.dailycash.tracker.ui.components.EmptyState
import com.dailycash.tracker.ui.components.EntryRow
import com.dailycash.tracker.ui.components.Segmented
import com.dailycash.tracker.ui.theme.Mono
import com.dailycash.tracker.ui.theme.Palette
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Filter(val label: String) { ALL("All"), SPENT("Spent"), ADDED("Added") }

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val home by vm.homeCurrency.collectAsStateWithLifecycle()
    val rates by vm.rates.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    var confirm by remember { mutableStateOf<CashEntry?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val shown = remember(entries, filter) {
        when (filter) {
            Filter.ALL -> entries
            Filter.SPENT -> entries.filter { it.isSpend() }
            Filter.ADDED -> entries.filter { !it.isSpend() }
        }
    }
    val byDay = remember(shown) { shown.groupBy { Stats.dateOf(it) } }
    val dayFmt = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }
    val today = LocalDate.now()

    confirm?.let { e ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Palette.Surface,
            title = { Text("Delete this entry?", color = Palette.Text, fontWeight = FontWeight.Bold) },
            text = { Text("${e.label} · ${Money.format(e.amountMinor, e.currency)}. Your wallet balance will be updated.", color = Palette.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    vm.delete(e)
                    scope.launch {
                        val r = snackbar.showSnackbar("Entry deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
                        if (r == SnackbarResult.ActionPerformed) vm.restore(e)
                    }
                }) { Text("Delete", color = Palette.Coral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Keep", color = Palette.Text) } },
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("History", color = Palette.Text, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Segmented(Filter.entries.toList(), filter, { it.label }, { filter = it })
                }
            }
            if (byDay.isEmpty()) {
                item { EmptyState(Icons.AutoMirrored.Rounded.ReceiptLong, "No entries yet", "Everything you log shows up here, newest first.") }
            }
            byDay.forEach { (date, list) ->
                stickyHeader(key = "d_$date") {
                    val spent = Stats.totalHome(list.filter { it.isSpend() }, home, rates)
                    Row(
                        Modifier.fillMaxWidth().background(Palette.Ink).padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            when (date) {
                                today -> "Today"
                                today.minusDays(1) -> "Yesterday"
                                else -> dayFmt.format(date)
                            },
                            color = Palette.Muted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (spent > 0) Text("−" + Money.format(spent, home), color = Palette.Amber, fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                items(list, key = { it.id }) { e ->
                    EntryRow(
                        e, home, rates,
                        Modifier.padding(start = 20.dp, end = 4.dp).animateItem(),
                        trailing = {
                            IconButton(onClick = { confirm = e }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${e.label}", tint = Palette.Muted)
                            }
                        },
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}
