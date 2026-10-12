package app.gigometer.mileage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gigometer.mileage.Format
import app.gigometer.mileage.ads.NativeAdCard
import app.gigometer.mileage.data.AppSettings
import app.gigometer.mileage.data.Trip
import app.gigometer.mileage.tracking.LiveTrip
import app.gigometer.mileage.ui.HomeState
import app.gigometer.mileage.ui.components.AppCard
import app.gigometer.mileage.ui.components.Dot
import app.gigometer.mileage.ui.components.ProgressBar
import app.gigometer.mileage.ui.components.StatTile
import app.gigometer.mileage.ui.components.TripRow
import app.gigometer.mileage.ui.theme.LocalPalette
import app.gigometer.mileage.ui.theme.numberStyle
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    state: HomeState,
    settings: AppSettings,
    live: LiveTrip?,
    autoReady: Boolean,
    onStartTrip: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenTrips: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenTrip: (Trip) -> Unit,
    onSetupAuto: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val p = LocalPalette.current
    val year = LocalDate.now().year
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Format.dayLong(LocalDate.now()), color = p.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(greeting(settings.driverName), style = numberStyle(26), color = p.ink)
            }
            val on = settings.autoDetect && autoReady
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(if (on) p.accentSoft else p.amberSoft)
                    .clickable(role = Role.Button, onClick = if (on) onOpenSettings else onSetupAuto)
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Dot(if (on) p.accent else p.amberText)
                Text(
                    if (on) "Auto-detect ON" else "Auto-detect OFF",
                    color = if (on) p.accent else p.amberText, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        if (!autoReady && settings.autoDetect) {
            AppCard(color = p.amberSoft) {
                Text("Turn on automatic tracking", fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 15.sp)
                Text(
                    "Gigometer can record every drive by itself — no tapping start. It needs location \"Allow all the time\" and Physical activity access.",
                    color = p.ink, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                )
                Button(onClick = onSetupAuto, colors = ButtonDefaults.buttonColors(containerColor = p.ink, contentColor = p.ground), shape = RoundedCornerShape(12.dp)) {
                    Text("Set up in 30 seconds", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Hero deduction card
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(p.hero).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("$year tax deduction so far", color = p.heroMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(Format.money(state.ytd.deduction), style = numberStyle(44), color = p.heroInk)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                HeroStat(Format.miles(state.ytd.miles), "business mi")
                HeroStat(state.ytd.trips.toString(), "trips")
            }
            if (state.lastYearBusinessMiles > 0) {
                val frac = (state.ytd.miles / state.lastYearBusinessMiles).toFloat()
                ProgressBar(frac, p.heroTrack, p.bright)
                Text("${(frac * 100).toInt()}% of ${year - 1}'s business miles · IRS rate ${Format.cents(state.currentRate)}/mi", color = p.heroMuted, fontSize = 12.sp)
            } else {
                Text("IRS standard rate today: ${Format.cents(state.currentRate)} per business mile", color = p.heroMuted, fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Today", Format.miles(state.today.miles), "mi · ${Format.money(state.today.deduction)}", Modifier.weight(1f))
            StatTile("This week", Format.miles(state.week.miles), "mi · ${Format.money(state.week.deduction)}", Modifier.weight(1f))
            StatTile(
                "To review", state.toReview.toString(), "unclassified", Modifier.weight(1f),
                valueColor = if (state.toReview > 0) p.amberText else null, onClick = onOpenReview,
            )
        }

        if (live != null) {
            Button(
                onClick = onOpenLive,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.amber, contentColor = Color(0xFF14211B)),
            ) {
                Icon(Icons.Filled.MyLocation, null, Modifier.size(20.dp))
                Text(
                    "  Trip in progress · ${Format.milesPrecise(live.distanceMeters / Trip.METERS_PER_MILE)} mi",
                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                )
            }
        } else {
            Button(
                onClick = onStartTrip,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
            ) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(24.dp))
                Text("  Start trip manually", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
        }

        NativeAdCard()

        Row(verticalAlignment = Alignment.Bottom) {
            Text("Recent trips", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = p.ink, modifier = Modifier.weight(1f))
            if (state.recent.isNotEmpty()) {
                Text(
                    "See all", color = p.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpenTrips).padding(8.dp),
                )
            }
        }
        if (state.recent.isEmpty()) {
            AppCard {
                Text("No trips yet", fontWeight = FontWeight.ExtraBold, color = p.ink)
                Text(
                    "Start driving with auto-detect on, or tap Start trip. Every business mile is worth ${Format.cents(state.currentRate)} off your taxable income.",
                    color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.card)) {
                state.recent.forEachIndexed { i, t ->
                    TripRow(t, settings.rates) { onOpenTrip(t) }
                    if (i < state.recent.lastIndex) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(p.line))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, color = p.heroInk, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(" $label", color = p.heroMuted, fontSize = 13.sp)
    }
}

private fun greeting(name: String): String {
    val h = LocalTime.now().hour
    val g = when { h < 12 -> "Good morning"; h < 17 -> "Good afternoon"; else -> "Good evening" }
    return if (name.isBlank()) g else "$g, ${name.substringBefore(" ")}"
}
