package app.layerline.toolkit3d.ui.cost

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.layerline.toolkit3d.data.Presets
import app.layerline.toolkit3d.data.Quote
import app.layerline.toolkit3d.data.Spool
import app.layerline.toolkit3d.domain.CostBreakdown
import app.layerline.toolkit3d.domain.Fmt
import app.layerline.toolkit3d.domain.Money
import app.layerline.toolkit3d.ui.ColorDot
import app.layerline.toolkit3d.ui.Routes
import app.layerline.toolkit3d.ui.ScreenContext
import app.layerline.toolkit3d.ui.ScreenScroll
import app.layerline.toolkit3d.ui.WorkshopViewModel
import app.layerline.toolkit3d.ui.components.ButtonStyle
import app.layerline.toolkit3d.ui.components.Chamfer
import app.layerline.toolkit3d.ui.components.ChamferS
import app.layerline.toolkit3d.ui.components.IconSquare
import app.layerline.toolkit3d.ui.components.LButton
import app.layerline.toolkit3d.ui.components.LChip
import app.layerline.toolkit3d.ui.components.LDialog
import app.layerline.toolkit3d.ui.components.LIcon
import app.layerline.toolkit3d.ui.components.LIcons
import app.layerline.toolkit3d.ui.components.LTextField
import app.layerline.toolkit3d.ui.components.NumberEntryDialog
import app.layerline.toolkit3d.ui.components.NumberField
import app.layerline.toolkit3d.ui.components.PickerDialog
import app.layerline.toolkit3d.ui.components.ScreenHeader
import app.layerline.toolkit3d.ui.components.SectionLabel
import app.layerline.toolkit3d.ui.components.StepperTile
import app.layerline.toolkit3d.ui.components.ToggleChip
import app.layerline.toolkit3d.ui.components.perforatedTop
import app.layerline.toolkit3d.ui.components.pressable
import app.layerline.toolkit3d.ui.theme.LL
import app.layerline.toolkit3d.ui.theme.LLType

private class NumberEdit(val title: String, val unit: String, val initial: Double, val apply: (Double) -> Unit)

