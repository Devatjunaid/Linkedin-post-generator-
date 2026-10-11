package com.shelfguard.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "products",
    indices = [Index("barcode"), Index("category")],
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val barcode: String?,
    val unit: String,
    /** Low-stock alert fires when total quantity is at or below this. 0 disables it. */
    val minStock: Int,
    val costPrice: Double,
    val sellPrice: Double,
    val supplier: String,
    /** Warn this many days before a batch expires. */
    val alertDaysBefore: Int,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * A quantity of a product received together, sharing one expiry date.
 * Total stock of a product is the sum of its batches.
 */
@Entity(
    tableName = "batches",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("productId"), Index("expiryEpochDay")],
)
data class BatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val quantity: Int,
    /** LocalDate.toEpochDay(), or null when the batch never expires. */
    val expiryEpochDay: Long?,
    val receivedAt: Long,
)

enum class MovementType { SALE, RESTOCK, WASTE, ADJUST }

/** History of every stock change; powers the Reports screen. Survives product deletion. */
@Entity(
    tableName = "movements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("productId"), Index("timestamp")],
)
data class MovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long?,
    val productName: String,
    val type: String,
    /** Always positive; the type says whether stock went up or down (ADJUST stores the signed delta). */
    val quantity: Int,
    val unitCost: Double,
    val unitPrice: Double,
    val timestamp: Long,
    val note: String = "",
)

val MovementEntity.movementType: MovementType get() = MovementType.valueOf(type)

data class ProductWithBatches(
    @Embedded val product: ProductEntity,
    @Relation(parentColumn = "id", entityColumn = "productId")
    val batches: List<BatchEntity>,
)
