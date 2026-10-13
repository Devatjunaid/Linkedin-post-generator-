package com.aquadoselab.poolchemistry.ads

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
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Ad unit IDs. These are Google's official TEST ids and are safe to use while developing.
 * Replace every value with your own ids from the AdMob console before publishing,
 * and also replace `admob_app_id` in res/values/strings.xml.
 */
object AdIds {
    const val BANNER = "ca-app-pub-3940256099942544/9214589741"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
}

/**
 * Handles consent (UMP), SDK start-up and interstitials.
 * Ads are only requested after consent has been gathered, as Google requires.
 */
object AdsManager {

    /** Minimum time between two interstitials. */
    private const val INTERSTITIAL_COOLDOWN_MS = 60_000L

    private val started = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    /** True once the Mobile Ads SDK is initialised and ads may be requested. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var consentInformation: ConsentInformation? = null
    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false
    private var lastShownAt = 0L
    private var showing = false

    /** Gathers consent if needed, then starts the SDK. [onDone] is always called once. */
    fun start(activity: Activity, onDone: () -> Unit) {
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = consent
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()
        var finished = false
        val finish = {
            if (!finished) {
                finished = true
                if (consent.canRequestAds()) initSdk(activity)
                onDone()
            }
        }
        consent.requestConsentInfoUpdate(
            activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { finish() }
            },
            { finish() }
        )
        // Consent from a previous session lets us start right away.
        if (consent.canRequestAds()) initSdk(activity)
    }

    private fun initSdk(context: Context) {
        if (!started.compareAndSet(false, true)) return
        MobileAds.initialize(context.applicationContext) {
            _ready.value = true
            loadInterstitial(context.applicationContext)
        }
    }

    /** Whether the user must be offered a "Privacy options" entry (GDPR regions). */
    fun privacyOptionsRequired(): Boolean =
        consentInformation?.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }

    private fun loadInterstitial(context: Context) {
        if (!_ready.value || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(
            context, AdIds.INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    loadingInterstitial = false
                }
            }
        )
    }

    /**
     * Shows an interstitial if one is ready and the cooldown has passed, then runs [then].
     * [then] always runs exactly once, ad or no ad.
     */
    fun showInterstitial(activity: Activity?, then: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        if (activity == null || ad == null || showing || now - lastShownAt < INTERSTITIAL_COOLDOWN_MS) {
            activity?.let { loadInterstitial(it.applicationContext) }
            then()
            return
        }
        var done = false
        val finish = {
            if (!done) {
                done = true
                showing = false
                then()
            }
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                lastShownAt = SystemClock.elapsedRealtime()
                loadInterstitial(activity.applicationContext)
                finish()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                loadInterstitial(activity.applicationContext)
                finish()
            }
        }
        showing = true
        interstitial = null
        ad.show(activity)
    }

    @Suppress("unused")
    private fun debugSettings(context: Context) = ConsentDebugSettings.Builder(context)
        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
        .build()
}
