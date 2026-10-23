package com.superdevlabs.petfood.portioncalc.domain

/**
 * Banner request policy: the first two consecutive failed requests are retried right away;
 * after that each new request waits [backoffMs] (15 s). A successful load resets the count.
 */
class BannerRetryPolicy(
    private val immediateAttempts: Int = 2,
    private val backoffMs: Long = 15_000L,
) {
    var consecutiveFailures = 0
        private set

    fun onLoaded() {
        consecutiveFailures = 0
    }

    /** Records a failure and returns how long to wait before the next request. */
    fun onFailed(): Long {
        consecutiveFailures++
        return if (consecutiveFailures < immediateAttempts) 0L else backoffMs
    }
}

/** Allows at most one full-screen ad per [intervalMs] (30 s). Times come from a monotonic clock. */
class FrequencyCap(private val intervalMs: Long = 30_000L) {
    private var lastShownAt: Long? = null

    fun canShow(now: Long): Boolean = lastShownAt?.let { now - it >= intervalMs } ?: true

    fun markShown(now: Long) {
        lastShownAt = now
    }
}
