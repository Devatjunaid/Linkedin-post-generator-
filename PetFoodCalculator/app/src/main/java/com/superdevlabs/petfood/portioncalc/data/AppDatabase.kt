package com.superdevlabs.petfood.portioncalc.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PetEntity::class, FoodEntity::class, FeedingLogEntity::class, WeightLogEntity::class, WikiCacheEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun foodDao(): FoodDao
    abstract fun feedingLogDao(): FeedingLogDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun wikiCacheDao(): WikiCacheDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "pet_food.db").build()
    }
}
