package com.devatjunaid.examclock.ads

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import com.devatjunaid.examclock.BuildConfig
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns the consent flow and the Mobile Ads SDK lifecycle.
 *
 * Ads are only requested after Google's User Messaging Platform says consent allows it
 * (GDPR / UK / Swiss users see the consent form; others continue straight away). The SDK is
 * initialised once, off the main thread, and [ready] flips to true afterwards.
 */
class AdsManager(private val app: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val initStarted = AtomicBoolean(false)
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(app)

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    val interstitial = InterstitialController(app, BuildConfig.INTERSTITIAL_AD_UNIT_ID) { _ready.value }

    val bannerUnitId: String get() = BuildConfig.BANNER_AD_UNIT_ID
    val nativeUnitId: String get() = BuildConfig.NATIVE_AD_UNIT_ID

    /** Call from the activity's onCreate. Safe to call on every launch. */
    fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    refreshPrivacyStatus()
                    if (consentInformation.canRequestAds()) initializeSdk()
                }
            },
            {
                // Network error etc.: fall back to the consent stored from a previous session.
                if (consentInformation.canRequestAds()) initializeSdk()
            },
        )
        // Consent obtained in an earlier session lets ads start loading while the update runs.
        if (consentInformation.canRequestAds()) initializeSdk()
    }

    /** Opens the "Privacy options" form so users can change their ad consent at any time. */
    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            refreshPrivacyStatus()
            onDone()
        }
    }

    private fun refreshPrivacyStatus() {
        _privacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun initializeSdk() {
        refreshPrivacyStatus()
        if (!initStarted.compareAndSet(false, true)) return
        // Study app used by teenagers: keep ad content suitable for teens.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
                .build(),
        )
        scope.launch {
            MobileAds.initialize(app) {
                mainHandler.post {
                    _ready.value = true
                    interstitial.preload()
                }
            }
        }
    }
}
