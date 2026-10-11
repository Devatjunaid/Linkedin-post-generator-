package com.shelfguard.app.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shelfguard.app.AppContainer
import com.shelfguard.app.data.prefs.AppSettings
import com.shelfguard.app.domain.AlertSummary
import com.shelfguard.app.domain.StockItem
import com.shelfguard.app.ui.components.AppCard
import com.shelfguard.app.ui.components.ConfirmDialog
import com.shelfguard.app.ui.components.EmptyState
import com.shelfguard.app.ui.components.RestockDialog
import com.shelfguard.app.ui.components.StatusBadge
import com.shelfguard.app.ui.components.StockRow
import com.shelfguard.app.ui.navigation.AppNavigator
import com.shelfguard.app.ui.theme.AppTheme
import com.shelfguard.app.util.Format
import com.shelfguard.app.util.containerViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class AlertsState(
    val loading: Boolean = true,
    val summary: AlertSummary = AlertSummary(emptyList(), emptyList(), emptyList(), emptyList()),
    val settings: AppSettings = AppSettings(),
)

class AlertsViewModel(private val container: AppContainer) : ViewModel() {
    val state = combine(container.inventory.observeStock(), container.settings.settings) { items, settings ->
        AlertsState(false, AlertSummary.from(items), settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertsState())

    suspend fun removeExpired(productId: Long?) = container.inventory.removeExpired(productId)
    fun restock(productId: Long, qty: Int, expiry: LocalDate?, cost: Double?) =
        viewModelScope.launch { container.inventory.restock(productId, qty, expiry, cost) }
}

@Composable
fun AlertsScreen(navigator: AppNavigator) {
    val vm = containerViewModel { AlertsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmRemoveAll by remember { mutableStateOf(false) }
    var restocking by remember { mutableStateOf<StockItem?>(null) }
    val s = state.summary
    val c = AppTheme.status

    if (confirmRemoveAll) ConfirmDialog(
        title = "Remove all expired stock?",
        message = "Expired batches of ${s.expired.size} products will be removed and recorded as waste.",
        confirmLabel = "Remove all",
        destructive = true,
        onConfirm = {
            scope.launch {
                val units = vm.removeExpired(null)
                snackbar.showSnackbar("Removed $units expired units")
            }
        },
        onDismiss = { confirmRemoveAll = false },
    )
    restocking?.let { item ->
        RestockDialog(
            productName = item.product.name,
            unit = item.product.unit,
            defaultCost = item.product.costPrice,
            currency = state.settings.currency,
            suggestedQty = (item.product.minStock * 2 - item.sellableQty).coerceAtLeast(0),
            onConfirm = { q, exp, cost ->
                vm.restock(item.id, q, exp, cost)
                restocking = null
                scope.launch { snackbar.showSnackbar("${item.product.name} restocked") }
            },
            onDismiss = { restocking = null },
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(Modifier.statusBarsPadding().padding(top = 16.dp, bottom = 4.dp)) {
                    Text("Alerts", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (s.total == 0) "Nothing needs attention" else "${s.total} products need attention",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { ReminderCard(state.settings, onChange = navigator::settings) }

            if (!state.loading && s.total == 0) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.CheckCircle,
                        title = "All clear",
                        message = "No expired, expiring or low-stock products right now.",
                    )
                }
            }

            section(
                title = "Expired · ${s.expired.size}",
                color = c.expired,
                list = s.expired,
                headerAction = if (s.expired.size > 1) ("Remove all" to { confirmRemoveAll = true }) else null,
                subtitle = { "${it.expiredQty} ${it.product.unit} expired" },
                onOpen = { navigator.product(it.id) },
            ) { item ->
                FilledTonalButton(onClick = {
                    scope.launch {
                        val units = vm.removeExpired(item.id)
                        snackbar.showSnackbar("Removed $units ${item.product.unit} of ${item.product.name}")
                    }
                }) { Text("Remove") }
            }

            section(
                title = "Expiring soon · ${s.expiringSoon.size}",
                color = c.expiring,
                list = s.expiringSoon,
                subtitle = { item ->
                    "${item.expiringSoonQty} ${item.product.unit} · ${item.nextExpiry?.let(Format::date)}"
                },
                onOpen = { navigator.product(it.id) },
            ) { item ->
                item.daysToExpiry?.let { StatusBadge(Format.daysBadge(it), c.expiring, c.expiringContainer) }
            }

            section(
                title = "Out of stock · ${s.outOfStock.size}",
                color = c.out,
                list = s.outOfStock,
                subtitle = { "No sellable stock left" },
                onOpen = { navigator.product(it.id) },
            ) { item ->
                FilledTonalButton(onClick = { restocking = item }) { Text("Restock") }
            }

            section(
                title = "Low stock · ${s.lowStock.size}",
                color = c.low,
                list = s.lowStock,
                subtitle = { "${it.sellableQty} left · minimum ${it.product.minStock}" },
                onOpen = { navigator.product(it.id) },
            ) { item ->
                FilledTonalButton(onClick = { restocking = item }) { Text("Restock") }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private fun LazyListScope.section(
    title: String,
    color: Color,
    list: List<StockItem>,
    subtitle: (StockItem) -> String,
    onOpen: (StockItem) -> Unit,
    headerAction: Pair<String, () -> Unit>? = null,
    trailing: @Composable (StockItem) -> Unit,
) {
    if (list.isEmpty()) return
    item(key = "header-$title") {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                modifier = Modifier.weight(1f),
            )
            headerAction?.let { (label, onClick) -> TextButton(onClick = onClick) { Text(label) } }
        }
    }
    items(list, key = { "$title-${it.id}" }) { item ->
        StockRow(item = item, subtitle = subtitle(item), onClick = { onOpen(item) }, trailing = { trailing(item) })
    }
}

@Composable
private fun ReminderCard(settings: AppSettings, onChange: () -> Unit) {
    val time = LocalTime.of(settings.reminderHour, settings.reminderMinute).format(DateTimeFormatter.ofPattern("h:mm a"))
    AppCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (settings.remindersEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (settings.remindersEnabled) "Daily summary at $time" else "Daily summary is off",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            TextButton(onClick = onChange) { Text("Change") }
        }
    }
}
