package app.layerline.toolkit3d.ui.cost

import androidx.lifecycle.ViewModel
import app.layerline.toolkit3d.data.Presets
import app.layerline.toolkit3d.data.Printer
import app.layerline.toolkit3d.data.Region
import app.layerline.toolkit3d.data.Spool
import app.layerline.toolkit3d.data.Workshop
import app.layerline.toolkit3d.domain.CostBreakdown
import app.layerline.toolkit3d.domain.CostCalculator
import app.layerline.toolkit3d.domain.CostInput
import app.layerline.toolkit3d.domain.Money
import app.layerline.toolkit3d.ui.repoFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** A machine the calculator can price with: one of the user's printers or a model preset. */
data class MachineOption(
    val brand: String,
    val name: String,
    val watts: Double,
    val price: Double,
    val lifeHours: Double,
    val mine: Boolean,
)

/** Everything the user has dialled into the calculator. Null rate = use the region/workshop default. */
data class CostForm(
    val regionCode: String? = null,
    val kwh: Double? = null,
    val filamentPerKg: Double? = null,
    val labourRate: Double? = null,
    val machineIndex: Int = 0,
    val material: String = "PLA",
    val spoolId: Long? = null,
    val grams: Double = 145.0,
    val hours: Double = 6.5,
    val quantity: Int = 1,
    val margin: Double? = null,
    val supports: Boolean = true,
    val supportsPct: Double = 12.0,
    val inserts: Boolean = false,
    val insertCount: Double = 8.0,
    val insertUnit: Double? = null,
    val finishing: Boolean = false,
    val finishingCost: Double? = null,
    val labour: Boolean = true,
    val labourMinutes: Double = 20.0,
    val packaging: Boolean = false,
    val packagingCost: Double? = null,
    val failure: Boolean = true,
    val failurePct: Double? = null,
    val tax: Boolean = false,
    val taxPct: Double = 0.0,
)

/** Resolved rates for the current form, ready to display and calculate with. */
data class CostRates(
    val region: Region,
    val money: Money,
    val kwh: Double,
    val filamentPerKg: Double,
    val labourRate: Double,
    val perUsd: Double,
    val margin: Double,
    val failurePct: Double,
    val insertUnit: Double,
    val finishingCost: Double,
    val packagingCost: Double,
)

class CostViewModel : ViewModel() {

    private val _form = MutableStateFlow(CostForm())
    val form: StateFlow<CostForm> = _form.asStateFlow()

    fun edit(change: (CostForm) -> CostForm) = _form.update(change)

    fun machines(printers: List<Printer>, rates: CostRates): List<MachineOption> =
        printers.map { MachineOption(it.nickname, "${it.brand} ${it.model}".trim(), it.watts, it.price, it.lifeHours, mine = true) } +
            Presets.machines.map { MachineOption(it.brand, it.model, it.avgWatts, it.priceUsd * rates.perUsd, it.lifeHours, mine = false) }

    fun rates(form: CostForm, w: Workshop, spool: Spool?): CostRates {
        val usingWorkshop = form.regionCode == null || form.regionCode == w.regionCode
        val region = if (usingWorkshop) w.region else Presets.region(form.regionCode)
        val money = if (usingWorkshop) w.money else Money(region.symbol, region.decimals)
        val perUsd = if (usingWorkshop) w.perUsd else region.perUsd
        val baseFilament = if (usingWorkshop) w.filamentPerKg else region.filamentPerKg
        val materialFilament = baseFilament * Presets.material(form.material).priceFactor
        return CostRates(
            region = region,
            money = money,
            kwh = form.kwh ?: if (usingWorkshop) w.kwhRate else region.kwhRate,
            filamentPerKg = form.filamentPerKg ?: spool?.pricePerKg?.takeIf { it > 0 } ?: materialFilament,
            labourRate = form.labourRate ?: if (usingWorkshop) w.labourPerHour else region.labourPerHour,
            perUsd = perUsd,
            margin = form.margin ?: w.marginPercent,
            failurePct = form.failurePct ?: w.failurePercent,
            insertUnit = form.insertUnit ?: (0.06 * perUsd),
            finishingCost = form.finishingCost ?: (1.5 * perUsd),
            packagingCost = form.packagingCost ?: (0.8 * perUsd),
        )
    }

    fun calculate(form: CostForm, rates: CostRates, machine: MachineOption): CostBreakdown =
        CostCalculator.calculate(
            CostInput(
                grams = form.grams,
                hours = form.hours,
                quantity = form.quantity,
                filamentPricePerKg = rates.filamentPerKg,
                wastePercent = if (form.supports) form.supportsPct else 0.0,
                watts = machine.watts,
                kwhRate = rates.kwh,
                machinePrice = machine.price,
                machineLifeHours = machine.lifeHours,
                partsCost = if (form.inserts) form.insertCount * rates.insertUnit else 0.0,
                finishingCost = if (form.finishing) rates.finishingCost else 0.0,
                packagingCost = if (form.packaging) rates.packagingCost else 0.0,
                labourMinutes = if (form.labour) form.labourMinutes else 0.0,
                labourRatePerHour = rates.labourRate,
                failurePercent = if (form.failure) rates.failurePct else 0.0,
                markupPercent = rates.margin,
                taxPercent = if (form.tax) form.taxPct else 0.0,
            ),
        )

    companion object {
        val Factory = repoFactory { CostViewModel() }
    }
}
