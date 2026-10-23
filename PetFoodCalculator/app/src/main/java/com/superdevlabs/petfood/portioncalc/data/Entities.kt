package com.superdevlabs.petfood.portioncalc.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.superdevlabs.petfood.portioncalc.domain.ActivityLevel
import com.superdevlabs.petfood.portioncalc.domain.Breeds
import com.superdevlabs.petfood.portioncalc.domain.FoodInfo
import com.superdevlabs.petfood.portioncalc.domain.FoodType
import com.superdevlabs.petfood.portioncalc.domain.PetProfile
import com.superdevlabs.petfood.portioncalc.domain.Species
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val species: Species,
    val breedId: String,
    val photoUri: String? = null,
    /** Age entered by the user, valid on [ageRecordedEpochDay]; the current age is derived. */
    val ageMonths: Int,
    val ageRecordedEpochDay: Long = LocalDate.now().toEpochDay(),
    val weightKg: Double,
    val targetWeightKg: Double? = null,
    val neutered: Boolean = true,
    val activity: ActivityLevel = ActivityLevel.NORMAL,
    val bodyCondition: Int = 5,
    val mealsPerDay: Int = 2,
    val treatPercent: Int = 10,
    val primaryFoodId: Long? = null,
    val secondaryFoodId: Long? = null,
    val secondarySharePercent: Int = 25,
    val remindersEnabled: Boolean = false,
    /** Meal times as minutes after midnight, comma separated, e.g. "450,1110". */
    val mealTimes: String = "450,1110",
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun breed() = Breeds.get(breedId) ?: Breeds.defaultFor(species)

    fun currentAgeMonths(today: LocalDate = LocalDate.now()): Int {
        val since = ChronoUnit.MONTHS.between(LocalDate.ofEpochDay(ageRecordedEpochDay), today).toInt()
        return ageMonths + since.coerceAtLeast(0)
    }

    fun mealTimeMinutes(): List<Int> =
        mealTimes.split(',').mapNotNull { it.trim().toIntOrNull() }.let { saved ->
            List(mealsPerDay) { i -> saved.getOrNull(i) ?: defaultMealTimes(mealsPerDay)[i] }
        }

    fun toProfile() = PetProfile(
        species = species,
        breed = breed(),
        ageMonths = currentAgeMonths(),
        weightKg = weightKg,
        targetWeightKg = targetWeightKg,
        neutered = neutered,
        activity = activity,
        bodyCondition = bodyCondition,
        treatPercent = treatPercent,
    )

    companion object {
        /** Evenly spread defaults between 07:30 and 19:30. */
        fun defaultMealTimes(meals: Int): List<Int> = when (meals) {
            1 -> listOf(8 * 60)
            2 -> listOf(7 * 60 + 30, 18 * 60 + 30)
            3 -> listOf(7 * 60 + 30, 13 * 60, 19 * 60)
            else -> listOf(7 * 60, 11 * 60, 15 * 60, 19 * 60)
        }
    }
}

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: FoodType,
    /** Null means the food suits both species. */
    val species: Species? = null,
    val kcalPer100g: Double,
    val gramsPerCup: Double? = null,
    val gramsPerUnit: Double? = null,
    val unitName: String? = null,
    val isPreset: Boolean = false,
) {
    fun toInfo() = FoodInfo(id, name, type, kcalPer100g, gramsPerCup, gramsPerUnit, unitName)
}

@Entity(
    tableName = "feeding_log",
    foreignKeys = [ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["petId", "epochDay", "mealIndex"], unique = true)],
)
data class FeedingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val epochDay: Long,
    val mealIndex: Int,
    val fedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "weight_log",
    foreignKeys = [ForeignKey(entity = PetEntity::class, parentColumns = ["id"], childColumns = ["petId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["petId", "epochDay"])],
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val epochDay: Long,
    val weightKg: Double,
)

/** Cached Wikipedia summary (real photo + intro text) for a breed or food article. */
@Entity(tableName = "wiki_cache")
data class WikiCacheEntity(
    @PrimaryKey val title: String,
    val imageUrl: String?,
    val extract: String?,
    val pageUrl: String?,
    val fetchedAt: Long,
)
