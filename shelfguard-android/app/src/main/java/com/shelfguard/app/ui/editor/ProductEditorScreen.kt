package com.shelfguard.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.shelfguard.app.AppContainer
import com.shelfguard.app.data.db.ProductEntity
import com.shelfguard.app.ui.components.AppCard
import com.shelfguard.app.ui.components.ChoicePill
import com.shelfguard.app.ui.components.ConfirmDialog
import com.shelfguard.app.ui.components.DateField
import com.shelfguard.app.ui.components.asInput
import com.shelfguard.app.ui.components.supportingText
import com.shelfguard.app.ui.components.toPrice
import com.shelfguard.app.ui.components.toQty
import com.shelfguard.app.ui.navigation.AppNavigator
import com.shelfguard.app.util.containerViewModel
import com.shelfguard.app.util.startBarcodeScan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

val DEFAULT_CATEGORIES = listOf(
    "Dairy", "Bakery", "Beverages", "Snacks", "Pantry", "Frozen", "Fruits & Vegetables",
    "Meat & Poultry", "Personal care", "Household", "Medicine", "Baby care",
)
val UNITS = listOf("pcs", "pack", "box", "bottle", "can", "bag", "tray", "dozen", "kg", "g", "L", "ml")

data class EditorForm(
    val name: String = "",
    val category: String = "",
    val barcode: String = "",
    val unit: String = "pcs",
    val quantity: String = "",
    val expiry: LocalDate? = null,
    val minStock: String = "",
    val alertDays: String = "7",
    val cost: String = "",
    val price: String = "",
    val supplier: String = "",
    val notes: String = "",
)

data class EditorState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val form: EditorForm = EditorForm(),
    val original: EditorForm = EditorForm(),
    val errors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val currency: String = "",
) {
    val dirty: Boolean get() = form != original
}

