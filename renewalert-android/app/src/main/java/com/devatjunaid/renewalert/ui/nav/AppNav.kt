package com.devatjunaid.renewalert.ui.nav

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devatjunaid.renewalert.ads.AdPlacement
import com.devatjunaid.renewalert.ads.BannerAd
import com.devatjunaid.renewalert.domain.Formats
import com.devatjunaid.renewalert.ui.AppViewModel
import com.devatjunaid.renewalert.ui.backup.BackupScreen
import com.devatjunaid.renewalert.ui.calendar.CalendarScreen
import com.devatjunaid.renewalert.ui.detail.DetailScreen
import com.devatjunaid.renewalert.ui.edit.EditScreen
import com.devatjunaid.renewalert.ui.home.HomeScreen
import com.devatjunaid.renewalert.ui.insights.InsightsScreen
import com.devatjunaid.renewalert.ui.onboarding.OnboardingScreen
import com.devatjunaid.renewalert.ui.settings.SettingsScreen
import com.devatjunaid.renewalert.ui.settings.rateApp
import com.devatjunaid.renewalert.ui.splash.SplashScreen
import com.devatjunaid.renewalert.ui.subs.SubscriptionsScreen
import com.devatjunaid.renewalert.ui.theme.Brand
import com.devatjunaid.renewalert.ui.theme.RenewAlertTheme
import com.devatjunaid.renewalert.ui.trials.TrialsScreen

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SUBS = "subs"
    const val CALENDAR = "calendar"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{id}"
    const val EDIT = "edit?id={id}"
    const val TRIALS = "trials"
    const val BACKUP = "backup"

    fun detail(id: Long) = "detail/$id"
    fun edit(id: Long? = null) = if (id == null) "edit" else "edit?id=$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val placement: AdPlacement?)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Rounded.Home, null),
    Tab(Routes.SUBS, "Subs", Icons.AutoMirrored.Rounded.List, null),
    Tab(Routes.CALENDAR, "Calendar", Icons.Rounded.CalendarMonth, AdPlacement.OPEN_CALENDAR),
    Tab(Routes.INSIGHTS, "Insights", Icons.Rounded.Insights, AdPlacement.OPEN_INSIGHTS),
    Tab(Routes.SETTINGS, "Settings", Icons.Rounded.Settings, null),
)

private val tabRoutes = tabs.map { it.route }.toSet()

/** Banner only on browsing screens; never on splash, onboarding or while typing in a form. */
private val bannerRoutes = tabRoutes + setOf(Routes.DETAIL, Routes.TRIALS, Routes.BACKUP)

