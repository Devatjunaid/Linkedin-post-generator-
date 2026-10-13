package com.aquadoselab.poolchemistry.chem

import com.aquadoselab.poolchemistry.data.AppData
import com.aquadoselab.poolchemistry.data.Pool
import com.aquadoselab.poolchemistry.data.Sanitizer
import com.aquadoselab.poolchemistry.data.WaterTest
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Test values with blanks filled from earlier tests where possible. */
data class Resolved(
    val fc: Double?,
    val cc: Double?,
    val ph: Double?,
    val ta: Double?,
    val ch: Double?,
    val cya: Double?,
    val salt: Double?,
    val tempF: Double?,
    /** Names of readings that came from an earlier test. */
    val carried: List<String>
)

enum class StepKind { DRAIN, BAKING_SODA, ACID, SODA_ASH, TA_LOWER, CALCIUM, STABILIZER, SALT, CHLORINE, SLAM, RETEST }
enum class ProductSlot { CHLORINE, ACID, CALCIUM }

data class DoseStep(
    val kind: StepKind,
    val title: String,
    val quantity: Quantity? = null,
    val productName: String? = null,
    val instructions: List<String> = emptyList(),
    val effects: List<String> = emptyList(),
    val waitAfter: String? = null,
    val urgent: Boolean = false,
    val slot: ProductSlot? = null,
    /** Extra amount line, e.g. "≈ 3 tablets" or "≈ 1 bag of 40 lb". */
    val extra: String? = null
)

data class SwimStatus(val safe: Boolean, val message: String)

data class DosingPlan(
    val steps: List<DoseStep>,
    val warnings: List<String>,
    val notes: List<String>,
    val swim: SwimStatus,
    val fcLevels: FcLevels,
    val csiNow: Double?,
    val csiAfter: Double?,
    val issues: Int
)

object DosingEngine {

    fun resolve(test: WaterTest, data: AppData): Resolved {
        val earlier = data.testsFor(test.poolId).filter { it.timestamp < test.timestamp }
        val carried = mutableListOf<String>()
        fun pick(name: String, own: Double?, sel: (WaterTest) -> Double?): Double? {
            if (own != null) return own
            val v = earlier.firstNotNullOfOrNull(sel)
            if (v != null) carried += name
            return v
        }
        return Resolved(
            fc = test.fc,
            cc = test.cc,
            ph = test.ph,
            ta = pick("alkalinity", test.ta) { it.ta },
            ch = pick("calcium hardness", test.ch) { it.ch },
            cya = pick("stabilizer (CYA)", test.cya) { it.cya },
            salt = pick("salt", test.salt) { it.salt },
            tempF = pick("water temperature", test.tempF) { it.tempF },
            carried = carried
        )
    }

