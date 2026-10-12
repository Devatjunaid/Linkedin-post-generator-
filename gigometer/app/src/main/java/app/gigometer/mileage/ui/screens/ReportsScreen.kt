package app.gigometer.mileage.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gigometer.mileage.Format
import app.gigometer.mileage.data.AppSettings
import app.gigometer.mileage.data.Trip
import app.gigometer.mileage.export.Exporter
import app.gigometer.mileage.ui.components.AppCard
import app.gigometer.mileage.ui.components.ScreenTitle
import app.gigometer.mileage.ui.theme.LocalPalette
import app.gigometer.mileage.ui.theme.numberStyle
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ReportsScreen(
    allTrips: List<Trip>,
    settings: AppSettings,
    onExportPdf: (Int) -> Unit,
    onExportCsv: (Int) -> Unit,
) {
    val p = LocalPalette.current
    val thisYear = LocalDate.now().year
    val years = (allTrips.map { Format.localDate(it.startTime).year } + thisYear).distinct().sortedDescending()
    var year by rememberSaveable { mutableIntStateOf(thisYear) }
    var menu by remember { mutableStateOf(false) }

    val trips = allTrips.filter { Format.localDate(it.startTime).year == year }
    val business = trips.filter { it.isBusiness }
    val monthly = DoubleArray(12).also { arr -> business.forEach { arr[Format.localDate(it.startTime).monthValue - 1] += it.miles } }
    val lastMonth = if (year == thisYear) LocalDate.now().monthValue else 12
    val lines = Exporter.rateBreakdown(trips, settings, year)
    val deduction = lines.sumOf { it.amount }
    val total = trips.sumOf { it.miles }
    val biz = business.sumOf { it.miles }
    val byPlatform = business.groupBy { it.platform ?: "Unassigned" }.mapValues { (_, v) -> v.sumOf { it.miles } }
        .entries.sortedByDescending { it.value }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScreenTitle("Reports", Modifier.weight(1f))
            Box {
                OutlinedButton(onClick = { menu = true }, shape = RoundedCornerShape(12.dp)) {
                    Text("Tax year $year", color = p.ink, fontWeight = FontWeight.ExtraBold)
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = p.ink, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    years.forEach { y -> DropdownMenuItem(text = { Text("Tax year $y") }, onClick = { year = y; menu = false }) }
                }
            }
        }

        AppCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("Business miles by month", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink, modifier = Modifier.weight(1f))
                Text("Jan – ${Month.of(lastMonth).getDisplayName(TextStyle.SHORT, Locale.US)}", color = p.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            val max = (monthly.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
            val peak = monthly.indices.maxByOrNull { monthly[it] } ?: 0
            Canvas(
                Modifier.fillMaxWidth().height(150.dp).semantics {
                    contentDescription = "Bar chart of business miles per month. Highest: ${Month.of(peak + 1).getDisplayName(TextStyle.FULL, Locale.US)} with ${Format.miles(monthly[peak])} miles."
                }
            ) {
                val gap = 6.dp.toPx()
                val w = (size.width - gap * 11) / 12
                for (i in 0 until 12) {
                    val h = (monthly[i] / max * (size.height - 4)).toFloat()
                    val x = i * (w + gap)
                    val color = when {
                        i + 1 > lastMonth -> p.line
                        year == thisYear && i + 1 == lastMonth -> p.amber
                        else -> p.accent
                    }
                    if (h > 0) drawRoundRect(color, Offset(x, size.height - h), Size(w, h), CornerRadius(6.dp.toPx()))
                    else drawRoundRect(p.line, Offset(x, size.height - 3.dp.toPx()), Size(w, 3.dp.toPx()), CornerRadius(2.dp.toPx()))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (m in 1..12) {
                    Text(Month.of(m).getDisplayName(TextStyle.NARROW, Locale.US), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 10.sp, color = p.muted, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.hero).padding(16.dp)) {
            Text("Estimated mileage deduction · Schedule C, line 9", color = p.heroMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(Format.money(deduction), style = numberStyle(36), color = p.bright)
            lines.filter { it.miles > 0 || lines.size == 1 }.forEach {
                Text(
                    "${Format.miles(it.miles)} mi × ${Format.rate(it.rate)}" + if (lines.size > 1) "  (${Format.dayShort(it.start)} – ${Format.dayShort(it.end)})" else "",
                    color = p.heroInk.copy(alpha = 0.85f), fontSize = 12.sp,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat("Total driven", "${Format.miles(total)} mi", Modifier.weight(1f))
            MiniStat("Business use", if (total > 0) "${(biz / total * 100).toInt()}%" else "—", Modifier.weight(1f))
        }

        if (byPlatform.isNotEmpty()) {
            AppCard {
                Text("By platform", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink)
                Spacer(Modifier.height(8.dp))
                byPlatform.forEach { (name, mi) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, color = p.ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(110.dp).padding(end = 10.dp), maxLines = 1)
                        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(p.line)) {
                            Box(Modifier.fillMaxWidth((mi / biz).toFloat().coerceIn(0.02f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(p.accent))
                        }
                        Text("${Format.miles(mi)} mi", color = p.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }
        }

        Text("Export for taxes", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink)
        Button(
            onClick = { onExportPdf(year) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
        ) {
            Icon(Icons.Filled.Description, null)
            Text("  IRS-ready mileage log (PDF)", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Download, null)
        }
        OutlinedButton(onClick = { onExportCsv(year) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Filled.TableChart, null, tint = p.ink)
            Text("  Spreadsheet (CSV) for your tax app", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Download, null, tint = p.ink)
        }
        Text(
            "Each entry lists the date, start and end location, business purpose, platform and miles — the details the IRS expects a mileage log to keep. Gigometer is not tax advice; confirm with a tax professional.",
            color = p.muted, fontSize = 11.sp, lineHeight = 16.sp,
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(p.card).padding(14.dp)) {
        Text(label.uppercase(), color = p.muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
        Text(value, style = numberStyle(22), color = p.ink)
    }
}