@Composable
fun CostScreen(ctx: ScreenContext) {
    val shared: WorkshopViewModel = viewModel(factory = WorkshopViewModel.Factory)
    val vm: CostViewModel = viewModel(factory = CostViewModel.Factory)
    val workshop = shared.workshop.collectAsStateWithLifecycle().value ?: return
    val printers by shared.printers.collectAsStateWithLifecycle()
    val spools by shared.spools.collectAsStateWithLifecycle()
    val form by vm.form.collectAsStateWithLifecycle()

    val spool = spools.firstOrNull { it.id == form.spoolId }
    val rates = vm.rates(form, workshop, spool)
    val machines = vm.machines(printers, rates)
    val machineIndex = form.machineIndex.coerceIn(0, machines.lastIndex)
    val machine = machines[machineIndex]
    val result = vm.calculate(form, rates, machine)
    val money = rates.money

    var numberEdit by remember { mutableStateOf<NumberEdit?>(null) }
    var pickMachine by remember { mutableStateOf(false) }
    var pickSpool by remember { mutableStateOf(false) }
    var tuneExtras by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ScreenScroll {
        ScreenHeader("; M73 — QUOTE A PRINT", "Print cost") {
            IconSquare(LIcons.Receipt, "Saved quotes", { ctx.open(Routes.QUOTES) })
        }

        SectionLabel("; REGION · RATES AUTO-FILL")
        val activeRegion = form.regionCode ?: workshop.regionCode
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(Presets.regions, key = { it.code }) { r ->
                LChip(
                    r.code, r.code == activeRegion,
                    {
                        vm.edit {
                            it.copy(
                                regionCode = r.code, kwh = null, filamentPerKg = null, labourRate = null,
                                insertUnit = null, finishingCost = null, packagingCost = null,
                            )
                        }
                    },
                    Modifier.width(56.dp),
                    sub = r.currency,
                    selectedColor = LL.Lime,
                    minHeight = 48.dp,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            RateChip("⚡ ${rates.money.symbol}${Fmt.trim(rates.kwh, 3)}/kWh") {
                numberEdit = NumberEdit("Electricity per kWh", money.symbol.trim(), rates.kwh) { v -> vm.edit { it.copy(kwh = v) } }
            }
            RateChip("◎ ${money.format(rates.filamentPerKg)}/kg") {
                numberEdit = NumberEdit("Filament per kg", money.symbol.trim(), rates.filamentPerKg) { v -> vm.edit { it.copy(filamentPerKg = v) } }
            }
            RateChip("✋ ${money.format(rates.labourRate)}/h") {
                numberEdit = NumberEdit("Labour per hour", money.symbol.trim(), rates.labourRate) { v -> vm.edit { it.copy(labourRate = v) } }
            }
        }

        SectionLabel("; MACHINE PROFILE")
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicMachineHeight)
                .background(LL.Panel, Chamfer),
        ) {
            CarouselButton(LIcons.Back, "Previous machine") {
                vm.edit { it.copy(machineIndex = (machineIndex - 1 + machines.size) % machines.size) }
            }
            AnimatedContent(
                targetState = machineIndex,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(260)) { it / 3 * dir } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(tween(200)) { -it / 3 * dir } + fadeOut(tween(200)))
                },
                modifier = Modifier.weight(1f).fillMaxHeight().pressable({ pickMachine = true }),
                label = "machine",
            ) { idx ->
                val m = machines[idx.coerceIn(0, machines.lastIndex)]
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        (if (m.mine) "MY PRINTER · " else "") + m.brand.uppercase() + " · ${idx + 1}/${machines.size}",
                        style = LLType.mono(10.sp, color = if (m.mine) LL.Lime else LL.Muted), maxLines = 1,
                    )
                    Text(if (m.mine) m.name.ifBlank { m.brand } else m.name, style = LLType.display(21.sp, FontWeight.SemiBold), maxLines = 1)
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("${Fmt.trim(m.watts, 0)} W avg", style = LLType.mono(11.sp, color = LL.Soft))
                        Text(money.format(m.price), style = LLType.mono(11.sp, color = LL.Soft))
                        Text("${Fmt.trim(m.lifeHours, 0)} h life", style = LLType.mono(11.sp, color = LL.Soft))
                    }
                }
            }
            CarouselButton(LIcons.Next, "Next machine") {
                vm.edit { it.copy(machineIndex = (machineIndex + 1) % machines.size) }
            }
        }

        SectionLabel("; MATERIAL")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(Presets.materials, key = { it.name }) { m ->
                LChip(m.name, spool == null && form.material == m.name, { vm.edit { it.copy(material = m.name, spoolId = null, filamentPerKg = null) } })
            }
        }
        Row(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .pressable({ pickSpool = true })
                .border(1.dp, if (spool != null) LL.Lime else LL.LineStrong, ChamferS)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (spool != null) {
                ColorDot(spool.color)
                Spacer(Modifier.width(8.dp))
                Text("Priced from ${spool.name} (${spool.material})", style = LLType.body(13.sp), modifier = Modifier.weight(1f))
            } else {
                LIcon(LIcons.Spool, LL.Muted, size = 16.dp)
                Spacer(Modifier.width(8.dp))
                Text("Price from a spool in stock", style = LLType.body(13.sp, color = LL.Soft), modifier = Modifier.weight(1f))
            }
            Text("PICK", style = LLType.mono(11.sp, FontWeight.SemiBold, LL.Lime))
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepperTile(
                "WEIGHT", Fmt.trim(form.grams, 1), "g",
                { vm.edit { it.copy(grams = (it.grams - 5).coerceAtLeast(1.0)) } },
                { vm.edit { it.copy(grams = it.grams + 5) } },
                { numberEdit = NumberEdit("Model weight", "g", form.grams) { v -> vm.edit { it.copy(grams = v.coerceAtLeast(0.1)) } } },
                Modifier.weight(1f),
            )
            StepperTile(
                "TIME", Fmt.trim(form.hours, 2), "h",
                { vm.edit { it.copy(hours = (it.hours - 0.25).coerceAtLeast(0.25)) } },
                { vm.edit { it.copy(hours = it.hours + 0.25) } },
                { numberEdit = NumberEdit("Print time", "hours", form.hours) { v -> vm.edit { it.copy(hours = v.coerceAtLeast(0.05)) } } },
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepperTile(
                "PIECES", form.quantity.toString(), "pcs",
                { vm.edit { it.copy(quantity = (it.quantity - 1).coerceAtLeast(1)) } },
                { vm.edit { it.copy(quantity = it.quantity + 1) } },
                { numberEdit = NumberEdit("Pieces on this plate", "pcs", form.quantity.toDouble()) { v -> vm.edit { it.copy(quantity = v.toInt().coerceAtLeast(1)) } } },
                Modifier.weight(1f),
            )
            StepperTile(
                "MARGIN", Fmt.trim(rates.margin, 1), "%",
                { vm.edit { it.copy(margin = (rates.margin - 5).coerceAtLeast(0.0)) } },
                { vm.edit { it.copy(margin = rates.margin + 5) } },
                { numberEdit = NumberEdit("Profit margin", "%", rates.margin) { v -> vm.edit { it.copy(margin = v) } } },
                Modifier.weight(1f),
            )
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("; WHAT THIS PRINT NEEDS", Modifier.weight(1f))
            Text(
                "TUNE",
                style = LLType.mono(11.sp, FontWeight.SemiBold, LL.Lime),
                modifier = Modifier.padding(top = 12.dp).pressable({ tuneExtras = true }).padding(8.dp),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ToggleChip("Supports +${Fmt.trim(form.supportsPct)}%", form.supports, { vm.edit { it.copy(supports = !it.supports) } })
            ToggleChip("Heat inserts ×${Fmt.trim(form.insertCount, 0)}", form.inserts, { vm.edit { it.copy(inserts = !it.inserts) } })
            ToggleChip("Paint / finish", form.finishing, { vm.edit { it.copy(finishing = !it.finishing) } })
            ToggleChip("Labour ${Fmt.trim(form.labourMinutes, 0)} min", form.labour, { vm.edit { it.copy(labour = !it.labour) } })
            ToggleChip("Packaging", form.packaging, { vm.edit { it.copy(packaging = !it.packaging) } })
            ToggleChip("Failure buffer ${Fmt.trim(rates.failurePct)}%", form.failure, { vm.edit { it.copy(failure = !it.failure) } })
            ToggleChip("Tax ${Fmt.trim(form.taxPct)}%", form.tax, { vm.edit { it.copy(tax = !it.tax) } })
        }

        Spacer(Modifier.height(22.dp))
        QuoteTicket(result, money, rates.region.code, machine.name.ifBlank { machine.brand }, spool?.material ?: form.material, rates.margin, form.quantity)

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LButton("Save quote", { saving = true }, Modifier.weight(1f), icon = LIcons.Check)
            LButton("Share", {
                shareQuote(context, buildSummary(result, money, machine.name.ifBlank { machine.brand }, spool?.material ?: form.material, form.grams, form.hours, form.quantity, rates.margin))
            }, Modifier.weight(1f), icon = LIcons.Share, style = ButtonStyle.Dark)
        }
    }

    numberEdit?.let { e ->
        NumberEntryDialog(e.title, e.initial, e.unit, onDismiss = { numberEdit = null }, onConfirm = { e.apply(it); numberEdit = null })
    }
    if (pickMachine) {
        PickerDialog(
            code = "; M115 — MACHINE",
            title = "Price with…",
            items = machines.indices.toList(),
            label = { i -> machines[i].let { if (it.mine) "${it.brand} (mine)" else "${it.brand} ${it.name}" } },
            sub = { i -> machines[i].let { "${Fmt.trim(it.watts, 0)} W · ${money.format(it.price)} · ${Fmt.trim(it.lifeHours, 0)} h" } },
            isSelected = { it == machineIndex },
            onDismiss = { pickMachine = false },
            onPick = { i -> vm.edit { it.copy(machineIndex = i) }; pickMachine = false },
        )
    }
    if (pickSpool) {
        PickerDialog(
            code = "; M600 — PRICE FROM STOCK",
            title = "Use a spool's price",
            items = listOf<Spool?>(null) + spools,
            label = { it?.name ?: "No spool (use material price)" },
            sub = { s -> s?.let { "${it.material} · ${money.format(it.pricePerKg)}/kg" } ?: "" },
            isSelected = { it?.id == form.spoolId },
            onDismiss = { pickSpool = false },
            onPick = { s -> vm.edit { it.copy(spoolId = s?.id, material = s?.material ?: it.material, filamentPerKg = null) }; pickSpool = false },
        )
    }
    if (tuneExtras) {
        ExtrasDialog(form, rates, onDismiss = { tuneExtras = false }) { updated ->
            vm.edit { updated }
            tuneExtras = false
        }
    }
    if (saving) {
        SaveQuoteDialog(
            suggested = "${machine.name.ifBlank { machine.brand }} · ${Fmt.trim(form.grams, 0)} g",
            onDismiss = { saving = false },
        ) { title ->
            shared.saveQuote(
                Quote(
                    title = title,
                    regionCode = rates.region.code,
                    symbol = money.symbol,
                    decimals = money.decimals,
                    machine = machine.name.ifBlank { machine.brand },
                    material = spool?.material ?: form.material,
                    grams = form.grams,
                    hours = form.hours,
                    quantity = form.quantity,
                    cost = result.productionCost,
                    price = result.sellPrice,
                    summary = buildSummary(result, money, machine.name.ifBlank { machine.brand }, spool?.material ?: form.material, form.grams, form.hours, form.quantity, rates.margin),
                ),
            )
            saving = false
            ctx.majorAction()
        }
    }
}

