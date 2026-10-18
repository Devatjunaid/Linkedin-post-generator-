package com.brickbeam.buildmate.ui.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.brickbeam.buildmate.ads.NativeAdSlot
import com.brickbeam.buildmate.calc.Category
import com.brickbeam.buildmate.calc.Conv
import com.brickbeam.buildmate.calc.Market
import com.brickbeam.buildmate.calc.PriceKey
import com.brickbeam.buildmate.calc.TileCalculator
import com.brickbeam.buildmate.calc.TileInput
import com.brickbeam.buildmate.calc.TilePattern
import com.brickbeam.buildmate.calc.TrowelNotch
import com.brickbeam.buildmate.calc.UnitSystem
import com.brickbeam.buildmate.data.DraftItem
import com.brickbeam.buildmate.ui.components.BmCard
import com.brickbeam.buildmate.ui.components.InfoNote
import com.brickbeam.buildmate.ui.components.InputField
import com.brickbeam.buildmate.ui.components.PickerField
import com.brickbeam.buildmate.ui.components.Reveal
import com.brickbeam.buildmate.ui.components.ResultHero
import com.brickbeam.buildmate.ui.components.SectionTitle
import com.brickbeam.buildmate.ui.components.Segmented
import com.brickbeam.buildmate.ui.components.Stat
import com.brickbeam.buildmate.ui.graphics.TilePlanPreview
import com.brickbeam.buildmate.ui.theme.BmType
import com.brickbeam.buildmate.ui.theme.LocalPalette

