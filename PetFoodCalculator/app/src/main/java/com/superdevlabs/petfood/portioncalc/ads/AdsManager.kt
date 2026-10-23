package com.superdevlabs.petfood.portioncalc.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
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
import com.superdevlabs.petfood.portioncalc.BuildConfig
import com.superdevlabs.petfood.portioncalc.domain.FrequencyCap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

object AdUnits {
    // Debug builds use Google's official sample units; release builds use the live units.
    val banner = if (BuildConfig.USE_TEST_ADS) "ca-app-pub-3940256099942544/9214589741" else "ca-app-pub-1948347222012540/4985691146"
    val interstitial = if (BuildConfig.USE_TEST_ADS) "ca-app-pub-3940256099942544/1033173712" else "ca-app-pub-1948347222012540/3480117918"
    val native = if (BuildConfig.USE_TEST_ADS) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-1948347222012540/7252761836"
}

/**
 * Owns consent (Google UMP), SDK start-up and the interstitial frequency cap.
 * No ad is requested until the user's consent choice allows it.
 */
class AdsManager(context: Context) {
    private val appContext = context.applicationContext
    private val consentInfo: ConsentInformation = UserMessagingPlatform.getConsentInformation(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val sdkStarted = AtomicBoolean(false)

    private val _ready = MutableStateFlow(false)
    /** True once ads may be requested (consent obtained or not required) and the SDK is started. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var interstitialShowing = false
    private val interstitialCap = FrequencyCap(INTERSTITIAL_CAP_MS)
    private var interstitialRetryDelayMs = 30_000L

    /** Call on every launch from the first Activity: refreshes consent, shows the form if needed. */
    fun gatherConsent(activity: Activity, onDone: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                    afterConsent()
                    onDone()
                }
            },
            { error ->
                Log.w(TAG, "Consent info update failed: ${error.message}")
                afterConsent()
                onDone()
            },
        )
        // A previous session's consent lets ads start in parallel with the refresh.
        if (consentInfo.canRequestAds()) startSdk()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "Privacy options: ${error.message}")
            afterConsent()
        }
    }

    private fun afterConsent() {
        _privacyOptionsRequired.value =
            consentInfo.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (consentInfo.canRequestAds()) startSdk()
    }

    private fun startSdk() {
        if (!sdkStarted.compareAndSet(false, true)) return
        scope.launch {
            withContext(Dispatchers.IO) {
                MobileAds.setRequestConfiguration(
                    RequestConfiguration.Builder()
                        // A general-audience pet care app: keep ads suitable for everyone.
                        .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
                        .build(),
                )
                MobileAds.initialize(appContext) {}
            }
            _ready.value = true
            loadInterstitial()
        }
    }

    // ------------------------------------------------------------------ interstitial

    private fun loadInterstitial() {
        if (!_ready.value || interstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(
            appContext,
            AdUnits.interstitial,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    interstitialLoading = false
                    interstitialRetryDelayMs = 30_000L
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    interstitialLoading = false
                    Log.d(TAG, "Interstitial failed: ${error.message}")
                    val wait = interstitialRetryDelayMs
                    interstitialRetryDelayMs = (interstitialRetryDelayMs * 2).coerceAtMost(5 * 60_000L)
                    scope.launch {
                        delay(wait)
                        loadInterstitial()
                    }
                }
            },
        )
    }

    /**
     * Shows an interstitial after a completed major action, at most once every 30 seconds.
     * [onContinue] always runs exactly once: right away when no ad is shown, otherwise after it closes.
     */
    fun showInterstitialThen(activity: Activity?, onContinue: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        val capped = !interstitialCap.canShow(now)
        if (activity == null || ad == null || capped || interstitialShowing || activity.isFinishing) {
            onContinue()
            loadInterstitial()
            return
        }
        interstitialShowing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                interstitialCap.markShown(SystemClock.elapsedRealtime())
            }

            override fun onAdDismissedFullScreenContent() {
                // The 30 s window starts when the user is back in the app.
                interstitialCap.markShown(SystemClock.elapsedRealtime())
                finish()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                finish()
            }

            private fun finish() {
                interstitial = null
                interstitialShowing = false
                onContinue()
                loadInterstitial()
            }
        }
        ad.show(activity)
    }

    companion object {
        private const val TAG = "Ads"
        const val INTERSTITIAL_CAP_MS = 30_000L
    }
}
