package com.sunarc.inverterlogger.domain

/** Allows at most one event per [intervalMillis]. Used to keep interstitials to one every 30 s. */
class FrequencyCap(
    private val intervalMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private var lastShownAt: Long? = null

    fun canShow(): Boolean {
        val last = lastShownAt ?: return true
        return clock() - last >= intervalMillis
    }

    fun millisUntilAllowed(): Long {
        val last = lastShownAt ?: return 0
        return (intervalMillis - (clock() - last)).coerceAtLeast(0)
    }

    fun markShown() {
        lastShownAt = clock()
    }
}

/**
 * Banner request pacing: the first [quickAttempts] requests go out right away; after that each
 * new request waits [backoffMillis]. A successful load resets the counter.
 */
class BannerRetryPolicy(
    private val quickAttempts: Int = 2,
    private val backoffMillis: Long = 15_000
) {
    var failedAttempts: Int = 0
        private set

    /** Delay to wait before the next request. */
    fun delayBeforeNextAttempt(): Long = if (failedAttempts < quickAttempts) 0 else backoffMillis

    fun onFailed() {
        failedAttempts++
    }

    fun onLoaded() {
        failedAttempts = 0
    }
}

object AdConfig {
    const val INTERSTITIAL_INTERVAL_MS = 30_000L
    const val BANNER_QUICK_ATTEMPTS = 2
    const val BANNER_BACKOFF_MS = 15_000L
}
