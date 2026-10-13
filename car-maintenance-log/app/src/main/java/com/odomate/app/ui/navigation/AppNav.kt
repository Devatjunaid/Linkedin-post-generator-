package com.odomate.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.odomate.app.ads.BannerAd
import com.odomate.app.domain.Level
import com.odomate.app.ui.LocalAppContainer
import com.odomate.app.ui.LocalSnackbar
import com.odomate.app.ui.exit.ExitDialog
import com.odomate.app.ui.fuel.FuelScreen
import com.odomate.app.ui.history.HistoryScreen
import com.odomate.app.ui.home.HomeScreen
import com.odomate.app.ui.log.LogServiceScreen
import com.odomate.app.ui.scan.ScanScreen
import com.odomate.app.ui.services.ServiceDetailScreen
import com.odomate.app.ui.services.ServiceEditScreen
import com.odomate.app.ui.services.ServicesScreen
import com.odomate.app.ui.settings.SettingsScreen
import com.odomate.app.ui.theme.AppTheme
import com.odomate.app.ui.vehicles.VehicleEditScreen
import com.odomate.app.ui.vehicles.VehiclesScreen
import com.odomate.app.util.findActivity
import com.odomate.app.util.openPlayStore

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SERVICES = "services"
    const val HISTORY = "history"
    const val MORE = "more"
    const val SCAN = "scan?mode={mode}"
    const val SERVICE = "service/{id}"
    const val SERVICE_EDIT = "service_edit?id={id}"
    const val LOG = "log?recordId={recordId}&serviceId={serviceId}"
    const val FUEL = "fuel?id={id}"
    const val VEHICLES = "vehicles"
    const val VEHICLE_EDIT = "vehicle_edit?id={id}"

    fun scan(pick: Boolean = false) = "scan?mode=${if (pick) "pick" else "save"}"
    fun service(id: Long) = "service/$id"
    fun serviceEdit(id: Long = 0) = "service_edit?id=$id"
    fun log(recordId: Long = 0, serviceId: Long = 0) = "log?recordId=$recordId&serviceId=$serviceId"
    fun fuel(id: Long = 0) = "fuel?id=$id"
    fun vehicleEdit(id: Long = 0) = "vehicle_edit?id=$id"

    val tabs = setOf(HOME, SERVICES, HISTORY, MORE)
}

/** Where a notification asked us to go. */
sealed interface LaunchTarget {
    data class Service(val serviceId: Long, val vehicleId: Long) : LaunchTarget
    data object Scan : LaunchTarget
}

private const val KEY_SCANNED = "scanned_odometer"

