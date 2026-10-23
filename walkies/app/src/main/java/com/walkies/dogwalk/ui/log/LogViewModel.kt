package com.walkies.dogwalk.ui.log

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walkies.dogwalk.AppContainer
import com.walkies.dogwalk.R
import com.walkies.dogwalk.data.Consistency
import com.walkies.dogwalk.data.LogEntry
import com.walkies.dogwalk.data.LogType
import com.walkies.dogwalk.data.Walk
import com.walkies.dogwalk.data.durationMillis
import com.walkies.dogwalk.util.formatDay
import com.walkies.dogwalk.util.formatDuration
import com.walkies.dogwalk.util.startMillis
import com.walkies.dogwalk.util.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LogFilter { ALL, PEE, POO, MEALS, WALKS }

data class DaySummary(val date: LocalDate, val pee: Int, val poo: Int)

sealed interface LogItem {
    val time: Long

    data class Entry(val entry: LogEntry) : LogItem {
        override val time get() = entry.timestamp
    }

    data class WalkItem(val walk: Walk, val pee: Int, val poo: Int) : LogItem {
        override val time get() = walk.startTime
    }
}

data class LogState(
    val loaded: Boolean = false,
    val dogName: String = "",
    val today: LocalDate = LocalDate.now(),
    val days: List<DaySummary> = emptyList(),
    val avgPee: Double = 0.0,
    val avgPoo: Double = 0.0,
    val selected: LocalDate = LocalDate.now(),
    val filter: LogFilter = LogFilter.ALL,
    val items: List<LogItem> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val selected = MutableStateFlow(LocalDate.now())
    private val filter = MutableStateFlow(LogFilter.ALL)

    private val today = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000)
        }
    }.distinctUntilChanged()

    private val week = today.flatMapLatest { t ->
        val from = t.minusDays(DAYS - 1L).startMillis()
        val to = t.plusDays(1).startMillis()
        combine(repo.entriesBetween(from, to), repo.walksBetween(from, to)) { e, w -> Triple(t, e, w) }
    }

    val state: StateFlow<LogState> = combine(
        container.settings.settings,
        week,
        selected,
        filter,
    ) { settings, (t, entries, walks), sel, f ->
        val days = (DAYS - 1 downTo 0).map { back ->
            val d = t.minusDays(back.toLong())
            val dayEntries = entries.filter { it.timestamp.toLocalDate() == d }
            DaySummary(d, dayEntries.count { it.type == LogType.PEE }, dayEntries.count { it.type == LogType.POO })
        }
        val dayEntries = entries.filter { it.timestamp.toLocalDate() == sel }
        val dayWalks = walks.filter { it.startTime.toLocalDate() == sel }
        val items = buildList<LogItem> {
            if (f != LogFilter.WALKS) {
                dayEntries.filter {
                    when (f) {
                        LogFilter.PEE -> it.type == LogType.PEE
                        LogFilter.POO -> it.type == LogType.POO
                        LogFilter.MEALS -> it.type == LogType.MEAL
                        else -> true
                    }
                }.forEach { add(LogItem.Entry(it)) }
            }
            if (f == LogFilter.ALL || f == LogFilter.WALKS) {
                dayWalks.forEach { w ->
                    add(
                        LogItem.WalkItem(
                            w,
                            entries.count { it.walkId == w.id && it.type == LogType.PEE },
                            entries.count { it.walkId == w.id && it.type == LogType.POO },
                        ),
                    )
                }
            }
        }.sortedByDescending { it.time }
        LogState(
            loaded = true,
            dogName = settings.dogName,
            today = t,
            days = days,
            avgPee = days.sumOf { it.pee } / DAYS.toDouble(),
            avgPoo = days.sumOf { it.poo } / DAYS.toDouble(),
            selected = sel,
            filter = f,
            items = items,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogState())

    fun select(date: LocalDate) {
        selected.value = date
    }

    fun setFilter(f: LogFilter) {
        filter.value = f
    }

    fun setConsistency(entry: LogEntry, consistency: Consistency?) {
        viewModelScope.launch { repo.update(entry.copy(consistency = consistency)) }
    }

    fun delete(entry: LogEntry, onDeleted: (undo: () -> Unit) -> Unit) {
        viewModelScope.launch {
            repo.delete(entry)
            onDeleted { viewModelScope.launch { repo.restore(entry) } }
        }
    }

    fun deleteWalk(walk: Walk) {
        viewModelScope.launch { repo.deleteWalk(walk) }
    }

    fun addEntry(type: LogType, date: LocalDate, hour: Int, minute: Int, consistency: Consistency?) {
        val at = date.atTime(hour, minute).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        viewModelScope.launch {
            repo.log(type, timestamp = at.coerceAtMost(System.currentTimeMillis()), consistency = consistency)
        }
    }

    /** Plain-text summary of the last 30 days, for sharing with a vet. */
    suspend fun buildReport(context: Context): String {
        val end = LocalDate.now()
        val start = end.minusDays(REPORT_DAYS - 1L)
        val from = start.startMillis()
        val to = end.plusDays(1).startMillis()
        val entries = repo.entriesBetweenOnce(from, to)
        val walks = repo.walksBetweenOnce(from, to)
        val dogName = container.settings.current().dogName
        val poos = entries.filter { it.type == LogType.POO }
        fun avg(n: Int) = String.format(Locale.getDefault(), "%.1f", n / REPORT_DAYS.toDouble())
        val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

        return buildString {
            appendLine(context.getString(R.string.report_header, dogName))
            appendLine(context.getString(R.string.report_period, formatDay(start), formatDay(end), REPORT_DAYS))
            appendLine()
            appendLine(
                context.getString(
                    R.string.report_averages,
                    avg(entries.count { it.type == LogType.PEE }),
                    avg(poos.size),
                    avg(walks.size),
                ),
            )
            if (walks.isNotEmpty()) {
                appendLine(context.getString(R.string.report_walk_time, formatDuration(context, walks.sumOf { it.durationMillis() } / walks.size)))
            }
            appendLine(
                context.getString(
                    R.string.report_consistency,
                    poos.count { it.consistency == Consistency.FIRM },
                    poos.count { it.consistency == Consistency.SOFT },
                    poos.count { it.consistency == Consistency.RUNNY },
                    poos.count { it.consistency == null },
                ),
            )
            appendLine()
            appendLine(context.getString(R.string.report_by_day))
            for (back in 0 until REPORT_DAYS) {
                val d = end.minusDays(back.toLong())
                val dayEntries = entries.filter { it.timestamp.toLocalDate() == d }
                appendLine(
                    context.getString(
                        R.string.report_day_line,
                        dayFormat.format(d),
                        dayEntries.count { it.type == LogType.PEE },
                        dayEntries.count { it.type == LogType.POO },
                        walks.count { it.startTime.toLocalDate() == d },
                    ),
                )
            }
            appendLine()
            append(context.getString(R.string.report_footer))
        }
    }

    private companion object {
        const val DAYS = 7
        const val REPORT_DAYS = 30
    }
}
