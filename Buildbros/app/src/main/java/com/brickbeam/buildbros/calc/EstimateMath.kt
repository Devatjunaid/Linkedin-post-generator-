package com.brickbeam.buildbros.calc

data class EstimateLine(
    val category: Category,
    val quantity: Double,
    val unitPrice: Double,
    val owned: Boolean,
) {
    val total: Double get() = quantity * unitPrice
}

data class EstimateTotals(
    val materials: Double,
    val taxRatePct: Double,
    val tax: Double,
    val taxIncluded: Boolean,
    val contingency: Double,
    val delivery: Double,
    val total: Double,
    val byCategory: Map<Category, Double>,
    val ownedValue: Double,
)

object EstimateMath {
    /**
     * US: sales tax (state + local) is added on top of materials.
     * Europe: VAT is already inside shelf prices, so it is only *shown* (extracted), not added.
     * Contingency is a % of materials; delivery is a flat fee. Owned items are excluded.
     */
    fun totals(
        lines: List<EstimateLine>,
        taxRatePct: Double,
        taxIncluded: Boolean,
        contingencyPct: Double,
        delivery: Double,
    ): EstimateTotals {
        val buying = lines.filter { !it.owned }
        val materials = buying.sumOf { it.total }
        val tax = if (taxIncluded) materials * taxRatePct / (100.0 + taxRatePct) else materials * taxRatePct / 100.0
        val contingency = materials * contingencyPct / 100.0
        val total = materials + (if (taxIncluded) 0.0 else tax) + contingency + delivery
        val byCategory = buying.groupBy { it.category }.mapValues { (_, l) -> l.sumOf { it.total } }
        return EstimateTotals(
            materials = materials,
            taxRatePct = taxRatePct,
            tax = tax,
            taxIncluded = taxIncluded,
            contingency = contingency,
            delivery = delivery,
            total = total,
            byCategory = byCategory,
            ownedValue = lines.filter { it.owned }.sumOf { it.total },
        )
    }
}
