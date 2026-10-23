package com.superdevlabs.petfood.portioncalc.ui.transition

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superdevlabs.petfood.portioncalc.R
import com.superdevlabs.petfood.portioncalc.ads.BannerAd
import com.superdevlabs.petfood.portioncalc.ads.NativeAdCard
import com.superdevlabs.petfood.portioncalc.data.FoodEntity
import com.superdevlabs.petfood.portioncalc.domain.Format
import com.superdevlabs.petfood.portioncalc.domain.NutritionCalculator
import com.superdevlabs.petfood.portioncalc.domain.TransitionDay
import com.superdevlabs.petfood.portioncalc.domain.TransitionPlanner
import com.superdevlabs.petfood.portioncalc.ui.AppTopBar
import com.superdevlabs.petfood.portioncalc.ui.LocalAppContainer
import com.superdevlabs.petfood.portioncalc.ui.Pill
import com.superdevlabs.petfood.portioncalc.ui.PrimaryButton
import com.superdevlabs.petfood.portioncalc.ui.SecondaryButton
import com.superdevlabs.petfood.portioncalc.ui.SectionCard

/** Plans a gradual switch from the current food to a new one to avoid stomach upsets. */
@Composable
fun TransitionScreen(petId: Long, onBack: () -> Unit) {
    val c = LocalAppContainer.current
    val activity = LocalActivity.current
    val pet by c.pets.observePet(petId).collectAsStateWithLifecycle(initialValue = null)
    val foods by c.pets.observeFoods().collectAsStateWithLifecycle(initialValue = emptyList())
    var oldFoodId by remember { mutableStateOf<Long?>(null) }
    var newFoodId by remember { mutableStateOf<Long?>(null) }
    var days by remember { mutableIntStateOf(7) }
    var plan by remember { mutableStateOf<List<TransitionDay>>(emptyList()) }

    val p = pet
    val suitable = foods.filter { p == null || it.species == null || it.species == p.species }
    val oldFood = suitable.firstOrNull { it.id == (oldFoodId ?: p?.primaryFoodId) }
    val newFood = suitable.firstOrNull { it.id == newFoodId }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Food switch planner", subtitle = p?.name, onBack = onBack) },
        bottomBar = { BannerAd(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) },
    ) { padding ->
        if (p == null) return@Scaffold
        val energy = NutritionCalculator.energy(p.toProfile())
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 4.dp, 16.dp, padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SectionCard {
                    Text("Switch food without tummy trouble", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Mix the new food in gradually. Amounts keep ${p.name} on ${Format.kcal(energy.foodKcal)} a day from meals.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FoodChooser("Current food", oldFood, suitable) { oldFoodId = it; plan = emptyList() }
                    FoodChooser("New food", newFood, suitable) { newFoodId = it; plan = emptyList() }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7 to "7 days", 10 to "10 days (sensitive stomach)").forEach { (d, label) ->
                            FilterChip(selected = days == d, onClick = { days = d; plan = emptyList() }, label = { Text(label) })
                        }
                    }
                    PrimaryButton(
                        "Create schedule",
                        enabled = oldFood != null && newFood != null && oldFood.id != newFood.id,
                        icon = R.drawable.ic_swap,
                        onClick = {
                            if (oldFood != null && newFood != null) {
                                val result = TransitionPlanner.plan(days, energy.foodKcal, oldFood.toInfo(), newFood.toInfo())
                                c.ads.showInterstitialThen(activity) { plan = result }
                            }
                        },
                    )
                }
            }
            if (plan.isNotEmpty() && oldFood != null && newFood != null) {
                item { NativeAdCard(showMedia = false) }
                item { Text("Daily amounts (split across ${p.mealsPerDay} meals)", style = MaterialTheme.typography.titleMedium) }
                items(plan, key = { it.day }) { d ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Day ${d.day}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Pill("${100 - d.newPercent}% old · ${d.newPercent}% new")
                        }
                        if (d.oldGrams > 0.5) Text("${amount(d.oldGrams, oldFood)}  ${oldFood.name}", style = MaterialTheme.typography.bodyMedium)
                        Text("${amount(d.newGrams, newFood)}  ${newFood.name}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item {
                    Text(
                        "If stools get loose, stay on the current step for 2–3 more days before moving on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun amount(grams: Double, food: FoodEntity): String {
    val cups = food.gramsPerCup?.let { Format.cups(grams / it) }
    val units = food.gramsPerUnit?.let { Format.units(grams / it, food.unitName ?: "unit") }
    return listOfNotNull(Format.grams(grams), cups ?: units).joinToString(" · ")
}

@Composable
private fun FoodChooser(label: String, selected: FoodEntity?, foods: List<FoodEntity>, onSelect: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        SecondaryButton(selected?.name ?: "Choose…", onClick = { open = true }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            foods.forEach { f ->
                DropdownMenuItem(text = { Text(f.name) }, onClick = { open = false; onSelect(f.id) })
            }
        }
    }
}
