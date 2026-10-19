package com.dailycash.tracker.ui.screens

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailycash.tracker.ads.InterstitialAds
import com.dailycash.tracker.data.CashEntry
import com.dailycash.tracker.data.Categories
import com.dailycash.tracker.data.Category
import com.dailycash.tracker.data.Denominations
import com.dailycash.tracker.data.Money
import com.dailycash.tracker.data.Stats
import com.dailycash.tracker.ui.MainViewModel
import com.dailycash.tracker.ui.components.AppTextField
import com.dailycash.tracker.ui.components.Chip
import com.dailycash.tracker.ui.components.CurrencyPickerDialog
import com.dailycash.tracker.ui.components.RollingText
import com.dailycash.tracker.ui.components.SectionHeader
import com.dailycash.tracker.ui.components.Segmented
import com.dailycash.tracker.ui.theme.Mono
import com.dailycash.tracker.ui.theme.Palette

private enum class Input(val label: String) { NOTES("Notes & coins"), KEYPAD("Keypad") }

private val defaultQuick = listOf("restaurant", "groceries", "coffee", "taxi", "street_food", "fuel", "pharmacy", "shopping")

@Composable
fun AddScreen(vm: MainViewModel, initialMode: String, initialCurrency: String?, onDone: () -> Unit) {
    val activity = LocalActivity.current
    val haptics = LocalHapticFeedback.current
    val entries by vm.entries.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    val custom by vm.customCategories.collectAsStateWithLifecycle()
    val home by vm.homeCurrency.collectAsStateWithLifecycle()
    val rates by vm.rates.collectAsStateWithLifecycle()
    val last by vm.lastCurrency.collectAsStateWithLifecycle()

    var spending by rememberSaveable { mutableStateOf(initialMode != "in") }
    var currency by rememberSaveable { mutableStateOf(initialCurrency ?: last ?: vm.place.currency) }
    val denoms = Denominations.forCurrency(currency)
    var input by rememberSaveable(currency) { mutableStateOf(if (denoms != null) Input.NOTES else Input.KEYPAD) }
    var tray by rememberSaveable(currency) { mutableStateOf(listOf<Long>()) }
    var typed by rememberSaveable(currency) { mutableStateOf("") }
    var changeBack by rememberSaveable(currency) { mutableStateOf(false) }
    var catId by rememberSaveable { mutableStateOf<String?>(null) }
    var catName by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var showSheet by remember { mutableStateOf(false) }
    var showCurrencies by remember { mutableStateOf(false) }

    val amount = when (input) {
        Input.NOTES -> tray.sum().coerceAtLeast(0)
        Input.KEYPAD -> Money.parse(typed, currency) ?: 0L
    }
    val category: Category? = catId?.let { Categories.resolve(it, catName) }
    val balance = balances.firstOrNull { it.currency == currency }?.balance ?: 0L
    val goesNegative = spending && amount > 0 && amount > balance

    val currencyChoices = remember(balances, currency, home) {
        (listOf(currency, vm.place.currency, home) + balances.map { it.currency }).distinct().take(6)
    }
    val quick = remember(entries, custom) {
        val fav = Stats.favouriteCategoryIds(entries).map { Categories.resolve(it.first, it.second) }
        val defaults = defaultQuick.map { id -> Categories.spend.first { it.id == id } }
        (fav + defaults).distinctBy { it.id }.take(8)
    }

    fun pick(c: Category) {
        catId = c.id
        catName = c.name
    }

    if (showSheet) {
        CategorySheet(
            selectedId = catId,
            custom = custom,
            onPick = { pick(it); showSheet = false },
            onCreate = { name -> vm.addCustomCategory(name)?.let { pick(it) }; showSheet = false },
            onDismiss = { showSheet = false },
        )
    }
    if (showCurrencies) {
        CurrencyPickerDialog(
            title = "Which currency?",
            onPick = { currency = it; showCurrencies = false },
            onDismiss = { showCurrencies = false },
        )
    }

    Column(Modifier.fillMaxSize().background(Palette.Ink).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Palette.Text) }
            Text(
                if (spending) "Count it like cash" else "Add cash to a wallet",
                color = Palette.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    if (input == Input.NOTES) tray = tray.dropLast(1) else typed = typed.dropLast(1)
                },
                enabled = tray.isNotEmpty() || typed.isNotEmpty(),
            ) { Icon(Icons.AutoMirrored.Rounded.Undo, "Undo last", tint = Palette.Text) }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Segmented(
                options = listOf(true, false),
                selected = spending,
                label = { if (it) "I spent" else "I got cash" },
                onSelect = { spending = it; catId = null },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(14.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                currencyChoices.forEach { code ->
                    Chip(code, code == currency, onClick = { currency = code }, mono = true)
                }
                Chip("More", false, onClick = { showCurrencies = true }, icon = Icons.Rounded.Apps)
            }

            // Amount
            Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when {
                        !spending -> "ADDING TO YOUR $currency WALLET"
                        changeBack -> "TAP THE CHANGE YOU GOT BACK"
                        else -> "TAP WHAT YOU HANDED OVER"
                    },
                    color = if (changeBack) Palette.Amber else Palette.Mint,
                    fontSize = 12.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                RollingText(
                    Money.formatExact(amount, currency),
                    style = TextStyle(fontSize = 50.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp),
                )
                val homeValue = rates.convert(amount, currency, home)
                if (currency != home) {
                    Text(
                        buildString {
                            append(if (homeValue != null) "≈ " + Money.format(homeValue, home) else "No rate for $currency")
                            rates.describe(currency, home)?.let { append(" · ").append(it) }
                            if (!rates.live) append(" · estimate")
                        },
                        color = Palette.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Palette.Surface)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                AnimatedVisibility(goesNegative, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, null, tint = Palette.Amber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "More than the ${Money.format(balance, currency)} in this wallet. It will show as overspent.",
                            color = Palette.Amber,
                            fontSize = 12.sp,
                        )
                    }
                }
                if (input == Input.NOTES && tray.isNotEmpty()) {
                    FlowRow(
                        Modifier.padding(top = 10.dp).animateContentSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        tray.forEach { v ->
                            Text(
                                (if (v < 0) "−" else "+") + Money.format(kotlin.math.abs(v), currency),
                                color = Palette.Ink,
                                fontFamily = Mono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (v < 0) Palette.Amber else Palette.Mint)
                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Segmented(
                    options = if (denoms != null) Input.entries.toList() else listOf(Input.KEYPAD),
                    selected = input,
                    label = { it.label },
                    onSelect = { input = it },
                )
                Spacer(Modifier.weight(1f))
                if (input == Input.NOTES && spending) {
                    Chip(
                        if (changeBack) "Done" else "Got change?",
                        changeBack,
                        onClick = { changeBack = !changeBack },
                        icon = Icons.Rounded.SwapVert,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (input == Input.NOTES && denoms != null) {
                val sign = if (changeBack) -1 else 1
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2,
                ) {
                    denoms.notes.forEachIndexed { i, d ->
                        val minor = Denominations.toMinor(d, currency)
                        BillTile(
                            label = Money.format(minor, currency),
                            colors = Palette.notes[i % Palette.notes.size],
                            modifier = Modifier.weight(1f),
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tray = tray + sign * minor
                        }
                    }
                }
                if (denoms.coins.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        denoms.coins.forEachIndexed { i, d ->
                            val minor = Denominations.toMinor(d, currency)
                            CoinTile(Money.format(minor, currency), coinColors[i % coinColors.size]) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tray = tray + sign * minor
                            }
                        }
                    }
                }
            } else {
                Keypad(
                    allowDecimal = Money.digits(currency) > 0,
                    onKey = { k -> typed = applyKey(typed, k, Money.digits(currency)) },
                )
            }

            Spacer(Modifier.height(20.dp))
            if (spending) {
                SectionHeader("What was it for?", action = "All 60 + your own", onAction = { showSheet = true })
                Spacer(Modifier.height(6.dp))
                val shown = if (category != null && quick.none { it.id == category.id }) listOf(category) + quick.take(7) else quick
                FlowRow(
                    Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    shown.forEach { c ->
                        CategoryTile(c, c.id == catId, onClick = { pick(c) }, modifier = Modifier.weight(1f))
                    }
                }
            } else {
                SectionHeader("Where did it come from?")
                Spacer(Modifier.height(6.dp))
                FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = 3, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Categories.income.forEach { c ->
                        CategoryTile(c, c.id == catId, onClick = { pick(c) }, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            AppTextField(
                note,
                { if (it.length <= 80) note = it },
                "Note (optional)",
                placeholder = if (spending) "e.g. Lunch with Sam" else "e.g. Airport exchange",
            )
            Text(
                "Saved with today's date, ${vm.place.countryName} and the current rate.",
                color = Palette.Faint,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )
        }

        val ready = amount > 0 && category != null
        Button(
            onClick = {
                val c = category ?: return@Button
                vm.save(if (spending) CashEntry.SPEND else CashEntry.ADD, amount, currency, c, note)
                InterstitialAds.showThen(activity, onDone)
            },
            enabled = ready,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Palette.Mint,
                contentColor = Palette.Ink,
                disabledContainerColor = Palette.Raised,
                disabledContentColor = Palette.Muted,
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth().height(58.dp),
        ) {
            Text(
                when {
                    amount <= 0 -> "Enter an amount"
                    category == null -> if (spending) "Pick what it was for" else "Pick where it came from"
                    spending -> "Drop ${Money.format(amount, currency)} from wallet"
                    else -> "Add ${Money.format(amount, currency)} to wallet"
                },
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
            )
        }
    }
}

private val coinColors = listOf(
    Color(0xFFE8C873) to Color(0xFFA8873A),
    Color(0xFFD9DDE0) to Color(0xFF8E979C),
    Color(0xFFE0A36B) to Color(0xFF9B6635),
    Color(0xFFC9CED1) to Color(0xFF7C858A),
)

@Composable
private fun BillTile(label: String, colors: Palette.NoteColors, modifier: Modifier = Modifier, onTap: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.94f else 1f, label = "bill")
    Box(
        modifier
            .height(64.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bg)
            .border(1.dp, colors.line, RoundedCornerShape(12.dp))
            .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), role = Role.Button, onClickLabel = "Add $label note", onClick = onTap),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = size.width / 6f
            for (k in 0..2) {
                val y = size.height * (0.35f + k * 0.15f)
                val p = Path().apply {
                    moveTo(0f, y)
                    var x = 0f
                    var up = true
                    while (x < size.width) {
                        quadraticTo(x + step / 2f, y + if (up) -10f else 10f, x + step, y)
                        x += step
                        up = !up
                    }
                }
                drawPath(p, colors.line.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))
            }
            drawCircle(colors.line, size.height * 0.24f, Offset(size.width - size.height * 0.45f, size.height / 2f), style = Stroke(1.dp.toPx()))
        }
        Text(
            label,
            color = Palette.Text,
            fontSize = 21.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 14.dp),
        )
    }
}

