package app.layerline.toolkit3d.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.layerline.toolkit3d.BuildConfig
import app.layerline.toolkit3d.R
import app.layerline.toolkit3d.data.Presets
import app.layerline.toolkit3d.domain.Fmt
import app.layerline.toolkit3d.ui.ScreenContext
import app.layerline.toolkit3d.ui.ScreenScroll
import app.layerline.toolkit3d.ui.WorkshopViewModel
import app.layerline.toolkit3d.ui.components.ChamferS
import app.layerline.toolkit3d.ui.components.LButton
import app.layerline.toolkit3d.ui.components.LIcon
import app.layerline.toolkit3d.ui.components.LIcons
import app.layerline.toolkit3d.ui.components.LTextField
import app.layerline.toolkit3d.ui.components.NumberField
import app.layerline.toolkit3d.ui.components.Panel
import app.layerline.toolkit3d.ui.components.PickerDialog
import app.layerline.toolkit3d.ui.components.ScreenHeader
import app.layerline.toolkit3d.ui.components.SectionLabel
import app.layerline.toolkit3d.ui.components.pressable
import app.layerline.toolkit3d.ui.theme.LL
import app.layerline.toolkit3d.ui.theme.LLType

@Composable
fun SettingsScreen(ctx: ScreenContext) {
    val vm: WorkshopViewModel = viewModel(factory = WorkshopViewModel.Factory)
    val workshop = vm.workshop.collectAsStateWithLifecycle().value ?: return
    val privacyRequired by ctx.app.ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val policyUrl = stringResource(R.string.privacy_policy_url)

    var pickRegion by remember { mutableStateOf(false) }
    var symbol by remember { mutableStateOf(workshop.symbol) }
    var kwh by remember { mutableStateOf(Fmt.trim(workshop.kwhRate, 3)) }
    var filament by remember { mutableStateOf(Fmt.trim(workshop.filamentPerKg, 2)) }
    var labour by remember { mutableStateOf(Fmt.trim(workshop.labourPerHour, 2)) }
    var perUsd by remember { mutableStateOf(Fmt.trim(workshop.perUsd, 3)) }
    var margin by remember { mutableStateOf(Fmt.trim(workshop.marginPercent, 1)) }
    var failure by remember { mutableStateOf(Fmt.trim(workshop.failurePercent, 1)) }
    var saved by remember { mutableStateOf(false) }

    // Refresh the fields when the region (and so every default) changes.
    LaunchedEffect(workshop.regionCode, workshop.kwhRate, workshop.filamentPerKg, workshop.labourPerHour) {
        symbol = workshop.symbol
        kwh = Fmt.trim(workshop.kwhRate, 3)
        filament = Fmt.trim(workshop.filamentPerKg, 2)
        labour = Fmt.trim(workshop.labourPerHour, 2)
        perUsd = Fmt.trim(workshop.perUsd, 3)
    }

    ScreenScroll {
        ScreenHeader("; M503 — REPORT SETTINGS", "Settings", onBack = ctx.back)

        SectionLabel("; REGION")
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .pressable({ pickRegion = true })
                .border(1.dp, LL.LineStrong, ChamferS)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LIcon(LIcons.Globe, LL.Lime, size = 20.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(workshop.region.name, style = LLType.body(16.sp, FontWeight.SemiBold))
                Text("${workshop.region.currency} · picking a region resets the rates below", style = LLType.mono(11.sp, color = LL.Muted))
            }
            Text("CHANGE", style = LLType.mono(11.sp, FontWeight.SemiBold, LL.Lime))
        }

        SectionLabel("; RATES USED BY EVERY CALCULATION")
        Panel(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LTextField("Currency symbol", symbol, { symbol = it.take(5); saved = false }, Modifier.weight(1f))
                NumberField("Electricity", kwh, { kwh = it; saved = false }, Modifier.weight(1f), suffix = "/kWh")
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("PLA per kg", filament, { filament = it; saved = false }, Modifier.weight(1f))
                NumberField("Labour", labour, { labour = it; saved = false }, Modifier.weight(1f), suffix = "/h")
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Default margin", margin, { margin = it; saved = false }, Modifier.weight(1f), suffix = "%")
                NumberField("Failure buffer", failure, { failure = it; saved = false }, Modifier.weight(1f), suffix = "%")
            }
            Spacer(Modifier.height(10.dp))
            NumberField("1 USD in your currency (for printer presets)", perUsd, { perUsd = it; saved = false })
            Text(
                "Defaults are rough national averages. Check your electricity bill for the real tariff.",
                style = LLType.mono(11.sp, color = LL.Muted),
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(12.dp))
            LButton(if (saved) "Saved" else "Save rates", {
                vm.saveWorkshop(
                    workshop.copy(
                        symbol = symbol.ifBlank { workshop.symbol },
                        kwhRate = kwh.toDoubleOrNull() ?: workshop.kwhRate,
                        filamentPerKg = filament.toDoubleOrNull() ?: workshop.filamentPerKg,
                        labourPerHour = labour.toDoubleOrNull() ?: workshop.labourPerHour,
                        perUsd = perUsd.toDoubleOrNull() ?: workshop.perUsd,
                        marginPercent = margin.toDoubleOrNull() ?: workshop.marginPercent,
                        failurePercent = failure.toDoubleOrNull() ?: workshop.failurePercent,
                    ),
                )
                saved = true
            }, Modifier.fillMaxWidth(), icon = LIcons.Check)
        }

        SectionLabel("; PRIVACY")
        if (privacyRequired && activity != null) {
            SettingRow(LIcons.Shield, "Ad privacy choices", "Change how ads use your data") {
                ctx.app.ads.showPrivacyOptions(activity)
            }
        }
        SettingRow(LIcons.Info, "Privacy policy", "What the app stores and what the ad SDK collects") {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(policyUrl))) }
        }
        Text(
            "Your workshop data (printers, spools, quotes, log) is stored only on this device. " +
                "Ads are served by Google AdMob, which may use device identifiers as described in the privacy policy.",
            style = LLType.body(13.sp, color = LL.Soft),
            modifier = Modifier.padding(top = 8.dp),
        )

        SectionLabel("; ABOUT")
        SettingRow(LIcons.Wrench, "Layerline ${BuildConfig.VERSION_NAME}", "3D print toolkit · fonts: Chakra Petch, IBM Plex Mono, Familjen Grotesk (SIL OFL)") {}
    }

    if (pickRegion) {
        PickerDialog(
            code = "; M550 — REGION",
            title = "Where do you print?",
            items = Presets.regions,
            label = { "${it.name} (${it.currency})" },
            sub = { "${it.symbol.trim()}${Fmt.trim(it.kwhRate, 3)}/kWh · ${it.symbol.trim()}${Fmt.trim(it.filamentPerKg, 0)}/kg PLA" },
            isSelected = { it.code == workshop.regionCode },
            onDismiss = { pickRegion = false },
            onPick = {
                vm.setRegion(it.code)
                pickRegion = false
            },
        )
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .pressable(onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LIcon(icon, LL.Ink, size = 20.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = LLType.body(15.sp, FontWeight.Medium))
            Text(sub, style = LLType.mono(11.sp, color = LL.Muted))
        }
    }
}
