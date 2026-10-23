package com.superdevlabs.petfood.portioncalc.ui.pet

import android.app.Application
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.superdevlabs.petfood.portioncalc.data.PetEntity
import com.superdevlabs.petfood.portioncalc.domain.ActivityLevel
import com.superdevlabs.petfood.portioncalc.domain.BodyCondition
import com.superdevlabs.petfood.portioncalc.domain.Breed
import com.superdevlabs.petfood.portioncalc.domain.Breeds
import com.superdevlabs.petfood.portioncalc.domain.Format
import com.superdevlabs.petfood.portioncalc.domain.Species
import com.superdevlabs.petfood.portioncalc.domain.WeightUnit
import com.superdevlabs.petfood.portioncalc.ui.AppIcon
import com.superdevlabs.petfood.portioncalc.ui.AppTopBar
import com.superdevlabs.petfood.portioncalc.ui.LocalAppContainer
import com.superdevlabs.petfood.portioncalc.ui.PrimaryButton
import com.superdevlabs.petfood.portioncalc.ui.RealPhoto
import com.superdevlabs.petfood.portioncalc.ui.SecondaryButton
import com.superdevlabs.petfood.portioncalc.ui.SectionCard
import com.superdevlabs.petfood.portioncalc.ui.Stepper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

data class PetForm(
    val id: Long = 0,
    val name: String = "",
    val species: Species = Species.DOG,
    val breed: Breed = Breeds.defaultFor(Species.DOG),
    val photoPath: String? = null,
    val years: Int = 3,
    val months: Int = 0,
    val weightText: String = "",
    val targetText: String = "",
    val neutered: Boolean = true,
    val activity: ActivityLevel = ActivityLevel.NORMAL,
    val bodyCondition: Int = 5,
    val mealsPerDay: Int = 2,
    val treatPercent: Int = 10,
) {
    val ageMonths get() = years * 12 + months
}

class PetEditorViewModel(private val c: AppContainer, private val app: Application, petId: Long) : ViewModel() {
    var form by mutableStateOf(PetForm())
        private set
    var unit by mutableStateOf(WeightUnit.KG)
        private set
    var loaded by mutableStateOf(petId <= 0)
        private set
    private var original: PetEntity? = null

    init {
        viewModelScope.launch {
            unit = c.settings.weightUnit.first()
            if (petId > 0) {
                c.pets.getPet(petId)?.let { pet ->
                    original = pet
                    val age = pet.currentAgeMonths()
                    form = PetForm(
                        id = pet.id, name = pet.name, species = pet.species, breed = pet.breed(), photoPath = pet.photoUri,
                        years = age / 12, months = age % 12,
                        weightText = Format.oneDecimal(Format.kgToUnit(pet.weightKg, unit)),
                        targetText = pet.targetWeightKg?.let { Format.oneDecimal(Format.kgToUnit(it, unit)) } ?: "",
                        neutered = pet.neutered, activity = pet.activity, bodyCondition = pet.bodyCondition,
                        mealsPerDay = pet.mealsPerDay, treatPercent = pet.treatPercent,
                    )
                }
                loaded = true
            }
        }
    }

    fun update(transform: (PetForm) -> PetForm) {
        form = transform(form)
    }

    fun setSpecies(species: Species) {
        if (species != form.species) form = form.copy(species = species, breed = Breeds.defaultFor(species))
    }

    fun weightKg(): Double? = Format.parseDecimal(form.weightText)?.let { Format.unitToKg(it, unit) }
    fun targetKg(): Double? = Format.parseDecimal(form.targetText)?.let { Format.unitToKg(it, unit) }

    fun weightError(): String? {
        val kg = weightKg() ?: return "Enter the current weight"
        return if (kg < 0.3 || kg > 120) "Enter a weight between ${Format.weight(0.3, unit)} and ${Format.weight(120.0, unit)}" else null
    }

    fun targetError(): String? {
        if (form.targetText.isBlank()) return null
        val kg = targetKg() ?: return "Enter a number or leave empty"
        return if (kg < 0.3 || kg > 120) "Target looks wrong" else null
    }

    fun canSave() = form.name.isNotBlank() && weightError() == null && targetError() == null

