package app.gigometer.mileage.tracking

import app.gigometer.mileage.data.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the UI shows while a trip is being recorded. */
data class LiveTrip(
    val tripId: Long,
    val startTime: Long,
    val distanceMeters: Double = 0.0,
    val speedMps: Float = 0f,
    val accuracyM: Float? = null,
    val points: List<GeoPoint> = emptyList(),
    val stops: Int = 0,
    val paused: Boolean = false,
    val auto: Boolean = false,
    /** Selected gig platform; null = not chosen yet. */
    val platform: String? = null,
    val personal: Boolean = false,
    val hasFix: Boolean = false,
)

object TripRecorder {
    private val _live = MutableStateFlow<LiveTrip?>(null)
    val live: StateFlow<LiveTrip?> = _live.asStateFlow()

    internal fun set(value: LiveTrip?) { _live.value = value }
    internal fun update(block: (LiveTrip) -> LiveTrip) {
        _live.value?.let { _live.value = block(it) }
    }

    fun choosePlatform(platform: String) = update { it.copy(platform = platform, personal = false) }
    fun choosePersonal() = update { it.copy(personal = true, platform = null) }
}
