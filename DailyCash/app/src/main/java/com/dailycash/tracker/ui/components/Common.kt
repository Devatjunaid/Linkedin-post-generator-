package com.dailycash.tracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dailycash.tracker.ads.BannerAd
import com.dailycash.tracker.data.CashEntry
import com.dailycash.tracker.data.Categories
import com.dailycash.tracker.data.Category
import com.dailycash.tracker.data.CountryDetector
import com.dailycash.tracker.data.Money
import com.dailycash.tracker.data.Rates
import com.dailycash.tracker.data.Stats
import com.dailycash.tracker.data.isSpend
import com.dailycash.tracker.ui.theme.Mono
import com.dailycash.tracker.ui.theme.Palette
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Tab(val label: String, val icon: ImageVector) {
    WALLET("Wallet", Icons.Rounded.AccountBalanceWallet),
    ATLAS("Atlas", Icons.Rounded.Map),
    INSIGHTS("Insights", Icons.Rounded.BarChart),
    HISTORY("History", Icons.AutoMirrored.Rounded.ReceiptLong),
}

/** Screen shell for the tab screens: content, the floating dock, and the banner under it. */
@Composable
fun MainFrame(
    current: Tab,
    onTab: (Tab) -> Unit,
    onAdd: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Palette.Ink).navigationBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        Dock(current, onTab, onAdd)
        Spacer(Modifier.height(10.dp))
        BannerAd()
    }
}

@Composable
private fun Dock(current: Tab, onTab: (Tab) -> Unit, onAdd: () -> Unit) {
    Surface(
        color = Palette.Surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .border(1.dp, Palette.Line, RoundedCornerShape(24.dp)),
    ) {
        Row(
            Modifier.height(68.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            DockItem(Tab.WALLET, current, onTab)
            DockItem(Tab.ATLAS, current, onTab)
            AddButton(onAdd)
            DockItem(Tab.INSIGHTS, current, onTab)
            DockItem(Tab.HISTORY, current, onTab)
        }
    }
}

@Composable
private fun RowScope.DockItem(tab: Tab, current: Tab, onTab: (Tab) -> Unit) {
    val active = tab == current
    val scale by animateFloatAsState(if (active) 1.12f else 1f, tween(300), label = "dock")
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .semantics { selected = active }
            .clickable(role = Role.Tab) { onTab(tab) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(tab.icon, contentDescription = null, tint = if (active) Palette.Mint else Palette.Muted, modifier = Modifier.scale(scale))
        Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) Palette.Mint else Palette.Muted)
    }
}

@Composable
private fun AddButton(onAdd: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "fab")
    val loop = rememberInfiniteTransition(label = "fab")
    val glow by loop.animateFloat(0.25f, 0.55f, infiniteRepeatable(tween(1400), androidx.compose.animation.core.RepeatMode.Reverse), label = "glow")
    Box(
        Modifier
            .offset(y = (-14).dp)
            .size(62.dp)
            .scale(scale)
            .background(Palette.Mint.copy(alpha = glow * 0.35f), RoundedCornerShape(22.dp))
            .padding(3.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Mint)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClickLabel = "Add a cash entry", onClick = onAdd),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "Add a cash entry", tint = Palette.Ink, modifier = Modifier.size(30.dp))
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold), color = Palette.Text, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                color = Palette.Mint,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
fun CategoryBadge(category: Category, size: Dp = 42.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(category.group.tile),
        contentAlignment = Alignment.Center,
    ) {
        Icon(category.icon, contentDescription = null, tint = category.group.tint, modifier = Modifier.size(size * 0.52f))
    }
}

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

