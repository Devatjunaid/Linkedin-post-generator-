package com.driveready.testprep.ui.theory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driveready.testprep.ads.NativeAdCard
import com.driveready.testprep.data.model.Country
import com.driveready.testprep.data.model.QuestionCategory
import com.driveready.testprep.data.model.Region
import com.driveready.testprep.quiz.QuizConfig
import com.driveready.testprep.quiz.QuizResult
import com.driveready.testprep.quiz.ShuffledQuestion
import com.driveready.testprep.quiz.shuffleOptions
import com.driveready.testprep.ui.components.AppCard
import com.driveready.testprep.ui.components.AppIcons
import com.driveready.testprep.ui.components.Disclaimer
import com.driveready.testprep.ui.components.IconTile
import com.driveready.testprep.ui.components.LinearMeter
import com.driveready.testprep.ui.components.Pill
import com.driveready.testprep.ui.components.PrimaryButton
import com.driveready.testprep.ui.components.RoadSign
import com.driveready.testprep.ui.components.RingMeter
import com.driveready.testprep.ui.components.ScreenHeader
import com.driveready.testprep.ui.components.SecondaryButton
import com.driveready.testprep.ui.components.SectionTitle
import com.driveready.testprep.ui.theme.Brand
import kotlinx.coroutines.delay

