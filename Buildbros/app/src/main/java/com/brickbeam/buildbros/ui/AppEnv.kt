package com.brickbeam.buildbros.ui

import android.app.Activity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import com.brickbeam.buildbros.ads.AdsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object Routes {
    const val SPLASH = "splash"
    const val REGION = "region"
    const val HOME = "home"
    const val BRICK = "brick"
    const val CONCRETE = "concrete"
    const val STEEL = "steel"
    const val TILES = "tiles"
    const val PAINT = "paint"
    const val CONVERTER = "converter"
    const val PROJECTS = "projects"
    const val ESTIMATE = "estimate/{id}"
    const val PRICES = "prices"
    const val SETTINGS = "settings"

    fun estimate(id: Long) = "estimate/$id"

    val TOP_LEVEL = setOf(HOME, PROJECTS, PRICES, SETTINGS)
}

/** App-wide helpers handed to every screen through [LocalAppEnv]. */
class AppEnv(
    val vm: AppViewModel,
    val nav: NavHostController,
    val snackbar: SnackbarHostState,
    val ads: AdsManager,
    val scope: CoroutineScope,
    val activity: () -> Activity?,
) {
    fun go(route: String) {
        nav.navigate(route) { launchSingleTop = true }
    }

    fun back() {
        nav.popBackStack()
    }

    fun toast(message: String) {
        scope.launch { snackbar.showSnackbar(message, duration = SnackbarDuration.Short) }
    }

    /**
     * Runs after a "major function" completes: offers the frequency-capped interstitial first,
     * then shows the confirmation snackbar once the ad (if any) is closed.
     */
    fun afterMajorAction(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        ads.showInterstitialIfEligible(activity()) {
            scope.launch {
                val r = snackbar.showSnackbar(message, actionLabel, withDismissAction = actionLabel == null, duration = SnackbarDuration.Short)
                if (r == SnackbarResult.ActionPerformed) onAction()
            }
        }
    }
}

val LocalAppEnv = staticCompositionLocalOf<AppEnv> { error("AppEnv not provided") }
