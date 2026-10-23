package com.walkies.dogwalk.ads

import android.app.Activity
import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns consent (Google UMP, required for users in the EEA, UK and Switzerland) and the Mobile Ads
 * SDK. No ad is requested until consent allows it.
 */
class AdsManager(private val app: Application) {

    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val initializeCalled = AtomicBoolean(false)

    private val _adsReady = MutableStateFlow(false)
    /** True once consent allows ads and the SDK has been started. */
    val adsReady: StateFlow<Boolean> = _adsReady.asStateFlow()

    private val _consentResolved = MutableStateFlow(false)
    /** True once this launch's consent check has finished (successfully or not). */
    val consentResolved: StateFlow<Boolean> = _consentResolved.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    val interstitials = InterstitialController(app) { _adsReady.value }
    val nativeAds = NativeAdCache(app) { _adsReady.value }

    /** Call on every launch from the activity, as the UMP SDK requires. */
    fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    onConsentFinished()
                }
            },
            { onConsentFinished() },
        )
        // Consent given in an earlier session lets ads start while the update runs.
        if (consentInformation.canRequestAds()) startSdk()
    }

    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            _privacyOptionsRequired.value = isPrivacyOptionsRequired()
            onDone()
        }
    }

    private fun onConsentFinished() {
        _privacyOptionsRequired.value = isPrivacyOptionsRequired()
        if (consentInformation.canRequestAds()) startSdk()
        _consentResolved.value = true
    }

    private fun isPrivacyOptionsRequired(): Boolean =
        consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    private fun startSdk() {
        if (initializeCalled.getAndSet(true)) return
        scope.launch {
            withContext(Dispatchers.IO) { MobileAds.initialize(app) {} }
            _adsReady.value = true
            interstitials.preload()
        }
    }
}
