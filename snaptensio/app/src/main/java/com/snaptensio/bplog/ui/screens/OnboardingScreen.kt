package com.snaptensio.bplog.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.unit.dp
import com.snaptensio.bplog.R
import com.snaptensio.bplog.ui.MainViewModel
import com.snaptensio.bplog.ui.components.AnimatedLogo
import com.snaptensio.bplog.ui.theme.Brand
import kotlinx.coroutines.launch

private data class Intro(val title: String, val body: String)

private val pages = listOf(
    Intro("Point your camera at the monitor", "SnapTensio reads the systolic, diastolic and pulse numbers off any digital blood pressure monitor. No Bluetooth, no typing."),
    Intro("See what your numbers mean", "Clear charts, colour-coded ranges, and patterns like “evenings run higher” worked out from your own readings."),
    Intro("Private by design", "No account and no cloud. Your readings stay on this phone until you choose to share a report with your doctor."),
)

@Composable
fun OnboardingScreen(vm: com.snaptensio.bplog.ui.MainViewModel, onDone: () -> Unit) {
    val pager = rememberPagerState { pages.size + 1 }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().imePadding()) {
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            if (page < pages.size) IntroPage(page, pages[page]) else SetupPage(vm, onDone)
        }
        if (pager.currentPage < pages.size) {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { scope.launch { pager.animateScrollToPage(pages.size) } }) { Text("Skip") }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(pages.size + 1) { i ->
                        val active = i == pager.currentPage
                        Box(
                            Modifier.height(8.dp).width(if (active) 22.dp else 8.dp).clip(CircleShape)
                                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                        )
                    }
                }
                Button(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Text("Next") }
            }
        }
    }
}