    fun plan(pool: Pool, r: Resolved): DosingPlan {
        val t = pool.targets
        val rg = Chemistry.ranges(pool)
        val gallons = Volume.gallons(pool)
        val liters = gallons * LITERS_PER_GALLON
        val isSalt = pool.sanitizer == Sanitizer.SALT
        val steps = mutableListOf<DoseStep>()
        val warnings = mutableListOf<String>()
        val notes = mutableListOf<String>()
        var issues = 0

        if (liters <= 0) {
            warnings += "Pool volume is missing. Edit the pool to add its size."
        }
        if (r.carried.isNotEmpty()) {
            notes += "Using your previous result for ${r.carried.joinToString()}."
        }

        // Working values (updated as each dose is planned)
        var ph = r.ph
        var ta = r.ta ?: t.ta.also {
            notes += "Alkalinity not tested — assumed ${Units.ppm(it)} ppm. Test TA for more accurate pH doses."
        }
        var ch = r.ch
        var cya = r.cya
        val cyaForFc = r.cya ?: 0.0
        if (r.cya == null) {
            warnings += "Stabilizer (CYA) not tested. Chlorine targets depend on it — test CYA soon."
        }
        val tempF = r.tempF ?: 80.0
        val salt = r.salt ?: if (isSalt) t.salt else 1000.0
        val csiNow = if (r.ph != null && r.ta != null && r.ch != null)
            Chemistry.csi(r.ph, r.ta, r.ch, cyaForFc, tempF, salt) else null

        // ---- 0. Things only a partial drain can fix --------------------------------------
        var drainFraction = 0.0
        val drainReasons = mutableListOf<String>()
        fun needDrain(current: Double?, target: Double, high: Double, name: String) {
            if (current != null && current > high && current > 0) {
                val f = 1 - target / current
                if (f > drainFraction) drainFraction = f
                drainReasons += "$name ${Units.ppm(current)} → ${Units.ppm(target)} ppm"
            }
        }
        needDrain(r.cya, t.cya, rg.cyaHigh, "CYA")
        needDrain(r.ch, t.ch, rg.chHigh, "Calcium")
        if (isSalt) needDrain(r.salt, t.salt, rg.saltHigh, "Salt")
        if (drainFraction > 0) {
            issues++
            val pct = (drainFraction * 100).roundToInt().coerceIn(5, 95)
            steps += DoseStep(
                kind = StepKind.DRAIN,
                title = "Replace about $pct% of the water",
                instructions = listOf(
                    "Only water replacement lowers ${drainReasons.joinToString { it.substringBefore(' ') }}.",
                    "Drain and refill in stages if your pool type limits how low you can drain (vinyl liners: keep water above the lowest point of the liner).",
                    "Retest everything after refilling — fill water changes all readings. Do the steps below after the refill."
                ),
                effects = drainReasons,
                waitAfter = "Retest after refilling"
            )
        }

        // ---- 1. Raise TA (before pH, since baking soda lifts pH slightly) --------------
        if (r.ta != null && ta < rg.taLow - 1e-9) {
            issues++
            val delta = t.ta - ta
            val mol = delta / 50_000.0
            val grams = mol * liters * 84.007
            val phAfter = ph?.let { Chemistry.phAfterBakingSoda(it, ta, cyaForFc, mol) }
            steps += DoseStep(
                kind = StepKind.BAKING_SODA,
                title = "Raise alkalinity ${Units.ppm(ta)} → ${Units.ppm(t.ta)} ppm",
                quantity = Quantity.Mass(grams),
                productName = "Baking soda (sodium bicarbonate)",
                instructions = listOf(
                    "Pump running. Broadcast evenly over the deep end, then brush any that settles.",
                    "Large amounts: add half, wait 6 hours, then add the rest."
                ),
                effects = listOfNotNull(
                    "TA +${Units.ppm(delta)} ppm",
                    phAfter?.let { "pH rises slightly to about ${Units.ph(it)}" }
                ),
                waitAfter = "Circulate 30 min before the next step"
            )
            if (phAfter != null) ph = phAfter
            ta = t.ta
        }

        // ---- 2. pH -------------------------------------------------------------------
        if (ph != null) {
            if (ph > t.ph + 0.2 + 1e-9) {
                issues++
                val acid = Products.acid(pool.products.acid)
                val eqPerL = Chemistry.acidEqPerLiter(ph, t.ph, ta, cyaForFc)
                val totalEq = eqPerL * liters
                val q = if (acid.form == ProductForm.LIQUID) Quantity.Volume(totalEq / acid.eqPerUnit)
                else Quantity.Mass(totalEq / acid.eqPerUnit)
                val taDrop = eqPerL * 50_000.0
                val big = ph - t.ph > 0.6
                steps += DoseStep(
                    kind = StepKind.ACID,
                    title = "Lower pH ${Units.ph(ph)} → ${Units.ph(t.ph)}",
                    quantity = q,
                    productName = acid.name,
                    instructions = listOfNotNull(
                        acid.tip,
                        if (big) "Big change: add half now, retest pH after 1 hour, then add the rest if still needed." else null,
                        if (r.ph != null && r.ph >= 8.2) "Your test may read high above 8.2 (most kits top out there) — retest after this dose." else null
                    ),
                    effects = listOf("TA −${Units.ppm(taDrop)} ppm"),
                    waitAfter = "Wait 30 min before adding chlorine",
                    slot = ProductSlot.ACID
                )
                ta -= taDrop
                ph = t.ph
            } else if (ph < t.ph - 0.2 - 1e-9) {
                issues++
                val mol = Chemistry.sodaAshMolPerLiter(ph, t.ph, ta, cyaForFc)
                val grams = mol * liters * 105.99
                val taRise = 2 * mol * 50_000.0
                steps += DoseStep(
                    kind = StepKind.SODA_ASH,
                    title = "Raise pH ${Units.ph(ph)} → ${Units.ph(t.ph)}",
                    quantity = Quantity.Mass(grams),
                    productName = Products.phUp.first().name,
                    instructions = listOfNotNull(
                        Products.phUp.first().tip,
                        if (ta > rg.taHigh - 20) "Your TA is already on the high side. Aeration (fountains, waterfalls, jets pointed up) raises pH without raising TA." else null,
                        if (r.ph != null && r.ph <= 6.8) "Your kit may read low below 6.8 — retest after this dose." else null
                    ),
                    effects = listOf("TA +${Units.ppm(taRise)} ppm"),
                    waitAfter = "Wait 30 min before the next step"
                )
                ta += taRise
                ph = t.ph
            }
        }

        // ---- 3. High TA: a process, not a single dose ---------------------------------
        if (r.ta != null && r.ta > rg.taHigh + 1e-9) {
            issues++
            val acid = Products.acid(pool.products.acid)
            val eqFor10 = 10.0 / 50_000.0 * liters
            val q = if (acid.form == ProductForm.LIQUID) Quantity.Volume(eqFor10 / acid.eqPerUnit)
            else Quantity.Mass(eqFor10 / acid.eqPerUnit)
            steps += DoseStep(
                kind = StepKind.TA_LOWER,
                title = "Lower alkalinity ${Units.ppm(r.ta)} → ${Units.ppm(t.ta)} ppm (over several days)",
                quantity = q,
                productName = "${acid.name} — per 10 ppm of TA",
                instructions = listOf(
                    "Add acid to bring pH down to 7.0–7.2.",
                    "Aerate hard (point returns up, run waterfalls/spa spillover) until pH climbs back to 7.5.",
                    "Repeat until TA reaches ${Units.ppm(t.ta)}. Each repeat lowers TA; the shown amount removes about 10 ppm."
                ),
                effects = listOf("High TA makes pH keep rising"),
                slot = ProductSlot.ACID
            )
        }

        // ---- 4. Calcium --------------------------------------------------------------
        if (ch != null && ch < rg.chLow - 1e-9) {
            issues++
            val delta = pool.targets.ch - ch
            val purity = Products.calciumPurity(pool.products.calcium)
            val grams = delta * liters / 100_086.0 * 110.98 / purity
            steps += DoseStep(
                kind = StepKind.CALCIUM,
                title = "Raise calcium ${Units.ppm(ch)} → ${Units.ppm(t.ch)} ppm",
                quantity = Quantity.Mass(grams),
                productName = Products.calcium(pool.products.calcium).name,
                instructions = listOf(
                    Products.calcium(pool.products.calcium).tip,
                    "Add no more than about a third of the dose at a time; let it circulate between additions."
                ),
                effects = listOf("Protects plaster and grout from etching"),
                waitAfter = "Circulate 1 hour before the next step",
                slot = ProductSlot.CALCIUM
            )
            ch = t.ch
        }

        // ---- 5. Stabilizer (CYA) -----------------------------------------------------
        if (cya != null && cya < rg.cyaLow - 1e-9) {
            issues++
            val delta = t.cya - cya
            val grams = delta * liters / 1000.0
            steps += DoseStep(
                kind = StepKind.STABILIZER,
                title = "Raise stabilizer (CYA) ${Units.ppm(cya)} → ${Units.ppm(t.cya)} ppm",
                quantity = Quantity.Mass(grams),
                productName = "Stabilizer / conditioner (cyanuric acid)",
                instructions = listOf(
                    "Put it in a sock or mesh bag in the skimmer basket (or in front of a return) and squeeze it daily.",
                    "It dissolves slowly — 2 to 7 days. Don't backwash or clean the filter for a week.",
                    "Retest CYA after 7 days before adding more."
                ),
                effects = listOf("Protects chlorine from sunlight"),
                waitAfter = null
            )
            cya = t.cya
        }

        // ---- 6. Salt -------------------------------------------------------------------
        if (isSalt && r.salt != null && r.salt < rg.saltLow - 1e-9) {
            issues++
            val delta = t.salt - r.salt
            val grams = delta * liters / 1000.0
            val bags = grams / 1000.0 / 18.14 // 40 lb bags
            steps += DoseStep(
                kind = StepKind.SALT,
                title = "Raise salt ${Units.ppm(r.salt)} → ${Units.ppm(t.salt)} ppm",
                quantity = Quantity.Mass(grams),
                productName = "Pool salt (99%+ sodium chloride)",
                instructions = listOf(
                    "Turn the salt cell off. Broadcast salt in the shallow end and brush it toward the main drain.",
                    "Run the pump 24 hours, then retest salt before turning the cell back on.",
                    "Check your cell's manual for its ideal salt level and adjust the target in Pool settings."
                ),
                extra = "≈ ${Units.num(bags)} bags of 40 lb"
            )
        }

        // ---- 7. Chlorine (always last; evening is best) --------------------------------
        val fcLevels = Chemistry.fcLevels(cyaForFc, pool.sanitizer)
        val fc = r.fc
        val cc = r.cc ?: 0.0
        if (fc != null) {
            val product = Products.chlorine(pool.products.chlorine)
            fun chlorineQuantity(ppm: Double): Quantity {
                val pureGrams = ppm * liters / 1000.0
                return if (product.form == ProductForm.LIQUID) Quantity.Volume(pureGrams / (product.strength / 100.0))
                else Quantity.Mass(pureGrams / (product.strength / 100.0))
            }
            fun sideEffects(ppm: Double): List<String> = listOfNotNull(
                if (product.cyaPerPpm > 0) "CYA +${Units.ppm(ppm * product.cyaPerPpm)} ppm" else null,
                if (product.chPerPpm > 0) "Calcium +${Units.ppm(ppm * product.chPerPpm)} ppm" else null
            )
            fun tablets(q: Quantity): String? =
                if (product.id == "trichlor_90" && q is Quantity.Mass) "≈ ${Units.num(q.grams / 200.0)} × 3-inch tablets (200 g)" else null

            if (cc > 0.5) {
                issues++
                val delta = max(0.0, fcLevels.slam - fc)
                val q = chlorineQuantity(delta)
                steps += DoseStep(
                    kind = StepKind.SLAM,
                    title = "Shock (SLAM): raise chlorine to ${Units.ppm(fcLevels.slam)} ppm and hold",
                    quantity = if (delta > 0) q else null,
                    productName = product.name,
                    instructions = listOf(
                        "Combined chlorine is ${Units.ppm(cc)} ppm — there are chloramines or organics to burn off.",
                        product.tip,
                        "Keep FC at ${Units.ppm(fcLevels.slam)} ppm: test and top up 2–3 times a day, brush, run the pump 24/7.",
                        "Done when: CC is 0.5 or less, FC drops 1 ppm or less overnight, and the water is clear."
                    ) + if (product.cyaPerPpm > 0) listOf("Tip: use liquid chlorine for shocking — stabilized chlorine piles up CYA.") else emptyList(),
                    effects = sideEffects(delta),
                    urgent = true,
                    slot = ProductSlot.CHLORINE,
                    extra = tablets(q)
                )
            } else if (fc < fcLevels.target - 0.05) {
                issues++
                val delta = fcLevels.target - fc
                val q = chlorineQuantity(delta)
                steps += DoseStep(
                    kind = StepKind.CHLORINE,
                    title = "Raise chlorine ${Units.ppm(fc)} → ${Units.ppm(fcLevels.target)} ppm",
                    quantity = q,
                    productName = product.name,
                    instructions = listOf(
                        product.tip,
                        "Best added at dusk — sunlight burns off unstabilized chlorine.",
                        "Target is based on CYA ${Units.ppm(cyaForFc)}: keep FC between ${Units.ppm(fcLevels.min)} and ${Units.ppm(fcLevels.upper)}."
                    ),
                    effects = sideEffects(delta),
                    urgent = fc < fcLevels.min,
                    slot = ProductSlot.CHLORINE,
                    extra = tablets(q)
                )
            }
            if (fc > fcLevels.slam) {
                warnings += "Chlorine is above the shock level (${Units.ppm(fcLevels.slam)} ppm). Don't swim until it drops — leave the cover off in the sun."
            }
            if (fc < fcLevels.min) {
                warnings += "Chlorine is below the safe minimum (${Units.ppm(fcLevels.min)} ppm for CYA ${Units.ppm(cyaForFc)}). Algae can start growing — add chlorine today."
            }
        }

        if (r.cya != null && r.cya > 90 && !isSalt) {
            warnings += "CYA above 90 ppm makes chlorine weak and hard to manage. Plan a partial drain."
        }
        if (pool.products.chlorine == "dichlor_56" || pool.products.chlorine == "trichlor_90") {
            notes += "Stabilized chlorine (dichlor/trichlor) adds CYA with every dose. Track CYA monthly."
        }

        // ---- 8. Retest -------------------------------------------------------------
        if (steps.isNotEmpty()) {
            steps += DoseStep(
                kind = StepKind.RETEST,
                title = if (steps.any { it.kind == StepKind.SLAM }) "Retest FC and CC in 4–6 hours"
                else "Retest pH and chlorine in 4–6 hours",
                instructions = listOf("Keep the pump running so everything mixes. Log the new test to update your plan.")
            )
        }

        // ---- Projections -------------------------------------------------------------
        val csiAfter = if (ph != null && ch != null)
            Chemistry.csi(ph, ta, ch, cya ?: cyaForFc, tempF, salt) else null
        if (csiAfter != null && abs(csiAfter) > 0.3 && steps.none { it.kind == StepKind.DRAIN }) {
            notes += if (csiAfter > 0.3) "After these doses the water still leans scale-forming (CSI ${Units.signed(round2(csiAfter))}). Keep pH toward 7.3–7.4."
            else "After these doses the water still leans corrosive (CSI ${Units.signed(round2(csiAfter))}). Raise calcium or keep pH toward 7.6–7.8."
        }

        val swim = swimStatus(r, fcLevels)
        return DosingPlan(steps, warnings, notes, swim, fcLevels, csiNow, csiAfter, issues)
    }

    private fun round2(v: Double) = Math.round(v * 100) / 100.0

    fun swimStatus(r: Resolved, levels: FcLevels): SwimStatus {
        val fc = r.fc ?: return SwimStatus(false, "Test free chlorine before swimming.")
        val ph = r.ph
        return when {
            fc < 1.0 -> SwimStatus(false, "Not safe to swim: chlorine is too low to sanitize.")
            fc > levels.slam -> SwimStatus(false, "Not safe to swim: chlorine is above shock level.")
            ph != null && (ph < 7.0 || ph > 8.2) -> SwimStatus(false, "Not comfortable to swim: pH can sting eyes and skin.")
            (r.cc ?: 0.0) > 0.5 && fc < levels.slam - 0.05 -> SwimStatus(true, "OK to swim, but shock soon — combined chlorine is high.")
            fc < levels.min -> SwimStatus(true, "OK to swim for now, but add chlorine today.")
            else -> SwimStatus(true, "Safe to swim.")
        }
    }
}
