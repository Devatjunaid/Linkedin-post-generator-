package com.inkformlabs.pdfsigner.ads

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
import com.inkformlabs.pdfsigner.AppConfig
import com.inkformlabs.pdfsigner.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

object AdUnits {
    val BANNER: String = BuildConfig.AD_UNIT_BANNER
    val INTERSTITIAL: String = BuildConfig.AD_UNIT_INTERSTITIAL
    val NATIVE: String = BuildConfig.AD_UNIT_NATIVE
}

/**
 * Initialises the Mobile Ads SDK (only after consent allows it) and manages the interstitial.
 *
 * Interstitial rules (AdMob & Play policy friendly):
 *  - only shown at natural break points right after the user finishes a major task,
 *  - never on app launch or exit,
 *  - at most one every [AppConfig.INTERSTITIAL_INTERVAL_MS] (30 s),
 *  - never blocks the user: if no ad is ready, the app continues immediately.
 */
class AdsManager(private val app: Application, private val consent: ConsentManager) {

    private val started = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())

    private val _ready = MutableStateFlow(false)
    /** True once the SDK is initialised and consent allows ad requests. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var interstitialShowing = false
    private var lastInterstitialShownAt = 0L
    private var interstitialFailures = 0

    /** Safe to call many times; does nothing until consent allows ads. */
    fun initializeIfAllowed() {
        if (!consent.canRequestAds) return
        if (!started.compareAndSet(false, true)) return
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
        if (!_ready.value || interstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(
            app,
            AdUnits.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    interstitialLoading = false
                    interstitialFailures = 0
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    interstitialLoading = false
                    interstitialFailures++
                    // Back off gently: 30 s, 60 s, 120 s ... capped at 5 minutes.
                    val delay = (30_000L shl (interstitialFailures - 1).coerceAtMost(4)).coerceAtMost(300_000L)
                    main.postDelayed({ preloadInterstitial() }, delay)
                }
            },
        )
    }

    /**
     * Shows an interstitial if one is ready and the 30-second cap allows it, then runs [onContinue].
     * [onContinue] always runs exactly once, whether or not an ad was shown.
     */
    fun showInterstitialThen(activity: Activity?, onContinue: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        val capped = lastInterstitialShownAt != 0L && now - lastInterstitialShownAt < AppConfig.INTERSTITIAL_INTERVAL_MS
        if (activity == null || ad == null || capped || interstitialShowing || !_ready.value) {
            if (ad == null) preloadInterstitial()
            onContinue()
            return
        }
        var continued = false
        val proceed = {
            if (!continued) {
                continued = true
                onContinue()
            }
        }
        interstitialShowing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastInterstitialShownAt = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                interstitialShowing = false
                preloadInterstitial()
                proceed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                interstitialShowing = false
                preloadInterstitial()
                proceed()
            }
        }
        interstitial = null
        ad.show(activity)
    }
}
