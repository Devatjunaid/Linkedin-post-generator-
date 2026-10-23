package com.driveready.testprep.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.driveready.testprep.R
import com.driveready.testprep.ads.NativeAdCard
import com.driveready.testprep.ads.NativeAdStyle
import com.driveready.testprep.ui.theme.Brand

/** Shown when Back is pressed on the home screen: Exit, Cancel and Rate us. */
@Composable
fun ExitDialog(onExit: () -> Unit, onCancel: () -> Unit, onRate: () -> Unit) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Brand.Surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandLogo(size = 64.dp)
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.exit_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.exit_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Brand.Muted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                NativeAdCard(style = NativeAdStyle.MEDIA)
                Spacer(Modifier.height(14.dp))
                AmberButton(
                    text = stringResource(R.string.exit_rate),
                    onClick = onRate,
                    icon = AppIcons.Star,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(stringResource(R.string.exit_cancel), onCancel, Modifier.weight(1f))
                    PrimaryButton(stringResource(R.string.exit_exit), onExit, Modifier.weight(1f))
                }
            }
        }
    }
}

/** Opens the app's Play Store page, falling back to the web listing. */
fun openPlayStore(context: Context) {
    val pkg = context.packageName
    val market = Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$pkg".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
