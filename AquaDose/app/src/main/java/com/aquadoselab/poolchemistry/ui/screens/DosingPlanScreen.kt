package com.aquadoselab.poolchemistry.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.aquadoselab.poolchemistry.ads.AdsManager
import com.aquadoselab.poolchemistry.ads.NativeAdCard
import com.aquadoselab.poolchemistry.ads.NativeStyle
import com.aquadoselab.poolchemistry.chem.Chemistry
import com.aquadoselab.poolchemistry.chem.DoseStep
import com.aquadoselab.poolchemistry.chem.DosingEngine
import com.aquadoselab.poolchemistry.chem.DosingPlan
import com.aquadoselab.poolchemistry.chem.Level
import com.aquadoselab.poolchemistry.chem.ProductSlot
import com.aquadoselab.poolchemistry.chem.Products
import com.aquadoselab.poolchemistry.chem.StepKind
import com.aquadoselab.poolchemistry.chem.Units
import com.aquadoselab.poolchemistry.data.Pool
import com.aquadoselab.poolchemistry.data.UnitSystem
import com.aquadoselab.poolchemistry.ui.AppViewModel
import com.aquadoselab.poolchemistry.ui.Routes
import com.aquadoselab.poolchemistry.ui.components.AppCard
import com.aquadoselab.poolchemistry.ui.components.BackHeader
import com.aquadoselab.poolchemistry.ui.components.Bullet
import com.aquadoselab.poolchemistry.ui.components.Chip
import com.aquadoselab.poolchemistry.ui.components.ChoiceDialog
import com.aquadoselab.poolchemistry.ui.components.InfoBanner
import com.aquadoselab.poolchemistry.ui.components.PrimaryButton
import com.aquadoselab.poolchemistry.ui.components.ScreenColumn
import com.aquadoselab.poolchemistry.ui.components.SecondaryButton
import com.aquadoselab.poolchemistry.ui.components.StatusChip
import com.aquadoselab.poolchemistry.ui.components.StepConnector
import com.aquadoselab.poolchemistry.ui.components.findActivity
import com.aquadoselab.poolchemistry.ui.goToTab
import com.aquadoselab.poolchemistry.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DosingPlanScreen(vm: AppViewModel, nav: NavHostController, testId: String) {
    val data by vm.data.collectAsState()
    val context = LocalContext.current
    val test = data.tests.firstOrNull { it.id == testId }
    val pool = test?.let { t -> data.pools.firstOrNull { it.id == t.poolId } }
    var picker by remember { mutableStateOf<ProductSlot?>(null) }

    if (test == null || pool == null) {
        Column {
            BackHeader("Dosing plan", onBack = { nav.popBackStack() })
            ScreenColumn {
                InfoBanner("This test was deleted.", warn = true, icon = Icons.Filled.Info)
            }
        }
        return
    }

    val resolved = DosingEngine.resolve(test, data)
    val plan = DosingEngine.plan(pool, resolved)
    val units = data.settings.units
    val dateText = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(test.timestamp))

    Column {
        BackHeader(
            title = "Your dosing plan",
            subtitle = "${Units.gallonsToDisplay(pool.volumeGallons, units)} · tested $dateText",
            onBack = { nav.popBackStack() }
        ) {
            IconButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Pool dosing plan — ${pool.name}")
                    putExtra(Intent.EXTRA_TEXT, planAsText(pool, plan, units, dateText))
                }
                context.startActivity(Intent.createChooser(send, "Share plan"))
            }) { Icon(Icons.Filled.Share, contentDescription = "Share plan", tint = AppColors.Ink) }
        }

        ScreenColumn {
            InfoBanner(
                plan.swim.message,
                warn = !plan.swim.safe,
                icon = if (plan.swim.safe) Icons.Filled.CheckCircle else Icons.Filled.Warning
            )
            plan.warnings.forEach { InfoBanner(it, warn = true, icon = Icons.Filled.Warning) }
            if (test.dosesApplied) {
                InfoBanner("You marked these chemicals as added. Retest to see your new levels.", warn = false, icon = Icons.Filled.Check)
            }

            val doseSteps = plan.steps
            if (doseSteps.isEmpty()) {
                AppCard(background = AppColors.OkBg, border = AppColors.OkBg, padding = 20.dp) {
                    Icon(Icons.Filled.Celebration, contentDescription = null, tint = AppColors.TealDark, modifier = Modifier.size(32.dp))
                    Text("Your water is balanced", style = MaterialTheme.typography.headlineSmall, color = AppColors.OkText)
                    Text(
                        "Nothing to add right now. Keep chlorine between ${Units.ppm(plan.fcLevels.min)} and ${Units.ppm(plan.fcLevels.upper)} ppm and test again in 1–2 days.",
                        style = MaterialTheme.typography.bodyMedium, color = AppColors.InkSoft
                    )
                }
            } else {
                var number = 0
                doseSteps.forEachIndexed { i, step ->
                    if (step.kind != StepKind.RETEST) number++
                    StepCard(step, if (step.kind == StepKind.RETEST) null else number, units, onChange = { picker = it })
                    val wait = step.waitAfter
                    if (wait != null && i < doseSteps.lastIndex) {
                        Row(Modifier.padding(start = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            StepConnector(28.dp)
                            Spacer(Modifier.width(14.dp))
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = AppColors.TealDark, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(wait, style = MaterialTheme.typography.labelMedium, color = AppColors.TealDark)
                        }
                    }
                }
            }

            BalanceCard(plan)
            ChlorineGuideCard(plan, resolved.cya)

            plan.notes.forEach { InfoBanner(it, warn = false, icon = Icons.Filled.Info) }

            NativeAdCard(style = NativeStyle.MEDIUM)

            SafetyCard()

            if (doseSteps.any { it.quantity != null }) {
                if (!test.dosesApplied) {
                    PrimaryButton("Mark chemicals added", icon = Icons.Filled.Check) {
                        vm.setDosesApplied(test.id, true)
                        AdsManager.showInterstitial(context.findActivity()) { nav.goToTab(Routes.HOME) }
                    }
                } else {
                    SecondaryButton("Undo “chemicals added”", icon = Icons.AutoMirrored.Filled.Undo) { vm.setDosesApplied(test.id, false) }
                }
            }
            SecondaryButton("Log a new test", icon = Icons.Filled.Schedule) { nav.goToTab(Routes.TEST) }

            Text(
                "Doses are estimates based on your test, pool volume and product strength. Always read the product label, add one chemical at a time, and retest after dosing.",
                style = MaterialTheme.typography.bodySmall, color = AppColors.Muted
            )
            Spacer(Modifier.height(4.dp))
        }
    }

    when (picker) {
        ProductSlot.CHLORINE -> ChoiceDialog(
            title = "Chlorine you use",
            options = Products.chlorine.map { it.id },
            selected = pool.products.chlorine,
            label = { id -> Products.chlorine(id).name },
            onDismiss = { picker = null },
            onSelect = { id -> vm.updateProducts(pool.id) { it.copy(chlorine = id) }; picker = null }
        )
        ProductSlot.ACID -> ChoiceDialog(
            title = "pH reducer you use",
            options = Products.acids.map { it.id },
            selected = pool.products.acid,
            label = { id -> Products.acid(id).name },
            onDismiss = { picker = null },
            onSelect = { id -> vm.updateProducts(pool.id) { it.copy(acid = id) }; picker = null }
        )
        ProductSlot.CALCIUM -> ChoiceDialog(
            title = "Calcium product you use",
            options = Products.calcium.map { it.id },
            selected = pool.products.calcium,
            label = { id -> Products.calcium(id).name },
            onDismiss = { picker = null },
            onSelect = { id -> vm.updateProducts(pool.id) { it.copy(calcium = id) }; picker = null }
        )
        null -> Unit
    }
}

