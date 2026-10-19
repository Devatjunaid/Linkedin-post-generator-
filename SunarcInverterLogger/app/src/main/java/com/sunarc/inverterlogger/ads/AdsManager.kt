package com.sunarc.inverterlogger.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
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
import com.sunarc.inverterlogger.BuildConfig
import com.sunarc.inverterlogger.domain.AdConfig
import com.sunarc.inverterlogger.domain.FrequencyCap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consent first, then the Mobile Ads SDK. No ad is requested before the Google User Messaging
 * Platform says ads may be requested (GDPR/UK GDPR and US state privacy messages are set up in
 * the AdMob console under Privacy & messaging).
 */
class AdsManager(private val appContext: Context) {
    private val consentInfo: ConsentInformation = UserMessagingPlatform.getConsentInformation(appContext)
    private val initStarted = AtomicBoolean(false)

    private val _ready = MutableStateFlow(false)
    /** True once ads may be requested and the SDK is initialised. */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    val interstitials = InterstitialController(appContext, this)

    fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    _privacyOptionsRequired.value = isPrivacyOptionsRequired()
                    if (consentInfo.canRequestAds()) initialize()
                }
            },
            { _ ->
                // Could not reach the consent service: use the consent from the last session, if any.
                if (consentInfo.canRequestAds()) initialize()
            }
        )
        // Consent given in an earlier session lets ads start loading right away.
        if (consentInfo.canRequestAds()) initialize()
    }

    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ ->
            _privacyOptionsRequired.value = isPrivacyOptionsRequired()
            onDone()
        }
    }

    private fun isPrivacyOptionsRequired() =
        consentInfo.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    private fun initialize() {
        if (!initStarted.compareAndSet(false, true)) return
        Thread {
            MobileAds.initialize(appContext) {
                Handler(Looper.getMainLooper()).post {
                    _ready.value = true
                    interstitials.preload()
                }
            }
        }.start()
    }

    fun request(): AdRequest = AdRequest.Builder().build()
}

/**
 * Interstitials only at the end of a major action (saving a reading, exporting, a report,
 * a network read), never on launch, exit or in the middle of a task, and at most one per 30 s.
 */
class InterstitialController(private val context: Context, private val ads: AdsManager) {
    // Counting from app start means the first interstitial can appear 30 s into a session at the earliest.
    private val cap = FrequencyCap(AdConfig.INTERSTITIAL_INTERVAL_MS).also { it.markShown() }
    private var ad: InterstitialAd? = null
    private var loading = false
    private val handler = Handler(Looper.getMainLooper())

    fun preload() {
        if (!ads.ready.value || loading || ad != null) return
        loading = true
        InterstitialAd.load(context, BuildConfig.INTERSTITIAL_AD_UNIT, ads.request(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                ad = interstitialAd
                loading = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                ad = null
                loading = false
                handler.postDelayed({ preload() }, 30_000)
            }
        })
    }

    /** Runs [then] right away when no ad is due, or after the ad is closed. */
    fun showAfterAction(activity: Activity?, then: () -> Unit) {
        val current = ad
        if (activity == null || current == null || !cap.canShow()) {
            then()
            preload()
            return
        }
        cap.markShown()
        ad = null
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                then()
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                then()
                preload()
            }
        }
        current.show(activity)
    }
}
