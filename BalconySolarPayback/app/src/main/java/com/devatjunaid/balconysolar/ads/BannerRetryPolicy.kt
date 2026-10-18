package com.devatjunaid.balconysolar.ads

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Banner retry policy: the first failed request is retried at once (second call); after two
 * consecutive failures every further request waits [BACKOFF_MS]. A success resets the count.
 */
internal class BannerRetryPolicy(private val scope: CoroutineScope, private val load: () -> Unit) {
    private var failures = 0
    private var job: Job? = null

    fun onLoaded() { failures = 0; job?.cancel() }

    fun onFailed() {
        failures++
        job?.cancel()
        job = scope.launch {
            if (failures >= IMMEDIATE_ATTEMPTS) delay(BACKOFF_MS)
            load()
        }
    }

    fun cancel() { job?.cancel() }

    companion object {
        const val IMMEDIATE_ATTEMPTS = 2
        const val BACKOFF_MS = 15_000L
    }
}
