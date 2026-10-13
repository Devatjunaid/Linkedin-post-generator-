package com.aquadoselab.poolchemistry.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aquadoselab.poolchemistry.ads.BannerAd
import com.aquadoselab.poolchemistry.ui.screens.DosingPlanScreen
import com.aquadoselab.poolchemistry.ui.screens.GuideScreen
import com.aquadoselab.poolchemistry.ui.screens.HistoryScreen
import com.aquadoselab.poolchemistry.ui.screens.HomeScreen
import com.aquadoselab.poolchemistry.ui.screens.PoolEditScreen
import com.aquadoselab.poolchemistry.ui.screens.SettingsScreen
import com.aquadoselab.poolchemistry.ui.screens.SplashScreen
import com.aquadoselab.poolchemistry.ui.screens.TestEntryScreen
import com.aquadoselab.poolchemistry.ui.theme.AppColors

object Routes {
    const val HOME = "home"
    const val TEST = "test"
    const val HISTORY = "history"
    const val GUIDE = "guide"
    const val SETTINGS = "settings"
    const val PLAN = "plan/{testId}"
    const val POOL = "pool?id={id}"

    fun plan(testId: String) = "plan/$testId"
    fun pool(id: String? = null) = if (id == null) "pool" else "pool?id=$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.TEST, "Test", Icons.Filled.Science),
    Tab(Routes.HISTORY, "History", Icons.Filled.ShowChart),
    Tab(Routes.GUIDE, "Guide", Icons.AutoMirrored.Filled.MenuBook),
    Tab(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

@Composable
fun AppRoot(activity: Activity) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    AnimatedContent(
        targetState = showSplash,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "splash"
    ) { splash ->
        if (splash) {
            SplashScreen(activity = activity, onFinished = { showSplash = false })
        } else {
            MainScaffold()
        }
    }
}

/** Switch to a bottom tab, keeping one copy of each tab on the back stack. */
fun NavHostController.goToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun MainScaffold() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val onTab = tabs.any { it.route == route }

    Scaffold(
        containerColor = AppColors.Ground,
        bottomBar = {
            Column(Modifier.background(Color.White)) {
                HorizontalDivider(color = AppColors.Border)
                BannerAd()
                if (onTab) {
                    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = route == tab.route,
                                onClick = { nav.goToTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AppColors.TealDark,
                                    selectedTextColor = AppColors.TealDark,
                                    indicatorColor = AppColors.OkBg,
                                    unselectedIconColor = AppColors.Muted,
                                    unselectedTextColor = AppColors.Muted
                                )
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .consumeWindowInsets(inner)
                .imePadding()
        ) {
            composable(Routes.HOME) { HomeScreen(vm, nav) }
            composable(Routes.TEST) { TestEntryScreen(vm, nav) }
            composable(Routes.HISTORY) { HistoryScreen(vm, nav) }
            composable(Routes.GUIDE) { GuideScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
            composable(
                Routes.PLAN,
                arguments = listOf(navArgument("testId") { type = NavType.StringType })
            ) { e ->
                DosingPlanScreen(vm, nav, e.arguments?.getString("testId").orEmpty())
            }
            composable(
                Routes.POOL,
                arguments = listOf(navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { e ->
                PoolEditScreen(vm, nav, e.arguments?.getString("id"))
            }
        }
    }
}
