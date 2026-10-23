package com.walkies.dogwalk.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walkies.dogwalk.AppContainer
import com.walkies.dogwalk.BuildConfig
import com.walkies.dogwalk.R
import com.walkies.dogwalk.data.Settings
import com.walkies.dogwalk.ui.components.DogPhoto
import com.walkies.dogwalk.ui.components.SectionCard
import com.walkies.dogwalk.ui.components.openPlayStoreListing
import com.walkies.dogwalk.ui.components.openUrl
import com.walkies.dogwalk.ui.components.rememberDogPhotoPicker
import com.walkies.dogwalk.ui.theme.WalkiesColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val GOALS = listOf(15, 20, 30, 45, 60)

@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val privacyRequired by container.ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val pickPhoto = rememberDogPhotoPicker { uri -> scope.launch { container.settings.setDogPhoto(uri.toString()) } }

    var name by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        if (name == null) name = container.settings.settings.first().dogName
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(12.dp))

        val s: Settings = settings ?: return@Column
        SectionCard {
            Text(stringResource(R.string.settings_dog), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DogPhoto(
                    s.dogPhotoUri,
                    contentDescription = s.dogName,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .border(3.dp, WalkiesColors.WalkTint, CircleShape),
                )
                Spacer(Modifier.size(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(onClick = pickPhoto) {
                        Icon(painterResource(R.drawable.ic_photo), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.settings_change_photo))
                    }
                    if (s.dogPhotoUri != null) {
                        TextButton(onClick = { scope.launch { container.settings.setDogPhoto(null) } }) {
                            Text(stringResource(R.string.settings_remove_photo))
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = name ?: s.dogName,
                onValueChange = { value ->
                    val trimmed = value.take(24)
                    name = trimmed
                    scope.launch { container.settings.setDogName(trimmed) }
                },
                label = { Text(stringResource(R.string.settings_dog_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.settings_walk_goal), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GOALS.forEach { g ->
                    FilterChip(
                        selected = s.walkGoalMinutes == g,
                        onClick = { scope.launch { container.settings.setWalkGoal(g) } },
                        label = { Text(stringResource(R.string.reminder_duration, g)) },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SectionCard(contentPadding = 6.dp) {
            if (privacyRequired && activity != null) {
                SettingsRow(R.drawable.ic_shield, stringResource(R.string.settings_privacy_options)) {
                    container.ads.showPrivacyOptions(activity)
                }
            }
            SettingsRow(R.drawable.ic_info, stringResource(R.string.settings_privacy_policy)) {
                openUrl(context, context.getString(R.string.privacy_policy_url))
            }
            SettingsRow(R.drawable.ic_star, stringResource(R.string.settings_rate)) {
                openPlayStoreListing(context)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.settings_ads_note) + "  " + stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsRow(@DrawableRes icon: Int, title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(14.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
