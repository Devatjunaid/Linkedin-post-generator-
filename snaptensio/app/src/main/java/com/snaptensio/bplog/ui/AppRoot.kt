package com.snaptensio.bplog.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.snaptensio.bplog.ads.BannerAd
import com.snaptensio.bplog.appContainer
import com.snaptensio.bplog.domain.BpClassifier
import com.snaptensio.bplog.ui.screens.CrisisScreen
import com.snaptensio.bplog.ui.screens.EditReadingScreen
import com.snaptensio.bplog.ui.screens.HistoryScreen
import com.snaptensio.bplog.ui.screens.HomeScreen
import com.snaptensio.bplog.ui.screens.InsightsScreen
import com.snaptensio.bplog.ui.screens.MedsScreen
import com.snaptensio.bplog.ui.screens.OnboardingScreen
import com.snaptensio.bplog.ui.screens.ReportScreen
import com.snaptensio.bplog.ui.screens.ScanScreen
import com.snaptensio.bplog.ui.screens.SessionScreen
import com.snaptensio.bplog.ui.screens.SettingsScreen
import com.snaptensio.bplog.ui.screens.SplashScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val HISTORY = "history"
    const val TRENDS = "trends"
    const val MEDS = "meds"
    const val SCAN = "scan"
    const val EDIT = "edit"
    const val SESSION = "session"
    const val CRISIS = "crisis"
    const val INSIGHTS = "insights"
    const val REPORT = "report"
    const val SETTINGS = "settings"

    /** Key used to hand a scanned "sys,dia,pulse" back to the screen that opened the camera. */
    const val SCAN_RESULT = "scan_result"

    fun scan(returnResult: Boolean = false) = "$SCAN?mode=${if (returnResult) "return" else "new"}"
    fun edit(id: Long = 0, sys: Int = -1, dia: Int = -1, pulse: Int = -1, source: String = "MANUAL") =
        "$EDIT?id=$id&sys=$sys&dia=$dia&pulse=$pulse&source=$source"
    fun crisis(sys: Int, dia: Int) = "$CRISIS?sys=$sys&dia=$dia"
}

private val TABS = setOf(Routes.HOME, Routes.HISTORY, Routes.TRENDS, Routes.MEDS)
private val BANNER_ROUTES = TABS + setOf(Routes.INSIGHTS, Routes.REPORT, Routes.SETTINGS)

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Shows an interstitial (if allowed by the frequency cap) at a natural break, then continues. */
@Composable
fun rememberInterstitial(): (() -> Unit) -> Unit {
    val context = LocalContext.current
    val ads = context.appContainer.ads
    return { next -> ads.showInterstitialThen(context.findActivity(), next) }
}

