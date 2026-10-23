package com.driveready.testprep.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.driveready.testprep.BuildConfig
import com.driveready.testprep.Readiness
import com.driveready.testprep.data.local.TestAttempt
import com.driveready.testprep.data.local.UserSettings
import com.driveready.testprep.data.model.Country
import com.driveready.testprep.data.model.Region
import com.driveready.testprep.ui.components.AppCard
import com.driveready.testprep.ui.components.AppIcons
import com.driveready.testprep.ui.components.Disclaimer
import com.driveready.testprep.ui.components.EmptyNote
import com.driveready.testprep.ui.components.FlagBadge
import com.driveready.testprep.ui.components.LinearMeter
import com.driveready.testprep.ui.components.ScreenHeader
import com.driveready.testprep.ui.theme.Brand

@Composable
fun ProgressScreen(
    country: Country,
    settings: UserSettings,
    attempts: List<TestAttempt>,
    readiness: Readiness,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val theory = attempts.filter { it.type == "theory" }
    val hazard = attempts.filter { it.type == "hazard" }
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader("Progress", country.name, onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${settings.streakDays}", "day streak", Modifier.weight(1f))
                Stat("${theory.size}", "mock exams", Modifier.weight(1f))
                Stat("${theory.count { it.passed }}", "passed", Modifier.weight(1f))
            }
            AppCard {
                Text("Mock theory scores", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                if (theory.isEmpty()) EmptyNote("Take a mock exam to see your trend.")
                else ScoreChart(theory.takeLast(10))
            }
            AppCard {
                Text("Readiness by area", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Area("Theory (best mock)", readiness.theory)
                Area("Road signs mastered", readiness.signs)
                Area("Hazard perception (best)", readiness.hazard)
                Area("Oral answers confident", readiness.oral)
                Area("Practical maneuvers confident", readiness.practical)
            }
            if (hazard.isNotEmpty()) {
                AppCard {
                    Text("Hazard perception", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    hazard.takeLast(5).reversed().forEach {
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text(if (it.passed) "Pass" else "Not yet", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = if (it.passed) Brand.Good else Brand.Bad)
                            Text("${it.score}/${it.total}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    AppCard(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
    }
}

@Composable
private fun Area(label: String, value: Float) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.height(6.dp))
    LinearMeter(value)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ScoreChart(attempts: List<TestAttempt>) {
    val pct = attempts.map { it.score.toFloat() / it.total }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .semantics { contentDescription = "Mock scores: " + pct.joinToString { "${(it * 100).toInt()}%" } }
    ) {
        val gap = 10.dp.toPx()
        val barW = (size.width - gap * (pct.size - 1)) / pct.size
        val passY = size.height * (1 - 0.8f)
        pct.forEachIndexed { i, p ->
            val h = size.height * p
            drawRoundRect(
                if (attempts[i].passed) Brand.Ink else Brand.Bad.copy(alpha = 0.7f),
                Offset(i * (barW + gap), size.height - h),
                Size(barW, h),
                CornerRadius(6.dp.toPx())
            )
        }
        drawLine(Brand.Amber, Offset(0f, passY), Offset(size.width, passY), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
    }
    Spacer(Modifier.height(6.dp))
    Text("Dashed line: typical 80% pass level", style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
}

@Composable
fun SettingsScreen(
    country: Country,
    region: Region?,
    showPrivacyOptions: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onChangeCountry: () -> Unit,
    onPrivacy: () -> Unit,
    onRate: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader("Settings", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppCard(onClick = onChangeCountry) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlagBadge(country, 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Country & region", style = MaterialTheme.typography.titleSmall)
                        Text(listOfNotNull(region?.name, country.name).joinToString(", "), style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                    }
                    Icon(AppIcons.ChevronRight, contentDescription = null)
                }
            }
            SettingRow("Rate DriveReady", AppIcons.Star, onRate)
            SettingRow("Share with a friend", AppIcons.Globe, onShare)
            if (showPrivacyOptions) SettingRow("Privacy & ad choices", AppIcons.Shield, onPrivacy)
            Disclaimer()
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = Brand.Muted, modifier = Modifier.padding(4.dp))
        }
    }
}

@Composable
private fun SettingRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    AppCard(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(AppIcons.ChevronRight, contentDescription = null)
        }
    }
}
