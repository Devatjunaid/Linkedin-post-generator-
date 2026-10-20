package com.superdevlabs.coldplunge.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.superdevlabs.coldplunge.ads.AdaptiveBanner
import com.superdevlabs.coldplunge.ads.rememberNativeAd
import com.superdevlabs.coldplunge.domain.PhaseType
import com.superdevlabs.coldplunge.domain.Protocol
import com.superdevlabs.coldplunge.domain.ProtocolPlanner
import com.superdevlabs.coldplunge.domain.SessionPlan
import com.superdevlabs.coldplunge.graph
import com.superdevlabs.coldplunge.ui.components.AmbientBackground
import com.superdevlabs.coldplunge.ui.components.PlungeBottomBar
import com.superdevlabs.coldplunge.ui.components.Tab
import com.superdevlabs.coldplunge.ui.exit.ExitDialog
import com.superdevlabs.coldplunge.ui.home.HomeScreen
import com.superdevlabs.coldplunge.ui.home.QuickStartSheet
import com.superdevlabs.coldplunge.ui.log.LogScreen
import com.superdevlabs.coldplunge.ui.log.ManualEntryDialog
import com.superdevlabs.coldplunge.ui.onboarding.SafetyOnboarding
import com.superdevlabs.coldplunge.ui.protocols.ProtocolEditorScreen
import com.superdevlabs.coldplunge.ui.protocols.ProtocolsScreen
import com.superdevlabs.coldplunge.ui.settings.SettingsScreen
import com.superdevlabs.coldplunge.ui.splash.SplashScreen
import com.superdevlabs.coldplunge.ui.theme.K
import com.superdevlabs.coldplunge.ui.timer.TimerScreen
import com.superdevlabs.coldplunge.util.CsvExporter

enum class Stage { Splash, Onboarding, Main }

sealed interface Route {
    data class Main(val tab: Tab) : Route
    data class Timer(val plan: SessionPlan, val id: Long = System.nanoTime()) : Route
    data class Editor(val protocol: Protocol?) : Route
    data object SafetyGuide : Route
}

/** Navigation state lives in a ViewModel so rotation never restarts the splash or drops a running session. */
class NavViewModel : ViewModel() {
    var stage by mutableStateOf(Stage.Splash)
    val stack = mutableStateListOf<Route>(Route.Main(Tab.Home))
    var forward by mutableStateOf(true)
        private set
    var consentRequested = false

    val top: Route get() = stack.last()

    fun push(route: Route) {
        forward = true
        stack.add(route)
    }

    fun pop() {
        if (stack.size > 1) {
            forward = false
            stack.removeAt(stack.lastIndex)
        }
    }

    fun selectTab(tab: Tab) {
        val current = (stack.first() as? Route.Main)?.tab ?: Tab.Home
        forward = tab.ordinal >= current.ordinal
        stack.clear()
        stack.add(Route.Main(tab))
    }
}

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun PlungeRoot(
    appVm: AppViewModel = viewModel(),
    nav: NavViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val graph = context.graph
    val settings by appVm.settings.collectAsStateWithLifecycle()
    var splashDone by rememberSaveable { mutableStateOf(nav.stage != Stage.Splash) }

    // Leave the splash once both the animation and the first settings read are done.
    LaunchedEffect(splashDone, settings) {
        val s = settings
        if (splashDone && s != null && nav.stage == Stage.Splash) {
            nav.stage = if (s.onboardingDone) Stage.Main else Stage.Onboarding
        }
    }
    // Ask for ad consent only after the splash, never before the user sees the app.
    LaunchedEffect(nav.stage) {
        if (nav.stage != Stage.Splash && !nav.consentRequested && activity != null) {
            nav.consentRequested = true
            graph.consent.gatherConsent(activity)
        }
    }
    val adsReady by graph.consent.adsReady.collectAsStateWithLifecycle()
    LaunchedEffect(adsReady) { if (adsReady) graph.interstitials.preload() }

    Box(Modifier.fillMaxSize().background(K.Ink)) {
        AnimatedContent(
            targetState = nav.stage,
            transitionSpec = { (fadeIn(tween(500)) + scaleIn(initialScale = 1.08f)) togetherWith fadeOut(tween(300)) },
            label = "stage",
        ) { stage ->
            when (stage) {
                Stage.Splash -> SplashScreen(onFinished = { splashDone = true })
                Stage.Onboarding -> SafetyOnboarding(onAccept = {
                    appVm.completeOnboarding()
                    nav.stage = Stage.Main
                })
                Stage.Main -> settings?.let { MainShell(appVm, nav, activity) }
            }
        }
    }
}

