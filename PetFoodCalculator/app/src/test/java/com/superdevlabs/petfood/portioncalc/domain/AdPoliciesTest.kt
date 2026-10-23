package com.superdevlabs.petfood.portioncalc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPoliciesTest {
    @Test
    fun bannerRetriesOnceImmediatelyThenWaits15Seconds() {
        val policy = BannerRetryPolicy()
        assertEquals(0L, policy.onFailed())      // 1st request failed -> 2nd request now
        assertEquals(15_000L, policy.onFailed()) // 2nd request failed -> wait 15 s
        assertEquals(15_000L, policy.onFailed()) // keeps waiting 15 s between requests
        policy.onLoaded()
        assertEquals(0L, policy.onFailed())
    }

    @Test
    fun interstitialCapIsOnePer30Seconds() {
        val cap = FrequencyCap()
        assertTrue(cap.canShow(1_000))
        cap.markShown(1_000)
        assertFalse(cap.canShow(30_999))
        assertTrue(cap.canShow(31_000))
    }
}
