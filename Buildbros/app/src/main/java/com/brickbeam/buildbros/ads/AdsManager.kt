package com.brickbeam.buildbros.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.brickbeam.buildbros.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

object AdUnits {
    val BANNER: String = BuildConfig.ADMOB_BANNER_ID
    val INTERSTITIAL: String = BuildConfig.ADMOB_INTERSTITIAL_ID
    val NATIVE: String = BuildConfig.ADMOB_NATIVE_ID
}

/**
 * Owns the whole ad lifecycle so the UI never talks to the SDK directly:
 *
 * 1. Consent first (Google UMP: GDPR/UK/Swiss consent and US state privacy messages).
 *    No ad request is made until [ConsentInformation.canRequestAds] is true.
 * 2. Mobile Ads SDK initialised once, off the main thread.
 * 3. One interstitial kept pre-loaded, shown at most once every [INTERSTITIAL_CAP_MS].
 * 4. Native ads cached per screen slot so returning to a screen doesn't re-request.
 */
class AdsManager(context: Context) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(app)
    private val initStarted = AtomicBoolean(false)

    private val _ready = MutableStateFlow(false)
    /** True once consent allows ad requests and the SDK has finished initialising. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** When true, Settings must show a "Privacy & ad choices" entry (UMP requirement). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    // ---------------------------------------------------------------- consent

    /** Call from the activity on every launch. Safe to call repeatedly. */
    fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        consent.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    refreshPrivacyState()
                    if (consent.canRequestAds()) initializeSdk()
                }
            },
            { _ ->
                // Network error etc.: fall back to the consent stored from a previous session.
                refreshPrivacyState()
                if (consent.canRequestAds()) initializeSdk()
            },
        )
        // Consent obtained in a previous session lets ads start loading in parallel.
        if (consent.canRequestAds()) initializeSdk()
    }

    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ ->
            refreshPrivacyState()
            onDone()
        }
    }

    private fun refreshPrivacyState() {
        _privacyOptionsRequired.value = consent.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun initializeSdk() {
        if (!initStarted.compareAndSet(false, true)) return
        Thread {
            MobileAds.initialize(app) {
                main.post {
                    _ready.value = true
                    loadInterstitial()
                }
            }
        }.start()
    }

    // ---------------------------------------------------------- interstitial

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var interstitialFailures = 0
    private var lastInterstitialShownAt = 0L
    private var showing = false

    private fun loadInterstitial() {
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
                    // Back off (30 s, 60 s, 120 s ... max 5 min) instead of hammering the server.
                    val delay = (30_000L shl (interstitialFailures - 1).coerceAtMost(4)).coerceAtMost(300_000L)
                    main.postDelayed({ loadInterstitial() }, delay)
                }
            },
        )
    }

    private fun capElapsed(): Boolean =
        lastInterstitialShownAt == 0L ||
            SystemClock.elapsedRealtime() - lastInterstitialShownAt >= INTERSTITIAL_CAP_MS

    /**
     * Shows an interstitial after a completed major action (save to project, export) if one is
     * loaded and the 30-second frequency cap has elapsed. [onFinished] always runs exactly once:
     * immediately when no ad is shown, otherwise after the ad is dismissed.
     */
    fun showInterstitialIfEligible(activity: Activity?, onFinished: () -> Unit) {
        val ad = interstitial
        if (activity == null || ad == null || showing || !capElapsed() || activity.isFinishing) {
            if (ad == null) loadInterstitial()
            onFinished()
            return
        }
        val finished = AtomicBoolean(false)
        val finishOnce = { if (finished.compareAndSet(false, true)) onFinished() }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastInterstitialShownAt = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                showing = false
                interstitial = null
                loadInterstitial()
                finishOnce()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showing = false
                interstitial = null
                loadInterstitial()
                finishOnce()
            }
        }
        showing = true
        interstitial = null
        ad.show(activity)
    }

    // ---------------------------------------------------------------- native

    private class CachedNative(val ad: NativeAd, val loadedAt: Long)

    private val nativeCache = HashMap<String, CachedNative>()
    private val nativeLoading = HashSet<String>()
    private val nativeFailedAt = HashMap<String, Long>()

    /**
     * Returns a cached native ad for [slot] or requests one; [onLoaded] is called on the main
     * thread when a new ad arrives. Ads older than 55 minutes are replaced (they expire at 60).
     */
    fun nativeFor(slot: String, onLoaded: (NativeAd) -> Unit): NativeAd? {
        val now = SystemClock.elapsedRealtime()
        val cached = nativeCache[slot]
        if (cached != null && now - cached.loadedAt < NATIVE_TTL_MS) return cached.ad
        if (!_ready.value || slot in nativeLoading) return cached?.ad
        val failedAt = nativeFailedAt[slot]
        if (failedAt != null && now - failedAt < NATIVE_RETRY_MS) return cached?.ad
        nativeLoading += slot
        val loader = AdLoader.Builder(app, AdUnits.NATIVE)
            .forNativeAd { ad ->
                main.post {
                    nativeLoading -= slot
                    nativeFailedAt.remove(slot)
                    nativeCache.put(slot, CachedNative(ad, SystemClock.elapsedRealtime()))?.ad?.destroy()
                    onLoaded(ad)
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    main.post {
                        nativeLoading -= slot
                        nativeFailedAt[slot] = SystemClock.elapsedRealtime()
                    }
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build(),
            )
            .build()
        loader.loadAd(AdRequest.Builder().build())
        return cached?.ad
    }

    companion object {
        const val INTERSTITIAL_CAP_MS = 30_000L
        private const val NATIVE_TTL_MS = 55 * 60 * 1000L
        private const val NATIVE_RETRY_MS = 60_000L
    }
}
