package com.superdevlabs.petfood.portioncalc.ui.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
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
import com.superdevlabs.petfood.portioncalc.ads.NativeAdCard
import com.superdevlabs.petfood.portioncalc.data.PetEntity
import com.superdevlabs.petfood.portioncalc.domain.EnergyResult
import com.superdevlabs.petfood.portioncalc.domain.Format
import com.superdevlabs.petfood.portioncalc.domain.FoodPortion
import com.superdevlabs.petfood.portioncalc.domain.NutritionCalculator
import com.superdevlabs.petfood.portioncalc.domain.WeightUnit
import com.superdevlabs.petfood.portioncalc.reminders.ReminderScheduler
import com.superdevlabs.petfood.portioncalc.ui.AppIcon
import com.superdevlabs.petfood.portioncalc.ui.AppTopBar
import com.superdevlabs.petfood.portioncalc.ui.Brand
import com.superdevlabs.petfood.portioncalc.ui.LocalAppContainer
import com.superdevlabs.petfood.portioncalc.ui.Pill
import com.superdevlabs.petfood.portioncalc.ui.PrimaryButton
import com.superdevlabs.petfood.portioncalc.ui.RealPhoto
import com.superdevlabs.petfood.portioncalc.ui.SectionCard
import com.superdevlabs.petfood.portioncalc.ui.exit.ExitDialog
import com.superdevlabs.petfood.portioncalc.ui.openPlayStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Everything the app knows about one pet for today. */
data class PetToday(
    val pet: PetEntity,
    val energy: EnergyResult,
    val portions: List<FoodPortion>,
    val fedMeals: Set<Int>,
)

