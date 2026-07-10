// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.compose.ui.graphics.Color
import com.ujwal.halter.data.HalterDatabase
import com.ujwal.halter.ui.util.formatMinutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class HalterWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) { loadWidgetData(context) }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(ColorProvider(Color(0xFF1A1A2E), Color(0xFFF5F5F5)))
                        .padding(14.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // ── Header row ──────────────────────────────────
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱ Halter",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = ColorProvider(Color(0xFF7B68EE), Color(0xFF5A4FD1))
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            if (data.focusActive) {
                                Text(
                                    text = "🔒 Focus",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = ColorProvider(Color(0xFFFF6B6B), Color(0xFFD32F2F))
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(10.dp))

                        // ── Screen time ─────────────────────────────────
                        Text(
                            text = "TODAY",
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFF888888), Color(0xFF999999))
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = if (data.totalScreenTimeMinutes > 0)
                                formatMinutes(data.totalScreenTimeMinutes.toLong())
                            else "0 min",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                color = ColorProvider(Color(0xFFFFFFFF), Color(0xFF1A1A2E))
                            )
                        )
                        Text(
                            text = "screen time",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = ColorProvider(Color(0xFFAAAAAA), Color(0xFF666666))
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(10.dp))

                        // ── Scrolls row ──────────────────────────────────
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "↕ ${data.totalScrollsToday}",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = ColorProvider(Color(0xFF98E4D6), Color(0xFF2E7D6B))
                                    )
                                )
                                Text(
                                    text = "scrolls",
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        color = ColorProvider(Color(0xFF888888), Color(0xFF999999))
                                    )
                                )
                            }
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "📱 ${data.monitoredCount}",
                                    style = TextStyle(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = ColorProvider(Color(0xFFFFD700), Color(0xFFB8860B))
                                    )
                                )
                                Text(
                                    text = "apps",
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        color = ColorProvider(Color(0xFF888888), Color(0xFF999999))
                                    )
                                )
                            }
                        }

                        // ── Streak (only if > 0) ─────────────────────────
                        if (data.streak > 0) {
                            Spacer(modifier = GlanceModifier.height(8.dp))
                            Text(
                                text = "🔥 ${data.streak}d streak",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ColorProvider(Color(0xFFFF8C42), Color(0xFFD4611E))
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadWidgetData(context: Context): WidgetData {
        val db = HalterDatabase.create(context)
        val usageDao = db.usageSessionDao()
        val scrollDao = db.scrollEventDao()
        val focusDao = db.focusSessionDao()
        val monitoredDao = db.monitoredAppDao()

        val now = System.currentTimeMillis()
        val dayStart = localDayStartMillis(now)

        val monitoredApps = monitoredDao.observeAll().first()
        var totalScreenTimeMinutes = 0L
        var totalScrollsToday = 0

        for (app in monitoredApps) {
            val millis = usageDao.totalUsageMillis(app.packageName, dayStart, now)
            totalScreenTimeMinutes += TimeUnit.MILLISECONDS.toMinutes(millis)
            totalScrollsToday += scrollDao.scrollsToday(app.packageName, dayStart)
        }

        val activeFocus = focusDao.active()
        val anySessionEver = usageDao.anySessionExists()
        val streak = if (anySessionEver) 1 else 0

        return WidgetData(
            totalScrollsToday = totalScrollsToday,
            totalScreenTimeMinutes = totalScreenTimeMinutes.toInt(),
            focusActive = activeFocus != null && !activeFocus.completed,
            streak = streak,
            monitoredCount = monitoredApps.size
        )
    }
}

data class WidgetData(
    val totalScrollsToday: Int,
    val totalScreenTimeMinutes: Int,
    val focusActive: Boolean,
    val streak: Int,
    val monitoredCount: Int
)

fun localDayStartMillis(nowMillis: Long): Long {
    val zone = java.time.ZoneId.systemDefault()
    return java.time.Instant.ofEpochMilli(nowMillis)
        .atZone(zone)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
}
