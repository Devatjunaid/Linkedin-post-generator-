package com.brickbeam.buildmate.ui.projects

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brickbeam.buildmate.ads.NativeAdSlot
import com.brickbeam.buildmate.calc.Category
import com.brickbeam.buildmate.calc.EstimateLine
import com.brickbeam.buildmate.calc.EstimateMath
import com.brickbeam.buildmate.calc.EstimateTotals
import com.brickbeam.buildmate.calc.Region
import com.brickbeam.buildmate.calc.Regions
import com.brickbeam.buildmate.data.AppSettings
import com.brickbeam.buildmate.data.db.LineItemEntity
import com.brickbeam.buildmate.data.db.ProjectEntity
import com.brickbeam.buildmate.export.EstimateExporter
import com.brickbeam.buildmate.ui.AppEnv
import com.brickbeam.buildmate.ui.LocalAppEnv
import com.brickbeam.buildmate.ui.calc.parseNum
import com.brickbeam.buildmate.ui.components.AccentButton
import com.brickbeam.buildmate.ui.components.AnimatedNumberText
import com.brickbeam.buildmate.ui.components.BmCard
import com.brickbeam.buildmate.ui.components.Chip
import com.brickbeam.buildmate.ui.components.Fmt
import com.brickbeam.buildmate.ui.components.IconSquareButton
import com.brickbeam.buildmate.ui.components.InputField
import com.brickbeam.buildmate.ui.components.OutlineButton
import com.brickbeam.buildmate.ui.components.ScreenHeader
import com.brickbeam.buildmate.ui.components.ToggleRow
import com.brickbeam.buildmate.ui.theme.BmIcons
import com.brickbeam.buildmate.ui.theme.BmType
import com.brickbeam.buildmate.ui.theme.LocalPalette
import com.brickbeam.buildmate.ui.theme.MaterialColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun totalsFor(project: ProjectEntity, items: List<LineItemEntity>, region: Region): EstimateTotals =
    EstimateMath.totals(
        lines = items.map {
            EstimateLine(runCatching { Category.valueOf(it.category) }.getOrDefault(Category.OTHER), it.quantity, it.unitPrice, it.owned)
        },
        taxRatePct = region.taxRatePct + if (region.taxIncludedInPrices) 0.0 else project.localTaxPct,
        taxIncluded = region.taxIncludedInPrices,
        contingencyPct = project.contingencyPct,
        delivery = project.deliveryFee,
    )

