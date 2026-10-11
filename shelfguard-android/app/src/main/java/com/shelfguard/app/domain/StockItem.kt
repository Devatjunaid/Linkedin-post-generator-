package com.shelfguard.app.domain

import com.shelfguard.app.data.db.BatchEntity
import com.shelfguard.app.data.db.ProductEntity
import com.shelfguard.app.data.db.ProductWithBatches
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class StockStatus { EXPIRED, OUT_OF_STOCK, EXPIRING_SOON, LOW_STOCK, OK }

/** A product plus everything the UI and alerts need to know about its stock today. */
data class StockItem(
    val product: ProductEntity,
    /** Batches with stock, soonest expiry first, non-expiring last. */
    val batches: List<BatchEntity>,
    val totalQty: Int,
    /** Quantity that has not expired and can be sold. */
    val sellableQty: Int,
    val expiredQty: Int,
    /** Earliest expiry among sellable batches. */
    val nextExpiry: LocalDate?,
    val daysToExpiry: Long?,
    val expiringSoonQty: Int,
) {
    val id: Long get() = product.id
    val hasExpired: Boolean get() = expiredQty > 0
    val isOutOfStock: Boolean get() = sellableQty == 0
    val isExpiringSoon: Boolean get() = expiringSoonQty > 0
    val isLowStock: Boolean get() = product.minStock > 0 && sellableQty in 1..product.minStock
    val needsAttention: Boolean get() = status != StockStatus.OK

    val status: StockStatus
        get() = when {
            hasExpired -> StockStatus.EXPIRED
            isOutOfStock -> StockStatus.OUT_OF_STOCK
            isExpiringSoon -> StockStatus.EXPIRING_SOON
            isLowStock -> StockStatus.LOW_STOCK
            else -> StockStatus.OK
        }

    val stockValueAtCost: Double get() = sellableQty * product.costPrice
    val stockValueAtPrice: Double get() = sellableQty * product.sellPrice

    companion object {
        fun from(source: ProductWithBatches, today: LocalDate): StockItem {
            val todayDay = today.toEpochDay()
            val product = source.product
            val batches = source.batches
                .filter { it.quantity > 0 }
                .sortedWith(compareBy(nullsLast()) { it.expiryEpochDay })
            val expired = batches.filter { it.expiryEpochDay != null && it.expiryEpochDay < todayDay }
            val sellable = batches - expired.toSet()
            val nextExpiryDay = sellable.firstOrNull { it.expiryEpochDay != null }?.expiryEpochDay
            val nextExpiry = nextExpiryDay?.let(LocalDate::ofEpochDay)
            val soonLimit = todayDay + product.alertDaysBefore
            val expiringSoonQty = sellable
                .filter { it.expiryEpochDay != null && it.expiryEpochDay <= soonLimit }
                .sumOf { it.quantity }
            return StockItem(
                product = product,
                batches = batches,
                totalQty = batches.sumOf { it.quantity },
                sellableQty = sellable.sumOf { it.quantity },
                expiredQty = expired.sumOf { it.quantity },
                nextExpiry = nextExpiry,
                daysToExpiry = nextExpiry?.let { ChronoUnit.DAYS.between(today, it) },
                expiringSoonQty = expiringSoonQty,
            )
        }
    }
}

/** Grouped lists of products that need attention. */
data class AlertSummary(
    val expired: List<StockItem>,
    val expiringSoon: List<StockItem>,
    val outOfStock: List<StockItem>,
    val lowStock: List<StockItem>,
) {
    val total: Int get() = expired.size + expiringSoon.size + outOfStock.size + lowStock.size

    companion object {
        fun from(items: List<StockItem>) = AlertSummary(
            expired = items.filter { it.hasExpired },
            expiringSoon = items.filter { it.isExpiringSoon }.sortedBy { it.daysToExpiry },
            outOfStock = items.filter { it.isOutOfStock && !it.hasExpired },
            lowStock = items.filter { it.isLowStock }.sortedBy { it.sellableQty },
        )
    }
}
