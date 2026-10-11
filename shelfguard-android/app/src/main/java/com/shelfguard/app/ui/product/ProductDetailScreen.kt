package com.shelfguard.app.ui.product

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shelfguard.app.AppContainer
import com.shelfguard.app.data.db.BatchEntity
import com.shelfguard.app.data.db.MovementEntity
import com.shelfguard.app.data.db.MovementType
import com.shelfguard.app.data.db.movementType
import com.shelfguard.app.data.prefs.AppSettings
import com.shelfguard.app.data.repo.StockException
import com.shelfguard.app.domain.StockItem
import com.shelfguard.app.notifications.NotificationHelper
import com.shelfguard.app.ui.components.AppCard
import com.shelfguard.app.ui.components.ConfirmDialog
import com.shelfguard.app.ui.components.EditBatchDialog
import com.shelfguard.app.ui.components.ProductAvatar
import com.shelfguard.app.ui.components.RestockDialog
import com.shelfguard.app.ui.components.SaleDialog
import com.shelfguard.app.ui.components.SectionHeader
import com.shelfguard.app.ui.navigation.AppNavigator
import com.shelfguard.app.ui.theme.AppTheme
import com.shelfguard.app.util.Format
import com.shelfguard.app.util.containerViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ProductState(
    val loading: Boolean = true,
    val stock: StockItem? = null,
    val settings: AppSettings = AppSettings(),
    val history: List<MovementEntity> = emptyList(),
)

class ProductDetailViewModel(private val container: AppContainer, private val productId: Long) : ViewModel() {
    private val repo = container.inventory
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    val state = combine(
        repo.observeItem(productId),
        container.settings.settings,
        repo.observeProductHistory(productId),
    ) { stock, settings, history ->
        ProductState(loading = false, stock = stock, settings = settings, history = history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductState())

    private fun run(success: String? = null, block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
            success?.let { messages.emit(it) }
        } catch (e: StockException) {
            messages.emit(e.message ?: "Something went wrong")
        }
    }

    fun sell(context: Context, qty: Int, price: Double) = run("Sale recorded") {
        val before = state.value.stock ?: return@run
        val remaining = repo.recordSale(productId, qty, price)
        val min = before.product.minStock
        // Heads-up the moment a sale pushes stock to (or below) the minimum.
        if ((min > 0 && before.sellableQty > min && remaining <= min) || (remaining == 0 && before.sellableQty > 0)) {
            NotificationHelper.postLowStock(context.applicationContext, productId, before.product.name, remaining, before.product.unit)
        }
    }

