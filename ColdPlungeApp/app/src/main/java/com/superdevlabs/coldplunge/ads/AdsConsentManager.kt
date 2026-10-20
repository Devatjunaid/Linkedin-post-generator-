package com.superdevlabs.coldplunge.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
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
 * Google UMP consent (GDPR / US state privacy messages) gate for AdMob.
 * No ad is requested until the user has had the chance to answer the consent form and
 * [ConsentInformation.canRequestAds] is true; only then is the Mobile Ads SDK initialized.
 */
class AdsConsentManager(private val app: Context) {

    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(app)
    private val sdkStarted = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _adsReady = MutableStateFlow(false)
    /** True once consent allows ads AND the Mobile Ads SDK finished initializing. */
    val adsReady: StateFlow<Boolean> = _adsReady.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** When true, Settings must offer a "Privacy options" entry (required by Google for EEA/UK users). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> refresh() }
            },
            { _ -> refresh() },
        )
        // Consent given in an earlier session may already allow ads; don't wait for the network.
        refresh()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> refresh() }
    }

    private fun refresh() {
        _privacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (consentInformation.canRequestAds() && sdkStarted.compareAndSet(false, true)) {
            scope.launch {
                MobileAds.initialize(app) { _adsReady.value = true }
            }
        }
    }
}
