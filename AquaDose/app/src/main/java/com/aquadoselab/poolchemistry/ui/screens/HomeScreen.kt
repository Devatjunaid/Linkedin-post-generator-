package com.aquadoselab.poolchemistry.ui.screens

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.aquadoselab.poolchemistry.ads.NativeAdCard
import com.aquadoselab.poolchemistry.ads.NativeStyle
import com.aquadoselab.poolchemistry.chem.DosingEngine
import com.aquadoselab.poolchemistry.chem.Level
import com.aquadoselab.poolchemistry.chem.Units
import com.aquadoselab.poolchemistry.chem.Volume
import com.aquadoselab.poolchemistry.data.AppData
import com.aquadoselab.poolchemistry.data.Pool
import com.aquadoselab.poolchemistry.ui.AppViewModel
import com.aquadoselab.poolchemistry.ui.Routes
import com.aquadoselab.poolchemistry.ui.components.AppCard
import com.aquadoselab.poolchemistry.ui.components.AppLogo
import com.aquadoselab.poolchemistry.ui.components.PrimaryButton
import com.aquadoselab.poolchemistry.ui.components.RangeBar
import com.aquadoselab.poolchemistry.ui.components.ScreenColumn
import com.aquadoselab.poolchemistry.ui.components.SectionTitle
import com.aquadoselab.poolchemistry.ui.components.StatusChip
import com.aquadoselab.poolchemistry.ui.components.TileSpec
import com.aquadoselab.poolchemistry.ui.components.buildTiles
import com.aquadoselab.poolchemistry.ui.components.findActivity
import com.aquadoselab.poolchemistry.ui.goToTab
import com.aquadoselab.poolchemistry.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavHostController) {
    val data by vm.data.collectAsState()
    val context = LocalContext.current
    var showExit by remember { mutableStateOf(false) }

    BackHandler { showExit = true }
    if (showExit) {
        ExitDialog(
            onStay = { showExit = false },
            onExit = { showExit = false; context.findActivity()?.finish() },
            onRate = { openPlayStore(context) }
        )
    }

    val pool = data.activePool
    if (pool == null) {
        WelcomeEmpty(onAdd = { nav.navigate(Routes.pool()) })
        return
    }
    val latest = data.testsFor(pool.id).firstOrNull()

    ScreenColumn {
        PoolHeader(data, pool, latest?.tempF, vm, nav)

        if (latest == null) {
            AppCard(background = AppColors.Navy, border = AppColors.Navy, padding = 20.dp) {
                Text("No tests yet", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Test your water with a test kit or strips, then enter the numbers. AquaDose tells you exactly what to add and how much.",
                    color = AppColors.OnNavyMuted, style = MaterialTheme.typography.bodyMedium
                )
                HeroButton("Log your first test") { nav.goToTab(Routes.TEST) }
            }
            NativeAdCard(style = NativeStyle.MEDIUM)
        } else {
            val resolved = DosingEngine.resolve(latest, data)
            val plan = DosingEngine.plan(pool, resolved)
            val tiles = buildTiles(pool, resolved, latest, data.settings.tempUnit)

            AppCard(background = AppColors.Navy, border = AppColors.Navy, padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (plan.issues > 0) AppColors.WarnDot else AppColors.AquaLight)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Tested " + DateUtils.getRelativeTimeSpanString(
                            latest.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                        ),
                        color = AppColors.OnNavyMuted, style = MaterialTheme.typography.labelMedium
                    )
                }
                val headline = when {
                    latest.dosesApplied -> "Chemicals added"
                    plan.issues == 0 -> "Water is balanced"
                    plan.issues == 1 -> "1 adjustment needed"
                    else -> "${plan.issues} adjustments needed"
                }
                Text(headline, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                val sub = when {
                    latest.dosesApplied -> "Retest in a few hours to confirm the new levels."
                    plan.issues == 0 -> "Nothing to add today. Keep testing chlorine and pH every 1–2 days."
                    else -> plan.steps.filter { it.quantity != null || it.kind.name == "DRAIN" }
                        .take(3).joinToString(" · ") { it.title }
                }
                Text(sub, color = AppColors.OnNavyMuted, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (plan.swim.safe) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (plan.swim.safe) AppColors.AquaLight else AppColors.WarnDot,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(plan.swim.message, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                HeroButton(if (plan.issues > 0 && !latest.dosesApplied) "See dosing plan" else "View full report") {
                    nav.navigate(Routes.plan(latest.id))
                }
            }

            SectionTitle("Latest readings") {
                TextButton(onClick = { nav.goToTab(Routes.HISTORY) }) { Text("History", color = AppColors.TealDark) }
            }
            tiles.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { ReadingTile(it, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            NativeAdCard(style = NativeStyle.SMALL)
        }

        PrimaryButton("Log a new test", icon = Icons.Filled.Add) { nav.goToTab(Routes.TEST) }

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Pool, contentDescription = null, tint = AppColors.Teal)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(pool.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${Units.gallonsToDisplay(Volume.gallons(pool), data.settings.units)} · ${pool.sanitizer.label} · ${pool.surface.label}",
                        style = MaterialTheme.typography.bodySmall, color = AppColors.Muted
                    )
                }
                TextButton(onClick = { nav.navigate(Routes.pool(pool.id)) }) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppColors.TealDark)
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", color = AppColors.TealDark)
                }
            }
        }

        TipCard()
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun HeroButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.AquaLight, contentColor = AppColors.Navy)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PoolHeader(data: AppData, pool: Pool, tempF: Double?, vm: AppViewModel, nav: NavHostController) {
    var menu by remember { mutableStateOf(false) }
    val date = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                date + (tempF?.let { " · " + Units.tempDisplay(it, data.settings.tempUnit) + " water" } ?: ""),
                style = MaterialTheme.typography.labelMedium, color = AppColors.Muted
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { menu = true }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    pool.name, style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
                Icon(Icons.Filled.ExpandMore, contentDescription = "Switch pool", tint = AppColors.Ink)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Color.White) {
                data.pools.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p.name, fontWeight = if (p.id == pool.id) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { vm.setActivePool(p.id); menu = false },
                        leadingIcon = {
                            Icon(
                                if (p.id == pool.id) Icons.Filled.CheckCircle else Icons.Filled.Pool,
                                contentDescription = null, tint = AppColors.Teal
                            )
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Add another pool") },
                    onClick = { menu = false; nav.navigate(Routes.pool()) },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
                )
            }
        }
        AppLogo(size = 44.dp)
    }
}

