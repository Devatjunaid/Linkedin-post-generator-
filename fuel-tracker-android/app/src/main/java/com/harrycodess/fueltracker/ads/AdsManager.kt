package com.harrycodess.fueltracker.ads

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
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.harrycodess.fueltracker.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

object AdsConfig {
    val bannerId: String get() = BuildConfig.ADMOB_BANNER_ID
    val interstitialId: String get() = BuildConfig.ADMOB_INTERSTITIAL_ID

    /** Minimum gap between two interstitials, so back-to-back saves don't spam ads. */
    const val INTERSTITIAL_MIN_INTERVAL_MS = 30_000L
}

/**
 * Owns the whole ad lifecycle: UMP consent (GDPR), Mobile Ads SDK initialisation
 * and the interstitial shown after a trip is saved. Banners are rendered by
 * [com.harrycodess.fueltracker.ui.components.AdBannerSlot] once [canRequestAds] is true.
 */
class AdsManager(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(appContext)
    private val initStarted = AtomicBoolean(false)

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private var interstitial: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var lastInterstitialShownAt = 0L

    /** Call from the activity on every launch. Shows the consent form only when required. */
    fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    refreshConsentState()
                }
            },
            { _ -> refreshConsentState() },
        )
        // Consent from a previous session lets ads start loading right away.
        refreshConsentState()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> refreshConsentState() }
    }

    private fun refreshConsentState() {
        _privacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (consentInformation.canRequestAds()) initializeSdk()
    }

    private fun initializeSdk() {
        if (!initStarted.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(appContext) {}
            withContext(Dispatchers.Main) {
                _canRequestAds.value = true
                preloadInterstitial()
            }
        }
    }

    fun preloadInterstitial() {
        if (!_canRequestAds.value || interstitial != null || isLoadingInterstitial) return
        isLoadingInterstitial = true
        InterstitialAd.load(
            appContext,
            AdsConfig.interstitialId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    isLoadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    isLoadingInterstitial = false
                }
            },
        )
    }

    /**
     * Shows the interstitial if one is loaded and the minimum interval has passed.
     * [onFinished] is always called exactly once: after the ad closes, or immediately
     * when no ad is shown, so the user is never blocked.
     */
    fun showInterstitial(activity: Activity, onFinished: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        val tooSoon = lastInterstitialShownAt != 0L &&
            now - lastInterstitialShownAt < AdsConfig.INTERSTITIAL_MIN_INTERVAL_MS
        if (ad == null || tooSoon) {
            preloadInterstitial()
            onFinished()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastInterstitialShownAt = SystemClock.elapsedRealtime()
            }

            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                preloadInterstitial()
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                preloadInterstitial()
                onFinished()
            }
        }
        ad.show(activity)
    }
}
