package com.sunarc.inverterlogger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunarc.inverterlogger.ads.NativeAdSlot
import com.sunarc.inverterlogger.domain.Aggregation
import com.sunarc.inverterlogger.domain.Reading
import com.sunarc.inverterlogger.domain.ReadingSource
import com.sunarc.inverterlogger.ui.AppViewModel
import com.sunarc.inverterlogger.ui.Navigator
import com.sunarc.inverterlogger.ui.components.Bar
import com.sunarc.inverterlogger.ui.components.BarChart
import com.sunarc.inverterlogger.ui.components.Fmt
import com.sunarc.inverterlogger.ui.components.ScreenHeader
import com.sunarc.inverterlogger.ui.components.SecondaryButton
import com.sunarc.inverterlogger.ui.components.Segmented
import com.sunarc.inverterlogger.ui.components.StatTile
import com.sunarc.inverterlogger.ui.components.SunCard
import com.sunarc.inverterlogger.ui.components.YieldHeatmap
import com.sunarc.inverterlogger.ui.theme.DisplayFont
import com.sunarc.inverterlogger.ui.theme.MonoFont
import com.sunarc.inverterlogger.ui.theme.Palette
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class Range(val label: String) { WEEK("Week"), MONTH("Month"), YEAR("Year") }

@Composable
fun HistoryScreen(vm: AppViewModel, nav: Navigator) {
    val dashboard by vm.dashboard.collectAsStateWithLifecycle()
    val readings by vm.readings.collectAsStateWithLifecycle()
    val inverters by vm.inverters.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val d = dashboard ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var range by rememberSaveable { mutableStateOf(Range.MONTH) }
    var deleting by remember { mutableStateOf<Reading?>(null) }
    val kwp = profile?.kwp?.takeIf { it > 0 }

    fun exportCsv() = scope.launch {
        val uri = runCatching { vm.csv() }.getOrNull() ?: return@launch context.toast("Export failed")
        vm.ads.interstitials.showAfterAction(context.findActivity()) {
            context.share(vm.exports.shareIntent(uri, "text/csv", "Share readings"))
        }
    }

    val today = d.today
    val (title, total, previousTotal, bars, ticks) = when (range) {
        Range.WEEK -> {
            val days = Aggregation.lastDays(d.days, today, 7)
            val prev = Aggregation.lastDays(d.days, today.minusDays(7), 7).sumOf { it.kwh }
            Quint("Last 7 days", days.sumOf { it.kwh }, prev, days.mapIndexed { i, x -> Bar(x.kwh, i == days.lastIndex, x.estimated) },
                days.filterIndexed { i, _ -> i % 2 == 0 }.map { it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) })
        }
        Range.MONTH -> {
            val month = YearMonth.from(today)
            val days = Aggregation.month(d.days, month).filter { !it.date.isAfter(today) }
            val prev = Aggregation.month(d.days, month.minusMonths(1)).filter { it.date.dayOfMonth <= today.dayOfMonth }.sumOf { it.kwh }
            Quint("${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} so far", days.sumOf { it.kwh }, prev,
                days.mapIndexed { i, x -> Bar(x.kwh, i == days.lastIndex, x.estimated) },
                listOf("1", "${(days.size / 3).coerceAtLeast(1)}", "${(2 * days.size / 3).coerceAtLeast(1)}", "${days.size}").distinct())
        }
        Range.YEAR -> {
            val months = Aggregation.year(d.days, today.year).take(today.monthValue)
            val prev = Aggregation.year(d.days, today.year - 1).take(today.monthValue).sum()
            Quint("${today.year} so far", months.sum(), prev, months.mapIndexed { i, v -> Bar(v, i == months.lastIndex) },
                listOf("Jan", "Apr", "Jul", "Oct").take((today.monthValue + 2) / 3))
        }
    }

    ScreenList(Modifier.fillMaxSize()) {
        item {
            ScreenHeader("History", actions = {
                SecondaryButton("CSV", { exportCsv() }, icon = Icons.Rounded.FileDownload)
            })
        }
        item { Segmented(Range.entries, range, { it.label }, { range = it }) }
        item {
            SunCard(Modifier.fillMaxWidth(), color = Palette.Panel) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(title, fontSize = 12.sp, color = Palette.Muted)
                        val (v, u) = Fmt.energy(total)
                        Text("$v $u", fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Palette.Ink)
                    }
                    if (previousTotal > 0 && total > 0) {
                        val pct = ((total - previousTotal) / previousTotal * 100).toInt()
                        Text("${if (pct >= 0) "+" else "−"}${kotlin.math.abs(pct)}% vs previous", fontFamily = MonoFont, fontSize = 12.sp, color = Palette.Sky)
                    }
                }
                Spacer(Modifier.height(12.dp))
                BarChart(bars, description = "$title production, total ${Fmt.kwh(total)} kilowatt hours", key = range to bars.size)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    ticks.forEach { Text(it, fontFamily = MonoFont, fontSize = 10.sp, color = Palette.Muted) }
                }
            }
        }
        item {
            val stats = Aggregation.stats(if (range == Range.WEEK) Aggregation.lastDays(d.days, today, 7) else Aggregation.month(d.days, d.month), kwp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Best day", Fmt.kwh(stats.bestDay?.kwh), "kWh", Modifier.weight(1f),
                    footnote = stats.bestDay?.date?.format(DateTimeFormatter.ofPattern("d MMM")))
                StatTile("Daily avg", Fmt.kwh(stats.averageKwh.takeIf { stats.daysWithData > 0 }), "kWh", Modifier.weight(1f))
                StatTile("Per kWp", stats.specificYield?.let { Fmt.number(it, 2) } ?: "–", null, Modifier.weight(1f), footnote = "kWh/kWp")
            }
        }
        item {
            SunCard(Modifier.fillMaxWidth(), color = Palette.Panel) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${d.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} heatmap", fontSize = 13.sp, color = Palette.Ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text("less → more", fontSize = 11.sp, color = Palette.Muted)
                }
                Spacer(Modifier.height(10.dp))
                val monthDays = Aggregation.month(d.days, d.month)
                val logged = d.days.associateBy { it.date }
                YieldHeatmap(
                    days = monthDays.map { it.date.dayOfMonth to logged[it.date]?.kwh },
                    firstDayOffset = d.month.atDay(1).dayOfWeek.value - 1,
                    today = today.dayOfMonth,
                    maxKwh = monthDays.maxOfOrNull { it.kwh } ?: 0.0
                )
            }
        }
        item { NativeAdSlot(vm.ads) }
        item { Text("Readings", fontSize = 13.sp, color = Palette.Muted, modifier = Modifier.padding(top = 4.dp)) }
        if (readings.isEmpty()) {
            item { Text("No readings yet. Scan your inverter to start the log.", fontSize = 13.sp, color = Palette.InkSoft) }
        }
        items(readings.take(60), key = { it.id }) { r ->
            ReadingRow(r, inverters.firstOrNull { it.id == r.inverterId }?.name.takeIf { inverters.size > 1 }) { deleting = r }
        }
    }

    deleting?.let { r ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this reading?") },
            text = { Text("This removes it from your log and charts.") },
            confirmButton = { TextButton(onClick = { vm.deleteReading(r.id); deleting = null }) { Text("Delete", color = Palette.Sun) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
            containerColor = Palette.Surface
        )
    }
}