@Composable
fun EntryRow(
    entry: CashEntry,
    home: String,
    rates: Rates,
    modifier: Modifier = Modifier,
    showDate: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val cat = Categories.resolve(entry.categoryId, entry.label)
    val time = Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault())
    val whenText = if (showDate) DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault()).format(time) else timeFmt.format(time)
    val sign = if (entry.isSpend()) -1 else 1
    val homeValue = if (entry.currency != home) Stats.homeValue(entry, home, rates) else null
    Row(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(cat)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.label, color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            val meta = buildString {
                append(cat.group.title.substringBefore(" &"))
                append(" · ").append(whenText)
                append(" · ").append(CountryDetector.countryName(entry.countryCode))
                if (entry.note.isNotBlank()) append(" · ").append(entry.note)
            }
            Text(meta, color = Palette.Muted, fontSize = 12.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Money.format(sign * entry.amountMinor, entry.currency, forceSign = true),
                color = if (entry.isSpend()) Palette.Text else Palette.Mint,
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            if (homeValue != null) {
                Text("≈ " + Money.format(homeValue, home), color = Palette.Muted, fontSize = 11.sp)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
        }
    }
}

@Composable
fun PulseDot(color: Color = Palette.Mint, size: Dp = 8.dp) {
    val loop = rememberInfiniteTransition(label = "pulse")
    val t by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing)), label = "t")
    Canvas(Modifier.size(size * 2.6f)) {
        val r = size.toPx() / 2f
        drawCircle(color.copy(alpha = (1f - t) * 0.5f), r + r * 1.6f * t)
        drawCircle(color, r)
    }
}

/** Money text that rolls up/down when the value changes. */
@Composable
fun RollingText(text: String, style: TextStyle, color: Color = Palette.Text, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(220)))
                .togetherWith(slideOutVertically(tween(260)) { -it / 2 } + fadeOut(tween(200)))
        },
        label = "rolling",
        modifier = modifier,
    ) { value ->
        Text(value, style = style, color = color, maxLines = 1)
    }
}

@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Line, RoundedCornerShape(14.dp))
            .padding(3.dp),
    ) {
        options.forEach { o ->
            val active = o == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (active) Palette.Text else Color.Transparent)
                    .semantics { this.selected = active }
                    .clickable(role = Role.Tab) { onSelect(o) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), color = if (active) Palette.Ink else Palette.Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, mono: Boolean = false) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Palette.Mint else Palette.Surface)
            .border(1.dp, if (selected) Palette.Mint else Palette.Line, RoundedCornerShape(12.dp))
            .semantics { this.selected = selected }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) Palette.Ink else Palette.Muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            color = if (selected) Palette.Ink else Palette.Text,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            fontFamily = if (mono) Mono else null,
        )
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = Palette.Surface, border: Color = Palette.Line, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) { content() }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Palette.Surface), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Palette.Mint, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(body, color = Palette.Muted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leading: ImageVector? = null,
    imeAction: ImeAction = ImeAction.Done,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        leadingIcon = if (leading != null) { { Icon(leading, contentDescription = null) } } else null,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Palette.Mint,
            unfocusedBorderColor = Palette.Line,
            focusedLabelColor = Palette.Mint,
            unfocusedLabelColor = Palette.Muted,
            cursorColor = Palette.Mint,
            focusedContainerColor = Palette.Surface,
            unfocusedContainerColor = Palette.Surface,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Searchable list of every ISO currency. */
@Composable
fun CurrencyPickerDialog(title: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val all = remember { Money.allCurrencies().map { it to Money.displayName(it) } }
    val shown = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) all else all.filter { it.first.contains(q, true) || it.second.contains(q, true) }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = Palette.Surface, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().height(560.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text(title, color = Palette.Text, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                AppTextField(query, { query = it }, "Search currency", leading = Icons.Rounded.Search, imeAction = ImeAction.Search)
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(shown, key = { it.first }) { (code, name) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button) { onPick(code) }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(code, fontFamily = Mono, fontWeight = FontWeight.Bold, color = Palette.Mint, modifier = Modifier.width(52.dp))
                            Text(name, color = Palette.Text, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(Money.symbol(code).trim(), color = Palette.Muted)
                        }
                    }
                }
            }
        }
    }
}