@Composable
private fun StepCard(step: DoseStep, number: Int?, units: UnitSystem, onChange: (ProductSlot) -> Unit) {
    val highlight = step.urgent
    AppCard(border = if (highlight) AppColors.WarnBorder else if (number == 1) AppColors.Teal else AppColors.Border) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (number == null) AppColors.Track else if (number == 1) AppColors.Teal else AppColors.OkBg),
                contentAlignment = Alignment.Center
            ) {
                if (number == null) Icon(Icons.Filled.Schedule, contentDescription = null, tint = AppColors.Muted, modifier = Modifier.size(16.dp))
                else Text("$number", fontWeight = FontWeight.Bold, color = if (number == 1) Color.White else AppColors.OkText)
            }
            Spacer(Modifier.width(10.dp))
            Text(step.title, style = MaterialTheme.typography.titleSmall, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (step.urgent) Chip("URGENT", AppColors.WarnBg, AppColors.WarnText)
        }
        val q = step.quantity
        if (q != null) {
            val f = Units.amount(q, units)
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(f.main, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.Ink, lineHeight = 38.sp)
                    val sub = listOfNotNull(step.productName, f.secondary, step.extra).joinToString(" · ")
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
                }
                val slot = step.slot
                if (slot != null) {
                    TextButton(onClick = { onChange(slot) }) { Text("Change", color = AppColors.TealDark, fontWeight = FontWeight.Bold) }
                }
            }
        } else if (step.productName != null) {
            Text(step.productName, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
        }
        if (step.instructions.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Ground)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                step.instructions.forEach { Bullet(it) }
            }
        }
        if (step.effects.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                step.effects.forEach { Chip(it, AppColors.OkBg, AppColors.OkText) }
            }
        }
    }
}

