package com.aquadoselab.poolchemistry.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquadoselab.poolchemistry.chem.Chemistry
import com.aquadoselab.poolchemistry.chem.Level
import com.aquadoselab.poolchemistry.chem.Resolved
import com.aquadoselab.poolchemistry.chem.Units
import com.aquadoselab.poolchemistry.data.Pool
import com.aquadoselab.poolchemistry.data.Sanitizer
import com.aquadoselab.poolchemistry.data.TempUnit
import com.aquadoselab.poolchemistry.data.WaterTest
import com.aquadoselab.poolchemistry.ui.theme.AppColors
import kotlin.math.max
import kotlin.math.min

enum class Param(val label: String, val short: String, val unit: String) {
    FC("Free chlorine", "FC", "ppm"),
    PH("pH", "pH", ""),
    TA("Alkalinity", "TA", "ppm"),
    CYA("Stabilizer (CYA)", "CYA", "ppm"),
    CH("Calcium hardness", "CH", "ppm"),
    CC("Combined chlorine", "CC", "ppm"),
    SALT("Salt", "Salt", "ppm"),
    TEMP("Water temp", "Temp", ""),
    CSI("Water balance (CSI)", "CSI", "")
}

data class Band(val low: Double, val high: Double, val scaleMin: Double, val scaleMax: Double)

/** Acceptable band and display scale for a parameter in this pool. */
fun bandFor(p: Param, pool: Pool, cya: Double?): Band? {
    val r = Chemistry.ranges(pool)
    return when (p) {
        Param.FC -> {
            val l = Chemistry.fcLevels(cya ?: 0.0, pool.sanitizer)
            Band(l.min, l.upper, 0.0, max(l.upper * 1.5, 6.0))
        }
        Param.PH -> Band(r.phLow, r.phHigh, 6.8, 8.2)
        Param.TA -> Band(r.taLow, r.taHigh, 0.0, 200.0)
        Param.CYA -> Band(r.cyaLow, r.cyaHigh, 0.0, max(120.0, r.cyaHigh + 20))
        Param.CH -> Band(r.chLow, r.chHigh, 0.0, 800.0)
        Param.CC -> Band(0.0, 0.5, 0.0, 2.0)
        Param.SALT -> Band(r.saltLow, r.saltHigh, 2000.0, 4600.0)
        Param.CSI -> Band(-0.3, 0.3, -1.0, 1.0)
        Param.TEMP -> null
    }
}

fun valueOf(p: Param, t: WaterTest): Double? = when (p) {
    Param.FC -> t.fc
    Param.PH -> t.ph
    Param.TA -> t.ta
    Param.CYA -> t.cya
    Param.CH -> t.ch
    Param.CC -> t.cc
    Param.SALT -> t.salt
    Param.TEMP -> t.tempF
    Param.CSI -> {
        val ph = t.ph; val ta = t.ta; val ch = t.ch
        if (ph != null && ta != null && ch != null)
            Chemistry.csi(ph, ta, ch, t.cya ?: 0.0, t.tempF ?: 80.0, t.salt ?: 1000.0) else null
    }
}

fun formatValue(p: Param, v: Double, tempUnit: TempUnit): String = when (p) {
    Param.PH -> Units.ph(v)
    Param.CSI -> Units.signed(Math.round(v * 100) / 100.0)
    Param.TEMP -> Units.tempDisplay(v, tempUnit)
    else -> Units.ppm(v)
}

data class TileSpec(
    val param: Param,
    val value: Double?,
    val display: String,
    val level: Level,
    val band: Band?,
    val targetText: String,
    val carried: Boolean
)

