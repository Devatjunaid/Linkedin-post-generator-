package com.odomate.app.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.odomate.app.AppContainer
import com.odomate.app.ads.NativeAdCard
import com.odomate.app.data.db.FuelEntry
import com.odomate.app.data.db.OdometerReading
import com.odomate.app.data.db.ReadingSource
import com.odomate.app.data.db.ServiceRecord
import com.odomate.app.domain.Stats
import com.odomate.app.ui.LocalAppContainer
import com.odomate.app.ui.LocalMessenger
import com.odomate.app.ui.LocalSettings
import com.odomate.app.ui.appViewModel
import com.odomate.app.ui.components.AppCard
import com.odomate.app.ui.components.ConfirmDialog
import com.odomate.app.ui.components.EmptyState
import com.odomate.app.ui.components.IconBadge
import com.odomate.app.ui.components.ScreenHeader
import com.odomate.app.ui.components.SectionLabel
import com.odomate.app.ui.theme.AppTheme
import com.odomate.app.util.Formats
import com.odomate.app.util.findActivity
import com.odomate.app.util.shareFile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

sealed class TimelineItem(val date: Long, val key: String) {
    class Service(val record: ServiceRecord) : TimelineItem(record.date, "s${record.id}")
    class Fuel(val entry: FuelEntry) : TimelineItem(entry.date, "f${entry.id}")
    class Reading(val reading: OdometerReading, val delta: Int?) : TimelineItem(reading.date, "r${reading.id}")
}

class HistoryViewModel(private val container: AppContainer) : ViewModel() {
    val snapshot = container.repository.activeSnapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun deleteReading(reading: OdometerReading) {
        viewModelScope.launch { container.repository.deleteReading(reading) }
    }

    suspend fun exportCsv(vehicleId: Long) = container.backup.exportCsv(vehicleId)
}

