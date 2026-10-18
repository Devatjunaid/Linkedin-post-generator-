package com.inkformlabs.pdfsigner

object AppConfig {
    /**
     * REQUIRED before publishing: Google Play requires a privacy policy for apps that show ads.
     * Host your policy page and put its address here.
     */
    const val PRIVACY_POLICY_URL = "https://example.com/inkform/privacy-policy"

    /** Minimum time between two interstitial ads. */
    const val INTERSTITIAL_INTERVAL_MS = 30_000L

    /** Banner retry policy: after this many failed requests in a row, wait [BANNER_RETRY_DELAY_MS]. */
    const val BANNER_FAST_ATTEMPTS = 2
    const val BANNER_RETRY_DELAY_MS = 15_000L
}