@Composable
private fun MainShell(appVm: AppViewModel, nav: NavViewModel, activity: Activity?) {
    val context = LocalContext.current
    val graph = context.graph
    val settings by appVm.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    val sessions by appVm.sessions.collectAsStateWithLifecycle()
    val custom by appVm.customProtocols.collectAsStateWithLifecycle()
    val stats by appVm.stats.collectAsStateWithLifecycle()
    val week by appVm.week.collectAsStateWithLifecycle()
    val featured by appVm.featuredProtocol.collectAsStateWithLifecycle()
    val privacyRequired by graph.consent.privacyOptionsRequired.collectAsStateWithLifecycle()

    var quickStart by rememberSaveable { mutableStateOf<String?>(null) } // "COLD", "HEAT", "ANY"
    var showExit by rememberSaveable { mutableStateOf(false) }
    var showManual by rememberSaveable { mutableStateOf(false) }

    // One native ad per tab (never the same ad in two places), held while the shell is visible.
    val homeAd = rememberNativeAd()
    val protocolsAd = rememberNativeAd()
    val logAd = rememberNativeAd()

    fun withInterstitial(action: () -> Unit) {
        if (activity != null) graph.interstitials.showThen(activity, action) else action()
    }

    fun startPlan(plan: SessionPlan) {
        quickStart = null
        withInterstitial { nav.push(Route.Timer(plan)) }
    }

    val top = nav.top
    BackHandler {
        when {
            nav.stack.size > 1 -> nav.pop()
            top is Route.Main && top.tab != Tab.Home -> nav.selectTab(Tab.Home)
            else -> showExit = true
        }
    }

    val isMain = top is Route.Main
    val showBanner = top is Route.Main || top is Route.Editor

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AmbientBackground(warmth = if (top is Route.Main && top.tab == Tab.Protocols) 0.7f else 0.4f)
            AnimatedContent(
                targetState = top,
                transitionSpec = { navTransition(initialState, targetState, nav.forward) },
                label = "nav",
            ) { route ->
                when (route) {
                    is Route.Main -> when (route.tab) {
                        Tab.Home -> HomeScreen(
                            settings = s,
                            stats = stats,
                            week = week,
                            featured = featured,
                            lastSession = sessions.firstOrNull(),
                            nativeAd = homeAd,
                            onQuickStart = { quickStart = it.name },
                            onStartProtocol = { startPlan(ProtocolPlanner.plan(it)) },
                            onEditProtocol = { nav.push(Route.Editor(it)) },
                            onOpenReminders = { nav.selectTab(Tab.Settings) },
                            onOpenLog = { nav.selectTab(Tab.Log) },
                        )
                        Tab.Protocols -> ProtocolsScreen(
                            custom = custom,
                            favoriteId = featured.id,
                            fahrenheit = s.fahrenheit,
                            nativeAd = protocolsAd,
                            onStart = { startPlan(ProtocolPlanner.plan(it)) },
                            onEdit = { nav.push(Route.Editor(it)) },
                            onDelete = { appVm.deleteProtocol(it.id) },
                            onFeature = { appVm.setFavorite(it) },
                        )
                        Tab.Log -> LogScreen(
                            sessions = sessions,
                            stats = stats,
                            week = week,
                            settings = s,
                            nativeAd = logAd,
                            onDelete = { appVm.deleteSession(it.id) },
                            onAddManual = { showManual = true },
                            onExport = {
                                withInterstitial { CsvExporter.exportAndShare(context, sessions) }
                            },
                        )
                        Tab.Settings -> SettingsScreen(
                            settings = s,
                            privacyOptionsRequired = privacyRequired,
                            onFahrenheit = appVm::setFahrenheit,
                            onSound = appVm::setSound,
                            onVibration = appVm::setVibration,
                            onBreathing = appVm::setBreathingGuide,
                            onColdGoal = appVm::setColdGoal,
                            onHeatGoal = appVm::setHeatGoal,
                            onReminder = { enabled, h, m -> appVm.setReminder(enabled, h, m) },
                            onPrivacyOptions = { activity?.let { graph.consent.showPrivacyOptions(it) } },
                            onSafetyGuide = { nav.push(Route.SafetyGuide) },
                        )
                    }
                    is Route.Timer -> TimerScreen(
                        plan = route.plan,
                        sessionId = route.id,
                        settings = s,
                        onSave = { record ->
                            appVm.saveSession(record)
                            withInterstitial {
                                nav.selectTab(Tab.Log)
                            }
                        },
                        onDiscard = { nav.pop() },
                    )
                    is Route.Editor -> ProtocolEditorScreen(
                        initial = route.protocol,
                        fahrenheit = s.fahrenheit,
                        onBack = { nav.pop() },
                        onSave = { p ->
                            appVm.saveProtocol(p) {
                                withInterstitial { nav.selectTab(Tab.Protocols) }
                            }
                        },
                        onStart = { p -> startPlan(ProtocolPlanner.plan(p)) },
                    )
                    Route.SafetyGuide -> SafetyOnboarding(onAccept = { nav.pop() }, acceptLabel = "Got it")
                }
            }

            // Fully qualified: inside Column > Box the ColumnScope overload would otherwise be picked.
            androidx.compose.animation.AnimatedVisibility(
                visible = isMain,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(spring(dampingRatio = 0.7f)) { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                PlungeBottomBar(
                    selected = (top as? Route.Main)?.tab ?: Tab.Home,
                    onSelect = nav::selectTab,
                    onStart = { quickStart = "ANY" },
                    modifier = if (showBanner) Modifier else Modifier.navigationBarsPadding(),
                )
            }
        }
        if (showBanner) {
            Box(Modifier.fillMaxWidth().background(K.InkDeep).navigationBarsPadding()) {
                AdaptiveBanner()
            }
        }
    }

    quickStart?.let { mode ->
        QuickStartSheet(
            initial = when (mode) {
                "COLD" -> PhaseType.COLD
                "HEAT" -> PhaseType.HEAT
                else -> null
            },
            settings = s,
            featured = featured,
            onDismiss = { quickStart = null },
            onStartSingle = { type, sec, temp ->
                appVm.rememberQuickStart(type, sec, temp)
                startPlan(ProtocolPlanner.single(type, sec, temp))
            },
            onStartProtocol = { startPlan(ProtocolPlanner.plan(it)) },
            onBrowseProtocols = {
                quickStart = null
                nav.selectTab(Tab.Protocols)
            },
        )
    }

    if (showManual) {
        ManualEntryDialog(
            fahrenheit = s.fahrenheit,
            defaultColdTemp = s.lastColdTempC,
            defaultHeatTemp = s.lastHeatTempC,
            onDismiss = { showManual = false },
            onSave = {
                appVm.saveSession(it)
                showManual = false
            },
        )
    }

    if (showExit) {
        ExitDialog(
            streak = stats.currentStreak,
            weekSessions = week.sessions,
            onStay = { showExit = false },
            onExit = {
                showExit = false
                activity?.finish()
            },
        )
    }
}

/**
 * Tabs slide sideways in the direction of travel; opening a session or editor rises up with a
 * slight zoom (like stepping into the water); going back sinks it away.
 */
private fun navTransition(from: Route, to: Route, forward: Boolean): ContentTransform {
    val tabSwitch = from is Route.Main && to is Route.Main
    return if (tabSwitch) {
        val dir = if (forward) 1 else -1
        (slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { it * dir / 4 } + fadeIn(tween(320))) togetherWith
            (slideOutHorizontally(tween(420, easing = FastOutSlowInEasing)) { -it * dir / 4 } + fadeOut(tween(200)))
    } else if (forward) {
        (slideInVertically(spring(dampingRatio = 0.85f, stiffness = 300f)) { it / 3 } + scaleIn(initialScale = 0.92f) + fadeIn(tween(300))) togetherWith
            (scaleOut(targetScale = 1.06f, animationSpec = tween(350)) + fadeOut(tween(250)))
    } else {
        (scaleIn(initialScale = 1.06f, animationSpec = tween(350)) + fadeIn(tween(300))) togetherWith
            (slideOutVertically(tween(350)) { it / 3 } + scaleOut(targetScale = 0.92f) + fadeOut(tween(250)))
    }
}
