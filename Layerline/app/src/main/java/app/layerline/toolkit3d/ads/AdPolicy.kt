package app.layerline.toolkit3d.ads

/**
 * Banner reload rule: the first two load calls go out back to back; once both have failed,
 * every further attempt waits [cooldownMs] (15 s) before asking AdMob again.
 * A successful load resets the counter.
 */
class BannerRetryPolicy(
    private val immediateAttempts: Int = 2,
    private val cooldownMs: Long = 15_000L,
) {
    var consecutiveFailures: Int = 0
        private set

    fun onLoaded() {
        consecutiveFailures = 0
    }

    /** Records a failure and returns how long to wait before the next request. */
    fun onFailed(): Long {
        consecutiveFailures++
        return if (consecutiveFailures < immediateAttempts) 0L else cooldownMs
    }
}

/**
 * Interstitial frequency cap: at most one ad per [intervalMs]. The window is measured from the
 * moment the previous ad was closed (and from app start, so nothing shows right at launch).
 */
class FrequencyCap(
    private val intervalMs: Long = 30_000L,
    startedAt: Long,
) {
    private var lastClosedAt: Long = startedAt

    fun canShow(now: Long): Boolean = now - lastClosedAt >= intervalMs

    fun onClosed(now: Long) {
        lastClosedAt = now
    }

    fun msUntilAllowed(now: Long): Long = (intervalMs - (now - lastClosedAt)).coerceAtLeast(0L)
}
