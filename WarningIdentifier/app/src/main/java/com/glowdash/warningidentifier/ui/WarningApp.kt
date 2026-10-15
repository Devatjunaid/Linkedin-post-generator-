package com.glowdash.warningidentifier.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.glowdash.warningidentifier.ads.AnchoredBanner
import com.glowdash.warningidentifier.ui.components.NavTab
import com.glowdash.warningidentifier.ui.history.HistoryScreen
import com.glowdash.warningidentifier.ui.home.HomeScreen
import com.glowdash.warningidentifier.ui.library.LibraryScreen
import com.glowdash.warningidentifier.ui.result.LightDetailScreen
import com.glowdash.warningidentifier.ui.result.ScanResultScreen
import com.glowdash.warningidentifier.ui.scan.ScanScreen
import com.glowdash.warningidentifier.ui.splash.SplashScreen
import com.glowdash.warningidentifier.ui.theme.WiColors

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val SCAN = "scan"
    const val SCAN_RESULT = "scan_result"
    const val LIBRARY = "library"
    const val HISTORY = "history"
    const val LIGHT = "light/{id}?conf={conf}&scan={scan}"

    fun light(id: String, confidence: Int = -1, fromScan: Boolean = false) =
        "light/$id?conf=$confidence&scan=$fromScan"
}

/** Screens where the bottom banner stays hidden (splash and full-screen camera). */
private val noBannerRoutes = setOf(Routes.SPLASH, Routes.SCAN)

@Composable
fun WarningApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBanner = route != null && route !in noBannerRoutes

    Column(
        Modifier
            .fillMaxSize()
            .background(WiColors.Bg)
            .navigationBarsPadding(),
    ) {
        NavHost(
            navController = nav,
            startDestination = Routes.SPLASH,
            modifier = Modifier.weight(1f),
            enterTransition = { fadeIn(tween(260)) + slideInHorizontally(tween(320)) { it / 8 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(320)) { it / 8 } },
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(onFinished = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onScan = { nav.navigate(Routes.SCAN) },
                    onScanResult = { nav.navigate(Routes.SCAN_RESULT) },
                    onOpenLight = { id -> nav.navigate(Routes.light(id)) },
                    onTab = { nav.switchTab(it) },
                )
            }
            composable(Routes.SCAN) {
                ScanScreen(
                    onBack = { nav.popBackStack() },
                    onResult = {
                        nav.navigate(Routes.SCAN_RESULT) { popUpTo(Routes.SCAN) { inclusive = true } }
                    },
                    onManualSearch = { nav.navigate(Routes.LIBRARY) { popUpTo(Routes.SCAN) { inclusive = true } } },
                )
            }
            composable(Routes.SCAN_RESULT) {
                ScanResultScreen(
                    onBack = { nav.popBackStack() },
                    onPick = { id -> nav.navigate(Routes.light(id, fromScan = true)) },
                    onScanAgain = { nav.navigate(Routes.SCAN) { popUpTo(Routes.SCAN_RESULT) { inclusive = true } } },
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onOpenLight = { id -> nav.navigate(Routes.light(id)) },
                    onScan = { nav.navigate(Routes.SCAN) },
                    onTab = { nav.switchTab(it) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onOpenLight = { id -> nav.navigate(Routes.light(id)) },
                    onScan = { nav.navigate(Routes.SCAN) },
                    onTab = { nav.switchTab(it) },
                )
            }
            composable(
                Routes.LIGHT,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("conf") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("scan") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { backStack ->
                val args = backStack.arguments
                LightDetailScreen(
                    lightId = args?.getString("id").orEmpty(),
                    confidence = args?.getInt("conf") ?: -1,
                    fromScan = args?.getBoolean("scan") ?: false,
                    alternatives = emptyList(),
                    onBack = { nav.popBackStack() },
                    onOpenLight = { id -> nav.navigate(Routes.light(id)) },
                )
            }
        }

        AnchoredBanner(visible = showBanner)
    }
}

private fun NavHostController.switchTab(tab: NavTab) {
    val target = when (tab) {
        NavTab.HOME -> Routes.HOME
        NavTab.LIBRARY -> Routes.LIBRARY
        NavTab.HISTORY -> Routes.HISTORY
    }
    if (currentDestination?.route == target) return
    navigate(target) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
