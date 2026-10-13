package com.aquadoselab.poolchemistry.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavHostController
import com.aquadoselab.poolchemistry.ads.AdsManager
import com.aquadoselab.poolchemistry.chem.FEET_PER_METER
import com.aquadoselab.poolchemistry.chem.LITERS_PER_GALLON
import com.aquadoselab.poolchemistry.chem.Products
import com.aquadoselab.poolchemistry.chem.Units
import com.aquadoselab.poolchemistry.chem.Volume
import com.aquadoselab.poolchemistry.data.Pool
import com.aquadoselab.poolchemistry.data.PoolShape
import com.aquadoselab.poolchemistry.data.Sanitizer
import com.aquadoselab.poolchemistry.data.Surface
import com.aquadoselab.poolchemistry.data.Targets
import com.aquadoselab.poolchemistry.data.UnitSystem
import com.aquadoselab.poolchemistry.ui.AppViewModel
import com.aquadoselab.poolchemistry.ui.components.AppCard
import com.aquadoselab.poolchemistry.ui.components.BackHeader
import com.aquadoselab.poolchemistry.ui.components.ChoiceDialog
import com.aquadoselab.poolchemistry.ui.components.ConfirmDialog
import com.aquadoselab.poolchemistry.ui.components.InfoBanner
import com.aquadoselab.poolchemistry.ui.components.NumberField
import com.aquadoselab.poolchemistry.ui.components.PrimaryButton
import com.aquadoselab.poolchemistry.ui.components.ScreenColumn
import com.aquadoselab.poolchemistry.ui.components.Segmented
import com.aquadoselab.poolchemistry.ui.components.SectionTitle
import com.aquadoselab.poolchemistry.ui.components.fieldColors
import com.aquadoselab.poolchemistry.ui.components.findActivity
import com.aquadoselab.poolchemistry.ui.theme.AppColors

private fun Double.str(): String = if (this <= 0.0) "" else Units.num(this).replace(",", "")

