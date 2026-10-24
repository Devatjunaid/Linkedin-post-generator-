package com.superdevlabs.autosubtitles.ui.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.superdevlabs.autosubtitles.AppContainer
import com.superdevlabs.autosubtitles.ads.BannerAd
import com.superdevlabs.autosubtitles.ui.components.pressable
import com.superdevlabs.autosubtitles.ui.editor.EditorScreen
import com.superdevlabs.autosubtitles.ui.export.ExportScreen
import com.superdevlabs.autosubtitles.ui.home.HomeScreen
import com.superdevlabs.autosubtitles.ui.languages.LanguagesScreen
import com.superdevlabs.autosubtitles.ui.processing.ProcessingScreen
import com.superdevlabs.autosubtitles.ui.projects.ProjectsScreen
import com.superdevlabs.autosubtitles.ui.settings.SettingsScreen
import com.superdevlabs.autosubtitles.ui.splash.SplashScreen
import com.superdevlabs.autosubtitles.ui.theme.AppColors
import kotlinx.serialization.Serializable

@Serializable data object SplashRoute
@Serializable data object HomeRoute
@Serializable data object ProjectsRoute
@Serializable data object LanguagesRoute
@Serializable data object SettingsRoute
@Serializable data class ProcessingRoute(val projectId: String)
@Serializable data class EditorRoute(val projectId: String)
@Serializable data class ExportRoute(val projectId: String)

enum class TopTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    PROJECTS("Projects", Icons.Rounded.VideoLibrary),
    LANGUAGES("Languages", Icons.Rounded.Translate),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

private val springy = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

// Forward: new screen rises and un-blurs from 92% scale; old screen recedes slightly.
private val forwardEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(260)) + scaleIn(springy, initialScale = 0.92f) +
        slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 10 }
}
private val forwardExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(200)) + scaleOut(tween(300), targetScale = 1.05f)
}
private val backEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(tween(260)) + scaleIn(springy, initialScale = 1.06f)
}
private val backExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(tween(200)) + scaleOut(tween(260), targetScale = 0.9f) +
        slideOutVertically(tween(300)) { it / 8 }
}

