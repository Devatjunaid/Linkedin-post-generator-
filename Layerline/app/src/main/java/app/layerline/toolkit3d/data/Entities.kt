package app.layerline.toolkit3d.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "printers")
data class Printer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,
    val brand: String,
    val model: String,
    val kind: String = MachineKind.BedSlinger.name,
    val watts: Double,
    /** Purchase price in the local currency at the time it was added. */
    val price: Double,
    val lifeHours: Double,
    val hoursRun: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "maintenance",
    foreignKeys = [
        ForeignKey(
            entity = Printer::class,
            parentColumns = ["id"],
            childColumns = ["printerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("printerId")],
)
data class MaintenanceTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val printerId: Long,
    val name: String,
    val intervalHours: Double,
    /** Printer hour meter reading when this job was last done. */
    val hoursAtService: Double = 0.0,
)

@Entity(tableName = "spools")
data class Spool(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String,
    val material: String,
    val color: Long,
    val diameter: Double = 1.75,
    val totalGrams: Double = 1000.0,
    val remainingGrams: Double = 1000.0,
    /** What was paid for the full spool, local currency. */
    val price: Double,
    val tareGrams: Double = 230.0,
    val lowThreshold: Double = 200.0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isLow: Boolean get() = remainingGrams < lowThreshold
    val pricePerKg: Double get() = if (totalGrams > 0) price / totalGrams * 1000.0 else 0.0
    val value: Double get() = remainingGrams / 1000.0 * pricePerKg
}

enum class JobStatus { PRINTING, DONE, FAILED }

@Entity(
    tableName = "jobs",
    foreignKeys = [
        ForeignKey(entity = Printer::class, parentColumns = ["id"], childColumns = ["printerId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = Spool::class, parentColumns = ["id"], childColumns = ["spoolId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("printerId"), Index("spoolId"), Index("startedAt")],
)
data class PrintJob(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val printerId: Long? = null,
    val spoolId: Long? = null,
    val printerLabel: String = "",
    val material: String = "PLA",
    val color: Long = 0xFFC8F03C,
    val grams: Double,
    val minutes: Double,
    val status: String = JobStatus.DONE.name,
    val failReason: String = "",
    /** How far a failed print got, 0-100. Used to charge the filament that was actually used. */
    val failedAtPercent: Double = 0.0,
    val cost: Double = 0.0,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val notes: String = "",
    /** True once filament and machine hours have been taken off the spool / added to the printer. */
    val accounted: Boolean = false,
) {
    val jobStatus: JobStatus get() = runCatching { JobStatus.valueOf(status) }.getOrDefault(JobStatus.DONE)

    /** Share of the planned print that actually ran. */
    val usedFraction: Double
        get() = if (jobStatus == JobStatus.FAILED) (failedAtPercent / 100.0).coerceIn(0.0, 1.0) else 1.0

    fun progressAt(now: Long): Double {
        if (jobStatus != JobStatus.PRINTING) return 1.0
        val total = minutes * 60_000.0
        if (total <= 0) return 1.0
        return ((now - startedAt) / total).coerceIn(0.0, 1.0)
    }
}

@Entity(tableName = "quotes")
data class Quote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val regionCode: String,
    val symbol: String,
    val decimals: Int,
    val machine: String,
    val material: String,
    val grams: Double,
    val hours: Double,
    val quantity: Int,
    val cost: Double,
    val price: Double,
    val summary: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "calibrations")
data class CalibrationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val calcId: String,
    val name: String,
    val target: String,
    val previous: Double,
    val result: Double,
    val unit: String,
    val decimals: Int,
    val gcode: String,
    val createdAt: Long = System.currentTimeMillis(),
)
