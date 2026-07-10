// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ujwal.halter.data.BlockSchedule
import com.ujwal.halter.data.HalterRepository
import com.ujwal.halter.data.MonitoredApp
import com.ujwal.halter.settings.HalterSettings
import com.ujwal.halter.settings.SettingsRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AppDetailScreen(packageName: String, onBack: () -> Unit) {
    val repository: HalterRepository = koinInject()
    val settingsRepository: SettingsRepository = koinInject()
    val settings by settingsRepository.settings.collectAsState(initial = HalterSettings())
    val scope = rememberCoroutineScope()
    var app by remember { mutableStateOf<MonitoredApp?>(null) }

    LaunchedEffect(packageName) {
        app = repository.getMonitoredApp(packageName) ?: MonitoredApp(packageName, packageName)
    }

    val current = app
    Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Hero
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppIcon(packageName = packageName, modifier = Modifier.size(56.dp))
            Column {
                Text(current?.displayName ?: packageName, style = MaterialTheme.typography.headlineSmall)
                Text("App-specific overrides", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider()

        // ── Breathing Gate ──
        IconToggleCard(
            icon = { Icon(Icons.Outlined.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            label = "Require breathing gate",
            description = "Shows a breathing pause + session picker before opening this app.",
            checked = current?.isFlaggedHarmful == true
        ) { app = current?.copy(isFlaggedHarmful = it) }

        // ── Strict Mode ──
        IconToggleCard(
            icon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            label = "Strict mode",
            description = "Blocks cannot be dismissed until they naturally expire. No override buttons.",
            checked = current?.strictModeEnabled == true
        ) { app = current?.copy(strictModeEnabled = it) }

        // ── Short-Video / Reels Block (only for known supported apps) ──
        val customScrollPackages = remember(settings.customScrollPackages) {
            settings.customScrollPackages.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        }
        val isScrollableApp = com.ujwal.halter.service.KnownScrollApps.all.contains(packageName) || packageName in customScrollPackages
        if (isScrollableApp) {
            IconToggleCard(
                icon = { Icon(Icons.Outlined.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                label = "Feed Guard",
                description = "Blocks Reels, Shorts & vertical video feeds in this app.",
                checked = current?.partialShortVideoBlocked == true
            ) { app = current?.copy(partialShortVideoBlocked = it) }
        }

        // ── Exclude from Focus ──
        IconToggleCard(
            icon = { Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            label = "Allow during Focus mode",
            description = "This app stays usable even when a Deep Focus session is active.",
            checked = current?.excludedFromFocus == true
        ) { app = current?.copy(excludedFromFocus = it) }

        HorizontalDivider()

        // ── Time Limits (conditional) ──
        var timeLimitsEnabled by remember(current) {
            mutableStateOf(current?.dailyTimeLimitMinutes != null || current?.sessionTimeLimitMinutes != null)
        }
        IconToggleCard(
            icon = { Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            label = "Time limits",
            description = "Restrict how long this app can be used.",
            checked = timeLimitsEnabled
        ) {
            timeLimitsEnabled = it
            if (!it) {
                app = current?.copy(dailyTimeLimitMinutes = null, sessionTimeLimitMinutes = null)
            }
        }

        if (timeLimitsEnabled) {
            var dailyStr by remember(current) { mutableStateOf(current?.dailyTimeLimitMinutes?.toString().orEmpty()) }
            var sessionStr by remember(current) { mutableStateOf(current?.sessionTimeLimitMinutes?.toString().orEmpty()) }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(dailyStr, { dailyStr = it.filter(Char::isDigit); app = current?.copy(dailyTimeLimitMinutes = dailyStr.toIntOrNull()) }, label = { Text("Daily limit (minutes)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(sessionStr, { sessionStr = it.filter(Char::isDigit); app = current?.copy(sessionTimeLimitMinutes = sessionStr.toIntOrNull()) }, label = { Text("Per-session limit (minutes)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
        }

        // Scroll Limits UI hidden — scroll tracking is disabled. DB fields preserved for future use.
        // if (isScrollableApp) {
        //     var scrollLimitsEnabled by remember(current) { ... }
        //     IconToggleCard(label = "Scroll Limits", ...) { ... }
        // }

        // ── Instant Block (orphan feature) ──
        var instantBlockEnabled by remember(current) { mutableStateOf(current?.isInstantBlocked == true) }
        IconToggleCard(
            icon = { Icon(Icons.Outlined.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            label = "Block now",
            description = "Instantly block this app until a set time or until manually unblocked.",
            checked = instantBlockEnabled
        ) {
            instantBlockEnabled = it
            if (!it) app = current?.copy(isInstantBlocked = false, instantBlockUntilEpochMillis = null)
            else app = current?.copy(isInstantBlocked = true)
        }

        // ── Scheduled Blocks ──
        val schedules by repository.observeSchedulesFor(packageName).collectAsState(initial = emptyList())
        var showAddSchedule by remember { mutableStateOf(false) }
        var scheduleStartMinute by remember { mutableIntStateOf(8 * 60) }  // default 8:00 AM
        var scheduleEndMinute by remember { mutableIntStateOf(18 * 60) }   // default 6:00 PM
        var scheduleDays by remember { mutableIntStateOf(0x7F) }          // default all days (bit 0=Mon..6=Sun)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Scheduled Blocks", style = MaterialTheme.typography.titleSmall)
            Button(onClick = { showAddSchedule = !showAddSchedule }) { Text(if (showAddSchedule) "Cancel" else "Add") }
        }

        if (showAddSchedule) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Days", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Mon" to 0, "Tue" to 1, "Wed" to 2, "Thu" to 3, "Fri" to 4, "Sat" to 5, "Sun" to 6).forEach { (label, bit) ->
                            val sel = scheduleDays and (1 shl bit) != 0
                            androidx.compose.material3.FilterChip(
                                onClick = { scheduleDays = scheduleDays xor (1 shl bit) },
                                label = { Text(label) },
                                selected = sel
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Start", style = MaterialTheme.typography.labelMedium)
                            OutlinedTextField(
                                value = "${scheduleStartMinute / 60}:${"%02d".format(scheduleStartMinute % 60)}",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = false
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("End", style = MaterialTheme.typography.labelMedium)
                            OutlinedTextField(
                                value = "${scheduleEndMinute / 60}:${"%02d".format(scheduleEndMinute % 60)}",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = false
                            )
                        }
                    }
                    // Sliders for time (simplified)
                    Text("Start: ${scheduleStartMinute / 60}:${"%02d".format(scheduleStartMinute % 60)}", style = MaterialTheme.typography.bodySmall)
                    androidx.compose.material3.Slider(value = scheduleStartMinute.toFloat(), onValueChange = { scheduleStartMinute = it.toInt().coerceIn(0, 1439) }, valueRange = 0f..1439f)
                    Text("End: ${scheduleEndMinute / 60}:${"%02d".format(scheduleEndMinute % 60)}", style = MaterialTheme.typography.bodySmall)
                    androidx.compose.material3.Slider(value = scheduleEndMinute.toFloat(), onValueChange = { scheduleEndMinute = it.toInt().coerceIn(0, 1439) }, valueRange = 0f..1439f)
                    Button(onClick = {
                        scope.launch {
                            repository.saveSchedule(BlockSchedule(packageName = packageName, startMinuteOfDay = scheduleStartMinute, endMinuteOfDay = scheduleEndMinute, daysOfWeekBitmask = scheduleDays))
                            showAddSchedule = false
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save Schedule") }
                }
            }
        }

        schedules.forEach { schedule ->
            val timeStr = "${schedule.startMinuteOfDay / 60}:${"%02d".format(schedule.startMinuteOfDay % 60)} - ${schedule.endMinuteOfDay / 60}:${"%02d".format(schedule.endMinuteOfDay % 60)}"
            val dayStr = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").filterIndexed { i, _ -> schedule.daysOfWeekBitmask and (1 shl i) != 0 }.joinToString(", ")
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(timeStr, style = MaterialTheme.typography.bodyMedium)
                        Text(dayStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { scope.launch { repository.deleteSchedule(schedule.id) } }) { Text("Delete") }
                }
            }
        }

        HorizontalDivider()

        Button(
            onClick = {
                current?.let {
                    scope.launch {
                        repository.saveMonitoredApp(it)
                        onBack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save Changes") }
    }
}

@Composable
private fun IconToggleCard(
    icon: @Composable () -> Unit,
    label: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            icon
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
