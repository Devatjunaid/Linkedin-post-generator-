package app.gigometer.mileage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gigometer.mileage.Format
import app.gigometer.mileage.data.AppSettings
import app.gigometer.mileage.data.Category
import app.gigometer.mileage.data.IrsRates
import app.gigometer.mileage.data.Polyline
import app.gigometer.mileage.data.Trip
import app.gigometer.mileage.ui.components.Chip
import app.gigometer.mileage.ui.components.RouteCanvas
import app.gigometer.mileage.ui.theme.LocalPalette
import app.gigometer.mileage.ui.theme.numberStyle
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** View/edit a trip, or create a manual one when [trip] has id 0. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TripSheet(
    trip: Trip,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (Trip) -> Unit,
    onDelete: (Trip) -> Unit,
) {
    val p = LocalPalette.current
    val isNew = trip.id == 0L
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var from by remember { mutableStateOf(trip.startAddress.orEmpty()) }
    var to by remember { mutableStateOf(trip.endAddress.orEmpty()) }
    var milesText by remember { mutableStateOf(if (isNew) "" else Format.milesPrecise(trip.miles)) }
    var category by remember { mutableStateOf(if (trip.category == Category.UNREVIEWED) Category.BUSINESS else trip.category) }
    var platform by remember { mutableStateOf(trip.platform) }
    var purpose by remember { mutableStateOf(trip.purpose) }
    var date by remember { mutableStateOf(Format.localDate(trip.startTime)) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val miles = milesText.replace(",", ".").toDoubleOrNull()
    val route = remember(trip.route) { Polyline.decode(trip.route) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = p.ground) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (isNew) "Add a trip" else "Trip details", style = numberStyle(24), color = p.ink)
            if (!isNew) {
                Text(
                    "${Format.date(trip.startTime)} · ${Format.time(trip.startTime)}" +
                        (trip.endTime?.let { " – ${Format.time(it)} · ${Format.duration(trip.durationMs)}" } ?: "") +
                        if (trip.autoDetected) " · auto-recorded" else "",
                    color = p.muted, fontSize = 13.sp,
                )
            }
            if (route.size >= 2) {
                RouteCanvas(
                    route, Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp)),
                    lineColor = p.accent, gridColor = p.line, background = p.card,
                )
            }
            if (isNew) {
                OutlinedButton(onClick = { pickDate = true }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Date: ${Format.date(date)}", color = p.ink, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedTextField(from, { from = it }, label = { Text("From") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(to, { to = it }, label = { Text("To") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                milesText, { milesText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(7) },
                label = { Text("Miles") }, singleLine = true, isError = milesText.isNotEmpty() && miles == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = if (!isNew && !trip.isManual) ({ Text("Measured by GPS — edit only to correct it.") }) else null,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Type", fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Business", category == Category.BUSINESS) { category = Category.BUSINESS }
                Chip("Personal", category == Category.PERSONAL) { category = Category.PERSONAL; platform = null }
            }
            if (category == Category.BUSINESS) {
                Text("Platform", fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    settings.platforms.forEach { pl -> Chip(pl, platform == pl) { platform = if (platform == pl) null else pl } }
                }
                OutlinedTextField(
                    purpose, { purpose = it.take(120) }, label = { Text("Business purpose (optional)") },
                    placeholder = { Text("e.g. DoorDash deliveries, Midtown zone") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                if (miles != null) {
                    val rate = IrsRates.rateOn(date, settings.rates)
                    Text("Deduction: ${Format.money(miles * rate)}  (${Format.milesPrecise(miles)} mi × ${Format.rate(rate)})", color = p.accent, fontWeight = FontWeight.ExtraBold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isNew) {
                    OutlinedButton(onClick = { confirmDelete = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.height(52.dp)) {
                        Text("Delete", color = p.danger, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Button(
                    enabled = miles != null && miles > 0,
                    onClick = {
                        val unchanged = !isNew && milesText == Format.milesPrecise(trip.miles)
                        val meters = if (unchanged) trip.distanceMeters else (miles ?: 0.0) * Trip.METERS_PER_MILE
                        val start = if (isNew) date.atTime(LocalTime.NOON).atZone(Format.zone).toInstant().toEpochMilli() else trip.startTime
                        onSave(
                            trip.copy(
                                startTime = start,
                                endTime = trip.endTime ?: start,
                                distanceMeters = meters,
                                startAddress = from.trim().ifBlank { null },
                                endAddress = to.trim().ifBlank { null },
                                category = category,
                                platform = if (category == Category.BUSINESS) platform else null,
                                purpose = purpose.trim(),
                                isManual = trip.isManual || isNew,
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(if (isNew) "Add trip" else "Save", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
            }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this trip?") },
            text = { Text("It will be removed from your mileage log and reports.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(trip) }) { Text("Delete", color = p.danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

fun newManualTrip(): Trip {
    val noon = LocalDate.now().atTime(LocalTime.NOON).atZone(Format.zone).toInstant().toEpochMilli()
    return Trip(startTime = noon, endTime = noon, isManual = true, category = Category.BUSINESS)
}
