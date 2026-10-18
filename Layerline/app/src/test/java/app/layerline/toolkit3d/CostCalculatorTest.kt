package app.layerline.toolkit3d

import app.layerline.toolkit3d.domain.CostCalculator
import app.layerline.toolkit3d.domain.CostInput
import org.junit.Assert.assertEquals
import org.junit.Test

class CostCalculatorTest {

    private val base = CostInput(
        grams = 100.0,
        hours = 5.0,
        filamentPricePerKg = 20.0,
        watts = 100.0,
        kwhRate = 0.20,
        machinePrice = 500.0,
        machineLifeHours = 5000.0,
    )

    @Test
    fun filamentElectricityAndWear() {
        val r = CostCalculator.calculate(base)
        assertEquals(2.0, r.filament, 1e-9)
        assertEquals(0.5, r.kwh, 1e-9)
        assertEquals(0.1, r.electricity, 1e-9)
        assertEquals(0.5, r.machineWear, 1e-9)
        assertEquals(2.6, r.productionCost, 1e-9)
        assertEquals(2.6, r.sellPrice, 1e-9)
    }

    @Test
    fun wasteFailureMarkupTaxAndQuantity() {
        val r = CostCalculator.calculate(
            base.copy(
                wastePercent = 10.0,
                labourMinutes = 30.0,
                labourRatePerHour = 10.0,
                partsCost = 1.0,
                failurePercent = 10.0,
                markupPercent = 50.0,
                taxPercent = 10.0,
                quantity = 4,
            ),
        )
        assertEquals(110.0, r.usedGrams, 1e-9)
        assertEquals(2.2, r.filament, 1e-9)
        // 2.2 + 0.1 + 0.5 + 1 + 5 = 8.8, +10% failure
        assertEquals(9.68, r.productionCost, 1e-9)
        assertEquals(4.84, r.profit, 1e-9)
        assertEquals(15.972, r.sellPrice, 1e-9)
        assertEquals(3.993, r.pricePerPiece, 1e-9)
    }

    @Test
    fun zeroLifeHoursDoesNotDivideByZero() {
        val r = CostCalculator.calculate(base.copy(machineLifeHours = 0.0))
        assertEquals(0.0, r.machineWear, 1e-9)
    }
}