@Composable
fun PoolEditScreen(vm: AppViewModel, nav: NavHostController, poolId: String?) {
    val data by vm.data.collectAsState()
    val context = LocalContext.current
    val metric = data.settings.units == UnitSystem.METRIC
    val existing = remember(poolId) { poolId?.let { id -> data.pools.firstOrNull { it.id == id } } }
    val base = remember(poolId) {
        existing ?: Pool(id = vm.newId(), name = if (data.pools.isEmpty()) "My Pool" else "Pool ${data.pools.size + 1}")
    }
    fun len(ft: Double) = if (metric) ft / FEET_PER_METER else ft

    var name by remember { mutableStateOf(base.name) }
    var shape by remember { mutableStateOf(base.shape) }
    var length by remember { mutableStateOf(len(base.lengthFt).str()) }
    var width by remember { mutableStateOf(len(base.widthFt).str()) }
    var width2 by remember { mutableStateOf(len(base.width2Ft).str()) }
    var shallow by remember { mutableStateOf(len(base.shallowFt).str()) }
    var deep by remember { mutableStateOf(len(base.deepFt).str()) }
    var manualVolume by remember {
        mutableStateOf((if (metric) base.volumeGallons * LITERS_PER_GALLON else base.volumeGallons).str())
    }
    var sanitizer by remember { mutableStateOf(base.sanitizer) }
    var surface by remember { mutableStateOf(base.surface) }
    var indoor by remember { mutableStateOf(base.indoor) }
    var tPh by remember { mutableStateOf(base.targets.ph.toString()) }
    var tTa by remember { mutableStateOf(base.targets.ta.str()) }
    var tCh by remember { mutableStateOf(base.targets.ch.str()) }
    var tCya by remember { mutableStateOf(if (base.targets.cya == 0.0) "0" else base.targets.cya.str()) }
    var tSalt by remember { mutableStateOf(base.targets.salt.str()) }
    var products by remember { mutableStateOf(base.products) }
    var error by remember { mutableStateOf<String?>(null) }
    var picker by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun applyRecommended(s: Sanitizer, sf: Surface, ind: Boolean) {
        val r = Targets.recommended(s, sf, ind)
        tPh = r.ph.toString(); tTa = r.ta.str(); tCh = r.ch.str()
        tCya = if (r.cya == 0.0) "0" else r.cya.str(); tSalt = r.salt.str()
    }

    fun toFt(s: String) = (s.toDoubleOrNull() ?: 0.0).let { if (metric) it * FEET_PER_METER else it }
    val gallons = if (shape == PoolShape.CUSTOM) {
        (manualVolume.toDoubleOrNull() ?: 0.0).let { if (metric) it / LITERS_PER_GALLON else it }
    } else Volume.gallons(shape, toFt(length), toFt(width), toFt(width2), toFt(shallow), toFt(deep))

    val lu = Units.lengthUnit(data.settings.units)

    Column {
        BackHeader(if (existing == null) "Add pool" else "Edit pool", onBack = { nav.popBackStack() }) {
            if (existing != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete pool", tint = AppColors.Muted)
                }
            }
        }
        ScreenColumn {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("Pool name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("Size")
            AppCard {
                Text("Shape", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                Segmented(
                    options = listOf(PoolShape.RECTANGLE, PoolShape.ROUND, PoolShape.OVAL),
                    selected = shape,
                    label = { it.label },
                    onSelect = { shape = it }
                )
                Segmented(
                    options = listOf(PoolShape.KIDNEY, PoolShape.CUSTOM),
                    selected = shape,
                    label = { it.label },
                    onSelect = { shape = it }
                )
                if (shape == PoolShape.CUSTOM) {
                    NumberField(
                        manualVolume, { manualVolume = it },
                        label = "Pool volume", suffix = Units.volumeUnit(data.settings.units)
                    )
                    Text(
                        "Use the volume from your pool builder or liner paperwork if you have it — it's the most accurate.",
                        style = MaterialTheme.typography.bodySmall, color = AppColors.Muted
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(
                            length, { length = it },
                            label = if (shape == PoolShape.ROUND) "Diameter" else "Length",
                            suffix = lu, modifier = Modifier.weight(1f)
                        )
                        if (shape != PoolShape.ROUND) {
                            NumberField(
                                width, { width = it },
                                label = if (shape == PoolShape.KIDNEY) "Wide end" else "Width",
                                suffix = lu, modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (shape == PoolShape.KIDNEY) {
                        NumberField(width2, { width2 = it }, label = "Narrow end width", suffix = lu)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(shallow, { shallow = it }, label = "Shallow depth", suffix = lu, modifier = Modifier.weight(1f))
                        NumberField(deep, { deep = it }, label = "Deep depth", suffix = lu, modifier = Modifier.weight(1f))
                    }
                    Text(
                        "Same depth everywhere (e.g. above-ground)? Enter it in both boxes. Measure water depth, not wall height.",
                        style = MaterialTheme.typography.bodySmall, color = AppColors.Muted
                    )
                }
                Text("Estimated volume", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                Text(
                    if (gallons > 0) Units.gallonsToDisplay(gallons, data.settings.units) else "—",
                    fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.Ink
                )
            }

            SectionTitle("Pool type")
            AppCard {
                Text("Sanitizer", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                Segmented(
                    options = Sanitizer.entries.toList(), selected = sanitizer, label = { it.label },
                    onSelect = { sanitizer = it; applyRecommended(it, surface, indoor) }
                )
                Text("Surface", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                Segmented(
                    options = Surface.entries.toList(), selected = surface,
                    label = { if (it == Surface.PLASTER) "Plaster" else it.label },
                    onSelect = { surface = it; applyRecommended(sanitizer, it, indoor) }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Indoor pool", style = MaterialTheme.typography.titleSmall)
                        Text("No sunlight, so it needs little or no stabilizer.", style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
                    }
                    Switch(
                        checked = indoor,
                        onCheckedChange = { indoor = it; applyRecommended(sanitizer, surface, it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Teal, checkedThumbColor = Color.White)
                    )
                }
            }

            SectionTitle("Target levels") {
                TextButton(onClick = { applyRecommended(sanitizer, surface, indoor) }) {
                    Icon(Icons.Filled.Restore, contentDescription = null, tint = AppColors.TealDark)
                    Text(" Recommended", color = AppColors.TealDark)
                }
            }
            AppCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(tPh, { tPh = it }, label = "pH", modifier = Modifier.weight(1f))
                    NumberField(tTa, { tTa = it }, label = "Alkalinity", suffix = "ppm", modifier = Modifier.weight(1f), decimal = false)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(tCh, { tCh = it }, label = "Calcium", suffix = "ppm", modifier = Modifier.weight(1f), decimal = false)
                    NumberField(tCya, { tCya = it }, label = "CYA", suffix = "ppm", modifier = Modifier.weight(1f), decimal = false)
                }
                if (sanitizer == Sanitizer.SALT) {
                    NumberField(tSalt, { tSalt = it }, label = "Salt (from your cell's manual)", suffix = "ppm", decimal = false)
                }
                Text(
                    "Free chlorine targets are worked out automatically from your CYA level.",
                    style = MaterialTheme.typography.bodySmall, color = AppColors.Muted
                )
            }

            SectionTitle("Products you use")
            AppCard {
                ProductRow("Chlorine", Products.chlorine(products.chlorine).name) { picker = "chlorine" }
                ProductRow("pH down", Products.acid(products.acid).name) { picker = "acid" }
                ProductRow("Calcium", Products.calcium(products.calcium).name) { picker = "calcium" }
                Text(
                    "Doses are calculated for these exact products and strengths. Check the % on your label.",
                    style = MaterialTheme.typography.bodySmall, color = AppColors.Muted
                )
            }

            error?.let { InfoBanner(it, warn = true, icon = Icons.Filled.Info) }

            PrimaryButton("Save pool", icon = Icons.Filled.Check) {
                val targets = Targets(
                    ph = tPh.toDoubleOrNull() ?: -1.0,
                    ta = tTa.toDoubleOrNull() ?: -1.0,
                    ch = tCh.toDoubleOrNull() ?: -1.0,
                    cya = tCya.toDoubleOrNull() ?: -1.0,
                    salt = if (sanitizer == Sanitizer.SALT) tSalt.toDoubleOrNull() ?: -1.0 else (tSalt.toDoubleOrNull() ?: 3200.0)
                )
                error = when {
                    name.isBlank() -> "Give your pool a name."
                    gallons < 50 -> "Enter the pool size so we can calculate doses."
                    gallons > 2_000_000 -> "That volume looks too large. Check the measurements and units."
                    targets.ph !in 7.0..8.0 -> "Target pH should be between 7.0 and 8.0."
                    targets.ta !in 40.0..200.0 -> "Target alkalinity should be between 40 and 200 ppm."
                    targets.ch !in 0.0..1000.0 -> "Target calcium should be between 0 and 1000 ppm."
                    targets.cya !in 0.0..150.0 -> "Target CYA should be between 0 and 150 ppm."
                    sanitizer == Sanitizer.SALT && targets.salt !in 1000.0..7000.0 -> "Target salt should be between 1000 and 7000 ppm."
                    else -> null
                }
                if (error != null) return@PrimaryButton
                val pool = base.copy(
                    name = name.trim(),
                    shape = shape,
                    lengthFt = toFt(length),
                    widthFt = toFt(width),
                    width2Ft = toFt(width2),
                    shallowFt = toFt(shallow),
                    deepFt = toFt(deep),
                    volumeGallons = gallons,
                    sanitizer = sanitizer,
                    surface = surface,
                    indoor = indoor,
                    targets = targets,
                    products = products
                )
                vm.savePool(pool)
                AdsManager.showInterstitial(context.findActivity()) { nav.popBackStack() }
            }
            Spacer(Modifier.height(4.dp))
        }
    }

    when (picker) {
        "chlorine" -> ChoiceDialog(
            "Chlorine you use", Products.chlorine.map { it.id }, products.chlorine,
            label = { Products.chlorine(it).name }, onDismiss = { picker = null },
            onSelect = { products = products.copy(chlorine = it); picker = null }
        )
        "acid" -> ChoiceDialog(
            "pH reducer you use", Products.acids.map { it.id }, products.acid,
            label = { Products.acid(it).name }, onDismiss = { picker = null },
            onSelect = { products = products.copy(acid = it); picker = null }
        )
        "calcium" -> ChoiceDialog(
            "Calcium product you use", Products.calcium.map { it.id }, products.calcium,
            label = { Products.calcium(it).name }, onDismiss = { picker = null },
            onSelect = { products = products.copy(calcium = it); picker = null }
        )
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Delete ${existing.name}?",
            message = "This removes the pool and all of its test history. This can't be undone.",
            confirm = "Delete",
            onConfirm = {
                confirmDelete = false
                vm.deletePool(existing.id)
                nav.popBackStack()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun ProductRow(label: String, value: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
        TextButton(onClick = onClick) { Text("Change", color = AppColors.TealDark, fontWeight = FontWeight.Bold) }
    }
}