@Composable
private fun BalanceCard(plan: DosingPlan) {
    AppCard {
        Text("Water balance (CSI)", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Now", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plan.csiNow?.let { Units.signed(Math.round(it * 100) / 100.0) } ?: "—", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(8.dp))
                    StatusChip(Chemistry.csiLevel(plan.csiNow))
                }
            }
            Column(Modifier.weight(1f)) {
                Text("After plan", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plan.csiAfter?.let { Units.signed(Math.round(it * 100) / 100.0) } ?: "—", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(8.dp))
                    StatusChip(Chemistry.csiLevel(plan.csiAfter))
                }
            }
        }
        Text(Chemistry.csiMeaning(plan.csiAfter ?: plan.csiNow), style = MaterialTheme.typography.bodySmall, color = AppColors.InkSoft)
        if (plan.csiNow == null) {
            Text("Add TA and calcium hardness to your tests to see this.", style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
        }
    }
}

@Composable
private fun ChlorineGuideCard(plan: DosingPlan, cya: Double?) {
    val l = plan.fcLevels
    AppCard {
        Text("Chlorine levels for CYA ${cya?.let { Units.ppm(it) } ?: "unknown"}", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LevelBox("Minimum", Units.ppm(l.min), Level.LOW)
            LevelBox("Target", Units.ppm(l.target), Level.OK)
            LevelBox("Shock (SLAM)", Units.ppm(l.slam), Level.HIGH)
        }
        Text(
            "Stabilizer protects chlorine from the sun but also slows it down, so the more CYA you have, the more chlorine you need.",
            style = MaterialTheme.typography.bodySmall, color = AppColors.Muted
        )
    }
}

@Composable
private fun LevelBox(label: String, value: String, level: Level) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = if (level == Level.OK) AppColors.TealDark else AppColors.Ink)
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
    }
}

@Composable
private fun SafetyCard() {
    AppCard(background = AppColors.WarnBg, border = AppColors.WarnBg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = AppColors.WarnText)
            Spacer(Modifier.width(8.dp))
            Text("Chemical safety", style = MaterialTheme.typography.titleSmall, color = AppColors.WarnText)
        }
        listOf(
            "Never mix chemicals — especially acid and chlorine (toxic gas).",
            "Add chemicals to water, never water to chemicals.",
            "Wear gloves and eye protection. Work upwind.",
            "Add one chemical at a time with the pump running; wait between them.",
            "Keep everyone out of the pool while dosing. Store chemicals apart, away from kids and pets."
        ).forEach { Bullet(it, AppColors.WarnText) }
    }
}

private fun planAsText(pool: Pool, plan: DosingPlan, units: UnitSystem, date: String): String {
    val sb = StringBuilder("AquaDose dosing plan — ${pool.name} (${Units.gallonsToDisplay(pool.volumeGallons, units)})\nTest: $date\n\n")
    if (plan.steps.isEmpty()) sb.append("Water is balanced — nothing to add.\n")
    var n = 0
    plan.steps.forEach { s ->
        if (s.kind != StepKind.RETEST) n++
        val prefix = if (s.kind == StepKind.RETEST) "•" else "$n."
        sb.append("$prefix ${s.title}\n")
        s.quantity?.let { q ->
            val f = Units.amount(q, units)
            sb.append("   ${f.main}${f.secondary?.let { " ($it)" } ?: ""} ${s.productName ?: ""}\n")
        }
        s.waitAfter?.let { sb.append("   → $it\n") }
    }
    plan.warnings.forEach { sb.append("\n⚠ $it") }
    sb.append("\n\n${plan.swim.message}\nNever mix chemicals. Follow product labels.")
    return sb.toString()
}
