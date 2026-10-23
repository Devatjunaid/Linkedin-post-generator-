package com.walkies.dogwalk.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walkies.dogwalk.R
import com.walkies.dogwalk.data.LogType
import com.walkies.dogwalk.ui.theme.WalkiesColors

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(26.dp))
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        trailing?.invoke()
    }
}

data class EventStyle(@DrawableRes val icon: Int, val tint: Color, val deep: Color, val solid: Color)

fun LogType.style(): EventStyle = when (this) {
    LogType.PEE -> EventStyle(R.drawable.ic_pee, WalkiesColors.PeeTint, WalkiesColors.PeeDeep, WalkiesColors.Pee)
    LogType.POO -> EventStyle(R.drawable.ic_poo, WalkiesColors.PooTint, WalkiesColors.PooDeep, WalkiesColors.Poo)
    LogType.MEAL -> EventStyle(R.drawable.ic_meal, WalkiesColors.MealTint, WalkiesColors.Meal, WalkiesColors.Meal)
}

val walkStyle = EventStyle(R.drawable.ic_paw, WalkiesColors.WalkTint, WalkiesColors.WalkDeep, WalkiesColors.Walk)

@Composable
fun LogType.label(): String = stringResource(
    when (this) {
        LogType.PEE -> R.string.event_pee
        LogType.POO -> R.string.event_poo
        LogType.MEAL -> R.string.event_meal
    },
)

@Composable
fun EventBadge(style: EventStyle, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier
            .size(size)
            .background(style.tint, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(style.icon), contentDescription = null, tint = style.deep, modifier = Modifier.size(size * 0.5f))
    }
}

fun openPlayStoreListing(context: Context) {
    val pkg = context.packageName
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        openUrl(context, "https://play.google.com/store/apps/details?id=$pkg")
    }
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No browser installed; nothing sensible to do.
    }
}
