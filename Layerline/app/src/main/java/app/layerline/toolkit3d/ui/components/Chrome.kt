package app.layerline.toolkit3d.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.layerline.toolkit3d.domain.Fmt
import app.layerline.toolkit3d.ui.theme.LL
import app.layerline.toolkit3d.ui.theme.LLType

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Bench("bench", "BENCH", LIcons.Bench),
    Cost("cost", "COST", LIcons.Cost),
    Stock("stock", "STOCK", LIcons.Spool),
    Calib("calib", "CALIB", LIcons.Calib),
    Log("log", "LOG", LIcons.Log),
}

/**
 * Bottom navigation drawn as a printer gantry: a dashed rail with a nozzle that glides to the
 * selected tab and lays a short bead of "filament" on the rail where it stops.
 */
@Composable
fun GantryNavBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .background(LL.Nav)
            .drawTopBorder(),
    ) {
        val tabWidth = maxWidth / Tab.entries.size
        val nozzleX by animateDpAsState(
            targetValue = tabWidth * selected.ordinal + tabWidth / 2,
            animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
            label = "nozzle",
        )
        val bead = remember { Animatable(0f) }
        LaunchedEffect(selected) {
            bead.snapTo(0f)
            bead.animateTo(1f, tween(420, delayMillis = 180))
        }

        Canvas(Modifier.fillMaxWidth().height(20.dp)) {
            val y = 9.dp.toPx()
            drawLine(
                LL.Line, Offset(18.dp.toPx(), y), Offset(size.width - 18.dp.toPx(), y), 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
            )
            val cx = nozzleX.toPx()
            val half = 12.dp.toPx() * bead.value
            drawLine(LL.Lime, Offset(cx - half, y), Offset(cx + half, y), 2.dp.toPx())
            val w = 11.dp.toPx()
            val top = 2.dp.toPx()
            val nozzle = Path().apply {
                moveTo(cx - w, top)
                lineTo(cx + w, top)
                lineTo(cx + w, top + 6.dp.toPx())
                lineTo(cx + 5.dp.toPx(), top + 6.dp.toPx())
                lineTo(cx + 2.dp.toPx(), top + 12.dp.toPx())
                lineTo(cx - 2.dp.toPx(), top + 12.dp.toPx())
                lineTo(cx - 5.dp.toPx(), top + 6.dp.toPx())
                lineTo(cx - w, top + 6.dp.toPx())
                close()
            }
            drawPath(nozzle, LL.Lime)
        }

        Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp)) {
            Tab.entries.forEach { tab ->
                val active = tab == selected
                val tint by animateColorAsState(if (active) LL.Lime else LL.Muted, label = "tab")
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .pressable({ onSelect(tab) }, role = Role.Tab)
                        .semantics { this.selected = active },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    LIcon(tab.icon, tint, size = 22.dp, contentDescription = null)
                    Spacer(Modifier.height(4.dp))
                    Text(tab.label, style = LLType.mono(10.sp, FontWeight.Medium, tint))
                }
            }
        }
    }
}

private fun Modifier.drawTopBorder(): Modifier = drawBehind {
    drawLine(LL.Line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
}

@Composable
fun ScreenHeader(
    code: String,
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconSquare(LIcons.Back, "Back", onBack)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            GcodeTag(code)
            Text(title.uppercase(), style = LLType.display(30.sp), modifier = Modifier.padding(top = 2.dp))
            if (subtitle != null) Text(subtitle, style = LLType.mono(12.sp, color = LL.Muted), modifier = Modifier.padding(top = 4.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

/**
 * Dialog shell: chamfered panel that springs in, scrollable body, actions row at the bottom.
 */
@Composable
fun LDialog(
    code: String,
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val appear = remember { Animatable(0f) }
        LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) }
        Box(
            Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .widthIn(max = 520.dp)
                .imePadding()
                .graphicsLayer {
                    alpha = appear.value.coerceIn(0f, 1f)
                    val sc = 0.9f + 0.1f * appear.value
                    scaleX = sc
                    scaleY = sc
                    translationY = (1f - appear.value) * 40f
                }
                .background(LL.Panel, Chamfer)
                .border(1.dp, LL.LineStrong, Chamfer),
        ) {
            Column(Modifier.padding(18.dp)) {
                GcodeTag(code)
                Text(title, style = LLType.display(24.sp), modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    content = content,
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = actions)
            }
        }
    }
}

/** Number entry dialog used by every tappable value. */
@Composable
fun NumberEntryDialog(
    title: String,
    initial: Double,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var text by remember { mutableStateOf(Fmt.trim(initial, 4)) }
    val parsed = text.toDoubleOrNull()
    LDialog(
        code = "; M117 — SET VALUE",
        title = title,
        onDismiss = onDismiss,
        actions = {
            LButton("Cancel", onDismiss, Modifier.weight(1f), style = ButtonStyle.Ghost)
            LButton("Set", { parsed?.let(onConfirm) }, Modifier.weight(1f), enabled = parsed != null)
        },
    ) {
        NumberField(title, text, { text = it }, suffix = unit)
    }
}

/** Generic single-choice list dialog. */
@Composable
fun <T> PickerDialog(
    code: String,
    title: String,
    items: List<T>,
    label: (T) -> String,
    sub: (T) -> String = { "" },
    isSelected: (T) -> Boolean,
    onDismiss: () -> Unit,
    onPick: (T) -> Unit,
) {
    LDialog(code, title, onDismiss, actions = { LButton("Close", onDismiss, Modifier.weight(1f), style = ButtonStyle.Ghost) }) {
        items.forEach { item ->
            val sel = isSelected(item)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .pressable({ onPick(item) })
                    .background(if (sel) LL.LimeDeep else LL.Panel, ChamferS)
                    .border(1.dp, if (sel) LL.Lime else LL.Line, ChamferS)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label(item), style = LLType.body(15.sp, FontWeight.Medium))
                    val s = sub(item)
                    if (s.isNotEmpty()) Text(s, style = LLType.mono(11.sp, color = LL.Muted))
                }
                if (sel) LIcon(LIcons.Check, LL.Lime, size = 18.dp)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    LDialog(
        code = "; M0 — CONFIRM",
        title = title,
        onDismiss = onDismiss,
        actions = {
            LButton("Cancel", onDismiss, Modifier.weight(1f), style = ButtonStyle.Ghost)
            LButton(confirm, onConfirm, Modifier.weight(1f), style = ButtonStyle.Hot)
        },
    ) { Text(body, style = LLType.body(14.sp, color = LL.Soft)) }
}
