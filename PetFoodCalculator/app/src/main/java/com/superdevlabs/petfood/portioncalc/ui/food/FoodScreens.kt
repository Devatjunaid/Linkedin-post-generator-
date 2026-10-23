package com.superdevlabs.petfood.portioncalc.ui.food

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superdevlabs.petfood.portioncalc.R
import com.superdevlabs.petfood.portioncalc.ads.BannerAd
import com.superdevlabs.petfood.portioncalc.ads.NativeAdCard
import com.superdevlabs.petfood.portioncalc.data.FoodEntity
import com.superdevlabs.petfood.portioncalc.data.WikiRepository
import com.superdevlabs.petfood.portioncalc.domain.FoodType
import com.superdevlabs.petfood.portioncalc.domain.Format
import com.superdevlabs.petfood.portioncalc.domain.Species
import com.superdevlabs.petfood.portioncalc.ui.AppIcon
import com.superdevlabs.petfood.portioncalc.ui.AppTopBar
import com.superdevlabs.petfood.portioncalc.ui.LocalAppContainer
import com.superdevlabs.petfood.portioncalc.ui.Pill
import com.superdevlabs.petfood.portioncalc.ui.PrimaryButton
import com.superdevlabs.petfood.portioncalc.ui.RealPhoto
import com.superdevlabs.petfood.portioncalc.ui.SectionCard
import com.superdevlabs.petfood.portioncalc.ui.plan.foodEnergyLabel
import kotlinx.coroutines.launch

/**
 * Food library. With [petId] and [slot] it works as a picker that assigns the food to the pet.
 */
@Composable
fun FoodLibraryScreen(
    petId: Long,
    slot: String?,
    onBack: () -> Unit,
    onAddFood: (Species?) -> Unit,
    onEditFood: (Long) -> Unit,
) {
    val c = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val foods by c.pets.observeFoods().collectAsStateWithLifecycle(initialValue = emptyList())
    val pet by c.pets.observePet(petId).collectAsStateWithLifecycle(initialValue = null)
    val picking = petId > 0 && slot != null
    val species = pet?.species
    val shown = foods.filter { species == null || it.species == null || it.species == species }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = if (picking) "Choose ${if (slot == "secondary") "second" else "main"} food" else "Food library",
                subtitle = pet?.let { "for ${it.name}" },
                onBack = onBack,
            ) {
                IconButton(onClick = { onAddFood(species) }) { AppIcon(R.drawable.ic_add, "Add food") }
            }
        },
        bottomBar = { BannerAd(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 4.dp, 16.dp, padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text("Best accuracy: add your exact food", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Brands differ by up to 40% in calories. Copy \"kcal/kg\" or \"kcal per cup\" from the bag or can " +
                            "(look for \"Metabolizable energy\" or \"Calorie content\").",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    PrimaryButton("Add my food from the label", { onAddFood(species) }, icon = R.drawable.ic_add)
                }
            }
            itemsIndexed(shown, key = { _, f -> f.id }) { index, food ->
                FoodCard(
                    food = food,
                    selected = picking && (if (slot == "secondary") pet?.secondaryFoodId else pet?.primaryFoodId) == food.id,
                    onClick = {
                        val p = pet
                        if (picking && p != null) {
                            scope.launch {
                                val updated = if (slot == "secondary") {
                                    p.copy(secondaryFoodId = food.id.takeIf { it != p.primaryFoodId })
                                } else {
                                    p.copy(primaryFoodId = food.id, secondaryFoodId = p.secondaryFoodId.takeIf { it != food.id })
                                }
                                c.pets.updatePet(updated)
                                onBack()
                            }
                        } else if (!food.isPreset) {
                            onEditFood(food.id)
                        }
                    },
                    onEdit = if (!food.isPreset) ({ onEditFood(food.id) }) else null,
                )
                if (index == 3) {
                    Spacer(Modifier.height(4.dp))
                    NativeAdCard()
                }
            }
        }
    }
}