@Composable
fun TileScreen() {
    val ctx = rememberCalcContext()
    val p = LocalPalette.current
    val sys = ctx.system
    val fmt = ctx.fmt
    val imperial = sys == UnitSystem.IMPERIAL
    val spec = ctx.spec

    var length by rememberSaveable(sys) { mutableStateOf(if (imperial) "14'" else "4.2") }
    var width by rememberSaveable(sys) { mutableStateOf(if (imperial) "12'" else "3.6") }
    var deduct by rememberSaveable(sys) { mutableStateOf("0") }
    var tileL by rememberSaveable(sys) { mutableStateOf(if (imperial) "24" else "600") }
    var tileW by rememberSaveable(sys) { mutableStateOf(if (imperial) "12" else "300") }
    var thick by rememberSaveable(sys) { mutableStateOf(if (imperial) "3/8" else "9") }
    var joint by rememberSaveable(sys) { mutableStateOf(if (imperial) "1/8" else "3") }
    var pattern by rememberSaveable { mutableStateOf(TilePattern.STRAIGHT) }
    var spare by rememberSaveable { mutableStateOf("3") }
    var notch by rememberSaveable { mutableStateOf(TrowelNotch.N10) }

    val lenM = parseLen(length, sys)
    val widM = parseLen(width, sys)
    val deductUser = if (deduct.isBlank()) 0.0 else parseNum(deduct)
    val tl = parseSmall(tileL, sys)
    val tw = parseSmall(tileW, sys)
    val th = parseSmall(thick, sys)
    val j = if (joint.trim() == "0") 0.0 else parseSmall(joint, sys)
    val spareV = parseNum(spare)?.coerceIn(0.0, 100.0)
    val suggestedPerBox = if (tl != null && tw != null) TileCalculator.suggestedTilesPerBox(tl, tw) else 8
    var perBox by rememberSaveable(tileL, tileW) { mutableStateOf(suggestedPerBox.toString()) }
    val perBoxV = perBox.toIntOrNull()?.takeIf { it in 1..999 }

    val valid = lenM != null && widM != null && deductUser != null && tl != null && tw != null && th != null && j != null && spareV != null && perBoxV != null
    val result = if (valid) TileCalculator.calculate(
        TileInput(
            areaLengthM = lenM!!, areaWidthM = widM!!, deductM2 = fmt.areaFromUser(deductUser!!),
            tileLengthMm = tl!!, tileWidthMm = tw!!, tileThicknessMm = th!!, jointMm = j!!,
            pattern = pattern, sparePct = spareV!!, tilesPerBox = perBoxV!!, notch = notch,
            adhesiveBagKg = spec.adhesiveBagKg, groutBagKg = spec.groutBagKg,
        ),
    ) else null

    val areaQty = (result?.boughtAreaM2 ?: 0.0).let { if (ctx.market == Market.US) Conv.m2ToFt2(it) else it }
    val cost = result?.let {
        areaQty * ctx.price(PriceKey.TILE) + it.adhesiveBags * ctx.price(PriceKey.ADHESIVE_BAG) + it.groutBags * ctx.price(PriceKey.GROUT_BAG)
    } ?: 0.0
    val tileName = if (tl != null && tw != null) "${fmt.small(tl)} × ${fmt.small(tw)}" else "Tile"

    CalcPage(
        title = "Tiles",
        subtitle = "${pattern.label} · ${ctx.region.name}",
        canAdd = result != null && result.tilesToBuy > 0,
        drafts = drafts@{
            val r = result ?: return@drafts emptyList()
            val note = "${r.boxes} boxes · ${r.tilesToBuy} tiles · ${pattern.label}"
            listOf(
                DraftItem(Category.TILE, "Tile $tileName", round2(areaQty), ctx.unitOf(PriceKey.TILE), ctx.price(PriceKey.TILE), note),
                DraftItem(Category.TILE, spec.adhesiveBagLabel, r.adhesiveBags.toDouble(), ctx.unitOf(PriceKey.ADHESIVE_BAG), ctx.price(PriceKey.ADHESIVE_BAG), notch.label + " trowel"),
                DraftItem(Category.TILE, spec.groutBagLabel, r.groutBags.toDouble(), ctx.unitOf(PriceKey.GROUT_BAG), ctx.price(PriceKey.GROUT_BAG), "Joint ${fmt.small(j ?: 0.0)}"),
            )
        },
    ) {
        Reveal(0) {
            BmCard(Modifier.fillMaxWidth(), padding = PaddingValues(10.dp)) {
                TilePlanPreview(
                    lengthM = lenM ?: 0.0, widthM = widM ?: 0.0, tileLengthMm = tl ?: 0.0, tileWidthMm = tw ?: 0.0,
                    jointMm = j ?: 2.0, pattern = pattern, groutColor = Color9A,
                    modifier = Modifier.fillMaxWidth().height(170.dp),
                )
                if (result != null) Text("Area ${fmt.area(result.netAreaM2)} · ${result.method}", style = BmType.tiny, color = p.mute, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InputField("Room length", length, { length = it }, Modifier.weight(1f), isError = lenM == null, hint = lengthHint(sys))
            InputField("Room width", width, { width = it }, Modifier.weight(1f), isError = widM == null)
        }
        InputField("Deduct (cabinets, tub…)", deduct, { deduct = it }, Modifier.fillMaxWidth(), suffix = fmt.areaUnit, isError = deductUser == null)
        SectionTitle("Tile")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InputField("Length", tileL, { tileL = it }, Modifier.weight(1f), suffix = fmt.smallUnit, isError = tl == null)
            InputField("Width", tileW, { tileW = it }, Modifier.weight(1f), suffix = fmt.smallUnit, isError = tw == null)
            InputField("Thickness", thick, { thick = it }, Modifier.weight(1f), suffix = fmt.smallUnit, isError = th == null)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InputField("Grout joint", joint, { joint = it }, Modifier.weight(1f), suffix = fmt.smallUnit, isError = j == null)
            InputField("Tiles per box", perBox, { perBox = it.filter(Char::isDigit).take(3) }, Modifier.weight(1f), keyboardType = KeyboardType.Number, isError = perBoxV == null)
            InputField("Spare", spare, { spare = it }, Modifier.weight(1f), suffix = "%", isError = spareV == null)
        }
        Text("Laying pattern (adds cutting waste)", style = BmType.label, color = p.mute)
        Segmented(TilePattern.entries, pattern, { it.label }, { pattern = it }, Modifier.fillMaxWidth(), sublabel = { "+${it.cutWastePct.toInt()}%" })
        PickerField("Trowel notch", notch, TrowelNotch.entries, { "${it.label} · ${it.kgPerM2} kg/m²" }, { notch = it }, Modifier.fillMaxWidth())

        if (result != null) {
            Reveal(1) {
                ResultHero(
                    caption = "You need",
                    figure = fmt.int(result.boxes),
                    unit = "boxes",
                    detail = "${fmt.int(result.tilesToBuy)} tiles (${fmt.int(result.tilesForLayout)} for the layout + ${spareV?.toInt()}% spare) · covers ${fmt.area(result.boughtAreaM2)}",
                ) {
                    val on = p.onAccent
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stat("Adhesive", "${result.adhesiveBags} × ${spec.adhesiveBagLabel}", Modifier.weight(1f), on)
                        Stat("Grout", "${result.groutBags} bag${if (result.groutBags == 1) "" else "s"} (${fmt.weight(result.groutKg)})", Modifier.weight(1f), on)
                        Stat("Est. cost", fmt.money(cost), Modifier.weight(1f), on)
                    }
                }
            }
            if (result.backButter) InfoNote("Large-format tile (a side ≥ 600 mm / 24 in): back-butter each tile; +1 kg/m² adhesive is already included.")
            InfoNote("Buy all boxes from the same batch / shade lot and keep the spare tiles for future repairs.")
        } else {
            InfoNote("Check the highlighted fields.")
        }
        NativeAdSlot("calc_tiles")
    }
}

private val Color9A = androidx.compose.ui.graphics.Color(0xFF9A9286)
