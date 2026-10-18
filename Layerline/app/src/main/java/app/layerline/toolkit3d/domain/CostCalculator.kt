package app.layerline.toolkit3d.domain

/**
 * Everything needed to price one print job (a whole plate). All money values are in the
 * same local currency; [quantity] only divides the final result into a per-piece price.
 */
data class CostInput(
    val grams: Double,
    val hours: Double,
    val quantity: Int = 1,
    val filamentPricePerKg: Double,
    val wastePercent: Double = 0.0,
    val watts: Double,
    val kwhRate: Double,
    val machinePrice: Double,
    val machineLifeHours: Double,
    val maintenancePerHour: Double = 0.0,
    val partsCost: Double = 0.0,
    val finishingCost: Double = 0.0,
    val packagingCost: Double = 0.0,
    val labourMinutes: Double = 0.0,
    val labourRatePerHour: Double = 0.0,
    val failurePercent: Double = 0.0,
    val markupPercent: Double = 0.0,
    val taxPercent: Double = 0.0,
)

data class CostBreakdown(
    val usedGrams: Double,
    val kwh: Double,
    val filament: Double,
    val electricity: Double,
    val machineWear: Double,
    val partsAndFinishing: Double,
    val labour: Double,
    val failureBuffer: Double,
    val productionCost: Double,
    val profit: Double,
    val tax: Double,
    val sellPrice: Double,
    val pricePerPiece: Double,
    val pricePerGram: Double,
) {
    /** Cost lines in display order, used for the breakdown bar and the receipt. */
    val lines: List<Pair<CostLine, Double>>
        get() = listOf(
            CostLine.Filament to filament,
            CostLine.Electricity to electricity,
            CostLine.Wear to machineWear,
            CostLine.Parts to partsAndFinishing,
            CostLine.Labour to labour,
            CostLine.Failure to failureBuffer,
        )
}

enum class CostLine(val label: String) {
    Filament("Filament"),
    Electricity("Electricity"),
    Wear("Machine wear"),
    Parts("Parts + finishing"),
    Labour("Labour"),
    Failure("Failure buffer"),
}

object CostCalculator {

    fun calculate(input: CostInput): CostBreakdown {
        val grams = input.grams.coerceAtLeast(0.0)
        val hours = input.hours.coerceAtLeast(0.0)
        val usedGrams = grams * (1 + input.wastePercent.coerceAtLeast(0.0) / 100.0)

        val filament = usedGrams / 1000.0 * input.filamentPricePerKg
        val kwh = input.watts.coerceAtLeast(0.0) / 1000.0 * hours
        val electricity = kwh * input.kwhRate
        val wearPerHour = if (input.machineLifeHours > 0) input.machinePrice / input.machineLifeHours else 0.0
        val machineWear = (wearPerHour + input.maintenancePerHour) * hours
        val parts = input.partsCost + input.finishingCost + input.packagingCost
        val labour = input.labourMinutes / 60.0 * input.labourRatePerHour

        val subtotal = filament + electricity + machineWear + parts + labour
        val failure = subtotal * input.failurePercent.coerceAtLeast(0.0) / 100.0
        val production = subtotal + failure
        val profit = production * input.markupPercent / 100.0
        val beforeTax = production + profit
        val tax = beforeTax * input.taxPercent.coerceAtLeast(0.0) / 100.0
        val sell = beforeTax + tax
        val qty = input.quantity.coerceAtLeast(1)

        return CostBreakdown(
            usedGrams = usedGrams,
            kwh = kwh,
            filament = filament,
            electricity = electricity,
            machineWear = machineWear,
            partsAndFinishing = parts,
            labour = labour,
            failureBuffer = failure,
            productionCost = production,
            profit = profit,
            tax = tax,
            sellPrice = sell,
            pricePerPiece = sell / qty,
            pricePerGram = if (usedGrams > 0) sell / usedGrams else 0.0,
        )
    }

    /** Running cost of a printer per hour of printing: depreciation plus electricity. */
    fun machineCostPerHour(price: Double, lifeHours: Double, watts: Double, kwhRate: Double): Double {
        val wear = if (lifeHours > 0) price / lifeHours else 0.0
        return wear + watts / 1000.0 * kwhRate
    }
}
