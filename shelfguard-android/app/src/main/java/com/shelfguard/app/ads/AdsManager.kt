package com.shelfguard.app.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.shelfguard.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

object AdIds {
    val banner: String get() = BuildConfig.ADMOB_BANNER_ID
    val interstitial: String get() = BuildConfig.ADMOB_INTERSTITIAL_ID
    val native: String get() = BuildConfig.ADMOB_NATIVE_ID
    val appOpen: String get() = BuildConfig.ADMOB_APP_OPEN_ID
}

/**
 * Shared bookkeeping so full-screen ads never stack or follow each other too closely,
 * and so returning from an ad click (or from the barcode scanner / share sheet) doesn't
 * trigger an app-open ad.
 */
object FullScreenAdGate {
    @Volatile var isShowing: Boolean = false
    @Volatile var lastDismissedAt: Long = 0L
    /** Set right before the user leaves the app on purpose, so the return isn't treated as a re-open. */
    @Volatile var skipNextAppOpen: Boolean = false

    fun markShown() { isShowing = true }
    fun markDismissed() {
        isShowing = false
        lastDismissedAt = SystemClock.elapsedRealtime()
    }
}

/** Handles GDPR/UMP consent and Mobile Ads SDK start-up. Ads are only requested after consent allows it. */
object AdsManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val initStarted = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    /** True once the SDK is initialised and ads may be requested. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private lateinit var consentInformation: ConsentInformation

    /**
     * Updates consent info, shows the consent form when required (EEA/UK users), then starts
     * the SDK. Safe to call on every launch. [onFinished] runs once consent is resolved either way.
     */
    fun gatherConsentAndInitialize(activity: Activity, onFinished: () -> Unit) {
        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        var finished = false
        val finishOnce = {
            if (!finished) {
                finished = true
                onFinished()
            }
        }
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    if (consentInformation.canRequestAds()) initialize(activity.applicationContext)
                    finishOnce()
                }
            },
            { _ ->
                // Consent info could not be updated (e.g. offline). Use what we know from last time.
                if (consentInformation.canRequestAds()) initialize(activity.applicationContext)
                finishOnce()
            },
        )
        // Consent obtained in a previous session: start loading ads right away.
        if (consentInformation.canRequestAds()) initialize(activity.applicationContext)
    }

    val isPrivacyOptionsRequired: Boolean
        get() = ::consentInformation.isInitialized &&
            consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptionsForm(activity: Activity, onDone: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> onDone() }
    }

    private fun initialize(context: Context) {
        if (!initStarted.compareAndSet(false, true)) return
        scope.launch {
            withContext(Dispatchers.IO) { MobileAds.initialize(context) {} }
            _ready.value = true
            InterstitialAdManager.load(context)
            AppOpenAdManager.instance?.load()
        }
    }
}
