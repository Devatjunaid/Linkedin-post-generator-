package com.sunarc.inverterlogger.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sunarc.inverterlogger.ads.BannerAdSlot
import com.sunarc.inverterlogger.ui.brand.AnimatedSplash
import com.sunarc.inverterlogger.ui.brand.ExitDialog
import com.sunarc.inverterlogger.ui.components.Fmt
import com.sunarc.inverterlogger.ui.screens.FaultsScreen
import com.sunarc.inverterlogger.ui.screens.HistoryScreen
import com.sunarc.inverterlogger.ui.screens.HomeScreen
import com.sunarc.inverterlogger.ui.screens.InsightsScreen
import com.sunarc.inverterlogger.ui.screens.NetworkReadScreen
import com.sunarc.inverterlogger.ui.screens.OnboardingScreen
import com.sunarc.inverterlogger.ui.screens.ReviewScreen
import com.sunarc.inverterlogger.ui.screens.ScanScreen
import com.sunarc.inverterlogger.ui.screens.SettingsScreen
import com.sunarc.inverterlogger.ui.theme.Palette

enum class Route(val isTab: Boolean = false, val showsBanner: Boolean = false) {
    SPLASH,
    ONBOARDING,
    HOME(isTab = true, showsBanner = true),
    HISTORY(isTab = true, showsBanner = true),
    INSIGHTS(isTab = true, showsBanner = true),
    FAULTS(isTab = true, showsBanner = true),
    SETTINGS,
    SCAN,
    REVIEW,
    NETWORK
}

enum class NavAction { PUSH, POP, TAB, RESET }

@Stable
class Navigator {
    val stack = mutableStateListOf(Route.SPLASH)
    var lastAction by mutableStateOf(NavAction.RESET)
        private set

    val current: Route get() = stack.last()

    fun push(route: Route) {
        if (current == route) return
        lastAction = NavAction.PUSH
        stack.add(route)
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        lastAction = NavAction.POP
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun tab(route: Route) {
        if (current == route) return
        lastAction = NavAction.TAB
        stack.clear()
        stack.add(Route.HOME)
        if (route != Route.HOME) stack.add(route)
    }

    fun reset(route: Route) {
        lastAction = NavAction.RESET
        stack.clear()
        stack.add(route)
    }

    /** Replaces the top screen, e.g. Scan → Review so Back from Review returns to the tab. */
    fun replace(route: Route) {
        lastAction = NavAction.PUSH
        stack[stack.lastIndex] = route
    }

    fun transition(from: Route, to: Route): ContentTransform {
        val sizeT = SizeTransform(clip = false)
        return when {
            from == Route.SPLASH -> (fadeIn(tween(500)) + scaleIn(tween(600), initialScale = 1.04f)) togetherWith fadeOut(tween(350))
            // The amber scan button grows into the camera view.
            to == Route.SCAN -> (scaleIn(tween(420, easing = FastOutSlowInEasing), initialScale = 0.12f, transformOrigin = TransformOrigin(0.5f, 0.96f)) + fadeIn(tween(180))) togetherWith fadeOut(tween(260))
            from == Route.SCAN && lastAction == NavAction.POP -> fadeIn(tween(300)) togetherWith (scaleOut(tween(380), targetScale = 0.12f, transformOrigin = TransformOrigin(0.5f, 0.96f)) + fadeOut(tween(300)))
            // "Sunrise": content rises and brightens when switching tabs.
            lastAction == NavAction.TAB || (lastAction == NavAction.POP && to.isTab && from.isTab) ->
                (fadeIn(tween(320, delayMillis = 60)) + slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 16 } + scaleIn(tween(380), initialScale = 0.98f)) togetherWith
                    (fadeOut(tween(160)) + slideOutVertically(tween(200)) { -it / 40 })
            lastAction == NavAction.POP -> (slideInHorizontally(tween(340)) { -it / 5 } + fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(320)) { it / 3 } + fadeOut(tween(220)))
            else -> (slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { it / 3 } + fadeIn(tween(300))) togetherWith (slideOutHorizontally(tween(320)) { -it / 5 } + fadeOut(tween(220)))
        }.using(sizeT)
    }
}

