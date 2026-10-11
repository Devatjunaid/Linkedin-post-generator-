package com.shelfguard.app.data.repo

import androidx.room.withTransaction
import com.shelfguard.app.data.db.AppDatabase
import com.shelfguard.app.data.db.BatchEntity
import com.shelfguard.app.data.db.MovementEntity
import com.shelfguard.app.data.db.MovementType
import com.shelfguard.app.data.db.ProductEntity
import com.shelfguard.app.data.db.ProductWithBatches
import com.shelfguard.app.domain.StockItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class StockException(message: String) : Exception(message)

class InventoryRepository(private val db: AppDatabase) {
    private val products = db.productDao()
    private val batches = db.batchDao()
    private val movements = db.movementDao()

    private fun now() = System.currentTimeMillis()
    private fun today() = LocalDate.now()

    // ---- Reads -------------------------------------------------------------------------------

    fun observeStock(): Flow<List<StockItem>> =
        products.observeAll().map { list -> list.map { StockItem.from(it, today()) } }

    fun observeItem(id: Long): Flow<StockItem?> =
        products.observe(id).map { it?.let { p -> StockItem.from(p, today()) } }

    fun observeCategories(): Flow<List<String>> = products.observeCategories()

    fun observeMovementsSince(since: Long): Flow<List<MovementEntity>> = movements.observeSince(since)

    fun observeProductHistory(productId: Long, limit: Int = 20): Flow<List<MovementEntity>> =
        movements.observeForProduct(productId, limit)

    suspend fun stockSnapshot(): List<StockItem> = products.getAll().map { StockItem.from(it, today()) }

    suspend fun getProduct(id: Long): ProductEntity? = products.get(id)?.product

    suspend fun findByBarcode(barcode: String): ProductEntity? =
        barcode.trim().takeIf { it.isNotEmpty() }?.let { products.findByBarcode(it) }

    // ---- Product CRUD -------------------------------------------------------------------------

    /** Creates a product and, when [initialQty] > 0, its first batch. Returns the new id. */
    suspend fun addProduct(product: ProductEntity, initialQty: Int, expiry: LocalDate?): Long =
        db.withTransaction {
            val time = now()
            val id = products.insert(product.copy(id = 0, createdAt = time, updatedAt = time))
            if (initialQty > 0) {
                batches.insert(
                    BatchEntity(productId = id, quantity = initialQty, expiryEpochDay = expiry?.toEpochDay(), receivedAt = time),
                )
                log(id, product.name, MovementType.RESTOCK, initialQty, product.costPrice, product.sellPrice, "Opening stock")
            }
            id
        }

    suspend fun updateProduct(product: ProductEntity) {
        products.update(product.copy(updatedAt = now()))
    }

    /** Deletes a product with its batches. Returns a snapshot that [restoreProduct] can put back (undo). */
    suspend fun deleteProduct(id: Long): ProductWithBatches? = db.withTransaction {
        val snapshot = products.get(id)
        products.delete(id)
        snapshot
    }

    suspend fun restoreProduct(snapshot: ProductWithBatches) = db.withTransaction {
        val newId = products.insert(snapshot.product.copy(id = 0))
        batches.insertAll(snapshot.batches.map { it.copy(id = 0, productId = newId) })
    }

    // ---- Stock operations ---------------------------------------------------------------------

    /**
     * Sells [qty] units, taking stock from the batch that expires first (FIFO by expiry).
     * Expired stock is never sold. Returns the product's remaining sellable quantity.
     */
    suspend fun recordSale(productId: Long, qty: Int, unitPrice: Double): Int = db.withTransaction {
        require(qty > 0)
        val pwb = products.get(productId) ?: throw StockException("Product not found")
        val item = StockItem.from(pwb, today())
        if (qty > item.sellableQty) {
            throw StockException("Only ${item.sellableQty} ${pwb.product.unit} available to sell")
        }
        val todayDay = today().toEpochDay()
        var remaining = qty
        for (batch in item.batches) {
            if (remaining == 0) break
            if (batch.expiryEpochDay != null && batch.expiryEpochDay < todayDay) continue
            val take = minOf(batch.quantity, remaining)
            if (take == batch.quantity) batches.delete(batch) else batches.update(batch.copy(quantity = batch.quantity - take))
            remaining -= take
        }
        log(productId, pwb.product.name, MovementType.SALE, qty, pwb.product.costPrice, unitPrice)
        item.sellableQty - qty
    }

