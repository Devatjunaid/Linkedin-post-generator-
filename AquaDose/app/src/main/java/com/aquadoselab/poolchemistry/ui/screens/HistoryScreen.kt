package com.aquadoselab.poolchemistry.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.aquadoselab.poolchemistry.ads.AdsManager
import com.aquadoselab.poolchemistry.ads.NativeAdCard
import com.aquadoselab.poolchemistry.ads.NativeStyle
import com.aquadoselab.poolchemistry.chem.DosingEngine
import com.aquadoselab.poolchemistry.chem.Units
import com.aquadoselab.poolchemistry.data.Sanitizer
import com.aquadoselab.poolchemistry.data.WaterTest
import com.aquadoselab.poolchemistry.ui.AppViewModel
import com.aquadoselab.poolchemistry.ui.Routes
import com.aquadoselab.poolchemistry.ui.components.AppCard
import com.aquadoselab.poolchemistry.ui.components.ChartLegend
import com.aquadoselab.poolchemistry.ui.components.Chip
import com.aquadoselab.poolchemistry.ui.components.ConfirmDialog
import com.aquadoselab.poolchemistry.ui.components.InfoBanner
import com.aquadoselab.poolchemistry.ui.components.Param
import com.aquadoselab.poolchemistry.ui.components.PrimaryButton
import com.aquadoselab.poolchemistry.ui.components.ScreenColumn
import com.aquadoselab.poolchemistry.ui.components.SectionTitle
import com.aquadoselab.poolchemistry.ui.components.TrendChart
import com.aquadoselab.poolchemistry.ui.components.bandFor
import com.aquadoselab.poolchemistry.ui.components.findActivity
import com.aquadoselab.poolchemistry.ui.components.formatValue
import com.aquadoselab.poolchemistry.ui.components.valueOf
import com.aquadoselab.poolchemistry.ui.goToTab
import com.aquadoselab.poolchemistry.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(vm: AppViewModel, nav: NavHostController) {
    val data by vm.data.collectAsState()
    val context = LocalContext.current
    val pool = data.activePool
    var paramName by rememberSaveable { mutableStateOf(Param.FC.name) }
    var toDelete by remember { mutableStateOf<WaterTest?>(null) }

    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("History", style = MaterialTheme.typography.headlineMedium)
                if (pool != null) Text(pool.name, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
            }
            if (pool != null) {
                IconButton(onClick = {
                    AdsManager.showInterstitial(context.findActivity()) {
                        val intent = vm.exportCsvIntent(context, pool.id)
                        if (intent == null) Toast.makeText(context, "No tests to export yet", Toast.LENGTH_SHORT).show()
                        else context.startActivity(intent)
                    }
                }) { Icon(Icons.Filled.FileDownload, contentDescription = "Export history as CSV", tint = AppColors.Ink) }
            }
        }

        if (pool == null) {
            InfoBanner("Set up a pool to start tracking your water.", warn = true, icon = Icons.Filled.Info)
            PrimaryButton("Set up my pool") { nav.navigate(Routes.pool()) }
            return@ScreenColumn
        }

        val tests = data.testsFor(pool.id)
        val params = Param.entries.filter { it != Param.SALT || pool.sanitizer == Sanitizer.SALT }
        val param = params.firstOrNull { it.name == paramName } ?: Param.FC
        val tempUnit = data.settings.tempUnit

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            params.forEach { p ->
                FilterChip(
                    selected = p == param,
                    onClick = { paramName = p.name },
                    label = { Text(p.short) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.Teal,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White
                    )
                )
            }
        }

        val points = tests
            .mapNotNull { t -> valueOf(param, t)?.let { t.timestamp to it } }
            .sortedBy { it.first }
            .takeLast(30)
        val latestCya = data.lastKnown(pool.id) { it.cya }
        val band = bandFor(param, pool, latestCya)

        AppCard {
            Text(
                "${param.label}${if (param.unit.isNotEmpty()) " (${param.unit})" else ""}",
                style = MaterialTheme.typography.titleSmall
            )
            if (points.size < 2) {
                Text(
                    if (points.isEmpty()) "No ${param.label.lowercase()} readings yet."
                    else "Log at least two tests to see a trend.",
                    style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted
                )
            } else {
                TrendChart(points, band, labelFor = { formatValue(param, it, tempUnit) })
                val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
                ChartLegend(fmt.format(Date(points.first().first)) to fmt.format(Date(points.last().first)))
                val vals = points.map { it.second }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat("Latest", formatValue(param, vals.last(), tempUnit))
                    Stat("Average", formatValue(param, vals.average(), tempUnit))
                    Stat("Low", formatValue(param, vals.min(), tempUnit))
                    Stat("High", formatValue(param, vals.max(), tempUnit))
                }
            }
            if (band != null) {
                Text(
                    "Shaded area: target ${formatValue(param, band.low, tempUnit)} – ${formatValue(param, band.high, tempUnit)}",
                    style = MaterialTheme.typography.bodySmall, color = AppColors.Muted
                )
            }
        }

        SectionTitle("Test log (${tests.size})")

        if (tests.isEmpty()) {
            AppCard {
                Text("No tests logged for ${pool.name} yet.", style = MaterialTheme.typography.bodyMedium)
                PrimaryButton("Log a test") { nav.goToTab(Routes.TEST) }
            }
            NativeAdCard(style = NativeStyle.MEDIUM)
        }

        val dateFmt = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())
        tests.forEachIndexed { i, t ->
            val plan = DosingEngine.plan(pool, DosingEngine.resolve(t, data))
            AppCard(onClick = { nav.navigate(Routes.plan(t.id)) }, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dateFmt.format(Date(t.timestamp)), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    when {
                        t.dosesApplied -> Chip("ADDED", AppColors.OkBg, AppColors.OkText)
                        plan.issues == 0 -> Chip("BALANCED", AppColors.OkBg, AppColors.OkText)
                        else -> Chip("${plan.issues} TO FIX", AppColors.WarnBg, AppColors.WarnText)
                    }
                    IconButton(onClick = { toDelete = t }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete test", tint = AppColors.Muted)
                    }
                }
                Text(summary(t, tempUnit), style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (t.note.isNotBlank()) Text("“${t.note}”", style = MaterialTheme.typography.bodySmall, color = AppColors.InkSoft)
            }
            if (i == 2 || (i == tests.lastIndex && tests.size < 3)) NativeAdCard(style = NativeStyle.SMALL)
        }
        Spacer(Modifier.height(4.dp))
    }

    toDelete?.let { t ->
        ConfirmDialog(
            title = "Delete this test?",
            message = "The readings from ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(t.timestamp))} will be removed from your history.",
            confirm = "Delete",
            onConfirm = { vm.deleteTest(t.id); toDelete = null },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
    }
}

private fun summary(t: WaterTest, tempUnit: com.aquadoselab.poolchemistry.data.TempUnit): String = listOfNotNull(
    t.fc?.let { "FC ${Units.ppm(it)}" },
    t.cc?.let { "CC ${Units.ppm(it)}" },
    t.ph?.let { "pH ${Units.ph(it)}" },
    t.ta?.let { "TA ${Units.ppm(it)}" },
    t.cya?.let { "CYA ${Units.ppm(it)}" },
    t.ch?.let { "CH ${Units.ppm(it)}" },
    t.salt?.let { "Salt ${Units.ppm(it)}" },
    t.tempF?.let { Units.tempDisplay(it, tempUnit) }
).joinToString(" · ")
