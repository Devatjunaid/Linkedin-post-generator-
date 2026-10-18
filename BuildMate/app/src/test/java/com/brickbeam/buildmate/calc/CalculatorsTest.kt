package com.brickbeam.buildmate.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LengthParserTest {
    private val ft = Conv.M_PER_FT
    private val inch = Conv.M_PER_IN

    @Test fun imperialForms() {
        assertEquals(12 * ft, LengthParser.parse("12", UnitSystem.IMPERIAL)!!, 1e-9)
        assertEquals(12.5 * ft, LengthParser.parse("12.5", UnitSystem.IMPERIAL)!!, 1e-9)
        assertEquals(12 * ft + 6 * inch, LengthParser.parse("12' 6\"", UnitSystem.IMPERIAL)!!, 1e-9)
        assertEquals(12 * ft + 6 * inch, LengthParser.parse("12'6", UnitSystem.IMPERIAL)!!, 1e-9)
        assertEquals(12 * ft + 6 * inch, LengthParser.parse("12 ft 6 in", UnitSystem.IMPERIAL)!!, 1e-9)
        assertEquals(6 * inch, LengthParser.parse("6\"", UnitSystem.IMPERIAL)!!, 1e-9)
        assertEquals(3.5 * inch, LengthParser.parse("3 1/2 in", UnitSystem.IMPERIAL)!!, 1e-9)
        assertNull(LengthParser.parse("abc", UnitSystem.IMPERIAL))
        assertNull(LengthParser.parse("", UnitSystem.IMPERIAL))
    }

    @Test fun smallImperialDefaultsToInches() {
        assertEquals(9.525, LengthParser.parseSmallMm("3/8", UnitSystem.IMPERIAL)!!, 1e-9)
    }

    @Test fun metricForms() {
        assertEquals(3.6, LengthParser.parse("3,6", UnitSystem.METRIC)!!, 1e-9)
        assertEquals(3.6, LengthParser.parse("360 cm", UnitSystem.METRIC)!!, 1e-9)
        assertEquals(10.0, LengthParser.parseSmallMm("10", UnitSystem.METRIC)!!, 1e-9)
        assertEquals(10.0, LengthParser.parseSmallMm("1 cm", UnitSystem.METRIC)!!, 1e-9)
    }

    @Test fun formatting() {
        assertEquals("12′ 6″", LengthFormat.feetInches(12 * ft + 6 * inch))
        assertEquals("3 3/8", LengthFormat.inchFraction(3.375))
        assertEquals("3.6 m", LengthFormat.big(3.6, UnitSystem.METRIC))
    }
}

class BrickCalculatorTest {
    private fun usWall(waste: Double = 5.0) = BrickInput(
        wallLengthM = 20 * Conv.M_PER_FT, wallHeightM = 8 * Conv.M_PER_FT,
        openings = listOf(
            Opening(3 * Conv.M_PER_FT, 7 * Conv.M_PER_FT),
            Opening(4 * Conv.M_PER_FT, 3 * Conv.M_PER_FT),
        ),
        unit = MasonryUnits.US_MODULAR, jointMm = 9.525, leaves = 1, wastePct = waste,
        sandParts = 3.0, mortarWastePct = 25.0, cementBag = Markets.US.masonryCement,
        tiesPerM2 = Markets.US.wallTiesPerM2, palletSize = 500, tiedVeneer = true,
    )

    @Test fun modularBrickPerSqFt() {
        // Actual 7⅝ × 2¼ in + ⅜ in joints = 8 × 2⅝ in per brick -> 144 / 21 = 6.857.
        // (BIA's rounded 6.75 assumes 2⅔ in modular coursing.)
        val perFt2 = BrickCalculator.unitsPerM2(MasonryUnits.US_MODULAR, 9.525) * Conv.M2_PER_FT2
        assertEquals(144.0 / 21.0, perFt2, 0.01)
    }

    @Test fun usWallWithOpenings() {
        val r = BrickCalculator.calculate(usWall())
        assertEquals(127.0, Conv.m2ToFt2(r.netAreaM2), 1e-6)
        // 127 ft² × 6.857 × 1.05 = 914.4 -> 915
        assertTrue(r.unitsToBuy.toString(), r.unitsToBuy in 912..917)
        assertEquals(r.unitsToBuy / 500, r.fullPallets)
        assertEquals(48, r.wallTies) // 127 / 2.67 = 47.6
        // BIA: ~5.5 ft³ mortar per 100 ft² of modular brick (no waste)
        val mortarNoWaste = Conv.m3ToFt3(r.mortarWetM3 / 1.25)
        assertEquals(5.5 * 1.27, mortarNoWaste, 0.6)
    }

