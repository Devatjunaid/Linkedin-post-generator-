package com.odomate.app.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromIds(ids: List<Long>): String = ids.joinToString(",")

    @TypeConverter
    fun toIds(raw: String): List<Long> =
        if (raw.isBlank()) emptyList() else raw.split(",").mapNotNull { it.trim().toLongOrNull() }
}
