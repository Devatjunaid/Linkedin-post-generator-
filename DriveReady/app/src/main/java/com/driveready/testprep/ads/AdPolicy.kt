package com.driveready.testprep.ads

/** Pure, unit-testable rules for how often ads may be requested and shown. */
object AdPolicy {
    /** Banner: the first two load attempts are made back-to-back. */
    const val BANNER_IMMEDIATE_ATTEMPTS = 2

    /** Banner: after two failed attempts, wait this long before each further request. */
    const val BANNER_RETRY_DELAY_MS = 15_000L

    /** Interstitial frequency cap: at most one interstitial every 30 seconds. */
    const val INTERSTITIAL_INTERVAL_MS = 30_000L

    /**
     * Delay before the next banner request, given how many consecutive attempts have failed.
     * Attempt 1 fails → retry immediately (attempt 2). Attempt 2 fails → wait 15 s, and keep
     * waiting 15 s between every attempt after that until one succeeds.
     */
    fun bannerRetryDelayMs(consecutiveFailures: Int): Long =
        if (consecutiveFailures < BANNER_IMMEDIATE_ATTEMPTS) 0L else BANNER_RETRY_DELAY_MS

    /** Whether an interstitial may be shown now, given when the last one was shown (0 = never). */
    fun canShowInterstitial(nowMs: Long, lastShownMs: Long): Boolean =
        lastShownMs == 0L || nowMs - lastShownMs >= INTERSTITIAL_INTERVAL_MS
}
