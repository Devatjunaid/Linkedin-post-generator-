package com.devatjunaid.kelvin.data

import com.devatjunaid.kelvin.data.db.ProtocolDao
import com.devatjunaid.kelvin.data.db.SessionDao
import com.devatjunaid.kelvin.data.db.toDomain
import com.devatjunaid.kelvin.data.db.toEntity
import com.devatjunaid.kelvin.domain.Protocol
import com.devatjunaid.kelvin.domain.SessionRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepository(private val dao: SessionDao) {
    val sessions: Flow<List<SessionRecord>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    suspend fun all(): List<SessionRecord> = dao.getAll().map { it.toDomain() }
    suspend fun save(record: SessionRecord): Long = dao.insert(record.toEntity())
    suspend fun delete(id: Long) = dao.delete(id)
}

class ProtocolRepository(private val dao: ProtocolDao) {
    val custom: Flow<List<Protocol>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    suspend fun save(protocol: Protocol): Long = dao.upsert(protocol.toEntity(System.currentTimeMillis()))
    suspend fun delete(id: Long) = dao.delete(id)
}
