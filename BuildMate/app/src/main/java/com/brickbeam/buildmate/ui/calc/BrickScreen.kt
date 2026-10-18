package com.brickbeam.buildmate.ui.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.brickbeam.buildmate.ads.NativeAdSlot
import com.brickbeam.buildmate.calc.BrickCalculator
import com.brickbeam.buildmate.calc.BrickInput
import com.brickbeam.buildmate.calc.Category
import com.brickbeam.buildmate.calc.Conv
import com.brickbeam.buildmate.calc.LengthFormat
import com.brickbeam.buildmate.calc.Market
import com.brickbeam.buildmate.calc.MasonryUnits
import com.brickbeam.buildmate.calc.Opening
import com.brickbeam.buildmate.calc.PriceKey
import com.brickbeam.buildmate.calc.UnitSystem
import com.brickbeam.buildmate.data.DraftItem
import com.brickbeam.buildmate.ui.components.BmCard
import com.brickbeam.buildmate.ui.components.Chip
import com.brickbeam.buildmate.ui.components.InfoNote
import com.brickbeam.buildmate.ui.components.InputField
import com.brickbeam.buildmate.ui.components.PickerField
import com.brickbeam.buildmate.ui.components.Reveal
import com.brickbeam.buildmate.ui.components.ResultHero
import com.brickbeam.buildmate.ui.components.SectionTitle
import com.brickbeam.buildmate.ui.components.Segmented
import com.brickbeam.buildmate.ui.components.Stat
import com.brickbeam.buildmate.ui.components.ToggleRow
import com.brickbeam.buildmate.ui.graphics.WallOpeningSpec
import com.brickbeam.buildmate.ui.graphics.WallPreview
import com.brickbeam.buildmate.ui.theme.BmIcons
import com.brickbeam.buildmate.ui.theme.BmType
import com.brickbeam.buildmate.ui.theme.LocalPalette

data class OpeningRow(val label: String, val w: String, val h: String, val n: String)

private val openingSaver = listSaver<SnapshotStateList<OpeningRow>, String>(
    save = { list -> list.flatMap { listOf(it.label, it.w, it.h, it.n) } },
    restore = { flat -> flat.chunked(4).map { OpeningRow(it[0], it[1], it[2], it[3]) }.toMutableStateList() },
)

private fun doorRow(s: UnitSystem) = if (s == UnitSystem.IMPERIAL) OpeningRow("Door", "3'", "7'", "1") else OpeningRow("Door", "0.9", "2.1", "1")
private fun windowRow(s: UnitSystem) = if (s == UnitSystem.IMPERIAL) OpeningRow("Window", "4'", "3'", "1") else OpeningRow("Window", "1.2", "1.2", "1")

