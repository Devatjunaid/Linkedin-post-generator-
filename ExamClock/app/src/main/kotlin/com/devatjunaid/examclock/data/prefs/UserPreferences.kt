package com.devatjunaid.examclock.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val onboardingDone: Boolean = false,
    val userName: String = "",
    val countryCode: String = "",
    val dailyGoalMinutes: Int = 120,
    val remindersEnabled: Boolean = true,
    val reminderMinuteOfDay: Int = 17 * 60,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val blocksBeforeLongBreak: Int = 4,
    val keepScreenOn: Boolean = true,
    val plan: PlanPreferences = PlanPreferences(),
)

data class PlanPreferences(
    val weekdayMinutes: Int = 180,
    val weekendMinutes: Int = 300,
    val restDays: Set<DayOfWeek> = emptySet(),
    val sessionMinutes: Int = 45,
    val breakMinutes: Int = 10,
    val pastPaperDays: Int = 3,
    val dayStartMinute: Int = 16 * 60,
    val dayEndMinute: Int = 22 * 60,
)

/** Running focus timer, persisted so it survives the app being closed or killed. */
data class FocusTimerState(
    val phase: Int = PHASE_IDLE,
    /** Wall-clock end of the running block, 0 when paused or idle. */
    val endAt: Long = 0L,
    /** Remaining millis while paused. */
    val pausedRemaining: Long = 0L,
    val blockMinutes: Int = 0,
    val sessionId: Long = 0L,
    val examId: Long = 0L,
    val completedBlocks: Int = 0,
    val blockStartedAt: Long = 0L,
) {
    val isRunning: Boolean get() = endAt > 0L
    val isPaused: Boolean get() = endAt == 0L && pausedRemaining > 0L

    companion object {
        const val PHASE_IDLE = 0
        const val PHASE_FOCUS = 1
        const val PHASE_BREAK = 2
    }
}

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val userName = stringPreferencesKey("user_name")
        val countryCode = stringPreferencesKey("country_code")
        val dailyGoal = intPreferencesKey("daily_goal_minutes")
        val reminders = booleanPreferencesKey("reminders_enabled")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val theme = intPreferencesKey("theme_mode")
        val focusMinutes = intPreferencesKey("focus_minutes")
        val shortBreak = intPreferencesKey("short_break_minutes")
        val longBreak = intPreferencesKey("long_break_minutes")
        val blocksBeforeLong = intPreferencesKey("blocks_before_long")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")

        val planWeekday = intPreferencesKey("plan_weekday")
        val planWeekend = intPreferencesKey("plan_weekend")
        val planRestDays = stringPreferencesKey("plan_rest_days")
        val planSession = intPreferencesKey("plan_session")
        val planBreak = intPreferencesKey("plan_break")
        val planPaperDays = intPreferencesKey("plan_paper_days")
        val planDayStart = intPreferencesKey("plan_day_start")
        val planDayEnd = intPreferencesKey("plan_day_end")

        val timerPhase = intPreferencesKey("timer_phase")
        val timerEndAt = longPreferencesKey("timer_end_at")
        val timerPaused = longPreferencesKey("timer_paused_remaining")
        val timerBlockMinutes = intPreferencesKey("timer_block_minutes")
        val timerSession = longPreferencesKey("timer_session")
        val timerExam = longPreferencesKey("timer_exam")
        val timerCompleted = intPreferencesKey("timer_completed_blocks")
        val timerStartedAt = longPreferencesKey("timer_started_at")

        val lastPlanAt = longPreferencesKey("last_plan_generated_at")
        val milestonesSent = stringPreferencesKey("milestones_sent")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { p ->
        val defaults = UserPreferences()
        val planDefaults = PlanPreferences()
        UserPreferences(
            onboardingDone = p[Keys.onboardingDone] ?: false,
            userName = p[Keys.userName] ?: "",
            countryCode = p[Keys.countryCode] ?: "",
            dailyGoalMinutes = p[Keys.dailyGoal] ?: defaults.dailyGoalMinutes,
            remindersEnabled = p[Keys.reminders] ?: defaults.remindersEnabled,
            reminderMinuteOfDay = p[Keys.reminderMinute] ?: defaults.reminderMinuteOfDay,
            themeMode = ThemeMode.entries.getOrElse(p[Keys.theme] ?: 0) { ThemeMode.SYSTEM },
            focusMinutes = p[Keys.focusMinutes] ?: defaults.focusMinutes,
            shortBreakMinutes = p[Keys.shortBreak] ?: defaults.shortBreakMinutes,
            longBreakMinutes = p[Keys.longBreak] ?: defaults.longBreakMinutes,
            blocksBeforeLongBreak = p[Keys.blocksBeforeLong] ?: defaults.blocksBeforeLongBreak,
            keepScreenOn = p[Keys.keepScreenOn] ?: defaults.keepScreenOn,
            plan = PlanPreferences(
                weekdayMinutes = p[Keys.planWeekday] ?: planDefaults.weekdayMinutes,
                weekendMinutes = p[Keys.planWeekend] ?: planDefaults.weekendMinutes,
                restDays = decodeDays(p[Keys.planRestDays]),
                sessionMinutes = p[Keys.planSession] ?: planDefaults.sessionMinutes,
                breakMinutes = p[Keys.planBreak] ?: planDefaults.breakMinutes,
                pastPaperDays = p[Keys.planPaperDays] ?: planDefaults.pastPaperDays,
                dayStartMinute = p[Keys.planDayStart] ?: planDefaults.dayStartMinute,
                dayEndMinute = p[Keys.planDayEnd] ?: planDefaults.dayEndMinute,
            ),
        )
    }

    val timer: Flow<FocusTimerState> = context.dataStore.data.map { p ->
        FocusTimerState(
            phase = p[Keys.timerPhase] ?: FocusTimerState.PHASE_IDLE,
            endAt = p[Keys.timerEndAt] ?: 0L,
            pausedRemaining = p[Keys.timerPaused] ?: 0L,
            blockMinutes = p[Keys.timerBlockMinutes] ?: 0,
            sessionId = p[Keys.timerSession] ?: 0L,
            examId = p[Keys.timerExam] ?: 0L,
            completedBlocks = p[Keys.timerCompleted] ?: 0,
            blockStartedAt = p[Keys.timerStartedAt] ?: 0L,
        )
    }

    suspend fun current(): UserPreferences = preferences.first()

    suspend fun completeOnboarding(name: String, countryCode: String, dailyGoalMinutes: Int) {
        context.dataStore.edit {
            it[Keys.userName] = name.trim()
            it[Keys.countryCode] = countryCode
            it[Keys.dailyGoal] = dailyGoalMinutes
            // Seed the revision plan: the daily goal on school days, 1.5x on the local weekend.
            it[Keys.planWeekday] = dailyGoalMinutes
            it[Keys.planWeekend] = (dailyGoalMinutes * 3 / 2).coerceAtMost(600)
            it[Keys.onboardingDone] = true
        }
    }

    suspend fun setUserName(value: String) = context.dataStore.edit { it[Keys.userName] = value.trim() }
    suspend fun setCountry(code: String) = context.dataStore.edit { it[Keys.countryCode] = code }
    suspend fun setDailyGoal(minutes: Int) = context.dataStore.edit { it[Keys.dailyGoal] = minutes }
    suspend fun setReminders(enabled: Boolean) = context.dataStore.edit { it[Keys.reminders] = enabled }
    suspend fun setReminderMinute(minute: Int) = context.dataStore.edit { it[Keys.reminderMinute] = minute }
    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.theme] = mode.ordinal }
    suspend fun setKeepScreenOn(value: Boolean) = context.dataStore.edit { it[Keys.keepScreenOn] = value }

    suspend fun setFocusDurations(focus: Int, shortBreak: Int, longBreak: Int) = context.dataStore.edit {
        it[Keys.focusMinutes] = focus
        it[Keys.shortBreak] = shortBreak
        it[Keys.longBreak] = longBreak
    }

    suspend fun setPlan(plan: PlanPreferences) = context.dataStore.edit {
        it[Keys.planWeekday] = plan.weekdayMinutes
        it[Keys.planWeekend] = plan.weekendMinutes
        it[Keys.planRestDays] = encodeDays(plan.restDays)
        it[Keys.planSession] = plan.sessionMinutes
        it[Keys.planBreak] = plan.breakMinutes
        it[Keys.planPaperDays] = plan.pastPaperDays
        it[Keys.planDayStart] = plan.dayStartMinute
        it[Keys.planDayEnd] = plan.dayEndMinute
        it[Keys.lastPlanAt] = System.currentTimeMillis()
    }

    suspend fun setTimer(state: FocusTimerState) = context.dataStore.edit {
        it[Keys.timerPhase] = state.phase
        it[Keys.timerEndAt] = state.endAt
        it[Keys.timerPaused] = state.pausedRemaining
        it[Keys.timerBlockMinutes] = state.blockMinutes
        it[Keys.timerSession] = state.sessionId
        it[Keys.timerExam] = state.examId
        it[Keys.timerCompleted] = state.completedBlocks
        it[Keys.timerStartedAt] = state.blockStartedAt
    }

    /**
     * Atomically checks that the running block ending at [expectedEndAt] has not been handled yet
     * and marks it as finished. Returns true only for the single caller that wins.
     */
    suspend fun claimFinishedBlock(expectedEndAt: Long, next: FocusTimerState): Boolean {
        var claimed = false
        context.dataStore.edit {
            if ((it[Keys.timerEndAt] ?: 0L) == expectedEndAt && expectedEndAt > 0L) {
                claimed = true
                it[Keys.timerPhase] = next.phase
                it[Keys.timerEndAt] = next.endAt
                it[Keys.timerPaused] = next.pausedRemaining
                it[Keys.timerBlockMinutes] = next.blockMinutes
                it[Keys.timerSession] = next.sessionId
                it[Keys.timerExam] = next.examId
                it[Keys.timerCompleted] = next.completedBlocks
                it[Keys.timerStartedAt] = next.blockStartedAt
            }
        }
        return claimed
    }

    /** Remembers which "N days to go" notifications were already shown (key "examId:days"). */
    suspend fun milestonesSent(): Set<String> =
        context.dataStore.data.first()[Keys.milestonesSent]?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

    suspend fun addMilestone(key: String) = context.dataStore.edit {
        val current = it[Keys.milestonesSent]?.split(',')?.filter { s -> s.isNotBlank() }.orEmpty()
        it[Keys.milestonesSent] = (current.takeLast(200) + key).joinToString(",")
    }

    suspend fun clearAll() = context.dataStore.edit { it.clear() }

    private fun encodeDays(days: Set<DayOfWeek>) = days.joinToString(",") { it.value.toString() }

    private fun decodeDays(raw: String?): Set<DayOfWeek> =
        raw?.split(',')?.mapNotNull { it.toIntOrNull()?.takeIf { v -> v in 1..7 }?.let(DayOfWeek::of) }?.toSet() ?: emptySet()
}
