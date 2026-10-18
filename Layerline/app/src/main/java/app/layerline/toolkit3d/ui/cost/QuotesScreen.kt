package app.layerline.toolkit3d.ui.cost

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.layerline.toolkit3d.data.Quote
import app.layerline.toolkit3d.domain.Fmt
import app.layerline.toolkit3d.domain.Money
import app.layerline.toolkit3d.ui.ScreenContext
import app.layerline.toolkit3d.ui.ScreenScroll
import app.layerline.toolkit3d.ui.WorkshopViewModel
import app.layerline.toolkit3d.ads.NativeAdSlot
import app.layerline.toolkit3d.ui.components.ConfirmDialog
import app.layerline.toolkit3d.ui.components.EmptyState
import app.layerline.toolkit3d.ui.components.IconSquare
import app.layerline.toolkit3d.ui.components.LIcons
import app.layerline.toolkit3d.ui.components.Panel
import app.layerline.toolkit3d.ui.components.ScreenHeader
import app.layerline.toolkit3d.ui.components.Tab
import app.layerline.toolkit3d.ui.theme.LL
import app.layerline.toolkit3d.ui.theme.LLType
import java.text.DateFormat
import java.util.Date

@Composable
fun QuotesScreen(ctx: ScreenContext) {
    val vm: WorkshopViewModel = viewModel(factory = WorkshopViewModel.Factory)
    val quotes by vm.quotes.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<Quote?>(null) }
    val context = LocalContext.current
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    ScreenScroll {
        ScreenHeader("; M20 — LIST SD CARD", "Saved quotes", onBack = ctx.back, subtitle = "${quotes.size} saved")
        Spacer(Modifier.height(16.dp))
        if (quotes.isEmpty()) {
            EmptyState(
                "No quotes yet",
                "Price a print in the calculator and tap Save quote to keep it here.",
                "Open calculator",
                { ctx.open(Tab.Cost.route) },
                LIcons.Receipt,
            )
        }
        quotes.forEachIndexed { i, q ->
            val money = Money(q.symbol, q.decimals)
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(q.title, style = LLType.display(18.sp, FontWeight.SemiBold), maxLines = 1)
                        Text(
                            "${q.machine} · ${q.material} · ${Fmt.trim(q.grams, 0)} g · ${Fmt.trim(q.hours, 2)} h",
                            style = LLType.mono(11.sp, color = LL.Muted),
                            maxLines = 1,
                        )
                        Text(dateFormat.format(Date(q.createdAt)), style = LLType.mono(10.sp, color = LL.Muted))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(money.format(q.price), style = LLType.display(20.sp, color = LL.Lime))
                        Text("cost ${money.format(q.cost)}", style = LLType.mono(10.sp, color = LL.Muted))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconSquare(LIcons.Share, "Share quote", { shareQuote(context, q.summary) })
                    IconSquare(LIcons.Trash, "Delete quote", { deleting = q }, tint = LL.Hot)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (i == 2) {
                NativeAdSlot(ctx.app.ads, "quotes")
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    deleting?.let { q ->
        ConfirmDialog("Delete quote?", "\"${q.title}\" will be removed.", "Delete", { deleting = null }) {
            vm.deleteQuote(q)
            deleting = null
        }
    }
}
