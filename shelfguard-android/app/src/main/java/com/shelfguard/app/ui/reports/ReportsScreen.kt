package com.shelfguard.app.ui.reports

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shelfguard.app.AppContainer
import com.shelfguard.app.ads.FullScreenAdGate
import com.shelfguard.app.ads.NativeAdCard
import com.shelfguard.app.data.db.MovementEntity
import com.shelfguard.app.data.db.MovementType
import com.shelfguard.app.data.db.movementType
import com.shelfguard.app.data.prefs.AppSettings
import com.shelfguard.app.domain.StockItem
import com.shelfguard.app.ui.components.AppCard
import com.shelfguard.app.ui.components.SectionHeader
import com.shelfguard.app.ui.theme.AppTheme
import com.shelfguard.app.util.Format
import com.shelfguard.app.util.containerViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class Period(val label: String, val days: Int) { WEEK("7 days", 7), MONTH("30 days", 30), QUARTER("90 days", 90) }

data class TopSeller(val name: String, val units: Int, val revenue: Double)
data class Bar(val label: String, val value: Double)

data class ReportState(
    val period: Period = Period.WEEK,
    val settings: AppSettings = AppSettings(),
    val revenue: Double = 0.0,
    val profit: Double = 0.0,
    val unitsSold: Int = 0,
    val sales: Int = 0,
    val wasteUnits: Int = 0,
    val wasteValue: Double = 0.0,
    val restockedUnits: Int = 0,
    val bars: List<Bar> = emptyList(),
    val topSellers: List<TopSeller> = emptyList(),
    val productCount: Int = 0,
    val valueAtCost: Double = 0.0,
    val valueAtPrice: Double = 0.0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(private val container: AppContainer) : ViewModel() {
    private val period = MutableStateFlow(Period.WEEK)
    private val zone = ZoneId.systemDefault()

    private val movements = period.flatMapLatest { p ->
        val since = LocalDate.now().minusDays(p.days - 1L).atStartOfDay(zone).toInstant().toEpochMilli()
        container.inventory.observeMovementsSince(since)
    }

    val state = combine(period, movements, container.inventory.observeStock(), container.settings.settings) { p, moves, stock, settings ->
        build(p, moves, stock, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportState())

    fun setPeriod(p: Period) { period.value = p }

    private fun build(p: Period, moves: List<MovementEntity>, stock: List<StockItem>, settings: AppSettings): ReportState {
        val sales = moves.filter { it.movementType == MovementType.SALE }
        val waste = moves.filter { it.movementType == MovementType.WASTE }
        val today = LocalDate.now()
        fun dayOf(m: MovementEntity) = Instant.ofEpochMilli(m.timestamp).atZone(zone).toLocalDate()

        val bars = if (p == Period.QUARTER) {
            // 13 weekly bars.
            (12 downTo 0).map { w ->
                val end = today.minusWeeks(w.toLong())
                val start = end.minusDays(6)
                val v = sales.filter { dayOf(it) in start..end }.sumOf { it.quantity * it.unitPrice }
                Bar(start.format(DateTimeFormatter.ofPattern("d/M")), v)
            }
        } else {
            val byDay = sales.groupBy(::dayOf)
            (p.days - 1 downTo 0).map { d ->
                val day = today.minusDays(d.toLong())
                val label = if (p == Period.WEEK) day.format(DateTimeFormatter.ofPattern("EEE")) else day.dayOfMonth.toString()
                Bar(label, byDay[day].orEmpty().sumOf { it.quantity * it.unitPrice })
            }
        }

        return ReportState(
            period = p,
            settings = settings,
            revenue = sales.sumOf { it.quantity * it.unitPrice },
            profit = sales.sumOf { it.quantity * (it.unitPrice - it.unitCost) },
            unitsSold = sales.sumOf { it.quantity },
            sales = sales.size,
            wasteUnits = waste.sumOf { it.quantity },
            wasteValue = waste.sumOf { it.quantity * it.unitCost },
            restockedUnits = moves.filter { it.movementType == MovementType.RESTOCK }.sumOf { it.quantity },
            bars = bars,
            topSellers = sales.groupBy { it.productName }
                .map { (name, list) -> TopSeller(name, list.sumOf { it.quantity }, list.sumOf { it.quantity * it.unitPrice }) }
                .sortedByDescending { it.revenue }
                .take(5),
            productCount = stock.size,
            valueAtCost = stock.sumOf { it.stockValueAtCost },
            valueAtPrice = stock.sumOf { it.stockValueAtPrice },
        )
    }

    /** Current inventory as CSV, for sharing to a spreadsheet or email. */
    suspend fun inventoryCsv(): String {
        val items = container.inventory.stockSnapshot()
        fun esc(v: String) = if (v.any { it == ',' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
        return buildString {
            appendLine("Name,Category,Barcode,Unit,In stock,Expired,Next expiry,Min stock,Cost,Price,Supplier,Status")
            items.sortedBy { it.product.name.lowercase() }.forEach { i ->
                val p = i.product
                appendLine(
                    listOf(
                        esc(p.name), esc(p.category), esc(p.barcode.orEmpty()), esc(p.unit),
                        i.sellableQty.toString(), i.expiredQty.toString(), i.nextExpiry?.toString().orEmpty(),
                        p.minStock.toString(), p.costPrice.toString(), p.sellPrice.toString(), esc(p.supplier),
                        i.status.name.lowercase().replace('_', ' '),
                    ).joinToString(","),
                )
            }
        }
    }
}

@Composable
fun ReportsScreen() {
    val vm = containerViewModel { ReportsViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currency = s.settings.currency

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                Modifier.statusBarsPadding().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Reports", style = MaterialTheme.typography.headlineMedium)
                    Text("Sales, profit and waste", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {
                    scope.launch {
                        val csv = vm.inventoryCsv()
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "${s.settings.shopName} inventory ${LocalDate.now()}")
                            putExtra(Intent.EXTRA_TEXT, csv)
                        }
                        FullScreenAdGate.skipNextAppOpen = true
                        context.startActivity(Intent.createChooser(send, "Export inventory"))
                    }
                }) { Icon(Icons.Outlined.IosShare, contentDescription = "Export inventory as CSV") }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Period.entries.forEachIndexed { i, p ->
                    SegmentedButton(
                        selected = s.period == p,
                        onClick = { vm.setPeriod(p) },
                        shape = SegmentedButtonDefaults.itemShape(i, Period.entries.size),
                        label = { Text(p.label) },
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("Revenue", Format.money(s.revenue, currency), "${s.sales} sales", Modifier.weight(1f), highlight = true)
                Metric("Profit", Format.money(s.profit, currency), "${s.unitsSold} units sold", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric(
                    "Waste", Format.money(s.wasteValue, currency), "${s.wasteUnits} units expired",
                    Modifier.weight(1f), valueColor = if (s.wasteUnits > 0) AppTheme.status.expired else null,
                )
                Metric("Restocked", Format.number(s.restockedUnits), "units received", Modifier.weight(1f))
            }
        }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Daily revenue", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(16.dp))
                    BarChart(s.bars, currency)
                }
            }
        }
        item { SectionHeader("Top sellers") }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                if (s.topSellers.isEmpty()) {
                    Text(
                        "No sales recorded in this period. Record sales from a product's page.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else Column {
                    s.topSellers.forEachIndexed { i, t ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${t.units} units", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(Format.money(t.revenue, currency), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }
        item { SectionHeader("Stock on hand") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("At cost", Format.money(s.valueAtCost, currency), "${s.productCount} products", Modifier.weight(1f))
                Metric("At retail", Format.money(s.valueAtPrice, currency), "if all sold", Modifier.weight(1f))
            }
        }
        item { NativeAdCard() }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier,
    highlight: Boolean = false,
    valueColor: Color? = null,
) {
    AppCard(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = valueColor ?: if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BarChart(bars: List<Bar>, currency: String) {
    val max = bars.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0
    val total = bars.sumOf { it.value }
    val barColor = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(Modifier.semantics { contentDescription = "Revenue chart, total ${Format.money(total, currency)}" }) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            if (bars.isEmpty()) return@Canvas
            val gap = if (bars.size > 14) 3.dp.toPx() else 8.dp.toPx()
            val w = (size.width - gap * (bars.size - 1)) / bars.size
            val radius = CornerRadius(minOf(w / 2, 6.dp.toPx()))
            bars.forEachIndexed { i, b ->
                val x = i * (w + gap)
                drawRoundRect(track, Offset(x, 0f), Size(w, size.height), radius)
                val h = (b.value / max * size.height).toFloat()
                if (h > 0f) drawRoundRect(barColor, Offset(x, size.height - h), Size(w, h), radius)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            if (bars.size <= 7) {
                bars.forEach {
                    Text(
                        it.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                    )
                }
            } else {
                Text(bars.first().label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(bars.last().label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
