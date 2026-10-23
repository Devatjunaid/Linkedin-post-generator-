package com.superdevlabs.petfood.portioncalc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionCalculatorTest {

    private val golden = Breeds.get("golden")!!
    private val mixedCat = Breeds.get("cat_mixed")!!
    private val kibble = FoodInfo(1, "Kibble", FoodType.DRY, 380.0, 100.0, null, null)
    private val wet = FoodInfo(2, "Wet", FoodType.WET, 95.0, null, 370.0, "can")

    private fun dog(
        ageMonths: Int = 36,
        weight: Double = 30.0,
        target: Double? = null,
        neutered: Boolean = true,
        activity: ActivityLevel = ActivityLevel.NORMAL,
        bcs: Int = 5,
        treats: Int = 10,
    ) = PetProfile(Species.DOG, golden, ageMonths, weight, target, neutered, activity, bcs, treats)

    @Test
    fun rerMatchesFormula() {
        assertEquals(897.5, NutritionCalculator.rer(30.0), 0.5)
        assertEquals(198.0, NutritionCalculator.rer(4.0), 0.5)
    }

    @Test
    fun neuteredAdultDogMaintenance() {
        val r = NutritionCalculator.energy(dog())
        assertEquals(LifeStage.ADULT, r.lifeStage)
        assertEquals(WeightGoal.MAINTAIN, r.goal)
        assertEquals(1.6, r.factor, 0.0)
        assertEquals(1436, r.dailyKcal)
        assertEquals(144, r.treatKcal)
        assertEquals(1292, r.foodKcal)
        assertNull(r.weeksToGoal)
    }

    @Test
    fun overweightDogUsesTargetWeightAndRerFactorOne() {
        val r = NutritionCalculator.energy(dog(weight = 36.0, bcs = 7))
        assertEquals(WeightGoal.LOSE, r.goal)
        assertEquals(30.0, r.targetWeightKg, 0.01)
        assertEquals(1.0, r.factor, 0.0)
        assertEquals(897, r.dailyKcal)
    }

    @Test
    fun largeBreedStillGrowingAt12Months() {
        val r = NutritionCalculator.energy(dog(ageMonths = 12, weight = 26.0, bcs = 7))
        assertEquals(LifeStage.GROWING, r.lifeStage)
        assertEquals(WeightGoal.MAINTAIN, r.goal)
        assertEquals(2.0, r.factor, 0.0)
    }

    @Test
    fun youngPuppyFactorThree() {
        val r = NutritionCalculator.energy(dog(ageMonths = 3, weight = 8.0))
        assertEquals(LifeStage.BABY_EARLY, r.lifeStage)
        assertEquals(3.0, r.factor, 0.0)
    }

    @Test
    fun seniorLargeDog() {
        val r = NutritionCalculator.energy(dog(ageMonths = 8 * 12))
        assertEquals(LifeStage.SENIOR, r.lifeStage)
        assertEquals(1.4, r.factor, 0.0)
    }

    @Test
    fun neuteredIndoorCat() {
        val cat = PetProfile(Species.CAT, mixedCat, 48, 4.0, null, true, ActivityLevel.NORMAL, 5, 0)
        val r = NutritionCalculator.energy(cat)
        assertEquals(1.2, r.factor, 0.0)
        assertEquals(238, r.dailyKcal)
        assertEquals(238, r.foodKcal)
    }

    @Test
    fun overweightCatLosesAtPointEight() {
        val cat = PetProfile(Species.CAT, mixedCat, 48, 6.0, 5.0, true, ActivityLevel.NORMAL, 7, 0)
        val r = NutritionCalculator.energy(cat)
        assertEquals(WeightGoal.LOSE, r.goal)
        assertEquals(0.8, r.factor, 0.0)
        assertEquals(NutritionCalculator.rer(5.0) * 0.8, r.dailyKcal.toDouble(), 1.0)
    }

    @Test
    fun portionsSplitByCalories() {
        val p = NutritionCalculator.portions(1292, kibble, wet, 25)
        assertEquals(2, p.size)
        assertEquals(969.0, p[0].kcalPerDay, 0.01)
        assertEquals(255.0, p[0].gramsPerDay, 0.1)
        assertEquals(2.55, p[0].cupsPerDay!!, 0.01)
        assertEquals(340.0, p[1].gramsPerDay, 0.1)
        assertEquals(0.92, p[1].unitsPerDay!!, 0.01)
    }

    @Test
    fun transitionSchedules() {
        assertEquals(listOf(25, 25, 50, 50, 75, 75, 100), (1..7).map { TransitionPlanner.newFoodPercent(it, 7) })
        assertEquals(listOf(25, 25, 25, 50, 50, 50, 75, 75, 75, 100), (1..10).map { TransitionPlanner.newFoodPercent(it, 10) })
    }

    @Test
    fun cupFormatting() {
        assertEquals("1 ¾ cups", Format.cups(1.7))
        assertEquals("½ cup", Format.cups(0.5))
        assertEquals("1 cup", Format.cups(1.0))
        assertEquals("2 cups", Format.cups(2.02))
    }
}
