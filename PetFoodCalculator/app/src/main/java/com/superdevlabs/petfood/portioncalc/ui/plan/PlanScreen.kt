package com.superdevlabs.petfood.portioncalc.ui.plan

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.superdevlabs.petfood.portioncalc.AppContainer
import com.superdevlabs.petfood.portioncalc.R
import com.superdevlabs.petfood.portioncalc.ads.BannerAd
import com.superdevlabs.petfood.portioncalc.ads.NativeAdCard
import com.superdevlabs.petfood.portioncalc.data.FoodEntity
import com.superdevlabs.petfood.portioncalc.data.PetEntity
import com.superdevlabs.petfood.portioncalc.data.WikiRepository
import com.superdevlabs.petfood.portioncalc.domain.EnergyResult
import com.superdevlabs.petfood.portioncalc.domain.Format
import com.superdevlabs.petfood.portioncalc.domain.FoodPortion
import com.superdevlabs.petfood.portioncalc.domain.NutritionCalculator
import com.superdevlabs.petfood.portioncalc.domain.WeightGoal
import com.superdevlabs.petfood.portioncalc.domain.WeightUnit
import com.superdevlabs.petfood.portioncalc.reminders.ReminderScheduler
import com.superdevlabs.petfood.portioncalc.ui.AppIcon
import com.superdevlabs.petfood.portioncalc.ui.AppTopBar
import com.superdevlabs.petfood.portioncalc.ui.Brand
import com.superdevlabs.petfood.portioncalc.ui.LocalAppContainer
import com.superdevlabs.petfood.portioncalc.ui.Pill
import com.superdevlabs.petfood.portioncalc.ui.RealPhoto
import com.superdevlabs.petfood.portioncalc.ui.SecondaryButton
import com.superdevlabs.petfood.portioncalc.ui.SectionCard
import com.superdevlabs.petfood.portioncalc.ui.Stepper
import com.superdevlabs.petfood.portioncalc.ui.home.clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

data class PlanState(
    val pet: PetEntity? = null,
    val energy: EnergyResult? = null,
    val primary: FoodEntity? = null,
    val secondary: FoodEntity? = null,
    val portions: List<FoodPortion> = emptyList(),
    val fed: Set<Int> = emptySet(),
    val unit: WeightUnit = WeightUnit.KG,
)

class PlanViewModel(private val c: AppContainer, private val petId: Long) : ViewModel() {
    private val today = LocalDate.now().toEpochDay()

