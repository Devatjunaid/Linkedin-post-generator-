package com.walkies.dogwalk.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LogType { PEE, POO, MEAL }

enum class Consistency { FIRM, SOFT, RUNNY }

/** One potty break or meal. Entries logged during a walk carry that walk's id. */
@Entity(tableName = "log_entries", indices = [Index("timestamp"), Index("walkId")])
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: LogType,
    val timestamp: Long,
    val consistency: Consistency? = null,
    val walkId: Long? = null,
)

/** A walk; [endTime] is null while it is still running. */
@Entity(tableName = "walks", indices = [Index("startTime")])
data class Walk(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    /** Phone step counter reading (steps since boot) when the walk started. */
    val startSteps: Long? = null,
    val steps: Long? = null,
)

fun Walk.durationMillis(now: Long = System.currentTimeMillis()): Long = (endTime ?: now) - startTime

enum class ReminderKind { WALK, POTTY }

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: ReminderKind,
    val label: String,
    val hour: Int,
    val minute: Int,
    /** Bit 0 = Monday … bit 6 = Sunday. */
    val daysMask: Int = ALL_DAYS,
    val durationMinutes: Int? = null,
    val enabled: Boolean = true,
) {
    companion object {
        const val ALL_DAYS = 0b1111111
        const val WEEKDAYS = 0b0011111
        const val WEEKENDS = 0b1100000
    }
}

/** Pee/poo count per walk, for the walk history. */
data class WalkCount(val walkId: Long, val type: String, val count: Int)
