package com.odomate.app.ui.exit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.odomate.app.ads.NativeAdCard
import com.odomate.app.ads.NativeAdSize
import com.odomate.app.ui.components.LogoMark
import com.odomate.app.ui.components.PrimaryButton
import com.odomate.app.ui.components.SecondaryButton
import com.odomate.app.ui.theme.AppTheme

/** Exit confirmation with a medium native ad and a nudge about anything that needs attention. */
@Composable
fun ExitDialog(
    attentionCount: Int,
    onStay: () -> Unit,
    onExit: () -> Unit,
    onRate: () -> Unit,
) {
    val c = AppTheme.colors
    Dialog(onDismissRequest = onStay, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(26.dp),
            color = c.bg,
            border = BorderStroke(1.dp, c.outline),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LogoMark(40.dp)
                    Column {
                        Text("Leaving already?", style = MaterialTheme.typography.titleLarge, color = c.text)
                        Text(
                            when (attentionCount) {
                                0 -> "Your car is all caught up. Drive safe!"
                                1 -> "1 service needs attention. Check it before you go?"
                                else -> "$attentionCount services need attention. Check them before you go?"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (attentionCount > 0) c.warn else c.muted,
                        )
                    }
                }
                NativeAdCard(size = NativeAdSize.MEDIUM)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Exit", onExit, Modifier.weight(1f), icon = Icons.Outlined.PowerSettingsNew)
                    PrimaryButton("Stay", onStay, Modifier.weight(1f))
                }
                TextButton(onClick = onRate, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Outlined.StarOutline, null, tint = c.accentText)
                    Text("  Enjoying OdoMate? Rate us", color = c.accentText)
                }
            }
        }
    }
}
