package com.devatjunaid.balconysolar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devatjunaid.balconysolar.domain.Fmt
import com.devatjunaid.balconysolar.domain.Results
import com.devatjunaid.balconysolar.domain.SetupState
import com.devatjunaid.balconysolar.engine.Irradiance
import com.devatjunaid.balconysolar.engine.SolarPosition
import com.devatjunaid.balconysolar.ui.components.AppIcons
import com.devatjunaid.balconysolar.ui.components.BorderedBox
import com.devatjunaid.balconysolar.ui.components.CardLabel
import com.devatjunaid.balconysolar.ui.components.Chip
import com.devatjunaid.balconysolar.ui.components.RoundIconButton
import com.devatjunaid.balconysolar.ui.components.ScreenHeader
import com.devatjunaid.balconysolar.ui.components.SectionCard
import com.devatjunaid.balconysolar.ui.components.StatTile
import com.devatjunaid.balconysolar.ui.graphics.DayCurve
import com.devatjunaid.balconysolar.ui.graphics.SkyPoint
import com.devatjunaid.balconysolar.ui.graphics.SunPathChart
import com.devatjunaid.balconysolar.ui.graphics.SunPathSeries
import com.devatjunaid.balconysolar.ui.theme.Palette
import com.devatjunaid.balconysolar.ui.theme.Type
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.acos
import kotlinx.coroutines.delay

