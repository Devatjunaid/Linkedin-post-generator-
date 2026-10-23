package com.superdevlabs.petfood.portioncalc.domain

import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt

enum class ActivityLevel(val label: String, val hint: String) {
    LOW("Low", "Mostly resting, short walks"),
    NORMAL("Normal", "Daily walks and play"),
    ACTIVE("Active", "Long runs, lots of play"),
    WORKING("Working / sport", "Herding, agility, hunting"),
}

enum class LifeStage(val label: String) {
    BABY_EARLY("Young baby (under 4 months)"),
    GROWING("Growing"),
    ADULT("Adult"),
    SENIOR("Senior"),
}

enum class WeightGoal(val label: String) { MAINTAIN("Maintain weight"), LOSE("Lose weight"), GAIN("Gain weight") }

enum class FoodType(val label: String) {
    DRY("Dry kibble"),
    WET("Wet / canned"),
    FRESH("Fresh cooked"),
    RAW("Raw"),
}

/** Body condition score on the WSAVA 9-point scale. */
object BodyCondition {
    const val MIN = 1
    const val MAX = 9

    fun describe(score: Int): String = when (score) {
        1 -> "1 · Emaciated: ribs, spine and hip bones visible from a distance"
        2 -> "2 · Very thin: ribs and spine easily visible, no fat"
        3 -> "3 · Thin: ribs easily felt and may be visible, waist obvious"
        4 -> "4 · Lean ideal: ribs easily felt with slight fat cover"
        5 -> "5 · Ideal: ribs felt without excess fat, waist seen from above"
        6 -> "6 · Slightly overweight: ribs felt with slight excess fat"
        7 -> "7 · Overweight: ribs hard to feel, waist barely visible"
        8 -> "8 · Obese: ribs not felt under heavy fat, no waist"
        else -> "9 · Severely obese: heavy fat on chest, spine and tail base"
    }
}

data class PetProfile(
    val species: Species,
    val breed: Breed,
    val ageMonths: Int,
    val weightKg: Double,
    val targetWeightKg: Double?,
    val neutered: Boolean,
    val activity: ActivityLevel,
    val bodyCondition: Int,
    val treatPercent: Int,
)

data class FoodInfo(
    val id: Long,
    val name: String,
    val type: FoodType,
    val kcalPer100g: Double,
    val gramsPerCup: Double?,
    val gramsPerUnit: Double?,
    val unitName: String?,
)

data class EnergyResult(
    val rerKcal: Double,
    val factor: Double,
    val factorBasisKg: Double,
    val dailyKcal: Int,
    val treatKcal: Int,
    val foodKcal: Int,
    val lifeStage: LifeStage,
    val goal: WeightGoal,
    val targetWeightKg: Double,
    val weeklyChangeKg: Double?,
    val weeksToGoal: Int?,
    val waterMlPerDay: Int,
    val notes: List<String>,
)

data class FoodPortion(
    val food: FoodInfo,
    val sharePercent: Int,
    val kcalPerDay: Double,
    val gramsPerDay: Double,
) {
    val cupsPerDay: Double? get() = food.gramsPerCup?.takeIf { it > 0 }?.let { gramsPerDay / it }
    val unitsPerDay: Double? get() = food.gramsPerUnit?.takeIf { it > 0 }?.let { gramsPerDay / it }
}

/**
 * Daily energy need using the standard veterinary approach (WSAVA / NRC):
 * resting energy RER = 70 x kg^0.75, multiplied by a life-stage/activity factor.
 * Weight change plans apply the factor to the target weight, not the current one.
 */
object NutritionCalculator {

    fun rer(weightKg: Double): Double = 70.0 * weightKg.coerceAtLeast(0.1).pow(0.75)

    fun lifeStage(species: Species, breed: Breed, ageMonths: Int, weightKg: Double): LifeStage {
        if (ageMonths < 4) return LifeStage.BABY_EARLY
        return when (species) {
            Species.DOG -> {
                val size = breed.sizeClassFor(weightKg)
                when {
                    ageMonths < size.adultAtMonths -> LifeStage.GROWING
                    ageMonths >= size.seniorAtYears * 12 -> LifeStage.SENIOR
                    else -> LifeStage.ADULT
                }
            }
            Species.CAT -> when {
                ageMonths < 12 -> LifeStage.GROWING
                ageMonths >= 11 * 12 -> LifeStage.SENIOR
                else -> LifeStage.ADULT
            }
        }
    }

