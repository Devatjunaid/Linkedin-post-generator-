package com.devatjunaid.examclock.domain

/**
 * Retry schedule for ad requests that fail before any ad has been shown:
 * the first [immediateAttempts] requests go out back to back, after that every new request
 * waits [backoffMillis]. A successful load resets the counter.
 */
class AdRetryPolicy(
    private val immediateAttempts: Int = 2,
    private val backoffMillis: Long = 15_000L,
) {
    var consecutiveFailures: Int = 0
        private set

    fun onSuccess() {
        consecutiveFailures = 0
    }

    /** Records a failure and returns how long to wait before the next request. */
    fun onFailure(): Long {
        consecutiveFailures++
        return if (consecutiveFailures < immediateAttempts) 0L else backoffMillis
    }
}

/** Allows at most one event per [intervalMillis], measured from the last [markShown]. */
class FrequencyCap(
    private val intervalMillis: Long = 30_000L,
    private val clock: () -> Long,
) {
    private var lastShownAt: Long? = null

    fun canShow(): Boolean {
        val last = lastShownAt ?: return true
        return clock() - last >= intervalMillis
    }

    fun markShown() {
        lastShownAt = clock()
    }
}
