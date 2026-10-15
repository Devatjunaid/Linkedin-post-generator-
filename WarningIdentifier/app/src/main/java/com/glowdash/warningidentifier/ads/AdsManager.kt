package com.glowdash.warningidentifier.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.glowdash.warningidentifier.BuildConfig
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Central place for every ad in the app.
 *
 * - Asks for GDPR/US-state consent with Google's UMP SDK before any ad request.
 * - Interstitials are preloaded and shown only at natural transitions
 *   ("main functions"), never more than once per [INTERSTITIAL_CAP_MS].
 * - Native ads are cached per placement so returning to a screen is instant.
 */
object AdsManager {
    private const val TAG = "AdsManager"

    /** Minimum time between two interstitials (also counted from app launch). */
    const val INTERSTITIAL_CAP_MS = 40_000L

    private val main = Handler(Looper.getMainLooper())
    private val initStarted = AtomicBoolean(false)

    private val _ready = MutableStateFlow(false)
    /** True once consent allows ads and the SDK is initialised. */
    val ready: StateFlow<Boolean> = _ready

    private var consentInfo: ConsentInformation? = null

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var interstitialShowing = false
    private var lastInterstitialAt = SystemClock.elapsedRealtime()

    private val natives = mutableMapOf<String, NativeAd>()
    private val nativeLoading = mutableSetOf<String>()
    private val nativeWaiters = mutableMapOf<String, MutableList<(NativeAd) -> Unit>>()

    // ---------------------------------------------------------------- consent

    fun gatherConsentAndInit(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo = info
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    formError?.let { Log.w(TAG, "Consent form: ${it.message}") }
                    if (info.canRequestAds()) initializeAds(activity)
                }
            },
            { error -> Log.w(TAG, "Consent update failed: ${error.message}") },
        )
        // Consent obtained in a previous session: start loading right away.
        if (info.canRequestAds()) initializeAds(activity)
    }

    val privacyOptionsRequired: Boolean
        get() = consentInfo?.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            error?.let { Log.w(TAG, "Privacy options: ${it.message}") }
        }
    }

    private fun initializeAds(activity: Activity) {
        if (!initStarted.compareAndSet(false, true)) return
        val app = activity.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(app) {
                main.post {
                    _ready.value = true
                    preloadInterstitial(app)
                }
            }
        }
    }

    // ----------------------------------------------------------- interstitial

    fun preloadInterstitial(context: Context) {
        if (!_ready.value || interstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(
            context.applicationContext,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    interstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    interstitialLoading = false
                    Log.d(TAG, "Interstitial failed: ${error.message}")
                }
            },
        )
    }

    /**
     * Runs [action] after an interstitial, if one is loaded and the 40-second cap
     * has passed. Otherwise runs [action] immediately — the user is never blocked.
     */
    fun showInterstitialThen(activity: Activity?, action: () -> Unit) {
        val ad = interstitial
        val capPassed = SystemClock.elapsedRealtime() - lastInterstitialAt >= INTERSTITIAL_CAP_MS
        if (activity == null || ad == null || !capPassed || interstitialShowing) {
            action()
            activity?.let { preloadInterstitial(it) }
            return
        }
        interstitial = null
        interstitialShowing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastInterstitialAt = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                interstitialShowing = false
                lastInterstitialAt = SystemClock.elapsedRealtime()
                preloadInterstitial(activity)
                action()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialShowing = false
                preloadInterstitial(activity)
                action()
            }
        }
        ad.show(activity)
    }

    // ----------------------------------------------------------------- native

    fun cachedNative(placement: String): NativeAd? = natives[placement]

    /** Loads (or returns the cached) native ad for a placement such as "home". */
    fun loadNative(context: Context, placement: String, onLoaded: (NativeAd) -> Unit) {
        natives[placement]?.let { onLoaded(it); return }
        if (!_ready.value) return
        nativeWaiters.getOrPut(placement) { mutableListOf() }.add(onLoaded)
        if (!nativeLoading.add(placement)) return

        AdLoader.Builder(context.applicationContext, BuildConfig.ADMOB_NATIVE_ID)
            .forNativeAd { ad ->
                nativeLoading.remove(placement)
                natives.put(placement, ad)?.destroy()
                nativeWaiters.remove(placement)?.forEach { it(ad) }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeLoading.remove(placement)
                    nativeWaiters.remove(placement)
                    Log.d(TAG, "Native '$placement' failed: ${error.message}")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                    .build(),
            )
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    /** Warms up a placement before it is on screen (e.g. the exit dialog). */
    fun prefetchNative(context: Context, placement: String) = loadNative(context, placement) {}

    fun releaseNatives() {
        natives.values.forEach { it.destroy() }
        natives.clear()
        nativeWaiters.clear()
    }
}