@Composable
fun AppRoot(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var splashDone by rememberSaveable { mutableStateOf(false) }
    val s = settings
    if (!splashDone || s == null) {
        SplashScreen(onFinished = { splashDone = true })
        return
    }

    val nav = rememberNavController()
    // Fixed once, so finishing onboarding doesn't rebuild the graph.
    val start = rememberSaveable { if (s.onboarded) Routes.HOME else Routes.ONBOARDING }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route?.substringBefore('?')
    val interstitial = rememberInterstitial()

    fun afterReadingSaved(sys: Int, dia: Int) {
        if (s.crisisAlerts && BpClassifier.isCrisis(sys, dia)) {
            // Safety first: no ad, go straight to guidance.
            nav.navigate(Routes.crisis(sys, dia)) { popUpTo(Routes.HOME) }
        } else {
            nav.popBackStack()
            interstitial {}
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                if (route in BANNER_ROUTES) BannerAd(Modifier.padding(top = 2.dp))
                if (route in TABS) BottomBar(route, nav)
            }
        },
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.padding(pad),
            enterTransition = { fadeIn() + slideInHorizontally { it / 12 } },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() + slideOutHorizontally { it / 12 } },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(vm) {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                }
            }
            composable(Routes.HOME) { HomeScreen(vm, nav) }
            composable(Routes.HISTORY) { HistoryScreen(vm, nav) }
            composable(Routes.TRENDS) { TrendsScreenRoute(vm, nav, interstitial) }
            composable(Routes.MEDS) { MedsScreen(vm, onSaved = { interstitial {} }) }
            composable(
                "${Routes.SCAN}?mode={mode}",
                arguments = listOf(navArgument("mode") { type = NavType.StringType; defaultValue = "new" }),
            ) { e ->
                val returnResult = e.arguments?.getString("mode") == "return"
                ScanScreen(
                    onClose = { nav.popBackStack() },
                    onResult = { sys, dia, pulse ->
                        if (returnResult) {
                            nav.previousBackStackEntry?.savedStateHandle?.set(Routes.SCAN_RESULT, "$sys,$dia,${pulse ?: -1}")
                            nav.popBackStack()
                        } else {
                            nav.popBackStack()
                            nav.navigate(Routes.edit(sys = sys, dia = dia, pulse = pulse ?: -1, source = "SCAN"))
                        }
                    },
                    onTypeInstead = {
                        if (returnResult) nav.popBackStack()
                        else {
                            nav.popBackStack()
                            nav.navigate(Routes.edit())
                        }
                    },
                )
            }
            composable(
                "${Routes.EDIT}?id={id}&sys={sys}&dia={dia}&pulse={pulse}&source={source}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("sys") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("dia") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("pulse") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("source") { type = NavType.StringType; defaultValue = "MANUAL" },
                ),
            ) { e ->
                val a = e.arguments!!
                EditReadingScreen(
                    vm = vm,
                    readingId = a.getLong("id"),
                    initialSys = a.getInt("sys").takeIf { it > 0 },
                    initialDia = a.getInt("dia").takeIf { it > 0 },
                    initialPulse = a.getInt("pulse").takeIf { it > 0 },
                    source = a.getString("source") ?: "MANUAL",
                    onBack = { nav.popBackStack() },
                    onSaved = ::afterReadingSaved,
                    onDeleted = { nav.popBackStack() },
                    onRescan = { nav.navigate(Routes.scan()) { popUpTo(Routes.HOME) } },
                )
            }
            composable(Routes.SESSION) { e ->
                SessionScreen(
                    vm = vm,
                    backStackEntry = e,
                    onClose = { nav.popBackStack() },
                    onScan = { nav.navigate(Routes.scan(returnResult = true)) },
                    onSaved = ::afterReadingSaved,
                )
            }
            composable(
                "${Routes.CRISIS}?sys={sys}&dia={dia}",
                arguments = listOf(
                    navArgument("sys") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("dia") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { e ->
                CrisisScreen(
                    sys = e.arguments!!.getInt("sys"),
                    dia = e.arguments!!.getInt("dia"),
                    settings = s,
                    onMeasureAgain = { nav.navigate(Routes.scan()) { popUpTo(Routes.HOME) } },
                    onDone = { nav.popBackStack(Routes.HOME, inclusive = false) },
                )
            }
            composable(Routes.INSIGHTS) { InsightsScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Routes.REPORT) { ReportScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsScreen(vm, onBack = { nav.popBackStack() }, onImported = { interstitial {} }) }
        }
    }
}

@Composable
private fun TrendsScreenRoute(vm: MainViewModel, nav: NavHostController, interstitial: (() -> Unit) -> Unit) {
    com.snaptensio.bplog.ui.screens.TrendsScreen(
        vm = vm,
        onInsights = { interstitial { nav.navigate(Routes.INSIGHTS) } },
        onReport = { interstitial { nav.navigate(Routes.REPORT) } },
    )
}

@Composable
private fun BottomBar(route: String?, nav: NavHostController) {
    fun go(r: String) {
        if (route == r) return
        nav.navigate(r) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem("Home", Icons.Filled.Home, route == Routes.HOME) { go(Routes.HOME) }
            TabItem("History", Icons.AutoMirrored.Filled.FormatListBulleted, route == Routes.HISTORY) { go(Routes.HISTORY) }
            Surface(
                onClick = { nav.navigate(Routes.scan()) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp,
                modifier = Modifier.size(60.dp).offset(y = (-14).dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Scan monitor", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                }
            }
            TabItem("Trends", Icons.AutoMirrored.Filled.ShowChart, route == Routes.TRENDS) { go(Routes.TRENDS) }
            TabItem("Meds", Icons.Filled.Medication, route == Routes.MEDS) { go(Routes.MEDS) }
        }
    }
}

@Composable
private fun TabItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.semantics { this.selected = selected; role = Role.Tab },
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}