fun buildTiles(pool: Pool, r: Resolved, test: WaterTest, tempUnit: TempUnit): List<TileSpec> {
    val list = mutableListOf<TileSpec>()
    fun add(p: Param, v: Double?, target: String, carried: Boolean = false, levelOverride: Level? = null) {
        val band = bandFor(p, pool, r.cya)
        val level = levelOverride ?: if (band == null) Level.UNKNOWN else Chemistry.level(v, band.low, band.high)
        list += TileSpec(p, v, v?.let { formatValue(p, it, tempUnit) } ?: "—", level, band, target, carried)
    }
    val fcl = Chemistry.fcLevels(r.cya ?: 0.0, pool.sanitizer)
    val rg = Chemistry.ranges(pool)
    add(Param.FC, r.fc, "Target ${Units.ppm(fcl.target)} (min ${Units.ppm(fcl.min)})")
    add(Param.PH, r.ph, "Target ${Units.ph(rg.phLow)}–${Units.ph(rg.phHigh)}")
    add(Param.TA, r.ta, "Target ${Units.ppm(rg.taLow)}–${Units.ppm(rg.taHigh)}", carried = test.ta == null && r.ta != null)
    add(Param.CYA, r.cya, "Target ${Units.ppm(rg.cyaLow)}–${Units.ppm(rg.cyaHigh)}", carried = test.cya == null && r.cya != null)
    add(Param.CH, r.ch, "Target ${Units.ppm(rg.chLow)}–${Units.ppm(rg.chHigh)}", carried = test.ch == null && r.ch != null)
    if (pool.sanitizer == Sanitizer.SALT) {
        add(Param.SALT, r.salt, "Target ${Units.ppm(rg.saltLow)}–${Units.ppm(rg.saltHigh)}", carried = test.salt == null && r.salt != null)
    }
    if (r.cc != null) add(Param.CC, r.cc, "Keep at 0.5 or below")
    val ph = r.ph; val ta = r.ta; val ch = r.ch
    val csi = if (ph != null && ta != null && ch != null)
        Chemistry.csi(ph, ta, ch, r.cya ?: 0.0, r.tempF ?: 80.0, r.salt ?: if (pool.sanitizer == Sanitizer.SALT) pool.targets.salt else 1000.0)
    else null
    add(Param.CSI, csi, "Balanced −0.3 to +0.3", levelOverride = Chemistry.csiLevel(csi))
    return list
}

/** Simple time-series line chart with the acceptable band shaded. */
@Composable
fun TrendChart(
    points: List<Pair<Long, Double>>,
    band: Band?,
    labelFor: (Double) -> String,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = AppColors.Muted)
    val values = points.map { it.second }
    var yMin = values.minOrNull() ?: 0.0
    var yMax = values.maxOrNull() ?: 1.0
    if (band != null) {
        yMin = min(yMin, band.low)
        yMax = max(yMax, band.high)
    }
    val pad = (yMax - yMin).let { if (it <= 0) 1.0 else it * 0.12 }
    yMin -= pad
    yMax += pad
    if (band != null && band.scaleMin >= 0 && yMin < 0 && (values.minOrNull() ?: 0.0) >= 0) yMin = 0.0

    Canvas(modifier.fillMaxWidth().height(180.dp)) {
        val left = 40.dp.toPx()
        val bottom = size.height - 6.dp.toPx()
        val top = 8.dp.toPx()
        val right = size.width - 10.dp.toPx()
        fun y(v: Double) = (bottom - (v - yMin) / (yMax - yMin) * (bottom - top)).toFloat()
        val t0 = points.first().first
        val t1 = points.last().first
        fun x(t: Long) = if (t1 == t0) (left + right) / 2 else (left + (t - t0).toDouble() / (t1 - t0) * (right - left)).toFloat()

        if (band != null) {
            val bt = y(band.high).coerceIn(top, bottom)
            val bb = y(band.low).coerceIn(top, bottom)
            drawRect(AppColors.OkBg, topLeft = Offset(left, bt), size = Size(right - left, bb - bt))
            listOf(band.high, band.low).forEach { v ->
                val layout = measurer.measure(labelFor(v), labelStyle)
                drawText(layout, topLeft = Offset(left - layout.size.width - 6.dp.toPx(), y(v) - layout.size.height / 2))
            }
        }
        drawLine(AppColors.BorderStrong, Offset(left, bottom), Offset(right, bottom), strokeWidth = 1.dp.toPx())

        val path = Path()
        points.forEachIndexed { i, (t, v) ->
            if (i == 0) path.moveTo(x(t), y(v)) else path.lineTo(x(t), y(v))
        }
        drawPath(path, AppColors.Teal, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.forEachIndexed { i, (t, v) ->
            val last = i == points.lastIndex
            val outOfBand = band != null && (v < band.low || v > band.high)
            val c = if (outOfBand) Color(0xFFD9822B) else AppColors.Teal
            drawCircle(Color.White, radius = (if (last) 6.5f else 4.5f).dp.toPx(), center = Offset(x(t), y(v)))
            drawCircle(c, radius = (if (last) 5f else 3.2f).dp.toPx(), center = Offset(x(t), y(v)))
        }
    }
}

@Composable
fun ChartLegend(dates: Pair<String, String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(dates.first, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
        Text(dates.second, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
    }
}