    /** Copies the picked picture into app storage so it never depends on another app's permission. */
    fun setPhoto(uri: Uri) = viewModelScope.launch {
        val copied = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(app.filesDir, "pet_photos").apply { mkdirs() }
                val out = File(dir, "pet_${System.currentTimeMillis()}.jpg")
                app.contentResolver.openInputStream(uri)?.use { input -> out.outputStream().use { input.copyTo(it) } }
                out.takeIf { it.length() > 0 }?.absolutePath
            }.getOrNull()
        }
        if (copied != null) {
            discardUnsavedPhoto()
            form = form.copy(photoPath = copied)
        }
    }

    fun removePhoto() {
        discardUnsavedPhoto()
        form = form.copy(photoPath = null)
    }

    private fun discardUnsavedPhoto() {
        val current = form.photoPath ?: return
        if (current != original?.photoUri) File(current).delete()
    }

    fun save(onSaved: (Long) -> Unit) = viewModelScope.launch {
        val f = form
        val kg = weightKg() ?: return@launch
        val base = original ?: PetEntity(name = f.name.trim(), species = f.species, breedId = f.breed.id, ageMonths = f.ageMonths, weightKg = kg)
        val pet = base.copy(
            name = f.name.trim(),
            species = f.species,
            breedId = f.breed.id,
            photoUri = f.photoPath,
            ageMonths = f.ageMonths,
            ageRecordedEpochDay = java.time.LocalDate.now().toEpochDay(),
            weightKg = (kg * 100).roundToInt() / 100.0,
            targetWeightKg = targetKg(),
            neutered = f.neutered,
            activity = f.activity,
            bodyCondition = f.bodyCondition,
            mealsPerDay = f.mealsPerDay,
            treatPercent = f.treatPercent,
            // Foods of the other species don't make sense after a species change.
            primaryFoodId = if (original?.species != null && original?.species != f.species) null else base.primaryFoodId,
            secondaryFoodId = if (original?.species != null && original?.species != f.species) null else base.secondaryFoodId,
        )
        val id = c.pets.savePet(pet)
        original?.photoUri?.let { old -> if (old != pet.photoUri) File(old).delete() }
        c.settings.setSelectedPet(id)
        c.pets.getPet(id)?.let { c.reminders.sync(it) }
        onSaved(id)
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        val pet = original ?: return@launch
        c.reminders.cancelAll(pet.id)
        c.pets.deletePet(pet)
        pet.photoUri?.let { File(it).delete() }
        onDeleted()
    }
}

