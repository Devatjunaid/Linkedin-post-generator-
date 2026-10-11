package com.shelfguard.app.ui.dashboard

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.RemoveShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shelfguard.app.AppContainer
import com.shelfguard.app.ads.NativeAdCard
import com.shelfguard.app.data.prefs.AppSettings
import com.shelfguard.app.domain.AlertSummary
import com.shelfguard.app.domain.StockItem
import com.shelfguard.app.domain.StockStatus
import com.shelfguard.app.notifications.NotificationHelper
import com.shelfguard.app.ui.components.AppCard
import com.shelfguard.app.ui.components.EmptyState
import com.shelfguard.app.ui.components.SectionHeader
import com.shelfguard.app.ui.components.ShelfGuardLogo
import com.shelfguard.app.ui.components.StockRow
import com.shelfguard.app.ui.components.expirySubtitle
import com.shelfguard.app.ui.components.statusStyle
import com.shelfguard.app.ui.navigation.AppNavigator
import com.shelfguard.app.ui.theme.AppTheme
import com.shelfguard.app.util.Format
import com.shelfguard.app.util.containerViewModel
import com.shelfguard.app.util.findActivity
import com.shelfguard.app.util.startBarcodeScan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class DashboardState(
    val loading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val productCount: Int = 0,
    val totalUnits: Int = 0,
    val summary: AlertSummary = AlertSummary(emptyList(), emptyList(), emptyList(), emptyList()),
    val attention: List<StockItem> = emptyList(),
    val valueAtCost: Double = 0.0,
    val valueAtPrice: Double = 0.0,
)