private val IntrinsicMachineHeight = 92.dp

@Composable
private fun RateChip(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = LLType.mono(11.sp, color = LL.Soft),
        modifier = Modifier
            .pressable(onClick)
            .border(1.dp, LL.Line, ChamferS)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        maxLines = 1,
    )
}

@Composable
private fun CarouselButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .width(44.dp)
            .fillMaxHeight()
            .pressable(onClick)
            .background(LL.Panel2)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { LIcon(icon, LL.Ink, size = 18.dp) }
}

@Composable
private fun QuoteTicket(r: CostBreakdown, money: Money, region: String, machine: String, material: String, margin: Double, quantity: Int) {
    val total = r.productionCost.takeIf { it > 0 } ?: 1.0
    val price by animateFloatAsState(r.sellPrice.toFloat(), tween(450), label = "price")
    Column(
        Modifier
            .fillMaxWidth()
            .background(LL.Paper)
            .perforatedTop(LL.Graphite)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("QUOTE · $region", style = LLType.mono(11.sp, color = LL.PaperMuted), modifier = Modifier.weight(1f))
            Text("$machine · $material", style = LLType.mono(11.sp, color = LL.PaperMuted), maxLines = 1)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp).height(12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            r.lines.forEachIndexed { i, (_, v) ->
                val f by animateFloatAsState((v / total).toFloat().coerceAtLeast(0f), tween(500), label = "seg$i")
                if (f > 0.001f) Box(Modifier.weight(f).fillMaxHeight().background(LL.Breakdown[i]))
            }
        }
        r.lines.forEachIndexed { i, (line, v) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).background(LL.Breakdown[i]).border(1.dp, LL.Graphite))
                Spacer(Modifier.width(8.dp))
                Text(line.label, style = LLType.mono(13.sp, color = LL.Graphite), modifier = Modifier.weight(1f))
                Text(money.format(v), style = LLType.mono(13.sp, FontWeight.Medium, LL.Graphite))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(LL.PaperLine))
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("PRODUCTION COST", style = LLType.mono(13.sp, FontWeight.SemiBold, LL.Graphite), modifier = Modifier.weight(1f))
            Text(money.format(r.productionCost), style = LLType.mono(13.sp, FontWeight.SemiBold, LL.Graphite))
        }
        if (r.tax > 0) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("TAX", style = LLType.mono(12.sp, color = LL.PaperMuted), modifier = Modifier.weight(1f))
                Text(money.format(r.tax), style = LLType.mono(12.sp, color = LL.PaperMuted))
            }
        }
        Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(2.dp).background(LL.Graphite))
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("SELL PRICE · ${Fmt.trim(margin, 1)}% MARGIN", style = LLType.mono(10.sp, color = LL.PaperMuted))
                Text(money.format(price.toDouble()), style = LLType.display(38.sp, color = LL.Graphite), maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (quantity > 1) Text("${money.format(r.pricePerPiece)} / pc", style = LLType.mono(11.sp, FontWeight.SemiBold, LL.Graphite))
                Text("${money.symbol}${Fmt.num(r.pricePerGram, 2)} / g", style = LLType.mono(10.sp, color = LL.PaperMuted))
                Text("${Fmt.num(r.kwh, 2)} kWh", style = LLType.mono(10.sp, color = LL.PaperMuted))
            }
        }
    }
}