@Composable
fun EstimateScreen(projectId: Long) {
    val env = LocalAppEnv.current
    val p = LocalPalette.current
    val context = LocalContext.current
    val repo = env.vm.projectRepo
    val projectFlow = remember(projectId) { repo.project(projectId) }
    val itemsFlow = remember(projectId) { repo.items(projectId) }
    val project by projectFlow.collectAsStateWithLifecycle(null)
    val items by itemsFlow.collectAsStateWithLifecycle(emptyList())
    val settings by env.vm.settings.collectAsStateWithLifecycle()
    val pr = project ?: return
    val region = Regions.byCode(pr.regionCode) ?: (settings ?: AppSettings()).region
    val fmt = Fmt((settings ?: AppSettings()).unitSystem, region)
    val totals = totalsFor(pr, items, region)

    var editing by remember { mutableStateOf<LineItemEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var editingSettings by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(pr.name, "${region.name} · ${items.size} items", onBack = { env.back() }) {
                IconSquareButton(BmIcons.Edit, "Project settings", { editingSettings = true })
            }
        }
        item { TotalsCard(pr, totals, region, fmt) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlineButton("Share", { context.startActivity(EstimateExporter.shareTextIntent(EstimateExporter.text(pr, items, totals, fmt), pr.name)) }, Modifier.weight(1f), BmIcons.Share)
                OutlineButton("CSV", { export(env, context, pr, items, totals, region, fmt, pdf = false) }, Modifier.weight(1f), BmIcons.Table)
                AccentButton("PDF", { export(env, context, pr, items, totals, region, fmt, pdf = true) }, Modifier.weight(1.2f), BmIcons.Document)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Shopping list", style = BmType.section, color = p.ink, modifier = Modifier.weight(1f))
                Text("Tap ✓ if you already have it", style = BmType.tiny, color = p.mute)
            }
        }
        if (items.isEmpty()) {
            item {
                BmCard(Modifier.fillMaxWidth()) {
                    Text("No items yet. Open a calculator and tap “Add to estimate”, or add an item by hand.", style = BmType.label, color = p.mute)
                }
            }
        }
        items(items, key = { it.id }) { item ->
            ItemRow(item, fmt,
                onToggle = { env.scope.launch { repo.updateItem(item.copy(owned = !item.owned)) } },
                onEdit = { editing = item },
            )
        }
        item {
            OutlineButton("Add item by hand", { adding = true }, Modifier.fillMaxWidth(), BmIcons.Add)
        }
        item { NativeAdSlot("estimate") }
        item {
            Text(
                if (region.taxIncludedInPrices) "${region.taxLabel} is shown as included because European shelf prices include it."
                else "${region.taxLabel} ${fmt.num(region.taxRatePct, 3)}% (state) + ${fmt.num(pr.localTaxPct, 3)}% local is added on top. Set your city/county rate in project settings.",
                style = BmType.tiny, color = p.mute,
            )
        }
    }

    editing?.let { item ->
        ItemDialog(
            initial = item, fmt = fmt,
            onDismiss = { editing = null },
            onSave = { env.scope.launch { repo.updateItem(it) }; editing = null },
            onDelete = { env.scope.launch { repo.deleteItem(item) }; editing = null },
        )
    }
    if (adding) {
        ItemDialog(
            initial = LineItemEntity(projectId = pr.id, category = Category.OTHER.name, name = "", quantity = 1.0, unit = "each", unitPrice = 0.0),
            fmt = fmt,
            onDismiss = { adding = false },
            onSave = { newItem ->
                env.scope.launch {
                    repo.addItems(pr.id, listOf(com.brickbeam.buildmate.data.DraftItem(
                        runCatching { Category.valueOf(newItem.category) }.getOrDefault(Category.OTHER),
                        newItem.name.ifBlank { "Item" }, newItem.quantity, newItem.unit, newItem.unitPrice, newItem.note,
                    )))
                }
                adding = false
            },
            onDelete = null,
        )
    }
    if (editingSettings) {
        ProjectSettingsDialog(pr, region, onDismiss = { editingSettings = false }) { updated ->
            env.scope.launch { repo.updateProject(updated) }
            editingSettings = false
        }
    }
}

private fun export(
    env: AppEnv, context: Context, project: ProjectEntity, items: List<LineItemEntity>,
    totals: EstimateTotals, region: Region, fmt: Fmt, pdf: Boolean,
) {
    env.scope.launch {
        val intent = runCatching {
            withContext(Dispatchers.IO) {
                if (pdf) EstimateExporter.shareIntent(context, EstimateExporter.pdf(context, project, items, totals, region, fmt), "application/pdf", "${project.name} estimate")
                else EstimateExporter.shareIntent(context, EstimateExporter.csv(context, project, items, totals, fmt), "text/csv", "${project.name} estimate")
            }
        }.getOrNull()
        if (intent == null) {
            env.toast("Couldn't create the file. Please try again.")
            return@launch
        }
        // Major action: frequency-capped interstitial first, then the share sheet.
        env.ads.showInterstitialIfEligible(env.activity()) {
            runCatching { context.startActivity(intent) }.onFailure { env.toast("No app available to share the file.") }
        }
    }
}