@Composable
fun SunScreen(state: SetupState, results: Results?, onBack: () -> Unit, onNext: () -> Unit) {
    val zone = remember(state.zoneId) { ZoneId.of(state.zoneId) }
    var epochDay by rememberSaveable { mutableLongStateOf(LocalDate.now(zone).toEpochDay()) }
    val date = LocalDate.ofEpochDay(epochDay)
    val year = date.year
    val summer = LocalDate.of(year, if (state.southern) 12 else 6, 21)
    val winter = LocalDate.of(year, if (state.southern) 6 else 12, 21)
    val equinox = LocalDate.of(year, 3, 20)

    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); nowMillis = System.currentTimeMillis() } }

    val lat = state.latitude
    val lon = state.longitude
    fun path(d: LocalDate): List<SkyPoint> {
        val start = d.atStartOfDay(zone).toInstant().toEpochMilli()
        return (0..144).map { i ->
            val s = SolarPosition.at(start + i * 600_000L, lat, lon)
            SkyPoint(s.azimuthDeg, s.elevationDeg)
        }
    }
    val refSeries = remember(lat, lon, year, zone) {
        listOf(
            SunPathSeries(path(winter), Palette.Sky, 3f),
            SunPathSeries(path(equinox), Palette.Mint, 3f),
            SunPathSeries(path(summer), Palette.Amber.copy(alpha = 0.55f), 3f),
        )
    }
    val selected = remember(lat, lon, epochDay, zone) { SunPathSeries(path(date), Palette.Amber, 6f) }
    val times = remember(lat, lon, epochDay, zone) { SolarPosition.sunTimes(date, lat, lon, zone) }
    val isToday = date == LocalDate.now(zone)
    val nowSun = if (isToday) SolarPosition.at(nowMillis, lat, lon) else null

    ScreenColumn {
        ScreenHeader(step = 3, title = "Sun path · ${state.custom?.label ?: state.city.name}", onBack = onBack)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(AppIcons.Back, "Previous day") { epochDay -= 1 }
            Text(Fmt.date(date), style = Type.bodyMedium, color = Palette.Ink, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            RoundIconButton(AppIcons.ChevronRight, "Next day") { epochDay += 1 }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("Today", isToday, { epochDay = LocalDate.now(zone).toEpochDay() }, Modifier.weight(1f))
            Chip("Winter", date == winter, { epochDay = winter.toEpochDay() }, Modifier.weight(1f))
            Chip("Equinox", date == equinox, { epochDay = equinox.toEpochDay() }, Modifier.weight(1f))
            Chip("Summer", date == summer, { epochDay = summer.toEpochDay() }, Modifier.weight(1f))
        }
        BorderedBox(Modifier.fillMaxWidth().height(250.dp)) {
            SunPathChart(
                series = refSeries + selected,
                facingAzimuth = state.pv.azimuthDeg,
                facingElevation = 90.0 - state.pv.tiltDeg,
                now = nowSun?.let { SkyPoint(it.azimuthDeg, it.elevationDeg) },
                southern = state.southern,
                modifier = Modifier.fillMaxWidth().height(250.dp),
                description = "Sun path for ${Fmt.date(date)}: highest ${Fmt.degrees(times.noonElevationDeg, 1)} at ${Fmt.time(times.solarNoon)}",
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Legend(Palette.Amber, "Selected day")
            Legend(Palette.Mint, "Equinox")
            Legend(Palette.Sky, "Winter")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Sunrise", Fmt.time(times.sunrise), Modifier.weight(1f))
            StatTile("Sunset", Fmt.time(times.sunset), Modifier.weight(1f))
            StatTile("Daylight", formatDuration(times.dayLength, times.polar), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Solar noon", Fmt.time(times.solarNoon), Modifier.weight(1f))
            StatTile("Highest sun", Fmt.degrees(times.noonElevationDeg, 1), Modifier.weight(1f), Palette.Amber)
        }
        if (nowSun != null && nowSun.elevationDeg > 0) {
            val cosAoi = Irradiance.cosIncidence(Math.toRadians(state.pv.tiltDeg), state.pv.azimuthDeg, Math.toRadians(90 - nowSun.trueElevationDeg), nowSun.azimuthDeg)
            val aoi = Math.toDegrees(acos(cosAoi.coerceIn(-1.0, 1.0)))
            SectionCard {
                CardLabel("Right now", trailing = Fmt.time(java.time.Instant.ofEpochMilli(nowMillis).atZone(zone)), trailingColor = Palette.Amber)
                Text(
                    if (aoi < 90) "The sun is ${Fmt.degrees(nowSun.elevationDeg, 1)} high in the ${Fmt.compassLong(nowSun.azimuthDeg).lowercase()} and hits your panel at ${Fmt.degrees(aoi)} from straight on."
                    else "The sun is ${Fmt.degrees(nowSun.elevationDeg, 1)} high but behind your panel's plane, so it only gets diffuse light now.",
                    style = Type.body, color = Palette.Ink,
                )
            }
        }
        if (results != null) {
            val m = date.monthValue - 1
            val pv = results.yield.typicalDayW[m]
            val load = results.consumption.loadDayW[m]
            val dayKwh = pv.sum() * results.yield.stepMinutes / 60.0 / 1000.0
            SectionCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                CardLabel("Typical ${Fmt.MONTHS[m]} day: output vs. your use", trailing = "${Fmt.number(dayKwh, 2)} kWh", trailingColor = Palette.Amber)
                DayCurve(pv, load, Modifier.fillMaxWidth().height(130.dp), description = "Average output on a ${Fmt.MONTHS[m]} day, ${Fmt.number(dayKwh, 2)} kWh")
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Legend(Palette.Amber, "Panels (average weather)")
                    Legend(Palette.Mint, "Household")
                }
            }
        }
        Text("NOAA solar position · Perez sky model · local time", style = Type.mono, color = Palette.Faint)
        com.devatjunaid.balconysolar.ui.components.PrimaryButton("See annual yield", onClick = onNext, icon = AppIcons.Bars)
    }
}

@Composable
fun Legend(color: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(Modifier.size(width = 14.dp, height = 4.dp)) { drawRect(color) }
        Spacer(Modifier.width(6.dp))
        Text(text, style = Type.label, color = Palette.Muted)
    }
}

private fun formatDuration(d: Duration, polar: SolarPosition.Polar): String = when (polar) {
    SolarPosition.Polar.POLAR_DAY -> "24 h"
    SolarPosition.Polar.POLAR_NIGHT -> "0 h"
    SolarPosition.Polar.NONE -> "${d.toHours()} h ${"%02d".format(d.toMinutes() % 60)}"
}