@Composable
private fun ExtrasDialog(form: CostForm, rates: CostRates, onDismiss: () -> Unit, onSave: (CostForm) -> Unit) {
    var supports by remember { mutableStateOf(Fmt.trim(form.supportsPct)) }
    var inserts by remember { mutableStateOf(Fmt.trim(form.insertCount, 0)) }
    var insertUnit by remember { mutableStateOf(Fmt.trim(rates.insertUnit, 3)) }
    var finishing by remember { mutableStateOf(Fmt.trim(rates.finishingCost, 2)) }
    var labour by remember { mutableStateOf(Fmt.trim(form.labourMinutes, 0)) }
    var packaging by remember { mutableStateOf(Fmt.trim(rates.packagingCost, 2)) }
    var failure by remember { mutableStateOf(Fmt.trim(rates.failurePct)) }
    var tax by remember { mutableStateOf(Fmt.trim(form.taxPct)) }
    val sym = rates.money.symbol.trim()
    LDialog(
        code = "; M500 — TUNE EXTRAS",
        title = "What the print needs",
        onDismiss = onDismiss,
        actions = {
            LButton("Cancel", onDismiss, Modifier.weight(1f), style = ButtonStyle.Ghost)
            LButton("Apply", {
                onSave(
                    form.copy(
                        supportsPct = supports.toDoubleOrNull() ?: form.supportsPct,
                        insertCount = inserts.toDoubleOrNull() ?: form.insertCount,
                        insertUnit = insertUnit.toDoubleOrNull(),
                        finishingCost = finishing.toDoubleOrNull(),
                        labourMinutes = labour.toDoubleOrNull() ?: form.labourMinutes,
                        packagingCost = packaging.toDoubleOrNull(),
                        failurePct = failure.toDoubleOrNull(),
                        taxPct = tax.toDoubleOrNull() ?: form.taxPct,
                    ),
                )
            }, Modifier.weight(1f))
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Supports / purge", supports, { supports = it }, Modifier.weight(1f), suffix = "%")
            NumberField("Failure buffer", failure, { failure = it }, Modifier.weight(1f), suffix = "%")
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Inserts / magnets", inserts, { inserts = it }, Modifier.weight(1f), suffix = "pcs", decimal = false)
            NumberField("Price each", insertUnit, { insertUnit = it }, Modifier.weight(1f), suffix = sym)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Paint / finish", finishing, { finishing = it }, Modifier.weight(1f), suffix = sym)
            NumberField("Packaging", packaging, { packaging = it }, Modifier.weight(1f), suffix = sym)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Labour time", labour, { labour = it }, Modifier.weight(1f), suffix = "min")
            NumberField("Sales tax / VAT", tax, { tax = it }, Modifier.weight(1f), suffix = "%")
        }
    }
}