class ProductEditorViewModel(
    private val container: AppContainer,
    private val productId: Long,
    private val initialBarcode: String,
) : ViewModel() {
    private val repo = container.inventory
    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()
    private var existing: ProductEntity? = null

    val categories = repo.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val settings = container.settings.current()
            val product = if (productId > 0) repo.getProduct(productId) else null
            existing = product
            val form = if (product != null) {
                EditorForm(
                    name = product.name,
                    category = product.category,
                    barcode = product.barcode.orEmpty(),
                    unit = product.unit,
                    minStock = if (product.minStock > 0) product.minStock.toString() else "",
                    alertDays = product.alertDaysBefore.toString(),
                    cost = product.costPrice.asInput(),
                    price = product.sellPrice.asInput(),
                    supplier = product.supplier,
                    notes = product.notes,
                )
            } else {
                EditorForm(
                    barcode = initialBarcode,
                    minStock = settings.defaultMinStock.takeIf { it > 0 }?.toString().orEmpty(),
                    alertDays = settings.defaultAlertDays.toString(),
                )
            }
            _state.value = EditorState(
                loaded = true,
                isNew = product == null,
                form = form,
                original = form,
                currency = settings.currency,
            )
        }
    }

    fun update(transform: (EditorForm) -> EditorForm) =
        _state.update { it.copy(form = transform(it.form), errors = emptyMap()) }

    fun save(onSaved: (id: Long, wasNew: Boolean) -> Unit) {
        val s = _state.value
        if (s.saving) return
        val f = s.form
        viewModelScope.launch {
            val errors = mutableMapOf<String, String>()
            if (f.name.isBlank()) errors["name"] = "Enter a product name"
            if (f.quantity.isNotBlank() && f.quantity.toQty() == null) errors["quantity"] = "Enter a whole number"
            if (f.minStock.isNotBlank() && f.minStock.toQty() == null) errors["minStock"] = "Enter a whole number"
            val days = f.alertDays.toQty()
            if (days == null || days > 365) errors["alertDays"] = "0 to 365 days"
            if (f.cost.isNotBlank() && f.cost.toPrice() == null) errors["cost"] = "Enter a valid price"
            if (f.price.isNotBlank() && f.price.toPrice() == null) errors["price"] = "Enter a valid price"
            val barcode = f.barcode.trim()
            if (barcode.isNotEmpty()) {
                val other = repo.findByBarcode(barcode)
                if (other != null && other.id != productId) errors["barcode"] = "Already used by ${other.name}"
            }
            if (errors.isNotEmpty()) {
                _state.update { it.copy(errors = errors) }
                return@launch
            }
            _state.update { it.copy(saving = true) }
            val now = System.currentTimeMillis()
            val entity = ProductEntity(
                id = existing?.id ?: 0,
                name = f.name.trim(),
                category = f.category.trim(),
                barcode = barcode.ifEmpty { null },
                unit = f.unit.trim().ifEmpty { "pcs" },
                minStock = f.minStock.toQty() ?: 0,
                costPrice = f.cost.toPrice() ?: 0.0,
                sellPrice = f.price.toPrice() ?: 0.0,
                supplier = f.supplier.trim(),
                alertDaysBefore = days ?: 7,
                notes = f.notes.trim(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            val id = if (existing == null) {
                repo.addProduct(entity, f.quantity.toQty() ?: 0, f.expiry)
            } else {
                repo.updateProduct(entity)
                entity.id
            }
            // Mark clean so leaving doesn't ask about unsaved changes.
            _state.update { it.copy(saving = false, original = it.form) }
            onSaved(id, existing == null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditorScreen(navigator: AppNavigator, productId: Long, initialBarcode: String) {
    val vm = containerViewModel(key = "editor-$productId") { ProductEditorViewModel(it, productId, initialBarcode) }
    val state by vm.state.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmDiscard by remember { mutableStateOf(false) }
    val f = state.form
    val e = state.errors

    val leave: () -> Unit = {
        if (state.dirty) confirmDiscard = true else navigator.back()
    }
    BackHandler(enabled = state.dirty) { confirmDiscard = true }
    if (confirmDiscard) ConfirmDialog(
        title = "Discard changes?",
        message = "Your changes to this product haven't been saved.",
        confirmLabel = "Discard",
        destructive = true,
        onConfirm = { navigator.back() },
        onDismiss = { confirmDiscard = false },
    )
    LaunchedEffect(e) {
        if (e.isNotEmpty()) snackbar.showSnackbar("Please check the highlighted fields")
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Add product" else "Edit product") },
                navigationIcon = { IconButton(onClick = leave) { Icon(Icons.Outlined.Close, "Close") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Button(
                        onClick = { vm.save { id, isNew -> navigator.afterSave(id, isNew) } },
                        enabled = state.loaded && !state.saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) { Text(if (state.isNew) "Save product" else "Save changes") }
                }
            }
        },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = f.barcode,
                onValueChange = { v -> vm.update { it.copy(barcode = v.trim()) } },
                label = { Text("Barcode (optional)") },
                isError = e["barcode"] != null,
                supportingText = supportingText(e["barcode"]),
                trailingIcon = {
                    IconButton(onClick = {
                        startBarcodeScan(
                            context,
                            onResult = { code -> vm.update { it.copy(barcode = code) } },
                            onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
                        )
                    }) { Icon(Icons.Outlined.QrCodeScanner, "Scan barcode") }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = fieldColors(),
            )
            OutlinedTextField(
                value = f.name,
                onValueChange = { v -> vm.update { it.copy(name = v) } },
                label = { Text("Product name *") },
                isError = e["name"] != null,
                supportingText = supportingText(e["name"]),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = fieldColors(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SuggestField(
                    label = "Category",
                    value = f.category,
                    options = (categories + DEFAULT_CATEGORIES).distinctBy { it.lowercase() },
                    onChange = { v -> vm.update { it.copy(category = v) } },
                    modifier = Modifier.weight(1.4f),
                )
                SuggestField(
                    label = "Unit",
                    value = f.unit,
                    options = UNITS,
                    onChange = { v -> vm.update { it.copy(unit = v) } },
                    modifier = Modifier.weight(1f),
                    filter = false,
                )
            }

            if (state.isNew) {
                SectionCard("Opening stock") {
                    NumberField("Quantity in stock", f.quantity, e["quantity"]) { v -> vm.update { it.copy(quantity = v) } }
                    DateField(label = "Expiry date", date = f.expiry, onDateChange = { d -> vm.update { it.copy(expiry = d) } })
                    Text(
                        "Leave the date empty for products that don't expire. You can add more batches with different dates later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    "Stock quantities and expiry dates are managed per batch on the product page (Restock, or tap a batch to correct it).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard("Prices") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PriceField("Cost price", f.cost, state.currency, e["cost"], Modifier.weight(1f)) { v -> vm.update { it.copy(cost = v) } }
                    PriceField("Selling price", f.price, state.currency, e["price"], Modifier.weight(1f)) { v -> vm.update { it.copy(price = v) } }
                }
            }

            SectionCard("Alerts") {
                Text("Warn me before expiry", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(1, 3, 7, 14, 30).forEach { d ->
                        ChoicePill(
                            text = if (d == 1) "1 day" else "$d d",
                            selected = f.alertDays == d.toString(),
                            onClick = { vm.update { it.copy(alertDays = d.toString()) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                NumberField("Days before expiry", f.alertDays, e["alertDays"]) { v -> vm.update { it.copy(alertDays = v) } }
                NumberField(
                    "Low-stock alert at or below",
                    f.minStock,
                    e["minStock"],
                    supporting = "Leave empty to turn off low-stock alerts for this product",
                ) { v -> vm.update { it.copy(minStock = v) } }
            }

            SectionCard("More details") {
                OutlinedTextField(
                    value = f.supplier,
                    onValueChange = { v -> vm.update { it.copy(supplier = v) } },
                    label = { Text("Supplier") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = fieldColors(),
                )
                OutlinedTextField(
                    value = f.notes,
                    onValueChange = { v -> vm.update { it.copy(notes = v) } },
                    label = { Text("Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = fieldColors(),
                )
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
)

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    error: String?,
    supporting: String? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(6)) },
        label = { Text(label) },
        isError = error != null,
        supportingText = supportingText(error ?: supporting),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
    )
}

@Composable
private fun PriceField(
    label: String,
    value: String,
    currency: String,
    error: String?,
    modifier: Modifier,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' }.take(10)) },
        label = { Text(label) },
        prefix = { if (currency.isNotBlank()) Text("$currency ") },
        isError = error != null,
        supportingText = supportingText(error),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
    )
}

/** Text field with a dropdown of suggestions; the user can still type anything. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestField(
    label: String,
    value: String,
    options: List<String>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    filter: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val shown = if (filter && value.isNotBlank()) {
        options.filter { it.contains(value.trim(), ignoreCase = true) && !it.equals(value.trim(), ignoreCase = true) }
    } else options
    ExposedDropdownMenuBox(expanded = expanded && shown.isNotEmpty(), onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it); expanded = true },
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && shown.isNotEmpty()) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            shape = MaterialTheme.shapes.small,
            colors = fieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded && shown.isNotEmpty(), onDismissRequest = { expanded = false }) {
            shown.take(12).forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onChange(option); expanded = false },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
