package com.walkies.dogwalk.ui.walk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.walkies.dogwalk.AppContainer
import com.walkies.dogwalk.R
import com.walkies.dogwalk.ads.NativeAdCache
import com.walkies.dogwalk.ads.NativeAdCard
import com.walkies.dogwalk.data.LogEntry
import com.walkies.dogwalk.data.LogType
import com.walkies.dogwalk.data.durationMillis
import com.walkies.dogwalk.ui.components.DogPhoto
import com.walkies.dogwalk.ui.components.EventBadge
import com.walkies.dogwalk.ui.components.SectionCard
import com.walkies.dogwalk.ui.components.SectionHeader
import com.walkies.dogwalk.ui.components.label
import com.walkies.dogwalk.ui.components.style
import com.walkies.dogwalk.ui.components.walkStyle
import com.walkies.dogwalk.ui.containerFactory
import com.walkies.dogwalk.ui.home.TimelineRow
import com.walkies.dogwalk.ui.theme.TimerTextStyle
import com.walkies.dogwalk.ui.theme.WalkiesColors
import com.walkies.dogwalk.util.formatClock
import com.walkies.dogwalk.util.formatDay
import com.walkies.dogwalk.util.formatDuration
import com.walkies.dogwalk.util.formatStopwatch
import com.walkies.dogwalk.util.toLocalDate
import com.walkies.dogwalk.walk.StepCounter
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun WalkScreen(
    container: AppContainer,
    onWalkSaved: () -> Unit,
    onLogged: (LogEntry, undo: () -> Unit) -> Unit,
    viewModel: WalkViewModel = viewModel(factory = containerFactory { WalkViewModel(it) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    val needsStepPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    fun hasStepPermission() = !needsStepPermission ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    val stepPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onStepPermission(granted)
    }
    LaunchedEffect(Unit) { viewModel.onStepPermission(hasStepPermission()) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        val active = state.active
        if (active == null) {
            Text(stringResource(R.string.nav_walk), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            ReadyCard(
                dogName = state.dogName,
                photoUri = state.photoUri,
                onStart = {
                    if (viewModel.stepCounterAvailable && !hasStepPermission()) {
                        stepPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                    viewModel.start()
                },
            )
            Spacer(Modifier.height(18.dp))
            NativeAdCard(container.ads, NativeAdCache.WALK)
            Spacer(Modifier.height(18.dp))
            RecentWalks(state.recent)
        } else {
            var showDiscard by rememberSaveable { mutableStateOf(false) }
            Text(stringResource(R.string.walk_in_progress, state.dogName), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            ActiveWalkCard(
                state = state,
                stepCounterAvailable = viewModel.stepCounterAvailable,
                onRequestSteps = { stepPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                onLog = { type -> viewModel.log(type) { entry -> onLogged(entry) { viewModel.undo(entry) } } },
                onDiscard = { showDiscard = true },
                onFinish = {
                    viewModel.finish {
                        // A finished walk is a natural break: show an interstitial if the cap allows.
                        val host = activity
                        if (host != null) container.ads.interstitials.showIfAllowed(host, onWalkSaved) else onWalkSaved()
                    }
                },
            )
            Spacer(Modifier.height(18.dp))
            SectionCard {
                SectionHeader(stringResource(R.string.walk_events))
                Spacer(Modifier.height(6.dp))
                if (state.events.isEmpty()) {
                    Text(
                        stringResource(R.string.walk_events_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
                state.events.forEach { e ->
                    TimelineRow(
                        badge = { EventBadge(e.type.style()) },
                        title = e.type.label(),
                        subtitle = null,
                        time = formatClock(context, e.timestamp),
                    )
                }
            }
            if (showDiscard) {
                AlertDialog(
                    onDismissRequest = { showDiscard = false },
                    title = { Text(stringResource(R.string.walk_cancel_title)) },
                    text = { Text(stringResource(R.string.walk_cancel_body)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showDiscard = false
                            viewModel.discard()
                        }) { Text(stringResource(R.string.discard)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDiscard = false }) { Text(stringResource(R.string.keep_walking)) }
                    },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ReadyCard(dogName: String, photoUri: String?, onStart: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(30.dp)),
    ) {
        DogPhoto(photoUri, contentDescription = dogName, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
        ) {
            Text(stringResource(R.string.walk_ready_title), style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.walk_ready_body, dogName),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WalkiesColors.Walk, contentColor = WalkiesColors.Ink),
            ) {
                Icon(painterResource(R.drawable.ic_paw), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.start_walk))
            }
        }
    }
}

@Composable
private fun ActiveWalkCard(
    state: WalkState,
    stepCounterAvailable: Boolean,
    onRequestSteps: () -> Unit,
    onLog: (LogType) -> Unit,
    onDiscard: () -> Unit,
    onFinish: () -> Unit,
) {
    val walk = state.active ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(walk.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val elapsed = walk.durationMillis(now)
    val goalMillis = state.goalMinutes * 60_000L
    val pee = state.events.count { it.type == LogType.PEE }
    val poo = state.events.count { it.type == LogType.POO }

    SectionCard(contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatStopwatch(elapsed), style = TimerTextStyle, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.walk_goal, state.goalMinutes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (elapsed.toFloat() / goalMillis).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = WalkiesColors.Walk,
            trackColor = MaterialTheme.colorScheme.background,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(12.dp))
        StepsLine(state.steps, stepCounterAvailable, onRequestSteps)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WalkActionButton(
                label = stringResource(R.string.walk_pee_here),
                count = pee,
                icon = R.drawable.ic_pee,
                container = WalkiesColors.Pee,
                content = WalkiesColors.Ink,
                modifier = Modifier.weight(1f),
            ) { onLog(LogType.PEE) }
            WalkActionButton(
                label = stringResource(R.string.walk_poo_here),
                count = poo,
                icon = R.drawable.ic_poo,
                container = WalkiesColors.Poo,
                content = Color.White,
                modifier = Modifier.weight(1f),
            ) { onLog(LogType.POO) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            ) { Text(stringResource(R.string.walk_cancel)) }
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .weight(2f)
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WalkiesColors.Ink, contentColor = Color.White),
            ) {
                Icon(painterResource(R.drawable.ic_stop), contentDescription = null, tint = WalkiesColors.Walk, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.walk_end))
            }
        }
    }
}

@Composable
private fun StepsLine(steps: Long?, available: Boolean, onRequestSteps: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_steps),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(8.dp))
        when {
            !available -> Text(
                stringResource(R.string.walk_steps_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            steps == null -> TextButton(onClick = onRequestSteps) { Text(stringResource(R.string.walk_steps_off)) }
            else -> {
                val km = steps * StepCounter.METERS_PER_STEP / 1000.0
                Text(
                    stringResource(R.string.walk_steps, NumberFormat.getIntegerInstance().format(steps)) +
                        "  ·  " + stringResource(R.string.walk_distance, String.format(Locale.getDefault(), "%.2f", km)),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun WalkActionButton(
    label: String,
    count: Int,
    @DrawableRes icon: Int,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(24.dp),
        color = container,
        contentColor = content,
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.size(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.walk_count_so_far, count), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun RecentWalks(recent: List<WalkSummary>) {
    val context = LocalContext.current
    SectionCard {
        SectionHeader(stringResource(R.string.walk_recent))
        Spacer(Modifier.height(6.dp))
        if (recent.isEmpty()) {
            Text(
                stringResource(R.string.walk_recent_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
        recent.forEach { item ->
            val w = item.walk
            TimelineRow(
                badge = { EventBadge(walkStyle) },
                title = formatDay(w.startTime.toLocalDate()),
                subtitle = stringResource(
                    R.string.walk_summary,
                    formatDuration(context, w.durationMillis()),
                    item.pee.toString(),
                    item.poo.toString(),
                ),
                time = formatClock(context, w.startTime),
            )
        }
    }
}