@Composable
private fun ReadingTile(t: TileSpec, modifier: Modifier = Modifier) {
    val warn = t.level == Level.LOW || t.level == Level.HIGH
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(1.5.dp, if (warn) AppColors.WarnBorder else AppColors.Border), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                t.param.label, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            StatusChip(t.level)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(t.display, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = AppColors.Ink)
            if (t.param.unit.isNotEmpty() && t.value != null) {
                Spacer(Modifier.width(4.dp))
                Text(t.param.unit, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        val band = t.band
        if (band != null) RangeBar(t.value, band.low, band.high, band.scaleMin, band.scaleMax)
        Text(
            if (t.carried) "From earlier test" else t.targetText,
            style = MaterialTheme.typography.bodySmall, color = AppColors.Muted,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

private val tips = listOf(
    "Add chlorine in the evening — the sun can burn off half of unstabilized chlorine in an hour.",
    "Always add acid to water, never water to acid. Pour slowly in front of a return jet.",
    "Never mix chemicals together, even in the same bucket. Add one, wait, then the next.",
    "Test chlorine and pH every 1–2 days in summer. Test TA, calcium and CYA every 1–2 weeks.",
    "A drop-based FAS-DPD test kit is far more accurate than strips for chlorine.",
    "Brush walls and floor weekly — algae starts in corners and on steps.",
    "Heavy rain dilutes chemicals and lowers pH. Retest after storms.",
    "Shocking only works if chlorine stays at shock level until the water is clear.",
    "Keep your pump running at least 8 hours a day so chemicals mix and filter.",
    "Too much stabilizer (CYA) weakens chlorine. Only a partial drain lowers it.",
    "Cloudy water? Check chlorine first, then filter pressure, then calcium and pH."
)

@Composable
private fun TipCard() {
    val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    AppCard(background = AppColors.OkBg, border = AppColors.OkBg) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = AppColors.TealDark, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Pool tip", style = MaterialTheme.typography.titleSmall, color = AppColors.OkText)
                Text(tips[day % tips.size], style = MaterialTheme.typography.bodyMedium, color = AppColors.InkSoft)
            }
        }
    }
}

@Composable
private fun WelcomeEmpty(onAdd: () -> Unit) {
    ScreenColumn {
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppLogo(size = 112.dp)
            Text("Welcome to AquaDose", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(
                "Clear, safe water in three steps. Tell us about your pool, enter your test results, and get exact chemical doses.",
                style = MaterialTheme.typography.bodyLarge, color = AppColors.Muted, textAlign = TextAlign.Center
            )
        }
        listOf(
            "1" to "Set up your pool — size, type and surface.",
            "2" to "Test your water and enter the readings.",
            "3" to "Follow your dosing plan, step by step."
        ).forEach { (n, s) ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(AppColors.Teal),
                        contentAlignment = Alignment.Center
                    ) { Text(n, color = Color.White, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Text(s, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        PrimaryButton("Set up my pool", icon = Icons.Filled.Pool, onClick = onAdd)
        NativeAdCard(style = NativeStyle.MEDIUM)
    }
}
