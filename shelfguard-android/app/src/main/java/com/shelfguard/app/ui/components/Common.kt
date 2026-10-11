package com.shelfguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shelfguard.app.domain.StockItem
import com.shelfguard.app.domain.StockStatus
import com.shelfguard.app.ui.theme.AppTheme
import com.shelfguard.app.util.Format

/** White card with a hairline border: the app's basic surface. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium, colors = colors, border = border) { content() }
    } else {
        Card(modifier = modifier, shape = MaterialTheme.shapes.medium, colors = colors, border = border) { content() }
    }
}

data class StatusStyle(val label: String, val color: Color, val container: Color)

@Composable
fun statusStyle(status: StockStatus): StatusStyle {
    val c = AppTheme.status
    return when (status) {
        StockStatus.EXPIRED -> StatusStyle("Expired", c.expired, c.expiredContainer)
        StockStatus.OUT_OF_STOCK -> StatusStyle("Out of stock", c.out, c.outContainer)
        StockStatus.EXPIRING_SOON -> StatusStyle("Expiring", c.expiring, c.expiringContainer)
        StockStatus.LOW_STOCK -> StatusStyle("Low stock", c.low, c.lowContainer)
        StockStatus.OK -> StatusStyle("In stock", c.ok, c.okContainer)
    }
}

@Composable
fun StatusBadge(text: String, color: Color, container: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Badge text for a product: days left when expiring, otherwise the status name. */
@Composable
fun ItemStatusBadge(item: StockItem, modifier: Modifier = Modifier) {
    val status = item.status
    if (status == StockStatus.OK) return
    val style = statusStyle(status)
    val text = if (status == StockStatus.EXPIRING_SOON && item.daysToExpiry != null) {
        Format.daysBadge(item.daysToExpiry)
    } else style.label
    StatusBadge(text, style.color, style.container, modifier)
}

/** Rounded-square avatar with the product's initials, tinted by category. */
@Composable
fun ProductAvatar(name: String, category: String, size: Dp = 44.dp) {
    val palette = listOf(
        Color(0xFFE3F0E8) to Color(0xFF1E6B47),
        Color(0xFFE6EDFB) to Color(0xFF1F4FBF),
        Color(0xFFFDF0E1) to Color(0xFF8A3F04),
        Color(0xFFF1E8FA) to Color(0xFF6B3FA0),
        Color(0xFFE2F3F4) to Color(0xFF0E6A70),
        Color(0xFFFBE9EF) to Color(0xFF9C2A55),
    )
    val (bg, fg) = palette[(category.lowercase().hashCode() and 0x7fffffff) % palette.size]
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            Format.initials(name),
            color = fg,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.34f).sp,
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = color)
        if (action != null && onAction != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/** A product row used by the inventory, dashboard and alerts lists. */
@Composable
fun StockRow(
    item: StockItem,
    subtitle: String,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(Format.quantity(item.sellableQty, item.product.unit), style = MaterialTheme.typography.titleSmall)
            ItemStatusBadge(item)
        }
    },
) {
    AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProductAvatar(item.product.name, item.product.category)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    item.product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing()
        }
    }
}

/** Optional supporting text for text fields. Written as an if-expression so the lambda is @Composable. */
fun supportingText(text: String?): (@Composable () -> Unit)? =
    if (text == null) null else { { Text(text) } }

/** Describes when a product next expires, for list subtitles. */
fun expirySubtitle(item: StockItem): String {
    val parts = mutableListOf<String>()
    if (item.product.category.isNotBlank()) parts += item.product.category
    when {
        item.hasExpired -> parts += "${item.expiredQty} ${item.product.unit} expired"
        item.nextExpiry != null -> parts += Format.expiry(item.nextExpiry)
        else -> parts += "No expiry"
    }
    return parts.joinToString(" · ")
}