    fun restock(qty: Int, expiry: LocalDate?, cost: Double?) = run("Stock added") { repo.restock(productId, qty, expiry, cost) }
    fun editBatch(batch: BatchEntity, qty: Int, expiry: LocalDate?) = run("Batch updated") { repo.editBatch(batch.id, qty, expiry) }
    fun deleteBatch(batch: BatchEntity) = run("Batch removed") { repo.deleteBatch(batch.id) }
    fun removeExpired() = run("Expired stock removed") { repo.removeExpired(productId) }
    /** The screen leaves by itself once the product stops existing. */
    fun delete() = viewModelScope.launch { repo.deleteProduct(productId) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(navigator: AppNavigator, productId: Long) {
    val vm = containerViewModel(key = "product-$productId") { ProductDetailViewModel(it, productId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var menu by remember { mutableStateOf(false) }
    var showSale by remember { mutableStateOf(false) }
    var showRestock by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmRemoveExpired by remember { mutableStateOf(false) }
    var editingBatch by remember { mutableStateOf<BatchEntity?>(null) }

    LaunchedEffect(vm) { vm.messages.collect { scope.launch { snackbar.showSnackbar(it) } } }

    val stock = state.stock
    // Product deleted elsewhere (or bad id): leave.
    LaunchedEffect(state.loading, stock) { if (!state.loading && stock == null) navigator.back() }
    if (stock == null) return
    val p = stock.product
    val currency = state.settings.currency
    val c = AppTheme.status

    if (showSale) SaleDialog(
        productName = p.name, unit = p.unit, available = stock.sellableQty, defaultPrice = p.sellPrice,
        currency = currency,
        onConfirm = { q, price -> vm.sell(context, q, price); showSale = false },
        onDismiss = { showSale = false },
    )
    if (showRestock) RestockDialog(
        productName = p.name, unit = p.unit, defaultCost = p.costPrice, currency = currency,
        suggestedQty = if (p.minStock > 0 && stock.sellableQty <= p.minStock) p.minStock * 2 - stock.sellableQty else 0,
        onConfirm = { q, exp, cost -> vm.restock(q, exp, cost); showRestock = false },
        onDismiss = { showRestock = false },
    )
    editingBatch?.let { batch ->
        EditBatchDialog(
            unit = p.unit,
            quantity = batch.quantity,
            expiry = batch.expiryEpochDay?.let(LocalDate::ofEpochDay),
            onConfirm = { q, exp -> vm.editBatch(batch, q, exp) },
            onDelete = { vm.deleteBatch(batch) },
            onDismiss = { editingBatch = null },
        )
    }
    if (confirmDelete) ConfirmDialog(
        title = "Delete ${p.name}?",
        message = "This removes the product and all its batches. Sales history stays in your reports.",
        confirmLabel = "Delete",
        destructive = true,
        onConfirm = vm::delete,
        onDismiss = { confirmDelete = false },
    )
    if (confirmRemoveExpired) ConfirmDialog(
        title = "Remove expired stock?",
        message = "${stock.expiredQty} ${p.unit} will be removed and recorded as waste.",
        confirmLabel = "Remove",
        destructive = true,
        onConfirm = vm::removeExpired,
        onDismiss = { confirmRemoveExpired = false },
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { navigator.editProduct(p.id) }) { Icon(Icons.Outlined.Edit, "Edit product") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "More options") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Delete product", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; confirmDelete = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProductAvatar(p.name, p.category, size = 64.dp)
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(p.category.ifBlank { null }, p.barcode?.let { "Barcode $it" }).joinToString(" · ").ifEmpty { "No category" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Warnings, most urgent first.
            if (stock.hasExpired) item {
                Notice(
                    Icons.Outlined.Warning, c.expired, c.expiredContainer,
                    "${stock.expiredQty} ${p.unit} have expired.",
                    "Take them off the shelf and record them as waste.",
                    action = "Remove" to { confirmRemoveExpired = true },
                )
            }
            if (stock.isExpiringSoon && stock.nextExpiry != null) item {
                Notice(
                    Icons.Outlined.Schedule, c.expiring, c.expiringContainer,
                    "${stock.expiringSoonQty} ${p.unit} ${Format.expiry(stock.nextExpiry).replaceFirstChar { it.lowercase() }}.",
                    "Consider a discount or moving them to the front shelf.",
                )
            }
            if (stock.isOutOfStock || stock.isLowStock) item {
                Notice(
                    Icons.Outlined.TrendingDown, c.low, c.lowContainer,
                    if (stock.isOutOfStock) "Out of stock." else "Low stock: ${stock.sellableQty} left (minimum ${p.minStock}).",
                    "Restock soon so you don't miss sales.",
                    action = "Restock" to { showRestock = true },
                )
            }

            item { StockCard(stock, onSell = { showSale = true }, onRestock = { showRestock = true }) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoTile("Cost", Format.money(p.costPrice, currency), Modifier.weight(1f))
                    InfoTile("Price", Format.money(p.sellPrice, currency), Modifier.weight(1f))
                    val margin = if (p.sellPrice > 0) ((p.sellPrice - p.costPrice) / p.sellPrice * 100) else 0.0
                    InfoTile("Margin", "${"%.0f".format(margin)}%", Modifier.weight(1f))
                }
            }

            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        DetailLine("Supplier", p.supplier.ifBlank { "—" })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailLine("Expiry warning", "${p.alertDaysBefore} days before")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailLine("Low-stock alert", if (p.minStock > 0) "At ${p.minStock} ${p.unit} or less" else "Off")
                        if (p.notes.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DetailLine("Notes", p.notes)
                        }
                    }
                }
            }

            item { SectionHeader("Batches by expiry") }
            if (stock.batches.isEmpty()) item {
                AppCard(Modifier.fillMaxWidth()) {
                    Text(
                        "No stock. Use Restock to add a batch.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else item {
                AppCard(Modifier.fillMaxWidth()) {
                    Column {
                        stock.batches.forEachIndexed { i, batch ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            BatchLine(batch, p.unit, p.alertDaysBefore) { editingBatch = batch }
                        }
                    }
                }
            }

            if (state.history.isNotEmpty()) {
                item { SectionHeader("Recent activity") }
                item {
                    AppCard(Modifier.fillMaxWidth()) {
                        Column {
                            state.history.forEachIndexed { i, m ->
                                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                HistoryLine(m, p.unit, currency)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Notice(
    icon: ImageVector,
    color: Color,
    container: Color,
    title: String,
    message: String,
    action: Pair<String, () -> Unit>? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = color)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = color)
            Text(message, style = MaterialTheme.typography.bodySmall, color = color)
        }
        action?.let { (label, onClick) ->
            TextButton(onClick = onClick) { Text(label, color = color) }
        }
    }
}

@Composable
private fun StockCard(stock: StockItem, onSell: () -> Unit, onRestock: () -> Unit) {
    val p = stock.product
    val target = maxOf(p.minStock * 3, stock.sellableQty, 1)
    val color = when {
        stock.isOutOfStock || stock.isLowStock -> AppTheme.status.low
        else -> MaterialTheme.colorScheme.primary
    }
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Available to sell", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(Format.number(stock.sellableQty), style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.size(6.dp))
                        Text(p.unit, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
                if (stock.expiredQty > 0) {
                    Text(
                        "+${stock.expiredQty} expired",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTheme.status.expired,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { (stock.sellableQty.toFloat() / target).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {},
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onSell,
                    enabled = stock.sellableQty > 0,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Outlined.PointOfSale, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Record sale")
                }
                Button(
                    onClick = onRestock,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Outlined.AddShoppingCart, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Restock")
                }
            }
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier) {
    AppCard(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun BatchLine(batch: BatchEntity, unit: String, alertDays: Int, onClick: () -> Unit) {
    val today = LocalDate.now()
    val expiry = batch.expiryEpochDay?.let(LocalDate::ofEpochDay)
    val c = AppTheme.status
    val dot = when {
        expiry == null -> MaterialTheme.colorScheme.primary
        expiry.isBefore(today) -> c.expired
        !expiry.isAfter(today.plusDays(alertDays.toLong())) -> c.expiring
        else -> MaterialTheme.colorScheme.primary
    }
    val received = Instant.ofEpochMilli(batch.receivedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Edit batch", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        Column(Modifier.weight(1f)) {
            Text(
                if (expiry == null) "No expiry date" else Format.expiry(expiry),
                style = MaterialTheme.typography.titleSmall,
                color = if (expiry != null && expiry.isBefore(today)) c.expired else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                listOfNotNull(expiry?.let { "Exp ${Format.date(it)}" }, "Received ${Format.shortDate(received)}").joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(Format.quantity(batch.quantity, unit), style = MaterialTheme.typography.titleSmall)
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val historyTime = DateTimeFormatter.ofPattern("d MMM, h:mm a")

@Composable
private fun HistoryLine(m: MovementEntity, unit: String, currency: String) {
    val (icon, label, tint) = when (m.movementType) {
        MovementType.SALE -> Triple(Icons.Outlined.PointOfSale, "Sold ${m.quantity} $unit", MaterialTheme.colorScheme.primary)
        MovementType.RESTOCK -> Triple(Icons.Outlined.Inventory, "Added ${m.quantity} $unit", AppTheme.status.low)
        MovementType.WASTE -> Triple(Icons.Outlined.DeleteSweep, "Wasted ${m.quantity} $unit", AppTheme.status.expired)
        MovementType.ADJUST -> Triple(
            Icons.Outlined.Edit,
            "Adjusted ${if (m.quantity > 0) "+" else ""}${m.quantity} $unit",
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val time = Instant.ofEpochMilli(m.timestamp).atZone(ZoneId.systemDefault()).format(historyTime)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                listOf(time, m.note).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (m.movementType == MovementType.SALE) {
            Text(Format.money(m.quantity * m.unitPrice, currency), style = MaterialTheme.typography.titleSmall)
        }
    }
}
