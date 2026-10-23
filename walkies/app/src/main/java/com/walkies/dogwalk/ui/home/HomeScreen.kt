package com.walkies.dogwalk.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.walkies.dogwalk.AppContainer
import com.walkies.dogwalk.R
import com.walkies.dogwalk.ads.NativeAdCache
import com.walkies.dogwalk.ads.NativeAdCard
import com.walkies.dogwalk.data.LogEntry
import com.walkies.dogwalk.data.LogType
import com.walkies.dogwalk.data.Walk
import com.walkies.dogwalk.data.durationMillis
import com.walkies.dogwalk.ui.components.DogPhoto
import com.walkies.dogwalk.ui.components.EventBadge
import com.walkies.dogwalk.ui.components.SectionCard
import com.walkies.dogwalk.ui.components.SectionHeader
import com.walkies.dogwalk.ui.components.label
import com.walkies.dogwalk.ui.components.rememberDogPhotoPicker
import com.walkies.dogwalk.ui.components.style
import com.walkies.dogwalk.ui.components.walkStyle
import com.walkies.dogwalk.ui.containerFactory
import com.walkies.dogwalk.ui.theme.WalkiesColors
import com.walkies.dogwalk.util.formatClock
import com.walkies.dogwalk.util.formatDuration
import com.walkies.dogwalk.util.formatStopwatch
import com.walkies.dogwalk.util.formatWeekdayDate
import com.walkies.dogwalk.util.toLocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenSettings: () -> Unit,
    onOpenWalk: () -> Unit,
    onLogged: (LogEntry, undo: () -> Unit) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = containerFactory { HomeViewModel(it) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickPhoto = rememberDogPhotoPicker { viewModel.setPhoto(it.toString()) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Header(state, onOpenSettings)
        Spacer(Modifier.height(16.dp))
        HeroCard(state, onAddPhoto = pickPhoto, onOpenWalk = onOpenWalk)
        Spacer(Modifier.height(22.dp))
        SectionHeader(stringResource(R.string.home_quick_log)) {
            Text(
                stringResource(R.string.home_quick_log_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        QuickLogGrid(
            walkRunning = state.activeWalk != null,
            onLog = { type -> viewModel.log(type) { entry -> onLogged(entry) { viewModel.undo(entry) } } },
            onWalk = { if (state.activeWalk != null) onOpenWalk() else viewModel.startWalk(onOpenWalk) },
        )
        Spacer(Modifier.height(18.dp))
        NativeAdCard(container.ads, NativeAdCache.HOME)
        Spacer(Modifier.height(18.dp))
        TodayCard(state)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Header(state: HomeState, onOpenSettings: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = stringResource(
        when {
            hour < 12 -> R.string.home_greeting_morning
            hour < 18 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        },
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "$greeting · ${formatWeekdayDate(state.now.toLocalDate())}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.home_title, state.dogName),
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FilledIconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings))
        }
    }
}

@Composable
private fun HeroCard(state: HomeState, onAddPhoto: () -> Unit, onOpenWalk: () -> Unit) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(236.dp)
            .clip(RoundedCornerShape(30.dp)),
    ) {
        DogPhoto(state.photoUri, contentDescription = state.dogName, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.10f),
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.62f),
                    ),
                ),
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 6.dp),
            color = MaterialTheme.colorScheme.surface,
            onClick = onOpenWalk,
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                val active = state.activeWalk
                if (active != null) {
                    Text(
                        stringResource(R.string.home_walk_running).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = WalkiesColors.WalkDeep,
                    )
                    Text(formatStopwatch(active.durationMillis(state.now)), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.home_resume_walk), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val next = state.nextWalk
                    Text(
                        stringResource(R.string.home_next_walk).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (next != null) {
                        Text(
                            stringResource(R.string.home_next_walk_in, formatDuration(context, next.at - state.now)),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            "${formatClock(context, next.at)} · ${next.label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(stringResource(R.string.home_next_walk_none), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier
                    .background(WalkiesColors.Ink.copy(alpha = 0.85f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_pee), contentDescription = null, tint = WalkiesColors.Pee, modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(6.dp))
                val lastPee = state.lastPeeAt
                Text(
                    if (lastPee != null) {
                        stringResource(R.string.home_last_pee, formatDuration(context, state.now - lastPee))
                    } else {
                        stringResource(R.string.home_no_pee_yet)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
            }
            if (state.loaded && state.photoUri == null) {
                Button(
                    onClick = onAddPhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = WalkiesColors.Ink),
                    shape = CircleShape,
                ) {
                    Icon(painterResource(R.drawable.ic_photo), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.home_add_photo, state.dogName), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun QuickLogGrid(walkRunning: Boolean, onLog: (LogType) -> Unit, onWalk: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile(LogType.PEE.label(), R.drawable.ic_pee, WalkiesColors.Pee, WalkiesColors.Ink, Modifier.weight(1f)) { onLog(LogType.PEE) }
            QuickTile(LogType.POO.label(), R.drawable.ic_poo, WalkiesColors.Poo, Color.White, Modifier.weight(1f)) { onLog(LogType.POO) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile(LogType.MEAL.label(), R.drawable.ic_meal, WalkiesColors.MealTint, WalkiesColors.Meal, Modifier.weight(1f)) { onLog(LogType.MEAL) }
            QuickTile(
                stringResource(if (walkRunning) R.string.home_resume_walk else R.string.start_walk),
                R.drawable.ic_paw,
                WalkiesColors.Walk,
                WalkiesColors.Ink,
                Modifier.weight(1f),
                onWalk,
            )
        }
    }
}

@Composable
private fun QuickTile(
    label: String,
    @DrawableRes icon: Int,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(24.dp),
        color = container,
        contentColor = content,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.size(12.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TodayCard(state: HomeState) {
    val context = LocalContext.current
    val pee = state.todayEntries.count { it.type == LogType.PEE }
    val poo = state.todayEntries.count { it.type == LogType.POO }
    SectionCard {
        SectionHeader(stringResource(R.string.home_today)) {
            Text(
                stringResource(R.string.home_today_summary, state.todayWalks.size, pee, poo),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        // Entries logged on a walk are summarised in that walk's row.
        val items: List<Any> = (state.todayEntries.filter { it.walkId == null } + state.todayWalks)
            .sortedByDescending { if (it is Walk) it.startTime else (it as LogEntry).timestamp }
        if (items.isEmpty()) {
            Text(
                stringResource(R.string.home_today_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            items.forEach { item ->
                when (item) {
                    is Walk -> WalkRow(item, state.todayEntries)
                    is LogEntry -> TimelineRow(
                        badge = { EventBadge(item.type.style()) },
                        title = item.type.label(),
                        subtitle = null,
                        time = formatClock(context, item.timestamp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WalkRow(walk: Walk, entries: List<LogEntry>) {
    val context = LocalContext.current
    val pee = entries.count { it.walkId == walk.id && it.type == LogType.PEE }
    val poo = entries.count { it.walkId == walk.id && it.type == LogType.POO }
    TimelineRow(
        badge = { EventBadge(walkStyle) },
        title = stringResource(R.string.event_walk),
        subtitle = stringResource(R.string.walk_summary, formatDuration(context, walk.durationMillis()), pee.toString(), poo.toString()),
        time = formatClock(context, walk.startTime),
    )
}

@Composable
fun TimelineRow(
    badge: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    time: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        badge()
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(time, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        trailing?.invoke()
    }
}
