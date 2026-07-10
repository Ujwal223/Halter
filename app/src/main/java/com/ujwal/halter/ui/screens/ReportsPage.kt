// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ujwal.halter.data.HalterRepository
import com.ujwal.halter.data.UsageSession
import com.ujwal.halter.data.ScrollEvent
import com.ujwal.halter.settings.HalterSettings
import com.ujwal.halter.settings.SettingsRepository
import com.ujwal.halter.ui.util.formatDurationMillis
import com.ujwal.halter.ui.util.formatMinutes
import org.koin.compose.koinInject
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

// ── Data helpers ──────────────────────────────────────────────────────────────

private data class DayStats(
    val label: String,       // "Mon", "Tue", etc.
    val screenTimeMillis: Long,
    val scrollCount: Int,
    val isToday: Boolean
)

private fun buildDayStats(
    sessions: List<UsageSession>,
    scrollEvents: List<ScrollEvent>,
    days: Int = 7
): List<DayStats> {
    val zone = ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone)
    return (days - 1 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val daySessionMillis = sessions
            .filter { it.startEpochMillis >= startMillis && it.startEpochMillis < endMillis }
            .sumOf { s ->
                val live = if (s.endEpochMillis == null && s.lastForegroundStartEpochMillis != null)
                    (System.currentTimeMillis() - s.lastForegroundStartEpochMillis).coerceAtLeast(0L) else 0L
                s.accumulatedUsageMillis + live
            }

        val dayScrolls = scrollEvents
            .filter { it.timestampEpochMillis >= startMillis && it.timestampEpochMillis < endMillis }
            .sumOf { it.count }

        DayStats(
            label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            screenTimeMillis = daySessionMillis,
            scrollCount = dayScrolls,
            isToday = daysAgo == 0
        )
    }
}