    @Test fun germanNfHalfBrickWallIsAbout48PerM2() {
        assertEquals(48.0, BrickCalculator.unitsPerM2(MasonryUnits.DE_NF, 12.0), 0.5)
        assertEquals(59.3, BrickCalculator.unitsPerM2(MasonryUnits.UK_STANDARD, 10.0), 0.1)
    }

    @Test fun openingsLargerThanWallClampToZero() {
        val r = BrickCalculator.calculate(usWall().copy(openings = listOf(Opening(100.0, 100.0))))
        assertEquals(0.0, r.netAreaM2, 1e-9)
        assertEquals(0, r.unitsToBuy)
    }
}

class ConcreteCalculatorTest {
    @Test fun slabVolume() {
        val d = ConcreteDims(ConcreteShape.SLAB, 6.0, 4.0, 0.15)
        assertEquals(3.6, ConcreteCalculator.volume(d), 1e-9)
    }

    @Test fun stepsVolume() {
        // 3 steps, 1 m wide, 0.18 rise, 0.28 run: (1+2+3) × 1 × 0.28 × 0.18
        val d = ConcreteDims(ConcreteShape.STEPS, a = 1.0, b = 0.18, c = 0.28, count = 3)
        assertEquals(6 * 0.28 * 0.18, ConcreteCalculator.volume(d), 1e-9)
    }

    @Test fun c25SlabMaterials() {
        val d = ConcreteDims(ConcreteShape.SLAB, 6.0, 4.0, 0.15)
        val r = ConcreteCalculator.calculate(d, MixGrades.EN[2], 0.0, Markets.EU, Markets.EU.premixBags[0])
        assertEquals(1152.0, r.cementKg, 1e-6)
        assertEquals(47, r.cementBags)
        assertEquals(1152.0 * 0.55, r.waterL, 1e-6)
        assertEquals(3.75, r.readyMixOrderM3, 1e-9) // next 0.25 m³ step
        assertEquals(1, r.trucks)
        assertTrue(r.recommendReadyMix)
    }

    @Test fun usSlabRoundsToQuarterYard() {
        // 20 × 12 ft × 4 in = 80 ft³ = 2.963 yd³ (+5% = 3.111) -> 3.25 yd³
        val d = ConcreteDims(ConcreteShape.SLAB, 20 * Conv.M_PER_FT, 12 * Conv.M_PER_FT, 4 * Conv.M_PER_IN)
        val r = ConcreteCalculator.calculate(d, MixGrades.US[1], 5.0, Markets.US, Markets.US.premixBags[0])
        assertEquals(3.25, Conv.m3ToYd3(r.readyMixOrderM3), 1e-9)
        assertEquals(140, r.premixBags) // 84 ft³ / 0.6 = 140
    }
}

class RebarCalculatorTest {
    private val d10 = Markets.EU.rebarSizes.first { it.id == "d10" }

    @Test fun barMassFormula() {
        assertEquals(0.617, d10.kgPerM, 0.001)
        val n4 = Markets.US.rebarSizes.first { it.id == "n4" }
        assertEquals(0.668, n4.kgPerM / Conv.KG_M_PER_LB_FT, 1e-9)
    }

    @Test fun slabMesh() {
        val r = RebarCalculator.calculate(
            RebarInput(6.0, 4.0, 0.05, 0.15, 0.15, d10, 6.0, 40.0, 1, 0.9),
        )
        assertEquals(27, r.barsAlongLength) // ceil(3.9/0.15)=26, +1
        assertEquals(41, r.barsAlongWidth)  // ceil(5.9/0.15)=40 (39.33->40), +1
        assertEquals(27 * 5.9 + 41 * 3.9, r.totalCutLengthM, 1e-6)
        assertEquals(27 * 41, r.tiePoints)
        // 41 pieces of 3.9 m: one per 6 m bar (2.1 m offcut each) + 27 pieces of 5.9 m
        assertEquals(68, r.plan.stockBars)
    }

    @Test fun lapSplicing() {
        val pieces = RebarCalculator.piecesForRun(15.0, 6.0, 0.4)
        // 15 m needs 3 bars: 6 + 6 + (15 + 2×0.4 - 12) = 3.8
        assertEquals(listOf(6.0, 6.0, 3.8), pieces.map { Math.round(it * 1000) / 1000.0 })
    }

