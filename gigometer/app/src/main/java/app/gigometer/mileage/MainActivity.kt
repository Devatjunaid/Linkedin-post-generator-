package app.gigometer.mileage

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.gigometer.mileage.ads.Ads
import app.gigometer.mileage.ui.AppRoot
import app.gigometer.mileage.ui.MainViewModel
import app.gigometer.mileage.ui.theme.GigometerTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private var openRequest by mutableStateOf<String?>(null)
    private var consentRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openRequest = intent?.getStringExtra(Notifications.EXTRA_OPEN)
        if (savedInstanceState != null) requestConsent()
        setContent {
            GigometerTheme {
                AppRoot(
                    vm = vm,
                    openRequest = openRequest,
                    onOpenHandled = { openRequest = null },
                    onSplashDone = { requestConsent() },
                )
            }
        }
    }

    /** Ask for ad consent only after the brand splash has played. */
    private fun requestConsent() {
        if (consentRequested) return
        consentRequested = true
        Ads.gatherConsentAndInit(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Notifications.EXTRA_OPEN)?.let { openRequest = it }
    }
}
