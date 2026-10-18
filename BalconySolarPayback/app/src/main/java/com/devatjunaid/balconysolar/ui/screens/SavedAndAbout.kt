package com.devatjunaid.balconysolar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.devatjunaid.balconysolar.domain.Fmt
import com.devatjunaid.balconysolar.ui.components.AppIcons
import com.devatjunaid.balconysolar.ui.components.CardLabel
import com.devatjunaid.balconysolar.ui.components.GhostButton
import com.devatjunaid.balconysolar.ui.components.ScreenHeader
import com.devatjunaid.balconysolar.ui.components.SectionCard
import com.devatjunaid.balconysolar.ui.graphics.AnimatedLogo
import com.devatjunaid.balconysolar.ui.theme.Palette
import com.devatjunaid.balconysolar.ui.theme.Type
import java.time.Instant
import java.time.ZoneId

data class SavedSummary(
    val id: Long,
    val name: String,
    val place: String,
    val savedAt: Long,
    val peakW: Double,
    val annualKwh: Double,
    val paybackYears: Double?,
)

@Composable
fun SavedScreen(
    items: List<SavedSummary>,
    onLoad: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    nativeAd: @Composable () -> Unit,
) {
    var confirm by remember { mutableStateOf<SavedSummary?>(null) }
    ScreenColumn {
        ScreenHeader(step = null, title = "Saved setups", onBack = onBack)
        if (items.isEmpty()) {
            SectionCard {
                Text("Nothing saved yet", style = Type.bodyMedium, color = Palette.Ink)
                Text("Use “Save this setup” on the payback screen to compare balconies, panels or prices later.", style = Type.label, color = Palette.Muted)
            }
        }
        items.forEachIndexed { i, s ->
            SectionCard(Modifier.clickable(role = Role.Button) { onLoad(s.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name, style = Type.bodyMedium, color = Palette.Ink)
                        Text(
                            "${s.place} · ${Fmt.number(s.peakW)} Wp · ${Fmt.date(Instant.ofEpochMilli(s.savedAt).atZone(ZoneId.systemDefault()).toLocalDate())}",
                            style = Type.label, color = Palette.Muted,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("${Fmt.kwh(s.annualKwh)} / yr · payback ${Fmt.years(s.paybackYears)}", style = Type.mono, color = Palette.Amber)
                    }
                    IconButton(onClick = { confirm = s }) { Icon(AppIcons.Trash, contentDescription = "Delete ${s.name}", tint = Palette.Muted) }
                }
            }
            if (i == 0) nativeAd()
        }
    }
    confirm?.let { s ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Palette.SurfaceHigh,
            title = { Text("Delete “${s.name}”?", style = Type.title, color = Palette.Ink) },
            text = { Text("This removes the saved setup from this phone.", style = Type.body, color = Palette.Muted) },
            confirmButton = { TextButton(onClick = { onDelete(s.id); confirm = null }) { Text("Delete", color = Palette.Coral) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel", color = Palette.Muted) } },
        )
    }
}

@Composable
fun AboutScreen(
    versionName: String,
    privacyOptionsAvailable: Boolean,
    onPrivacyOptions: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenColumn {
        ScreenHeader(step = null, title = "How it works", onBack = onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedLogo(64.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Balcony Solar Payback", style = Type.title, color = Palette.Ink)
                Text("Version $versionName", style = Type.mono, color = Palette.Muted)
            }
        }
        SectionCard {
            CardLabel("The calculation, step by step")
            Spacer(Modifier.height(8.dp))
            for ((title, body) in methodology) {
                Text(title, style = Type.labelStrong, color = Palette.Ink, modifier = Modifier.padding(top = 8.dp))
                Text(body, style = Type.label, color = Palette.Muted)
            }
        }
        SectionCard {
            CardLabel("Data")
            Spacer(Modifier.height(6.dp))
            Text(
                "Built-in tables hold long-term monthly sunshine and temperature for 169 cities in 59 countries, plus typical tariffs, grid CO₂ and plug-in limits. " +
                    "They are approximate; for your exact coordinates use “Refine with PVGIS”, which loads satellite data from the European Commission's Joint Research Centre (PVGIS © European Union).",
                style = Type.label, color = Palette.Muted,
            )
        }
        SectionCard {
            CardLabel("Important")
            Spacer(Modifier.height(6.dp))
            Text(
                "Results are estimates for planning, not a guarantee. Real output depends on the weather of each year, shading and your equipment. " +
                    "Rules for plug-in solar differ by country and grid operator: check them, and use certified equipment, before connecting anything to your home's wiring.",
                style = Type.label, color = Palette.Muted,
            )
        }
        SectionCard {
            CardLabel("Privacy & ads")
            Spacer(Modifier.height(6.dp))
            Text(
                "Your setups stay on this phone. Location is used only when you tap “Use my location”, to find your coordinates; it is not stored on any server. " +
                    "If you refine with PVGIS, only the coordinates are sent to PVGIS. The app shows ads from Google AdMob, which may use device identifiers according to your consent choices.",
                style = Type.label, color = Palette.Muted,
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Privacy policy", onClick = onPrivacyPolicy, icon = AppIcons.Shield, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp), color = Palette.Ink)
                if (privacyOptionsAvailable) {
                    GhostButton("Ad privacy choices", onClick = onPrivacyOptions, icon = AppIcons.Sliders, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        SectionCard {
            CardLabel("Licences")
            Spacer(Modifier.height(6.dp))
            Text("Bricolage Grotesque and IBM Plex fonts: SIL Open Font License 1.1.", style = Type.label, color = Palette.Muted)
        }
    }
}

private val methodology = listOf(
    "1 · Sun position" to "NOAA solar position algorithm (Meeus): declination, equation of time, hour angle and refraction for every 15 minutes of the year at your coordinates.",
    "2 · Sunlight per time step" to "Monthly climate averages are split into sunny, average and cloudy days that keep the monthly total. Each day is shaped hour by hour (Collares-Pereira & Rabl) and split into direct and diffuse light (Erbs).",
    "3 · Light on your panel" to "Perez 1990 sky model for diffuse light, direct beam geometry for your tilt and direction, and ground reflection.",
    "4 · From light to power" to "Glass reflection (Martin & Ruiz), shading, dirt, low-light efficiency, cell temperature (PVsyst heat-loss model with your mounting) and cable losses.",
    "5 · Inverter" to "PVWatts efficiency curve and your inverter's AC limit, so clipping on bright hours is counted.",
    "6 · Self-use" to "Each time step's output is compared with an hourly household profile, with realistic load fluctuations; an optional battery is simulated step by step.",
    "7 · Payback" to "Year-by-year cash flow with panel ageing, price change, feed-in and optional discounting.",
)
