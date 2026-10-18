package com.devatjunaid.balconysolar.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devatjunaid.balconysolar.BuildConfig
import com.devatjunaid.balconysolar.MainViewModel
import com.devatjunaid.balconysolar.R
import com.devatjunaid.balconysolar.ads.AdsManager
import com.devatjunaid.balconysolar.ads.BannerAdSlot
import com.devatjunaid.balconysolar.ads.NativeAdCard
import com.devatjunaid.balconysolar.domain.Report
import com.devatjunaid.balconysolar.sensors.DeviceLocation
import com.devatjunaid.balconysolar.sensors.MeasureDialog
import com.devatjunaid.balconysolar.ui.components.AppIcons
import com.devatjunaid.balconysolar.ui.exit.ExitDialog
import com.devatjunaid.balconysolar.ui.screens.AboutScreen
import com.devatjunaid.balconysolar.ui.screens.LocationScreen
import com.devatjunaid.balconysolar.ui.screens.MeasureKind
import com.devatjunaid.balconysolar.ui.screens.PaybackScreen
import com.devatjunaid.balconysolar.ui.screens.SavedScreen
import com.devatjunaid.balconysolar.ui.screens.SetupScreen
import com.devatjunaid.balconysolar.ui.screens.SunScreen
import com.devatjunaid.balconysolar.ui.screens.YieldScreen
import com.devatjunaid.balconysolar.ui.screens.fieldColors
import com.devatjunaid.balconysolar.ui.splash.SplashScreen
import com.devatjunaid.balconysolar.ui.theme.Palette
import com.devatjunaid.balconysolar.ui.theme.Type
import kotlinx.coroutines.launch

enum class Screen(val step: Int?, val tab: String?, val icon: ImageVector?) {
    LOCATION(1, "Location", AppIcons.Pin),
    SETUP(2, "Setup", AppIcons.Sliders),
    SUN(3, "Sun", AppIcons.SunArc),
    YIELD(4, "Yield", AppIcons.Bars),
    PAYBACK(5, "Payback", AppIcons.Coin),
    SAVED(null, null, null),
    ABOUT(null, null, null),
}

