package com.devatjunaid.examclock.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devatjunaid.examclock.LocalAppContainer
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.ads.BannerAd
import com.devatjunaid.examclock.ui.exams.CatalogScreen
import com.devatjunaid.examclock.ui.exams.EditExamScreen
import com.devatjunaid.examclock.ui.exams.ExamDetailScreen
import com.devatjunaid.examclock.ui.exams.ExamsScreen
import com.devatjunaid.examclock.ui.focus.FocusScreen
import com.devatjunaid.examclock.ui.home.HomeScreen
import com.devatjunaid.examclock.ui.onboarding.GuideScreen
import com.devatjunaid.examclock.ui.onboarding.SetupScreen
import com.devatjunaid.examclock.ui.onboarding.SplashScreen
import com.devatjunaid.examclock.ui.planner.AutoPlanScreen
import com.devatjunaid.examclock.ui.planner.PlannerScreen
import com.devatjunaid.examclock.ui.settings.PrivacyPolicyScreen
import com.devatjunaid.examclock.ui.settings.SettingsScreen
import com.devatjunaid.examclock.ui.stats.StatsScreen

object Routes {
    const val SPLASH = "splash"
    const val GUIDE = "guide"
    const val SETUP = "setup"
    const val HOME = "home"
    const val EXAMS = "exams"
    const val PLANNER = "planner"
    const val FOCUS = "focus"
    const val FOCUS_PATTERN = "focus?sessionId={sessionId}"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val EXAM_PATTERN = "exam/{id}"
    const val EDIT_PATTERN = "exam_edit?id={id}"
    const val CATALOG = "catalog"
    const val AUTOPLAN = "autoplan"

    fun exam(id: Long) = "exam/$id"
    fun edit(id: Long?) = if (id == null) "exam_edit" else "exam_edit?id=$id"
    fun focus(sessionId: Long) = "focus?sessionId=$sessionId"
}

private data class Tab(val route: String, val pattern: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, Routes.HOME, R.string.tab_home, Icons.Rounded.Home),
    Tab(Routes.EXAMS, Routes.EXAMS, R.string.tab_exams, Icons.Rounded.Event),
    Tab(Routes.PLANNER, Routes.PLANNER, R.string.tab_planner, Icons.Rounded.CalendarMonth),
    Tab(Routes.FOCUS, Routes.FOCUS_PATTERN, R.string.tab_focus, Icons.Rounded.Timer),
    Tab(Routes.STATS, Routes.STATS, R.string.tab_progress, Icons.Rounded.Insights),
)

@Composable
fun AppNavigation(onboardingDone: Boolean) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val currentTab = tabs.firstOrNull { it.pattern == route }
    val ads = LocalAppContainer.current.ads
    val activity = LocalActivity.current
    val context = LocalContext.current
    var showExit by remember { mutableStateOf(false) }

    /** Runs [then] after an interstitial (if one is ready and the 30 s cap allows it). */
    fun afterInterstitial(then: () -> Unit) = ads.interstitial.showIfAllowed(activity, then)

    BackHandler(enabled = route == Routes.HOME) { showExit = true }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentTab != null) {
                Column {
                    if (currentTab.route != Routes.FOCUS) BannerAd()
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = tab == currentTab,
                                onClick = { if (tab != currentTab) nav.navigateTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(stringResource(tab.label), maxLines = 1) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.SPLASH,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(onFinished = {
                    nav.navigate(if (onboardingDone) Routes.HOME else Routes.GUIDE) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                })
            }
            composable(Routes.GUIDE) {
                GuideScreen(onDone = { nav.navigate(Routes.SETUP) })
            }
            composable(Routes.SETUP) {
                SetupScreen(
                    onFinished = {
                        nav.navigate(Routes.HOME) { popUpTo(nav.graph.id) { inclusive = true } }
                    },
                    onExit = { nav.popBackStack() },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenExam = { nav.navigate(Routes.exam(it)) },
                    onEditExam = { nav.navigate(Routes.edit(it)) },
                    onAddExam = { nav.navigate(Routes.edit(null)) },
                    onBrowseCatalog = { nav.navigate(Routes.CATALOG) },
                    onOpenPlanner = { nav.navigateTab(Routes.PLANNER) },
                    onBuildPlan = { nav.navigate(Routes.AUTOPLAN) },
                    onStartFocus = { nav.navigateTab(Routes.focus(it)) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.EXAMS) {
                ExamsScreen(
                    onOpenExam = { nav.navigate(Routes.exam(it)) },
                    onAddExam = { nav.navigate(Routes.edit(null)) },
                    onBrowseCatalog = { nav.navigate(Routes.CATALOG) },
                )
            }
            composable(Routes.PLANNER) {
                PlannerScreen(
                    onAutoPlan = { nav.navigate(Routes.AUTOPLAN) },
                    onStartFocus = { nav.navigateTab(Routes.focus(it)) },
                )
            }
            composable(
                Routes.FOCUS_PATTERN,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType; defaultValue = 0L }),
            ) { backStack ->
                val sessionId = backStack.arguments?.getLong("sessionId")?.takeIf { it != 0L }
                FocusScreen(
                    sessionId = sessionId,
                    onFinished = { afterInterstitial { nav.navigateTab(Routes.HOME) } },
                )
            }
            composable(Routes.STATS) { StatsScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onPrivacyPolicy = { nav.navigate(Routes.PRIVACY) },
                    onReset = { nav.navigate(Routes.GUIDE) { popUpTo(nav.graph.id) { inclusive = true } } },
                )
            }
            composable(Routes.PRIVACY) { PrivacyPolicyScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.EXAM_PATTERN, arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                val id = backStack.arguments?.getLong("id") ?: 0L
                ExamDetailScreen(
                    examId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.edit(id)) },
                    onPlanRevision = { nav.navigate(Routes.AUTOPLAN) },
                    onFocus = { nav.navigateTab(Routes.FOCUS) },
                )
            }
            composable(
                Routes.EDIT_PATTERN,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L }),
            ) { backStack ->
                val id = backStack.arguments?.getLong("id")?.takeIf { it != 0L }
                EditExamScreen(
                    examId = id,
                    onBack = { nav.popBackStack() },
                    onSaved = { isNew ->
                        if (isNew) afterInterstitial { nav.popBackStack() } else nav.popBackStack()
                    },
                )
            }
            composable(Routes.CATALOG) {
                CatalogScreen(
                    onBack = { nav.popBackStack() },
                    onAdded = { afterInterstitial { nav.navigateTab(Routes.EXAMS) } },
                )
            }
            composable(Routes.AUTOPLAN) {
                AutoPlanScreen(
                    onBack = { nav.popBackStack() },
                    onViewPlan = {
                        afterInterstitial {
                            nav.navigateTab(Routes.PLANNER)
                        }
                    },
                )
            }
        }
    }

    if (showExit) {
        ExitDialog(
            onCancel = { showExit = false },
            onRate = {
                showExit = false
                ExternalLinks.openStoreListing(context)
            },
            onExit = {
                showExit = false
                activity?.finish()
            },
        )
    }
}

/** Switches bottom-navigation tabs keeping Home as the single root, so Back always returns home. */
private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = route.substringBefore('?') != Routes.FOCUS }
        launchSingleTop = true
        restoreState = route.substringBefore('?') != Routes.FOCUS
    }
}
