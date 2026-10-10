package com.harrycodess.fueltracker.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.harrycodess.fueltracker.AppContainer
import com.harrycodess.fueltracker.ui.calculator.CalculatorScreen
import com.harrycodess.fueltracker.ui.components.AdBannerSlot
import com.harrycodess.fueltracker.ui.edit.EditTripScreen
import com.harrycodess.fueltracker.ui.edit.EditTripViewModel
import com.harrycodess.fueltracker.ui.history.HistoryScreen
import com.harrycodess.fueltracker.ui.settings.SettingsScreen
import com.harrycodess.fueltracker.ui.vehicles.VehiclesScreen
import com.harrycodess.fueltracker.util.findActivity

object Routes {
    const val CALCULATOR = "calculator"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val VEHICLES = "vehicles"
    const val EDIT_TRIP = "trip/{${EditTripViewModel.TRIP_ID_ARG}}"
    fun editTrip(id: Long) = "trip/$id"
}

private enum class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    CALCULATOR(Routes.CALCULATOR, "Calculator", Icons.Rounded.Calculate),
    HISTORY(Routes.HISTORY, "History", Icons.Rounded.History),
}

@Composable
fun FuelTrackerRoot(container: AppContainer) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val ads = container.adsManager
    val canRequestAds by ads.canRequestAds.collectAsStateWithLifecycle()
    val privacyOptionsRequired by ads.privacyOptionsRequired.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showNavBar = TopLevel.entries.any { it.route == currentRoute }

    Scaffold(
        // Each screen draws its own top bar, which handles the status bar inset.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                // The banner sits in the same place on every screen and its height never changes.
                AdBannerSlot(
                    canRequestAds = canRequestAds,
                    modifier = if (showNavBar) Modifier else Modifier.navigationBarsPadding(),
                )
                if (showNavBar) {
                    NavigationBar {
                        TopLevel.entries.forEach { dest ->
                            NavigationBarItem(
                                selected = currentRoute == dest.route,
                                onClick = { navController.navigateTopLevel(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = null) },
                                label = { Text(dest.label) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CALCULATOR,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            composable(Routes.CALCULATOR) {
                CalculatorScreen(
                    snackbarHostState = snackbarHostState,
                    showInterstitial = ads::showInterstitial,
                    onViewHistory = { navController.navigateTopLevel(Routes.HISTORY) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    snackbarHostState = snackbarHostState,
                    onTripClick = { navController.navigate(Routes.editTrip(it.id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onGoToCalculator = { navController.navigateTopLevel(Routes.CALCULATOR) },
                )
            }
            composable(
                route = Routes.EDIT_TRIP,
                arguments = listOf(navArgument(EditTripViewModel.TRIP_ID_ARG) { type = NavType.LongType }),
            ) {
                EditTripScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    snackbarHostState = snackbarHostState,
                    privacyOptionsRequired = privacyOptionsRequired,
                    onPrivacyOptions = { context.findActivity()?.let(ads::showPrivacyOptions) },
                    onOpenVehicles = { navController.navigate(Routes.VEHICLES) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.VEHICLES) {
                VehiclesScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
