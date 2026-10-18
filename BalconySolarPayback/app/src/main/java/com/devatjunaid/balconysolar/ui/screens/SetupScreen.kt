package com.devatjunaid.balconysolar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.devatjunaid.balconysolar.domain.Fmt
import com.devatjunaid.balconysolar.domain.SetupState
import com.devatjunaid.balconysolar.engine.Mounting
import com.devatjunaid.balconysolar.engine.PvSystem
import com.devatjunaid.balconysolar.engine.SolarPosition
import com.devatjunaid.balconysolar.engine.YieldEngine
import com.devatjunaid.balconysolar.ui.components.AppIcons
import com.devatjunaid.balconysolar.ui.components.BorderedBox
import com.devatjunaid.balconysolar.ui.components.CardLabel
import com.devatjunaid.balconysolar.ui.components.Chip
import com.devatjunaid.balconysolar.ui.components.GhostButton
import com.devatjunaid.balconysolar.ui.components.InfoBanner
import com.devatjunaid.balconysolar.ui.components.NumberEdit
import com.devatjunaid.balconysolar.ui.components.NumberEditDialog
import com.devatjunaid.balconysolar.ui.components.PrimaryButton
import com.devatjunaid.balconysolar.ui.components.ScreenHeader
import com.devatjunaid.balconysolar.ui.components.SectionCard
import com.devatjunaid.balconysolar.ui.components.Segmented
import com.devatjunaid.balconysolar.ui.components.Stepper
import com.devatjunaid.balconysolar.ui.components.ValueRow
import com.devatjunaid.balconysolar.ui.graphics.BalconyGraphic
import com.devatjunaid.balconysolar.ui.graphics.CompassGraphic
import com.devatjunaid.balconysolar.ui.graphics.TiltGauge
import com.devatjunaid.balconysolar.ui.theme.Palette
import com.devatjunaid.balconysolar.ui.theme.Type
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

enum class MeasureKind { TILT, AZIMUTH }

sealed interface TiltScanState {
    data object Idle : TiltScanState
    data object Running : TiltScanState
    data class Ready(val scan: YieldEngine.TiltScan, val forPv: PvSystem) : TiltScanState
}

private val shadingPresets = listOf(0.0 to "None", 5.0 to "Light", 15.0 to "Medium", 30.0 to "Heavy")

