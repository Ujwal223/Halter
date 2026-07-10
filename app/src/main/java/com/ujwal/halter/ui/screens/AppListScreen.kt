// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ujwal.halter.data.HalterRepository
import com.ujwal.halter.data.InstalledApp
import com.ujwal.halter.data.MonitoredApp
import com.ujwal.halter.settings.SettingsRepository
import com.ujwal.halter.ui.components.LazyColumnScrollbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

private const val PAGE_SIZE = 50

@Composable
fun AppListScreen(onOpenApp: (String) -> Unit) {
    val repository: HalterRepository = koinInject()
    val settingsRepository: SettingsRepository = koinInject()
    val monitored by repository.observeMonitoredApps().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }
    var installed by remember { mutableStateOf(emptyList<InstalledApp>()) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var displayLimit by remember { mutableIntStateOf(PAGE_SIZE) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= displayLimit - 10
        }
    }
    if (shouldLoadMore && displayLimit < installed.size) {
        LaunchedEffect(shouldLoadMore) {
            displayLimit = (displayLimit + PAGE_SIZE).coerceAtMost(installed.size)
        }
    }

    LaunchedEffect(Unit) {
        installed = withContext(Dispatchers.Default) { repository.installedApps() }
    }

    val filtered = installed.filter {
        it.displayName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
    }
    val monitoredPkgs = monitored.map { it.packageName }.toSet()
    val monitoredList = filtered.filter { it.packageName in monitoredPkgs }
    val unmonitoredList = filtered.filter { it.packageName !in monitoredPkgs }
    val paginatedUnmonitored = if (query.isNotBlank()) unmonitoredList else unmonitoredList.take(displayLimit)

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                "Apps",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    displayLimit = PAGE_SIZE
                },
                placeholder = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            if (filtered.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val defaults = settingsRepository.settings.first()
                                val toAdd = filtered.filter { it.packageName !in monitoredPkgs }
                                toAdd.forEach { app ->
                                    repository.saveMonitoredApp(
                                        MonitoredApp(
                                            packageName = app.packageName,
                                            displayName = app.displayName,
                                            isFlaggedHarmful = defaults.breathingGateGlobalDefault,
                                            strictModeEnabled = defaults.strictModeGlobalDefault,
                                            partialShortVideoBlocked = defaults.blockShortVideoGlobalDefault,
                                            excludedFromFocus = defaults.excludeFromFocusGlobalDefault,
                                            dailyTimeLimitMinutes = defaults.defaultDailyTimeLimitMinutes,
                                            sessionTimeLimitMinutes = defaults.defaultSessionTimeLimitMinutes,
                                            scrollLimitPerSession = defaults.defaultScrollLimitPerSession,
                                            holdToOpenSeconds = defaults.defaultHoldToOpenSeconds
                                        )
                                    )
                                }
                                snackbar.showSnackbar("${toAdd.size} app(s) added")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Select All") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val toRemove = monitoredList.toList()
                                toRemove.forEach { repository.removeMonitoredApp(it.packageName) }
                                snackbar.showSnackbar("${toRemove.size} app(s) removed")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Deselect All") }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(end = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filtered.isEmpty() && query.isNotBlank()) {
                        item {
                            InfoCard(
                                icon = {
                                    Icon(
                                        Icons.Outlined.SearchOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                headline = "No results",
                                body = "Try a different search term."
                            )
                        }
                    }

                    if (monitoredList.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Monitored",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "${monitoredList.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }
                        items(monitoredList, key = { it.packageName }) { app ->
                            AppCard(
                                app = app,
                                isMonitored = true,
                                onTap = { onOpenApp(app.packageName) },
                                onToggle = { checked ->
                                    scope.launch {
                                        if (!checked) {
                                            repository.removeMonitoredApp(app.packageName)
                                            snackbar.showSnackbar("${app.displayName} removed")
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (paginatedUnmonitored.isNotEmpty()) {
                        item {
                            Text(
                                if (monitoredList.isEmpty()) "All Apps" else "Other Apps",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(paginatedUnmonitored, key = { it.packageName }) { app ->
                            AppCard(
                                app = app,
                                isMonitored = false,
                                onTap = { onOpenApp(app.packageName) },
                                onToggle = { checked ->
                                    scope.launch {
                                        if (checked) {
                                            val defaults = settingsRepository.settings.first()
                                            repository.saveMonitoredApp(
                                                MonitoredApp(
                                                    packageName = app.packageName,
                                                    displayName = app.displayName,
                                                    isFlaggedHarmful = defaults.breathingGateGlobalDefault,
                                                    strictModeEnabled = defaults.strictModeGlobalDefault,
                                                    partialShortVideoBlocked = defaults.blockShortVideoGlobalDefault,
                                                    excludedFromFocus = defaults.excludeFromFocusGlobalDefault,
                                                    dailyTimeLimitMinutes = defaults.defaultDailyTimeLimitMinutes,
                                                    sessionTimeLimitMinutes = defaults.defaultSessionTimeLimitMinutes,
                                                    scrollLimitPerSession = defaults.defaultScrollLimitPerSession,
                                                    holdToOpenSeconds = defaults.defaultHoldToOpenSeconds
                                                )
                                            )
                                            snackbar.showSnackbar("${app.displayName} added")
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (query.isBlank() && displayLimit < unmonitoredList.size) {
                        item {
                            Text(
                                "Scroll for more apps (${unmonitoredList.size - displayLimit} remaining)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    if (installed.isEmpty()) {
                        item {
                            InfoCard(
                                icon = {
                                    Icon(
                                        Icons.Outlined.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                headline = "No apps found",
                                body = "Ensure Halter has the 'Query all packages' permission in App Info or your device settings."
                            )
                        }
                    }
                }
                LazyColumnScrollbar(
                    listState = listState,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@Composable
private fun AppCard(
    app: InstalledApp,
    isMonitored: Boolean,
    onTap: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMonitored)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppIcon(
                packageName = app.packageName,
                modifier = Modifier.size(42.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    app.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
                if (app.isSystemApp) {
                    Text(
                        "System app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
            Switch(
                checked = isMonitored,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
