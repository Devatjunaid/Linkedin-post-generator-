package com.harrycodess.fueltracker.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.harrycodess.fueltracker.domain.Fmt
import com.harrycodess.fueltracker.domain.parseDecimal
import com.harrycodess.fueltracker.ui.AppViewModelProvider
import com.harrycodess.fueltracker.ui.components.ArcGauge
import com.harrycodess.fueltracker.ui.components.CockpitCard
import com.harrycodess.fueltracker.ui.components.DecimalField
import com.harrycodess.fueltracker.ui.components.EmptyState
import com.harrycodess.fueltracker.ui.components.Readout
import com.harrycodess.fueltracker.ui.components.ScreenTopBar
import com.harrycodess.fueltracker.ui.components.SectionTitle
import com.harrycodess.fueltracker.ui.components.budgetColor
import com.harrycodess.fueltracker.ui.theme.InstrumentLabel
import com.harrycodess.fueltracker.ui.theme.ReadoutLarge
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val shortMonthName = DateTimeFormatter.ofPattern("MMM")

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "Fuel Budget",
            subtitle = Fmt.month(state.month),
            actions = {
                if (state.budget != null) {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit budget")
                    }
                }
            },
        )

        val budget = state.budget
        if (!state.isLoading && budget == null) {
            EmptyState(
                icon = Icons.Rounded.AccountBalanceWallet,
                title = "Set a monthly fuel budget",
                message = "See how much you've spent this month, what's left per day, " +
                    "and whether you're on track.",
                action = { Button(onClick = { showDialog = true }) { Text("Set budget") } },
            )
        } else if (budget != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BudgetGaugeCard(state, budget)
                MonthlyBars(state)
                if (state.byVehicle.isNotEmpty()) {
                    VehicleBreakdown(state)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showDialog) {
        BudgetDialog(
            current = state.budget,
            currency = state.currency,
            onDismiss = { showDialog = false },
            onSave = {
                viewModel.setBudget(it)
                showDialog = false
            },
        )
    }
}

@Composable
private fun BudgetGaugeCard(state: BudgetUiState, budget: Double) {
    val currency = state.currency
    val color = budgetColor(state.fraction)
    val status = when {
        state.remaining < 0 -> "Over budget by ${Fmt.money(-state.remaining, currency)}"
        state.projected > budget -> "Heads up: on pace to spend ${Fmt.money(state.projected, currency)} this month"
        else -> "On track · ${state.daysLeft} days left this month"
    }

    CockpitCard {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ArcGauge(progress = state.fraction, color = color, modifier = Modifier.size(230.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SPENT", style = InstrumentLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Fmt.money(state.spent, currency), style = ReadoutLarge.copy(fontSize = ReadoutLarge.fontSize * 0.8f), color = color, maxLines = 1)
                    Text(
                        "of ${Fmt.money(budget, currency)} · ${(state.fraction * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Readout(
                label = "Left",
                value = Fmt.money(maxOf(state.remaining, 0.0), currency),
                icon = Icons.Rounded.AccountBalanceWallet,
                modifier = Modifier.weight(1f),
            )
            Readout(
                label = "Per day",
                value = Fmt.money(state.dailyAllowance, currency),
                icon = Icons.Rounded.Today,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Readout(
                label = "Projected",
                value = Fmt.money(state.projected, currency),
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                modifier = Modifier.weight(1f),
            )
            Readout(
                label = "Trips",
                value = state.tripCount.toString(),
                icon = Icons.Rounded.CalendarToday,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Simple bar chart of the last 6 months; bars over budget turn red. */
@Composable
private fun MonthlyBars(state: BudgetUiState) {
    val budget = state.budget ?: 0.0
    val maxValue = maxOf(state.history.maxOfOrNull { it.total } ?: 0.0, budget).coerceAtLeast(1.0)
    val chartHeight = 140.dp
    CockpitCard {
        SectionTitle("Last 6 months", trailing = "Budget ${Fmt.money(budget, state.currency)}")
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            state.history.forEach { entry ->
                val fraction = (entry.total / maxValue).toFloat()
                val isCurrent = entry.month == state.month
                val barColor = when {
                    budget > 0 && entry.total > budget -> MaterialTheme.colorScheme.error
                    isCurrent -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (entry.total > 0) Fmt.number(entry.total, 0) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(chartHeight * fraction.coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(barColor),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        entry.month.format(shortMonthName),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun VehicleBreakdown(state: BudgetUiState) {
    val total = state.spent.coerceAtLeast(0.01)
    CockpitCard {
        SectionTitle("This month by vehicle")
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.byVehicle.forEach { item ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            Fmt.money(item.total, state.currency),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (item.total / total).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetDialog(
    current: Double?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(current?.let { Fmt.input(it) }.orEmpty()) }
    val amount = parseDecimal(text)?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly fuel budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "How much do you want to spend on fuel each month?",
                    style = MaterialTheme.typography.bodyMedium,
                )
                DecimalField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Budget",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    unit = currency.trim(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = amount != null, onClick = { onSave(amount) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = { onSave(null) }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