    /** Adds stock as a new batch, or tops up an existing batch with the same expiry date. */
    suspend fun restock(productId: Long, qty: Int, expiry: LocalDate?, unitCost: Double? = null) = db.withTransaction {
        require(qty > 0)
        val pwb = products.get(productId) ?: throw StockException("Product not found")
        val expiryDay = expiry?.toEpochDay()
        val sameExpiry = pwb.batches.firstOrNull { it.expiryEpochDay == expiryDay }
        if (sameExpiry != null) {
            batches.update(sameExpiry.copy(quantity = sameExpiry.quantity + qty, receivedAt = now()))
        } else {
            batches.insert(BatchEntity(productId = productId, quantity = qty, expiryEpochDay = expiryDay, receivedAt = now()))
        }
        val cost = unitCost ?: pwb.product.costPrice
        if (unitCost != null && unitCost != pwb.product.costPrice) {
            products.update(pwb.product.copy(costPrice = unitCost, updatedAt = now()))
        }
        log(productId, pwb.product.name, MovementType.RESTOCK, qty, cost, pwb.product.sellPrice)
    }

    /** Corrects a batch after a stock count. Quantity 0 removes the batch. */
    suspend fun editBatch(batchId: Long, newQty: Int, newExpiry: LocalDate?) = db.withTransaction {
        require(newQty >= 0)
        val batch = batches.get(batchId) ?: return@withTransaction
        val product = products.get(batch.productId)?.product ?: return@withTransaction
        val delta = newQty - batch.quantity
        if (newQty == 0) batches.delete(batch)
        else batches.update(batch.copy(quantity = newQty, expiryEpochDay = newExpiry?.toEpochDay()))
        if (delta != 0) {
            log(product.id, product.name, MovementType.ADJUST, delta, product.costPrice, product.sellPrice, "Stock count correction")
        }
    }

    /** Removes a whole batch. Expired batches are logged as waste, others as an adjustment. */
    suspend fun deleteBatch(batchId: Long) = db.withTransaction {
        val batch = batches.get(batchId) ?: return@withTransaction
        val product = products.get(batch.productId)?.product ?: return@withTransaction
        batches.delete(batch)
        val expired = batch.expiryEpochDay != null && batch.expiryEpochDay < today().toEpochDay()
        if (expired) {
            log(product.id, product.name, MovementType.WASTE, batch.quantity, product.costPrice, product.sellPrice, "Expired batch removed")
        } else {
            log(product.id, product.name, MovementType.ADJUST, -batch.quantity, product.costPrice, product.sellPrice, "Batch removed")
        }
    }

    /** Removes expired batches of one product, or of every product when [productId] is null. Returns units removed. */
    suspend fun removeExpired(productId: Long? = null): Int = db.withTransaction {
        val todayDay = today().toEpochDay()
        val expired = batches.expired(todayDay).filter { productId == null || it.productId == productId }
        expired.groupBy { it.productId }.forEach { (pid, list) ->
            val product = products.get(pid)?.product ?: return@forEach
            list.forEach { batches.delete(it) }
            log(pid, product.name, MovementType.WASTE, list.sumOf { it.quantity }, product.costPrice, product.sellPrice, "Expired stock removed")
        }
        expired.sumOf { it.quantity }
    }

    suspend fun deleteAllData() = db.withTransaction {
        movements.deleteAll()
        products.deleteAll()
    }

    private suspend fun log(
        productId: Long,
        name: String,
        type: MovementType,
        qty: Int,
        unitCost: Double,
        unitPrice: Double,
        note: String = "",
    ) {
        movements.insert(
            MovementEntity(
                productId = productId,
                productName = name,
                type = type.name,
                quantity = qty,
                unitCost = unitCost,
                unitPrice = unitPrice,
                timestamp = now(),
                note = note,
            ),
        )
    }
}
