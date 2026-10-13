package com.aquadoselab.poolchemistry.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aquadoselab.poolchemistry.ads.NativeAdCard
import com.aquadoselab.poolchemistry.ads.NativeStyle
import com.aquadoselab.poolchemistry.ui.components.AppCard
import com.aquadoselab.poolchemistry.ui.components.Bullet
import com.aquadoselab.poolchemistry.ui.components.Chip
import com.aquadoselab.poolchemistry.ui.components.ScreenColumn
import com.aquadoselab.poolchemistry.ui.theme.AppColors

private data class GuideTopic(
    val title: String,
    val range: String?,
    val intro: String,
    val sections: List<Pair<String, List<String>>>
)

private val topics = listOf(
    GuideTopic(
        "Free chlorine (FC)", "Depends on CYA — e.g. 5 ppm at CYA 40",
        "The active sanitizer that kills germs and algae. How much you need depends on your stabilizer (CYA): roughly 7.5% of CYA as a minimum, plus a buffer.",
        listOf(
            "If low" to listOf("Algae and bacteria can grow within a day.", "Water turns green or cloudy."),
            "If high" to listOf("Up to the shock (SLAM) level is safe to swim.", "Above it, wait for the sun to lower it."),
            "Raise with" to listOf("Liquid chlorine or plain bleach (no CYA or calcium added).", "Cal-hypo (adds calcium), dichlor or trichlor (add CYA)."),
            "Lower with" to listOf("Sunlight — leave the cover off.", "Time. Don't use chlorine reducers unless it is far too high.")
        )
    ),
    GuideTopic(
        "Combined chlorine (CC)", "0 – 0.5 ppm",
        "Chlorine that has reacted with sweat, urine and other waste (chloramines). It causes the 'chlorine smell' and red eyes.",
        listOf(
            "If above 0.5" to listOf("Shock the pool: hold FC at the SLAM level until CC is 0.5 or less, the water is clear, and FC drops 1 ppm or less overnight.")
        )
    ),
    GuideTopic(
        "pH", "7.4 – 7.6 ideal (7.2 – 7.8 OK)",
        "How acidic or basic the water is. It affects comfort, how well chlorine works, and whether water scales or corrodes.",
        listOf(
            "If low" to listOf("Stinging eyes, corroded metal, etched plaster.", "Raise with soda ash, or aerate (raises pH without raising TA)."),
            "If high" to listOf("Cloudy water, scale, weaker chlorine.", "Lower with muriatic acid or dry acid."),
            "Why pH keeps rising" to listOf("High TA, new plaster, salt cells and aeration all push pH up. Lowering TA slows the rise.")
        )
    ),
    GuideTopic(
        "Total alkalinity (TA)", "60 – 120 ppm (80 typical)",
        "A buffer that resists pH changes. Too low and pH swings; too high and pH keeps drifting up.",
        listOf(
            "Raise with" to listOf("Baking soda (sodium bicarbonate). Raises TA a lot, pH only a little."),
            "Lower with" to listOf("Acid + aeration: lower pH to 7.0–7.2 with acid, aerate back up to 7.5, repeat.")
        )
    ),
    GuideTopic(
        "Calcium hardness (CH)", "Plaster 250 – 650 · Vinyl/Fiberglass 150 – 450",
        "Dissolved calcium. Plaster and tile grout need enough calcium or the water will dissolve it. Vinyl and fiberglass are less sensitive.",
        listOf(
            "Raise with" to listOf("Calcium chloride. Pre-dissolve in a bucket (it gets hot)."),
            "Lower with" to listOf("Only by replacing water with softer fill water.")
        )
    ),
    GuideTopic(
        "Stabilizer / CYA", "30 – 50 ppm chlorine pools · 60 – 80 salt pools",
        "Cyanuric acid shields chlorine from sunlight. Without it the sun destroys most chlorine within hours. Too much makes chlorine weak.",
        listOf(
            "Raise with" to listOf("Stabilizer (cyanuric acid) in a sock in the skimmer. Takes up to a week to dissolve.", "Dichlor and trichlor also add CYA every time you use them."),
            "Lower with" to listOf("Only a partial drain and refill.")
        )
    ),
    GuideTopic(
        "Salt", "Usually 2700 – 3400 ppm (check your cell)",
        "Salt water chlorine generators (SWG) turn salt into chlorine. Each brand has its own ideal level.",
        listOf(
            "Raise with" to listOf("Pool salt (99%+ sodium chloride). Turn the cell off for 24 hours while it dissolves."),
            "Lower with" to listOf("Partial drain and refill.")
        )
    ),
    GuideTopic(
        "Water balance (CSI)", "−0.3 to +0.3",
        "The Calcite Saturation Index combines pH, TA, calcium, CYA, salt and temperature to show if water will dissolve plaster (negative) or deposit scale (positive).",
        listOf(
            "Too negative" to listOf("Raise calcium, pH or TA."),
            "Too positive" to listOf("Lower pH or TA. Warm water (spas, heated pools) scales more easily.")
        )
    ),
    GuideTopic(
        "Order to add chemicals", null,
        "Add one chemical at a time with the pump running, in front of a return jet.",
        listOf(
            "Recommended order" to listOf(
                "1. Partial drain if needed (then retest).",
                "2. Baking soda (TA).",
                "3. Acid or soda ash (pH) — wait 30 minutes.",
                "4. Calcium chloride — wait 1 hour.",
                "5. Stabilizer (in a sock).",
                "6. Salt (salt pools).",
                "7. Chlorine last, ideally at dusk."
            )
        )
    ),
    GuideTopic(
        "Testing tips", null,
        "Accurate tests give accurate doses.",
        listOf(
            "How often" to listOf("FC and pH: every 1–2 days in season.", "TA, CH, CYA, salt: every 1–2 weeks and after big rain or refills."),
            "How" to listOf(
                "Take water from elbow depth, away from returns.",
                "Use a FAS-DPD drop kit for chlorine; strips are approximate.",
                "pH tests read wrong when FC is above ~10 ppm.",
                "Test at the same time of day for comparable results."
            )
        )
    ),
    GuideTopic(
        "Green or cloudy water", null,
        "Almost always a chlorine problem.",
        listOf(
            "What to do" to listOf(
                "Test FC, CC, pH and CYA.",
                "Bring pH to 7.2–7.6, then raise FC to the SLAM level for your CYA.",
                "Brush daily, run the filter 24/7 and clean it as pressure rises.",
                "Keep FC at SLAM level until the water is clear and CC ≤ 0.5.",
                "If still cloudy with good chlorine: check filter, calcium and pH (scale)."
            )
        )
    ),
    GuideTopic(
        "Chemical safety", null,
        "Pool chemicals are safe when used correctly — and dangerous when not.",
        listOf(
            "Always" to listOf(
                "Read the product label and wear gloves and eye protection.",
                "Add chemicals to water — never water to chemicals.",
                "Store acid and chlorine far apart in a cool, dry, ventilated place.",
                "Keep chemicals locked away from children and pets."
            ),
            "Never" to listOf(
                "Mix chemicals together, or in the same bucket.",
                "Put cal-hypo or dichlor in a trichlor feeder (fire/explosion risk).",
                "Add chemicals while people are in the pool."
            )
        )
    )
)