class DashboardViewModel(private val container: AppContainer) : ViewModel() {
    val state = combine(container.inventory.observeStock(), container.settings.settings) { items, settings ->
        val summary = AlertSummary.from(items)
        DashboardState(
            loading = false,
            settings = settings,
            productCount = items.size,
            totalUnits = items.sumOf { it.sellableQty },
            summary = summary,
            attention = items.filter { it.needsAttention }
                .sortedWith(compareBy({ it.status.ordinal }, { it.daysToExpiry ?: Long.MAX_VALUE }))
                .take(5),
            valueAtCost = items.sumOf { it.stockValueAtCost },
            valueAtPrice = items.sumOf { it.stockValueAtPrice },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    suspend fun findByBarcode(code: String) = container.inventory.findByBarcode(code)
}

@Composable
fun DashboardScreen(navigator: AppNavigator) {
    val vm = containerViewModel { DashboardViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var notificationsAllowed by remember { mutableStateOf(NotificationHelper.canPost(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { notificationsAllowed = NotificationHelper.canPost(context) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted
        val permanentlyDenied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == false
        if (!granted && permanentlyDenied) {
            // Android won't show the prompt any more, so open the app's notification settings instead.
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    val scan: () -> Unit = {
        startBarcodeScan(
            context,
            onResult = { code ->
                scope.launch {
                    val found = vm.findByBarcode(code)
                    if (found != null) navigator.product(found.id) else navigator.newProduct(code)
                }
            },
            onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Header(state.settings.shopName, onScan = scan, onSettings = navigator::settings) }

            if (!notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    AppCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Outlined.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text("Turn on alerts", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Get a daily heads-up before stock expires or runs out.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text("Allow")
                            }
                        }
                    }
                }
            }

            if (!state.loading && state.productCount == 0) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Inventory2,
                        title = "Add your first product",
                        message = "Track quantities and expiry dates. ShelfGuard warns you before stock expires or runs low.",
                        action = {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = scan) {
                                    Icon(Icons.Outlined.QrCodeScanner, null, Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text("Scan")
                                }
                                Button(onClick = { navigator.newProduct() }) {
                                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text("Add product")
                                }
                            }
                        },
                    )
                }
            } else if (!state.loading) {
                item { StatGrid(state, navigator) }
                item { ValueCard(state) }

                item {
                    SectionHeader(
                        "Needs attention",
                        action = if (state.summary.total > 0) "See all" else null,
                        onAction = navigator::alerts,
                    )
                }
                if (state.attention.isEmpty()) {
                    item {
                        AppCard(Modifier.fillMaxWidth()) {
                            Text(
                                "All good. Nothing is expired, expiring soon or running low.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    items(state.attention, key = { it.id }) { item ->
                        val style = statusStyle(item.status)
                        StockRow(
                            item = item,
                            subtitle = attentionText(item),
                            subtitleColor = style.color,
                            onClick = { navigator.product(item.id) },
                        )
                    }
                }
            }

            item { SectionHeader("Quick actions") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickAction(Icons.Outlined.Add, "Add item", Modifier.weight(1f)) { navigator.newProduct() }
                    QuickAction(Icons.Outlined.QrCodeScanner, "Scan", Modifier.weight(1f), scan)
                    QuickAction(Icons.Outlined.Inventory2, "Restock", Modifier.weight(1f)) { navigator.inventory("low") }
                    QuickAction(Icons.Outlined.BarChart, "Reports", Modifier.weight(1f), navigator::reports)
                }
            }

            // Quiet native ad in the spare space at the end of the page.
            item { NativeAdCard(Modifier.padding(top = 4.dp)) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private fun attentionText(item: StockItem): String = when (item.status) {
    StockStatus.EXPIRED -> "${item.expiredQty} ${item.product.unit} expired · remove from shelf"
    StockStatus.OUT_OF_STOCK -> "Out of stock · restock needed"
    StockStatus.EXPIRING_SOON -> "${item.nextExpiry?.let { Format.expiry(it) }} · ${item.expiringSoonQty} ${item.product.unit}"
    StockStatus.LOW_STOCK -> "${item.sellableQty} left · minimum ${item.product.minStock}"
    StockStatus.OK -> expirySubtitle(item)
}

@Composable
private fun Header(shopName: String, onScan: () -> Unit, onSettings: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShelfGuardLogo(Modifier.size(44.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text("$greeting · $today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(shopName, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onScan) { Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scan barcode") }
        IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
    }
}

@Composable
private fun StatGrid(state: DashboardState, navigator: AppNavigator) {
    val c = AppTheme.status
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                onClick = { navigator.inventory("all") },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Products", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
                    Text(Format.number(state.productCount), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimary)
                    Text("${Format.number(state.totalUnits)} units in stock", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
                }
            }
            StatTile(
                "Low stock",
                state.summary.lowStock.size + state.summary.outOfStock.size,
                if (state.summary.outOfStock.isEmpty()) "at or below minimum" else "${state.summary.outOfStock.size} out of stock",
                Icons.Outlined.TrendingDown, c.low, c.lowContainer, Modifier.weight(1f),
            ) { navigator.inventory("low") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                "Expiring soon", state.summary.expiringSoon.size, "within alert window",
                Icons.Outlined.Schedule, c.expiring, c.expiringContainer, Modifier.weight(1f),
            ) { navigator.inventory("expiring") }
            StatTile(
                "Expired", state.summary.expired.size, "remove from shelf",
                Icons.Outlined.Warning, c.expired, c.expiredContainer, Modifier.weight(1f),
            ) { navigator.inventory("expired") }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: Int,
    caption: String,
    icon: ImageVector,
    color: Color,
    container: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(container),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = color, modifier = Modifier.size(16.dp)) }
            }
            Text(Format.number(value), style = MaterialTheme.typography.displaySmall)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun ValueCard(state: DashboardState) {
    val currency = state.settings.currency
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Stock value (cost)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(Format.money(state.valueAtCost, currency), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
            Column(Modifier.weight(1f)) {
                Text("Retail value", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(Format.money(state.valueAtPrice, currency), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
            Column(Modifier.weight(1f)) {
                Text("Potential profit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(
                    Format.money(state.valueAtPrice - state.valueAtCost, currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier = modifier, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
