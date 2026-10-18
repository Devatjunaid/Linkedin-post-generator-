package com.devatjunaid.balconysolar

import com.devatjunaid.balconysolar.ads.BannerRetryPolicy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BannerRetryPolicyTest {

    @Test
    fun retriesOnceImmediatelyThenWaitsFifteenSeconds() = runTest {
        var requests = 1 // the first request is made by the banner itself
        val policy = BannerRetryPolicy(this) { requests++ }

        policy.onFailed() // first call failed → second call right away
        runCurrent()
        assertEquals(2, requests)

        policy.onFailed() // second call failed → wait 15 s
        runCurrent()
        assertEquals(2, requests)
        advanceTimeBy(14_999)
        runCurrent()
        assertEquals(2, requests)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(3, requests)

        policy.onFailed() // keeps waiting 15 s while failing
        advanceTimeBy(15_001)
        runCurrent()
        assertEquals(4, requests)

        policy.onLoaded() // success resets: next failure retries immediately again
        policy.onFailed()
        runCurrent()
        assertEquals(5, requests)
        policy.cancel()
    }
}
