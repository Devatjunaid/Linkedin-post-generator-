package com.dailycash.tracker.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dailycash.tracker.ads.InterstitialAds
import com.dailycash.tracker.ui.brand.AnimatedSplash
import com.dailycash.tracker.ui.components.MainFrame
import com.dailycash.tracker.ui.components.Tab
import com.dailycash.tracker.ui.screens.AddScreen
import com.dailycash.tracker.ui.screens.AtlasScreen
import com.dailycash.tracker.ui.screens.HistoryScreen
import com.dailycash.tracker.ui.screens.HomeScreen
import com.dailycash.tracker.ui.screens.InsightsScreen
import com.dailycash.tracker.ui.screens.SettingsScreen
import com.dailycash.tracker.ui.theme.Palette

private object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val ATLAS = "atlas"
    const val INSIGHTS = "insights"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ADD = "add/{mode}?currency={currency}"
    fun add(mode: String, currency: String?) = "add/$mode" + (currency?.let { "?currency=$it" } ?: "")
}

private fun Tab.route() = when (this) {
    Tab.WALLET -> Routes.HOME
    Tab.ATLAS -> Routes.ATLAS
    Tab.INSIGHTS -> Routes.INSIGHTS
    Tab.HISTORY -> Routes.HISTORY
}

@Composable
fun DailyCashNav(onExit: () -> Unit) {
    val nav = rememberNavController()
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory)
    val activity = LocalActivity.current

    fun goTab(tab: Tab) {
        val go = {
            nav.navigate(tab.route()) {
                popUpTo(Routes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        // Reports are a "major function": an interstitial may show first (max once per 30 s).
        if (tab == Tab.ATLAS || tab == Tab.INSIGHTS) InterstitialAds.showThen(activity, go) else go()
    }

    fun openAdd(mode: String, currency: String?) = nav.navigate(Routes.add(mode, currency))

    val spec = tween<Float>(380, easing = FastOutSlowInEasing)
    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH,
        modifier = Modifier.fillMaxSize().background(Palette.Ink),
        enterTransition = { fadeIn(spec) + scaleIn(spec, initialScale = 0.94f) },
        exitTransition = { fadeOut(tween(200)) + scaleOut(spec, targetScale = 1.04f) },
        popEnterTransition = { fadeIn(spec) + scaleIn(spec, initialScale = 1.04f) },
        popExitTransition = { fadeOut(tween(200)) + scaleOut(spec, targetScale = 0.94f) },
    ) {
        composable(
            Routes.SPLASH,
            exitTransition = { fadeOut(tween(500)) + scaleOut(tween(500), targetScale = 1.15f) },
        ) {
            AnimatedSplash(onFinished = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            MainFrame(Tab.WALLET, ::goTab, { openAdd("out", null) }) {
                HomeScreen(
                    vm = vm,
                    onAdd = ::openAdd,
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                    onHistory = { goTab(Tab.HISTORY) },
                    onExit = onExit,
                )
            }
        }
        composable(Routes.ATLAS) {
            MainFrame(Tab.ATLAS, ::goTab, { openAdd("out", null) }) { AtlasScreen(vm) }
        }
        composable(Routes.INSIGHTS) {
            MainFrame(Tab.INSIGHTS, ::goTab, { openAdd("out", null) }) { InsightsScreen(vm) }
        }
        composable(Routes.HISTORY) {
            MainFrame(Tab.HISTORY, ::goTab, { openAdd("out", null) }) { HistoryScreen(vm) }
        }
        composable(
            Routes.ADD,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType; defaultValue = "out" },
                navArgument("currency") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(420)) + fadeIn() },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(380)) + fadeOut() },
        ) { entry ->
            AddScreen(
                vm = vm,
                initialMode = entry.arguments?.getString("mode") ?: "out",
                initialCurrency = entry.arguments?.getString("currency"),
                onDone = { nav.popBackStack() },
            )
        }
        composable(
            Routes.SETTINGS,
            enterTransition = { slideInVertically(tween(380)) { it / 3 } + fadeIn(spec) },
            popExitTransition = { slideOutVertically(tween(300)) { it / 3 } + fadeOut(spec) },
        ) {
            SettingsScreen(vm, onBack = { nav.popBackStack() })
        }
    }
}
