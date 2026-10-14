package com.devatjunaid.renewalert.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.devatjunaid.renewalert.MainActivity
import com.devatjunaid.renewalert.R
import com.devatjunaid.renewalert.container
import com.devatjunaid.renewalert.domain.Formats
import com.devatjunaid.renewalert.domain.Portfolio
import java.time.LocalDate

private data class WidgetRow(val name: String, val whenText: String, val amount: String)

private data class WidgetData(val total: String, val subtitle: String, val rows: List<WidgetRow>)

class RenewAlertWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        provideContent { WidgetContent(data) }
    }

    private suspend fun load(context: Context): WidgetData {
        val c = context.container
        val settings = c.settings.current()
        val today = LocalDate.now()
        val p = Portfolio(c.repository.all(), c.repository.allPrices(), settings, today)
        val hide = settings.hideAmounts
        val rows = p.upcoming(30).take(3).map {
            WidgetRow(it.sub.name, Formats.relativeDay(today, it.date), Formats.money(it.amountMinor, it.sub.currency, hide))
        }
        return WidgetData(
            total = Formats.money(p.monthlyTotal, settings.currency, hide),
            subtitle = "per month · ${p.active.size} active",
            rows = rows,
        )
    }
}

private val White = ColorProvider(Color.White)
private val Muted = ColorProvider(Color(0xFFB9BDC6))

@Composable
private fun WidgetContent(d: WidgetData) {
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .background(Color(0xFF15171C))
            .cornerRadius(24.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(ImageProvider(R.drawable.ic_logo_tile), contentDescription = null, modifier = GlanceModifier.size(22.dp))
            Spacer(GlanceModifier.width(8.dp))
            Text("Renew Alert", style = TextStyle(color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium))
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(d.total, style = TextStyle(color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold))
        Text(d.subtitle, style = TextStyle(color = Muted, fontSize = 12.sp))
        Spacer(GlanceModifier.height(8.dp))
        if (d.rows.isEmpty()) {
            Text("No charges in the next 30 days", style = TextStyle(color = Muted, fontSize = 12.sp))
        } else {
            d.rows.forEach { r ->
                Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        "${r.name} · ${r.whenText}",
                        style = TextStyle(color = White, fontSize = 12.sp),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(r.amount, style = TextStyle(color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

class RenewAlertWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RenewAlertWidget()
}

object WidgetUpdater {
    suspend fun update(context: Context) {
        runCatching { RenewAlertWidget().updateAll(context) }
    }
}
