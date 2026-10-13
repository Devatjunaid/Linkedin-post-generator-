package com.odomate.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odomate.app.domain.Level
import com.odomate.app.domain.ServiceStatus
import com.odomate.app.ui.theme.AppTheme

/** Card-style row with a progress meter (Home "Up next"). */
@Composable
fun ServiceCard(status: ServiceStatus, unit: String, onClick: () -> Unit) {
    val c = AppTheme.colors
    val color = c.levelColor(status.level)
    AppCard(onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ServiceIconBadge(status.type.iconKey, status.level)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        status.type.name, color = c.text, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (status.level == Level.OVERDUE) "OVERDUE" else statusShort(status, unit).uppercase(),
                        color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                }
                if (status.level != Level.UNKNOWN && status.level != Level.DISABLED) {
                    LinearMeter(status.progress, color, height = 5.dp)
                }
                Text(statusDetail(status, unit), color = c.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Compact list row (Services list). */
@Composable
fun ServiceListRow(status: ServiceStatus, unit: String, onClick: () -> Unit, showDivider: Boolean) {
    val c = AppTheme.colors
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ServiceIconBadge(status.type.iconKey, status.level, size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(status.type.name, color = c.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (status.level == Level.OK || status.level == Level.DISABLED || status.level == Level.UNKNOWN)
                        intervalText(status.type.intervalDistance, status.type.intervalMonths, unit)
                    else statusDetail(status, unit),
                    color = c.muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(statusShort(status, unit), color = c.levelColor(status.level), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                val sub = when (status.level) {
                    Level.OVERDUE -> "overdue"
                    Level.SOON, Level.OK -> "left"
                    else -> ""
                }
                if (sub.isNotEmpty()) Text(sub, color = c.levelColor(status.level), fontSize = 11.sp)
            }
        }
        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(color = c.divider, modifier = Modifier.padding(horizontal = 14.dp))
        }
    }
}
