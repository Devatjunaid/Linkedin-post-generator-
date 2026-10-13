package com.snaptensio.bplog.ads

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.snaptensio.bplog.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Central place for every ad decision, written to stay inside AdMob and Google Play policy:
 *
 *  - Ads are requested only after the UMP consent flow (GDPR/US state privacy) says we may.
 *  - No ads on the splash screen, first-run screens, camera, guided measurement, the reading form
 *    or the very-high-reading safety screen.
 *  - Interstitials appear only at natural breaks *after* a task finishes (never on launch, never on
 *    exit, never while the user is in the middle of entering data), at most once per [COOLDOWN_MS],
 *    and not in the first [GRACE_MS] of a session.
 *  - Ad content is capped at PG, and no health data is ever passed to the ad request.
 */
class AdsManager(private val app: Application) {

    var canRequestAds by mutableStateOf(false)
        private set
    var privacyOptionsRequired by mutableStateOf(false)
        private set

    private val initialized = AtomicBoolean(false)
    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var lastInterstitialAt = 0L
    private val sessionStart = SystemClock.elapsedRealtime()

    fun gatherConsent(activity: Activity, onDone: () -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                    privacyOptionsRequired = info.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                    if (info.canRequestAds()) initialize()
                    onDone()
                }
            },
            { error ->
                Log.w(TAG, "Consent info: ${error.message}")
                if (info.canRequestAds()) initialize()
                onDone()
            },
        )
        // Consent obtained in a previous session: start loading ads right away.
        if (info.canRequestAds()) initialize()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "Privacy options: ${error.message}")
        }
    }

    private fun initialize() {
        if (initialized.getAndSet(true)) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
                .build(),
        )
        CoroutineScope(Dispatchers.IO).launch { MobileAds.initialize(app) {} }
        canRequestAds = true
        preloadInterstitial()
    }

    fun request(): AdRequest = AdRequest.Builder().build()

    private fun preloadInterstitial() {
        if (!canRequestAds || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(app, BuildConfig.AD_INTERSTITIAL_ID, request(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
                loadingInterstitial = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitial = null
                loadingInterstitial = false
                Log.d(TAG, "Interstitial failed: ${error.message}")
            }
        })
    }

    /**
     * Shows an interstitial at a natural break if one is ready and the frequency cap allows,
     * then always calls [onContinue] exactly once so the user's flow carries on.
     */
    fun showInterstitialThen(activity: Activity?, onContinue: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        val allowed = activity != null && ad != null &&
            now - sessionStart >= GRACE_MS &&
            (lastInterstitialAt == 0L || now - lastInterstitialAt >= COOLDOWN_MS)
        if (!allowed) {
            preloadInterstitial()
            onContinue()
            return
        }
        var continued = false
        fun proceed() {
            if (continued) return
            continued = true
            interstitial = null
            preloadInterstitial()
            onContinue()
        }
        ad!!.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = proceed()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = proceed()
            override fun onAdShowedFullScreenContent() {
                lastInterstitialAt = SystemClock.elapsedRealtime()
            }
        }
        ad.show(activity!!)
    }

    companion object {
        private const val TAG = "SnapTensioAds"
        const val COOLDOWN_MS = 90_000L
        const val GRACE_MS = 45_000L
    }
}