// ── Bar Chart ─────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyBarChart(
    dayStats: List<DayStats>,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dayStats.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val todayColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error

    val maxMillis = dayStats.maxOfOrNull { it.screenTimeMillis }?.takeIf { it > 0 } ?: 1L

    // Animate bar heights from 0→final
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, animationSpec = tween(700))
    }
    val progress = animProgress.value

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .pointerInput(dayStats.size) {
                detectTapGestures { offset ->
                    val barWidth = size.width.toFloat() / dayStats.size
                    val idx = (offset.x / barWidth).toInt().coerceIn(0, dayStats.size - 1)
                    onDaySelected(idx)
                }
            }
    ) {
        val barCount = dayStats.size
        val totalWidth = size.width
        val totalHeight = size.height
        val barSlotWidth = totalWidth / barCount
        val barPadding = barSlotWidth * 0.18f
        val barWidth = barSlotWidth - barPadding * 2
        val maxBarHeight = totalHeight * 0.72f
        val labelHeight = 24.dp.toPx()

        dayStats.forEachIndexed { i, day ->
            val ratio = (day.screenTimeMillis.toFloat() / maxMillis.toFloat()).coerceIn(0f, 1f)
            val barHeight = (maxBarHeight * ratio * progress).coerceAtLeast(if (day.screenTimeMillis > 0) 6.dp.toPx() else 0f)
            val left = i * barSlotWidth + barPadding
            val top = totalHeight - barHeight - labelHeight
            val isSelected = i == selectedDay

            // Bar fill
            val barColor = when {
                isSelected -> primaryColor
                day.isToday -> todayColor
                else -> primaryContainerColor
            }
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Selected indicator dot above bar
            if (isSelected && barHeight > 0) {
                drawCircle(
                    color = primaryColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(left + barWidth / 2, top - 8.dp.toPx())
                )
            }
        }
    }

    // Day labels row
    Row(modifier = Modifier.fillMaxWidth()) {
        dayStats.forEachIndexed { i, day ->
            val isSelected = i == selectedDay
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (isSelected) 11.sp else 10.sp
                    ),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        day.isToday -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

// ── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsPage(onBack: () -> Unit) {
    val settingsRepository: SettingsRepository = koinInject()
    val settings by settingsRepository.settings.collectAsState(initial = HalterSettings())
    val halterRepository: HalterRepository = koinInject()
    // 7-day window for chart; today window for the summary headline
    val from = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
    val todayStart = remember {
        val zone = java.time.ZoneId.systemDefault()
        java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    }
    val sessions by halterRepository.observeRecentUsage(from).collectAsState(initial = emptyList())
    val scrolls by halterRepository.observeRecentScrolls(from).collectAsState(initial = emptyList())
    val monitored by halterRepository.observeMonitoredApps().collectAsState(initial = emptyList())

    // Today’s screen time (closed sessions only to avoid inflating active session time)
    val todayMillis = sessions
        .filter { it.startEpochMillis >= todayStart }
        .sumOf { s ->
            val live = if (s.endEpochMillis == null && s.lastForegroundStartEpochMillis != null)
                (System.currentTimeMillis() - s.lastForegroundStartEpochMillis).coerceAtLeast(0L) else 0L
            s.accumulatedUsageMillis + live
        }
    // 7-day total (kept for reference, not shown in headline)
    val totalMillis = sessions.sumOf { s ->
        val live = if (s.endEpochMillis == null && s.lastForegroundStartEpochMillis != null)
            (System.currentTimeMillis() - s.lastForegroundStartEpochMillis).coerceAtLeast(0L) else 0L
        s.accumulatedUsageMillis + live
    }
    // Scroll tracking disabled — totals kept for chart data only
    // val totalScrolls = scrolls.sumOf { it.count }

    val dayStats = remember(sessions, scrolls) { buildDayStats(sessions, scrolls) }
    var selectedDayIndex by remember(dayStats) { mutableIntStateOf(dayStats.size - 1) }
    val selectedDay = dayStats.getOrNull(selectedDayIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Weekly summary card ───────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Column {
                            Text(
                                "Screen Time",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.4.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${formatDurationMillis(todayMillis)} today",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    // Short-Video Scrolls widget hidden — scroll tracking is disabled
                    // Row(...) { Icon(Swipe) ... Text("$totalScrolls scrolls") }
                }
            }

            // ── Interactive 7-day bar chart ───────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Daily Screen Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))

                    if (dayStats.all { it.screenTimeMillis == 0L && it.scrollCount == 0 }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No data yet — start using Halter to see your stats here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        WeeklyBarChart(
                            dayStats = dayStats,
                            selectedDay = selectedDayIndex,
                            onDaySelected = { selectedDayIndex = it }
                        )
                        Spacer(Modifier.height(8.dp))
                        // Selected day detail
                        if (selectedDay != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Screen Time",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            formatDurationMillis(selectedDay.screenTimeMillis),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    // Scrolls column hidden — scroll tracking disabled
                                    // Column(horizontalAlignment = Alignment.End) { Text("${selectedDay.scrollCount}") }
                                }
                            }
                        }
                    }
                }
            }

            // ── Per-app breakdown ─────────────────────────────────────────
            val monitoredPkgNames = monitored.map { it.packageName }.toSet()
            val sessionPkgNames = sessions.map { it.packageName }.distinct().filter { it !in monitoredPkgNames }
            val allAppPkgs = monitored.map { it.packageName } + sessionPkgNames

            val appEntries = allAppPkgs.mapNotNull { pkg ->
                val appMillis = sessions.filter { it.packageName == pkg }
                    .sumOf { s ->
                        val live = if (s.endEpochMillis == null && s.lastForegroundStartEpochMillis != null)
                            (System.currentTimeMillis() - s.lastForegroundStartEpochMillis).coerceAtLeast(0L) else 0L
                        s.accumulatedUsageMillis + live
                    }
                val appScrolls = scrolls.filter { it.packageName == pkg }
                    .sumOf { it.count }
                if (appMillis <= 0 && appScrolls <= 0) return@mapNotNull null
                val displayName = monitored.firstOrNull { it.packageName == pkg }?.displayName ?: pkg
                Triple(pkg, displayName, Pair(appMillis, appScrolls))
            }.sortedByDescending { (_, _, data) -> data.first + data.second * 10L }

            val maxAppMillis = appEntries.maxOfOrNull { it.third.first }?.takeIf { it > 0 } ?: 1L

            if (appEntries.isNotEmpty()) {
                Text("Per-App", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                appEntries.forEach { (pkg, displayName, data) ->
                    val (appMillis, appScrolls) = data
                    val isCurrentlyMonitored = pkg in monitoredPkgNames
                    val barFraction = (appMillis.toFloat() / maxAppMillis.toFloat()).coerceIn(0f, 1f)

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AppIcon(packageName = pkg, modifier = Modifier.size(40.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(displayName, style = MaterialTheme.typography.titleSmall)
                                        if (!isCurrentlyMonitored) {
                                            Text(
                                                "(inactive)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        formatDurationMillis(appMillis),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // Mini usage bar
                            if (appMillis > 0) {
                                val barAnim = remember { Animatable(0f) }
                                LaunchedEffect(barFraction) {
                                    barAnim.animateTo(barFraction, animationSpec = tween(600))
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(barAnim.value)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("No data yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Add apps from the Apps tab to start tracking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
