// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DoNotDisturb
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.ujwal.halter.data.FocusSession
import com.ujwal.halter.data.HalterRepository
import com.ujwal.halter.data.MonitoredApp
import com.ujwal.halter.settings.HalterSettings
import com.ujwal.halter.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun FocusSessionScreen() {
    val context = LocalContext.current
    val repository: HalterRepository = koinInject()
    val settingsRepository: SettingsRepository = koinInject()
    val settings by settingsRepository.settings.collectAsState(initial = HalterSettings())
    val sessions by repository.observeFocusSessions().collectAsState(initial = emptyList())
    val monitored by repository.observeMonitoredApps().collectAsState(initial = emptyList())
    var minutes by remember(settings.defaultFocusSessionMinutes) { mutableStateOf(settings.defaultFocusSessionMinutes.toString()) }
    val scope = rememberCoroutineScope()
    val active = sessions.firstOrNull { !it.completed }
    var showExcludePicker by remember { mutableStateOf(false) }
    var excludedPkgs by remember { mutableStateOf(monitored.filter { it.excludedFromFocus }.map { it.packageName }.toSet()) }

    // Refresh excluded set when monitored list changes
    LaunchedEffect(monitored) {
        excludedPkgs = monitored.filter { it.excludedFromFocus }.map { it.packageName }.toSet()
    }

    // Track DND permission state reactively — check immediately on composition,
    // then poll, so returning from Settings dismisses the "Grant" button immediately.
    var dndGranted by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        // Immediate first check
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        dndGranted = nm.isNotificationPolicyAccessGranted
        // Then poll every 2s
        while (true) {
            delay(2000L)
            dndGranted = nm.isNotificationPolicyAccessGranted
        }
    }
    // Also re-check on lifecycle resume (when returning from DND permission settings)
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                dndGranted = nm.isNotificationPolicyAccessGranted
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Focus", style = MaterialTheme.typography.headlineMedium) }

        if (active == null) {
            // ── Duration ──
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Session duration", style = MaterialTheme.typography.titleSmall)
                        }
                        OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, label = { Text("Minutes") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                }
            }

            // ── Excluded Apps ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showExcludePicker = !showExcludePicker },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Allowed during Focus", style = MaterialTheme.typography.titleSmall)
                            val excluded = monitored.filter { it.excludedFromFocus }
                            Text(if (excluded.isEmpty()) "All apps blocked" else "${excluded.size} app(s) allowed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (showExcludePicker) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                AnimatedVisibility(showExcludePicker) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tap to toggle which apps stay usable during focus:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            monitored.forEach { app ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    AppIcon(packageName = app.packageName, modifier = Modifier.size(36.dp))
                                    Text(app.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Switch(checked = app.excludedFromFocus, onCheckedChange = { checked ->
                                        scope.launch {
                                            repository.saveMonitoredApp(app.copy(excludedFromFocus = checked))
                                            if (checked) excludedPkgs = excludedPkgs + app.packageName
                                            else excludedPkgs = excludedPkgs - app.packageName
                                        }
                                    })
                                }
                            }
                            if (monitored.isEmpty()) {
                                Text("No apps monitored yet. Add apps from the Apps tab.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ── DND Toggle ──
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.DoNotDisturb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text("Do Not Disturb", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (dndGranted) "Silence notifications during focus. Returns to previous state after."
                                    else "Permission needed to silence notifications during focus.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (dndGranted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (!dndGranted) {
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                },
                                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                            ) { Text("Grant DND Permission") }
                        }
                    }
                }
            }

            // ── Start button ──
            item {
                Button(
                    onClick = {
                        scope.launch {
                            // Enable DND (silence notifications during focus)
                            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            if (nm.isNotificationPolicyAccessGranted) {
                                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                            }
                            repository.saveFocusSession(
                                FocusSession(
                                    startEpochMillis = System.currentTimeMillis(),
                                    durationMinutes = minutes.toIntOrNull() ?: settings.defaultFocusSessionMinutes,
                                    completed = false
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Start Deep Focus") }
            }

            // Help text
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f))) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                        Text("During Deep Focus, all monitored apps are hard-blocked except those you allow above. No overrides. No shortcuts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        } else {
            // ── Active session ──
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f))) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.DoNotDisturb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Text("Focus Active", style = MaterialTheme.typography.headlineSmall)
                        Text("${active.durationMinutes} minutes · ${active.interruptionCount} interruptions", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("All monitored apps are blocked (except those you allowed).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = {
                                scope.launch {
                                    // Restore DND
                                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    if (nm.isNotificationPolicyAccessGranted) {
                                        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                                    }
                                    repository.saveFocusSession(active.copy(completed = true))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("End Focus Session") }
                    }
                }
            }
        }

        // ── Past sessions ──
        if (sessions.isNotEmpty()) {
            item { Text("Past Sessions", style = MaterialTheme.typography.titleLarge) }
            items(sessions.take(10)) { session ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(if (session.completed) Icons.Outlined.PlayArrow else Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Column {
                            Text("${session.durationMinutes} min", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (session.completed) "Completed · ${session.interruptionCount} interruptions"
                                else "Active now",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
