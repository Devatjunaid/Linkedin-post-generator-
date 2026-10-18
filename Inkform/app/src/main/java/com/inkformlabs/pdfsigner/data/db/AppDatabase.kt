package com.inkformlabs.pdfsigner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DocumentEntity::class, SignatureEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun signatureDao(): SignatureDao
}
