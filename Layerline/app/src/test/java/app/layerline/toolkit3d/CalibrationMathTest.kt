package app.layerline.toolkit3d

import app.layerline.toolkit3d.domain.CalibrationMath
import app.layerline.toolkit3d.domain.Calibrations
import app.layerline.toolkit3d.domain.FilamentMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CalibrationMathTest {

    @Test
    fun eSteps() {
        assertEquals(90.643, CalibrationMath.eSteps(93.0, 120.0, 17.4, 100.0)!!, 1e-3)
        assertNull(CalibrationMath.eSteps(93.0, 100.0, 100.0, 100.0))
    }

    @Test
    fun rotationDistanceIsInverseOfESteps() {
        assertEquals(22.6789 * 0.9, CalibrationMath.rotationDistance(22.6789, 120.0, 30.0, 100.0)!!, 1e-9)
    }

    @Test
    fun flowShrinkPaRetractTemp() {
        assertEquals(93.75, CalibrationMath.flow(100.0, 0.45, 0.48)!!, 1e-9)
        assertEquals(100.583, CalibrationMath.shrinkageScale(100.0, 99.42)!!, 1e-3)
        assertEquals(0.029, CalibrationMath.pressureAdvance(0.0, 0.002, 14.5), 1e-9)
        assertEquals(1.0, CalibrationMath.retraction(0.4, 0.2, 4.0)!!, 1e-9)
        assertEquals(215.0, CalibrationMath.temperatureTower(230.0, 5.0, 4.0)!!, 1e-9)
        assertEquals(18.0, CalibrationMath.volumetricFlow(0.2, 0.45, 200.0), 1e-9)
        assertEquals(166.667, CalibrationMath.maxSpeedForFlow(15.0, 0.2, 0.45)!!, 1e-3)
    }

    @Test
    fun everyCalculatorWorksWithItsDefaults() {
        Calibrations.all.forEach { c -> assertNotNull(c.id, c.compute(c.defaults())) }
    }

    @Test
    fun filamentLength() {
        // 1.75 mm PLA is about 2.98 g per metre.
        assertEquals(2.982, FilamentMath.gramsPerMeter(1.75, 1.24), 1e-3)
        assertEquals(335.3, FilamentMath.metersFromGrams(1000.0, 1.75, 1.24), 0.1)
        assertEquals(0.0, FilamentMath.remainingFromGross(100.0, 230.0), 1e-9)
    }
}
