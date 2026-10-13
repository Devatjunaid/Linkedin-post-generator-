package com.devatjunaid.renevo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.devatjunaid.renevo.data.ThemeMode
import com.devatjunaid.renevo.reminders.Notifier
import com.devatjunaid.renevo.security.AppLock
import com.devatjunaid.renevo.security.LockGate
import com.devatjunaid.renevo.ui.AppViewModel
import com.devatjunaid.renevo.ui.lock.LockScreen
import com.devatjunaid.renevo.ui.nav.RenevoRoot
import com.devatjunaid.renevo.ui.nav.Routes
import com.devatjunaid.renevo.ui.theme.RenevoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val vm: AppViewModel by viewModels {
        viewModelFactory { initializer { AppViewModel(applicationContext.container) } }
    }

    private val pendingDetail = MutableStateFlow<Long?>(null)
    private val locked = MutableStateFlow(false)
    private val consentDone = MutableStateFlow(false)
    private var backgroundedAt = 0L
    private var authInProgress = false
    private var lockMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { vm.settings.value == null }
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate().alpha(0f).setDuration(220L).withEndAction { provider.remove() }.start()
        }
        enableEdgeToEdge()
        readIntent(intent)

        // Cold start: lock immediately if app lock is on.
        lifecycleScope.launch {
            val s = vm.settings.filterNotNull().first()
            if (s.appLock && !unlockedThisProcess) locked.value = true
        }
        // Screenshot blocking follows the setting live.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.settings.filterNotNull().collect { s ->
                    if (s.blockScreenshots) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
        lifecycleScope.launch {
            locked.collect { vm.ads.suppressed = it }
        }

        // Google consent (GDPR/US states) before any ad request. Runs once per launch.
        if (!vm.ads.consentStarted) {
            vm.ads.gatherConsent(this) { consentDone.value = true }
        } else {
            consentDone.value = true
        }

        setContent {
            val settings by vm.settings.collectAsState()
            val isLocked by locked.collectAsState()
            val consent by consentDone.collectAsState()
            val detail by pendingDetail.collectAsState()
            var route by remember { mutableStateOf<String?>(null) }
            val mode = settings?.theme ?: ThemeMode.SYSTEM
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val darkBars = dark || route == Routes.SPLASH || isLocked
            LaunchedEffect(darkBars) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkBars) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (darkBars) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
            }
            RenevoTheme(mode) {
                RenevoRoot(
                    vm = vm,
                    consentDone = consent,
                    pendingDetailId = if (isLocked) null else detail,
                    onDetailConsumed = { pendingDetail.value = null },
                    onExit = { finish() },
                    onRouteChanged = { route = it },
                )
                if (isLocked) LockScreen(lockMessage) { unlock() }
            }
        }
    }

    private fun unlock() {
        if (authInProgress) return
        if (!AppLock.isAvailable(this)) {
            // Screen lock was removed from the phone: nothing to verify against.
            locked.value = false
            return
        }
        authInProgress = true
        AppLock.prompt(
            this,
            title = "Unlock Renevo",
            subtitle = "Confirm it's you",
            onSuccess = {
                authInProgress = false
                lockMessage = null
                backgroundedAt = 0L
                unlockedThisProcess = true
                locked.value = false
            },
            onError = { msg ->
                authInProgress = false
                lockMessage = msg
            },
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent?) {
        val id = intent?.getLongExtra(Notifier.EXTRA_SUBSCRIPTION_ID, -1L) ?: -1L
        if (id > 0) pendingDetail.value = id
    }

    override fun onStart() {
        super.onStart()
        vm.refreshDay()
        val s = vm.settings.value
        val away = if (backgroundedAt > 0) SystemClock.elapsedRealtime() - backgroundedAt else 0L
        if (s?.appLock == true && backgroundedAt > 0 && away >= LOCK_GRACE_MS) locked.value = true
        backgroundedAt = 0L
        LockGate.externalUiOpen = false
    }

    override fun onStop() {
        super.onStop()
        // Pickers we opened, ads and the lock prompt itself don't count as leaving the app.
        if (!isChangingConfigurations && !LockGate.externalUiOpen && !authInProgress && !vm.ads.isShowingFullScreen) {
            backgroundedAt = SystemClock.elapsedRealtime()
        }
    }

    companion object {
        private const val LOCK_GRACE_MS = 5_000L

        /** Survives rotation, resets when Android kills the process, so a cold start always locks. */
        private var unlockedThisProcess = false
    }
}