@Composable
private fun TotalsCard(pr: ProjectEntity, totals: EstimateTotals, region: Region, fmt: Fmt) {
    val p = LocalPalette.current
    BmCard(Modifier.fillMaxWidth(), radius = 24.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(if (totals.taxIncluded) "Total (incl. ${region.taxLabel})" else "Total with ${fmt.num(totals.taxRatePct, 3)}% tax", style = BmType.label, color = p.mute)
                AnimatedNumberText(fmt.moneyPrecise(totals.total), BmType.bigFigure.copy(fontSize = BmType.bigFigure.fontSize * 0.85f), p.ink)
                val parts = buildList {
                    add(fmt.moneyPrecise(totals.materials))
                    if (!totals.taxIncluded) add("${fmt.moneyPrecise(totals.tax)} tax")
                    if (totals.contingency > 0) add("${fmt.moneyPrecise(totals.contingency)} contingency")
                    if (totals.delivery > 0) add("${fmt.moneyPrecise(totals.delivery)} delivery")
                }
                Text(parts.joinToString(" + "), style = BmType.tiny, color = p.mute)
                if (totals.taxIncluded) Text("incl. ${fmt.moneyPrecise(totals.tax)} ${region.taxLabel}", style = BmType.tiny, color = p.mute)
            }
            if (pr.budget > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Budget", style = BmType.tiny, color = p.mute)
                    Text(fmt.money(pr.budget), style = BmType.figure, color = p.ink)
                    val left = pr.budget - totals.total
                    Text(
                        if (left >= 0) "${fmt.money(left)} left" else "${fmt.money(-left)} over",
                        style = BmType.tiny.copy(fontWeight = FontWeight.Bold), color = if (left >= 0) p.success else p.danger,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        val sum = totals.byCategory.values.sum()
        Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(p.surface2), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (sum > 0) Category.entries.forEach { c ->
                val v = totals.byCategory[c] ?: 0.0
                if (v > 0) {
                    val grow by animateFloatAsState((v / sum).toFloat(), tween(700), label = "seg")
                    Box(Modifier.weight(grow.coerceAtLeast(0.001f)).fillMaxSize().background(MaterialColors.of(c)))
                }
            }
        }
        FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Category.entries.forEach { c ->
                val v = totals.byCategory[c] ?: return@forEach
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(3.dp)).background(MaterialColors.of(c)))
                    Text("${c.label} ${if (sum > 0) (v / sum * 100).toInt() else 0}%", style = BmType.tiny, color = p.mute)
                }
            }
        }
        if (totals.ownedValue > 0) Text("Already on site: ${fmt.moneyPrecise(totals.ownedValue)} (not counted)", style = BmType.tiny, color = p.mute, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ItemRow(item: LineItemEntity, fmt: Fmt, onToggle: () -> Unit, onEdit: () -> Unit) {
    val p = LocalPalette.current
    val cat = runCatching { Category.valueOf(item.category) }.getOrDefault(Category.OTHER)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(p.surface)
            .border(1.dp, p.line, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = "Edit ${item.name}", onClick = onEdit)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (item.owned) p.accent else p.surface)
                .border(1.5.dp, if (item.owned) p.accent else p.mute, RoundedCornerShape(9.dp))
                .clickable(role = Role.Checkbox, onClickLabel = "Already have ${item.name}", onClick = onToggle)
                .semantics { selected = item.owned },
            contentAlignment = Alignment.Center,
        ) { if (item.owned) Icon(BmIcons.Check, null, tint = p.onAccent, modifier = Modifier.size(18.dp)) }
        Box(Modifier.width(5.dp).height(30.dp).clip(RoundedCornerShape(3.dp)).background(MaterialColors.of(cat)))
        Column(Modifier.weight(1f)) {
            Text(
                item.name, style = BmType.label.copy(fontWeight = FontWeight.Bold, textDecoration = if (item.owned) TextDecoration.LineThrough else null),
                color = if (item.owned) p.mute else p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (item.owned) "Already on site" else "${fmt.num(item.quantity)} ${item.unit} × ${fmt.moneyPrecise(item.unitPrice)}",
                style = BmType.tiny, color = p.mute, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            fmt.moneyPrecise(item.quantity * item.unitPrice),
            style = BmType.figure.copy(fontSize = BmType.label.fontSize, textDecoration = if (item.owned) TextDecoration.LineThrough else null),
            color = if (item.owned) p.mute else p.ink,
        )
    }
}

