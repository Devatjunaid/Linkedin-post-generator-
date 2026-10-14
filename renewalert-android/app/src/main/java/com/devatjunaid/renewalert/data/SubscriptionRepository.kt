package com.devatjunaid.renewalert.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class SubscriptionRepository(
    private val db: AppDatabase,
    private val onDataChanged: () -> Unit,
) {
    private val dao = db.dao()

    val subscriptions: Flow<List<Subscription>> = dao.observeAll()
    val prices: Flow<List<PricePoint>> = dao.observePrices()

    suspend fun all(): List<Subscription> = dao.getAll()
    suspend fun allPrices(): List<PricePoint> = dao.getAllPrices()
    suspend fun get(id: Long): Subscription? = dao.get(id)

    /** Inserts or updates; records a price-history point whenever the price changes. */
    suspend fun save(sub: Subscription): Long {
        val today = LocalDate.now().toEpochDay()
        val id: Long
        if (sub.id == 0L) {
            id = dao.insert(sub)
            val since = minOf(sub.firstPaymentEpochDay, today)
            dao.insertPrice(PricePoint(subscriptionId = id, priceMinor = sub.priceMinor, effectiveEpochDay = since))
        } else {
            id = sub.id
            val old = dao.get(id)
            dao.update(sub)
            if (old != null && old.priceMinor != sub.priceMinor) {
                val history = dao.pricesFor(id)
                if (history.isEmpty()) {
                    dao.insertPrice(PricePoint(subscriptionId = id, priceMinor = old.priceMinor, effectiveEpochDay = old.firstPaymentEpochDay))
                }
                dao.deletePriceOn(id, today)
                dao.insertPrice(PricePoint(subscriptionId = id, priceMinor = sub.priceMinor, effectiveEpochDay = today))
            }
        }
        onDataChanged()
        return id
    }

    suspend fun setStatus(id: Long, status: SubStatus) {
        val s = dao.get(id) ?: return
        val today = LocalDate.now().toEpochDay()
        val updated = when (status) {
            SubStatus.ACTIVE -> {
                if (s.status == SubStatus.TRIAL) {
                    s.copy(status = SubStatus.ACTIVE, statusChangedEpochDay = null)
                } else if (s.status == SubStatus.PAUSED || s.status == SubStatus.CANCELLED) {
                    // Resume: next charge from today onwards on the same billing day.
                    s.copy(status = SubStatus.ACTIVE, statusChangedEpochDay = null)
                } else s
            }
            SubStatus.PAUSED, SubStatus.CANCELLED -> s.copy(status = status, statusChangedEpochDay = today)
            SubStatus.TRIAL -> s.copy(status = status)
        }
        dao.update(updated)
        onDataChanged()
    }

    suspend fun setUsage(id: Long, usage: Usage) {
        val s = dao.get(id) ?: return
        dao.update(s.copy(usage = usage))
        onDataChanged()
    }

    suspend fun delete(id: Long) {
        val s = dao.get(id) ?: return
        dao.delete(s)
        onDataChanged()
    }

    suspend fun deleteAll() {
        dao.deleteAll()
        onDataChanged()
    }

    /** Trials whose end date has passed become regular paid subscriptions. */
    suspend fun convertEndedTrials(today: LocalDate = LocalDate.now()) {
        var changed = false
        dao.getAll().filter { it.status == SubStatus.TRIAL }.forEach { t ->
            val end = t.trialEndEpochDay ?: t.firstPaymentEpochDay
            if (end < today.toEpochDay()) {
                dao.update(t.copy(status = SubStatus.ACTIVE))
                changed = true
            }
        }
        if (changed) onDataChanged()
    }

    suspend fun import(subs: List<Subscription>, prices: List<PricePoint>, replace: Boolean) {
        if (replace) dao.replaceAll(subs, prices) else dao.insertAll(subs, prices)
        onDataChanged()
    }
}
