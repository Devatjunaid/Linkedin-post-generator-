package app.gigometer.mileage.tracking

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import app.gigometer.mileage.Graph
import app.gigometer.mileage.data.Category
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Automatic drive detection using Google Play services' Activity Recognition
 * transitions (low power: the phone's motion sensors decide when you're in a vehicle).
 */
object AutoDetect {
    private const val TAG = "AutoDetect"

    private fun pendingIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, ActivityTransitionReceiver::class.java).setAction("app.gigometer.TRANSITION")
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        return PendingIntent.getBroadcast(ctx, 7, intent, flags)
    }

    @SuppressLint("MissingPermission")
    suspend fun enable(ctx: Context): Boolean {
        if (!Permissions.hasActivityRecognition(ctx)) return false
        val transitions = listOf(
            ActivityTransition.Builder().setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
            ActivityTransition.Builder().setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build(),
        )
        return try {
            ActivityRecognition.getClient(ctx)
                .requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), pendingIntent(ctx))
                .await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "enable failed", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun disable(ctx: Context) {
        try {
            if (Permissions.hasActivityRecognition(ctx)) {
                ActivityRecognition.getClient(ctx).removeActivityTransitionUpdates(pendingIntent(ctx)).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "disable failed", e)
        }
    }

    /** Re-applies the user's preference (called on app start, boot and app update). */
    suspend fun sync(ctx: Context) {
        val s = Graph.settings.current()
        if (s.autoDetect && Permissions.autoReady(ctx)) enable(ctx) else disable(ctx)
    }

    /**
     * If the app was killed mid-trip, close the leftover open trip using its last checkpoint.
     */
    suspend fun recoverOrphanedTrip() {
        if (TripRecorder.live.value != null) return
        val open = Graph.trips.openTrip() ?: return
        if (open.distanceMeters < 160) {
            Graph.trips.delete(open)
        } else {
            Graph.trips.update(open.copy(endTime = maxOf(open.updatedAt, open.startTime + 60_000), category = Category.UNREVIEWED))
        }
    }
}

class ActivityTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val last = result.transitionEvents.lastOrNull { it.activityType == DetectedActivity.IN_VEHICLE } ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val settings = Graph.settings.current()
                when (last.transitionType) {
                    ActivityTransition.ACTIVITY_TRANSITION_ENTER ->
                        if (settings.autoDetect && Permissions.autoReady(context) && TripRecorder.live.value == null) {
                            TrackingService.send(context, TrackingService.ACTION_START_AUTO)
                        }
                    ActivityTransition.ACTIVITY_TRANSITION_EXIT ->
                        if (TripRecorder.live.value != null) TrackingService.send(context, TrackingService.ACTION_VEHICLE_EXIT)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                AutoDetect.sync(context)
                AutoDetect.recoverOrphanedTrip()
            } finally {
                pending.finish()
            }
        }
    }
}
