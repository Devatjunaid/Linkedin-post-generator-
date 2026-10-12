package app.gigometer.mileage.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import app.gigometer.mileage.ads.Ads
import app.gigometer.mileage.ads.BannerAd
import app.gigometer.mileage.data.Trip
import app.gigometer.mileage.tracking.Permissions
import app.gigometer.mileage.ui.screens.AnimatedSplash
import app.gigometer.mileage.ui.screens.HomeScreen
import app.gigometer.mileage.ui.screens.LiveTripScreen
import app.gigometer.mileage.ui.screens.ReportsScreen
import app.gigometer.mileage.ui.screens.SettingsActions
import app.gigometer.mileage.ui.screens.SettingsScreen
import app.gigometer.mileage.ui.screens.TripFilter
import app.gigometer.mileage.ui.screens.TripSheet
import app.gigometer.mileage.ui.screens.TripsScreen
import app.gigometer.mileage.ui.screens.newManualTrip
import app.gigometer.mileage.ui.theme.LocalPalette

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    TRIPS("Trips", Icons.AutoMirrored.Filled.FormatListBulleted),
    REPORTS("Reports", Icons.Filled.BarChart),
    SETTINGS("Settings", Icons.Filled.Settings),
}

@Composable
fun AppRoot(vm: MainViewModel, openRequest: String?, onOpenHandled: () -> Unit, onSplashDone: () -> Unit) {
    var splash by rememberSaveable { mutableStateOf(true) }
    AnimatedContent(splash, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "splash") { showSplash ->
        if (showSplash) AnimatedSplash { splash = false; onSplashDone() }
        else MainScaffold(vm, openRequest, onOpenHandled)
    }
}

@Composable
private fun MainScaffold(vm: MainViewModel, openRequest: String?, onOpenHandled: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsState()
    val trips by vm.trips.collectAsState()
    val home by vm.home.collectAsState()
    val live by vm.live.collectAsState()
    val privacyRequired by Ads.privacyOptionsRequired.collectAsState()

    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var showLive by rememberSaveable { mutableStateOf(false) }
    var tripsFilter by rememberSaveable { mutableStateOf(TripFilter.ALL) }
    var editing by remember { mutableStateOf<Trip?>(null) }

    // Permissions can change in system settings while we're in the background.
    var permTick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) permTick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val autoReady = remember(permTick) { Permissions.autoReady(ctx) }
    LaunchedEffect(autoReady) { vm.resyncAutoDetect() }

    val perms = rememberPermissionController(onFinished = { permTick++; vm.resyncAutoDetect() })

    LaunchedEffect(openRequest) {
        when (openRequest) {
            "live" -> showLive = true
            "trips" -> { showLive = false; tab = Tab.TRIPS; tripsFilter = TripFilter.REVIEW }
        }
        if (openRequest != null) onOpenHandled()
    }

    val startTrip: () -> Unit = {
        perms.startManual {
            if (vm.startManualTrip()) {
                showLive = true
            } else {
                Toast.makeText(ctx, "Couldn't start GPS. Check location permission.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = p.ground,
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                BannerAd()
                if (!showLive) {
                    NavigationBar(containerColor = p.card, tonalElevation = androidx.compose.ui.unit.Dp(0f), windowInsets = androidx.compose.foundation.layout.WindowInsets(0)) {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t; if (t == Tab.TRIPS) tripsFilter = TripFilter.ALL },
                                icon = { Icon(t.icon, contentDescription = null) },
                                label = { Text(t.label, fontWeight = if (tab == t) FontWeight.ExtraBold else FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = p.accent, selectedTextColor = p.accent, indicatorColor = p.accentSoft,
                                    unselectedIconColor = p.muted, unselectedTextColor = p.muted,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            if (showLive) {
                androidx.activity.compose.BackHandler { showLive = false }
                LiveTripScreen(
                    live = live, settings = settings,
                    onBack = { showLive = false },
                    onStart = startTrip,
                    onPause = { vm.pauseTrip() },
                    onResume = { vm.resumeTrip() },
                    onEnd = { vm.endTrip(); showLive = false; tab = Tab.TRIPS; tripsFilter = TripFilter.ALL },
                )
            } else {
                if (tab != Tab.HOME) androidx.activity.compose.BackHandler { tab = Tab.HOME }
                when (tab) {
                    Tab.HOME -> HomeScreen(
                        state = home, settings = settings, live = live, autoReady = autoReady,
                        onStartTrip = startTrip,
                        onOpenLive = { showLive = true },
                        onOpenTrips = { tab = Tab.TRIPS; tripsFilter = TripFilter.ALL },
                        onOpenReview = { tab = Tab.TRIPS; tripsFilter = TripFilter.REVIEW },
                        onOpenTrip = { editing = it },
                        onSetupAuto = { vm.setAutoDetect(true); perms.startAutoSetup() },
                        onOpenSettings = { tab = Tab.SETTINGS },
                    )
                    Tab.TRIPS -> TripsScreen(
                        trips = trips, settings = settings, initialFilter = tripsFilter,
                        onClassify = { t, c -> vm.classify(t, c) },
                        onOpenTrip = { editing = it },
                        onAddTrip = { editing = newManualTrip() },
                    )
                    Tab.REPORTS -> ReportsScreen(trips, settings, onExportPdf = { vm.exportPdf(it) }, onExportCsv = { vm.exportCsv(it) })
                    Tab.SETTINGS -> SettingsScreen(
                        settings, autoReady,
                        SettingsActions(
                            setAutoDetect = vm::setAutoDetect,
                            setBatterySaver = vm::setBatterySaver,
                            setReminder = { on -> vm.setReminder(on); if (on && !Permissions.hasNotifications(ctx)) perms.startAutoSetup() },
                            setLookup = vm::setLookupAddresses,
                            setWorkAuto = vm::setWorkAutoBusiness,
                            setWorkHours = vm::setWorkHours,
                            setPlatforms = vm::setPlatforms,
                            setName = vm::setDriverName,
                            addRate = vm::addRate,
                            removeRate = vm::removeRate,
                            resetRates = vm::resetRates,
                            backup = vm::backupTo,
                            restore = vm::restoreFrom,
                            setupPermissions = { perms.startAutoSetup() },
                            privacyOptions = if (privacyRequired) ({ (ctx as? Activity)?.let(Ads::showPrivacyOptions) }) else null,
                        ),
                    )
                }
            }
        }
    }

    editing?.let { t ->
        TripSheet(
            trip = t, settings = settings,
            onDismiss = { editing = null },
            onSave = { vm.saveTrip(it); editing = null },
            onDelete = { vm.deleteTrip(it); editing = null },
        )
    }
}