    /** Each body condition point away from ideal is roughly 10% of body weight. */
    fun estimatedIdealWeight(weightKg: Double, bodyCondition: Int): Double = when {
        bodyCondition >= 6 -> weightKg / (1 + 0.10 * (bodyCondition - 5))
        bodyCondition <= 3 -> weightKg / (1 - 0.10 * (4 - bodyCondition))
        else -> weightKg
    }

    fun maintenanceFactor(species: Species, stage: LifeStage, neutered: Boolean, activity: ActivityLevel): Double =
        when (species) {
            Species.DOG -> when (stage) {
                LifeStage.BABY_EARLY -> 3.0
                LifeStage.GROWING -> 2.0
                LifeStage.SENIOR -> when (activity) {
                    ActivityLevel.LOW -> 1.2
                    ActivityLevel.NORMAL -> 1.4
                    ActivityLevel.ACTIVE -> 1.6
                    ActivityLevel.WORKING -> 2.0
                }
                LifeStage.ADULT -> when (activity) {
                    ActivityLevel.LOW -> if (neutered) 1.4 else 1.6
                    ActivityLevel.NORMAL -> if (neutered) 1.6 else 1.8
                    ActivityLevel.ACTIVE -> 2.0
                    ActivityLevel.WORKING -> 3.0
                }
            }
            Species.CAT -> when (stage) {
                LifeStage.BABY_EARLY -> 3.0
                LifeStage.GROWING -> 2.5
                LifeStage.SENIOR -> when (activity) {
                    ActivityLevel.LOW -> 1.0
                    ActivityLevel.NORMAL -> 1.1
                    else -> 1.3
                }
                LifeStage.ADULT -> when (activity) {
                    ActivityLevel.LOW -> if (neutered) 1.0 else 1.2
                    ActivityLevel.NORMAL -> if (neutered) 1.2 else 1.4
                    else -> 1.6
                }
            }
        }

    fun energy(p: PetProfile): EnergyResult {
        val stage = lifeStage(p.species, p.breed, p.ageMonths, p.weightKg)
        val growing = stage == LifeStage.BABY_EARLY || stage == LifeStage.GROWING
        val maintenance = maintenanceFactor(p.species, stage, p.neutered, p.activity)
        val notes = mutableListOf<String>()

        val target = (p.targetWeightKg ?: estimatedIdealWeight(p.weightKg, p.bodyCondition))
            .takeIf { it > 0 } ?: p.weightKg
        val diff = (target - p.weightKg) / p.weightKg
        val goal = when {
            growing -> WeightGoal.MAINTAIN
            diff < -0.03 -> WeightGoal.LOSE
            diff > 0.03 -> WeightGoal.GAIN
            else -> WeightGoal.MAINTAIN
        }

        val basisKg: Double
        val factor: Double
        when (goal) {
            WeightGoal.LOSE -> {
                basisKg = target
                factor = if (p.species == Species.DOG) 1.0 else 0.8
            }
            WeightGoal.GAIN -> {
                basisKg = target
                factor = maintenance
            }
            WeightGoal.MAINTAIN -> {
                basisKg = p.weightKg
                factor = maintenance
            }
        }
        var daily = rer(basisKg) * factor
        if (goal == WeightGoal.GAIN) {
            // Never plan less than ~10% above what keeps the current weight steady.
            daily = maxOf(daily, rer(p.weightKg) * maintenance * 1.1)
        }

        val weeklyRate = when (goal) {
            WeightGoal.LOSE -> if (p.species == Species.DOG) 0.015 else 0.0075
            WeightGoal.GAIN -> 0.01
            WeightGoal.MAINTAIN -> null
        }
        val weeklyChange = weeklyRate?.let { p.weightKg * it }
        val weeks = weeklyChange?.let { ceil(kotlin.math.abs(target - p.weightKg) / it).toInt() }

        if (growing) {
            notes += "Growing pets change fast: weigh every 1–2 weeks and update the profile so portions keep up."
            if (p.targetWeightKg != null || p.bodyCondition != 5) {
                notes += "Weight-loss plans are not applied to puppies or kittens. Ask your vet if you are worried about weight."
            }
        }
        if (goal == WeightGoal.LOSE) {
            notes += "Safe loss is about ${formatPercent(weeklyRate!!)} of body weight per week. Re-weigh every 2 weeks and adjust."
        }
        if (goal == WeightGoal.GAIN) {
            notes += "Increase food gradually over a week and re-weigh every 2 weeks."
        }
        if (stage == LifeStage.SENIOR) {
            notes += "Seniors vary a lot: if weight drifts more than 5% in a month, adjust portions or check with your vet."
        }
        notes += "Pregnant or nursing pets and pets on medical diets need a plan from your vet."

        val treatPct = p.treatPercent.coerceIn(0, 10)
        val dailyInt = daily.roundToInt()
        val treat = (dailyInt * treatPct / 100.0).roundToInt()
        val waterPerKg = if (p.species == Species.DOG) 55 else 45
        return EnergyResult(
            rerKcal = rer(basisKg),
            factor = factor,
            factorBasisKg = basisKg,
            dailyKcal = dailyInt,
            treatKcal = treat,
            foodKcal = dailyInt - treat,
            lifeStage = stage,
            goal = goal,
            targetWeightKg = target,
            weeklyChangeKg = weeklyChange,
            weeksToGoal = weeks,
            waterMlPerDay = (p.weightKg * waterPerKg).roundToInt(),
            notes = notes,
        )
    }