data class HomeState(
    val loading: Boolean = true,
    val pets: List<PetToday> = emptyList(),
    val selectedId: Long? = null,
    val unit: WeightUnit = WeightUnit.KG,
) {
    val selected: PetToday? get() = pets.firstOrNull { it.pet.id == selectedId } ?: pets.firstOrNull()
}

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    private val today = LocalDate.now().toEpochDay()

    val state: StateFlow<HomeState> = combine(
        c.pets.observePets(),
        c.pets.observeFoods(),
        c.pets.observeFeedingOn(today),
        c.settings.selectedPetId,
        c.settings.weightUnit,
    ) { pets, foods, log, selected, unit ->
        val foodById = foods.associateBy { it.id }
        HomeState(
            loading = false,
            pets = pets.map { pet ->
                val energy = NutritionCalculator.energy(pet.toProfile())
                val portions = NutritionCalculator.portions(
                    energy.foodKcal,
                    pet.primaryFoodId?.let { foodById[it] }?.toInfo(),
                    pet.secondaryFoodId?.let { foodById[it] }?.toInfo(),
                    pet.secondarySharePercent,
                )
                PetToday(pet, energy, portions, log.filter { it.petId == pet.id }.map { it.mealIndex }.toSet())
            },
            selectedId = selected,
            unit = unit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun select(id: Long) = viewModelScope.launch { c.settings.setSelectedPet(id) }

    fun toggleMeal(petId: Long, meal: Int, fed: Boolean) = viewModelScope.launch {
        c.pets.setMealFed(petId, today, meal, fed)
    }
}

@Composable
fun HomeScreen(
    onAddPet: () -> Unit,
    onOpenPlan: (Long) -> Unit,
    onOpenWeight: (Long) -> Unit,
    onOpenTransition: (Long) -> Unit,
    onOpenFoods: () -> Unit,
    onOpenBreed: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(container) } })
    val state by vm.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    var showExit by remember { mutableStateOf(false) }

    BackHandler { showExit = true }
    if (showExit) {
        ExitDialog(
            onExit = { showExit = false; activity?.finish() },
            onRate = { showExit = false; openPlayStore(context) },
            onCancel = { showExit = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(title = "Pet Food Calculator", subtitle = "Daily portions by breed & weight") {
                IconButton(onClick = onOpenSettings) { AppIcon(R.drawable.ic_settings, "Settings") }
            }
        },
        bottomBar = { BannerAd(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) },
    ) { padding ->
        if (!state.loading && state.pets.isEmpty()) {
            EmptyHome(Modifier.padding(padding), onAddPet)
            return@Scaffold
        }
        val selected = state.selected
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.pets, key = { it.pet.id }) { item ->
                        PetCard(item, state.unit, selected = item.pet.id == selected?.pet?.id) { vm.select(item.pet.id) }
                    }
                    item { AddPetCard(onAddPet) }
                }
            }
            if (selected != null) {
                item {
                    TodayCard(selected, onToggle = { meal, fed -> vm.toggleMeal(selected.pet.id, meal, fed) }, onOpenPlan = { onOpenPlan(selected.pet.id) })
                }
                item { NativeAdCard() }
                item {
                    Text("Tools for ${selected.pet.name}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ToolTile("Portion plan", "Calories, cups & grams", R.drawable.ic_calc, Brand.Peach, Modifier.weight(1f)) { onOpenPlan(selected.pet.id) }
                            ToolTile("Weight tracker", "Log weigh-ins, see trend", R.drawable.ic_scale, Brand.MintSoft, Modifier.weight(1f)) { onOpenWeight(selected.pet.id) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ToolTile("Food switch", "7–10 day mixing schedule", R.drawable.ic_swap, Brand.Sky, Modifier.weight(1f)) { onOpenTransition(selected.pet.id) }
                            ToolTile("Food library", "Your foods' calories", R.drawable.ic_book, Brand.Butter, Modifier.weight(1f)) { onOpenFoods() }
                        }
                        ToolTile(
                            "${selected.pet.breed().name} guide",
                            "Real photo, size and feeding facts",
                            R.drawable.ic_paw, MaterialTheme.colorScheme.surface, Modifier.fillMaxWidth(),
                        ) { onOpenBreed(selected.pet.breedId) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHome(modifier: Modifier, onAddPet: () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.logo_full), contentDescription = null, modifier = Modifier.size(150.dp))
        Spacer(Modifier.height(20.dp))
        Text("How much should your pet eat?", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add your dog or cat with breed, age and weight. You'll get daily calories, exact cups and grams per meal, " +
                "a treat budget and meal reminders.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Add my pet", onAddPet, icon = R.drawable.ic_add)
    }
}

@Composable
private fun PetCard(item: PetToday, unit: WeightUnit, selected: Boolean, onClick: () -> Unit) {
    val pet = item.pet
    Card(
        onClick = onClick,
        modifier = Modifier.width(156.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Brand.Peach else MaterialTheme.colorScheme.surface),
        border = if (selected) BorderStroke(3.dp, Brand.Navy) else null,
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RealPhoto(
                wikiTitle = pet.breed().wikiTitle,
                localPhotoPath = pet.photoUri,
                contentDescription = "${pet.name}, ${pet.breed().name}",
                size = 104.dp,
            )
            Spacer(Modifier.height(8.dp))
            Text(pet.name, style = MaterialTheme.typography.titleMedium, color = if (selected) Brand.Navy else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${pet.breed().name} · ${Format.weight(pet.weightKg, unit)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) Color(0xFF5A3A20) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AddPetCard(onClick: () -> Unit) {
    Box(
        Modifier
            .width(110.dp)
            .height(196.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(26.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                AppIcon(R.drawable.ic_add, null)
            }
            Spacer(Modifier.height(8.dp))
            Text("Add pet", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TodayCard(item: PetToday, onToggle: (Int, Boolean) -> Unit, onOpenPlan: () -> Unit) {
    val pet = item.pet
    val meals = pet.mealsPerDay
    val times = pet.mealTimeMinutes()
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Today for ${pet.name}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${Format.kcal(item.energy.dailyKcal)} per day · ${item.energy.goal.label.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Pill("${item.fedMeals.count { it < meals }} of $meals fed")
        }
        LinearProgressIndicator(
            progress = { item.fedMeals.count { it < meals } / meals.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Brand.Orange,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        if (item.portions.isEmpty()) {
            Text(
                "Pick ${pet.name}'s food to turn ${Format.kcal(item.energy.foodKcal)} into cups and grams.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        for (meal in 0 until meals) {
            val fed = meal in item.fedMeals
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Checkbox) { onToggle(meal, !fed) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = fed, onCheckedChange = { onToggle(meal, it) })
                Column(Modifier.weight(1f)) {
                    Text(
                        "${ReminderScheduler.mealName(meal, meals)} · ${clock(times[meal])}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        mealAmount(item, meals),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        PrimaryButton("Open full portion plan", onOpenPlan, icon = R.drawable.ic_calc)
    }
}

@Composable
private fun ToolTile(title: String, subtitle: String, icon: Int, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
                AppIcon(icon, null, tint = Brand.Navy)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                val onTile = if (color == MaterialTheme.colorScheme.surface) MaterialTheme.colorScheme.onSurface else Brand.Navy
                Text(title, style = MaterialTheme.typography.labelLarge, color = onTile, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = onTile.copy(alpha = 0.8f), maxLines = 2)
            }
        }
    }
}

fun clock(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(h12, m, if (h < 12) "AM" else "PM")
}

fun mealAmount(item: PetToday, meals: Int): String {
    if (item.portions.isEmpty()) return "${Format.kcal(item.energy.foodKcal / meals)} this meal"
    return item.portions.joinToString(" + ") { p ->
        val g = p.gramsPerDay / meals
        val cups = p.cupsPerDay?.let { Format.cups(it / meals) }
        val units = p.unitsPerDay?.let { Format.units(it / meals, p.food.unitName ?: "unit") }
        listOfNotNull(cups ?: units, Format.grams(g)).joinToString(" · ")
    }
}