@Composable
fun HistoryScreen(onOpenRecord: (Long) -> Unit, onOpenFuel: (Long) -> Unit) {
    val vm = appViewModel { c, _ -> HistoryViewModel(c) }
    val snap by vm.snapshot.collectAsStateWithLifecycle()
    val settings = LocalSettings.current
    val unit = settings.distanceUnit
    val c = AppTheme.colors
    val context = LocalContext.current
    val ads = LocalAppContainer.current.ads
    val message = LocalMessenger.current
    val scope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var readingToDelete by remember { mutableStateOf<OdometerReading?>(null) }

    val s = snap
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("History", subtitle = s?.vehicle?.name) {
            IconButton(onClick = {
                val vehicleId = s?.vehicle?.id ?: return@IconButton
                scope.launch {
                    vm.exportCsv(vehicleId)
                        .onSuccess { uri ->
                            ads.showInterstitial(context.findActivity()) {
                                context.shareFile(uri, "text/csv", "Export history")
                            }
                        }
                        .onFailure { message("Export failed: ${it.message}") }
                }
            }) { Icon(Icons.Outlined.FileDownload, contentDescription = "Export CSV", tint = c.text) }
        }
        if (s == null) return@Column

        val year = LocalDate.now().year
        val items = remember(s.records, s.fuel, s.readings) { buildTimeline(s.records, s.fuel, s.readings) }
        val filtered = when (filter) {
            1 -> items.filterIsInstance<TimelineItem.Service>()
            2 -> items.filterIsInstance<TimelineItem.Fuel>()
            3 -> items.filterIsInstance<TimelineItem.Reading>()
            else -> items
        }
        val bars = remember(s.readings) { Stats.monthlyDistance(s.readings, 10) }
        val spent = remember(s.records, s.fuel) { Stats.spentInYear(s.records, s.fuel, year) }
        val servicesThisYear = s.records.count { com.odomate.app.domain.StatusCalculator.toLocalDate(it.date).year == year }
        val driven = remember(s.readings) { Stats.distanceInYear(s.readings, year) }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                AppCard {
                    Row {
                        Stat("Spent in $year", Formats.money(spent, settings.currency), Modifier.weight(1f))
                        Stat("Services", servicesThisYear.toString(), Modifier.weight(1f))
                        Stat("Driven", Formats.distance(driven, unit), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    MonthlyBars(bars.map { it.distance })
                    Spacer(Modifier.height(6.dp))
                    Row {
                        Text(bars.firstOrNull()?.month?.month?.getDisplayName(TextStyle.SHORT, Locale.getDefault()) ?: "", color = c.muted, style = MaterialTheme.typography.labelSmall)
                        Text("$unit driven per month", color = c.muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text(bars.lastOrNull()?.month?.month?.getDisplayName(TextStyle.SHORT, Locale.getDefault()) ?: "", color = c.muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val labels = listOf("All", "Services", "Fuel", "Odometer")
                    items(labels.size) { i ->
                        FilterChip(
                            selected = filter == i,
                            onClick = { filter = i },
                            label = { Text(labels[i]) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = c.surface, labelColor = c.muted,
                                selectedContainerColor = c.text, selectedLabelColor = c.bg,
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = filter == i, borderColor = c.outline, selectedBorderColor = c.text),
                        )
                    }
                }
            }

            item { NativeAdCard() }

            if (filtered.isEmpty()) {
                item { EmptyState(Icons.Outlined.History, "Nothing here yet", "Logged services, fill-ups and odometer scans will appear here.") }
            }

            var lastHeader: String? = null
            filtered.forEach { entry ->
                val header = Formats.monthHeader(entry.date)
                if (header != lastHeader) {
                    lastHeader = header
                    item(key = "h_${header}_${entry.key}") { SectionLabel(header) }
                }
                item(key = entry.key) {
                    when (entry) {
                        is TimelineItem.Service -> TimelineRow(
                            icon = Icons.Outlined.Build, tint = c.accentText, bg = c.accentSoft,
                            title = entry.record.serviceNames,
                            subtitle = listOf(
                                Formats.shortDate(entry.record.date),
                                Formats.distance(entry.record.odometer, unit),
                                entry.record.shop,
                                if (entry.record.receiptPath != null) "receipt" else "",
                            ).filter { it.isNotBlank() }.joinToString(" · "),
                            trailing = entry.record.cost?.let { Formats.money(it, settings.currency) },
                            onClick = { onOpenRecord(entry.record.id) },
                        )
                        is TimelineItem.Fuel -> TimelineRow(
                            icon = Icons.Outlined.LocalGasStation, tint = c.ok, bg = c.okSoft,
                            title = "Fuel · ${Formats.decimal(entry.entry.volume, 2)} ${settings.volumeUnit}" + if (entry.entry.fullTank) " (full)" else "",
                            subtitle = listOf(Formats.shortDate(entry.entry.date), Formats.distance(entry.entry.odometer, unit), entry.entry.station)
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            trailing = entry.entry.cost?.let { Formats.money(it, settings.currency) },
                            onClick = { onOpenFuel(entry.entry.id) },
                        )
                        is TimelineItem.Reading -> TimelineRow(
                            icon = if (entry.reading.source == ReadingSource.SCAN) Icons.Outlined.PhotoCamera else Icons.Outlined.Speed,
                            tint = c.muted, bg = c.surface2,
                            title = when (entry.reading.source) {
                                ReadingSource.SCAN -> "Odometer scanned"
                                ReadingSource.INITIAL -> "Starting odometer"
                                ReadingSource.SERVICE -> "Odometer at service"
                                ReadingSource.FUEL -> "Odometer at fill-up"
                                else -> "Odometer entered"
                            },
                            subtitle = listOfNotNull(
                                Formats.shortDate(entry.reading.date),
                                Formats.distance(entry.reading.value, unit),
                                entry.delta?.takeIf { it > 0 }?.let { "+${Formats.number(it)}" },
                            ).joinToString(" · "),
                            trailing = null,
                            onClick = { readingToDelete = entry.reading },
                        )
                    }
                }
            }
        }
    }

    readingToDelete?.let { r ->
        ConfirmDialog(
            title = "Delete this reading?",
            text = "${Formats.distance(r.value, unit)} on ${Formats.date(r.date)}. Use this to remove a mistaken scan.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { vm.deleteReading(r); message("Reading deleted") },
            onDismiss = { readingToDelete = null },
        )
    }
}

private fun buildTimeline(records: List<ServiceRecord>, fuel: List<FuelEntry>, readings: List<OdometerReading>): List<TimelineItem> {
    val sortedReadings = readings.sortedWith(compareBy<OdometerReading> { it.date }.thenBy { it.value })
    val readingItems = sortedReadings.mapIndexed { i, r ->
        TimelineItem.Reading(r, if (i == 0) null else r.value - sortedReadings[i - 1].value)
    }.filter { it.reading.source == ReadingSource.SCAN || it.reading.source == ReadingSource.MANUAL || it.reading.source == ReadingSource.INITIAL }
    return (records.map { TimelineItem.Service(it) } + fuel.map { TimelineItem.Fuel(it) } + readingItems)
        .sortedByDescending { it.date }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    val c = AppTheme.colors
    Column(modifier) {
        Text(label, color = c.muted, style = MaterialTheme.typography.bodySmall)
        Text(value, color = c.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun MonthlyBars(values: List<Int>) {
    val c = AppTheme.colors
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val track = c.surface2
    val bar = c.faint
    val accent = c.accent
    Canvas(Modifier.fillMaxWidth().height(72.dp)) {
        if (values.isEmpty()) return@Canvas
        val gap = 6.dp.toPx()
        val w = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val x = i * (w + gap)
            drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(4.dp.toPx()))
            val h = size.height * v / max
            if (h > 0f) {
                drawRoundRect(
                    if (i == values.lastIndex) accent else bar,
                    Offset(x, size.height - h), Size(w, h), CornerRadius(4.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    bg: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    trailing: String?,
    onClick: () -> Unit,
) {
    val c = AppTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon, tint, bg, 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = c.text, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Text(subtitle, color = c.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            trailing?.let { Text(it, color = c.text, fontWeight = FontWeight.SemiBold) }
        }
        HorizontalDivider(color = c.divider)
    }
}