@Composable
private fun SaveQuoteDialog(suggested: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var title by remember { mutableStateOf(suggested) }
    LDialog(
        code = "; M500 — SAVE QUOTE",
        title = "Save this quote",
        onDismiss = onDismiss,
        actions = {
            LButton("Cancel", onDismiss, Modifier.weight(1f), style = ButtonStyle.Ghost)
            LButton("Save", { onSave(title.trim().ifEmpty { suggested }) }, Modifier.weight(1f))
        },
    ) {
        LTextField("Quote name", title, { title = it }, placeholder = "Customer or part name")
    }
}

internal fun buildSummary(
    r: CostBreakdown,
    money: Money,
    machine: String,
    material: String,
    grams: Double,
    hours: Double,
    quantity: Int,
    margin: Double,
): String = buildString {
    appendLine("3D print quote")
    appendLine("$machine · $material · ${Fmt.trim(grams, 1)} g · ${Fmt.trim(hours, 2)} h" + if (quantity > 1) " · $quantity pcs" else "")
    appendLine()
    r.lines.forEach { (line, v) -> if (v > 0) appendLine("${line.label}: ${money.format(v)}") }
    appendLine("Production cost: ${money.format(r.productionCost)}")
    if (r.tax > 0) appendLine("Tax: ${money.format(r.tax)}")
    appendLine("Price (${Fmt.trim(margin, 1)}% margin): ${money.format(r.sellPrice)}")
    if (quantity > 1) appendLine("Per piece: ${money.format(r.pricePerPiece)}")
    append("— made with Layerline")
}

internal fun shareQuote(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share quote"))
}
