package com.unpin.photoscrub.core

/**
 * Banner request cap: the first two load attempts go out back to back; once two
 * in a row have failed, every further attempt waits [backoffMs] (15 s).
 */
class BannerRetryPolicy(
    private val immediateAttempts: Int = 2,
    private val backoffMs: Long = 15_000L,
) {
    var consecutiveFailures: Int = 0
        private set

    /** Records a failure and returns how long to wait before the next request. */
    fun onFailure(): Long {
        consecutiveFailures++
        return if (consecutiveFailures < immediateAttempts) 0L else backoffMs
    }

    fun onSuccess() {
        consecutiveFailures = 0
    }
}

/** Frequency cap: at most one interstitial per [windowMs] (30 s). */
class InterstitialCap(
    private val windowMs: Long = 30_000L,
    private val clock: () -> Long,
) {
    private var lastShownAt: Long? = null

    fun canShow(): Boolean {
        val last = lastShownAt ?: return true
        return clock() - last >= windowMs
    }

    fun markShown() {
        lastShownAt = clock()
    }
}