@Composable
private fun FoodCard(food: FoodEntity, selected: Boolean, onClick: () -> Unit, onEdit: (() -> Unit)?) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            RealPhoto(WikiRepository.foodTitle(food.type, food.species), null, size = 66.dp, shape = RoundedCornerShape(16.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(food.type.label)
                    if (food.isPreset) Pill("Typical", color = MaterialTheme.colorScheme.surfaceVariant, textColor = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(food.name, style = MaterialTheme.typography.titleMedium)
                Text(foodEnergyLabel(food), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            if (onEdit != null) IconButton(onClick = onEdit) { AppIcon(R.drawable.ic_edit, "Edit ${food.name}") }
        }
    }
}

private enum class EnergyBasis(val label: String) { PER_KG("kcal / kg"), PER_100G("kcal / 100 g"), PER_CUP("kcal / cup"), PER_UNIT("kcal / can or pouch") }

@Composable
fun FoodEditorScreen(foodId: Long, species: Species?, onBack: () -> Unit) {
    val c = LocalAppContainer.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(FoodType.DRY) }
    var forSpecies by rememberSaveable { mutableStateOf(species) }
    var basis by rememberSaveable { mutableStateOf(EnergyBasis.PER_KG) }
    var energyText by rememberSaveable { mutableStateOf("") }
    var cupText by rememberSaveable { mutableStateOf("") }
    var unitText by rememberSaveable { mutableStateOf("") }
    var unitName by rememberSaveable { mutableStateOf("can") }
    var existing by remember { mutableStateOf<FoodEntity?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(foodId) {
        if (foodId > 0) c.pets.getFood(foodId)?.let { f ->
            existing = f
            name = f.name; type = f.type; forSpecies = f.species
            basis = EnergyBasis.PER_100G
            energyText = Format.oneDecimal(f.kcalPer100g)
            cupText = f.gramsPerCup?.let { Format.oneDecimal(it) } ?: ""
            unitText = f.gramsPerUnit?.let { Format.oneDecimal(it) } ?: ""
            unitName = f.unitName ?: "can"
        }
    }

    val energy = Format.parseDecimal(energyText)
    val cupGrams = Format.parseDecimal(cupText)
    val unitGrams = Format.parseDecimal(unitText)
    val kcalPer100g: Double? = when (basis) {
        EnergyBasis.PER_KG -> energy?.div(10)
        EnergyBasis.PER_100G -> energy
        EnergyBasis.PER_CUP -> if (energy != null && cupGrams != null && cupGrams > 0) energy / cupGrams * 100 else null
        EnergyBasis.PER_UNIT -> if (energy != null && unitGrams != null && unitGrams > 0) energy / unitGrams * 100 else null
    }
    val energyError = when {
        energyText.isBlank() -> null
        kcalPer100g == null -> when (basis) {
            EnergyBasis.PER_CUP -> "Also enter how many grams one cup holds"
            EnergyBasis.PER_UNIT -> "Also enter the can or pouch weight"
            else -> "Enter a number"
        }
        kcalPer100g < 40 || kcalPer100g > 700 -> "That's ${kcalPer100g.toInt()} kcal/100 g, outside normal pet food (40–700). Check the unit."
        else -> null
    }
    val valid = name.isNotBlank() && kcalPer100g != null && energyError == null

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this food?") },
            text = { Text("Pets using it will need a new food choice.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    existing?.let { f -> scope.launch { c.pets.deleteFood(f); onBack() } }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(title = if (foodId > 0) "Edit food" else "Add food from label", onBack = onBack) {
                if (foodId > 0) IconButton(onClick = { confirmDelete = true }) { AppIcon(R.drawable.ic_delete, "Delete food") }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard {
                OutlinedTextField(name, { name = it.take(60) }, label = { Text("Food name (brand & recipe)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Type", style = MaterialTheme.typography.labelLarge)
                FlowChips(FoodType.entries.map { it to it.label }, type) { type = it }
                Text("For", style = MaterialTheme.typography.labelLarge)
                FlowChips(listOf<Pair<Species?, String>>(Species.DOG to "Dogs", Species.CAT to "Cats", null to "Both"), forSpecies) { forSpecies = it }
            }
            SectionCard {
                Text("Calories from the label", style = MaterialTheme.typography.titleMedium)
                FlowChips(EnergyBasis.entries.map { it to it.label }, basis) { basis = it }
                NumberField(energyText, { energyText = it }, "Calories (${basis.label})", energyError)
                NumberField(cupText, { cupText = it }, "Grams in one 8 oz cup (dry food, optional)", null, "Most kibble: 90–120 g. Weigh one level cup for accuracy.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumberField(unitText, { unitText = it }, "Can / pouch size (g, optional)", null, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(unitName, { unitName = it.take(12) }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.width(100.dp))
                }
                if (kcalPer100g != null && energyError == null) {
                    Text(
                        "= ${kcalPer100g.toInt()} kcal per 100 g" + (cupGrams?.let { " · ${(kcalPer100g * it / 100).toInt()} kcal per cup" } ?: ""),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            PrimaryButton(
                "Save food",
                enabled = valid,
                icon = R.drawable.ic_check,
                onClick = {
                    val k = kcalPer100g ?: return@PrimaryButton
                    scope.launch {
                        c.pets.saveFood(
                            FoodEntity(
                                id = existing?.id ?: 0,
                                name = name.trim(),
                                type = type,
                                species = forSpecies,
                                kcalPer100g = k,
                                gramsPerCup = cupGrams?.takeIf { it > 0 },
                                gramsPerUnit = unitGrams?.takeIf { it > 0 },
                                unitName = unitName.trim().ifBlank { null }?.takeIf { unitGrams != null },
                            ),
                        )
                        c.ads.showInterstitialThen(activity) { onBack() }
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun <T> FlowChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, error: String?, help: String? = null, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' || it == ',' }.take(7)) },
        label = { Text(label) },
        isError = error != null,
        supportingText = (error ?: help)?.let { msg -> @Composable { Text(msg) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier,
    )
}