@Composable
fun SetupScreen(
    state: SetupState,
    scan: TiltScanState,
    onState: (SetupState) -> Unit,
    onMeasure: (MeasureKind) -> Unit,
    onScanTilt: () -> Unit,
    onBack: () -> Unit,
    onCalculate: () -> Unit,
) {
    val pv = state.pv
    fun update(p: PvSystem) = onState(state.copy(pv = p))
    var edit by remember { mutableStateOf<NumberEdit?>(null) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    val summerNoon = remember(state.latitude, state.longitude) {
        val d = if (state.southern) LocalDate.of(LocalDate.now().year, 12, 21) else LocalDate.of(LocalDate.now().year, 6, 21)
        SolarPosition.sunTimes(d, state.latitude, state.longitude, ZoneId.of(state.zoneId)).noonElevationDeg
    }

    ScreenColumn {
        ScreenHeader(step = 2, title = "Your setup", onBack = onBack)
        BorderedBox(Modifier.fillMaxWidth().height(180.dp)) {
            BalconyGraphic(pv.tiltDeg, summerNoon, pv.panelCount, Modifier.fillMaxWidth().height(180.dp))
        }

        SectionCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Panels", style = Type.bodyMedium, color = Palette.Ink)
                    Text("${Fmt.number(pv.peakW)} Wp in total", style = Type.label, color = Palette.Muted)
                }
                Stepper(pv.panelCount, { update(pv.copy(panelCount = it)) }, 1, 8, "panels")
            }
            ValueRow("Power per panel", "${Fmt.number(pv.panelWp)} Wp", onClick = {
                edit = NumberEdit("Power per panel", "Wp", pv.panelWp, 50.0, 700.0, 0, "The rated power on the panel's label (STC).") { update(pv.copy(panelWp = it)) }
            })
            ValueRow("Inverter limit", "${Fmt.number(pv.inverterAcW)} W", showDivider = false, hint = "Maximum AC output of your micro-inverter", onClick = {
                edit = NumberEdit("Inverter AC limit", "W", pv.inverterAcW, 100.0, 3000.0, 0, "Typical plug-in limit in ${state.country.name}: ${Fmt.number(state.country.inverterLimitW)} W.") { update(pv.copy(inverterAcW = it)) }
            })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard(Modifier.weight(1f), padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                CardLabel("Facing")
                CompassGraphic(pv.azimuthDeg, Modifier.fillMaxWidth().height(116.dp).padding(top = 6.dp))
                Text(
                    "${Fmt.compassLong(pv.azimuthDeg)} · ${Fmt.degrees(pv.azimuthDeg)}",
                    style = Type.stat, color = Palette.Ink, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                )
                Slider(
                    value = pv.azimuthDeg.toFloat(), onValueChange = { update(pv.copy(azimuthDeg = (it / 5f).roundToInt() * 5.0 % 360)) },
                    valueRange = 0f..355f, colors = sliderColors(),
                )
                GhostButton("Phone compass", onClick = { onMeasure(MeasureKind.AZIMUTH) }, icon = AppIcons.Compass, modifier = Modifier.fillMaxWidth())
            }
            SectionCard(Modifier.weight(1f), padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                CardLabel("Tilt from flat")
                TiltGauge(pv.tiltDeg, Modifier.fillMaxWidth().height(116.dp).padding(top = 6.dp))
                Text(Fmt.degrees(pv.tiltDeg), style = Type.stat, color = Palette.Ink, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
                Slider(
                    value = pv.tiltDeg.toFloat(), onValueChange = { update(pv.copy(tiltDeg = it.roundToInt().toDouble())) },
                    valueRange = 0f..90f, colors = sliderColors(),
                )
                GhostButton("Measure", onClick = { onMeasure(MeasureKind.TILT) }, icon = AppIcons.PhoneTilt, modifier = Modifier.fillMaxWidth())
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val towardSun = if (state.southern) 0.0 else 180.0
            Chip("Railing 90°", pv.tiltDeg == 90.0, { update(pv.copy(tiltDeg = 90.0)) }, accent = Palette.Mint, onAccent = Palette.OnMint)
            Chip("Stand 60°", pv.tiltDeg == 60.0, { update(pv.copy(tiltDeg = 60.0)) }, accent = Palette.Mint, onAccent = Palette.OnMint)
            Chip("Roof 30°", pv.tiltDeg == 30.0, { update(pv.copy(tiltDeg = 30.0)) }, accent = Palette.Mint, onAccent = Palette.OnMint)
            Chip("Flat 10°", pv.tiltDeg == 10.0, { update(pv.copy(tiltDeg = 10.0)) }, accent = Palette.Mint, onAccent = Palette.OnMint)
            for ((az, name) in listOf(towardSun to "Toward sun", 90.0 to "East", 270.0 to "West")) {
                Chip(name, pv.azimuthDeg == az, { update(pv.copy(azimuthDeg = az)) })
            }
        }

        SectionCard {
            CardLabel("Mounting (affects panel temperature)")
            Spacer(Modifier.height(8.dp))
            Segmented(Mounting.entries, pv.mounting, { it.short }, { update(pv.copy(mounting = it)) })
            Spacer(Modifier.height(14.dp))
            CardLabel("Shading from walls, trees, balcony above", trailing = "${Fmt.number(pv.shadingPct)} %")
            Spacer(Modifier.height(8.dp))
            Segmented(shadingPresets.map { it.first }, shadingPresets.map { it.first }.minByOrNull { kotlin.math.abs(it - pv.shadingPct) } ?: 0.0,
                { v -> shadingPresets.first { it.first == v }.second }, { update(pv.copy(shadingPct = it)) })
            Text(
                "Exact value",
                style = Type.labelStrong, color = Palette.Mint,
                modifier = Modifier.padding(top = 10.dp).clickable(role = Role.Button) {
                    edit = NumberEdit("Shading loss", "%", pv.shadingPct, 0.0, 90.0, 0, "Share of yearly sunlight blocked on the panel.") { update(pv.copy(shadingPct = it)) }
                }.padding(vertical = 6.dp),
            )
        }

        TiltAdvice(state, scan, onScanTilt) { update(pv.copy(tiltDeg = it)) }

        SectionCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { advanced = !advanced },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Advanced panel data", style = Type.bodyMedium, color = Palette.Ink, modifier = Modifier.weight(1f))
                Icon(AppIcons.ChevronRight, null, tint = Palette.Muted, modifier = Modifier.size(18.dp).rotate(if (advanced) 90f else 0f))
            }
            AnimatedVisibility(advanced) {
                Column {
                    ValueRow("Temperature coefficient", "${Fmt.number(pv.tempCoeffPct, 2)} %/°C", onClick = {
                        edit = NumberEdit("Temperature coefficient (Pmax)", "%/°C", pv.tempCoeffPct, -0.6, -0.2, 2, "On the datasheet; mono PERC ≈ −0.35, TOPCon ≈ −0.30, HJT ≈ −0.26.") { update(pv.copy(tempCoeffPct = it)) }
                    })
                    ValueRow("Module efficiency", "${Fmt.number(pv.moduleEfficiency * 100, 1)} %", onClick = {
                        edit = NumberEdit("Module efficiency", "%", pv.moduleEfficiency * 100, 10.0, 26.0, 1) { update(pv.copy(moduleEfficiency = it / 100)) }
                    })
                    ValueRow("Inverter efficiency", "${Fmt.number(pv.inverterEfficiency * 100, 1)} %", onClick = {
                        edit = NumberEdit("Nominal inverter efficiency", "%", pv.inverterEfficiency * 100, 85.0, 99.0, 1, "Weighted (EU/CEC) efficiency from the inverter datasheet.") { update(pv.copy(inverterEfficiency = it / 100)) }
                    })
                    ValueRow("Dirt & dust", "${Fmt.number(pv.soilingPct, 1)} %", onClick = {
                        edit = NumberEdit("Soiling loss", "%", pv.soilingPct, 0.0, 20.0, 1) { update(pv.copy(soilingPct = it)) }
                    })
                    ValueRow("Cables & mismatch", "${Fmt.number(pv.cableMismatchPct, 1)} %", onClick = {
                        edit = NumberEdit("Cable & mismatch loss", "%", pv.cableMismatchPct, 0.0, 15.0, 1) { update(pv.copy(cableMismatchPct = it)) }
                    })
                    ValueRow("Ground reflectance", Fmt.number(pv.albedo, 2), showDivider = false, hint = "0.2 typical, 0.6+ with snow or white floor", onClick = {
                        edit = NumberEdit("Albedo", "", pv.albedo, 0.0, 0.9, 2) { update(pv.copy(albedo = it)) }
                    })
                }
            }
        }

        if (pv.peakW > pv.inverterAcW) {
            InfoBanner("${Fmt.number(pv.peakW)} Wp of panels into a ${Fmt.number(pv.inverterAcW)} W inverter: on bright hours the inverter caps the output. The yield screen shows exactly how much.")
        }
        PrimaryButton("Calculate yield", onClick = onCalculate, icon = AppIcons.Bars)
    }

    edit?.let { NumberEditDialog(it) { edit = null } }
}

