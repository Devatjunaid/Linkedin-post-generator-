package com.shelfguard.app.ui.navigation

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shelfguard.app.AppContainer
import com.shelfguard.app.ads.AdaptiveBanner
import com.shelfguard.app.ads.InterstitialAdManager
import com.shelfguard.app.data.prefs.AppSettings
import com.shelfguard.app.domain.AlertSummary
import com.shelfguard.app.notifications.NotificationHelper
import com.shelfguard.app.ui.alerts.AlertsScreen
import com.shelfguard.app.ui.components.ExitDialog
import com.shelfguard.app.ui.dashboard.DashboardScreen
import com.shelfguard.app.ui.editor.ProductEditorScreen
import com.shelfguard.app.ui.inventory.InventoryScreen
import com.shelfguard.app.ui.product.ProductDetailScreen
import com.shelfguard.app.ui.reports.ReportsScreen
import com.shelfguard.app.ui.settings.SettingsScreen
import com.shelfguard.app.util.containerViewModel
import com.shelfguard.app.util.findActivity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RootViewModel(container: AppContainer) : ViewModel() {
    val alertCount = container.inventory.observeStock()
        .map { AlertSummary.from(it).total }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val settings = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
}

/** Navigation actions handed to screens, so screens never touch the NavController directly. */
class AppNavigator(private val nav: NavHostController, private val context: Context) {
    private val activity get() = context.findActivity()

    private fun goTopLevel(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun tab(tab: TopLevel) = when (tab) {
        // Natural breaks where a (frequency-capped) interstitial is acceptable.
        TopLevel.STOCK, TopLevel.REPORTS -> InterstitialAdManager.showIfEligible(activity) { goTopLevel(tab.navRoute) }
        else -> goTopLevel(tab.navRoute)
    }

    fun inventory(filter: String) = InterstitialAdManager.showIfEligible(activity) {
        nav.navigate(Routes.inventory(filter)) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = false }
            launchSingleTop = true
        }
    }

    fun reports() = tab(TopLevel.REPORTS)
    fun alerts() = goTopLevel(Routes.ALERTS)
    fun settings() = nav.navigate(Routes.SETTINGS) { launchSingleTop = true }
    fun product(id: Long) = nav.navigate(Routes.product(id)) { launchSingleTop = true }
    fun newProduct(barcode: String = "") = nav.navigate(Routes.newProduct(barcode))
    fun editProduct(id: Long) = nav.navigate(Routes.editProduct(id))
    fun back() = nav.popBackStack()

    /** After saving: leave the editor, maybe via an interstitial, and show the product. */
    fun afterSave(productId: Long, wasNew: Boolean) {
        InterstitialAdManager.showIfEligible(activity) {
            nav.popBackStack()
            if (wasNew) product(productId)
        }
    }
}

@Composable
fun AppRoot(openAlertsRequest: Boolean, onAlertsRequestHandled: () -> Unit, onExit: () -> Unit) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val navigator = remember(nav) { AppNavigator(nav, context) }
    val vm = containerViewModel { RootViewModel(it) }
    val alertCount by vm.alertCount.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val currentTab = TopLevel.entries.firstOrNull { it.route == route }
    val showBanner = route != null && route != Routes.EDITOR

    var showExit by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = route == Routes.DASHBOARD) { showExit = true }
    if (showExit) {
        ExitDialog(
            onStay = { showExit = false },
            onExit = { showExit = false; onExit() },
            alertsEnabled = settings.remindersEnabled,
        )
    }

    LaunchedEffect(openAlertsRequest) {
        if (openAlertsRequest) {
            navigator.alerts()
            onAlertsRequestHandled()
        }
    }

    // Ask for notification permission once (Android 13+), so daily alerts can be delivered.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.canPost(context)) {
            val prefs = context.getSharedPreferences("app_flags", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("asked_notifications", false)) {
                prefs.edit().putBoolean("asked_notifications", true).apply()
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                if (currentTab != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0),
                    ) {
                        TopLevel.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { if (!selected) navigator.tab(tab) },
                                icon = {
                                    if (tab == TopLevel.ALERTS && alertCount > 0) {
                                        BadgedBox(badge = { Badge { Text(if (alertCount > 99) "99+" else alertCount.toString()) } }) {
                                            Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null)
                                        }
                                    } else {
                                        Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null)
                                    }
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                    }
                }
                if (showBanner) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AdaptiveBanner()
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.DASHBOARD) { DashboardScreen(navigator) }
            composable(
                Routes.INVENTORY,
                arguments = listOf(navArgument("filter") { type = NavType.StringType; defaultValue = "all" }),
            ) { backStack ->
                InventoryScreen(navigator, initialFilter = backStack.arguments?.getString("filter") ?: "all")
            }
            composable(Routes.ALERTS) { AlertsScreen(navigator) }
            composable(Routes.REPORTS) { ReportsScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(navigator) }
            composable(
                Routes.PRODUCT,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { backStack ->
                ProductDetailScreen(navigator, productId = backStack.arguments?.getLong("id") ?: -1L)
            }
            composable(
                Routes.EDITOR,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("barcode") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { backStack ->
                ProductEditorScreen(
                    navigator,
                    productId = backStack.arguments?.getLong("id") ?: -1L,
                    initialBarcode = backStack.arguments?.getString("barcode").orEmpty(),
                )
            }
        }
    }
}
