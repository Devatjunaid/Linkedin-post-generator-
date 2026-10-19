package com.verba.subtitles.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.verba.subtitles.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object AdUnits {
    val banner: String get() = BuildConfig.BANNER_AD_UNIT
    val interstitial: String get() = BuildConfig.INTERSTITIAL_AD_UNIT
    val native: String get() = BuildConfig.NATIVE_AD_UNIT
}

/**
 * Owns Mobile Ads SDK start-up and the interstitial.
 *
 * Interstitial frequency cap: at most one interstitial every [INTERSTITIAL_INTERVAL_MS]
 * (30 s), measured from the moment the last one was shown. When no ad is ready or the cap is
 * active, the user's action simply continues — the app never waits for an ad.
 */
class AdsManager(private val context: Context, private val appScope: CoroutineScope) {

    private val started = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)

    /** True once consent allows ads and the SDK is initialised. Banners/natives wait for this. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var interstitialRetry: Job? = null
    private var lastInterstitialShownAt = 0L
    private var showing = false

    /** Call after consent allows ads. Safe to call more than once. */
    fun initialize() {
        if (!started.compareAndSet(false, true)) return
        appScope.launch(Dispatchers.IO) {
            MobileAds.initialize(context) {}
            launch(Dispatchers.Main) {
                _ready.value = true
                preloadInterstitial()
            }
        }
    }

    fun preloadInterstitial() {
        if (!_ready.value || interstitialLoading || interstitial != null) return
        interstitialLoading = true
        InterstitialAd.load(
            context,
            AdUnits.interstitial,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    interstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    interstitialLoading = false
                    interstitialRetry?.cancel()
                    interstitialRetry = appScope.launch(Dispatchers.Main) {
                        delay(INTERSTITIAL_RETRY_MS)
                        preloadInterstitial()
                    }
                }
            },
        )
    }

    /**
     * Shows an interstitial before a major action (start transcription, export, save) if one is
     * loaded and the 30-second cap allows it, then runs [onContinue]. [onContinue] always runs
     * exactly once.
     */
    fun runWithInterstitial(activity: Activity?, onContinue: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        val ad = interstitial
        val capped = lastInterstitialShownAt != 0L && now - lastInterstitialShownAt < INTERSTITIAL_INTERVAL_MS
        if (activity == null || ad == null || capped || showing || activity.isFinishing) {
            preloadInterstitial()
            onContinue()
            return
        }
        interstitial = null
        showing = true
        var continued = false
        fun continueOnce() {
            if (!continued) {
                continued = true
                onContinue()
            }
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastInterstitialShownAt = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                showing = false
                preloadInterstitial()
                continueOnce()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showing = false
                preloadInterstitial()
                continueOnce()
            }
        }
        ad.show(activity)
    }

    companion object {
        const val INTERSTITIAL_INTERVAL_MS = 30_000L
        private const val INTERSTITIAL_RETRY_MS = 30_000L
    }
}
