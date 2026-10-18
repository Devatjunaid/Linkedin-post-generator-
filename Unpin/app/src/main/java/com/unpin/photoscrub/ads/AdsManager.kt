package com.unpin.photoscrub.ads

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
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.unpin.photoscrub.BuildConfig
import com.unpin.photoscrub.core.BannerRetryPolicy
import com.unpin.photoscrub.core.InterstitialCap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consent first (Google UMP, required for EEA/UK/CH users), then the Mobile Ads SDK.
 * No ad is requested until [ready] is true.
 */
class AdsManager(private val app: Application) {

    private val consentInfo: ConsentInformation = UserMessagingPlatform.getConsentInformation(app)
    private val initStarted = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _consentFlowDone = MutableStateFlow(false)
    val consentFlowDone: StateFlow<Boolean> = _consentFlowDone.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    val interstitial = InterstitialController(app, BuildConfig.AD_INTERSTITIAL_ID) { _ready.value }

    fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { finishConsent() }
            },
            { finishConsent() },
        )
        // Consent given in an earlier session: start right away.
        if (consentInfo.canRequestAds()) initialize()
    }

    private fun finishConsent() {
        _privacyOptionsRequired.value =
            consentInfo.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        _consentFlowDone.value = true
        if (consentInfo.canRequestAds()) initialize()
    }

    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            if (consentInfo.canRequestAds()) initialize()
            onDone()
        }
    }

    private fun initialize() {
        if (!initStarted.compareAndSet(false, true)) return
        scope.launch {
            MobileAds.initialize(app) {
                main.post {
                    _ready.value = true
                    interstitial.preload()
                }
            }
        }
    }
}

/** Interstitials: preloaded, shown only at natural breaks, max one per 30 seconds. */
class InterstitialController(
    private val app: Application,
    private val adUnitId: String,
    private val canRequest: () -> Boolean,
) {
    private val cap = InterstitialCap(windowMs = 30_000L, clock = SystemClock::elapsedRealtime)
    private val retry = BannerRetryPolicy(immediateAttempts = 1, backoffMs = 30_000L)
    private val main = Handler(Looper.getMainLooper())
    private var ad: InterstitialAd? = null
    private var loading = false

    fun preload() {
        if (ad != null || loading || !canRequest()) return
        loading = true
        InterstitialAd.load(app, adUnitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(loaded: InterstitialAd) {
                ad = loaded
                loading = false
                retry.onSuccess()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                loading = false
                main.postDelayed({ preload() }, retry.onFailure().coerceAtLeast(10_000L))
            }
        })
    }

    /** Shows an ad if one is ready and the 30 s cap allows it, then always runs [then]. */
    fun showIfAllowed(activity: Activity, then: () -> Unit) {
        val current = ad
        if (current == null || !cap.canShow()) {
            preload()
            then()
            return
        }
        ad = null
        var finished = false
        val finish = {
            if (!finished) {
                finished = true
                then()
            }
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preload()
                finish()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                preload()
                finish()
            }
        }
        cap.markShown()
        current.show(activity)
    }
}
