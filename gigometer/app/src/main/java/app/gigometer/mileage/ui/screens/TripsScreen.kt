package app.gigometer.mileage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.gigometer.mileage.Format
import app.gigometer.mileage.ads.NativeAdCard
import app.gigometer.mileage.data.AppSettings
import app.gigometer.mileage.data.Category
import app.gigometer.mileage.data.IrsRates
import app.gigometer.mileage.data.Trip
import app.gigometer.mileage.ui.components.Chip
import app.gigometer.mileage.ui.components.ScreenTitle
import app.gigometer.mileage.ui.components.SectionLabel
import app.gigometer.mileage.ui.components.TripRow
import app.gigometer.mileage.ui.components.routeTitle
import app.gigometer.mileage.ui.components.tripMeta
import app.gigometer.mileage.ui.theme.LocalPalette
import app.gigometer.mileage.ui.theme.numberStyle
import java.time.LocalDate

enum class TripFilter(val label: String) { ALL("All"), REVIEW("Needs review"), BUSINESS("Business"), PERSONAL("Personal") }

private sealed interface Row_ {
    data class Header(val day: LocalDate, val miles: Double, val deduction: Double) : Row_
    data class Item(val trip: Trip) : Row_
    data object Ad : Row_
}

@Composable
fun TripsScreen(
    trips: List<Trip>,
    settings: AppSettings,
    initialFilter: TripFilter,
    onClassify: (Trip, String) -> Unit,
    onOpenTrip: (Trip) -> Unit,
    onAddTrip: () -> Unit,
) {
    val p = LocalPalette.current
    var filter by rememberSaveable { mutableStateOf(initialFilter) }
    LaunchedEffect(initialFilter) { filter = initialFilter }
    val reviewCount = trips.count { it.category == Category.UNREVIEWED }
    val shown = trips.filter {
        when (filter) {
            TripFilter.ALL -> true
            TripFilter.REVIEW -> it.category == Category.UNREVIEWED
            TripFilter.BUSINESS -> it.category == Category.BUSINESS
            TripFilter.PERSONAL -> it.category == Category.PERSONAL
        }
    }
    val rows = buildList<Row_> {
        var count = 0
        shown.groupBy { Format.localDate(it.startTime) }.forEach { (day, list) ->
            val biz = list.filter { it.isBusiness }
            add(Row_.Header(day, list.sumOf { it.miles }, biz.sumOf { IrsRates.deduction(it, settings.rates) }))
            list.forEach {
                add(Row_.Item(it)); count++
                if (count == 3) add(Row_.Ad)
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            ScreenTitle("Trips", Modifier.weight(1f))
            FilledIconButton(
                onClick = onAddTrip, modifier = Modifier.size(44.dp), shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = p.ink, contentColor = p.ground),
            ) { Icon(Icons.Filled.Add, contentDescription = "Add trip manually") }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(TripFilter.entries) { f ->
                val label = if (f == TripFilter.REVIEW && reviewCount > 0) "Needs review · $reviewCount" else f.label
                Chip(label, selected = filter == f) { filter = f }
            }
        }
        if (rows.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (filter == TripFilter.REVIEW) "All caught up" else "No trips here yet", fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 17.sp)
                Text(
                    if (filter == TripFilter.REVIEW) "Every trip is sorted into Business or Personal." else "Drives you record show up here, grouped by day.",
                    color = p.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(rows, key = {
                when (it) { is Row_.Header -> "h${it.day}"; is Row_.Item -> "t${it.trip.id}"; Row_.Ad -> "ad" }
            }) { row ->
                when (row) {
                    is Row_.Header -> SectionLabel(
                        if (row.day == LocalDate.now()) "Today · ${Format.dayShort(row.day)}"
                        else if (row.day == LocalDate.now().minusDays(1)) "Yesterday · ${Format.dayShort(row.day)}"
                        else Format.dayLong(row.day),
                        trailing = "${Format.miles(row.miles)} mi · ${Format.money(row.deduction)}",
                    )
                    is Row_.Item -> if (row.trip.category == Category.UNREVIEWED) {
                        ReviewCard(row.trip, settings, onClassify, onOpenTrip)
                    } else {
                        Box(Modifier.clip(RoundedCornerShape(18.dp)).background(p.card)) {
                            TripRow(row.trip, settings.rates) { onOpenTrip(row.trip) }
                        }
                    }
                    Row_.Ad -> NativeAdCard()
                }
            }
        }
    }
}

/** Unclassified trip: one tap (or a swipe) to file it as Business or Personal. */
@Composable
private fun ReviewCard(trip: Trip, settings: AppSettings, onClassify: (Trip, String) -> Unit, onOpen: (Trip) -> Unit) {
    val p = LocalPalette.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> { onClassify(trip, Category.BUSINESS); true }
                SwipeToDismissBoxValue.EndToStart -> { onClassify(trip, Category.PERSONAL); true }
                else -> false
            }
        }
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val toBusiness = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(if (toBusiness) p.accent else p.neutralBar).padding(horizontal = 24.dp),
                contentAlignment = if (toBusiness) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Text(if (toBusiness) "Business" else "Personal", color = p.onAccent, fontWeight = FontWeight.ExtraBold)
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(p.card)
                .border(2.dp, p.amber, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(routeTitle(trip), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(tripMeta(trip), color = p.muted, fontSize = 12.sp)
                }
                Text("${Format.miles(trip.miles)} mi", style = numberStyle(17), color = p.ink)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onClassify(trip, Category.BUSINESS) }, modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
                ) {
                    val rate = IrsRates.rateFor(trip, settings.rates)
                    Text("Business · ${Format.money(trip.miles * rate)}", fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
                OutlinedButton(onClick = { onClassify(trip, Category.PERSONAL) }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("Personal", fontWeight = FontWeight.ExtraBold, color = p.ink)
                }
            }
            Row {
                Text("Swipe right for Business, left for Personal", color = p.muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text(
                    "Details", color = p.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onOpen(trip) }.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