private data class Quint(val a: String, val b: Double, val c: Double, val d: List<Bar>, val e: List<String>)

@Composable
private fun ReadingRow(r: Reading, inverterName: String?, onDelete: () -> Unit) {
    val t = r.timestamp.atZone(java.time.ZoneId.systemDefault())
    SunCard(Modifier.fillMaxWidth(), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (r.source) {
                    ReadingSource.PHOTO -> Icons.Rounded.CameraAlt
                    ReadingSource.SCREENSHOT -> Icons.Rounded.Image
                    ReadingSource.MANUAL -> Icons.Rounded.Edit
                    ReadingSource.NETWORK -> Icons.Rounded.Wifi
                },
                contentDescription = r.source.label, tint = Palette.Muted, modifier = Modifier.size(18.dp)
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    t.format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm")) + (inverterName?.let { " · $it" } ?: ""),
                    fontSize = 13.sp, color = Palette.Ink
                )
                val parts = buildList {
                    r.todayKwh?.let { add("Today ${Fmt.kwh(it)} kWh") }
                    r.totalKwh?.let { add("Total ${Fmt.kwh(it)}") }
                    r.powerW?.let { val (v, u) = Fmt.power(it); add("$v $u") }
                    r.faultCode?.let { add("Fault $it") }
                }
                Text(parts.joinToString(" · "), fontFamily = MonoFont, fontSize = 12.sp, color = Palette.InkSoft)
                r.note?.let { Text(it, fontSize = 12.sp, color = Palette.Muted) }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete reading", tint = Palette.Muted) }
        }
    }
}
