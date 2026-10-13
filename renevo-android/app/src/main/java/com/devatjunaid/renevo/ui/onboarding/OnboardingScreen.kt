package com.devatjunaid.renevo.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devatjunaid.renevo.R
import com.devatjunaid.renevo.data.AppSettings
import com.devatjunaid.renevo.domain.Catalog
import com.devatjunaid.renevo.domain.Formats
import com.devatjunaid.renevo.ui.components.RenevoLogo
import com.devatjunaid.renevo.ui.theme.RenevoTheme
import kotlinx.coroutines.launch

private data class Page(val image: Int, val title: String, val body: String)

private val pages = listOf(
    Page(
        R.drawable.ill_privacy,
        "Private by design",
        "No bank link and no account. You add what you pay for, and it's stored only on this phone. Lock it with your fingerprint.",
    ),
    Page(
        R.drawable.ill_reminders,
        "Never miss a renewal",
        "Get a heads-up before every charge, and alerts 3 days, 1 day and the morning before a free trial turns paid.",
    ),
    Page(
        R.drawable.ill_insights,
        "See where it all goes",
        "Monthly and yearly totals, categories, price increases and smart tips for subscriptions you barely use.",
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: (currency: String) -> Unit) {
    val c = RenevoTheme.colors
    val pager = rememberPagerState(pageCount = { pages.size + 1 })
    val scope = rememberCoroutineScope()
    var currency by rememberSaveable { mutableStateOf(AppSettings.defaultCurrency()) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone(currency) }

    fun finish() {
        if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else onDone(currency)
    }

    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RenevoLogo(size = 34.dp, animate = false, idleMotion = false)
            Spacer(Modifier.width(10.dp))
            Text("Renevo", style = MaterialTheme.typography.headlineSmall, color = c.onSurface, modifier = Modifier.weight(1f))
            if (pager.currentPage < pages.size) {
                TextButton(onClick = { scope.launch { pager.animateScrollToPage(pages.size) } }) { Text("Skip") }
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            if (index < pages.size) {
                val page = pages[index]
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(
                        painterResource(page.image), contentDescription = null,
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(page.title, style = MaterialTheme.typography.headlineLarge, color = c.onSurface, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(page.body, style = MaterialTheme.typography.bodyLarge, color = c.muted, textAlign = TextAlign.Center)
                }
            } else {
                CurrencyPage(currency) { currency = it }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(pages.size + 1) { i ->
                    val selected = pager.currentPage == i
                    val w by animateDpAsState(if (selected) 22.dp else 8.dp, label = "dot")
                    val col by animateColorAsState(if (selected) c.accent else c.outline, label = "dotc")
                    Box(Modifier.height(8.dp).width(w).clip(CircleShape).background(col))
                }
            }
            val last = pager.currentPage == pages.size
            Button(
                onClick = { if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.accent),
            ) {
                Text(if (last) "Get started" else "Next", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun CurrencyPage(currency: String, onPick: (String) -> Unit) {
    val c = RenevoTheme.colors
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RenevoLogo(size = 120.dp)
        Spacer(Modifier.height(28.dp))
        Text("Your main currency", style = MaterialTheme.typography.headlineLarge, color = c.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Totals and budgets use it. Subscriptions in other currencies are converted with rates you set.",
            style = MaterialTheme.typography.bodyLarge, color = c.muted, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Box {
            OutlinedButton(onClick = { open = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.height(56.dp)) {
                Text("$currency  ${Formats.symbol(currency)}", style = MaterialTheme.typography.titleLarge, color = c.onSurface)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = c.muted, modifier = Modifier.size(22.dp))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                (listOf(currency) + Catalog.currencies.filter { it != currency }).forEach { code ->
                    DropdownMenuItem(text = { Text("$code  ${Formats.symbol(code)}") }, onClick = { onPick(code); open = false })
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Ads keep Renevo free. They're provided by Google AdMob and never see your subscriptions.",
            style = MaterialTheme.typography.bodySmall, color = c.muted, textAlign = TextAlign.Center,
        )
    }
}
