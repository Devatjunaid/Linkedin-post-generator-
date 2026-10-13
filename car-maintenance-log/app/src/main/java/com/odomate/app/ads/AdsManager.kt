package com.odomate.app.ads

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.odomate.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns consent (Google UMP), Mobile Ads SDK start-up and the interstitial lifecycle.
 * Banners and native ads only load once [ready] is true, i.e. after consent allows ads.
 */
class AdsManager(private val app: Application) {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private val initStarted = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    private var consentInfo: ConsentInformation? = null

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var lastInterstitialAt = 0L
    private var retryDelayMs = 15_000L

    /** Shows the consent form when required (EEA/UK), then starts the SDK. Always calls [onComplete]. */
    fun gatherConsent(activity: Activity, onComplete: () -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo = info
        val params = ConsentRequestParameters.Builder().apply {
            if (BuildConfig.DEBUG) {
                setConsentDebugSettings(ConsentDebugSettings.Builder(activity).build())
            }
        }.build()
        val done = AtomicBoolean(false)
        val finish = {
            if (done.compareAndSet(false, true)) {
                _privacyOptionsRequired.value =
                    info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                startSdkIfAllowed()
                onComplete()
            }
        }
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> finish() }
            },
            { _ -> finish() },
        )
        // Returning users who already consented can start loading immediately.
        if (info.canRequestAds()) startSdkIfAllowed()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> startSdkIfAllowed() }
    }

    private fun startSdkIfAllowed() {
        val info = consentInfo ?: return
        if (!info.canRequestAds()) return
        if (!initStarted.compareAndSet(false, true)) return
        Thread {
            MobileAds.initialize(app) {
                main.post {
                    _ready.value = true
                    preloadInterstitial()
                }
            }
        }.start()
    }

    fun preloadInterstitial() {
        if (!_ready.value || loadingInterstitial || interstitial != null) return
        loadingInterstitial = true
        InterstitialAd.load(
            app,
            BuildConfig.INTERSTITIAL_AD_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loadingInterstitial = false
                    retryDelayMs = 15_000L
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    loadingInterstitial = false
                    main.postDelayed({ preloadInterstitial() }, retryDelayMs)
                    retryDelayMs = (retryDelayMs * 2).coerceAtMost(5 * 60_000L)
                }
            },
        )
    }

    /**
     * Shows an interstitial after a key action (saving a reading, logging a service, exporting),
     * at most once every [MIN_INTERVAL_MS]. [onFinished] always runs exactly once, ad or not.
     */
    fun showInterstitial(activity: Activity?, onFinished: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        val coolingDown = lastInterstitialAt != 0L && now - lastInterstitialAt < MIN_INTERVAL_MS
        if (activity == null || ad == null || coolingDown) {
            onFinished()
            preloadInterstitial()
            return
        }
        val finished = AtomicBoolean(false)
        val finishOnce = { if (finished.compareAndSet(false, true)) onFinished() }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                lastInterstitialAt = SystemClock.elapsedRealtime()
                finishOnce()
                preloadInterstitial()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                finishOnce()
                preloadInterstitial()
            }
        }
        interstitial = null
        ad.show(activity)
    }

    companion object {
        const val MIN_INTERVAL_MS = 60_000L
    }
}
