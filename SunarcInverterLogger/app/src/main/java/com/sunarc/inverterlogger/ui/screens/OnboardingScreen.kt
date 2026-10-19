package com.sunarc.inverterlogger.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunarc.inverterlogger.domain.Countries
import com.sunarc.inverterlogger.domain.InverterBrands
import com.sunarc.inverterlogger.domain.Region
import com.sunarc.inverterlogger.ui.AppViewModel
import com.sunarc.inverterlogger.ui.brand.SunarcLogo
import com.sunarc.inverterlogger.ui.components.NumberField
import com.sunarc.inverterlogger.ui.components.PrimaryButton
import com.sunarc.inverterlogger.ui.components.Segmented
import com.sunarc.inverterlogger.ui.components.SunCard
import com.sunarc.inverterlogger.ui.components.SunChip
import com.sunarc.inverterlogger.ui.components.TextInput
import com.sunarc.inverterlogger.ui.components.parseUserNumber
import com.sunarc.inverterlogger.ui.theme.MonoFont
import com.sunarc.inverterlogger.ui.theme.Palette

val Directions = listOf("N" to 0.0, "NE" to 45.0, "E" to 90.0, "SE" to 135.0, "S" to 180.0, "SW" to 225.0, "W" to 270.0, "NW" to 315.0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(vm: AppViewModel, onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var region by remember { mutableStateOf(Region.EUROPE) }
    var countryCode by remember { mutableStateOf("DE") }
    var brandId by remember { mutableStateOf("sma") }
    var query by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("Rooftop") }
    var kwp by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var tilt by remember { mutableFloatStateOf(30f) }
    var azimuth by remember { mutableStateOf(180.0) }

    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            for (i in 0..2) {
                val c by animateColorAsState(if (i <= step) Palette.Sun else Palette.Line, tween(300), label = "step")
                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c))
            }
            Text("${step + 1} / 3", fontFamily = MonoFont, fontSize = 12.sp, color = Palette.Muted, modifier = Modifier.padding(start = 6.dp))
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(380)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(200)))
            },
            modifier = Modifier.weight(1f),
            label = "onboarding"
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 18.dp, bottom = 12.dp)) {
                when (s) {
                    0 -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SunarcLogo(size = 170.dp) }
                        Spacer(Modifier.height(12.dp))
                        Text("Your inverter,\nin your pocket.", style = MaterialTheme.typography.headlineLarge, color = Palette.Ink)
                        Spacer(Modifier.height(8.dp))
                        Text("Photograph the inverter display or a screenshot of its app. Sunarc reads the numbers, logs them and charts your daily output.", color = Palette.InkSoft, fontSize = 15.sp)
                        Spacer(Modifier.height(20.dp))
                        Feature(Icons.Rounded.DocumentScanner, "Reads 20+ inverter brands", "Labels in English, German, Dutch, French, Spanish, Italian and Polish.")
                        Feature(Icons.Rounded.CloudOff, "Works offline", "Text recognition runs on your phone. No account, no cloud, no API keys.")
                        Feature(Icons.Rounded.Insights, "Know your system", "Daily charts, savings, payback, health checks and fault-code help.")
                    }
                    1 -> {
                        Text("Where does your\nsun shine?", style = MaterialTheme.typography.headlineLarge, color = Palette.Ink)
                        Spacer(Modifier.height(6.dp))
                        Text("Sets the screen labels, units, currency and tariff rules for your country.", color = Palette.Muted, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Segmented(Region.entries, region, { it.label }, { r ->
                            region = r
                            countryCode = Countries.inRegion(r).first().code
                            if (InverterBrands.forRegion(r).none { it.id == brandId }) brandId = InverterBrands.forRegion(r).first().id
                        })
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Countries.inRegion(region).forEach { c -> SunChip(c.name, c.code == countryCode, { countryCode = c.code }) }
                        }
                        Spacer(Modifier.height(22.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Your inverter", style = MaterialTheme.typography.titleLarge, color = Palette.Ink, modifier = Modifier.weight(1f))
                            val c = Countries.byCode(countryCode)
                            Text("${c.currencyCode} · ${c.tariffModel.label.uppercase()}", fontFamily = MonoFont, fontSize = 10.sp, color = Palette.Muted, maxLines = 1)
                        }
                        Spacer(Modifier.height(10.dp))
                        TextInput("Search brand or model", query, { query = it }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        val brands = InverterBrands.forRegion(region).filter { b ->
                            query.isBlank() || b.name.contains(query, true) || b.models.any { it.contains(query, true) }
                        }
                        brands.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                row.forEach { b -> BrandTile(b.name, b.initials, b.id == brandId, Modifier.weight(1f)) { brandId = b.id } }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        val brand = InverterBrands.byId(brandId)
                        SunCard(Modifier.fillMaxWidth().padding(top = 4.dp), border = Palette.Line) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null, tint = Palette.Sky, modifier = Modifier.size(20.dp))
                                Column(Modifier.padding(start = 10.dp)) {
                                    Text("${brand.name}: ${brand.scanTarget.label.lowercase()}", fontSize = 13.sp, color = Palette.Ink, fontWeight = FontWeight.Medium)
                                    Text(brand.tip, fontSize = 12.sp, color = Palette.InkSoft)
                                }
                            }
                        }
                    }
                    else -> {
                        Text("About your\nsystem", style = MaterialTheme.typography.headlineLarge, color = Palette.Ink)
                        Spacer(Modifier.height(6.dp))
                        Text("Used for expected output, health checks and sanity checks on each scan. You can change it later.", color = Palette.Muted, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        TextInput("System name", name, { name = it }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumberField("Size", kwp, { kwp = it }, Modifier.weight(1f), suffix = "kWp")
                            TextInput("Inverter model (optional)", model, { model = it }, Modifier.weight(1.4f))
                        }
                        Text("Total panel power from your installer's paperwork, e.g. 16 × 400 W = 6.4 kWp.", fontSize = 12.sp, color = Palette.Muted, modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.height(18.dp))
                        Text("Panels face", style = MaterialTheme.typography.titleMedium, color = Palette.Ink)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Directions.forEach { (label, deg) -> SunChip(label, azimuth == deg, { azimuth = deg }) }
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tilt", style = MaterialTheme.typography.titleMedium, color = Palette.Ink, modifier = Modifier.weight(1f))
                            Text("${tilt.toInt()}°", fontFamily = MonoFont, color = Palette.SunSoft)
                        }
                        Slider(
                            value = tilt, onValueChange = { tilt = it }, valueRange = 0f..70f, steps = 13,
                            colors = SliderDefaults.colors(thumbColor = Palette.Sun, activeTrackColor = Palette.Sun, inactiveTrackColor = Palette.Line)
                        )
                        Text("Flat roof 0–15°, typical pitched roof 25–45°.", fontSize = 12.sp, color = Palette.Muted)
                    }
                }
            }
        }

        val kwpValue = parseUserNumber(kwp)
        val canFinish = kwpValue != null && kwpValue > 0 && kwpValue < 1000
        PrimaryButton(
            text = if (step < 2) "Continue" else "Start logging",
            icon = if (step < 2) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.CheckCircle,
            enabled = step < 2 || canFinish,
            onClick = {
                if (step < 2) step++ else {
                    vm.completeOnboarding(countryCode, brandId, model.trim(), kwpValue ?: 0.0, tilt.toDouble(), azimuth, name)
                    onDone()
                }
            },
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun Feature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Palette.Surface).border(1.dp, Palette.Line, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = Palette.Sun, modifier = Modifier.size(20.dp)) }
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, fontSize = 15.sp, color = Palette.Ink, fontWeight = FontWeight.Medium)
            Text(body, fontSize = 13.sp, color = Palette.InkSoft)
        }
    }
}

@Composable
fun BrandTile(name: String, initials: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val stroke by animateColorAsState(if (selected) Palette.Sun else Palette.Line, tween(200), label = "brand")
    Column(
        modifier
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Palette.SunDeep.copy(alpha = 0.6f) else Palette.Surface)
            .border(if (selected) 1.5.dp else 1.dp, stroke, RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(if (selected) Palette.Sun else Palette.SurfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, fontFamily = MonoFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (selected) Palette.OnSun else Palette.Muted)
        }
        Spacer(Modifier.height(8.dp))
        Text(name, fontSize = 12.sp, color = Palette.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
