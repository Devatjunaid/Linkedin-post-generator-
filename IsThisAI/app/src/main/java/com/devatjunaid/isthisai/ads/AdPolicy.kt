package com.devatjunaid.isthisai.ads

/** Ad request and frequency rules, kept free of Android types so they are unit-tested. */
object AdPolicy {
    /** The first two banner requests go out back to back. */
    const val BANNER_FAST_ATTEMPTS = 2

    /** After two failed banner requests, wait this long before each new request. */
    const val BANNER_RETRY_DELAY_MS = 15_000L

    /** At most one interstitial per 30 seconds. */
    const val INTERSTITIAL_MIN_INTERVAL_MS = 30_000L

    /**
     * Delay before the next banner request, given how many requests in a row have failed.
     * Failure 1 -> retry immediately (2nd call). Failure 2 and later -> wait 15 s.
     */
    fun bannerDelayAfterFailure(consecutiveFailures: Int): Long =
        if (consecutiveFailures < BANNER_FAST_ATTEMPTS) 0L else BANNER_RETRY_DELAY_MS

    /** Back-off for interstitial and native preloads: 15 s, 30 s, 45 s, then every 60 s. */
    fun preloadRetryDelay(consecutiveFailures: Int): Long =
        (15_000L * consecutiveFailures.coerceAtLeast(1)).coerceAtMost(60_000L)

    /** Native slots retry a failed request at most twice. */
    const val NATIVE_MAX_RETRIES = 2
}

/** Allows one event per [intervalMs]. Time comes from the caller so tests can control it. */
class FrequencyCap(private val intervalMs: Long) {
    private var lastShownAt: Long? = null

    fun canShow(now: Long): Boolean = lastShownAt?.let { now - it >= intervalMs } ?: true

    fun markShown(now: Long) {
        lastShownAt = now
    }
}