@Composable
private fun IntroPage(index: Int, intro: Intro) {
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(Brush.verticalGradient(listOf(Brand.BlueBright, Brand.BlueDeep))),
            contentAlignment = Alignment.Center,
        ) {
            when (index) {
                0 -> ScanIllustration()
                1 -> ChartIllustration()
                else -> PrivacyIllustration()
            }
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(intro.title, style = MaterialTheme.typography.headlineLarge)
            Text(intro.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A BP monitor with a scan line sweeping across its LCD and the digits lighting up. */
@Composable
private fun ScanIllustration() {
    val t = rememberInfiniteTransition(label = "scan")
    val sweep by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sweep")
    Canvas(Modifier.size(260.dp)) {
        val w = size.width
        val device = Size(w * 0.62f, w * 0.82f)
        val o = Offset((w - device.width) / 2, (size.height - device.height) / 2)
        drawRoundRect(Color(0xFFE9ECEF), o, device, CornerRadius(28.dp.toPx()))
        val lcdO = Offset(o.x + device.width * 0.1f, o.y + device.height * 0.1f)
        val lcd = Size(device.width * 0.8f, device.height * 0.6f)
        drawRoundRect(Color(0xFFA9B8A2), lcdO, lcd, CornerRadius(10.dp.toPx()))
        // Seven-segment-ish bars for 128 / 82 / 72
        val ink = Color(0xFF1B2418)
        val rows = listOf(0.12f to 0.9f, 0.45f to 0.7f, 0.75f to 0.45f)
        rows.forEach { (top, widthFrac) ->
            val y = lcdO.y + lcd.height * top
            val h = lcd.height * 0.18f * widthFrac.coerceAtLeast(0.6f)
            val digits = if (widthFrac > 0.8f) 3 else 2
            val dw = lcd.width * 0.18f
            for (d in 0 until digits) {
                val x = lcdO.x + lcd.width - 10.dp.toPx() - (digits - d) * (dw + 6.dp.toPx())
                drawRoundRect(ink, Offset(x, y), Size(dw, h), CornerRadius(3.dp.toPx()), style = Stroke(4.dp.toPx()))
            }
        }
        // Buttons
        drawRoundRect(Brand.Blue, Offset(o.x + device.width * 0.18f, o.y + device.height * 0.8f), Size(device.width * 0.26f, 18.dp.toPx()), CornerRadius(9.dp.toPx()))
        drawRoundRect(Color(0xFFB4BCC6), Offset(o.x + device.width * 0.56f, o.y + device.height * 0.8f), Size(device.width * 0.26f, 18.dp.toPx()), CornerRadius(9.dp.toPx()))
        // Viewfinder corners
        val pad = 14.dp.toPx()
        val l = 26.dp.toPx()
        val tl = Offset(lcdO.x - pad, lcdO.y - pad)
        val br = Offset(lcdO.x + lcd.width + pad, lcdO.y + lcd.height + pad)
        val sw = 4.dp.toPx()
        listOf(
            tl to Offset(1f, 1f), Offset(br.x, tl.y) to Offset(-1f, 1f),
            Offset(tl.x, br.y) to Offset(1f, -1f), br to Offset(-1f, -1f),
        ).forEach { (p, d) ->
            drawLine(Color.White, p, Offset(p.x + l * d.x, p.y), sw, StrokeCap.Round)
            drawLine(Color.White, p, Offset(p.x, p.y + l * d.y), sw, StrokeCap.Round)
        }
        // Scan line
        val y = lcdO.y + lcd.height * sweep
        drawLine(Color(0xFF3DDC84), Offset(tl.x + 6.dp.toPx(), y), Offset(br.x - 6.dp.toPx(), y), 3.dp.toPx(), StrokeCap.Round)
        clipRect(top = lcdO.y, bottom = y) {
            drawRoundRect(Color(0x333DDC84), lcdO, lcd, CornerRadius(10.dp.toPx()))
        }
    }
}

@Composable
private fun ChartIllustration() {
    val t = rememberInfiniteTransition(label = "chart")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "draw")
    Canvas(Modifier.size(280.dp, 220.dp)) {
        drawRoundRect(Color.White, size = size, cornerRadius = CornerRadius(24.dp.toPx()))
        val sys = listOf(0.30f, 0.22f, 0.28f, 0.18f, 0.26f, 0.20f, 0.24f, 0.16f, 0.2f)
        val dia = listOf(0.62f, 0.56f, 0.6f, 0.54f, 0.6f, 0.55f, 0.58f, 0.52f, 0.55f)
        val left = 20.dp.toPx(); val right = size.width - 20.dp.toPx()
        val top = 30.dp.toPx(); val h = size.height - 60.dp.toPx()
        drawRect(Color(0xFFE7F4EC), Offset(left, top + h * 0.34f), Size(right - left, h * 0.18f))
        fun line(vals: List<Float>, color: Color) {
            val path = Path()
            vals.forEachIndexed { i, v ->
                val x = left + (right - left) * i / (vals.size - 1)
                if (i == 0) path.moveTo(x, top + h * v) else path.lineTo(x, top + h * v)
            }
            clipRect(right = left + (right - left) * (p * 1.3f).coerceAtMost(1f)) {
                drawPath(path, color, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        line(sys, Brand.Systolic)
        line(dia, Brand.Diastolic)
        val bars = listOf(Color(0xFF1E8E5A) to 0.3f, Color(0xFFE0A526) to 0.2f, Color(0xFFE07A2B) to 0.38f, Color(0xFFC2272D) to 0.12f)
        var x = left
        bars.forEach { (c, f) ->
            val bw = (right - left) * f
            drawRect(c, Offset(x, size.height - 22.dp.toPx()), Size(bw - 3f, 8.dp.toPx()))
            x += bw
        }
    }
}

@Composable
private fun PrivacyIllustration() {
    val t = rememberInfiniteTransition(label = "privacy")
    val pulse by t.animateFloat(0.92f, 1.04f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulse")
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(240.dp)) {
            val c = center
            for (i in 1..3) drawCircle(Color.White.copy(alpha = 0.08f * i), radius = size.minDimension / 2 * (0.5f + i * 0.16f) * pulse, center = c)
            val s = size.minDimension * 0.5f
            val shield = Path().apply {
                moveTo(c.x, c.y - s / 2)
                lineTo(c.x + s * 0.42f, c.y - s * 0.34f)
                lineTo(c.x + s * 0.42f, c.y + s * 0.02f)
                cubicTo(c.x + s * 0.42f, c.y + s * 0.32f, c.x + s * 0.18f, c.y + s * 0.46f, c.x, c.y + s * 0.54f)
                cubicTo(c.x - s * 0.18f, c.y + s * 0.46f, c.x - s * 0.42f, c.y + s * 0.32f, c.x - s * 0.42f, c.y + s * 0.02f)
                lineTo(c.x - s * 0.42f, c.y - s * 0.34f)
                close()
            }
            drawPath(shield, Color.White)
        }
        AnimatedLogo(size = 92.dp, intro = false, tile = true)
    }
}

@Composable
private fun SetupPage(vm: MainViewModel, onDone: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var sys by rememberSaveable { mutableStateOf("130") }
    var dia by rememberSaveable { mutableStateOf("80") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    val sysV = sys.toIntOrNull()
    val diaV = dia.toIntOrNull()
    val targetOk = sysV != null && diaV != null && sysV in 90..200 && diaV in 50..130 && sysV > diaV
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        AnimatedLogo(size = 72.dp, intro = false)
        Text("Let's set you up", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(name, { name = it.take(30) }, label = { Text("Your name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Your target", style = MaterialTheme.typography.titleMedium)
        Text(
            "130/80 is a common home target. Use the one your doctor gave you; you can change it later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(sys, { sys = it.filter(Char::isDigit).take(3) }, label = { Text("Systolic below") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(dia, { dia = it.filter(Char::isDigit).take(3) }, label = { Text("Diastolic below") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .toggleable(accepted, role = Role.Checkbox) { accepted = it }
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(checked = accepted, onCheckedChange = null)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = {
                vm.completeOnboarding(name, sysV ?: 130, diaV ?: 80)
                onDone()
            },
            enabled = accepted && targetOk,
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text("Get started") }
        Text(
            "Ads keep SnapTensio free. They never use your health readings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
