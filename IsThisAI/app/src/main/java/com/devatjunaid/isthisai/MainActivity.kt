package com.devatjunaid.isthisai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.devatjunaid.isthisai.ads.AdsManager
import com.devatjunaid.isthisai.ui.AppRoot
import com.devatjunaid.isthisai.ui.MainViewModel
import com.devatjunaid.isthisai.ui.theme.IsThisAiTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdsManager.gatherConsentAndStart(this)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            IsThisAiTheme { AppRoot(vm) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Images shared to the app ("Share" / "Open with") are queued and checked after the splash. */
    private fun handleIntent(intent: Intent?) {
        val uri: Uri? = when (intent?.action) {
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }
        if (uri != null) vm.pendingUri = uri
    }
}