    @Test fun ffdPacksShortPieces() {
        val plan = RebarCalculator.optimize(listOf(2.9, 2.9, 2.9, 2.9), 6.0)
        assertEquals(2, plan.stockBars)
        assertEquals(0.4, plan.offcutM, 1e-9)
    }
}

class TileCalculatorTest {
    private fun base(pattern: TilePattern) = TileInput(
        areaLengthM = 4.2, areaWidthM = 3.6, deductM2 = 0.0,
        tileLengthMm = 600.0, tileWidthMm = 300.0, tileThicknessMm = 9.0, jointMm = 3.0,
        pattern = pattern, sparePct = 0.0, tilesPerBox = 8, notch = TrowelNotch.N10,
        adhesiveBagKg = 20.0, groutBagKg = 5.0,
    )

    @Test fun straightUsesRowColumnLayout() {
        val r = TileCalculator.calculate(base(TilePattern.STRAIGHT))
        // 4200 / 603 -> 7 across, 3600 / 303 -> 12 down = 84; rotated: 14 × 6 = 84
        assertEquals(84, r.tilesForLayout)
        assertEquals(11, r.boxes)
    }

    @Test fun herringbone() {
        val r = TileCalculator.calculate(base(TilePattern.HERRINGBONE))
        val expected = kotlin.math.ceil(15.12 / (0.603 * 0.303) * 1.17 - 1e-9).toInt()
        assertEquals(expected, r.tilesForLayout)
        assertTrue(r.backButter)
    }

    @Test fun groutFormula() {
        assertEquals(0.216, TileCalculator.groutKgPerM2(600.0, 300.0, 3.0, 9.0), 1e-9)
    }
}

class PaintCalculatorTest {
    private val ukCans = Markets.UK.paintCans
    private val prices = DefaultPrices.forZone(PriceZone.GBP)

    @Test fun roomAreas() {
        val r = PaintCalculator.calculate(
            PaintInput(4.2, 3.6, 2.4, null, 1, 1.68, 1, 1.8, 2, false, 2, 12.0, Surface.SMOOTH, false, 10.0),
        )
        assertEquals(37.44, r.wallAreaM2, 1e-9)
        assertEquals(33.96, r.paintableWallM2, 1e-9)
        assertEquals(33.96 * 2 / 12, r.wallLitres, 1e-9)
    }

    @Test fun cheapestCanMix() {
        val plan = PaintCalculator.planCans(5.66, ukCans, prices)
        assertEquals("1 × 1 L + 1 × 5 L", plan.description)
        assertEquals(41.0, plan.cost, 1e-9)
    }

    @Test fun usGallons() {
        val plan = PaintCalculator.planCans(2.47 * Conv.L_PER_GAL, Markets.US.paintCans, DefaultPrices.forZone(PriceZone.USD))
        // 2 gal + 2 qt ($126) beats 3 gal ($135)
        assertEquals("2 × Quart + 2 × Gallon", plan.description)
    }
}

class EstimateMathTest {
    private val lines = listOf(
        EstimateLine(Category.BRICK, 900.0, 0.65, false),
        EstimateLine(Category.PAINT, 3.0, 45.0, false),
        EstimateLine(Category.TILE, 10.0, 5.0, true),
    )

    @Test fun usSalesTaxAdded() {
        val t = EstimateMath.totals(lines, 8.25, false, 0.0, 75.0)
        assertEquals(720.0, t.materials, 1e-9)
        assertEquals(59.4, t.tax, 1e-9)
        assertEquals(720.0 + 59.4 + 75.0, t.total, 1e-9)
        assertEquals(50.0, t.ownedValue, 1e-9)
    }

    @Test fun euVatIncluded() {
        val t = EstimateMath.totals(lines, 20.0, true, 10.0, 0.0)
        assertEquals(120.0, t.tax, 1e-9)
        assertEquals(720.0 + 72.0, t.total, 1e-9)
    }
}

class RegionsTest {
    @Test fun allStatesPresent() {
        assertEquals(51, Regions.US_STATES.size) // 50 states + DC
        assertEquals(Regions.ALL.size, Regions.ALL.map { it.code }.toSet().size)
    }

    @Test fun defaultBricksExist() {
        Regions.ALL.forEach { assertTrue(it.code, MasonryUnits.byId(it.defaultBrickId) != null) }
    }
}
