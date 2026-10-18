package com.devatjunaid.balconysolar.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.devatjunaid.balconysolar.BuildConfig
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Ad unit IDs: Google's test units in debug builds, the live units in release builds. */
object AdUnits {
    val banner: String get() = BuildConfig.ADMOB_BANNER_ID
    val interstitial: String get() = BuildConfig.ADMOB_INTERSTITIAL_ID
    val native: String get() = BuildConfig.ADMOB_NATIVE_ID
}

/**
 * Consent (Google UMP) and AdMob lifecycle. No ad is requested before the consent flow says
 * ads may be requested, and interstitials are capped at one per [INTERSTITIAL_INTERVAL_MS].
 */
object AdsManager {

    const val INTERSTITIAL_INTERVAL_MS = 30_000L
    private const val INTERSTITIAL_RETRY_MS = 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val initStarted = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    /** True once consent allows ads and the Mobile Ads SDK is initialised. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private var consentInfo: ConsentInformation? = null
    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    // The cap also applies from app start, so no interstitial appears in the first 30 seconds.
    private var lastInterstitialAt = SystemClock.elapsedRealtime()

    /** Runs the consent flow (shows the form only where required, e.g. EEA/UK) and starts the SDK. */
    fun gatherConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo = info
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    updatePrivacyStatus(info)
                    if (info.canRequestAds()) initialize(activity)
                }
            },
            {
                // Network or configuration error: use the consent obtained in an earlier session, if any.
                updatePrivacyStatus(info)
                if (info.canRequestAds()) initialize(activity)
            },
        )
        // Consent from a previous session lets ads start loading while the update runs.
        if (info.canRequestAds()) initialize(activity)
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            consentInfo?.let(::updatePrivacyStatus)
        }
    }

    private fun updatePrivacyStatus(info: ConsentInformation) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun initialize(context: Context) {
        if (!initStarted.compareAndSet(false, true)) return
        val app = context.applicationContext
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(app) {
                scope.launch {
                    _ready.value = true
                    preloadInterstitial(app)
                }
            }
        }
    }

    fun preloadInterstitial(context: Context) {
        if (!_ready.value || interstitialLoading || interstitial != null) return
        interstitialLoading = true
        InterstitialAd.load(
            context.applicationContext, AdUnits.interstitial, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    interstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    interstitialLoading = false
                    scope.launch {
                        delay(INTERSTITIAL_RETRY_MS)
                        preloadInterstitial(context.applicationContext)
                    }
                }
            },
        )
    }

    /**
     * Shows an interstitial at a natural break (after the user starts a major action) if one is
     * loaded and at least 30 seconds have passed since the last one; then runs [then].
     */
    fun showInterstitialThen(activity: Activity, then: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        if (!_ready.value || ad == null || now - lastInterstitialAt < INTERSTITIAL_INTERVAL_MS) {
            if (ad == null) preloadInterstitial(activity)
            then()
            return
        }
        interstitial = null
        var finished = false
        fun done() {
            if (finished) return
            finished = true
            lastInterstitialAt = SystemClock.elapsedRealtime()
            preloadInterstitial(activity)
            then()
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { lastInterstitialAt = SystemClock.elapsedRealtime() }
            override fun onAdDismissedFullScreenContent() = done()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = done()
        }
        ad.show(activity)
    }
}
