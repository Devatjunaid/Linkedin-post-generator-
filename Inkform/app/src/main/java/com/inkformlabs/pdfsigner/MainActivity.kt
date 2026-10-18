package com.inkformlabs.pdfsigner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.inkformlabs.pdfsigner.ui.InkformRoot
import com.inkformlabs.pdfsigner.ui.LocalAppContainer
import com.inkformlabs.pdfsigner.ui.nav.AppViewModel

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash hands over at once to the animated Compose splash.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as InkformApp).container

        // Consent first; ads are only initialised once the user's choice allows it.
        container.ads.initializeIfAllowed()
        if (savedInstanceState == null) {
            container.consent.gatherConsent(this) { container.ads.initializeIfAllowed() }
            handleIncoming(intent)
        }

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                InkformRoot(appViewModel = appViewModel, onExitApp = { finish() })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncoming(intent)
    }

    /** "Open with Inkform" / "Share to Inkform" from other apps. */
    private fun handleIncoming(intent: Intent?) {
        val uri: Uri? = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
        if (uri != null) appViewModel.incomingPdf = uri
    }
}
