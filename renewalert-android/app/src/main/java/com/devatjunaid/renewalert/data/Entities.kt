package com.devatjunaid.renewalert.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class CycleUnit { DAY, WEEK, MONTH, YEAR }

enum class SubStatus { ACTIVE, TRIAL, PAUSED, CANCELLED }

enum class Usage { UNKNOWN, DAILY, WEEKLY, MONTHLY, RARELY }

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Price per billing cycle in minor units (cents). */
    val priceMinor: Long,
    val currency: String,
    val cycleUnit: CycleUnit = CycleUnit.MONTH,
    val cycleCount: Int = 1,
    /** Date of the first (paid) charge. For a trial this is the trial end date. */
    val firstPaymentEpochDay: Long,
    val category: String = "Other",
    val paymentMethod: String = "",
    val notes: String = "",
    val status: SubStatus = SubStatus.ACTIVE,
    val trialEndEpochDay: Long? = null,
    /** -1 = no reminder. */
    val reminderDaysBefore: Int = 3,
    val usage: Usage = Usage.UNKNOWN,
    val createdAtMillis: Long = System.currentTimeMillis(),
    /** Date the subscription was paused or cancelled. */
    val statusChangedEpochDay: Long? = null,
)

@Entity(
    tableName = "price_points",
    foreignKeys = [
        ForeignKey(
            entity = Subscription::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subscriptionId")],
)
data class PricePoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subscriptionId: Long,
    val priceMinor: Long,
    val effectiveEpochDay: Long,
)

class Converters {
    @TypeConverter fun cycleToString(v: CycleUnit): String = v.name
    @TypeConverter fun stringToCycle(v: String): CycleUnit = CycleUnit.valueOf(v)
    @TypeConverter fun statusToString(v: SubStatus): String = v.name
    @TypeConverter fun stringToStatus(v: String): SubStatus = SubStatus.valueOf(v)
    @TypeConverter fun usageToString(v: Usage): String = v.name
    @TypeConverter fun stringToUsage(v: String): Usage = Usage.valueOf(v)
}
