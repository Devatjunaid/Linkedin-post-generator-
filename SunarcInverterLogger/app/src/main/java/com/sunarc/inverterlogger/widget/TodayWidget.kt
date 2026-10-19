package com.sunarc.inverterlogger.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sunarc.inverterlogger.MainActivity
import com.sunarc.inverterlogger.SunarcApp
import com.sunarc.inverterlogger.domain.Aggregation
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Home-screen widget: today's energy and when it was last scanned. */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as SunarcApp
        val profile = app.container.settings.current()
        val readings = app.container.log.allReadings()
        val today = LocalDate.now(profile.zone)
        val todayKwh = Aggregation.dailyYields(readings, profile.zone).firstOrNull { it.date == today }?.kwh
        val last = readings.maxByOrNull { it.timestamp }
        val lastText = last?.let {
            val t = it.timestamp.atZone(profile.zone)
            if (t.toLocalDate() == today) "Scanned ${t.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            else "Last scan ${t.format(DateTimeFormatter.ofPattern("d MMM"))}"
        } ?: "Tap to scan"

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF0C0F14))
                    .cornerRadius(24.dp)
                    .padding(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("●", style = TextStyle(color = ColorProvider(Color(0xFFFFB020)), fontSize = 14.sp))
                    Spacer(GlanceModifier.width(6.dp))
                    Text("Today", style = TextStyle(color = ColorProvider(Color(0xFF9AA4B2)), fontSize = 13.sp))
                }
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    if (todayKwh != null) "%.1f kWh".format(todayKwh) else "– kWh",
                    style = TextStyle(color = ColorProvider(Color(0xFFF2F4F7)), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(lastText, style = TextStyle(color = ColorProvider(Color(0xFFFFC65C)), fontSize = 12.sp))
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) = runCatching { TodayWidget().updateAll(context) }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