@Composable
fun AppNavHost(container: AppContainer, showSplash: Boolean) {
    val nav = rememberNavController()
    val activity = LocalActivity.current

    NavHost(
        navController = nav,
        startDestination = if (showSplash) SplashRoute else HomeRoute,
        modifier = Modifier.fillMaxSize().background(AppColors.Ink),
        enterTransition = forwardEnter,
        exitTransition = forwardExit,
        popEnterTransition = backEnter,
        popExitTransition = backExit,
    ) {
        composable<SplashRoute>(exitTransition = { fadeOut(tween(450)) + scaleOut(tween(450), targetScale = 1.15f) }) {
            SplashScreen(onFinished = {
                nav.navigate(HomeRoute) { popUpTo<SplashRoute> { inclusive = true } }
            })
        }
        composable<HomeRoute>(enterTransition = { fadeIn(tween(500)) + scaleIn(springy, initialScale = 0.96f) }) {
            TopLevelScaffold(TopTab.HOME, nav, container, showBanner = true) { padding ->
                HomeScreen(
                    container = container,
                    contentPadding = padding,
                    onStartProject = { id ->
                        container.ads.runWithInterstitial(activity) { nav.navigate(ProcessingRoute(id)) }
                    },
                    onOpenProject = { id -> nav.navigate(EditorRoute(id)) },
                    onOpenLanguages = { nav.navigateTab(TopTab.LANGUAGES) },
                    onExit = { activity?.finish() },
                )
            }
        }
        composable<ProjectsRoute> {
            TopLevelScaffold(TopTab.PROJECTS, nav, container, showBanner = true) { padding ->
                ProjectsScreen(
                    container = container,
                    contentPadding = padding,
                    onOpenProject = { id, ready -> nav.navigate(if (ready) EditorRoute(id) else ProcessingRoute(id)) },
                )
            }
        }
        composable<LanguagesRoute> {
            TopLevelScaffold(TopTab.LANGUAGES, nav, container, showBanner = true) { padding ->
                LanguagesScreen(container = container, contentPadding = padding)
            }
        }
        composable<SettingsRoute> {
            TopLevelScaffold(TopTab.SETTINGS, nav, container, showBanner = true) { padding ->
                SettingsScreen(container = container, contentPadding = padding, onOpenLanguages = { nav.navigateTab(TopTab.LANGUAGES) })
            }
        }
        composable<ProcessingRoute> { entry ->
            val route = entry.toRoute<ProcessingRoute>()
            ProcessingScreen(
                container = container,
                projectId = route.projectId,
                onBack = { nav.popBackStack() },
                onDone = {
                    nav.navigate(EditorRoute(route.projectId)) { popUpTo<ProcessingRoute> { inclusive = true } }
                },
            )
        }
        composable<EditorRoute>(
            enterTransition = { slideInVertically(tween(420, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(300)) },
        ) { entry ->
            val route = entry.toRoute<EditorRoute>()
            EditorScreen(
                container = container,
                projectId = route.projectId,
                onBack = { nav.popBackStack() },
                onExport = { nav.navigate(ExportRoute(route.projectId)) },
            )
        }
        composable<ExportRoute> { entry ->
            val route = entry.toRoute<ExportRoute>()
            ExportScreen(
                container = container,
                projectId = route.projectId,
                onBack = { nav.popBackStack() },
                onHome = { nav.navigate(HomeRoute) { popUpTo<HomeRoute> { inclusive = true } } },
            )
        }
    }
}

private fun NavHostController.navigateTab(tab: TopTab) {
    val route: Any = when (tab) {
        TopTab.HOME -> HomeRoute
        TopTab.PROJECTS -> ProjectsRoute
        TopTab.LANGUAGES -> LanguagesRoute
        TopTab.SETTINGS -> SettingsRoute
    }
    navigate(route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Top-level screens, stacked so nothing ever sits behind anything else:
 * screen content → banner ad → navigation dock → system navigation bar.
 * Ads must never be covered by app UI (AdMob policy), so the dock is not a floating overlay.
 */
@Composable
private fun TopLevelScaffold(
    tab: TopTab,
    nav: NavHostController,
    container: AppContainer,
    showBanner: Boolean,
    content: @Composable (PaddingValues) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AppColors.Ink)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            content(PaddingValues(bottom = 8.dp))
        }
        if (showBanner) {
            BannerAd(container.ads, Modifier.padding(top = 4.dp, bottom = 8.dp))
        }
        Dock(
            current = tab,
            onSelect = { if (it != tab) nav.navigateTab(it) },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .navigationBarsPadding()
                .padding(top = 4.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun Dock(current: TopTab, onSelect: (TopTab) -> Unit, modifier: Modifier = Modifier) {
    val itemWidth = 60.dp
    val indicatorX by animateDpAsState(
        targetValue = itemWidth * current.ordinal,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
        label = "dock",
    )
    Box(
        modifier
            .shadow(24.dp, RoundedCornerShape(30.dp))
            .clip(RoundedCornerShape(30.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Line, RoundedCornerShape(30.dp))
            .padding(6.dp),
    ) {
        Box(
            Modifier
                .offset(x = indicatorX)
                .size(itemWidth, 48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.Lime),
        )
        Row {
            TopTab.entries.forEach { item ->
                val selected = item == current
                Box(
                    Modifier
                        .size(itemWidth, 48.dp)
                        .semantics { contentDescription = item.label }
                        .pressable(onClick = { onSelect(item) }, role = Role.Tab),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, contentDescription = null, tint = if (selected) AppColors.Ink else AppColors.Muted)
                }
            }
        }
    }
}

/** Used by screens that need their own status-bar inset. */
fun Modifier.screenInsets(): Modifier = this.statusBarsPadding()
