package com.sudzy.carwash.ads

import android.app.Activity
import android.app.Application
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * All AdMob work lives here.
 *
 * Google Play Families / child-directed setup:
 *  - every request is tagged child-directed (COPPA) and under-age-of-consent (GDPR),
 *  - max ad content rating is G,
 *  - the AD_ID permission is removed in the manifest,
 *  - UMP consent is requested with the under-age tag (no consent form is shown to kids).
 * These flags are set before the SDK is initialised so that no request ever goes out untagged.
 */
class AdsManager(private val app: Application) {
    private val sdkStarted = AtomicBoolean(false)
    @Volatile var ready = false; private set
    private var consent: ConsentInformation? = null

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var banner: AdView? = null

    /** True while an interstitial or app open ad covers the screen. */
    var showingFullScreen = false

    val appOpen = AppOpenAdManager(app, this)

    init { applyFamilyPolicySettings() }

    private fun applyFamilyPolicySettings() {
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build()
        )
    }

    fun request(): AdRequest = AdRequest.Builder().build()

    /** Gathers consent (UMP), then initialises the Mobile Ads SDK once. */
    fun initialize(activity: Activity, onReady: () -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consent = info
        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(true).build()
        info.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                startSdk(activity, onReady)
            }
        }, { error ->
            Log.w(TAG, "Consent info update failed: ${error.message}")
            startSdk(activity, onReady)
        })
        // Consent from a previous session can be used straight away.
        startSdk(activity, onReady)
    }

    private fun startSdk(activity: Activity, onReady: () -> Unit) {
        if (consent?.canRequestAds() != true) return
        if (sdkStarted.getAndSet(true)) return
        Thread {
            MobileAds.initialize(app) {
                activity.runOnUiThread {
                    ready = true
                    loadInterstitial()
                    appOpen.load()
                    onReady()
                }
            }
        }.start()
    }

    // ------------------------------------------------------------------ banner
    fun bannerSize(activity: Activity): AdSize {
        val metrics = activity.resources.displayMetrics
        val widthDp = (metrics.widthPixels / metrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp)
    }

    /** Anchored adaptive banner in its own container below the game (never over gameplay). */
    fun loadBanner(activity: Activity, container: ViewGroup) {
        if (!ready || banner != null) return
        val view = AdView(activity)
        view.adUnitId = AdsConfig.BANNER
        view.setAdSize(bannerSize(activity))
        container.removeAllViews()
        container.addView(view)
        banner = view
        view.loadAd(request())
    }

    fun pauseBanner() = banner?.pause()
    fun resumeBanner() = banner?.resume()
    fun destroyBanner() { banner?.destroy(); banner = null }

    // ------------------------------------------------------------------ interstitial
    fun loadInterstitial() {
        if (!ready || interstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(app, AdsConfig.INTERSTITIAL, request(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; interstitialLoading = false }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Interstitial failed: ${error.message}")
                interstitial = null; interstitialLoading = false
            }
        })
    }

    /** Shows the interstitial if one is ready; [onDone] always runs exactly once. */
    fun showInterstitial(activity: Activity, onDone: () -> Unit) {
        val ad = interstitial
        if (ad == null || showingFullScreen) { onDone(); loadInterstitial(); return }
        interstitial = null
        var finished = false
        val finish = { if (!finished) { finished = true; showingFullScreen = false; onDone(); loadInterstitial() } }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { showingFullScreen = true }
            override fun onAdDismissedFullScreenContent() { finish() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { Log.w(TAG, "Interstitial show failed: ${error.message}"); finish() }
            // Coming back from the advertiser's page is not a "new" app open.
            override fun onAdClicked() { appOpen.skipNextForeground() }
        }
        ad.show(activity)
    }

    companion object { const val TAG = "SudzyAds" }
}