@Composable
private fun ItemDialog(
    initial: LineItemEntity,
    fmt: Fmt,
    onDismiss: () -> Unit,
    onSave: (LineItemEntity) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val p = LocalPalette.current
    var name by remember { mutableStateOf(initial.name) }
    var qty by remember { mutableStateOf(trim(initial.quantity)) }
    var unit by remember { mutableStateOf(initial.unit) }
    var price by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", initial.unitPrice)) }
    var owned by remember { mutableStateOf(initial.owned) }
    var category by remember { mutableStateOf(runCatching { Category.valueOf(initial.category) }.getOrDefault(Category.OTHER)) }
    val qtyV = parseNum(qty)?.takeIf { it >= 0 }
    val priceV = parseNum(price)?.takeIf { it >= 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surface,
        title = { Text(if (onDelete == null) "Add item" else "Edit item", color = p.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InputField("Name", name, { name = it }, Modifier.fillMaxWidth(), keyboardType = KeyboardType.Text)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputField("Quantity", qty, { qty = it }, Modifier.weight(1f), isError = qtyV == null)
                    InputField("Unit", unit, { unit = it }, Modifier.weight(1f), keyboardType = KeyboardType.Text)
                }
                InputField("Unit price (${fmt.region.currency})", price, { price = it }, Modifier.fillMaxWidth(), isError = priceV == null)
                if (onDelete == null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Category.entries.forEach { c -> Chip(c.label, c == category, { category = c }) }
                    }
                }
                ToggleRow("Already have it", owned, { owned = it }, sub = "Kept on the list but not counted in the total")
                if (qtyV != null && priceV != null) Text("Line total ${fmt.moneyPrecise(qtyV * priceV)}", style = BmType.label, color = p.mute)
            }
        },
        confirmButton = {
            TextButton(
                enabled = qtyV != null && priceV != null,
                onClick = {
                    onSave(initial.copy(name = name.trim().ifEmpty { initial.name.ifEmpty { "Item" } }, quantity = qtyV ?: 0.0, unit = unit.trim(), unitPrice = priceV ?: 0.0, owned = owned, category = category.name))
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = p.danger) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun ProjectSettingsDialog(pr: ProjectEntity, region: Region, onDismiss: () -> Unit, onSave: (ProjectEntity) -> Unit) {
    val p = LocalPalette.current
    var name by remember { mutableStateOf(pr.name) }
    var localTax by remember { mutableStateOf(trim(pr.localTaxPct)) }
    var contingency by remember { mutableStateOf(trim(pr.contingencyPct)) }
    var delivery by remember { mutableStateOf(trim(pr.deliveryFee)) }
    var budget by remember { mutableStateOf(trim(pr.budget)) }
    val lt = parseNum(localTax.ifBlank { "0" })?.takeIf { it in 0.0..20.0 }
    val ct = parseNum(contingency.ifBlank { "0" })?.takeIf { it in 0.0..100.0 }
    val dl = parseNum(delivery.ifBlank { "0" })?.takeIf { it >= 0 }
    val bg = parseNum(budget.ifBlank { "0" })?.takeIf { it >= 0 }
    val ok = lt != null && ct != null && dl != null && bg != null
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surface,
        title = { Text("Project settings", color = p.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InputField("Name", name, { name = it }, Modifier.fillMaxWidth(), keyboardType = KeyboardType.Text)
                if (!region.taxIncludedInPrices) {
                    InputField("Local sales tax (city/county)", localTax, { localTax = it }, Modifier.fillMaxWidth(), suffix = "%", isError = lt == null,
                        hint = "Added to ${region.name}'s ${region.taxRatePct}% state rate")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputField("Contingency", contingency, { contingency = it }, Modifier.weight(1f), suffix = "%", isError = ct == null)
                    InputField("Delivery fee", delivery, { delivery = it }, Modifier.weight(1f), suffix = region.currency, isError = dl == null)
                }
                InputField("Budget (0 = none)", budget, { budget = it }, Modifier.fillMaxWidth(), suffix = region.currency, isError = bg == null)
            }
        },
        confirmButton = {
            TextButton(enabled = ok, onClick = {
                onSave(pr.copy(name = name.trim().ifEmpty { pr.name }, localTaxPct = lt ?: 0.0, contingencyPct = ct ?: 0.0, deliveryFee = dl ?: 0.0, budget = bg ?: 0.0))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun trim(v: Double): String =
    if (v == 0.0) "0" else String.format(java.util.Locale.US, "%.3f", v).trimEnd('0').trimEnd('.')
