package com.superdevlabs.autosubtitles.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google User Messaging Platform (UMP): shows the GDPR / US-state privacy message when it is
 * required, and only lets ads load once the user's choice allows it. Required by AdMob policy.
 */
class ConsentManager(context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context.applicationContext)

    val canRequestAds: Boolean get() = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Updates consent info and shows the consent form if needed. [onReady] is called at most
     * once, as soon as ads may be requested (immediately if consent was given in a past session).
     */
    fun gather(activity: Activity, onReady: () -> Unit) {
        val delivered = AtomicBoolean(false)
        fun deliverIfAllowed() {
            if (consentInformation.canRequestAds() && delivered.compareAndSet(false, true)) onReady()
        }

        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> deliverIfAllowed() }
            },
            { _ -> deliverIfAllowed() },
        )
        // Consent obtained in a previous session: start loading ads right away.
        deliverIfAllowed()
    }

    fun showPrivacyOptions(activity: Activity, onDismissed: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> onDismissed() }
    }
}
