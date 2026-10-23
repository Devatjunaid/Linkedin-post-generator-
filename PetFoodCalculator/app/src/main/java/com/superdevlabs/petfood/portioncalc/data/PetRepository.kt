package com.superdevlabs.petfood.portioncalc.data

import androidx.room.withTransaction
import com.superdevlabs.petfood.portioncalc.domain.FoodType
import com.superdevlabs.petfood.portioncalc.domain.Species
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PetRepository(private val db: AppDatabase) {
    private val pets = db.petDao()
    private val foods = db.foodDao()
    private val feeding = db.feedingLogDao()
    private val weights = db.weightLogDao()

    // ---- pets
    fun observePets(): Flow<List<PetEntity>> = pets.observeAll()
    fun observePet(id: Long): Flow<PetEntity?> = pets.observe(id)
    suspend fun getPet(id: Long): PetEntity? = pets.get(id)
    suspend fun allPets(): List<PetEntity> = pets.getAll()

    /** Saves the pet and records a weight entry when the weight is new or changed. */
    suspend fun savePet(pet: PetEntity): Long = db.withTransaction {
        val today = LocalDate.now().toEpochDay()
        if (pet.id == 0L) {
            val id = pets.insert(pet)
            weights.insert(WeightLogEntity(petId = id, epochDay = today, weightKg = pet.weightKg))
            id
        } else {
            val old = pets.get(pet.id)
            pets.update(pet)
            if (old == null || kotlin.math.abs(old.weightKg - pet.weightKg) > 0.001) {
                weights.insert(WeightLogEntity(petId = pet.id, epochDay = today, weightKg = pet.weightKg))
            }
            pet.id
        }
    }

    suspend fun updatePet(pet: PetEntity) = pets.update(pet)
    suspend fun deletePet(pet: PetEntity) = pets.delete(pet)

    // ---- foods
    fun observeFoods(): Flow<List<FoodEntity>> = foods.observeAll()
    fun observeFood(id: Long): Flow<FoodEntity?> = foods.observe(id)
    suspend fun getFood(id: Long): FoodEntity? = foods.get(id)

    suspend fun saveFood(food: FoodEntity): Long =
        if (food.id == 0L) foods.insert(food) else food.id.also { foods.update(food) }

    suspend fun deleteFood(food: FoodEntity) = db.withTransaction {
        pets.clearPrimaryFood(food.id)
        pets.clearSecondaryFood(food.id)
        foods.delete(food)
    }

    /** Typical label values so the calculator works before the user adds their own food. */
    suspend fun seedPresetsIfNeeded() {
        if (foods.presetCount() > 0) return
        foods.insertAll(
            listOf(
                FoodEntity(name = "Dry dog kibble · adult (typical)", type = FoodType.DRY, species = Species.DOG, kcalPer100g = 370.0, gramsPerCup = 100.0, isPreset = true),
                FoodEntity(name = "Dry dog kibble · puppy (typical)", type = FoodType.DRY, species = Species.DOG, kcalPer100g = 400.0, gramsPerCup = 100.0, isPreset = true),
                FoodEntity(name = "Dry dog kibble · light (typical)", type = FoodType.DRY, species = Species.DOG, kcalPer100g = 320.0, gramsPerCup = 95.0, isPreset = true),
                FoodEntity(name = "Dry dog kibble · senior (typical)", type = FoodType.DRY, species = Species.DOG, kcalPer100g = 350.0, gramsPerCup = 100.0, isPreset = true),
                FoodEntity(name = "Wet dog food · 370 g can (typical)", type = FoodType.WET, species = Species.DOG, kcalPer100g = 95.0, gramsPerUnit = 370.0, unitName = "can", isPreset = true),
                FoodEntity(name = "Fresh cooked dog food (typical)", type = FoodType.FRESH, species = Species.DOG, kcalPer100g = 130.0, isPreset = true),
                FoodEntity(name = "Raw dog diet (typical)", type = FoodType.RAW, species = Species.DOG, kcalPer100g = 160.0, isPreset = true),
                FoodEntity(name = "Dry cat food · adult (typical)", type = FoodType.DRY, species = Species.CAT, kcalPer100g = 380.0, gramsPerCup = 100.0, isPreset = true),
                FoodEntity(name = "Dry cat food · kitten (typical)", type = FoodType.DRY, species = Species.CAT, kcalPer100g = 410.0, gramsPerCup = 100.0, isPreset = true),
                FoodEntity(name = "Wet cat food · 85 g pouch (typical)", type = FoodType.WET, species = Species.CAT, kcalPer100g = 80.0, gramsPerUnit = 85.0, unitName = "pouch", isPreset = true),
                FoodEntity(name = "Wet cat food · 156 g can (typical)", type = FoodType.WET, species = Species.CAT, kcalPer100g = 95.0, gramsPerUnit = 156.0, unitName = "can", isPreset = true),
                FoodEntity(name = "Raw cat diet (typical)", type = FoodType.RAW, species = Species.CAT, kcalPer100g = 160.0, isPreset = true),
            ),
        )
    }

    // ---- feeding log
    fun observeFeedingOn(epochDay: Long): Flow<List<FeedingLogEntity>> = feeding.observeDay(epochDay)
    fun observeFeedingSince(petId: Long, fromEpochDay: Long) = feeding.observeSince(petId, fromEpochDay)

    suspend fun setMealFed(petId: Long, epochDay: Long, mealIndex: Int, fed: Boolean) {
        if (fed) feeding.insert(FeedingLogEntity(petId = petId, epochDay = epochDay, mealIndex = mealIndex))
        else feeding.delete(petId, epochDay, mealIndex)
    }

    suspend fun isMealFed(petId: Long, epochDay: Long, mealIndex: Int) = feeding.count(petId, epochDay, mealIndex) > 0

    suspend fun pruneFeedingLog() = feeding.deleteOlderThan(LocalDate.now().minusDays(90).toEpochDay())

    // ---- weight log
    fun observeWeights(petId: Long): Flow<List<WeightLogEntity>> = weights.observe(petId)

    /** Adds a weigh-in; when it is the newest entry the pet's current weight follows it. */
    suspend fun addWeight(petId: Long, epochDay: Long, kg: Double) = db.withTransaction {
        weights.insert(WeightLogEntity(petId = petId, epochDay = epochDay, weightKg = kg))
        val latest = weights.latest(petId)
        val pet = pets.get(petId)
        if (pet != null && latest != null && latest.epochDay == epochDay) pets.update(pet.copy(weightKg = kg))
    }

    suspend fun deleteWeight(entry: WeightLogEntity) = db.withTransaction {
        weights.delete(entry)
        val latest = weights.latest(entry.petId)
        val pet = pets.get(entry.petId)
        if (pet != null && latest != null) pets.update(pet.copy(weightKg = latest.weightKg))
    }
}