@Composable
fun GuideScreen() {
    ScreenColumn {
        Text("Pool guide", style = MaterialTheme.typography.headlineMedium)
        Text(
            "What each reading means, ideal ranges, and how to fix problems.",
            style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted
        )
        topics.forEachIndexed { i, t ->
            TopicCard(t)
            if (i == 3 || i == 9) NativeAdCard(style = NativeStyle.SMALL)
        }
        Text(
            "Ranges follow widely used residential pool guidelines. Your equipment or surface maker may recommend different levels — follow them where they differ.",
            style = MaterialTheme.typography.bodySmall, color = AppColors.Muted
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun TopicCard(t: GuideTopic) {
    var open by rememberSaveable(t.title) { mutableStateOf(false) }
    AppCard(onClick = { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t.title, style = MaterialTheme.typography.titleMedium)
                if (t.range != null) {
                    Spacer(Modifier.height(4.dp))
                    Chip(t.range, AppColors.OkBg, AppColors.OkText)
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (open) "Collapse" else "Expand",
                tint = AppColors.Muted
            )
        }
        AnimatedVisibility(visible = open) {
            Column {
                Text(t.intro, style = MaterialTheme.typography.bodyMedium, color = AppColors.InkSoft)
                t.sections.forEach { (heading, lines) ->
                    Spacer(Modifier.height(10.dp))
                    Text(heading, style = MaterialTheme.typography.titleSmall, color = AppColors.TealDark)
                    Spacer(Modifier.height(4.dp))
                    lines.forEach { Bullet(it) }
                }
            }
        }
    }
}