@Composable
fun PetEditorScreen(petId: Long, onBack: () -> Unit, onSaved: (Long) -> Unit, onDeleted: () -> Unit) {
    val container = LocalAppContainer.current
    val activity = LocalActivity.current
    val app = activity?.application ?: return
    val vm: PetEditorViewModel = viewModel(
        key = "pet_editor_$petId",
        factory = viewModelFactory { initializer { PetEditorViewModel(container, app, petId) } },
    )
    val form = vm.form
    var showBreeds by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setPhoto(uri)
    }

    if (showBreeds) {
        BreedPickerDialog(form.species, onPick = { vm.update { f -> f.copy(breed = it) }; showBreeds = false }, onDismiss = { showBreeds = false })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${form.name}?") },
            text = { Text("The profile, weight history and feeding log will be removed from this phone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(onDeleted) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(title = if (petId > 0) "Edit ${form.name}" else "Add a pet", onBack = onBack) {
                if (petId > 0) IconButton(onClick = { confirmDelete = true }) { AppIcon(R.drawable.ic_delete, "Delete pet") }
            }
        },
    ) { padding ->
        if (!vm.loaded) return@Scaffold
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
                Text("Dog or cat?", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Species.entries.forEach { s ->
                        FilterChip(selected = form.species == s, onClick = { vm.setSpecies(s) }, label = { Text(s.label) })
                    }
                }
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { v -> vm.update { it.copy(name = v.take(30)) } },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RealPhoto(form.breed.wikiTitle, "Photo", localPhotoPath = form.photoPath, size = 84.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SecondaryButton(
                            if (form.photoPath == null) "Use my pet's photo" else "Change photo",
                            onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            icon = R.drawable.ic_photo,
                        )
                        if (form.photoPath != null) TextButton(onClick = vm::removePhoto) { Text("Use breed photo instead") }
                    }
                }
            }

            SectionCard {
                Text("Breed", style = MaterialTheme.typography.titleMedium)
                Card(
                    onClick = { showBreeds = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RealPhoto(form.breed.wikiTitle, null, size = 52.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(form.breed.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (form.breed.isMixed) "Size is taken from the weight you enter"
                                else "Typical adult: ${Format.weight(form.breed.minKg, vm.unit)} – ${Format.weight(form.breed.maxKg, vm.unit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AppIcon(R.drawable.ic_chevron, "Change breed")
                    }
                }
            }

            SectionCard {
                Text("Age", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Years", Modifier.width(72.dp))
                    Stepper("${form.years}", { vm.update { it.copy(years = (it.years - 1).coerceAtLeast(0)) } }, { vm.update { it.copy(years = (it.years + 1).coerceAtMost(30)) } }, "Fewer years", "More years")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Months", Modifier.width(72.dp))
                    Stepper("${form.months}", { vm.update { it.copy(months = (it.months - 1).coerceAtLeast(0)) } }, { vm.update { it.copy(months = (it.months + 1).coerceAtMost(11)) } }, "Fewer months", "More months")
                }
                if (form.ageMonths < 1) Text("Under 1 month: kittens and puppies this young should be on milk; ask your vet.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            SectionCard {
                Text("Weight", style = MaterialTheme.typography.titleMedium)
                val weightError = vm.weightError().takeIf { form.weightText.isNotEmpty() }
                OutlinedTextField(
                    value = form.weightText,
                    onValueChange = { v -> vm.update { it.copy(weightText = v.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6)) } },
                    label = { Text("Current weight (${vm.unit.label})") },
                    isError = weightError != null,
                    supportingText = { Text(weightError ?: "Weigh on a scale for the best result") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.targetText,
                    onValueChange = { v -> vm.update { it.copy(targetText = v.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6)) } },
                    label = { Text("Target weight (${vm.unit.label}, optional)") },
                    isError = vm.targetError() != null,
                    supportingText = { Text(vm.targetError() ?: "Leave empty to estimate it from body condition") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Body condition: ${form.bodyCondition} / 9", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = form.bodyCondition.toFloat(),
                    onValueChange = { v -> vm.update { it.copy(bodyCondition = v.roundToInt()) } },
                    valueRange = BodyCondition.MIN.toFloat()..BodyCondition.MAX.toFloat(),
                    steps = BodyCondition.MAX - BodyCondition.MIN - 1,
                )
                Text(BodyCondition.describe(form.bodyCondition), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SectionCard {
                Text("Lifestyle", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Spayed / neutered", style = MaterialTheme.typography.labelLarge)
                        Text("Neutered pets need about 10–20% fewer calories", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = form.neutered, onCheckedChange = { v -> vm.update { it.copy(neutered = v) } })
                }
                Text("Activity", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ActivityLevel.entries.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { level ->
                                FilterChip(
                                    selected = form.activity == level,
                                    onClick = { vm.update { it.copy(activity = level) } },
                                    label = { Text(level.label) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                Text(form.activity.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SectionCard {
                Text("Feeding", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Meals per day", Modifier.weight(1f))
                    Stepper("${form.mealsPerDay}", { vm.update { it.copy(mealsPerDay = (it.mealsPerDay - 1).coerceAtLeast(1)) } }, { vm.update { it.copy(mealsPerDay = (it.mealsPerDay + 1).coerceAtMost(4)) } }, "Fewer meals", "More meals")
                }
                Text("Treat budget: ${form.treatPercent}% of daily calories", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = form.treatPercent.toFloat(),
                    onValueChange = { v -> vm.update { it.copy(treatPercent = v.roundToInt()) } },
                    valueRange = 0f..10f,
                    steps = 9,
                )
                Text("Vets recommend keeping treats at 10% or less; that share is taken out of meals.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            PrimaryButton(
                text = if (petId > 0) "Save & recalculate" else "Calculate portions",
                enabled = vm.canSave(),
                icon = R.drawable.ic_calc,
                onClick = {
                    vm.save { id ->
                        // Major action finished: an interstitial may show (max one per 30 s), then the plan opens.
                        container.ads.showInterstitialThen(activity) { onSaved(id) }
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