/** Theory tab: mock exam, practice by topic, signs and syllabus. */
@Composable
fun TheoryHubScreen(
    country: Country,
    region: Region?,
    poolSize: Int,
    contentPadding: PaddingValues,
    onMock: () -> Unit,
    onPractice: (QuestionCategory?) -> Unit,
    onSigns: () -> Unit,
    onSyllabus: () -> Unit
) {
    val questions = region?.questions ?: country.theory.questions
    val pass = region?.passMark ?: country.theory.passMark
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader("Theory", listOfNotNull(region?.name, country.name).joinToString(", ") + " · ${country.theory.name}")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(24.dp), color = Brand.Navy, onClick = onMock) {
                Column(Modifier.padding(20.dp)) {
                    Pill("MOCK EXAM", Brand.Amber)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (questions > 0 && pass > 0) "$questions questions · $pass to pass" else "Full mock in the official style",
                        color = Color.White, style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        (region?.notes?.takeIf { it.isNotBlank() } ?: country.theory.notes.firstOrNull() ?: "Timed, with answers revealed at the end."),
                        color = Color(0xFFC9CBD1), style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Play, contentDescription = null, tint = Brand.Amber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Start mock exam", color = Brand.Amber, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (country.theory.notes.size > 1) {
                AppCard {
                    Text("How the test works", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    country.theory.notes.forEach { note ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("•", color = Brand.Amber, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text(note, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppCard(onClick = onSigns, modifier = Modifier.weight(1f)) {
                    IconTile(AppIcons.Signs, Brand.AmberSoft)
                    Spacer(Modifier.height(8.dp))
                    Text("Road signs", style = MaterialTheme.typography.titleSmall)
                    Text("Library & flashcards", style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                }
                AppCard(onClick = onSyllabus, modifier = Modifier.weight(1f)) {
                    IconTile(AppIcons.List, Brand.GoodSoft)
                    Spacer(Modifier.height(8.dp))
                    Text("Syllabus", style = MaterialTheme.typography.titleSmall)
                    Text("${country.syllabus.size} modules", style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                }
            }

            SectionTitle("Practice by topic")
            AppCard(onClick = { onPractice(null) }, contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(AppIcons.Timer, Brand.BlueSoft)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Quick 15", style = MaterialTheme.typography.titleSmall)
                        Text("Mixed questions from $poolSize in your bank", style = MaterialTheme.typography.bodySmall, color = Brand.Muted)
                    }
                    Icon(AppIcons.ChevronRight, contentDescription = null)
                }
            }
            QuestionCategory.entries.forEachIndexed { i, cat ->
                AppCard(onClick = { onPractice(cat) }, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Icon(AppIcons.ChevronRight, contentDescription = null)
                    }
                }
                if (i == 3) NativeAdCard()
            }
            Disclaimer()
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Runs a quiz session. Mock mode hides answers until the end and enforces the time limit. */
@Composable
fun QuizScreen(
    config: QuizConfig,
    country: Country,
    contentPadding: PaddingValues,
    onExit: () -> Unit,
    onFinish: (QuizResult) -> Unit
) {
    val seed = rememberSaveable { System.nanoTime() }
    val items: List<ShuffledQuestion> = remember(config) { config.questions.mapIndexed { i, q -> shuffleOptions(q, seed + i) } }
    val answers = remember(config) { mutableStateListOf<Int?>().apply { repeat(items.size) { add(null) } } }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var elapsed by rememberSaveable { mutableIntStateOf(0) }
    var confirmExit by remember { mutableStateOf(false) }
    val flagged = remember(config) { mutableStateListOf<Int>() }

    fun finish() = onFinish(QuizResult(config, items, answers.toList(), elapsed))

    LaunchedEffect(config) {
        while (true) {
            delay(1000)
            elapsed++
            val limit = config.timeLimitSec
            if (limit != null && elapsed >= limit) {
                finish(); break
            }
        }
    }
    BackHandler { confirmExit = true }

    if (items.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(contentPadding).padding(24.dp)) {
            Text("No questions available for this topic yet.")
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Back", onExit)
        }
        return
    }

    val item = items[index]
    val answer = answers[index]
    val revealed = config.showFeedback && answer != null

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { confirmExit = true }) { Icon(AppIcons.Close, contentDescription = "Exit quiz") }
            Column(Modifier.weight(1f)) {
                Row {
                    Text("Question ${index + 1} of ${items.size}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    val limit = config.timeLimitSec
                    if (limit != null) {
                        val left = (limit - elapsed).coerceAtLeast(0)
                        Text("%d:%02d left".format(left / 60, left % 60), style = MaterialTheme.typography.labelLarge, color = if (left < 60) Brand.Bad else Brand.Muted)
                    }
                }
                Spacer(Modifier.height(6.dp))
                LinearMeter((index + 1f) / items.size)
            }
            IconButton(onClick = { if (index in flagged) flagged.remove(index) else flagged.add(index) }) {
                Icon(AppIcons.Flag, contentDescription = "Flag for review", tint = if (index in flagged) Brand.Bad else Brand.Ink)
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(item.question.category.label)
                Pill(if (config.isMock) "Mock exam" else "Practice", Color(0xFFEDEBE4), Color(0xFF3F4249))
            }
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    item.question.sign?.let { sign ->
                        RoadSign(
                            type = sign,
                            style = country.signStyle,
                            size = 120.dp,
                            value = item.question.signValue,
                            rectangularSpeed = country.id == "us" || country.id == "ca",
                            giveWayText = giveWayText(country)
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(item.question.text, style = MaterialTheme.typography.titleLarge)
                }
            }
            item.options.forEachIndexed { i, option ->
                val isCorrect = i == item.correctIndex
                val (bg, border, tag) = when {
                    revealed && isCorrect -> Triple(Brand.GoodSoft, Brand.Good, "Correct")
                    revealed && i == answer -> Triple(Brand.BadSoft, Brand.Bad, "Your answer")
                    !revealed && i == answer -> Triple(Brand.AmberSoft, Brand.Ink, "")
                    else -> Triple(Brand.Card, Brand.Line, "")
                }
                Surface(
                    onClick = { if (!revealed) answers[index] = i },
                    shape = RoundedCornerShape(16.dp),
                    color = bg,
                    border = BorderStroke(if (border == Brand.Line) 1.dp else 2.dp, border),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(if (border == Brand.Line) Color(0xFFEDEBE4) else border),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(('A' + i).toString(), color = if (border == Brand.Line) Brand.Ink else Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(option, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (tag.isNotEmpty()) Text(tag, style = MaterialTheme.typography.labelMedium, color = border)
                    }
                }
            }
            if (revealed) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brand.Ink)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(AppIcons.Lightbulb, contentDescription = null, tint = Brand.Amber, modifier = Modifier.size(22.dp))
                    Text(item.question.explanation, color = Color(0xFFE4E6EA), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Previous", { if (index > 0) index-- }, Modifier.weight(1f))
            val last = index == items.lastIndex
            PrimaryButton(if (last) "Finish" else "Next", { if (last) finish() else index++ }, Modifier.weight(1.5f))
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Leave this test?") },
            text = { Text(if (config.isMock) "Your answers will be lost and the mock won't be scored." else "Your progress in this practice set will be lost.") },
            confirmButton = { TextButton(onClick = { confirmExit = false; onExit() }) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Keep going") } }
        )
    }
}

fun giveWayText(country: Country): String? = when {
    country.id == "uk" -> "GIVE WAY"
    country.signStyle.name == "DIAMOND" -> "YIELD"
    else -> null
}

@Composable
fun ResultScreen(
    result: QuizResult,
    country: Country,
    contentPadding: PaddingValues,
    onDone: () -> Unit,
    onRetry: () -> Unit
) {
    var reviewOpen by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(result.config.title, onBack = onDone)
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(24.dp), color = Brand.Navy) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Pill(if (result.passed) "PASS" else "NOT YET", if (result.passed) Brand.Amber else Brand.BadSoft, if (result.passed) Brand.Ink else Brand.Bad)
                    Spacer(Modifier.height(12.dp))
                    RingMeter(result.correct.toFloat() / result.total, 132.dp, 12.dp, if (result.passed) Brand.Amber else Color(0xFFFF8A65), Color(0xFF2B3446)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${result.correct}", color = Color.White, style = MaterialTheme.typography.displaySmall)
                            Text("of ${result.total}", color = Color(0xFFC9CBD1), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Pass mark ${result.config.passMark} · ${result.elapsedSec / 60} min ${result.elapsedSec % 60} s",
                        color = Color(0xFFC9CBD1), style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            AppCard {
                Text("By topic", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                result.byCategory.forEach { cs ->
                    val f = cs.correct.toFloat() / cs.total
                    Row {
                        Text(cs.category.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("${cs.correct}/${cs.total}", style = MaterialTheme.typography.labelLarge, color = if (f < 0.8f) Brand.Bad else Brand.Ink)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearMeter(f, color = if (f < 0.8f) Brand.Bad else Brand.Ink)
                    Spacer(Modifier.height(10.dp))
                }
            }

            result.weakestCategory?.let { weak ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brand.BadSoft)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(AppIcons.Warning, contentDescription = null, tint = Brand.Bad, modifier = Modifier.size(22.dp))
                    Column {
                        Text("Focus next: ${weak.label}", style = MaterialTheme.typography.titleSmall)
                        Text("Practise this topic from the Theory tab.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF3F4249))
                    }
                }
            }

            NativeAdCard()

            SecondaryButton(if (reviewOpen) "Hide answers" else "Review answers", { reviewOpen = !reviewOpen }, Modifier.fillMaxWidth(), icon = AppIcons.Eye)
            if (reviewOpen) {
                result.items.forEachIndexed { i, item ->
                    val ans = result.answers.getOrNull(i)
                    val ok = ans == item.correctIndex
                    AppCard(color = if (ok) Brand.GoodSoft else Brand.BadSoft, border = false) {
                        Text("${i + 1}. ${item.question.text}", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        Text("Correct: ${item.options[item.correctIndex]}", style = MaterialTheme.typography.bodyMedium, color = Brand.Good)
                        if (!ok) Text("Your answer: ${ans?.let { item.options[it] } ?: "Not answered"}", style = MaterialTheme.typography.bodyMedium, color = Brand.Bad)
                        Spacer(Modifier.height(6.dp))
                        Text(item.question.explanation, style = MaterialTheme.typography.bodySmall, color = Color(0xFF3F4249))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Done", onDone, Modifier.weight(1f))
                PrimaryButton("Try again", onRetry, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
