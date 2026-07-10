// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Brightness7
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterCenterFocus
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.HorizontalSplit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.FilterBAndW

import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ujwal.halter.data.HalterRepository
import com.ujwal.halter.settings.DarkModePreference
import com.ujwal.halter.settings.HalterSettings
import com.ujwal.halter.settings.SettingsRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.security.MessageDigest
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.mutableFloatStateOf
import android.content.pm.PackageManager
import com.ujwal.halter.utils.ShizukuHelper
import com.ujwal.halter.ui.util.formatDurationMillis

@Composable
fun SettingsScreen(onOpenJournal: () -> Unit, onOpenReports: () -> Unit = {}, onOpenRoutines: () -> Unit = {}) {
    val settingsRepository: SettingsRepository = koinInject()
    val settings by settingsRepository.settings.collectAsState(initial = HalterSettings())
    val scope = rememberCoroutineScope()

    // Per-action password gate state
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pwDialogInput by remember { mutableStateOf("") }
    var pwDialogError by remember { mutableStateOf<String?>(null) }
    var lastPwAuthMs by remember { mutableLongStateOf(0L) }

    fun requirePasswordFor(action: () -> Unit) {
        if (settings.requirePasswordForSettingsChanges && settings.settingsPasswordHash != null) {
            // Cache auth for 30 seconds to avoid re-prompting on rapid changes
            if (System.currentTimeMillis() - lastPwAuthMs < 30_000) {
                action()
                return
            }
            pendingAction = {
                lastPwAuthMs = System.currentTimeMillis()
                action()
            }
            pwDialogInput = ""
            pwDialogError = null
        } else {
            action()
        }
    }

    // Reports data — today only
    val halterRepository: HalterRepository = koinInject()
    val todayStart = remember {
        val zone = java.time.ZoneId.systemDefault()
        java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    }
    val sessions by halterRepository.observeRecentUsage(todayStart).collectAsState(initial = emptyList())
    // Only count time for closed sessions (endEpochMillis != null) to avoid inflating time
    // for sessions that are still active (which would add 'now - start' incorrectly).
    val totalMillis = sessions.sumOf { s ->
        val live = if (s.endEpochMillis == null && s.lastForegroundStartEpochMillis != null)
            (System.currentTimeMillis() - s.lastForegroundStartEpochMillis).coerceAtLeast(0L) else 0L
        s.accumulatedUsageMillis + live
    }
    // Scroll tracking disabled — totalScrolls not displayed
    // val scrolls by halterRepository.observeRecentScrolls(todayStart).collectAsState(initial = emptyList())
    // val totalScrolls = scrolls.sumOf { it.count }

    // Password dialog state
    var showPasswordDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var dialogPwError by remember { mutableStateOf<String?>(null) }

    // Section collapse state
    var advancedExpanded by remember { mutableStateOf(false) }
    var appearanceExpanded by remember { mutableStateOf(false) }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false; newPassword = ""; confirmPassword = ""; dialogPwError = null },
            title = { Text(if (settings.settingsPasswordHash == null) "Set Password" else "Change Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (settings.settingsPasswordHash != null) {
                        Text("Current password is set. Enter a new one to change, or leave blank to remove.")
                    }
                    OutlinedTextField(newPassword, { newPassword = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(confirmPassword, { confirmPassword = it }, label = { Text("Confirm password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (dialogPwError != null) Text(dialogPwError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPassword != confirmPassword) {
                        dialogPwError = "Passwords do not match"
                    } else {
                        scope.launch {
                            val hash = if (newPassword.isNotEmpty()) sha256(newPassword) else null
                            settingsRepository.updateString(SettingsRepository.Names.settingsPasswordHash, hash)
                            if (hash == null) settingsRepository.updateBoolean(SettingsRepository.Names.requirePasswordForSettingsChanges, false)
                            showPasswordDialog = false
                            newPassword = ""
                            confirmPassword = ""
                            dialogPwError = null
                        }
                    }
                }) { Text(if (newPassword.isEmpty() && settings.settingsPasswordHash != null) "Remove Password" else "Save") }
            },
            dismissButton = { TextButton(onClick = { showPasswordDialog = false; newPassword = ""; confirmPassword = ""; dialogPwError = null }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        // ── Quick Appearance Toggle ──
        Card(
            modifier = Modifier.fillMaxWidth().clickable { appearanceExpanded = !appearanceExpanded },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        ) {
            Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (settings.darkModePreference == DarkModePreference.DARK) Icons.Outlined.Brightness4 else Icons.Outlined.Brightness7,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleSmall)
                    Text(
                        when (settings.darkModePreference) { DarkModePreference.SYSTEM -> "Follow system"; DarkModePreference.LIGHT -> "Light"; DarkModePreference.DARK -> "Dark" },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(DarkModePreference.LIGHT, DarkModePreference.SYSTEM, DarkModePreference.DARK).forEach { pref ->
                        IconButton(onClick = { requirePasswordFor { scope.launch { settingsRepository.updateString(SettingsRepository.Names.darkModePreference, pref.name) } } }) {
                            Icon(
                                when (pref) { DarkModePreference.LIGHT -> Icons.Outlined.Brightness7; DarkModePreference.SYSTEM -> Icons.Outlined.HorizontalSplit; DarkModePreference.DARK -> Icons.Outlined.Brightness4 },
                                contentDescription = pref.name,
                                tint = if (settings.darkModePreference == pref) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(appearanceExpanded) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BoolSetting("Use wallpaper colors", null, settings.useDynamicWallpaperColor) {
                        requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.useDynamicWallpaperColor, it) } }
                    }
                    BoolSetting("Blur effects", "Adds glass-like blur to overlays.", settings.blurEffectsEnabled) {
                        requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.blurEffectsEnabled, it) } }
                    }
                }
            }
        }

        HorizontalDivider()

        // ── Breathing Timer (clickable to expand) ──
        var breathExpanded by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth().clickable { breathExpanded = !breathExpanded }
        ) {
            Column {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Breathing Timer", style = MaterialTheme.typography.titleSmall)
                        Text("${settings.breathingTotalDurationSeconds}s total · Tap to adjust", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(if (breathExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AnimatedVisibility(breathExpanded) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        IntSetting("Total duration (seconds)", settings.breathingTotalDurationSeconds) {
                            requirePasswordFor { scope.launch { settingsRepository.updateInt(SettingsRepository.Names.breathingTotalDurationSeconds, it) } }
                        }
                        BoolSetting("Allow skip", "Lets users tap 'Skip'.", settings.allowSkipBreathing) {
                            requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.allowSkipBreathing, it) } }
                        }
                        BoolSetting("Haptics", "Gentle vibration on each breathing phase.", settings.hapticsEnabled) {
                            requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.hapticsEnabled, it) } }
                        }
                    }
                }
            }
        }

        // ── Productivity Filters (clickable to expand) ──
        var filterExpanded by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth().clickable { filterExpanded = !filterExpanded }
        ) {
            Column {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.FilterAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Productivity Filters", style = MaterialTheme.typography.titleSmall)
                        Text("Site & Keyword Blocking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(if (filterExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AnimatedVisibility(filterExpanded) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BoolSetting("Site Blocking", "Prevents visiting specific websites in browsers.", settings.siteBlockingEnabled) {
                            requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.siteBlockingEnabled, it) } }
                        }
                        AnimatedVisibility(settings.siteBlockingEnabled) {
                            StringSetting(
                                label = "Blocked Sites (comma-separated)",
                                placeholder = "facebook.com, twitter.com, reddit.com",
                                value = settings.siteBlockedList
                            ) {
                                requirePasswordFor { scope.launch { settingsRepository.updateString(SettingsRepository.Names.siteBlockedList, it) } }
                            }
                        }
                        
                        BoolSetting("Keyword Blocking", "Blocks screens containing specific keywords.", settings.keywordBlockingEnabled) {
                            requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.keywordBlockingEnabled, it) } }
                        }
                        AnimatedVisibility(settings.keywordBlockingEnabled) {
                            StringSetting(
                                label = "Blocked Keywords (comma-separated)",
                                placeholder = "shorts, reels, game, addict",
                                value = settings.keywordBlockedList
                            ) {
                                requirePasswordFor { scope.launch { settingsRepository.updateString(SettingsRepository.Names.keywordBlockedList, it) } }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()



        // ── Focus Sessions (clickable to expand) ──
        var focusExpanded by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth().clickable { focusExpanded = !focusExpanded }
        ) {
            Column {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Deep Focus", style = MaterialTheme.typography.titleSmall)
                        Text("${settings.defaultFocusSessionMinutes} min default · Tap to adjust", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(if (focusExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AnimatedVisibility(focusExpanded) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        IntSetting("Default duration (minutes)", settings.defaultFocusSessionMinutes) {
                            requirePasswordFor { scope.launch { settingsRepository.updateInt(SettingsRepository.Names.defaultFocusSessionMinutes, it) } }
                        }
                        Text("During Deep Focus, all monitored apps are blocked. Exclude specific apps from each session on the Focus page.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ── Bedtime & Greyscale (clickable to expand) ──
        var bedtimeExpanded by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth().clickable { bedtimeExpanded = !bedtimeExpanded }
        ) {
            Column {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.NightsStay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Bedtime & Greyscale", style = MaterialTheme.typography.titleSmall)
                        Text("Sleep mode blocker & screen greyscale", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(if (bedtimeExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AnimatedVisibility(bedtimeExpanded) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BoolSetting("Bedtime Reminder", "Block monitored apps during bedtime.", settings.bedtimeEnabled) {
                            requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.bedtimeEnabled, it) } }
                        }
                        AnimatedVisibility(settings.bedtimeEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SwipeableTimePicker(
                                    label = "Bedtime Start",
                                    hour = settings.bedtimeStartHour,
                                    minute = settings.bedtimeStartMinute,
                                    onHourChange = { requirePasswordFor { scope.launch { settingsRepository.updateInt(SettingsRepository.Names.bedtimeStartHour, it) } } },
                                    onMinuteChange = { requirePasswordFor { scope.launch { settingsRepository.updateInt(SettingsRepository.Names.bedtimeStartMinute, it) } } }
                                )
                                SwipeableTimePicker(
                                    label = "Bedtime End",
                                    hour = settings.bedtimeEndHour,
                                    minute = settings.bedtimeEndMinute,
                                    onHourChange = { requirePasswordFor { scope.launch { settingsRepository.updateInt(SettingsRepository.Names.bedtimeEndHour, it) } } },
                                    onMinuteChange = { requirePasswordFor { scope.launch { settingsRepository.updateInt(SettingsRepository.Names.bedtimeEndMinute, it) } } }
                                )
                            }
                        }

                        BoolSetting("Greyscale Mode", "Turn screen monochrome (black & white).", settings.greyscaleEnabled) {
                            requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.greyscaleEnabled, it) } }
                        }
                        
                        val context = LocalContext.current
                        val hasPermission = remember(Unit) {
                            ShizukuHelper.hasWriteSecureSettings(context)
                        }
                        var grantStatus by remember { mutableStateOf(if (hasPermission) "granted" else "idle") }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (grantStatus) {
                                    "granted" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    "error"   -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                    else      -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        if (grantStatus == "granted") Icons.Outlined.Lock else Icons.Outlined.SmartToy,
                                        contentDescription = null,
                                        tint = if (grantStatus == "granted") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        if (grantStatus == "granted") "Secure Settings: Granted ✓"
                                        else "Secure Settings Permission",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                when (grantStatus) {
                                    "granted" -> Text(
                                        "Greyscale mode is fully enabled.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    else -> {
                                        Text(
                                            "Grant via Shizuku for automatic setup, or run the ADB command manually.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val shizukuRunning = remember(Unit) { ShizukuHelper.isShizukuRunning() }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (shizukuRunning) {
                                                Button(
                                                    onClick = {
                                                        val activity = context as? android.app.Activity
                                                        if (activity != null) {
                                                            ShizukuHelper.requestPermissionAndGrant(activity) { ok ->
                                                                grantStatus = if (ok) "granted" else "error"
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Grant via Shizuku") }
                                            } else {
                                                Button(
                                                    onClick = { ShizukuHelper.launchShizuku(context) },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Open Shizuku") }
                                            }
                                        }
                                        Text(
                                            "Manual ADB:\nadb shell pm grant com.ujwal.halter android.permission.WRITE_SECURE_SETTINGS",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // ── Journal (compact toggle) ──
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onOpenJournal() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Journal", style = MaterialTheme.typography.titleSmall)
                    Text("Optional reflection prompts when opening apps.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = settings.journalPromptEnabled, onCheckedChange = { requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.journalPromptEnabled, it) } } })
            }
        }

        // ── Security (password gate) ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Security", style = MaterialTheme.typography.titleSmall)
                    Text(
                        when {
                            settings.requirePasswordForSettingsChanges -> "Password required for settings changes"
                            settings.settingsPasswordHash != null -> "Password saved but not required"
                            else -> "Lock settings behind a password"
                        },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.requirePasswordForSettingsChanges,
                    onCheckedChange = { enabled ->
                        if (enabled && settings.settingsPasswordHash == null) {
                            showPasswordDialog = true  // prompt to set password first
                        } else {
                            requirePasswordFor {
                                scope.launch {
                                    settingsRepository.updateBoolean(SettingsRepository.Names.requirePasswordForSettingsChanges, enabled)
                                }
                            }
                        }
                    }
                )
            }
            if (settings.settingsPasswordHash != null) {
                Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    TextButton(onClick = { showPasswordDialog = true }) {
                        Text("Change or remove password")
                    }
                }
            }
        }

        HorizontalDivider()

        // ── Your Stats (opens full page) ──
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onOpenReports() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f))
        ) {
            Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Your Stats", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${formatDurationMillis(totalMillis)} today",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }

        HorizontalDivider()

        // ── Session Cooldown (top-level, clearly visible) ──
        var cooldownExpanded by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth().clickable { cooldownExpanded = !cooldownExpanded },
            colors = CardDefaults.cardColors(
                containerColor = if (settings.sessionCooldownEnabled)
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f)
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Session Cooldown", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (settings.sessionCooldownEnabled)
                                "Apps blocked for ${settings.sessionCooldownMinutes} min after session ends"
                            else
                                "Disabled — no cooldown after session limit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.sessionCooldownEnabled,
                        onCheckedChange = {
                            requirePasswordFor {
                                scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.sessionCooldownEnabled, it) }
                            }
                        }
                    )
                }
                AnimatedVisibility(cooldownExpanded && settings.sessionCooldownEnabled) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        IntSetting("Cooldown duration (minutes)", settings.sessionCooldownMinutes) {
                            requirePasswordFor { scope.launch { settingsRepository.updateInt(SettingsRepository.Names.sessionCooldownMinutes, it) } }
                        }
                        Text(
                            "When the session timer reaches 0, the app is blocked for the cooldown period before you can open a new session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        HorizontalDivider()
        Card(
            modifier = Modifier.fillMaxWidth().clickable { advancedExpanded = !advancedExpanded },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Advanced", style = MaterialTheme.typography.titleSmall)
                    Text("Blocking defaults, session limits & cooldowns", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (advancedExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        AnimatedVisibility(advancedExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Blocking defaults (comprehensive — matches AppDetailScreen overrides)
                var blockingDefaultsExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { blockingDefaultsExpanded = !blockingDefaultsExpanded }
                ) {
                    Column {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.Block, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text("Blocking Defaults", style = MaterialTheme.typography.titleSmall)
                                Text("${if (settings.breathingGateGlobalDefault) "Breathing gate ON" else "Breathing gate OFF"} · Strict: ${if (settings.strictModeGlobalDefault) "ON" else "OFF"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(if (blockingDefaultsExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AnimatedVisibility(blockingDefaultsExpanded) {
                            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                BoolSetting("Allow custom session limit", "Shows a Custom option in the session picker when opening apps.", settings.allowCustomSessionLimit) {
                                    requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.allowCustomSessionLimit, it) } }
                                }
                                BoolSetting("Require breathing gate", "Shows a breathing pause + session picker before opening.", settings.breathingGateGlobalDefault) {
                                    requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.breathingGateGlobalDefault, it) } }
                                }
                                BoolSetting("Strict mode", "Blocks cannot be dismissed until they naturally expire.", settings.strictModeGlobalDefault) {
                                    requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.strictModeGlobalDefault, it) } }
                                }
                                BoolSetting("Feed Guard", "Blocks Reels, Shorts & vertical video feeds for new apps.", settings.blockShortVideoGlobalDefault) {
                                    requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.blockShortVideoGlobalDefault, it) } }
                                }
                                BoolSetting("Allow during Focus", "New apps stay usable during Deep Focus.", settings.excludeFromFocusGlobalDefault) {
                                    requirePasswordFor { scope.launch { settingsRepository.updateBoolean(SettingsRepository.Names.excludeFromFocusGlobalDefault, it) } }
                                }
                                IntSettingNullable("Daily time limit (min)", settings.defaultDailyTimeLimitMinutes) {
                                    val stored = it ?: -1
                                    requirePasswordFor { scope.launch { settingsRepository.updateInt(SettingsRepository.Names.defaultDailyTimeLimitMinutes, stored) } }
                                }
                                IntSettingNullable("Session time limit (min)", settings.defaultSessionTimeLimitMinutes) {
                                    val stored = it ?: -1
                                    requirePasswordFor { scope.launch { settingsRepository.updateInt(SettingsRepository.Names.defaultSessionTimeLimitMinutes, stored) } }
                                }
                                IntSettingNullable("Scroll limit per session", settings.defaultScrollLimitPerSession) {
                                    val stored = it ?: -1
                                    requirePasswordFor { scope.launch { settingsRepository.updateInt(SettingsRepository.Names.defaultScrollLimitPerSession, stored) } }
                                }
                                IntSettingNullable("Hold-to-open (seconds)", settings.defaultHoldToOpenSeconds) {
                                    val stored = it ?: -1
                                    requirePasswordFor { scope.launch { settingsRepository.updateInt(SettingsRepository.Names.defaultHoldToOpenSeconds, stored) } }
                                }
                                // Scroll limit setting hidden — scroll tracking is disabled
                                // IntSettingNullable("Scroll limit per session", settings.defaultScrollLimitPerSession) { ... }
                                Text("These apply to any app newly added to the monitored list. Existing apps keep their per-app settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Custom scroll packages card removed — scroll detection tuning is disabled
                // Users can still use the global Feed Guard toggle per-app.
            }
        }

        HorizontalDivider()

        // ── Routines ──
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onOpenRoutines() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f))
        ) {
            Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Routine Schedule Blocks", style = MaterialTheme.typography.titleSmall)
                    Text("Block selected apps on a repeating schedule.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }

        // ── How App Works ──
        Card(
            modifier = Modifier.fillMaxWidth().clickable { /* expandable — keep as reference */ },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.20f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("How App Works", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text("1", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Add apps", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("From the Apps tab. Defaults from Settings apply automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text("2", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Customize per app", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Tap any app to set breathing gate, time limits, strict mode, and more.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text("3", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Automatic enforcement", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Limits activate when you open an app. Blocks and scroll detection run locally.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // ── Per-action password prompt dialog ──
    if (pendingAction != null && settings.requirePasswordForSettingsChanges && settings.settingsPasswordHash != null) {
        AlertDialog(
            onDismissRequest = { pendingAction = null; pwDialogInput = ""; pwDialogError = null },
            title = { Text("Confirm Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter your password to make this change.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = pwDialogInput,
                        onValueChange = { pwDialogInput = it; pwDialogError = null },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    if (pwDialogError != null) {
                        Text(pwDialogError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (sha256(pwDialogInput) == settings.settingsPasswordHash) {
                        pendingAction?.invoke()
                        pendingAction = null
                        pwDialogInput = ""
                        pwDialogError = null
                    } else {
                        pwDialogError = "Incorrect password"
                    }
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { pendingAction = null; pwDialogInput = ""; pwDialogError = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun IntSettingNullable(label: String, value: Int?, onChange: (Int?) -> Unit) {
    var text by remember(value) { mutableStateOf(value?.toString() ?: "") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit) },
                placeholder = { Text("No default") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (text.isNotEmpty()) {
                        text.toIntOrNull()?.let { onChange(it) }
                    } else {
                        onChange(null)
                    }
                },
                enabled = text.isNotEmpty()
            ) { Text("Set") }
            TextButton(onClick = { text = ""; onChange(null) }) { Text("Clear") }
        }
    }
}

@Composable
private fun BoolSetting(
    label: String,
    description: String?,
    value: Boolean,
    onChange: (Boolean) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = value, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun IntSetting(label: String, value: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { raw -> raw.filter(Char::isDigit).toIntOrNull()?.let(onChange) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

private fun sha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

@Composable
private fun StringSetting(label: String, placeholder: String, value: String, onChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = { onChange(text) },
                enabled = text != value
            ) { Text("Save") }
        }
    }
}

/**
 * A premium swipeable time picker. Drag the hours box left/right to change hours,
 * drag the minutes box left/right to change minutes (5-min steps). Haptic feedback
 * on every tick for a satisfying, tactile feel.
 */
@Composable
private fun SwipeableTimePicker(
    label: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var hourDrag by remember { mutableFloatStateOf(0f) }
    var minuteDrag by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Hours ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(hour) {
                        detectHorizontalDragGestures(
                            onDragStart = { hourDrag = 0f },
                            onHorizontalDrag = { change, delta ->
                                change.consume()
                                hourDrag += delta
                                val step = 30f
                                if (kotlin.math.abs(hourDrag) >= step) {
                                    val ticks = (hourDrag / step).toInt()
                                    hourDrag -= ticks * step
                                    val newH = ((hour - ticks) % 24 + 24) % 24
                                    onHourChange(newH)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                    }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%02d", hour),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "\u2190 Hours \u2192",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                ":",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            // ── Minutes ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(minute) {
                        detectHorizontalDragGestures(
                            onDragStart = { minuteDrag = 0f },
                            onHorizontalDrag = { change, delta ->
                                change.consume()
                                minuteDrag += delta
                                val step = 20f
                                if (kotlin.math.abs(minuteDrag) >= step) {
                                    val ticks = (minuteDrag / step).toInt()
                                    minuteDrag -= ticks * step
                                    val newMin = ((minute - ticks * 5) % 60 + 60) % 60
                                    onMinuteChange(newMin)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        )
                    }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%02d", minute),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "\u2190 Min \u2192",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