@Composable
fun RenewAlertRoot(
    vm: AppViewModel,
    consentDone: Boolean,
    pendingDetailId: Long?,
    onDetailConsumed: () -> Unit,
    onExit: () -> Unit,
    onRouteChanged: (String?) -> Unit,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val settings by vm.settings.collectAsState()
    val c = RenewAlertTheme.colors
    val activity = LocalContext.current as Activity
    var showExit by remember { mutableStateOf(false) }

    LaunchedEffect(route) { onRouteChanged(route) }

    val afterAction: (AdPlacement, () -> Unit) -> Unit = { placement, then ->
        vm.ads.showInterstitial(activity, placement, then)
    }

    // Deep link from a reminder notification.
    LaunchedEffect(pendingDetailId, route) {
        val id = pendingDetailId ?: return@LaunchedEffect
        if (route != null && route != Routes.SPLASH && route != Routes.ONBOARDING) {
            nav.navigate(Routes.detail(id)) { launchSingleTop = true }
            onDetailConsumed()
        }
    }

    BackHandler(enabled = route == Routes.HOME) { showExit = true }

    val showTabs = route in tabRoutes
    val showBanner = route in bannerRoutes

    Scaffold(
        containerColor = if (route == Routes.SPLASH) Brand.Navy else c.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showTabs || showBanner) {
                Column(Modifier.fillMaxWidth().background(c.surface)) {
                    HorizontalDivider(color = c.outline)
                    if (showTabs) {
                        NavigationBar(containerColor = c.surface, tonalElevation = 0.dp, windowInsets = WindowInsets(0)) {
                            tabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = route == tab.route,
                                    onClick = {
                                        if (route == tab.route) return@NavigationBarItem
                                        val go = { nav.navigateTab(tab.route) }
                                        if (tab.placement != null) afterAction(tab.placement, go) else go()
                                    },
                                    icon = { Icon(tab.icon, contentDescription = null) },
                                    label = { Text(tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = c.accent,
                                        selectedTextColor = c.accent,
                                        indicatorColor = c.accentSoft,
                                        unselectedIconColor = c.muted,
                                        unselectedTextColor = c.muted,
                                    ),
                                )
                            }
                        }
                    }
                    if (showBanner) BannerAd(vm.ads)
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        },
    ) { inner ->
        val needsStatusPadding = route != Routes.SPLASH && route != Routes.ONBOARDING
        Box(
            Modifier.fillMaxSize().padding(inner).consumeWindowInsets(inner)
                .then(if (needsStatusPadding) Modifier.statusBarsPadding() else Modifier),
        ) {
            NavHost(
                navController = nav,
                startDestination = Routes.SPLASH,
                enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 } },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 12 } },
            ) {
                composable(Routes.SPLASH) {
                    SplashScreen(ready = consentDone && settings != null) {
                        val next = if (settings?.onboardingDone == true) Routes.HOME else Routes.ONBOARDING
                        nav.navigate(next) { popUpTo(Routes.SPLASH) { inclusive = true } }
                    }
                }
                composable(Routes.ONBOARDING) {
                    OnboardingScreen { currency ->
                        vm.completeOnboarding(currency)
                        nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    }
                }
                composable(Routes.HOME) {
                    HomeScreen(
                        vm,
                        onOpenDetail = { nav.navigate(Routes.detail(it)) },
                        onAdd = { nav.navigate(Routes.edit()) },
                        onOpenTrials = { afterAction(AdPlacement.OPEN_TRIALS) { nav.navigate(Routes.TRIALS) } },
                        onOpenCalendar = { afterAction(AdPlacement.OPEN_CALENDAR) { nav.navigateTab(Routes.CALENDAR) } },
                        onOpenSubs = { nav.navigateTab(Routes.SUBS) },
                        onOpenInsights = { afterAction(AdPlacement.OPEN_INSIGHTS) { nav.navigateTab(Routes.INSIGHTS) } },
                    )
                }
                composable(Routes.SUBS) {
                    SubscriptionsScreen(vm, onOpenDetail = { nav.navigate(Routes.detail(it)) }, onAdd = { nav.navigate(Routes.edit()) })
                }
                composable(Routes.CALENDAR) { CalendarScreen(vm) { nav.navigate(Routes.detail(it)) } }
                composable(Routes.INSIGHTS) { InsightsScreen(vm) { nav.navigate(Routes.detail(it)) } }
                composable(Routes.SETTINGS) { SettingsScreen(vm) { nav.navigate(Routes.BACKUP) } }
                composable(Routes.TRIALS) {
                    TrialsScreen(vm, onBack = { nav.popBackStack() }, onOpenDetail = { nav.navigate(Routes.detail(it)) }, afterAction = afterAction)
                }
                composable(Routes.BACKUP) { BackupScreen(vm, onBack = { nav.popBackStack() }, afterAction = afterAction) }
                composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    val id = e.arguments?.getLong("id") ?: return@composable
                    DetailScreen(
                        vm, id,
                        onBack = { nav.popBackStack() },
                        onEdit = { nav.navigate(Routes.edit(it)) },
                        afterAction = afterAction,
                    )
                }
                composable(
                    Routes.EDIT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
                ) { e ->
                    val id = e.arguments?.getLong("id")?.takeIf { it > 0 }
                    EditScreen(vm, id, onClose = { nav.popBackStack() }, afterAction = afterAction)
                }
            }
        }
    }

    if (showExit) {
        val p by vm.portfolio.collectAsState()
        val next = remember(p) { p.upcoming(7).firstOrNull() }
        ExitDialog(
            upcomingText = next?.let {
                "Next up: ${it.sub.name} ${Formats.relativeDay(p.today, it.date)}. We'll remind you before it renews."
            },
            onStay = { showExit = false },
            onExit = { showExit = false; onExit() },
            onRate = { showExit = false; rateApp(activity) },
        )
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
