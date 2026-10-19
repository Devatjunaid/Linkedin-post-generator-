package com.devatjunaid.isthisai.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.devatjunaid.isthisai.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consent first, ads second: the Google UMP consent form is shown where the law requires it
 * (EEA, UK, Switzerland, some US states) and the Mobile Ads SDK only starts once
 * [ConsentInformation.canRequestAds] allows it.
 */
object AdsManager {
    private val started = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun gatherConsentAndStart(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    updatePrivacyStatus(info)
                    if (info.canRequestAds()) startSdk(activity)
                }
            },
            { _ -> if (info.canRequestAds()) startSdk(activity) },
        )
        // Consent from an earlier session lets ads start right away.
        if (info.canRequestAds()) startSdk(activity)
        updatePrivacyStatus(info)
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ ->
            updatePrivacyStatus(UserMessagingPlatform.getConsentInformation(activity))
        }
    }

    private fun updatePrivacyStatus(info: ConsentInformation) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun startSdk(activity: Activity) {
        if (!started.compareAndSet(false, true)) return
        val app = activity.applicationContext
        scope.launch {
            MobileAds.initialize(app) {
                _ready.value = true
                Interstitials.preload(app)
            }
        }
    }
}

/** Interstitials at natural breaks only, never more than one per 30 seconds. */
object Interstitials {
    private val main = Handler(Looper.getMainLooper())
    private val cap = FrequencyCap(AdPolicy.INTERSTITIAL_MIN_INTERVAL_MS)
    private var ad: InterstitialAd? = null
    private var loading = false
    private var showing = false
    private var failures = 0

    fun preload(context: Context) {
        val app = context.applicationContext
        main.post {
            if (!AdsManager.ready.value || loading || ad != null) return@post
            loading = true
            InterstitialAd.load(app, BuildConfig.INTERSTITIAL_AD_UNIT, AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        loading = false
                        failures = 0
                        ad = interstitialAd
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loading = false
                        failures++
                        main.postDelayed({ preload(app) }, AdPolicy.preloadRetryDelay(failures))
                    }
                })
        }
    }

    /**
     * Shows an interstitial if one is ready and the cap allows it, then runs [onContinue].
     * [onContinue] always runs exactly once, whether or not an ad was shown.
     */
    fun showThen(activity: Activity, onContinue: () -> Unit) {
        val current = ad
        val now = SystemClock.elapsedRealtime()
        if (current == null || showing || !cap.canShow(now) || activity.isFinishing) {
            preload(activity)
            onContinue()
            return
        }
        ad = null
        showing = true
        var continued = false
        fun continueOnce() {
            if (continued) return
            continued = true
            showing = false
            onContinue()
            preload(activity)
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                cap.markShown(SystemClock.elapsedRealtime())
            }

            override fun onAdDismissedFullScreenContent() = continueOnce()

            override fun onAdFailedToShowFullScreenContent(error: AdError) = continueOnce()
        }
        cap.markShown(now)
        current.show(activity)
    }
}
