package app.gigometer.mileage.tracking

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.gigometer.mileage.Format
import app.gigometer.mileage.Graph
import app.gigometer.mileage.Notifications
import app.gigometer.mileage.R
import app.gigometer.mileage.data.Category
import app.gigometer.mileage.data.GeoPoint
import app.gigometer.mileage.data.IrsRates
import app.gigometer.mileage.data.Polyline
import app.gigometer.mileage.data.Trip
import app.gigometer.mileage.data.isWithinWorkHours
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/**
 * Foreground service that records one trip from GPS. Started manually from the app or
 * automatically by [ActivityTransitionReceiver] when the phone detects driving.
 */
class TrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private var lastAccepted: Location? = null
    private var lastPointAdded: Location? = null
    private var lastMoveAt = 0L
    private var stoppedSince: Long? = null
    private var countedCurrentStop = false
    private var vehicleExited = false
    private var lastCheckpoint = 0L
    private var lastNotifUpdate = 0L
    private var watchdog: Job? = null
    private var finishing = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach(::onLocation)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MANUAL -> start(auto = false)
            ACTION_START_AUTO -> start(auto = true)
            ACTION_STOP -> finish()
            ACTION_PAUSE -> TripRecorder.update { it.copy(paused = true) }.also { refreshNotification(force = true) }
            ACTION_RESUME -> {
                lastAccepted = null // don't count the distance travelled while paused
                TripRecorder.update { it.copy(paused = false) }
                refreshNotification(force = true)
            }
            ACTION_VEHICLE_EXIT -> vehicleExited = true
            else -> if (TripRecorder.live.value == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun start(auto: Boolean) {
        if (TripRecorder.live.value != null) {
            // Already recording: a manual start "adopts" an automatic trip.
            if (!auto) TripRecorder.update { it.copy(auto = false) }
            return
        }
        if (!Permissions.hasLocation(this)) {
            stopSelf(); return
        }
        val now = System.currentTimeMillis()
        val placeholder = LiveTrip(tripId = -1, startTime = now, auto = auto)
        TripRecorder.set(placeholder)
        try {
            ServiceCompat.startForeground(
                this, Notifications.ID_TRACKING, buildNotification(placeholder),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
        } catch (e: Exception) {
            // Android refused a background start (missing "Allow all the time"). Give up quietly.
            Log.w(TAG, "startForeground refused", e)
            TripRecorder.set(null)
            stopSelf(); return
        }
        lastMoveAt = now
        vehicleExited = false
        scope.launch {
            val settings = Graph.settings.current()
            val id = withContext(Dispatchers.IO) {
                Graph.trips.insert(Trip(startTime = now, autoDetected = auto, updatedAt = now))
            }
            TripRecorder.update { it.copy(tripId = id, platform = if (auto) null else settings.lastPlatform) }
            val request = if (settings.batterySaver) {
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
                    .setMinUpdateIntervalMillis(3_000L)
                    .setMinUpdateDistanceMeters(10f)
                    .build()
            } else {
                LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
                    .setMinUpdateIntervalMillis(1_000L)
                    .build()
            }
            try {
                fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
            } catch (e: SecurityException) {
                Log.w(TAG, "location permission revoked", e)
                finish()
                return@launch
            }
            startWatchdog()
        }
    }

    private fun onLocation(loc: Location) {
        val live = TripRecorder.live.value ?: return
        if (live.paused || finishing) return
        if (loc.hasAccuracy() && loc.accuracy > MAX_ACCURACY_M) {
            TripRecorder.update { it.copy(accuracyM = loc.accuracy) }
            return
        }
        val now = System.currentTimeMillis()
        val prev = lastAccepted
        var added = 0.0
        if (prev == null) {
            lastAccepted = loc
            addPoint(loc)
        } else {
            val d = prev.distanceTo(loc).toDouble()
            val dt = (loc.time - prev.time) / 1000.0
            if (dt <= 0) return
            if (d / dt > MAX_SPEED_MPS) return // GPS jump, ignore
            val threshold = maxOf(MIN_STEP_M, (if (loc.hasAccuracy()) loc.accuracy.toDouble() else 10.0) * 0.6)
            if (d >= threshold) {
                added = d
                lastAccepted = loc
                addPoint(loc)
            }
        }
        val speed = if (loc.hasSpeed()) loc.speed else 0f
        val moving = speed > STOPPED_SPEED_MPS || added > 0
        if (moving) {
            lastMoveAt = now
            stoppedSince = null
            countedCurrentStop = false
        } else if (stoppedSince == null) {
            stoppedSince = now
        }
        val newStop = !moving && !countedCurrentStop && stoppedSince != null && now - stoppedSince!! > STOP_COUNT_MS
        if (newStop) countedCurrentStop = true

        TripRecorder.update {
            it.copy(
                distanceMeters = it.distanceMeters + added,
                speedMps = if (moving) speed else 0f,
                accuracyM = if (loc.hasAccuracy()) loc.accuracy else null,
                points = if (lastPointAdded === loc) it.points + GeoPoint(loc.latitude, loc.longitude) else it.points,
                stops = it.stops + if (newStop) 1 else 0,
                hasFix = true,
            )
        }
        refreshNotification(force = false)
        if (now - lastCheckpoint > CHECKPOINT_MS) checkpoint()
    }

    private fun addPoint(loc: Location) {
        val last = lastPointAdded
        if (last == null || last.distanceTo(loc) >= POINT_SPACING_M) lastPointAdded = loc
    }

    /** Ends automatic trips once the car has been parked for a while. */
    private fun startWatchdog() {
        watchdog?.cancel()
        watchdog = scope.launch {
            while (isActive) {
                delay(15_000)
                val live = TripRecorder.live.value ?: break
                if (!live.auto || live.paused) continue
                val idle = System.currentTimeMillis() - lastMoveAt
                val limit = if (vehicleExited) PARKED_AFTER_EXIT_MS else PARKED_LIMIT_MS
                if (idle > limit) { finish(); break }
            }
        }
    }

    private fun checkpoint() {
        val live = TripRecorder.live.value ?: return
        if (live.tripId <= 0) return
        lastCheckpoint = System.currentTimeMillis()
        scope.launch(Dispatchers.IO) {
            val t = Graph.trips.byId(live.tripId) ?: return@launch
            Graph.trips.update(
                t.copy(
                    distanceMeters = live.distanceMeters,
                    route = Polyline.encode(live.points),
                    startLat = live.points.firstOrNull()?.lat,
                    startLng = live.points.firstOrNull()?.lng,
                    updatedAt = lastCheckpoint,
                )
            )
        }
    }

    private fun finish() {
        if (finishing) return
        finishing = true
        watchdog?.cancel()
        fused.removeLocationUpdates(callback)
        val live = TripRecorder.live.value
        TripRecorder.set(null)
        if (live == null || live.tripId <= 0) {
            stopEverything(); return
        }
        scope.launch {
            withContext(Dispatchers.IO + NonCancellable) { save(live) }
            stopEverything()
        }
    }

    private suspend fun save(live: LiveTrip) {
        val dao = Graph.trips
        val existing = dao.byId(live.tripId) ?: return
        if (live.distanceMeters < MIN_TRIP_M) {
            dao.delete(existing); return
        }
        val settings = Graph.settings.current()
        val now = System.currentTimeMillis()
        val minute = Instant.ofEpochMilli(live.startTime).atZone(ZoneId.systemDefault()).let { it.hour * 60 + it.minute }
        val category = when {
            live.personal -> Category.PERSONAL
            live.platform != null -> Category.BUSINESS
            settings.workHoursAutoBusiness && settings.isWithinWorkHours(minute) -> Category.BUSINESS
            else -> Category.UNREVIEWED
        }
        val first = live.points.firstOrNull()
        val last = live.points.lastOrNull()
        val (from, to) = if (settings.lookupAddresses) {
            Geo.shortAddress(this, first) to Geo.shortAddress(this, last)
        } else null to null
        val trip = existing.copy(
            endTime = now,
            distanceMeters = live.distanceMeters,
            startLat = first?.lat, startLng = first?.lng,
            endLat = last?.lat, endLng = last?.lng,
            startAddress = from, endAddress = to,
            category = category,
            platform = live.platform,
            route = Polyline.encode(live.points),
            autoDetected = live.auto,
            updatedAt = now,
        )
        dao.update(trip)
        if (live.platform != null) Graph.settings.setLastPlatform(live.platform)

        val miles = Format.miles(trip.miles)
        val text = if (category == Category.BUSINESS)
            getString(R.string.notif_saved_business, miles, Format.money(IrsRates.deduction(trip, settings.rates)))
        else if (category == Category.PERSONAL) getString(R.string.notif_saved_personal, miles)
        else getString(R.string.notif_saved_review, miles)
        Notifications.post(this, Notifications.ID_SAVED, getString(R.string.notif_saved_title), text, Notifications.OPEN_TRIPS)
    }

    private fun stopEverything() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun refreshNotification(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastNotifUpdate < 5_000) return
        lastNotifUpdate = now
        val live = TripRecorder.live.value ?: return
        if (Permissions.hasNotifications(this)) {
            NotificationManagerCompat.from(this).notify(Notifications.ID_TRACKING, buildNotification(live))
        }
    }

    private fun buildNotification(live: LiveTrip): Notification {
        val miles = Format.milesPrecise(live.distanceMeters / Trip.METERS_PER_MILE)
        val title = when {
            live.paused -> getString(R.string.notif_paused)
            live.auto -> getString(R.string.notif_recording_auto)
            else -> getString(R.string.notif_recording)
        }
        val pauseResume = if (live.paused)
            NotificationCompat.Action(0, getString(R.string.resume), servicePending(ACTION_RESUME, 2))
        else NotificationCompat.Action(0, getString(R.string.pause), servicePending(ACTION_PAUSE, 3))
        return NotificationCompat.Builder(this, Notifications.CHANNEL_TRACKING)
            .setSmallIcon(R.drawable.ic_stat_gauge)
            .setColor(0xFF1B6B45.toInt())
            .setContentTitle(title)
            .setContentText(getString(R.string.notif_distance, miles))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setWhen(live.startTime)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(Notifications.openAppIntent(this, Notifications.OPEN_LIVE, 1))
            .addAction(pauseResume)
            .addAction(NotificationCompat.Action(0, getString(R.string.end_trip), servicePending(ACTION_STOP, 4)))
            .build()
    }

    private fun servicePending(action: String, code: Int): PendingIntent =
        PendingIntent.getService(
            this, code, Intent(this, TrackingService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        val live = TripRecorder.live.value
        if (live != null && !finishing) {
            // System is killing us mid-trip: keep what we have.
            TripRecorder.set(null)
            kotlinx.coroutines.runBlocking(Dispatchers.IO) { runCatching { save(live) } }
        }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "TrackingService"
        const val ACTION_START_MANUAL = "app.gigometer.START_MANUAL"
        const val ACTION_START_AUTO = "app.gigometer.START_AUTO"
        const val ACTION_STOP = "app.gigometer.STOP"
        const val ACTION_PAUSE = "app.gigometer.PAUSE"
        const val ACTION_RESUME = "app.gigometer.RESUME"
        const val ACTION_VEHICLE_EXIT = "app.gigometer.VEHICLE_EXIT"

        private const val MAX_ACCURACY_M = 40f
        private const val MAX_SPEED_MPS = 60.0 // ~134 mph
        private const val MIN_STEP_M = 8.0
        private const val POINT_SPACING_M = 25f
        private const val STOPPED_SPEED_MPS = 1.2f
        private const val STOP_COUNT_MS = 45_000L
        private const val PARKED_LIMIT_MS = 4 * 60_000L
        private const val PARKED_AFTER_EXIT_MS = 75_000L
        private const val CHECKPOINT_MS = 30_000L
        private const val MIN_TRIP_M = 160.0 // ~0.1 mi

        fun send(context: Context, action: String): Boolean = try {
            val i = Intent(context, TrackingService::class.java).setAction(action)
            if (action == ACTION_START_AUTO || action == ACTION_START_MANUAL) ContextCompat.startForegroundService(context, i)
            else context.startService(i)
            true
        } catch (e: Exception) {
            Log.w(TAG, "could not send $action", e)
            false
        }
    }
}