@Composable
fun BrickScreen() {
    val ctx = rememberCalcContext()
    val p = LocalPalette.current
    val sys = ctx.system
    val fmt = ctx.fmt
    val imperial = sys == UnitSystem.IMPERIAL
    val units = MasonryUnits.forMarket(ctx.market)

    var length by rememberSaveable(sys) { mutableStateOf(if (imperial) "20'" else "6") }
    var height by rememberSaveable(sys) { mutableStateOf(if (imperial) "8'" else "2.5") }
    val openings = rememberSaveable(sys, saver = openingSaver) { mutableListOf(doorRow(sys), windowRow(sys)).toMutableStateList() }
    var unitId by rememberSaveable(ctx.region.code) { mutableStateOf(ctx.region.defaultBrickId) }
    val unit = MasonryUnits.byId(unitId)?.takeIf { it in units } ?: units.first()
    var joint by rememberSaveable(sys, unit.id) {
        mutableStateOf(if (imperial) LengthFormat.inchFraction(unit.defaultJointMm / 25.4) else LengthFormat.trim(unit.defaultJointMm, 1))
    }
    var leaves by rememberSaveable { mutableStateOf(1) }
    var tiedVeneer by rememberSaveable { mutableStateOf(ctx.market == Market.US) }
    var waste by rememberSaveable { mutableStateOf("5") }
    var sandParts by rememberSaveable { mutableStateOf(if (ctx.market == Market.US) 3 else 4) }
    var mortarWaste by rememberSaveable { mutableStateOf("25") }
    var pallet by rememberSaveable(ctx.market) { mutableStateOf(ctx.spec.defaultPalletSize.toString()) }

    val lenM = parseLen(length, sys)
    val hgtM = parseLen(height, sys)
    val jointMm = parseSmall(joint, sys)
    val parsedOpenings = openings.mapNotNull { o ->
        val w = parseLen(o.w, sys)
        val h = parseLen(o.h, sys)
        val n = o.n.toIntOrNull()
        if (w != null && h != null && n != null && n > 0) Opening(w, h, n) else null
    }
    val wasteV = parseNum(waste)?.coerceIn(0.0, 100.0)
    val mortarWasteV = parseNum(mortarWaste)?.coerceIn(0.0, 200.0)
    val valid = lenM != null && hgtM != null && jointMm != null && wasteV != null && mortarWasteV != null
    val result = if (valid) BrickCalculator.calculate(
        BrickInput(
            wallLengthM = lenM!!, wallHeightM = hgtM!!, openings = parsedOpenings, unit = unit,
            jointMm = jointMm!!, leaves = leaves, wastePct = wasteV!!, sandParts = sandParts.toDouble(),
            mortarWastePct = mortarWasteV!!, cementBag = ctx.spec.masonryCement, tiesPerM2 = ctx.spec.wallTiesPerM2,
            palletSize = pallet.toIntOrNull() ?: 0, tiedVeneer = tiedVeneer && leaves == 1,
        ),
    ) else null

    val unitKey = if (unit.isBlock) PriceKey.BLOCK else PriceKey.BRICK
    val noun = if (unit.isBlock) "blocks" else "bricks"
    val sandQty = (result?.sandKg ?: 0.0) / ctx.spec.bulkTonKg
    val cost = result?.let {
        it.unitsToBuy * ctx.price(unitKey) + it.cementBags * ctx.price(PriceKey.MASONRY_CEMENT_BAG) +
            sandQty * ctx.price(PriceKey.SAND) + it.wallTies * ctx.price(PriceKey.WALL_TIE)
    } ?: 0.0
    val note = if (valid) "Wall ${fmt.length(lenM!!)} × ${fmt.length(hgtM!!)}" else ""

    CalcPage(
        title = if (unit.isBlock) "Block wall" else "Brick wall",
        subtitle = "${ctx.region.name} · ${unit.name}",
        canAdd = result != null && result.unitsToBuy > 0,
        drafts = drafts@{
            val r = result ?: return@drafts emptyList()
            listOf(
                DraftItem(Category.BRICK, unit.name, r.unitsToBuy.toDouble(), ctx.unitOf(unitKey), ctx.price(unitKey), note),
                DraftItem(Category.CEMENT, ctx.spec.masonryCement.label, r.cementBags.toDouble(), ctx.unitOf(PriceKey.MASONRY_CEMENT_BAG), ctx.price(PriceKey.MASONRY_CEMENT_BAG), "Mortar 1:$sandParts"),
                DraftItem(Category.CEMENT, "Mason sand", Math.round(sandQty * 100) / 100.0, ctx.unitOf(PriceKey.SAND), ctx.price(PriceKey.SAND), "Mortar 1:$sandParts"),
                DraftItem(Category.BRICK, "Wall ties", r.wallTies.toDouble(), ctx.unitOf(PriceKey.WALL_TIE), ctx.price(PriceKey.WALL_TIE), note),
            )
        },
    ) {
        Reveal(0) {
            BmCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth()) {
                    Text(if (leaves == 1) "Running bond · single wythe" else "Running bond · double wythe", style = BmType.tiny, color = p.mute, modifier = Modifier.weight(1f))
                    Text(lenM?.let { fmt.length(it) } ?: "—", style = BmType.tiny, color = p.mute)
                }
                WallPreview(
                    lengthM = lenM ?: 0.0, heightM = hgtM ?: 0.0,
                    openings = parsedOpenings.flatMap { o -> List(o.count.coerceAtMost(12)) { WallOpeningSpec(o.widthM, o.heightM) } },
                    unitLengthMm = unit.lengthMm, unitHeightMm = unit.heightMm, jointMm = jointMm ?: 10.0,
                    background = p.surface2, surface = p.surface, accent = p.accentText,
                    modifier = Modifier.fillMaxWidth().height(170.dp),
                )
                if (result != null) {
                    Text(
                        "Gross ${fmt.area(result.grossAreaM2)} − openings ${fmt.area(result.openingsAreaM2)} = net ${fmt.area(result.netAreaM2)}",
                        style = BmType.tiny, color = p.mute,
                    )
                }
            }
        }

        Reveal(1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputField("Wall length", length, { length = it }, Modifier.weight(1f), hint = lengthHint(sys), isError = lenM == null)
                InputField("Wall height", height, { height = it }, Modifier.weight(1f), isError = hgtM == null)
            }
        }

        SectionTitle("Doors & windows (deducted)")
        openings.forEachIndexed { i, o ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InputField("${o.label} W", o.w, { openings[i] = o.copy(w = it) }, Modifier.weight(1f), isError = parseLen(o.w, sys) == null)
                InputField("H", o.h, { openings[i] = o.copy(h = it) }, Modifier.weight(1f), isError = parseLen(o.h, sys) == null)
                InputField("Qty", o.n, { openings[i] = o.copy(n = it.filter(Char::isDigit).take(3)) }, Modifier.width(64.dp), keyboardType = KeyboardType.Number)
                IconButton(onClick = { openings.removeAt(i) }) { Icon(BmIcons.Delete, "Remove ${o.label}", tint = p.mute) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("+ Door", false, { openings.add(doorRow(sys)) })
            Chip("+ Window", false, { openings.add(windowRow(sys)) })
            Chip("+ Other opening", false, { openings.add(OpeningRow("Opening", if (imperial) "2'" else "0.6", if (imperial) "2'" else "0.6", "1")) })
        }

        SectionTitle("Units & mortar")
        PickerField("Brick / block", unit, units, { it.name }, { unitId = it.id }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InputField("Mortar joint", joint, { joint = it }, Modifier.weight(1f), suffix = fmt.smallUnit, isError = jointMm == null)
            InputField("Wastage", waste, { waste = it }, Modifier.weight(1f), suffix = "%", isError = wasteV == null)
        }
        Segmented(listOf(1, 2), leaves, { if (it == 1) "Single wythe / half-brick" else "Double / one-brick" }, { leaves = it }, Modifier.fillMaxWidth())
        if (leaves == 1) ToggleRow("Veneer tied to a backing wall", tiedVeneer, { tiedVeneer = it }, sub = "Adds wall ties (e.g. brick veneer on a timber frame)")
        Text("Mortar mix (cement : sand)", style = BmType.label, color = p.mute)
        Segmented(listOf(3, 4, 5, 6), sandParts, { "1:$it" }, { sandParts = it }, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InputField("Mortar waste", mortarWaste, { mortarWaste = it }, Modifier.weight(1f), suffix = "%", isError = mortarWasteV == null)
            InputField("Pallet size", pallet, { pallet = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f), keyboardType = KeyboardType.Number, suffix = noun)
        }

        if (result != null) {
            Reveal(2) {
                ResultHero(
                    caption = "You need",
                    figure = fmt.int(result.unitsToBuy),
                    unit = noun,
                    detail = "${fmt.num(if (imperial) result.unitsPerM2 * Conv.M2_PER_FT2 else result.unitsPerM2, 2)} per ${fmt.areaUnit} × ${if (leaves == 2) "2 wythes" else "1 wythe"} + ${wasteV?.toInt()}% wastage" +
                        if (result.fullPallets > 0 || result.looseUnits > 0) " · ${result.fullPallets} pallet${if (result.fullPallets == 1) "" else "s"} + ${result.looseUnits} loose" else "",
                ) {
                    val on = p.onAccent
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stat(ctx.spec.masonryCement.label, "${result.cementBags} bags", Modifier.weight(1f), on)
                        Stat("Sand", fmt.bulk(result.sandKg), Modifier.weight(1f), on)
                        Stat("Wall ties", if (result.wallTies > 0) fmt.int(result.wallTies) else "—", Modifier.weight(1f), on)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stat("Wet mortar", fmt.volumeFt3(result.mortarWetM3), Modifier.weight(1f), on)
                        Stat("Est. cost", fmt.money(cost), Modifier.weight(1f), on)
                    }
                }
            }
            InfoNote(
                "Mortar = wall volume − brick volume, +${mortarWasteV?.toInt()}% waste, ×1.27 for dry materials. " +
                    "Costs use your ${ctx.region.name} prices (edit them in Prices); ${if (ctx.region.taxIncludedInPrices) "VAT included" else "tax added in the estimate"}.",
            )
        } else {
            InfoNote("Check the highlighted fields: lengths accept ${lengthHint(sys)}.")
        }
        NativeAdSlot("calc_brick")
    }
}

