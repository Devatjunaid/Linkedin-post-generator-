package com.sunarc.inverterlogger.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunarc.inverterlogger.ui.components.SunCard
import com.sunarc.inverterlogger.ui.theme.Palette

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Context.share(intent: Intent) = runCatching { startActivity(intent) }.onFailure { toast("No app available to share this file") }

fun Context.openUrl(url: String) = runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

/** A scrolling screen body with the standard side padding. */
@Composable
fun ScreenList(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun EmptyStateCard(icon: ImageVector, title: String, body: String, action: (@Composable () -> Unit)? = null) {
    SunCard(Modifier.fillMaxWidth(), padding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Palette.Sun, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, fontSize = 16.sp, color = Palette.Ink)
                Text(body, fontSize = 13.sp, color = Palette.InkSoft)
            }
        }
        if (action != null) {
            Spacer(Modifier.height(14.dp))
            action()
        }
    }
}

@Composable
fun BusyOverlay(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        CircularProgressIndicator(color = Palette.Sun, trackColor = Palette.Line)
        Spacer(Modifier.height(10.dp))
        Text(text, color = Palette.InkSoft, fontSize = 14.sp)
    }
}

/** Placeholder for the link to your hosted privacy policy (see PRIVACY_POLICY.md). */
const val PRIVACY_POLICY_URL = "https://YOUR-DOMAIN.example/sunarc/privacy"
