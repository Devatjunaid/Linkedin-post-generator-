package com.brickbeam.buildmate.ui.calc

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import com.brickbeam.buildmate.ads.NativeAdSlot
import com.brickbeam.buildmate.calc.Category
import com.brickbeam.buildmate.calc.Conv
import com.brickbeam.buildmate.calc.LengthFormat
import com.brickbeam.buildmate.calc.Market
import com.brickbeam.buildmate.calc.PriceKey
import com.brickbeam.buildmate.calc.RebarCalculator
import com.brickbeam.buildmate.calc.RebarInput
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
import com.brickbeam.buildmate.ui.graphics.SlabPreview
import com.brickbeam.buildmate.ui.theme.BmType
import com.brickbeam.buildmate.ui.theme.LocalPalette

@Composable
fun SteelScreen() {
    val ctx = rememberCalcContext()
    val p = LocalPalette.current
    val sys = ctx.system
    val fmt = ctx.fmt
    val imperial = sys == UnitSystem.IMPERIAL
    val spec = ctx.spec

    var length by rememberSaveable(sys) { mutableStateOf(if (imperial) "20'" else "6") }
    var width by rememberSaveable(sys) { mutableStateOf(if (imperial) "12'" else "4") }
    var cover by rememberSaveable(sys) { mutableStateOf(if (imperial) "2" else "50") }
    var spacingL by rememberSaveable(sys) { mutableStateOf(if (imperial) "18" else "150") }
    var spacingW by rememberSaveable(sys) { mutableStateOf(if (imperial) "18" else "150") }
    var barId by rememberSaveable(ctx.market) { mutableStateOf(spec.defaultRebarId) }
    val bar = spec.rebarSizes.firstOrNull { it.id == barId } ?: spec.rebarSizes[0]
    var stockIdx by rememberSaveable(ctx.market) { mutableStateOf(0) }
    val stock = spec.stockLengthsM.getOrElse(stockIdx) { spec.stockLengthsM[0] }
    var lap by rememberSaveable { mutableStateOf("40") }
    var layers by rememberSaveable { mutableStateOf(1) }
    var chair by rememberSaveable(sys) { mutableStateOf(if (imperial) "3'" else "0.9") }

    val lenM = parseLen(length, sys)
    val widM = parseLen(width, sys)
    val coverM = LengthParserCompat.smallOrZero(cover, sys)
    val sL = parseSmall(spacingL, sys)?.div(1000)
    val sW = parseSmall(spacingW, sys)?.div(1000)
    val lapD = parseNum(lap)?.takeIf { it in 0.0..200.0 }
    val chairM = parseLen(chair, sys)
    val lapM = (lapD ?: 0.0) * bar.diameterMm / 1000
    val lapOk = lapM < stock
    val valid = lenM != null && widM != null && coverM != null && sL != null && sW != null && lapD != null && chairM != null && lapOk
    val result = if (valid) RebarCalculator.calculate(
        RebarInput(lenM!!, widM!!, coverM!!, sL!!, sW!!, bar, stock, lapD!!, layers, chairM!!),
    ) else null

    val weightUnitKg = if (ctx.market == Market.US) Conv.KG_PER_LB else 1.0
    val boughtQty = (result?.boughtWeightKg ?: 0.0) / weightUnitKg
    val wireQty = (result?.tieWireKg ?: 0.0) / weightUnitKg
    val cost = result?.let {
        boughtQty * ctx.price(PriceKey.REBAR) + wireQty * ctx.price(PriceKey.TIE_WIRE) + it.chairs * ctx.price(PriceKey.REBAR_CHAIR)
    } ?: 0.0
    val stockLabel = { m: Double -> if (ctx.market == Market.US) "${Math.round(m / Conv.M_PER_FT)} ft" else "${LengthFormat.trim(m, 1)} m" }

    CalcPage(
        title = "Steel / rebar",
        subtitle = "Slab mesh · ${bar.label} · ${ctx.region.name}",
        canAdd = result != null && result.plan.stockBars > 0,
        drafts = drafts@{
            val r = result ?: return@drafts emptyList()
            val note = "${r.plan.stockBars} × ${stockLabel(stock)} ${bar.label} bars"
            listOf(
                DraftItem(Category.STEEL, "Rebar ${bar.label}", round2(boughtQty), ctx.unitOf(PriceKey.REBAR), ctx.price(PriceKey.REBAR), note),
                DraftItem(Category.STEEL, "Tie wire", round2(wireQty).coerceAtLeast(0.01), ctx.unitOf(PriceKey.TIE_WIRE), ctx.price(PriceKey.TIE_WIRE), "${r.tiePoints} ties"),
                DraftItem(Category.STEEL, "Rebar chairs / spacers", r.chairs.toDouble(), ctx.unitOf(PriceKey.REBAR_CHAIR), ctx.price(PriceKey.REBAR_CHAIR), ""),
            )
        },
    ) {
        Reveal(0) {
            BmCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                SlabPreview(
                    lengthM = lenM ?: 1.0, widthM = widM ?: 1.0, thicknessM = 0.15,
                    rebarSpacingM = sL ?: 0.2, showRebar = true, ink = p.ink,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InputField("Slab length", length, { length = it }, Modifier.weight(1f), isError = lenM == null, hint = lengthHint(sys))
            InputField("Slab width", width, { width = it }, Modifier.weight(1f), isError = widM == null)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InputField("Spacing ↔ bars", spacingL, { spacingL = it }, Modifier.weight(1f), suffix = fmt.smallUnit, isError = sL == null)
            InputField("Spacing ↕ bars", spacingW, { spacingW = it }, Modifier.weight(1f), suffix = fmt.smallUnit, isError = sW == null)
            InputField("Edge cover", cover, { cover = it }, Modifier.weight(1f), suffix = fmt.smallUnit, isError = coverM == null)
        }
        SectionTitle("Bar & stock")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PickerField("Bar size", bar, spec.rebarSizes, { "${it.label} · ${if (imperial) fmt.num(it.kgPerM / Conv.KG_M_PER_LB_FT, 3) + " lb/ft" else fmt.num(it.kgPerM, 3) + " kg/m"}" }, { barId = it.id }, Modifier.weight(1.3f))
            PickerField("Stock length", stock, spec.stockLengthsM, stockLabel, { stockIdx = spec.stockLengthsM.indexOf(it) }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InputField("Lap splice", lap, { lap = it }, Modifier.weight(1f), suffix = "× Ø", isError = lapD == null || !lapOk, hint = if (!lapOk) "Lap longer than stock bar" else "= ${fmt.small(lapM * 1000)}")
            InputField("Chair spacing", chair, { chair = it }, Modifier.weight(1f), isError = chairM == null)
        }
        Segmented(listOf(1, 2), layers, { if (it == 1) "Single mat" else "Top + bottom mats" }, { layers = it }, Modifier.fillMaxWidth())

        if (result != null) {
            Reveal(1) {
                ResultHero(
                    caption = "Buy",
                    figure = fmt.int(result.plan.stockBars),
                    unit = "bars × ${stockLabel(stock)}",
                    detail = "${result.barsAlongLength} bars ↔ (${fmt.length(result.runLengthLongM)}) + ${result.barsAlongWidth} bars ↕ (${fmt.length(result.runLengthShortM)})",
                ) {
                    val on = p.onAccent
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stat("Steel to buy", fmt.weight(result.boughtWeightKg), Modifier.weight(1f), on)
                        Stat("Net in slab", fmt.weight(result.netWeightKg), Modifier.weight(1f), on)
                        Stat("Offcut", "${fmt.num(result.plan.offcutPct, 1)}%", Modifier.weight(1f), on)
                    }
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stat("Tie points", fmt.int(result.tiePoints), Modifier.weight(1f), on)
                        Stat("Chairs", fmt.int(result.chairs), Modifier.weight(1f), on)
                        Stat("Est. cost", fmt.money(cost), Modifier.weight(1f), on)
                    }
                }
            }
            if (result.splicesPerLongBar > 0 || result.splicesPerShortBar > 0) {
                InfoNote("Runs longer than the stock bar are spliced with ${fmt.small(result.lapM * 1000)} laps (${result.splicesPerLongBar} per ↔ bar, ${result.splicesPerShortBar} per ↕ bar). Laps are included in the steel to buy.")
            }
            Text(
                "Cut plan: first-fit decreasing over ${result.plan.stockBars} stock bars, ${fmt.length(result.plan.offcutM)} total offcut. Tie wire ≈ ${fmt.weight(result.tieWireKg)}.",
                style = BmType.tiny, color = p.mute,
            )
            InfoNote("Bar size, spacing and cover must follow your engineer's drawings or local code. This tool counts and costs the steel; it does not design it.")
        } else {
            InfoNote(if (!lapOk) "The lap length is longer than the stock bar; choose a longer stock length or a smaller lap." else "Check the highlighted fields.")
        }
        NativeAdSlot("calc_steel")
    }
}

/** Cover can legitimately be 0. */
private object LengthParserCompat {
    fun smallOrZero(text: String, sys: UnitSystem): Double? =
        if (text.trim() == "0") 0.0 else parseSmall(text, sys)?.div(1000)
}
