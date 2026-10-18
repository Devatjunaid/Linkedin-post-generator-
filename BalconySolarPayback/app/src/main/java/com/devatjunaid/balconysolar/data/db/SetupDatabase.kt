package com.devatjunaid.balconysolar.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** One saved calculation. The row with [DRAFT_ID] holds the setup currently being edited. */
@Entity(tableName = "setups")
data class SetupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val savedAt: Long,
    val countryCode: String,
    val cityName: String,
    val customLabel: String?,
    val customLat: Double?,
    val customLon: Double?,
    val customZone: String?,
    /** PVGIS monthly values as 12 comma-separated numbers, null when the table is used. */
    val refinedGhi: String?,
    val refinedTemp: String?,
    val refinedSource: String?,
    val panelCount: Int,
    val panelWp: Double,
    val tiltDeg: Double,
    val azimuthDeg: Double,
    val inverterAcW: Double,
    val inverterEfficiency: Double,
    val tempCoeffPct: Double,
    val moduleEfficiency: Double,
    val mounting: String,
    val shadingPct: Double,
    val soilingPct: Double,
    val cableMismatchPct: Double,
    val albedo: Double,
    val annualKwh: Double,
    val loadProfile: String,
    val loadSeason: String,
    val batteryKwh: Double,
    val selfUseOverridePct: Double?,
    val systemCost: Double,
    val pricePerKwh: Double,
    val priceChangePct: Double,
    val feedInPerKwh: Double,
    val degradationPct: Double,
    val lifetimeYears: Int,
    val discountRatePct: Double,
    val maintenancePerYear: Double,
    val co2GramsPerKwh: Double,
    val currencyCode: String,
    val resultAnnualKwh: Double,
    val resultPaybackYears: Double?,
) {
    companion object { const val DRAFT_ID = -1L }
}

@Dao
interface SetupDao {
    @Query("SELECT * FROM setups WHERE id != -1 ORDER BY savedAt DESC")
    fun observeSaved(): Flow<List<SetupEntity>>

    @Query("SELECT * FROM setups WHERE id = :id")
    suspend fun get(id: Long): SetupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SetupEntity): Long

    @Query("DELETE FROM setups WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [SetupEntity::class], version = 1, exportSchema = false)
abstract class SetupDatabase : RoomDatabase() {
    abstract fun setups(): SetupDao

    companion object {
        @Volatile private var instance: SetupDatabase? = null

        fun get(context: Context): SetupDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, SetupDatabase::class.java, "setups.db")
                .build().also { instance = it }
        }
    }
}
