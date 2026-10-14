package com.harrycodess.fueltracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.harrycodess.fueltracker.domain.FuelMath
import com.harrycodess.fueltracker.domain.Fmt
import com.harrycodess.fueltracker.domain.TripEstimate
import com.harrycodess.fueltracker.domain.UnitPrefs
import com.harrycodess.fueltracker.ui.theme.InstrumentLabel
import com.harrycodess.fueltracker.ui.theme.ReadoutLarge

/** Compact cost panel (used on the Edit trip screen). */
@Composable
fun ResultCard(
    estimate: TripEstimate?,
    units: UnitPrefs,
    currency: String,
    modifier: Modifier = Modifier,
    title: String = "Estimated trip cost",
) {
    CockpitCard(modifier = modifier) {
        Text(title.uppercase(), style = InstrumentLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AnimatedContent(
            targetState = estimate?.let { Fmt.money(it.totalCost, currency) } ?: "—",
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "cost",
        ) { text ->
            Text(text, style = ReadoutLarge, color = MaterialTheme.colorScheme.primary)
        }
        if (estimate == null) {
            Text(
                "Enter distance, fuel price and efficiency to see the cost.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Readout(
                    label = "Fuel needed",
                    value = "${Fmt.number(FuelMath.litersToVolume(estimate.fuelLiters, units.system))} ${units.volumeUnit}",
                    icon = Icons.Rounded.LocalGasStation,
                    modifier = Modifier.weight(1f),
                )
                Readout(
                    label = "Per ${units.distanceUnit}",
                    value = Fmt.money(FuelMath.costPerDistance(estimate.costPerKm, units.system), currency),
                    icon = Icons.Rounded.Straighten,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
