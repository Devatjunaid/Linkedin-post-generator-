package com.inkformlabs.pdfsigner.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val fieldCount: Int,
    val filledCount: Int,
    val signed: Boolean,
    val exported: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "signatures")
data class SignatureEntity(
    @PrimaryKey val id: String,
    /** "signature" or "initials" */
    val kind: String,
    val fileName: String,
    val width: Int,
    val height: Int,
    val createdAt: Long,
)