    /**
     * Splits the food calories between up to two foods. [secondarySharePercent] is the share
     * of calories (not grams) that comes from the second food.
     */
    fun portions(
        foodKcal: Int,
        primary: FoodInfo?,
        secondary: FoodInfo?,
        secondarySharePercent: Int,
    ): List<FoodPortion> {
        if (primary == null) return emptyList()
        val share = if (secondary == null) 0 else secondarySharePercent.coerceIn(0, 90)
        val out = mutableListOf<FoodPortion>()
        val primaryKcal = foodKcal * (100 - share) / 100.0
        out += FoodPortion(primary, 100 - share, primaryKcal, gramsFor(primaryKcal, primary))
        if (secondary != null && share > 0) {
            val kcal = foodKcal * share / 100.0
            out += FoodPortion(secondary, share, kcal, gramsFor(kcal, secondary))
        }
        return out
    }

    fun gramsFor(kcal: Double, food: FoodInfo): Double =
        if (food.kcalPer100g <= 0) 0.0 else kcal / (food.kcalPer100g / 100.0)

    private fun formatPercent(v: Double): String {
        val pct = v * 100
        return if (pct % 1.0 == 0.0) "${pct.toInt()}%" else "${"%.2f".format(pct).trimEnd('0').trimEnd('.')}%"
    }
}

/** Food switch schedule: the share of calories from the new food on each day. */
data class TransitionDay(val day: Int, val newPercent: Int, val oldGrams: Double, val newGrams: Double)

object TransitionPlanner {
    /** 7 days: 25,25,50,50,75,75,100. 10 days: three days each of 25/50/75, then 100. */
    fun newFoodPercent(day: Int, totalDays: Int): Int {
        if (day >= totalDays) return 100
        val step = ((totalDays - 1) / 3).coerceAtLeast(1)
        return minOf(75, 25 * (1 + (day - 1) / step))
    }

    fun plan(totalDays: Int, foodKcal: Int, oldFood: FoodInfo, newFood: FoodInfo): List<TransitionDay> =
        (1..totalDays).map { day ->
            val pct = newFoodPercent(day, totalDays)
            val newKcal = foodKcal * pct / 100.0
            val oldKcal = foodKcal - newKcal
            TransitionDay(day, pct, NutritionCalculator.gramsFor(oldKcal, oldFood), NutritionCalculator.gramsFor(newKcal, newFood))
        }
}
