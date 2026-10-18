package com.inkformlabs.pdfsigner.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

/**
 * Google User Messaging Platform (UMP): shows the GDPR / US-state privacy message where required.
 * Ads are only requested once [canRequestAds] is true.
 */
class ConsentManager(context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context.applicationContext)

    val canRequestAds: Boolean get() = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Call on every app start, from the launcher Activity. */
    fun gatherConsent(activity: Activity, onDone: (FormError?) -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    onDone(formError)
                }
            },
            { requestError -> onDone(requestError) },
        )
    }

    /** "Privacy options" entry point in Settings, required when [isPrivacyOptionsRequired]. */
    fun showPrivacyOptions(activity: Activity, onDone: (FormError?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError -> onDone(formError) }
    }
}
