package com.devatjunaid.kelvin.ads

/**
 * Banner request policy: the first [immediateAttempts] requests go out back to back;
 * after that every failed request waits [retryDelayMs] before the next one.
 * A successful load resets the counter.
 */
class BannerRetryPolicy(
    private val immediateAttempts: Int = 2,
    private val retryDelayMs: Long = 15_000L,
) {
    var failures: Int = 0
        private set

    fun onLoaded() {
        failures = 0
    }

    /** Records a failure and returns how long to wait before the next request. */
    fun onFailed(): Long {
        failures++
        return if (failures < immediateAttempts) 0L else retryDelayMs
    }
}

/** Allows at most one event per [intervalMs]. Time comes from [clock] (monotonic millis). */
class FrequencyCap(
    private val intervalMs: Long = 30_000L,
    private val clock: () -> Long,
) {
    private var lastShownAt: Long? = null

    fun canShow(): Boolean = lastShownAt?.let { clock() - it >= intervalMs } ?: true

    fun markShown() {
        lastShownAt = clock()
    }

    fun msUntilAllowed(): Long = lastShownAt?.let { (intervalMs - (clock() - it)).coerceAtLeast(0L) } ?: 0L
}
