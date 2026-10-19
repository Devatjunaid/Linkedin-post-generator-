package com.brickbeam.buildbros.ui

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.brickbeam.buildbros.ads.AdsManager
import com.brickbeam.buildbros.ads.BannerAdBar
import com.brickbeam.buildbros.data.AppSettings
import com.brickbeam.buildbros.ui.calc.BrickScreen
import com.brickbeam.buildbros.ui.calc.ConcreteScreen
import com.brickbeam.buildbros.ui.calc.ConverterScreen
import com.brickbeam.buildbros.ui.calc.PaintScreen
import com.brickbeam.buildbros.ui.calc.SteelScreen
import com.brickbeam.buildbros.ui.calc.TileScreen
import com.brickbeam.buildbros.ui.components.AppBackground
import com.brickbeam.buildbros.ui.home.HomeScreen
import com.brickbeam.buildbros.ui.prices.PricesScreen
import com.brickbeam.buildbros.ui.projects.EstimateScreen
import com.brickbeam.buildbros.ui.projects.ProjectsScreen
import com.brickbeam.buildbros.ui.region.RegionScreen
import com.brickbeam.buildbros.ui.settings.SettingsScreen
import com.brickbeam.buildbros.ui.splash.SplashScreen
import com.brickbeam.buildbros.ui.theme.BmIcons
import com.brickbeam.buildbros.ui.theme.BmType
import com.brickbeam.buildbros.ui.theme.BuildbrosTheme
import com.brickbeam.buildbros.ui.theme.LocalPalette

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "Home", BmIcons.Home),
    Tab(Routes.PROJECTS, "Projects", BmIcons.Folder),
    Tab(Routes.PRICES, "Prices", BmIcons.Tag),
    Tab(Routes.SETTINGS, "Settings", BmIcons.Tune),
)

@Composable
fun BuildbrosRoot(vm: AppViewModel, ads: AdsManager, activity: ComponentActivity) {
    val settingsOrNull by vm.settings.collectAsStateWithLifecycle()
    val settings = settingsOrNull ?: AppSettings()
    var splashDone by rememberSaveable { mutableStateOf(false) }

    BuildbrosTheme(settings.theme, settings.motion) {
        val p = LocalPalette.current
        // Status / navigation bar icons follow the app theme, not the system dark mode.
        LaunchedEffect(p.isDark, splashDone) {
            val style = if (p.isDark || !splashDone) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }

        if (!splashDone) {
            SplashScreen(motion = settings.motion, ready = settingsOrNull != null) { splashDone = true }
            return@BuildbrosTheme
        }

        val nav = rememberNavController()
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val env = remember(nav) { AppEnv(vm, nav, snackbar, ads, scope) { activity } }
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        val firstRun = remember { settingsOrNull?.hasChosenRegion == false }
        val start = remember { if (firstRun) "region?first=true" else Routes.HOME }
        val onboarding = route?.startsWith(Routes.REGION) == true && backStack?.arguments?.getBoolean("first") == true
        var showExit by remember { mutableStateOf(false) }

        BackHandler(enabled = route == Routes.HOME && !showExit) { showExit = true }

        CompositionLocalProvider(LocalAppEnv provides env) {
            AppBackground {
                Scaffold(
                    containerColor = Color.Transparent,
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = {
                        if (!onboarding) {
                            Column(Modifier.navigationBarsPadding()) {
                                if (route in Routes.TOP_LEVEL) BottomTabs(route) { tab ->
                                    nav.navigate(tab) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                BannerAdBar()
                            }
                        }
                    },
                ) { inner ->
                    val motion = settings.motion
                    NavHost(
                        navController = nav,
                        startDestination = start,
                        modifier = Modifier.padding(inner).consumeWindowInsets(inner),
                        enterTransition = {
                            if (motion) fadeIn(tween(260)) + slideInHorizontally(tween(320)) { it / 10 } else EnterTransition.None
                        },
                        exitTransition = { if (motion) fadeOut(tween(200)) else ExitTransition.None },
                        popEnterTransition = { if (motion) fadeIn(tween(260)) + scaleIn(tween(320), initialScale = 0.97f) else EnterTransition.None },
                        popExitTransition = {
                            if (motion) fadeOut(tween(200)) + slideOutHorizontally(tween(260)) { it / 10 } else ExitTransition.None
                        },
                    ) {
                        composable(
                            "region?first={first}",
                            arguments = listOf(navArgument("first") { type = NavType.BoolType; defaultValue = false }),
                        ) { entry -> RegionScreen(firstRun = entry.arguments?.getBoolean("first") == true) }
                        composable(Routes.HOME) { HomeScreen() }
                        composable(Routes.BRICK) { BrickScreen() }
                        composable(Routes.CONCRETE) { ConcreteScreen() }
                        composable(Routes.STEEL) { SteelScreen() }
                        composable(Routes.TILES) { TileScreen() }
                        composable(Routes.PAINT) { PaintScreen() }
                        composable(Routes.CONVERTER) { ConverterScreen() }
                        composable(Routes.PROJECTS) { ProjectsScreen() }
                        composable(
                            Routes.ESTIMATE,
                            arguments = listOf(navArgument("id") { type = NavType.LongType }),
                        ) { entry -> EstimateScreen(entry.arguments?.getLong("id") ?: 0L) }
                        composable(Routes.PRICES) { PricesScreen() }
                        composable(Routes.SETTINGS) { SettingsScreen() }
                    }
                }
            }

            if (showExit) {
                ExitDialog(
                    onStay = { showExit = false },
                    onExit = { showExit = false; activity.finish() },
                    onRate = {
                        showExit = false
                        val pkg = activity.packageName
                        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))) }
                            .onFailure { runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))) } }
                    },
                )
            }
        }
    }
}

@Composable
private fun BottomTabs(current: String?, onSelect: (String) -> Unit) {
    val p = LocalPalette.current
    NavigationBar(containerColor = p.surface, tonalElevation = androidx.compose.ui.unit.Dp(0f), windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)) {
        TABS.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = { onSelect(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label, style = BmType.tiny) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = p.onAccent,
                    selectedTextColor = p.accentText,
                    indicatorColor = p.accent,
                    unselectedIconColor = p.mute,
                    unselectedTextColor = p.mute,
                ),
            )
        }
    }
}
