package com.driveready.testprep

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.driveready.testprep.data.content.Maneuvers
import com.driveready.testprep.data.content.countryById
import com.driveready.testprep.data.content.questionsFor
import com.driveready.testprep.data.content.signsFor
import com.driveready.testprep.data.local.AppDatabase
import com.driveready.testprep.data.local.TestAttempt
import com.driveready.testprep.data.local.UserPrefs
import com.driveready.testprep.data.local.UserSettings
import com.driveready.testprep.data.model.Country
import com.driveready.testprep.data.model.Question
import com.driveready.testprep.data.model.QuestionCategory
import com.driveready.testprep.data.model.Region
import com.driveready.testprep.quiz.QuizConfig
import com.driveready.testprep.quiz.QuizResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class Readiness(
    val overall: Float,
    val theory: Float,
    val signs: Float,
    val hazard: Float,
    val oral: Float,
    val practical: Float
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = UserPrefs(app)
    private val dao = AppDatabase.get(app).attempts()

    /** Null until DataStore has emitted once (used to hold the splash screen). */
    val settings: StateFlow<UserSettings?> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val attempts: StateFlow<List<TestAttempt>> = prefs.settings
        .map { it.countryId }
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else dao.observeForCountry(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Quiz about to be started (set just before navigating to the quiz screen). */
    var activeQuiz: QuizConfig? = null

    /** Result of the most recently finished quiz, shown on the result screen. */
    var lastResult: QuizResult? = null
        private set

    init {
        viewModelScope.launch { prefs.recordActiveDay(LocalDate.now().toEpochDay()) }
    }

    fun country(settings: UserSettings?): Country? = countryById(settings?.countryId)

    fun region(settings: UserSettings?): Region? {
        val c = country(settings) ?: return null
        return c.regions.firstOrNull { it.id == settings?.regionId }
    }

    fun questionPool(country: Country): List<Question> =
        questionsFor(country.id, country.drivingSide, country.speedUnit)

    /** Mock exam built from the official format (region overrides country when set). */
    fun mockConfig(country: Country, region: Region?): QuizConfig {
        val pool = questionPool(country)
        val officialCount = region?.questions ?: country.theory.questions.takeIf { it > 0 } ?: 30
        val officialPass = region?.passMark ?: country.theory.passMark.takeIf { it > 0 }
            ?: kotlin.math.ceil(officialCount * 0.8).toInt()
        val count = officialCount.coerceAtMost(pool.size)
        val pass = if (count == officialCount) officialPass else
            kotlin.math.ceil(officialPass.toDouble() * count / officialCount).toInt()
        val minutes = country.theory.minutes ?: (count * 1.2).toInt().coerceAtLeast(10)
        return QuizConfig(
            title = "Mock ${region?.name?.let { "$it " } ?: ""}${country.theory.name.lowercase()}",
            questions = pool.shuffled().take(count),
            passMark = pass,
            timeLimitSec = minutes * 60,
            showFeedback = false,
            isMock = true
        )
    }

    fun practiceConfig(country: Country, category: QuestionCategory?): QuizConfig {
        val pool = questionPool(country).filter { category == null || it.category == category }
        val qs = pool.shuffled().take(15)
        return QuizConfig(
            title = category?.label ?: "Quick practice",
            questions = qs,
            passMark = kotlin.math.ceil(qs.size * 0.8).toInt(),
            timeLimitSec = null,
            showFeedback = true,
            isMock = false
        )
    }

    fun finishQuiz(country: Country, result: QuizResult) {
        lastResult = result
        if (result.config.isMock) {
            viewModelScope.launch {
                dao.insert(
                    TestAttempt(
                        countryId = country.id,
                        type = "theory",
                        score = result.correct,
                        total = result.total,
                        passed = result.passed,
                        timestamp = System.currentTimeMillis(),
                        weakestCategory = result.weakestCategory?.label ?: ""
                    )
                )
            }
        }
    }

    fun saveHazard(country: Country, score: Int, total: Int, passed: Boolean) {
        viewModelScope.launch {
            dao.insert(TestAttempt(countryId = country.id, type = "hazard", score = score, total = total, passed = passed, timestamp = System.currentTimeMillis()))
        }
    }

    fun readiness(country: Country, s: UserSettings, attempts: List<TestAttempt>): Readiness {
        val theoryBest = attempts.filter { it.type == "theory" }.maxOfOrNull { it.score.toFloat() / it.total } ?: 0f
        val hazardBest = attempts.filter { it.type == "hazard" }.maxOfOrNull { it.score.toFloat() / it.total } ?: 0f
        val signTotal = signsFor(country).size.coerceAtLeast(1)
        val signs = s.masteredSigns.count { it.startsWith("${country.id}:") }.toFloat() / signTotal
        val oralTotal = country.oral.questions.size.coerceAtLeast(1)
        val oral = s.oralConfident.count { it.startsWith("${country.id}:") }.toFloat() / oralTotal
        val manTotal = country.practical.maneuverIds.count { it in Maneuvers }.coerceAtLeast(1)
        val practical = s.maneuversConfident.count { it.startsWith("${country.id}:") }.toFloat() / manTotal
        val overall = theoryBest * 0.35f + signs.coerceAtMost(1f) * 0.15f + hazardBest * 0.15f + oral.coerceAtMost(1f) * 0.15f + practical.coerceAtMost(1f) * 0.2f
        return Readiness(overall, theoryBest, signs.coerceAtMost(1f), hazardBest, oral.coerceAtMost(1f), practical.coerceAtMost(1f))
    }

    fun completeOnboarding() = viewModelScope.launch { prefs.setOnboardingDone() }
    fun selectCountry(countryId: String, regionId: String?) = viewModelScope.launch { prefs.setCountry(countryId, regionId) }
    fun setTestDate(epochDay: Long?) = viewModelScope.launch { prefs.setTestDate(epochDay) }
    fun toggleSign(country: Country, id: String) = viewModelScope.launch { prefs.toggleSign("${country.id}:$id") }
    fun toggleChecklist(country: Country, id: String) = viewModelScope.launch { prefs.toggleChecklist("${country.id}:$id") }
    fun toggleOral(country: Country, index: Int) = viewModelScope.launch { prefs.toggleOral("${country.id}:$index") }
    fun toggleManeuver(country: Country, id: String) = viewModelScope.launch { prefs.toggleManeuver("${country.id}:$id") }
    fun toggleSyllabus(country: Country, key: String) = viewModelScope.launch { prefs.toggleSyllabus("${country.id}:$key") }
}
