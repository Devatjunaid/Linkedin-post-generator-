package com.aquadoselab.poolchemistry.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavHostController
import com.aquadoselab.poolchemistry.ads.AdsManager
import com.aquadoselab.poolchemistry.chem.Units
import com.aquadoselab.poolchemistry.data.Sanitizer
import com.aquadoselab.poolchemistry.data.TempUnit
import com.aquadoselab.poolchemistry.data.WaterTest
import com.aquadoselab.poolchemistry.ui.AppViewModel
import com.aquadoselab.poolchemistry.ui.Routes
import com.aquadoselab.poolchemistry.ui.components.AppCard
import com.aquadoselab.poolchemistry.ui.components.InfoBanner
import com.aquadoselab.poolchemistry.ui.components.PrimaryButton
import com.aquadoselab.poolchemistry.ui.components.ScreenColumn
import com.aquadoselab.poolchemistry.ui.components.StepperRow
import com.aquadoselab.poolchemistry.ui.components.fieldColors
import com.aquadoselab.poolchemistry.ui.components.findActivity
import com.aquadoselab.poolchemistry.ui.theme.AppColors

private data class FieldDef(
    val key: String,
    val label: String,
    val hint: String,
    val step: Double,
    val min: Double,
    val max: Double,
    val start: Double,
    val decimals: Int,
    val required: Boolean
)

@Composable
fun TestEntryScreen(vm: AppViewModel, nav: NavHostController) {
    val data by vm.data.collectAsState()
    val context = LocalContext.current
    val pool = data.activePool

    if (pool == null) {
        ScreenColumn {
            Text("New test", style = MaterialTheme.typography.headlineMedium)
            InfoBanner("Set up your pool first so we can work out the right doses for its size.", warn = true, icon = Icons.Filled.Info)
            PrimaryButton("Set up my pool", icon = Icons.Filled.Pool) { nav.navigate(Routes.pool()) }
        }
        return
    }

    val tempUnit = data.settings.tempUnit
    val isSalt = pool.sanitizer == Sanitizer.SALT
    val fields = remember(isSalt, tempUnit) {
        buildList {
            add(FieldDef("fc", "Free chlorine (FC)", "ppm · required", 0.5, 0.0, 50.0, 3.0, 1, true))
            add(FieldDef("ph", "pH", "6.8 – 8.4 · required", 0.1, 6.0, 9.0, 7.5, 1, true))
            add(FieldDef("cc", "Combined chlorine (CC)", "ppm · optional", 0.1, 0.0, 20.0, 0.0, 1, false))
            add(FieldDef("ta", "Total alkalinity (TA)", "ppm · optional", 10.0, 0.0, 400.0, 80.0, 0, false))
            add(FieldDef("cya", "Stabilizer (CYA)", "ppm · optional", 10.0, 0.0, 300.0, 40.0, 0, false))
            add(FieldDef("ch", "Calcium hardness (CH)", "ppm · optional", 25.0, 0.0, 2000.0, 300.0, 0, false))
            if (isSalt) add(FieldDef("salt", "Salt", "ppm · optional", 100.0, 0.0, 8000.0, 3200.0, 0, false))
            if (tempUnit == TempUnit.F) add(FieldDef("temp", "Water temperature", "°F · optional", 1.0, 32.0, 110.0, 80.0, 0, false))
            else add(FieldDef("temp", "Water temperature", "°C · optional", 1.0, 0.0, 43.0, 27.0, 0, false))
        }
    }

    // Last known values are shown as greyed placeholders; leaving a field blank reuses them.
    val last = remember(data.tests, pool.id, tempUnit) {
        mapOf(
            "fc" to data.lastKnown(pool.id) { it.fc },
            "ph" to data.lastKnown(pool.id) { it.ph },
            "cc" to data.lastKnown(pool.id) { it.cc },
            "ta" to data.lastKnown(pool.id) { it.ta },
            "cya" to data.lastKnown(pool.id) { it.cya },
            "ch" to data.lastKnown(pool.id) { it.ch },
            "salt" to data.lastKnown(pool.id) { it.salt },
            "temp" to data.lastKnown(pool.id) { it.tempF }?.let { Units.fToUnit(it, tempUnit) }
        )
    }

    val values = remember { mutableStateMapOf<String, String>() }
    var note by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(setOf<String>()) }

    ScreenColumn {
        Text("New test", style = MaterialTheme.typography.headlineMedium)
        Text(
            "${pool.name} · ${Units.gallonsToDisplay(pool.volumeGallons, data.settings.units)}",
            style = MaterialTheme.typography.labelMedium, color = AppColors.Muted
        )

        AppCard(padding = 12.dp) {
            Column {
                fields.forEachIndexed { i, f ->
                    val placeholder = if (f.required) null else last[f.key]?.let { fmt(it, f.decimals) }
                    StepperRow(
                        label = f.label,
                        hint = if (f.key in errors) "Enter a value between ${fmt(f.min, f.decimals)} and ${fmt(f.max, f.decimals)}"
                        else if (placeholder != null) "${f.hint.substringBefore(" ·")} · blank = last ($placeholder)" else f.hint,
                        value = values[f.key].orEmpty(),
                        onValueChange = { values[f.key] = it; errors = errors - f.key },
                        step = f.step,
                        min = f.min,
                        max = f.max,
                        start = last[f.key] ?: f.start,
                        decimals = f.decimals,
                        placeholder = placeholder,
                        isError = f.key in errors
                    )
                    if (i < fields.lastIndex) HorizontalDivider(color = AppColors.Border)
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(200) },
            label = { Text("Note (optional)") },
            placeholder = { Text("e.g. after heavy rain, cloudy water") },
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        InfoBanner(
            "For best accuracy use a drop test kit (FAS-DPD for chlorine). Test strips are fine for a quick check.",
            warn = false, icon = Icons.Filled.Info
        )

        PrimaryButton("Calculate dosing plan", icon = Icons.AutoMirrored.Filled.ArrowForward) {
            val bad = mutableSetOf<String>()
            val parsed = mutableMapOf<String, Double?>()
            fields.forEach { f ->
                val raw = values[f.key].orEmpty()
                val v = raw.toDoubleOrNull()
                when {
                    raw.isBlank() && f.required -> bad.add(f.key)
                    raw.isBlank() -> parsed.put(f.key, null)
                    v == null || v < f.min || v > f.max -> bad.add(f.key)
                    else -> parsed.put(f.key, v)
                }
            }
            errors = bad
            if (bad.isNotEmpty()) return@PrimaryButton
            val test = WaterTest(
                id = vm.newId(),
                poolId = pool.id,
                timestamp = System.currentTimeMillis(),
                fc = parsed["fc"],
                cc = parsed["cc"],
                ph = parsed["ph"],
                ta = parsed["ta"],
                ch = parsed["ch"],
                cya = parsed["cya"],
                salt = parsed["salt"],
                tempF = parsed["temp"]?.let { Units.unitToF(it, tempUnit) },
                note = note.trim()
            )
            vm.saveTest(test)
            values.clear()
            note = ""
            AdsManager.showInterstitial(context.findActivity()) {
                nav.navigate(Routes.plan(test.id))
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

private fun fmt(v: Double, decimals: Int): String =
    if (decimals == 0) Math.round(v).toString() else String.format(java.util.Locale.US, "%.${decimals}f", v)
