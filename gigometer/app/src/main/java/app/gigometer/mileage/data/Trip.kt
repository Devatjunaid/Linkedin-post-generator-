package app.gigometer.mileage.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object Category {
    const val BUSINESS = "BUSINESS"
    const val PERSONAL = "PERSONAL"
    const val UNREVIEWED = "UNREVIEWED"
}

@Entity(tableName = "trips", indices = [Index("startTime"), Index("category")])
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    /** Null while the trip is still being recorded. */
    val endTime: Long? = null,
    val distanceMeters: Double = 0.0,
    val startLat: Double? = null,
    val startLng: Double? = null,
    val endLat: Double? = null,
    val endLng: Double? = null,
    val startAddress: String? = null,
    val endAddress: String? = null,
    val category: String = Category.UNREVIEWED,
    val platform: String? = null,
    val purpose: String = "",
    /** Google encoded-polyline of the recorded GPS route (empty for manual trips). */
    val route: String = "",
    val isManual: Boolean = false,
    val autoDetected: Boolean = false,
    /** Last time the recorder checkpointed this trip (used to recover after a crash). */
    val updatedAt: Long = 0,
) {
    val miles: Double get() = distanceMeters / METERS_PER_MILE
    val durationMs: Long get() = (endTime ?: System.currentTimeMillis()) - startTime
    val isBusiness: Boolean get() = category == Category.BUSINESS

    companion object {
        const val METERS_PER_MILE = 1609.344
    }
}
