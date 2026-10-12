package app.gigometer.mileage.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gigometer.mileage.Format
import app.gigometer.mileage.data.AppSettings
import app.gigometer.mileage.data.IrsRates
import app.gigometer.mileage.data.Trip
import app.gigometer.mileage.tracking.LiveTrip
import app.gigometer.mileage.tracking.TripRecorder
import app.gigometer.mileage.ui.components.Chip
import app.gigometer.mileage.ui.components.Dot
import app.gigometer.mileage.ui.components.RouteCanvas
import app.gigometer.mileage.ui.theme.numberStyle
import kotlinx.coroutines.delay
import java.time.LocalDate

private val Night = Color(0xFF14211B)
private val NightCard = Color(0xFF1F3029)
private val NightMap = Color(0xFF1C2C24)
private val NightGrid = Color(0xFF2A3D33)
private val Mist = Color(0xFFB9C6BE)
private val Mint = Color(0xFF4FD18B)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveTripScreen(
    live: LiveTrip?,
    settings: AppSettings,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(live != null) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }
    Column(Modifier.fillMaxSize().background(Night).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(380.dp)) {
            RouteCanvas(live?.points.orEmpty(), Modifier.fillMaxSize(), lineColor = Mint, gridColor = NightGrid, background = NightMap, live = true)
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Night.copy(alpha = 0.9f), contentColor = Color.White),
                    modifier = Modifier.size(44.dp),
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                if (live != null) {
                    Row(
                        Modifier.clip(CircleShape).background(if (live.paused) Mist else Color(0xFFF2B33D)).padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Dot(if (live.paused) Night else Color(0xFFB4421F))
                        Text(
                            when {
                                live.paused -> "  PAUSED"
                                live.auto -> "  RECORDING · started automatically"
                                else -> "  RECORDING"
                            },
                            color = Night, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
            Text(
                when {
                    live == null -> "Not recording"
                    !live.hasFix -> "Waiting for GPS…"
                    live.accuracyM != null -> "GPS accuracy ±${live.accuracyM.toInt()} m" + if (settings.batterySaver) " · battery saver on" else ""
                    else -> "GPS locked"
                },
                color = Color(0xFFDCE4DF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 34.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Night.copy(alpha = 0.9f)).padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .offset(y = (-18).dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Night)
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val miles = (live?.distanceMeters ?: 0.0) / Trip.METERS_PER_MILE
            val rate = IrsRates.rateOn(LocalDate.now(), settings.rates)
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Distance", color = Mist, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(Format.milesPrecise(miles), style = numberStyle(60), color = Color.White)
                        Text(" mi", style = numberStyle(22, FontWeight.Medium), color = Mist, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (live?.personal == true) "Personal" else "Deduction", color = Mist, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (live?.personal == true) "—" else Format.money(miles * rate), style = numberStyle(30), color = Mint)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LiveStat("TIME", Format.clock(if (live != null) now - live.startTime else 0), Modifier.weight(1f))
                LiveStat("SPEED", "${((live?.speedMps ?: 0f) * 2.23694f).toInt()} mph", Modifier.weight(1f))
                LiveStat("STOPS", (live?.stops ?: 0).toString(), Modifier.weight(1f))
            }
            if (live != null) {
                Column {
                    Text("Working for", color = Mist, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(bottom = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        settings.platforms.forEach { pl ->
                            Chip(pl, selected = live.platform == pl, onDark = true) { TripRecorder.choosePlatform(pl) }
                        }
                        Chip("Personal", selected = live.personal, onDark = true) { TripRecorder.choosePersonal() }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = if (live.paused) onResume else onPause,
                        modifier = Modifier.size(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2C3B33), contentColor = Color.White),
                    ) {
                        Icon(if (live.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = if (live.paused) "Resume trip" else "Pause trip")
                    }
                    Button(
                        onClick = onEnd,
                        modifier = Modifier.weight(1f).height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB4421F), contentColor = Color.White),
                    ) {
                        Icon(Icons.Filled.Stop, null)
                        Text("  End & save trip", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    }
                }
            } else {
                Text(
                    "Tap start when you begin a delivery or ride. With auto-detect on, Gigometer starts recording by itself when you drive.",
                    color = Mist, fontSize = 14.sp,
                )
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Night),
                ) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Text("  Start trip", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun LiveStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(NightCard).padding(12.dp)) {
        Text(label, color = Mist, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Text(value, style = numberStyle(20), color = Color.White, maxLines = 1)
    }
}
