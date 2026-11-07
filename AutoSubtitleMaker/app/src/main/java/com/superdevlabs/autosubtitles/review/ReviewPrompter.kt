package com.superdevlabs.autosubtitles.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import com.superdevlabs.autosubtitles.AppContainer
import com.superdevlabs.autosubtitles.util.await
import kotlinx.coroutines.CancellationException

/**
 * Google Play in-app review, asked at a happy moment: right after the 2nd, 5th, 10th…
 * successful export, never more than once every 45 days. Google also limits how often the
 * sheet really appears, and may show nothing — the app never knows or asks about the result
 * (Play policy: no incentives, no "rate 5 stars" wording, no gating features on it).
 */
object ReviewPrompter {

    private const val MIN_GAP_MS = 45L * 24 * 60 * 60 * 1000
    private val milestones = setOf(2, 5, 10, 20, 40)

    suspend fun onExportFinished(activity: Activity, container: AppContainer, exportsSoFar: Int) {
        if (exportsSoFar !in milestones) return
        val settings = container.settings.current()
        val now = System.currentTimeMillis()
        if (now - settings.lastReviewPromptAt < MIN_GAP_MS) return
        try {
            val manager = ReviewManagerFactory.create(activity)
            val info = manager.requestReviewFlow().await()
            container.settings.markReviewPrompted(now)
            manager.launchReviewFlow(activity, info).await()
            container.telemetry.event("review_prompt", "exports" to exportsSoFar)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // No Play Store, quota reached, offline… silently skip.
        }
    }
}
