package com.odomate.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odomate.app.AppContainer
import com.odomate.app.data.AppSettings
import com.odomate.app.data.ThemeMode
import com.odomate.app.ui.navigation.AppNav
import com.odomate.app.ui.navigation.LaunchTarget
import com.odomate.app.ui.splash.SplashScreen
import com.odomate.app.ui.theme.OdoMateTheme
import kotlinx.coroutines.launch

@Composable
fun OdoMateRoot(
    container: AppContainer,
    consentDone: Boolean,
    launchTarget: LaunchTarget?,
    onLaunchTargetHandled: () -> Unit,
) {
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val vehicles by container.repository.vehicles.collectAsStateWithLifecycle(initialValue = null)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messenger: (String) -> Unit = remember {
        { text: String ->
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(text)
            }
            Unit
        }
    }
    var splashDone by rememberSaveable { mutableStateOf(false) }

    OdoMateTheme(settings?.themeMode ?: ThemeMode.DARK) {
        CompositionLocalProvider(
            LocalAppContainer provides container,
            LocalSettings provides (settings ?: AppSettings()),
            LocalSnackbar provides snackbar,
            LocalMessenger provides messenger,
        ) {
            val loadedVehicles = vehicles
            if (!splashDone || loadedVehicles == null || settings == null) {
                SplashScreen(
                    ready = consentDone && loadedVehicles != null && settings != null,
                    onFinished = { splashDone = true },
                )
            } else {
                // Decide the start screen once; later changes are handled by navigation.
                val startOnboarding = rememberSaveable { loadedVehicles.isEmpty() }
                AppNav(
                    startOnboarding = startOnboarding,
                    launchTarget = launchTarget,
                    onLaunchTargetHandled = onLaunchTargetHandled,
                )
            }
        }
    }
}
