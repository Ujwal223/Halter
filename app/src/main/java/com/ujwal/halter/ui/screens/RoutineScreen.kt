// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.ui.screens

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ujwal.halter.data.HalterRepository
import com.ujwal.halter.data.InstalledApp
import com.ujwal.halter.data.Routine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(onBack: () -> Unit) {
    val repository: HalterRepository = koinInject()
    val routines by repository.observeRoutines().collectAsState(initial = emptyList())
    var installed by remember { mutableStateOf(emptyList<InstalledApp>()) }
    val scope = rememberCoroutineScope()

    // New routine dialog state
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var selectedApps by remember { mutableStateOf(emptySet<String>()) }
    var startHour by remember { mutableIntStateOf(8) }
    var startMin by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(18) }
    var endMin by remember { mutableIntStateOf(0) }
    var daysBitmask by remember { mutableIntStateOf(0x7F) } // all days
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        installed = withContext(Dispatchers.Default) { repository.installedApps() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routines") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("   Create Routine")
                }
            }

            if (routines.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No routines yet", style = MaterialTheme.typography.titleMedium)
                            Text("Create routines to automatically block selected apps during specific times.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(routines) { routine ->
                val apps = routine.packageNames.split(",")
                val appNames = apps.mapNotNull { pkg -> installed.firstOrNull { it.packageName == pkg }?.displayName ?: pkg }
                val timeStr = "${routine.startMinuteOfDay / 60}:${"%02d".format(routine.startMinuteOfDay % 60)} - ${routine.endMinuteOfDay / 60}:${"%02d".format(routine.endMinuteOfDay % 60)}"
                val dayStr = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").filterIndexed { i, _ -> routine.daysOfWeekBitmask and (1 shl i) != 0 }.joinToString(", ")

                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(routine.name, style = MaterialTheme.typography.titleSmall)
                            Text("$timeStr · $dayStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${appNames.size} app(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { scope.launch { repository.deleteRoutine(routine.id) } }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    // ── Create Routine Dialog ──
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Create Routine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(newName, { newName = it }, label = { Text("Routine name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Text("Select apps", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(modifier = Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(installed.take(30)) { app ->
                            Row(Modifier.fillMaxWidth().clickable {
                                selectedApps = if (app.packageName in selectedApps) selectedApps - app.packageName else selectedApps + app.packageName
                            }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppIcon(packageName = app.packageName, modifier = Modifier.size(32.dp))
                                Text(app.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Switch(checked = app.packageName in selectedApps, onCheckedChange = null)
                            }
                        }
                    }

                    Text("Days", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Mon" to 0, "Tue" to 1, "Wed" to 2, "Thu" to 3, "Fri" to 4, "Sat" to 5, "Sun" to 6).forEach { (label, bit) ->
                            FilterChip(onClick = { daysBitmask = daysBitmask xor (1 shl bit) }, label = { Text(label) }, selected = daysBitmask and (1 shl bit) != 0)
                        }
                    }

                    Text("Start time", style = MaterialTheme.typography.titleSmall)
                    Button(onClick = { showStartPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${startHour}:${"%02d".format(startMin)} ${if (startHour < 12) "AM" else "PM"}")
                    }
                    Text("End time", style = MaterialTheme.typography.titleSmall)
                    Button(onClick = { showEndPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${endHour}:${"%02d".format(endMin)} ${if (endHour < 12) "AM" else "PM"}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank() && selectedApps.isNotEmpty()) {
                        scope.launch {
                            repository.saveRoutine(
                                Routine(
                                    name = newName,
                                    packageNames = selectedApps.joinToString(","),
                                    startMinuteOfDay = startHour * 60 + startMin,
                                    endMinuteOfDay = endHour * 60 + endMin,
                                    daysOfWeekBitmask = daysBitmask
                                )
                            )
                            showCreate = false
                            newName = ""
                            selectedApps = emptySet()
                            daysBitmask = 0x7F
                            startHour = 8; startMin = 0; endHour = 18; endMin = 0
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
}
