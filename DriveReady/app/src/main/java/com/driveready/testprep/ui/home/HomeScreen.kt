package com.driveready.testprep.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driveready.testprep.Readiness
import com.driveready.testprep.ads.NativeAdCard
import com.driveready.testprep.data.local.UserSettings
import com.driveready.testprep.data.model.Country
import com.driveready.testprep.data.model.Region
import com.driveready.testprep.ui.components.AppCard
import com.driveready.testprep.ui.components.AppIcons
import com.driveready.testprep.ui.components.FlagBadge
import com.driveready.testprep.ui.components.IconTile
import com.driveready.testprep.ui.components.LinearMeter
import com.driveready.testprep.ui.components.RingMeter
import com.driveready.testprep.ui.components.SectionTitle
import com.driveready.testprep.ui.theme.Brand
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class HomeAction { MOCK, PRACTICE, SYLLABUS, SIGNS, HAZARD, ORAL, PRACTICAL, TEST_DAY, PROGRESS, SETTINGS, CHANGE_COUNTRY }

@Composable
fun HomeScreen(
    country: Country,
    region: Region?,
    settings: UserSettings,
    readiness: Readiness,
    contentPadding: PaddingValues,
    onAction: (HomeAction) -> Unit,
    onSetTestDate: (Long?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val testDate = settings.testDateEpochDay?.let { LocalDate.ofEpochDay(it) }
    val daysLeft = testDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = Brand.Muted)
                Text("Let's get you test-ready", style = MaterialTheme.typography.headlineSmall)
            }
            IconButton(onClick = { onAction(HomeAction.SETTINGS) }) {
                Icon(AppIcons.Settings, contentDescription = "Settings", tint = Brand.Ink)
            }
        }

        Surface(onClick = { onAction(HomeAction.CHANGE_COUNTRY) }, shape = RoundedCornerShape(16.dp), color = Brand.Card, border = androidx.compose.foundation.BorderStroke(1.dp, Brand.Line)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                FlagBadge(country, 34.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(listOfNotNull(region?.name, country.name).joinToString(", "), style = MaterialTheme.typography.titleSmall)
                    Text(region?.agency ?: country.authority, style = MaterialTheme.typography.bodySmall, color = Brand.Muted, maxLines = 1)
                }
                Text("Change", style = MaterialTheme.typography.labelLarge, color = Brand.Blue)
            }
        }

        // Readiness + countdown hero.
        Surface(shape = RoundedCornerShape(24.dp), color = Brand.Navy, onClick = { onAction(HomeAction.PROGRESS) }) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                RingMeter(readiness.overall, 92.dp, 10.dp, Brand.Amber, Color(0xFF2B3446)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(readiness.overall * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("ready", color = Color(0xFFC9CBD1), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("YOUR TEST", color = Brand.Amber, style = MaterialTheme.typography.labelMedium)
                    Text(
                        when {
                            daysLeft == null -> "Set your test date"
                            daysLeft > 1 -> "In $daysLeft days"
                            daysLeft == 1L -> "Tomorrow"
                            daysLeft == 0L -> "Today — good luck!"
                            else -> "Test date passed"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        testDate?.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy")) ?: "Tap the calendar to add it",
                        color = Color(0xFFC9CBD1),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(AppIcons.Calendar, contentDescription = "Set test date", tint = Color.White)
                }
            }
        }

        // Official format summary.
        AppCard {
            Text("Your test, at a glance", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            val questions = region?.questions ?: country.theory.questions
            val pass = region?.passMark ?: country.theory.passMark
            FormatRow(AppIcons.Book, country.theory.name, if (questions > 0 && pass > 0) "$questions questions · $pass to pass" else country.theory.notes.firstOrNull() ?: "See syllabus")
            FormatRow(AppIcons.Eye, country.hazard?.name ?: "Hazard perception", if (country.hazard?.isOfficialPart == true) "Official part of the test" else "Practice — marked on the road test")
            FormatRow(AppIcons.Mic, country.oral.name, "${country.oral.questions.size} practice questions")
            FormatRow(AppIcons.Wheel, country.practical.name, country.practical.durationMinutes?.let { "About $it minutes" } ?: "Examiner-marked")
        }

        SectionTitle("Study")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile("Mock exam", "Real format", AppIcons.Timer, Brand.AmberSoft, Modifier.weight(1f), readiness.theory) { onAction(HomeAction.MOCK) }
                ActionTile("Road signs", "${(readiness.signs * 100).toInt()}% mastered", AppIcons.Signs, Brand.AmberSoft, Modifier.weight(1f), readiness.signs) { onAction(HomeAction.SIGNS) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile("Hazard clips", "Tap as it develops", AppIcons.Eye, Brand.BlueSoft, Modifier.weight(1f), readiness.hazard) { onAction(HomeAction.HAZARD) }
                ActionTile("Oral test", country.oral.name, AppIcons.Mic, Brand.BlueSoft, Modifier.weight(1f), readiness.oral) { onAction(HomeAction.ORAL) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile("Practical", "Maneuver guides", AppIcons.Wheel, Brand.GoodSoft, Modifier.weight(1f), readiness.practical) { onAction(HomeAction.PRACTICAL) }
                ActionTile("Syllabus", "${country.syllabus.size} modules", AppIcons.List, Brand.GoodSoft, Modifier.weight(1f), null) { onAction(HomeAction.SYLLABUS) }
            }
        }

        NativeAdCard()

        SectionTitle("Before the big day")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionTile("Test day", "Documents & checklist", AppIcons.Doc, Color(0xFFEDEBE4), Modifier.weight(1f), null) { onAction(HomeAction.TEST_DAY) }
            ActionTile("Progress", "Scores & streak", AppIcons.Chart, Color(0xFFEDEBE4), Modifier.weight(1f), null) { onAction(HomeAction.PROGRESS) }
        }

        country.tips.firstOrNull()?.let { tip ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brand.AmberSoft)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(AppIcons.Lightbulb, contentDescription = null, tint = Brand.Ink, modifier = Modifier.size(20.dp))
                Text(tip, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = settings.testDateEpochDay?.let { LocalDate.ofEpochDay(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        onSetTestDate(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay())
                    }
                    showDatePicker = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { onSetTestDate(null); showDatePicker = false }) { Text("Clear") }
            }
        ) { DatePicker(state = state) }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun FormatRow(icon: ImageVector, title: String, detail: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, Brand.Surface, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier,
    progress: Float?,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        IconTile(icon, tint)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Brand.Muted, maxLines = 1)
        if (progress != null) {
            Spacer(Modifier.height(8.dp))
            LinearMeter(progress)
        }
    }
}
