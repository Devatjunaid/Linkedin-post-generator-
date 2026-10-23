package com.superdevlabs.petfood.portioncalc.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.superdevlabs.petfood.portioncalc.domain.Species
import com.superdevlabs.petfood.portioncalc.ui.breed.BreedGuideScreen
import com.superdevlabs.petfood.portioncalc.ui.food.FoodEditorScreen
import com.superdevlabs.petfood.portioncalc.ui.food.FoodLibraryScreen
import com.superdevlabs.petfood.portioncalc.ui.home.HomeScreen
import com.superdevlabs.petfood.portioncalc.ui.pet.PetEditorScreen
import com.superdevlabs.petfood.portioncalc.ui.plan.PlanScreen
import com.superdevlabs.petfood.portioncalc.ui.settings.PrivacyPolicyScreen
import com.superdevlabs.petfood.portioncalc.ui.settings.SettingsScreen
import com.superdevlabs.petfood.portioncalc.ui.transition.TransitionScreen
import com.superdevlabs.petfood.portioncalc.ui.weight.WeightScreen

private object Routes {
    const val HOME = "home"
    const val PET_EDITOR = "pet?petId={petId}"
    const val PLAN = "plan/{petId}"
    const val WEIGHT = "weight/{petId}"
    const val TRANSITION = "transition/{petId}"
    const val FOODS = "foods?petId={petId}&slot={slot}"
    const val FOOD_EDITOR = "food?foodId={foodId}&species={species}"
    const val BREED = "breed/{breedId}"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"

    fun petEditor(id: Long = 0) = "pet?petId=$id"
    fun plan(id: Long) = "plan/$id"
    fun weight(id: Long) = "weight/$id"
    fun transition(id: Long) = "transition/$id"
    fun foods(petId: Long = 0, slot: String? = null) = "foods?petId=$petId&slot=${slot ?: ""}"
    fun foodEditor(id: Long = 0, species: Species? = null) = "food?foodId=$id&species=${species?.name ?: ""}"
    fun breed(id: String) = "breed/$id"
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    fun back() {
        nav.popBackStack()
    }

    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddPet = { nav.navigate(Routes.petEditor()) },
                onOpenPlan = { nav.navigate(Routes.plan(it)) },
                onOpenWeight = { nav.navigate(Routes.weight(it)) },
                onOpenTransition = { nav.navigate(Routes.transition(it)) },
                onOpenFoods = { nav.navigate(Routes.foods()) },
                onOpenBreed = { nav.navigate(Routes.breed(it)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.PET_EDITOR, listOf(navArgument("petId") { type = NavType.LongType; defaultValue = 0L })) { entry ->
            val petId = entry.arguments?.getLong("petId") ?: 0L
            PetEditorScreen(
                petId = petId,
                onBack = ::back,
                onSaved = { id ->
                    nav.navigate(Routes.plan(id)) { popUpTo(Routes.HOME) }
                },
                onDeleted = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.PLAN, listOf(navArgument("petId") { type = NavType.LongType })) { entry ->
            val petId = entry.arguments?.getLong("petId") ?: return@composable
            PlanScreen(
                petId = petId,
                onBack = ::back,
                onEditPet = { nav.navigate(Routes.petEditor(petId)) },
                onPickFood = { slot -> nav.navigate(Routes.foods(petId, slot)) },
                onOpenWeight = { nav.navigate(Routes.weight(petId)) },
            )
        }
        composable(Routes.WEIGHT, listOf(navArgument("petId") { type = NavType.LongType })) { entry ->
            WeightScreen(petId = entry.arguments?.getLong("petId") ?: return@composable, onBack = ::back)
        }
        composable(Routes.TRANSITION, listOf(navArgument("petId") { type = NavType.LongType })) { entry ->
            TransitionScreen(petId = entry.arguments?.getLong("petId") ?: return@composable, onBack = ::back)
        }
        composable(
            Routes.FOODS,
            listOf(
                navArgument("petId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("slot") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            FoodLibraryScreen(
                petId = entry.arguments?.getLong("petId") ?: 0L,
                slot = entry.arguments?.getString("slot")?.ifBlank { null },
                onBack = ::back,
                onAddFood = { species -> nav.navigate(Routes.foodEditor(species = species)) },
                onEditFood = { id -> nav.navigate(Routes.foodEditor(id)) },
            )
        }
        composable(
            Routes.FOOD_EDITOR,
            listOf(
                navArgument("foodId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("species") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val species = entry.arguments?.getString("species")?.let { s -> Species.entries.firstOrNull { it.name == s } }
            FoodEditorScreen(foodId = entry.arguments?.getLong("foodId") ?: 0L, species = species, onBack = ::back)
        }
        composable(Routes.BREED, listOf(navArgument("breedId") { type = NavType.StringType })) { entry ->
            BreedGuideScreen(breedId = entry.arguments?.getString("breedId") ?: return@composable, onBack = ::back)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = ::back, onOpenPrivacy = { nav.navigate(Routes.PRIVACY) })
        }
        composable(Routes.PRIVACY) { PrivacyPolicyScreen(onBack = ::back) }
    }
}
