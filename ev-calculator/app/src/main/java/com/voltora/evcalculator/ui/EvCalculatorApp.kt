package com.voltora.evcalculator.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.voltora.evcalculator.AppViewModel
import com.voltora.evcalculator.ads.AdsManager
import com.voltora.evcalculator.ads.BannerAd
import com.voltora.evcalculator.ui.components.CountryPickerDialog
import com.voltora.evcalculator.ui.components.ExitDialog
import com.voltora.evcalculator.ui.screens.ChargeScreen
import com.voltora.evcalculator.ui.screens.HomeScreen
import com.voltora.evcalculator.ui.screens.SettingsScreen
import com.voltora.evcalculator.ui.screens.SplashScreen
import com.voltora.evcalculator.ui.screens.TripScreen
import com.voltora.evcalculator.ui.screens.VehicleScreen
import com.voltora.evcalculator.ui.theme.Body
import com.voltora.evcalculator.ui.theme.Ev

private object Routes {
    const val HOME = "home"
    const val VEHICLE = "vehicle"
    const val CHARGE = "charge"
    const val TRIP = "trip"
    const val SETTINGS = "settings"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Outlined.Home),
    Tab(Routes.VEHICLE, "Vehicle", Icons.Outlined.DirectionsCar),
    Tab(Routes.CHARGE, "Charge", Icons.Outlined.Bolt),
    Tab(Routes.TRIP, "Trip", Icons.Outlined.Map),
)

@Composable
fun EvCalculatorApp() {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    AnimatedContent(
        targetState = showSplash,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "splash",
    ) { splash ->
        if (splash) SplashScreen(onFinished = { showSplash = false }) else MainScaffold()
    }
}

@Composable
private fun MainScaffold(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val activity = LocalContext.current as? Activity
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: Routes.HOME

    var showExit by rememberSaveable { mutableStateOf(false) }
    var showCountry by rememberSaveable { mutableStateOf(false) }

    // Back on Home asks before leaving; other tabs fall back to Home via the back stack.
    BackHandler(enabled = route == Routes.HOME) { showExit = true }

    Scaffold(
        containerColor = Ev.Paper,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Column(Modifier.background(Ev.Ink).navigationBarsPadding()) {
                if (route != Routes.SETTINGS) BottomNav(route) { goTab(nav, it) }
                BannerAd()
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    state = state,
                    onSocChange = vm::setSoc,
                    onPickCountry = { showCountry = true },
                    onOpenVehicle = { goTab(nav, Routes.VEHICLE) },
                    onOpenCharge = { goTab(nav, Routes.CHARGE) },
                    onOpenTrip = { goTab(nav, Routes.TRIP) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                )
            }
            composable(Routes.VEHICLE) {
                VehicleScreen(
                    state = state,
                    onSelect = { vm.setModel(it.id) },
                    onContinue = {
                        AdsManager.maybeShowInterstitial(activity) { goTab(nav, Routes.CHARGE) }
                    },
                )
            }
            composable(Routes.CHARGE) { ChargeScreen(state) }
            composable(Routes.TRIP) {
                TripScreen(
                    state = state,
                    onPlanRequested = { ready -> AdsManager.maybeShowInterstitial(activity, ready) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    state = state,
                    onBack = { nav.popBackStack() },
                    onPickCountry = { showCountry = true },
                    onUnit = vm::setDistanceUnit,
                    onHomeKw = vm::setHomeChargerKw,
                    onPetrolL = vm::setPetrolLPer100,
                    onSaveRates = { h, a, d, p -> vm.setRates(h, a, d, p) },
                    privacyOptionsRequired = AdsManager.privacyOptionsRequired,
                    onPrivacyOptions = { activity?.let { AdsManager.showPrivacyOptions(it) } },
                )
            }
        }
    }

    if (showCountry) {
        CountryPickerDialog(
            selected = state.country,
            onPick = { vm.setCountry(it.code); showCountry = false },
            onDismiss = { showCountry = false },
        )
    }
    if (showExit) {
        ExitDialog(onStay = { showExit = false }, onExit = { showExit = false; activity?.finish() })
    }
}

private fun goTab(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomNav(current: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Ev.Ink)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        tabs.forEach { tab ->
            val selected = current == tab.route
            val color = if (selected) Ev.Volt else Ev.MutedOnDark
            Column(
                Modifier
                    .heightIn(min = 52.dp)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab.route) })
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Text(
                    tab.label, color = color, fontFamily = Body, fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}
