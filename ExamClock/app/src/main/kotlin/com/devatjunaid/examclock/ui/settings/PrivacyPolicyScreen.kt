package com.devatjunaid.examclock.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devatjunaid.examclock.R
import com.devatjunaid.examclock.ui.components.ScreenScaffold

/** The same policy text as PRIVACY_POLICY.md in the repository, readable offline in the app. */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val sections = listOf(
        R.string.privacy_s1_title to R.string.privacy_s1_body,
        R.string.privacy_s2_title to R.string.privacy_s2_body,
        R.string.privacy_s3_title to R.string.privacy_s3_body,
        R.string.privacy_s4_title to R.string.privacy_s4_body,
        R.string.privacy_s5_title to R.string.privacy_s5_body,
        R.string.privacy_s6_title to R.string.privacy_s6_body,
    )
    ScreenScaffold(title = stringResource(R.string.settings_privacy_policy), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.privacy_intro), style = MaterialTheme.typography.bodyMedium)
            sections.forEach { (title, body) ->
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