@Composable
private fun CoinTile(label: String, colors: Pair<Color, Color>, onTap: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.86f else 1f, label = "coin")
    Box(
        Modifier
            .size(66.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(colors.first)
            .border(3.dp, colors.second, CircleShape)
            .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), role = Role.Button, onClickLabel = "Add $label coin", onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Palette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun Keypad(allowDecimal: Boolean, onKey: (Char) -> Unit) {
    val rows = listOf("123", "456", "789", (if (allowDecimal) "." else " ") + "0<")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (k == ' ') Color.Transparent else Palette.Surface)
                            .then(if (k == ' ') Modifier else Modifier.clickable(role = Role.Button) { onKey(k) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (k) {
                            '<' -> Icon(Icons.AutoMirrored.Rounded.Backspace, "Delete", tint = Palette.Text)
                            ' ' -> Unit
                            else -> Text(k.toString(), color = Palette.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** Keypad text rules: one decimal point, no more decimals than the currency has, sane length. */
private fun applyKey(current: String, key: Char, digits: Int): String = when (key) {
    '<' -> current.dropLast(1)
    '.' -> when {
        digits == 0 || current.contains('.') -> current
        current.isEmpty() -> "0."
        else -> "$current."
    }
    else -> {
        val dot = current.indexOf('.')
        when {
            dot >= 0 && current.length - dot - 1 >= digits -> current
            dot < 0 && current.length >= 10 -> current
            current == "0" -> key.toString()
            else -> current + key
        }
    }
}