    val state: StateFlow<PlanState> = combine(
        c.pets.observePet(petId),
        c.pets.observeFoods(),
        c.pets.observeFeedingOn(today),
        c.settings.weightUnit,
    ) { pet, foods, log, unit ->
        if (pet == null) return@combine PlanState(unit = unit)
        val energy = NutritionCalculator.energy(pet.toProfile())
        val primary = foods.firstOrNull { it.id == pet.primaryFoodId }
        val secondary = foods.firstOrNull { it.id == pet.secondaryFoodId }
        PlanState(
            pet = pet,
            energy = energy,
            primary = primary,
            secondary = secondary,
            portions = NutritionCalculator.portions(energy.foodKcal, primary?.toInfo(), secondary?.toInfo(), pet.secondarySharePercent),
            fed = log.filter { it.petId == petId }.map { it.mealIndex }.toSet(),
            unit = unit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanState())

    private fun edit(transform: (PetEntity) -> PetEntity) = viewModelScope.launch {
        val pet = c.pets.getPet(petId) ?: return@launch
        val updated = transform(pet)
        c.pets.updatePet(updated)
        c.reminders.sync(updated)
    }

    fun setMeals(n: Int) = edit { p ->
        val meals = n.coerceIn(1, 4)
        p.copy(mealsPerDay = meals, mealTimes = PetEntity.defaultMealTimes(meals).joinToString(","))
    }

    fun setMealTime(index: Int, minutes: Int) = edit { p ->
        val times = p.mealTimeMinutes().toMutableList()
        times[index] = minutes
        p.copy(mealTimes = times.joinToString(","))
    }

    fun setShare(percent: Int) = edit { it.copy(secondarySharePercent = percent.coerceIn(10, 90)) }
    fun removeSecondary() = edit { it.copy(secondaryFoodId = null) }
    fun setReminders(on: Boolean) = edit { it.copy(remindersEnabled = on) }
    fun toggleFed(meal: Int, fed: Boolean) = viewModelScope.launch { c.pets.setMealFed(petId, today, meal, fed) }
}

@Composable
fun PlanScreen(
    petId: Long,
    onBack: () -> Unit,
    onEditPet: () -> Unit,
    onPickFood: (slot: String) -> Unit,
    onOpenWeight: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: PlanViewModel = viewModel(key = "plan_$petId", factory = viewModelFactory { initializer { PlanViewModel(container, petId) } })
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setReminders(granted)
    }
    val pet = s.pet
    val energy = s.energy

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(title = pet?.let { "${it.name}'s portion plan" } ?: "Portion plan", onBack = onBack) {
                IconButton(onClick = onEditPet) { AppIcon(R.drawable.ic_edit, "Edit profile") }
            }
        },
        bottomBar = { BannerAd(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) },
    ) { padding ->
        if (pet == null || energy == null) return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 4.dp, 16.dp, padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { EnergyHero(pet, energy, s.unit) }

            if (energy.goal != WeightGoal.MAINTAIN) {
                item {
                    SectionCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(R.drawable.ic_target, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(energy.goal.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Text(
                            "Target ${Format.weight(energy.targetWeightKg, s.unit)} (now ${Format.weight(pet.weightKg, s.unit)}). " +
                                "Aim for about ${Format.weight(energy.weeklyChangeKg ?: 0.0, s.unit)} per week" +
                                (energy.weeksToGoal?.let { ", roughly $it weeks." } ?: "."),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            item {
                SectionCard {
                    Text("Food", style = MaterialTheme.typography.titleMedium)
                    FoodRow(s.primary, pet, label = "Main food", onClick = { onPickFood("primary") })
                    if (s.secondary != null) {
                        FoodRow(s.secondary, pet, label = "Mixed with", onClick = { onPickFood("secondary") })
                        Text("${s.secondary!!.name.substringBefore(" (")}: ${pet.secondarySharePercent}% of meal calories", style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = pet.secondarySharePercent.toFloat(),
                            onValueChange = { vm.setShare((it / 5).roundToInt() * 5) },
                            valueRange = 10f..90f,
                            steps = 15,
                        )
                        TextButton(onClick = vm::removeSecondary) { Text("Remove second food") }
                    } else if (s.primary != null) {
                        TextButton(onClick = { onPickFood("secondary") }) { Text("+ Mix in a second food (e.g. wet + dry)") }
                    }
                }
            }

            item { PortionsCard(s, pet) }

            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Meals per day", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Stepper("${pet.mealsPerDay}", { vm.setMeals(pet.mealsPerDay - 1) }, { vm.setMeals(pet.mealsPerDay + 1) }, "Fewer meals", "More meals")
                    }
                    val times = pet.mealTimeMinutes()
                    for (meal in 0 until pet.mealsPerDay) {
                        val fed = meal in s.fed
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = fed, onCheckedChange = { vm.toggleFed(meal, it) })
                            Column(Modifier.weight(1f)) {
                                Text(ReminderScheduler.mealName(meal, pet.mealsPerDay), style = MaterialTheme.typography.labelLarge)
                                Text(perMealText(s.portions, energy, pet.mealsPerDay), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                clock(times[meal]),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(role = Role.Button) {
                                        TimePickerDialog(
                                            context,
                                            { _, h, m -> vm.setMealTime(meal, h * 60 + m) },
                                            times[meal] / 60, times[meal] % 60, DateFormat.is24HourFormat(context),
                                        ).show()
                                    }
                                    .padding(10.dp),
                            )
                        }
                    }
                    Text("Tick a meal when it's served. Tap a time to change it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item { NativeAdCard() }

            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(R.drawable.ic_bell, null)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Meal reminders", style = MaterialTheme.typography.titleMedium)
                            Text("A notification with the exact amount at each meal time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = pet.remindersEnabled,
                            onCheckedChange = { on ->
                                val needsPermission = Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                if (on && needsPermission) notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.setReminders(on)
                            },
                        )
                    }
                }
            }

            item {
                SectionCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoTile(R.drawable.ic_treat, "Treats", "up to ${Format.kcal(energy.treatKcal)}", Modifier.weight(1f))
                        InfoTile(R.drawable.ic_water, "Water", "about ${Format.ml(energy.waterMlPerDay)}", Modifier.weight(1f))
                    }
                    energy.notes.forEach { note ->
                        Row {
                            Text("•  ", color = MaterialTheme.colorScheme.secondary)
                            Text(note, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Edit profile", onEditPet, Modifier.weight(1f), icon = R.drawable.ic_edit)
                    SecondaryButton("Log weight", onOpenWeight, Modifier.weight(1f), icon = R.drawable.ic_scale)
                }
            }

            item {
                Text(
                    "Estimates use the veterinary resting-energy formula (70 × kg^0.75) with life-stage factors. Every pet is different: " +
                        "check weight every 2–4 weeks and follow your vet's advice. This app does not replace veterinary care.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EnergyHero(pet: PetEntity, energy: EnergyResult, unit: WeightUnit) {
    SectionCard(color = Brand.Peach) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DAILY ENERGY", style = MaterialTheme.typography.labelMedium, color = Brand.Navy.copy(alpha = 0.75f))
                Text(Format.kcal(energy.dailyKcal), fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Brand.Navy)
                Text(
                    "${Format.kcal(energy.foodKcal)} from meals",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Brand.Navy,
                )
            }
            RealPhoto(pet.breed().wikiTitle, "${pet.name}, ${pet.breed().name}", localPhotoPath = pet.photoUri, size = 104.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(Format.weight(pet.weightKg, unit), color = androidx.compose.ui.graphics.Color.White, textColor = Brand.Navy)
            Pill(Format.age(pet.currentAgeMonths()), color = androidx.compose.ui.graphics.Color.White, textColor = Brand.Navy)
            Pill(energy.lifeStage.label.substringBefore(" ("), color = androidx.compose.ui.graphics.Color.White, textColor = Brand.Navy)
        }
        Text(
            "How: resting energy ${energy.rerKcal.roundToInt()} kcal (for ${Format.weight(energy.factorBasisKg, unit)}) × ${energy.factor} " +
                "for ${energy.lifeStage.label.lowercase()}, ${if (pet.neutered) "neutered" else "intact"}, ${pet.activity.label.lowercase()} activity.",
            style = MaterialTheme.typography.bodySmall,
            color = Brand.Navy.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun FoodRow(food: FoodEntity?, pet: PetEntity, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RealPhoto(
            wikiTitle = WikiRepository.foodTitle(food?.type ?: com.superdevlabs.petfood.portioncalc.domain.FoodType.DRY, pet.species),
            contentDescription = null,
            size = 58.dp,
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(food?.name ?: "Choose a food", style = MaterialTheme.typography.titleMedium)
            if (food != null) {
                Text(foodEnergyLabel(food), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            } else {
                Text("Needed to convert calories into cups and grams", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
        AppIcon(R.drawable.ic_chevron, "Change")
    }
}

@Composable
private fun PortionsCard(s: PlanState, pet: PetEntity) {
    val energy = s.energy ?: return
    SectionCard(color = Brand.Navy) {
        Text("PER DAY", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color(0xFFC9D0E2))
        if (s.portions.isEmpty()) {
            Text("${Format.kcal(energy.foodKcal)} of food", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.headlineSmall)
            Text("Pick a food above to see cups and grams.", color = androidx.compose.ui.graphics.Color(0xFFC9D0E2))
            return@SectionCard
        }
        s.portions.forEach { p ->
            val amount = p.cupsPerDay?.let { Format.cups(it) } ?: p.unitsPerDay?.let { Format.units(it, p.food.unitName ?: "unit") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(amount ?: Format.grams(p.gramsPerDay), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        listOfNotNull(if (amount != null) Format.grams(p.gramsPerDay) else null, p.food.name).joinToString(" · "),
                        color = androidx.compose.ui.graphics.Color(0xFFC9D0E2),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Pill("${p.sharePercent}%", color = Brand.Orange, textColor = Brand.Navy)
            }
        }
        Text(
            "Per meal (${pet.mealsPerDay}×): ${perMealText(s.portions, energy, pet.mealsPerDay)}",
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun InfoTile(icon: Int, title: String, value: String, modifier: Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon, null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun foodEnergyLabel(food: FoodEntity): String {
    val parts = mutableListOf("${food.kcalPer100g.roundToInt()} kcal / 100 g")
    food.gramsPerCup?.let { parts += "${(food.kcalPer100g * it / 100).roundToInt()} kcal / cup" }
    food.gramsPerUnit?.let { parts += "${(food.kcalPer100g * it / 100).roundToInt()} kcal / ${food.unitName ?: "unit"}" }
    return parts.joinToString(" · ")
}

fun perMealText(portions: List<FoodPortion>, energy: EnergyResult, meals: Int): String {
    if (portions.isEmpty()) return "${Format.kcal(energy.foodKcal / meals)} per meal"
    return portions.joinToString(" + ") { p ->
        val g = p.gramsPerDay / meals
        val friendly = p.cupsPerDay?.let { Format.cups(it / meals) } ?: p.unitsPerDay?.let { Format.units(it / meals, p.food.unitName ?: "unit") }
        listOfNotNull(friendly, Format.grams(g)).joinToString(" / ")
    }
}
