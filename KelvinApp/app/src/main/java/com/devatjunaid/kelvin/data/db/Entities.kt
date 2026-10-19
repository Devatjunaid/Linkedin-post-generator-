package com.devatjunaid.kelvin.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.devatjunaid.kelvin.domain.Protocol
import com.devatjunaid.kelvin.domain.SessionKind
import com.devatjunaid.kelvin.domain.SessionRecord

@Entity(tableName = "sessions", indices = [Index("startedAt")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startedAt: Long,
    val kind: String,
    val title: String,
    val totalSec: Int,
    val coldSec: Int,
    val heatSec: Int,
    val rounds: Int,
    val coldTempC: Float?,
    val heatTempC: Float?,
    val mood: Int?,
    val note: String,
)

@Entity(tableName = "protocols")
data class ProtocolEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val heatSec: Int,
    val coldSec: Int,
    val restSec: Int,
    val rounds: Int,
    val endOnCold: Boolean,
    val heatTempC: Float,
    val coldTempC: Float,
    val updatedAt: Long,
)

fun SessionEntity.toDomain() = SessionRecord(
    id = id,
    startedAt = startedAt,
    kind = runCatching { SessionKind.valueOf(kind) }.getOrDefault(SessionKind.CONTRAST),
    title = title,
    totalSec = totalSec,
    coldSec = coldSec,
    heatSec = heatSec,
    rounds = rounds,
    coldTempC = coldTempC,
    heatTempC = heatTempC,
    mood = mood,
    note = note,
)

fun SessionRecord.toEntity() = SessionEntity(
    id = id,
    startedAt = startedAt,
    kind = kind.name,
    title = title,
    totalSec = totalSec,
    coldSec = coldSec,
    heatSec = heatSec,
    rounds = rounds,
    coldTempC = coldTempC,
    heatTempC = heatTempC,
    mood = mood,
    note = note,
)

fun ProtocolEntity.toDomain() = Protocol(
    id = id,
    name = name,
    heatSec = heatSec,
    coldSec = coldSec,
    restSec = restSec,
    rounds = rounds,
    endOnCold = endOnCold,
    heatTempC = heatTempC,
    coldTempC = coldTempC,
)

fun Protocol.toEntity(now: Long) = ProtocolEntity(
    id = if (id > 0) id else 0L,
    name = name,
    heatSec = heatSec,
    coldSec = coldSec,
    restSec = restSec,
    rounds = rounds,
    endOnCold = endOnCold,
    heatTempC = heatTempC,
    coldTempC = coldTempC,
    updatedAt = now,
)
