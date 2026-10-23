package com.walkies.dogwalk.ui

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.walkies.dogwalk.AppContainer
import com.walkies.dogwalk.R
import com.walkies.dogwalk.ads.AnchoredBanner
import com.walkies.dogwalk.data.LogEntry
import com.walkies.dogwalk.data.LogType
import com.walkies.dogwalk.ui.components.ExitDialog
import com.walkies.dogwalk.ui.components.label
import com.walkies.dogwalk.ui.components.openPlayStoreListing
import com.walkies.dogwalk.ui.home.HomeScreen
import com.walkies.dogwalk.ui.log.LogScreen
import com.walkies.dogwalk.ui.reminders.RemindersScreen
import com.walkies.dogwalk.ui.settings.SettingsScreen
import com.walkies.dogwalk.ui.splash.AnimatedSplash
import com.walkies.dogwalk.ui.theme.WalkiesColors
import com.walkies.dogwalk.ui.walk.WalkScreen
import com.walkies.dogwalk.util.formatClock
import kotlinx.coroutines.launch

private object Routes {
    const val HOME = "home"
    const val WALK = "walk"
    const val LOG = "log"
    const val REMINDERS = "reminders"
    const val SETTINGS = "settings"
}

private data class Tab(val route: String, @StringRes val label: Int, @DrawableRes val icon: Int)

private val TABS = listOf(
    Tab(Routes.HOME, R.string.nav_home, R.drawable.ic_nav_home),
    Tab(Routes.WALK, R.string.nav_walk, R.drawable.ic_nav_walk),
    Tab(Routes.LOG, R.string.nav_log, R.drawable.ic_nav_log),
    Tab(Routes.REMINDERS, R.string.nav_reminders, R.drawable.ic_nav_reminders),
)

@Composable
fun WalkiesRoot(container: AppContainer, onExit: () -> Unit) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val activity = LocalActivity.current as? ComponentActivity

    // Light status-bar icons over the dark splash, dark icons over the light app.
    LaunchedEffect(showSplash) {
        val transparent = AndroidColor.TRANSPARENT
        val style = if (showSplash) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    Crossfade(targetState = showSplash, animationSpec = tween(450), label = "splash") { splash ->
        if (splash) {
            val consentResolved by container.ads.consentResolved.collectAsStateWithLifecycle()
            AnimatedSplash(ready = consentResolved, onFinished = { showSplash = false })
        } else {
            MainScaffold(container, onExit)
        }
    }
}

@Composable
private fun MainScaffold(container: AppContainer, onExit: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showExit by rememberSaveable { mutableStateOf(false) }
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)

    // Back on Home asks before leaving; other tabs go back to Home first (NavHost handles that).
    BackHandler(enabled = route == Routes.HOME || route == null) { showExit = true }

    fun showUndo(message: String, undo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, actionLabel = context.getString(R.string.undo), duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) undo()
        }
    }

    @Composable
    fun loggedHandler(): (LogEntry, () -> Unit) -> Unit {
        val names = LogType.entries.associateWith { it.label() }
        return { entry, undo ->
            showUndo(context.getString(R.string.logged_at, names[entry.type], formatClock(context, entry.timestamp)), undo)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                if (route != Routes.SETTINGS) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    ) {
                        TABS.forEach { tab ->
                            NavigationBarItem(
                                selected = route == tab.route,
                                onClick = { navController.navigateTab(tab.route) },
                                icon = { Icon(painterResource(tab.icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
                                label = { Text(stringResource(tab.label)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = WalkiesColors.Ink,
                                    selectedTextColor = WalkiesColors.Ink,
                                    indicatorColor = WalkiesColors.Walk,
                                ),
                            )
                        }
                    }
                }
                // Banner sits below the navigation bar, separated by a divider and padding,
                // so it is never mistaken for (or tapped instead of) a tab.
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AnchoredBanner(container.ads, Modifier.padding(top = 4.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    container = container,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenWalk = { navController.navigateTab(Routes.WALK) },
                    onLogged = loggedHandler(),
                )
            }
            composable(Routes.WALK) {
                WalkScreen(
                    container = container,
                    onWalkSaved = { navController.navigateTab(Routes.LOG) },
                    onLogged = loggedHandler(),
                )
            }
            composable(Routes.LOG) {
                val deletedMessage = stringResource(R.string.entry_deleted)
                LogScreen(container = container, onDeleted = { undo -> showUndo(deletedMessage, undo) })
            }
            composable(Routes.REMINDERS) { RemindersScreen(container = container) }
            composable(Routes.SETTINGS) { SettingsScreen(container = container, onBack = { navController.popBackStack() }) }
        }
    }

    if (showExit) {
        ExitDialog(
            dogName = settings?.dogName ?: stringResource(R.string.default_dog_name),
            photoUri = settings?.dogPhotoUri,
            onCancel = { showExit = false },
            onRate = {
                showExit = false
                openPlayStoreListing(context)
            },
            onExit = {
                showExit = false
                onExit()
            },
        )
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
