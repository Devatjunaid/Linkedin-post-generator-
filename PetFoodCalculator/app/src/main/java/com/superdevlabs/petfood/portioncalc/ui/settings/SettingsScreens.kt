package com.superdevlabs.petfood.portioncalc.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.superdevlabs.petfood.portioncalc.BuildConfig
import com.superdevlabs.petfood.portioncalc.R
import com.superdevlabs.petfood.portioncalc.ads.BannerAd
import com.superdevlabs.petfood.portioncalc.domain.WeightUnit
import com.superdevlabs.petfood.portioncalc.ui.AppIcon
import com.superdevlabs.petfood.portioncalc.ui.AppTopBar
import com.superdevlabs.petfood.portioncalc.ui.LocalAppContainer
import com.superdevlabs.petfood.portioncalc.ui.SectionCard
import com.superdevlabs.petfood.portioncalc.ui.openPlayStore
import com.superdevlabs.petfood.portioncalc.ui.shareApp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenPrivacy: () -> Unit) {
    val c = LocalAppContainer.current
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val unit by c.settings.weightUnit.collectAsStateWithLifecycle(initialValue = WeightUnit.KG)
    val privacyRequired by c.ads.privacyOptionsRequired.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Settings", onBack = onBack) },
        bottomBar = { BannerAd(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 4.dp, 16.dp, padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SectionCard {
                    Text("Weight unit", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeightUnit.entries.forEach { u ->
                            FilterChip(
                                selected = unit == u,
                                onClick = { scope.launch { c.settings.setWeightUnit(u) } },
                                label = { Text(if (u == WeightUnit.KG) "Kilograms (kg)" else "Pounds (lb)") },
                            )
                        }
                    }
                }
            }
            item {
                SectionCard(padding = PaddingValues(vertical = 6.dp)) {
                    SettingRow(R.drawable.ic_star, "Rate us", "Tell other pet owners what you think") { openPlayStore(context) }
                    SettingRow(R.drawable.ic_share, "Share app", "Send it to a friend with a dog or cat") { shareApp(context) }
                    if (privacyRequired && activity != null) {
                        SettingRow(R.drawable.ic_shield, "Ad privacy choices", "Change your consent for personalised ads") {
                            c.ads.showPrivacyOptions(activity)
                        }
                    }
                    SettingRow(R.drawable.ic_info, "Privacy policy", "What data the app uses and why", onOpenPrivacy)
                }
            }
            item {
                SectionCard {
                    Text("About the calculations", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Daily energy = resting energy (70 × body weight in kg^0.75) × a factor for life stage, neuter status and " +
                            "activity, following WSAVA and NRC guidance. Weight-loss plans use the target weight. Portions come " +
                            "from the calories on your food's label. Results are estimates for healthy pets and do not replace your vet.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Breed and food photos and breed descriptions come from Wikipedia / Wikimedia Commons under free licences " +
                            "(mostly CC BY-SA); open a breed guide to see each source and author.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon, null)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppIcon(R.drawable.ic_chevron, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Privacy policy", onBack = onBack) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            SectionCard {
                Text(PRIVACY_POLICY, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

const val PRIVACY_POLICY = """Pet Food Calculator: Privacy Policy
Publisher: Super Dev Labs · Contact: ELONOPO@GMAIL.COM

1. Data you enter
Pet names, breeds, ages, weights, foods, feeding logs, weight history, meal times and optional pet photos are stored only on your device in the app's private storage. We do not run servers and do not receive this data. Android may include it in your own device backup if backups are on.

2. Photos
If you choose your own pet photo, Android's photo picker gives the app only that one picture, and a copy is kept in the app's private storage. No storage or camera permission is used.

3. Breed and food images
To show real breed and food photos and short breed descriptions, the app requests public information from Wikipedia (Wikimedia Foundation). These requests contain only the article name and standard network data such as your IP address. See the Wikimedia privacy policy at foundation.wikimedia.org/wiki/Privacy_policy.

4. Advertising
The app shows ads from Google AdMob. Google may collect and use device identifiers such as the advertising ID, IP address, approximate location from IP, and app interaction data to deliver, measure and personalise ads and to prevent fraud. In the EEA, UK and Switzerland the app asks for your consent through Google's consent form, and you can change it any time in Settings › Ad privacy choices. You can reset or delete your advertising ID in Android settings. See policies.google.com/technologies/partner-sites.

5. Notifications
Meal reminders are scheduled on your device. Notification permission is requested only when you turn reminders on.

6. Children
The app is meant for adult pet owners and is not directed at children under 13.

7. Data deletion
Delete a pet in the app to remove its data, or uninstall the app (or clear its storage) to remove everything.

8. Health disclaimer
Results are estimates for healthy pets and are not veterinary advice.

9. Changes
We may update this policy; the latest version is always shown in the app and on the store listing.

Questions: ELONOPO@GMAIL.COM"""