@Composable
fun AppRoot(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val refine by vm.refine.collectAsStateWithLifecycle()
    val scan by vm.scan.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val privacyOptions by AdsManager.privacyOptionsRequired.collectAsStateWithLifecycle()

    var showSplash by rememberSaveable { mutableStateOf(true) }
    var screen by rememberSaveable { mutableStateOf(Screen.LOCATION) }
    var returnTo by rememberSaveable { mutableStateOf(Screen.LOCATION) }
    var showExit by rememberSaveable { mutableStateOf(false) }
    var measure by rememberSaveable { mutableStateOf<MeasureKind?>(null) }
    var saveDialog by rememberSaveable { mutableStateOf(false) }
    var locationDisclosure by rememberSaveable { mutableStateOf(false) }
    var gpsBusy by remember { mutableStateOf(false) }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    fun go(to: Screen) {
        if (to == Screen.SAVED || to == Screen.ABOUT) {
            if (screen != Screen.SAVED && screen != Screen.ABOUT) returnTo = screen
        }
        screen = to
    }
    /** Major actions get an interstitial at the transition, subject to the 30-second cap. */
    fun withInterstitial(action: () -> Unit) {
        if (activity != null) AdsManager.showInterstitialThen(activity, action) else action()
    }
    fun fetchLocation() {
        gpsBusy = true
        scope.launch {
            val loc = DeviceLocation.current(context)
            gpsBusy = false
            if (loc != null) vm.useDeviceLocation(loc.latitude, loc.longitude)
            else Toast.makeText(context, "Location unavailable. Turn on location or enter coordinates.", Toast.LENGTH_LONG).show()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetchLocation()
        else Toast.makeText(context, "No problem: pick a city or enter coordinates instead.", Toast.LENGTH_LONG).show()
    }

    BackHandler(enabled = !showExit) {
        when (screen) {
            Screen.LOCATION -> showExit = true
            Screen.SAVED, Screen.ABOUT -> screen = returnTo
            else -> screen = Screen.entries[(screen.step ?: 1) - 2]
        }
    }

    Column(Modifier.fillMaxSize().background(Palette.Night)) {
        Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding().imePadding()) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    val forward = (targetState.step ?: 9) >= (initialState.step ?: 9)
                    (slideInHorizontally { if (forward) it / 6 else -it / 6 } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it / 6 else it / 6 } + fadeOut())
                },
                label = "screens",
            ) { s ->
                when (s) {
                    Screen.LOCATION -> LocationScreen(
                        state = state, gpsBusy = gpsBusy, refine = refine, onState = vm::update,
                        onUseGps = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) fetchLocation()
                            else locationDisclosure = true
                        },
                        onRefine = vm::refineWithPvgis,
                        onNext = { go(Screen.SETUP) },
                        onOpenSaved = { go(Screen.SAVED) },
                        onOpenAbout = { go(Screen.ABOUT) },
                    )
                    Screen.SETUP -> SetupScreen(
                        state = state, scan = scan, onState = vm::update,
                        onMeasure = { measure = it },
                        onScanTilt = vm::scanTilt,
                        onBack = { go(Screen.LOCATION) },
                        onCalculate = { withInterstitial { go(Screen.YIELD) } },
                    )
                    Screen.SUN -> SunScreen(state, results, onBack = { go(Screen.SETUP) }, onNext = { go(Screen.YIELD) })
                    Screen.YIELD -> YieldScreen(
                        state, results, vm::update, onBack = { go(Screen.SUN) },
                        onNext = { withInterstitial { go(Screen.PAYBACK) } },
                        nativeAd = { NativeAdCard() },
                    )
                    Screen.PAYBACK -> PaybackScreen(
                        state, results, vm::update, onBack = { go(Screen.YIELD) },
                        onSave = { saveDialog = true },
                        onShare = {
                            val r = results ?: return@PaybackScreen
                            withInterstitial { share(context, Report.build(state, r)) }
                        },
                        nativeAd = { NativeAdCard() },
                    )
                    Screen.SAVED -> SavedScreen(
                        saved,
                        onLoad = { id -> vm.load(id); screen = Screen.YIELD },
                        onDelete = vm::delete,
                        onBack = { screen = returnTo },
                        nativeAd = { NativeAdCard() },
                    )
                    Screen.ABOUT -> AboutScreen(
                        versionName = BuildConfig.VERSION_NAME,
                        privacyOptionsAvailable = privacyOptions,
                        onPrivacyOptions = { activity?.let(AdsManager::showPrivacyOptions) },
                        onPrivacyPolicy = { openUrl(context, context.getString(R.string.privacy_policy_url)) },
                        onBack = { screen = returnTo },
                    )
                }
            }
        }
        BottomNav(current = screen, onSelect = { go(it) })
        Spacer(Modifier.fillMaxWidth().height(4.dp).background(Palette.NightDeep))
        Box(Modifier.fillMaxWidth().background(Palette.NightDeep).navigationBarsPadding()) { BannerAdSlot() }
    }

    measure?.let { kind ->
        MeasureDialog(kind, state.latitude, state.longitude, onDismiss = { measure = null }) { v ->
            vm.update(if (kind == MeasureKind.TILT) state.copy(pv = state.pv.copy(tiltDeg = v)) else state.copy(pv = state.pv.copy(azimuthDeg = v)))
            measure = null
        }
    }
    if (saveDialog) SaveDialog(defaultName = "${state.custom?.label ?: state.city.name} · ${state.pv.panelCount}×${state.pv.panelWp.toInt()} Wp", onDismiss = { saveDialog = false }) { name ->
        saveDialog = false
        withInterstitial {
            vm.save(name)
            Toast.makeText(context, "Saved “$name”", Toast.LENGTH_SHORT).show()
        }
    }
    if (locationDisclosure) {
        AlertDialog(
            onDismissRequest = { locationDisclosure = false },
            containerColor = Palette.SurfaceHigh,
            icon = { Icon(AppIcons.Pin, null, tint = Palette.Mint) },
            title = { Text("Use your approximate location?", style = Type.title, color = Palette.Ink) },
            text = {
                Text(
                    "Balcony Solar uses your approximate location once, to calculate sun angles and pick climate data for where you are. " +
                        "It is kept on this phone and never sold or shared for advertising.",
                    style = Type.body, color = Palette.Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = { locationDisclosure = false; permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) {
                    Text("Continue", color = Palette.Amber)
                }
            },
            dismissButton = { TextButton(onClick = { locationDisclosure = false }) { Text("Not now", color = Palette.Muted) } },
        )
    }
    if (showExit) {
        ExitDialog(
            annualKwh = results?.yield?.annualKwh,
            onStay = { showExit = false },
            onExit = { showExit = false; activity?.finish() },
        )
    }
}

@Composable
private fun BottomNav(current: Screen, onSelect: (Screen) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Palette.Panel).padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (s in Screen.entries) {
            val tab = s.tab ?: continue
            val on = s == current
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clickable(role = Role.Tab) { onSelect(s) }
                    .semantics { selected = on },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(s.icon!!, contentDescription = null, tint = if (on) Palette.Amber else Palette.Muted, modifier = Modifier.size(22.dp))
                Text(tab, style = if (on) Type.labelStrong.copy(fontSize = Type.label.fontSize) else Type.label, color = if (on) Palette.Amber else Palette.Muted)
            }
        }
    }
}

@Composable
private fun SaveDialog(defaultName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.SurfaceHigh,
        title = { Text("Save this setup", style = Type.title, color = Palette.Ink) },
        text = { OutlinedTextField(name, { name = it.take(60) }, singleLine = true, label = { Text("Name") }, colors = fieldColors(), modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim()) }) { Text("Save", color = Palette.Amber) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.Muted) } },
    )
}

private fun share(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "My balcony solar estimate")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share report"))
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, url, Toast.LENGTH_LONG).show()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
