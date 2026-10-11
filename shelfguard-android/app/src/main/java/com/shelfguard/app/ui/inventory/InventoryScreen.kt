package com.shelfguard.app.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shelfguard.app.AppContainer
import com.shelfguard.app.data.db.ProductWithBatches
import com.shelfguard.app.domain.StockItem
import com.shelfguard.app.ui.components.ConfirmDialog
import com.shelfguard.app.ui.components.EmptyState
import com.shelfguard.app.ui.components.ItemStatusBadge
import com.shelfguard.app.ui.components.StockRow
import com.shelfguard.app.ui.components.expirySubtitle
import com.shelfguard.app.ui.navigation.AppNavigator
import com.shelfguard.app.util.Format
import com.shelfguard.app.util.containerViewModel
import com.shelfguard.app.util.startBarcodeScan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StockFilter(val key: String, val label: String) {
    ALL("all", "All"),
    LOW("low", "Low stock"),
    EXPIRING("expiring", "Expiring"),
    EXPIRED("expired", "Expired"),
    OUT("out", "Out of stock");

    fun matches(item: StockItem): Boolean = when (this) {
        ALL -> true
        LOW -> item.isLowStock || item.isOutOfStock
        EXPIRING -> item.isExpiringSoon
        EXPIRED -> item.hasExpired
        OUT -> item.isOutOfStock
    }

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: ALL
    }
}

enum class StockSort(val label: String) {
    NAME("Name (A–Z)"),
    EXPIRY("Expiry (soonest first)"),
    QTY_LOW("Quantity (lowest first)"),
    RECENT("Recently added"),
}

data class InventoryState(
    val loading: Boolean = true,
    val all: List<StockItem> = emptyList(),
    val visible: List<StockItem> = emptyList(),
    val counts: Map<StockFilter, Int> = emptyMap(),
    val query: String = "",
    val filter: StockFilter = StockFilter.ALL,
    val sort: StockSort = StockSort.NAME,
)

class InventoryViewModel(private val container: AppContainer, initialFilter: String) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(StockFilter.of(initialFilter))
    private val sort = MutableStateFlow(StockSort.NAME)

    val state = combine(container.inventory.observeStock(), query, filter, sort) { items, q, f, s ->
        val needle = q.trim().lowercase()
        val searched = if (needle.isEmpty()) items else items.filter {
            it.product.name.lowercase().contains(needle) ||
                it.product.category.lowercase().contains(needle) ||
                it.product.barcode.orEmpty().contains(needle) ||
                it.product.supplier.lowercase().contains(needle)
        }
        val filtered = searched.filter(f::matches)
        val sorted = when (s) {
            StockSort.NAME -> filtered.sortedBy { it.product.name.lowercase() }
            StockSort.EXPIRY -> filtered.sortedWith(
                compareBy<StockItem> { if (it.hasExpired) 0 else 1 }.thenBy { it.nextExpiry ?: java.time.LocalDate.MAX },
            )
            StockSort.QTY_LOW -> filtered.sortedBy { it.sellableQty }
            StockSort.RECENT -> filtered.sortedByDescending { it.product.createdAt }
        }
        InventoryState(
            loading = false,
            all = items,
            visible = sorted,
            counts = StockFilter.entries.associateWith { sf -> searched.count(sf::matches) },
            query = q,
            filter = f,
            sort = s,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryState(filter = StockFilter.of(initialFilter)))

    fun setQuery(value: String) { query.value = value }
    fun setFilter(value: StockFilter) { filter.value = value }
    fun setSort(value: StockSort) { sort.value = value }

    suspend fun delete(id: Long): ProductWithBatches? = container.inventory.deleteProduct(id)
    fun restore(snapshot: ProductWithBatches) = viewModelScope.launch { container.inventory.restoreProduct(snapshot) }
}

@Composable
fun InventoryScreen(navigator: AppNavigator, initialFilter: String) {
    val vm = containerViewModel(key = "inventory") { InventoryViewModel(it, initialFilter) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<StockItem?>(null) }
    var sortMenu by remember { mutableStateOf(false) }

    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "Delete ${item.product.name}?",
            message = "This removes the product and all its stock batches. Sales history stays in your reports.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                scope.launch {
                    val snapshot = vm.delete(item.id) ?: return@launch
                    val result = snackbar.showSnackbar(
                        "${item.product.name} deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) vm.restore(snapshot)
                }
            },
            onDismiss = { pendingDelete = null },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navigator.newProduct() },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Add product") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Inventory", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "${Format.number(state.all.size)} products",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            StockSort.entries.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.label) },
                                    leadingIcon = { RadioButton(selected = s == state.sort, onClick = null) },
                                    onClick = { vm.setSort(s); sortMenu = false },
                                )
                            }
                        }
                    }
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search name, category or barcode") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Outlined.Close, "Clear search") }
                    } else {
                        IconButton(onClick = {
                            startBarcodeScan(context, onResult = vm::setQuery)
                        }) { Icon(Icons.Outlined.QrCodeScanner, "Search by barcode") }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(StockFilter.entries) { f ->
                    val count = state.counts[f] ?: 0
                    FilterChip(
                        selected = state.filter == f,
                        onClick = { vm.setFilter(f) },
                        label = { Text(if (f == StockFilter.ALL) f.label else "${f.label} · $count") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            }

            when {
                state.loading -> Unit
                state.all.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = "No products yet",
                    message = "Add products with their quantity and expiry date to start tracking.",
                    action = { Button(onClick = { navigator.newProduct() }) { Text("Add product") } },
                )
                state.visible.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Nothing matches",
                    message = "Try another search or filter.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.visible, key = { it.id }) { item ->
                        InventoryRow(
                            item = item,
                            onOpen = { navigator.product(item.id) },
                            onEdit = { navigator.editProduct(item.id) },
                            onDelete = { pendingDelete = item },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryRow(item: StockItem, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    StockRow(
        item = item,
        subtitle = expirySubtitle(item),
        onClick = onOpen,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(Format.quantity(item.sellableQty, item.product.unit), style = MaterialTheme.typography.titleSmall)
                    ItemStatusBadge(item)
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options for ${item.product.name}")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("View details") },
                            leadingIcon = { Icon(Icons.Outlined.Visibility, null) },
                            onClick = { menu = false; onOpen() },
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = { menu = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
        },
    )
}