@Composable
fun SunarcRoot(vm: AppViewModel) {
    val nav = remember { Navigator() }
    val profile by vm.profile.collectAsStateWithLifecycle()
    val dashboard by vm.dashboard.collectAsStateWithLifecycle()
    var showExit by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    BackHandler(enabled = nav.current != Route.SPLASH) {
        if (!nav.pop()) showExit = true
    }

    Box(Modifier.fillMaxSize().background(Palette.Night)) {
        Column(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = nav.current,
                transitionSpec = { nav.transition(initialState, targetState) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "screens"
            ) { route ->
                val pad = if (route == Route.SPLASH) Modifier else Modifier.statusBarsPadding()
                Box(Modifier.fillMaxSize().then(pad)) {
                    when (route) {
                        Route.SPLASH -> AnimatedSplash(onFinished = {
                            val p = vm.profile.value
                            nav.reset(if (p?.onboarded == true) Route.HOME else Route.ONBOARDING)
                        })
                        Route.ONBOARDING -> OnboardingScreen(vm, onDone = { nav.reset(Route.HOME) })
                        Route.HOME -> HomeScreen(vm, nav)
                        Route.HISTORY -> HistoryScreen(vm, nav)
                        Route.INSIGHTS -> InsightsScreen(vm, nav)
                        Route.FAULTS -> FaultsScreen(vm, nav)
                        Route.SETTINGS -> SettingsScreen(vm, nav)
                        Route.SCAN -> ScanScreen(vm, nav)
                        Route.REVIEW -> ReviewScreen(vm, nav)
                        Route.NETWORK -> NetworkReadScreen(vm, nav)
                    }
                }
            }

            val current = nav.current
            AnimatedVisibility(visible = current.showsBanner) {
                BannerAdSlot(vm.ads, Modifier.padding(top = 4.dp))
            }
            AnimatedVisibility(
                visible = current.isTab,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                MainBottomBar(current, onTab = nav::tab, onScan = { nav.push(Route.SCAN) })
            }
            if (!current.isTab && current != Route.SPLASH) Box(Modifier.navigationBarsPadding())
        }
    }

    if (showExit) {
        val today = dashboard?.todayKwh
        ExitDialog(
            todaySummary = today?.let { "You've logged ${Fmt.kwh(it)} kWh today. Your log is saved on this phone." },
            onStay = { showExit = false },
            onExit = {
                showExit = false
                activity?.finish()
            }
        )
    }
}

private data class TabItem(val route: Route, val label: String, val icon: ImageVector)

@Composable
private fun MainBottomBar(current: Route, onTab: (Route) -> Unit, onScan: () -> Unit) {
    val left = listOf(TabItem(Route.HOME, "Today", Icons.Rounded.WbTwilight), TabItem(Route.HISTORY, "History", Icons.Rounded.BarChart))
    val right = listOf(TabItem(Route.INSIGHTS, "Insights", Icons.Rounded.Lightbulb), TabItem(Route.FAULTS, "Faults", Icons.Rounded.WarningAmber))
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Palette.SurfaceHigh.copy(alpha = 0.96f))
                .border(1.dp, Palette.Line, RoundedCornerShape(26.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            left.forEach { TabButton(it, current == it.route) { onTab(it.route) } }
            Box(Modifier.size(64.dp))
            right.forEach { TabButton(it, current == it.route) { onTab(it.route) } }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-16).dp)
                .size(66.dp)
                .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = Palette.Sun, spotColor = Palette.Sun)
                .clip(RoundedCornerShape(22.dp))
                .background(Palette.Sun)
                .border(5.dp, Palette.Night, RoundedCornerShape(22.dp))
                .clickable(role = Role.Button, onClickLabel = "Scan inverter screen", onClick = onScan)
                .semantics { contentDescription = "Scan inverter screen" },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.DocumentScanner, contentDescription = null, tint = Palette.OnSun, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun TabButton(item: TabItem, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) Palette.Sun else Palette.Muted
    Column(
        Modifier
            .widthIn(min = 60.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics { role = Role.Tab; this.selected = selected }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(item.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(item.label, fontSize = 11.sp, color = color)
    }
}
