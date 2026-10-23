package com.superdevlabs.petfood.portioncalc.ui.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.superdevlabs.petfood.portioncalc.AppContainer
import com.superdevlabs.petfood.portioncalc.R
import com.superdevlabs.petfood.portioncalc.ads.BannerAd
import com.superdevlabs.petfood.portioncalc.data.PetEntity
import com.superdevlabs.petfood.portioncalc.data.WeightLogEntity
import com.superdevlabs.petfood.portioncalc.domain.Format
import com.superdevlabs.petfood.portioncalc.domain.NutritionCalculator
import com.superdevlabs.petfood.portioncalc.domain.WeightUnit
import com.superdevlabs.petfood.portioncalc.ui.AppIcon
import com.superdevlabs.petfood.portioncalc.ui.AppTopBar
import com.superdevlabs.petfood.portioncalc.ui.Brand
import com.superdevlabs.petfood.portioncalc.ui.LocalAppContainer
import com.superdevlabs.petfood.portioncalc.ui.PrimaryButton
import com.superdevlabs.petfood.portioncalc.ui.SectionCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class WeightState(val pet: PetEntity? = null, val entries: List<WeightLogEntity> = emptyList(), val unit: WeightUnit = WeightUnit.KG)

class WeightViewModel(private val c: AppContainer, private val petId: Long) : ViewModel() {
    val state: StateFlow<WeightState> = combine(
        c.pets.observePet(petId), c.pets.observeWeights(petId), c.settings.weightUnit,
    ) { pet, entries, unit -> WeightState(pet, entries, unit) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightState())

    fun add(kg: Double, onDone: () -> Unit) = viewModelScope.launch {
        c.pets.addWeight(petId, LocalDate.now().toEpochDay(), kg)
        c.pets.getPet(petId)?.let { c.reminders.sync(it) }
        onDone()
    }

    fun delete(entry: WeightLogEntity) = viewModelScope.launch { c.pets.deleteWeight(entry) }
}

@Composable
fun WeightScreen(petId: Long, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: WeightViewModel = viewModel(key = "weight_$petId", factory = viewModelFactory { initializer { WeightViewModel(container, petId) } })
    val s by vm.state.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val pet = s.pet
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Weight tracker", subtitle = pet?.name, onBack = onBack) },
        bottomBar = { BannerAd(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) },
    ) { padding ->
        if (pet == null) return@Scaffold
        val energy = NutritionCalculator.energy(pet.toProfile())
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 4.dp, 16.dp, padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SectionCard {
                    Text("Now ${Format.weight(pet.weightKg, s.unit)}", style = MaterialTheme.typography.headlineSmall)
                    val first = s.entries.firstOrNull()
                    if (first != null && s.entries.size > 1) {
                        val days = (s.entries.last().epochDay - first.epochDay).coerceAtLeast(1)
                        Text(
                            "${Format.signedWeight(pet.weightKg - first.weightKg, s.unit)} over $days days",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "Target ${Format.weight(energy.targetWeightKg, s.unit)} · ${energy.goal.label}",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    WeightChart(s.entries, energy.targetWeightKg, s.unit)
                }
            }
            item {
                SectionCard {
                    Text("Add today's weigh-in", style = MaterialTheme.typography.titleMedium)
                    val kg = Format.parseDecimal(input)?.let { Format.unitToKg(it, s.unit) }
                    val valid = kg != null && kg in 0.3..120.0
                    OutlinedTextField(
                        value = input,
                        onValueChange = { v -> input = v.filter { it.isDigit() || it == '.' || it == ',' }.take(6) },
                        label = { Text("Weight (${s.unit.label})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = input.isNotEmpty() && !valid,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PrimaryButton("Save weight & update portions", enabled = valid, icon = R.drawable.ic_check, onClick = {
                        if (kg != null) vm.add(kg) { input = "" }
                    })
                    Text("Weigh at the same time of day, before a meal, for comparable numbers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Text("History", style = MaterialTheme.typography.titleMedium) }
            items(s.entries.reversed(), key = { it.id }) { entry ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Format.weight(entry.weightKg, s.unit), style = MaterialTheme.typography.titleMedium)
                        Text(LocalDate.ofEpochDay(entry.epochDay).format(dateFormat), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (s.entries.size > 1) {
                        IconButton(onClick = { vm.delete(entry) }) { AppIcon(R.drawable.ic_delete, "Delete entry") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightChart(entries: List<WeightLogEntity>, targetKg: Double, unit: WeightUnit) {
    if (entries.size < 2) {
        Text("Add another weigh-in to see the trend line.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val line = Brand.Orange
    val targetColor = MaterialTheme.colorScheme.secondary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val values = entries.map { it.weightKg }
    val min = minOf(values.min(), targetKg) * 0.97
    val max = maxOf(values.max(), targetKg) * 1.03
    val firstDay = entries.first().epochDay
    val span = (entries.last().epochDay - firstDay).coerceAtLeast(1).toFloat()
    val description = "Weight chart from ${Format.weight(values.first(), unit)} to ${Format.weight(values.last(), unit)}"
    Canvas(Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp).semantics { contentDescription = description }) {
        fun y(v: Double) = size.height - ((v - min) / (max - min)).toFloat() * size.height
        for (i in 0..3) {
            val gy = size.height * i / 3f
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx())
        }
        drawLine(
            targetColor, Offset(0f, y(targetKg)), Offset(size.width, y(targetKg)),
            strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
        )
        val path = Path()
        entries.forEachIndexed { i, e ->
            val x = (e.epochDay - firstDay) / span * size.width
            if (i == 0) path.moveTo(x, y(e.weightKg)) else path.lineTo(x, y(e.weightKg))
        }
        drawPath(path, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        entries.forEach { e ->
            drawCircle(line, radius = 4.dp.toPx(), center = Offset((e.epochDay - firstDay) / span * size.width, y(e.weightKg)))
        }
    }
}