@Composable
private fun TiltAdvice(state: SetupState, scan: TiltScanState, onScan: () -> Unit, onApply: (Double) -> Unit) {
    SectionCard {
        CardLabel("Best tilt for this location", trailing = "full simulation")
        Spacer(Modifier.height(8.dp))
        when (scan) {
            TiltScanState.Idle -> {
                Text("Tests every tilt from 0° to 90° with your direction, panels and climate.", style = Type.label, color = Palette.Muted)
                Spacer(Modifier.height(10.dp))
                GhostButton("Find best tilt", onClick = onScan, icon = AppIcons.Spark, modifier = Modifier.fillMaxWidth())
            }
            TiltScanState.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Palette.Mint, strokeWidth = 2.dp)
                Text("  Simulating 19 tilts…", style = Type.label, color = Palette.Muted)
            }
            is TiltScanState.Ready -> {
                val s = scan.scan
                val stale = scan.forPv.copy(tiltDeg = 0.0) != state.pv.copy(tiltDeg = 0.0)
                val share = if (s.bestKwh > 0) s.currentKwh / s.bestKwh * 100 else 0.0
                Text(
                    "Best: ${Fmt.degrees(s.bestTiltDeg)} → ${Fmt.kwh(s.bestKwh)}/yr. At ${Fmt.degrees(scan.forPv.tiltDeg)} you get ${Fmt.number(share)} % of that.",
                    style = Type.body, color = Palette.Ink,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton("Use ${Fmt.degrees(s.bestTiltDeg)}", onClick = { onApply(s.bestTiltDeg) }, modifier = Modifier.weight(1f))
                    GhostButton(if (stale) "Re-run" else "Run again", onClick = onScan, modifier = Modifier.weight(1f), color = Palette.Ink)
                }
            }
        }
    }
}

@Composable
fun sliderColors() = SliderDefaults.colors(
    thumbColor = Palette.Amber,
    activeTrackColor = Palette.Amber,
    inactiveTrackColor = Palette.Line,
)
