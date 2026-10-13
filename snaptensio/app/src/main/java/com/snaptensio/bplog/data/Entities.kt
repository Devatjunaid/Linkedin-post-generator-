package com.snaptensio.bplog.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A person whose readings are logged on this phone (family members can share one device). */
@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetSystolic: Int = 130,
    val targetDiastolic: Int = 80,
    val colorIndex: Int = 0,
)

@Entity(
    tableName = "readings",
    indices = [Index("profileId"), Index("timestamp")],
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class Reading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val timestamp: Long,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?,
    val arm: String = Arm.LEFT.name,
    val position: String = Position.SITTING.name,
    /** Comma separated tag labels. */
    val tags: String = "",
    val note: String = "",
    val source: String = Source.MANUAL.name,
    /** How many cuff measurements were averaged into this entry (guided session). */
    val averagedCount: Int = 1,
)

fun Reading.tagList(): List<String> = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }

enum class Arm(val label: String) { LEFT("Left"), RIGHT("Right") }
enum class Position(val label: String) { SITTING("Sitting"), STANDING("Standing"), LYING("Lying") }
enum class Source { MANUAL, SCAN, SESSION, IMPORT }

val DEFAULT_TAGS = listOf(
    "Took meds", "After coffee", "Stressed", "After exercise", "Poor sleep",
    "Headache", "Dizzy", "Irregular heartbeat", "After meal", "Salty food",
)

@Entity(
    tableName = "medications",
    indices = [Index("profileId")],
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val dose: String,
    /** Comma separated "HH:mm" times. */
    val times: String,
    val startDate: Long,
    val active: Boolean = true,
    val pillsLeft: Int? = null,
)

fun Medication.timeList(): List<String> = times.split(',').map { it.trim() }.filter { it.matches(Regex("\\d{2}:\\d{2}")) }

@Entity(
    tableName = "dose_logs",
    indices = [Index(value = ["medicationId", "dateKey", "time"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DoseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    /** yyyyMMdd */
    val dateKey: Int,
    val time: String,
    val takenAt: Long,
)

/** Daily "measure your blood pressure" reminder. */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean = true,
)
