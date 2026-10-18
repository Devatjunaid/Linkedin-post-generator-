package app.layerline.toolkit3d.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.layerline.toolkit3d.LayerlineApp
import app.layerline.toolkit3d.ads.BannerAdSlot
import app.layerline.toolkit3d.ui.bench.WorkbenchScreen
import app.layerline.toolkit3d.ui.calib.CalibrateScreen
import app.layerline.toolkit3d.ui.components.GantryNavBar
import app.layerline.toolkit3d.ui.components.Tab
import app.layerline.toolkit3d.ui.components.bedDots
import app.layerline.toolkit3d.ui.cost.CostScreen
import app.layerline.toolkit3d.ui.cost.QuotesScreen
import app.layerline.toolkit3d.ui.log.LogScreen
import app.layerline.toolkit3d.ui.machines.MachinesScreen
import app.layerline.toolkit3d.ui.settings.SettingsScreen
import app.layerline.toolkit3d.ui.stock.StockScreen
import app.layerline.toolkit3d.ui.theme.LL

object Routes {
    const val MACHINES = "machines"
    const val SETTINGS = "settings"
    const val QUOTES = "quotes"
}

@Composable
fun LayerlineRoot(app: LayerlineApp) {
    var splashDone by rememberSaveable { mutableStateOf(false) }
    AnimatedContent(
        targetState = splashDone,
        transitionSpec = { (fadeIn(tween(450)) + scaleIn(tween(450), initialScale = 0.97f)) togetherWith fadeOut(tween(300)) },
        modifier = Modifier.fillMaxSize().background(LL.Graphite),
        label = "splash",
    ) { done ->
        if (!done) SplashScreen(onFinished = { splashDone = true }) else MainShell(app)
    }
}

@Composable
private fun MainShell(app: LayerlineApp) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Tab.Bench.route
    val tab = Tab.entries.firstOrNull { it.route == route } ?: Tab.Bench
    val activity = LocalActivity.current
    var showExit by remember { mutableStateOf(false) }

    val ctx = remember(nav, activity) {
        ScreenContext(
            app = app,
            open = { r ->
                val isTab = Tab.entries.any { it.route == r }
                nav.navigate(r) {
                    if (isTab) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        restoreState = true
                    }
                    launchSingleTop = true
                }
            },
            back = { nav.popBackStack() },
            majorAction = { app.ads.showInterstitialAtBreak(activity) },
        )
    }

    // Back on the Workbench asks before leaving; other screens pop back as usual.
    BackHandler(enabled = route == Tab.Bench.route && !showExit) { showExit = true }

    Column(
        Modifier
            .fillMaxSize()
            .background(LL.Graphite)
            .bedDots(),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            NavHost(
                navController = nav,
                startDestination = Tab.Bench.route,
                enterTransition = { fadeIn(tween(240)) + slideInVertically(tween(260)) { it / 24 } },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(240)) },
                popExitTransition = { fadeOut(tween(160)) },
            ) {
                composable(Tab.Bench.route) { WorkbenchScreen(ctx) }
                composable(Tab.Cost.route) { CostScreen(ctx) }
                composable(Tab.Stock.route) { StockScreen(ctx) }
                composable(Tab.Calib.route) { CalibrateScreen(ctx) }
                composable(Tab.Log.route) { LogScreen(ctx) }
                composable(Routes.MACHINES) { MachinesScreen(ctx) }
                composable(Routes.SETTINGS) { SettingsScreen(ctx) }
                composable(Routes.QUOTES) { QuotesScreen(ctx) }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(LL.Nav)
                .navigationBarsPadding(),
        ) {
            GantryNavBar(selected = tab, onSelect = { ctx.open(it.route) })
            BannerAdSlot(app.ads)
        }
    }

    if (showExit) {
        ExitDialog(
            onStay = { showExit = false },
            onExit = {
                showExit = false
                activity?.finish()
            },
        )
    }
}