private fun idArg(name: String = "id") = navArgument(name) { type = NavType.LongType; defaultValue = 0L }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppNav(startOnboarding: Boolean, launchTarget: LaunchTarget?, onLaunchTargetHandled: () -> Unit) {
    val nav = rememberNavController()
    val container = LocalAppContainer.current
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current
    val c = AppTheme.colors
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val isTab = route in Routes.tabs
    val imeVisible = WindowInsets.isImeVisible
    val showBanner = route != null && route != Routes.SCAN && route != Routes.ONBOARDING && !imeVisible
    val showBar = isTab && !imeVisible
    var showExit by rememberSaveable { mutableStateOf(false) }
    val snapshot by container.repository.activeSnapshot.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(launchTarget, route) {
        if (launchTarget == null || route == null || route == Routes.ONBOARDING) return@LaunchedEffect
        when (launchTarget) {
            is LaunchTarget.Service -> {
                if (launchTarget.vehicleId != 0L) container.repository.setActiveVehicle(launchTarget.vehicleId)
                if (launchTarget.serviceId != 0L) nav.navigate(Routes.service(launchTarget.serviceId))
            }
            LaunchTarget.Scan -> nav.navigate(Routes.scan())
        }
        onLaunchTargetHandled()
    }

    BackHandler(enabled = route == Routes.HOME) { showExit = true }

    Scaffold(
        containerColor = c.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
        bottomBar = {
            if (showBanner || showBar) {
                Column(Modifier.background(c.bg).navigationBarsPadding()) {
                    if (showBanner) {
                        HorizontalDivider(color = c.divider)
                        BannerAd()
                    }
                    if (showBar) BottomBar(route) { target -> if (target == "scan") nav.navigate(Routes.scan()) else nav.navigateTab(target) }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.HOME,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { fadeIn(tween(180)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            composable(Routes.ONBOARDING) { entry ->
                val scanned by entry.savedStateHandle.getStateFlow<Int?>(KEY_SCANNED, null).collectAsStateWithLifecycle()
                VehicleEditScreen(
                    onboarding = true,
                    scannedOdometer = scanned,
                    onConsumeScanned = { entry.savedStateHandle[KEY_SCANNED] = null },
                    onScan = { nav.navigate(Routes.scan(pick = true)) },
                    onBack = null,
                    onSaved = {
                        nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
                    },
                    onGarageEmpty = {},
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onScan = { nav.navigate(Routes.scan()) },
                    onOpenService = { nav.navigate(Routes.service(it)) },
                    onSeeServices = { nav.navigateTab(Routes.SERVICES) },
                    onLogService = { nav.navigate(Routes.log()) },
                    onAddFuel = { nav.navigate(Routes.fuel()) },
                    onManageVehicles = { nav.navigate(Routes.VEHICLES) },
                    onAddVehicle = { nav.navigate(Routes.vehicleEdit()) },
                )
            }
            composable(Routes.SERVICES) {
                ServicesScreen(
                    onOpenService = { nav.navigate(Routes.service(it)) },
                    onAddCustom = { nav.navigate(Routes.serviceEdit()) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onOpenRecord = { nav.navigate(Routes.log(recordId = it)) },
                    onOpenFuel = { nav.navigate(Routes.fuel(it)) },
                )
            }
            composable(Routes.MORE) {
                SettingsScreen(onManageVehicles = { nav.navigate(Routes.VEHICLES) })
            }
            composable(
                Routes.SCAN,
                arguments = listOf(navArgument("mode") { type = NavType.StringType; defaultValue = "save" }),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
            ) {
                ScanScreen(
                    onClose = { nav.popBackStack() },
                    onPicked = { value ->
                        nav.previousBackStackEntry?.savedStateHandle?.set(KEY_SCANNED, value)
                        nav.popBackStack()
                    },
                    onSaved = { nav.popBackStack() },
                )
            }
            composable(Routes.SERVICE, arguments = listOf(idArg())) {
                ServiceDetailScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.serviceEdit(it)) },
                    onLog = { nav.navigate(Routes.log(serviceId = it)) },
                    onOpenRecord = { nav.navigate(Routes.log(recordId = it)) },
                )
            }
            composable(Routes.SERVICE_EDIT, arguments = listOf(idArg())) {
                ServiceEditScreen(
                    onBack = { nav.popBackStack() },
                    onDeleted = { nav.popBackStack(Routes.SERVICES, inclusive = false).let { popped -> if (!popped) nav.navigateTab(Routes.SERVICES) } },
                )
            }
            composable(Routes.LOG, arguments = listOf(idArg("recordId"), idArg("serviceId"))) { entry ->
                val scanned by entry.savedStateHandle.getStateFlow<Int?>(KEY_SCANNED, null).collectAsStateWithLifecycle()
                LogServiceScreen(
                    scannedOdometer = scanned,
                    onConsumeScanned = { entry.savedStateHandle[KEY_SCANNED] = null },
                    onScan = { nav.navigate(Routes.scan(pick = true)) },
                    onBack = { nav.popBackStack() },
                    onDone = { nav.popBackStack() },
                )
            }
            composable(Routes.FUEL, arguments = listOf(idArg())) { entry ->
                val scanned by entry.savedStateHandle.getStateFlow<Int?>(KEY_SCANNED, null).collectAsStateWithLifecycle()
                FuelScreen(
                    scannedOdometer = scanned,
                    onConsumeScanned = { entry.savedStateHandle[KEY_SCANNED] = null },
                    onScan = { nav.navigate(Routes.scan(pick = true)) },
                    onBack = { nav.popBackStack() },
                    onDone = { nav.popBackStack() },
                )
            }
            composable(Routes.VEHICLES) {
                VehiclesScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.vehicleEdit(it)) },
                    onAdd = { nav.navigate(Routes.vehicleEdit()) },
                )
            }
            composable(Routes.VEHICLE_EDIT, arguments = listOf(idArg())) { entry ->
                val scanned by entry.savedStateHandle.getStateFlow<Int?>(KEY_SCANNED, null).collectAsStateWithLifecycle()
                VehicleEditScreen(
                    onboarding = false,
                    scannedOdometer = scanned,
                    onConsumeScanned = { entry.savedStateHandle[KEY_SCANNED] = null },
                    onScan = { nav.navigate(Routes.scan(pick = true)) },
                    onBack = { nav.popBackStack() },
                    onSaved = { nav.popBackStack() },
                    onGarageEmpty = { nav.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } } },
                )
            }
        }
    }

    if (showExit) {
        ExitDialog(
            attentionCount = snapshot?.statuses?.count { it.level == Level.OVERDUE || it.level == Level.SOON } ?: 0,
            onStay = { showExit = false },
            onExit = {
                showExit = false
                context.findActivity()?.finish()
            },
            onRate = {
                showExit = false
                context.openPlayStore()
            },
        )
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        // Home is always the root once onboarding is done.
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(current: String?, onSelect: (String) -> Unit) {
    val c = AppTheme.colors
    Box(Modifier.fillMaxWidth().background(c.surface)) {
        HorizontalDivider(color = c.divider)
        Row(
            Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(Icons.Outlined.Home, "Home", current == Routes.HOME, Modifier.weight(1f)) { onSelect(Routes.HOME) }
            TabItem(Icons.Outlined.Build, "Services", current == Routes.SERVICES, Modifier.weight(1f)) { onSelect(Routes.SERVICES) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .offset(y = (-14).dp)
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(c.accent)
                        .border(4.dp, c.bg, CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Scan odometer") { onSelect("scan") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Scan odometer", tint = c.onAccent, modifier = Modifier.size(28.dp))
                }
            }
            TabItem(Icons.Outlined.History, "History", current == Routes.HISTORY, Modifier.weight(1f)) { onSelect(Routes.HISTORY) }
            TabItem(Icons.Outlined.MoreHoriz, "More", current == Routes.MORE, Modifier.weight(1f)) { onSelect(Routes.MORE) }
        }
    }
}

@Composable
private fun TabItem(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = AppTheme.colors
    val color = if (selected) c.accentText else c.muted
    Column(
        modifier
            .clip(CircleShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}
