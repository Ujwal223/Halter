// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.service

import com.ujwal.halter.data.ContentType
import com.ujwal.halter.data.HalterRepository
import com.ujwal.halter.data.MonitoredApp
import com.ujwal.halter.settings.HalterSettings
import com.ujwal.halter.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

data class BlockDecision(
    val blocked: Boolean,
    val appName: String,
    val reason: String,
    val strict: Boolean
) {
    companion object {
        fun allowed(appName: String = "") = BlockDecision(false, appName, "", false)
    }
}

class BlockDecisionEngine(
    private val repository: HalterRepository,
    private val settingsRepository: SettingsRepository,
    private val selfPackageName: String
) {
    suspend fun decisionForForeground(packageName: String, nowMillis: Long = System.currentTimeMillis()): BlockDecision {
        val app = repository.getMonitoredApp(packageName) ?: return BlockDecision.allowed()
        val activeFocus = repository.activeFocusSession()
        if (activeFocus != null && !activeFocus.completed) {
            if (packageName == selfPackageName || packageName.contains(".settings")) {
                return BlockDecision.allowed(app.displayName)
            }
            if (!app.excludedFromFocus) {
                repository.incrementFocusInterruptions(activeFocus.id)
                return BlockDecision(true, app.displayName, "Deep Focus is active", true)
            }
        }
        if (app.isInstantBlocked && (app.instantBlockUntilEpochMillis == null || app.instantBlockUntilEpochMillis > nowMillis)) {
            return BlockDecision(true, app.displayName, "Instant block active", app.strictModeEnabled)
        }
        if (app.cooldownUntilEpochMillis != null && app.cooldownUntilEpochMillis > nowMillis) {
            val remainingMillis = app.cooldownUntilEpochMillis - nowMillis
            val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis).coerceAtLeast(1)
            val remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
            val waitLabel = if (remainingMinutes >= 1 && remainingSeconds == 0L) {
                "${remainingMinutes}m"
            } else if (remainingMinutes >= 1) {
                "${remainingMinutes}m ${remainingSeconds}s"
            } else {
                "${TimeUnit.MILLISECONDS.toSeconds(remainingMillis).coerceAtLeast(1)}s"
            }
            return BlockDecision(
                true,
                app.displayName,
                "Your session limit is over. Wait $waitLabel to use this app again.",
                app.strictModeEnabled
            )
        }
        if (isScheduled(app.packageName, nowMillis)) {
            return BlockDecision(true, app.displayName, "Scheduled block active", app.strictModeEnabled)
        }
        if (isRoutineBlocked(app.packageName, nowMillis)) {
            return BlockDecision(true, app.displayName, "Blocked by routine schedule", app.strictModeEnabled)
        }

        // Bedtime Block
        val settings = settings()
        if (settings.bedtimeEnabled) {
            val now = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
            val minute = now.hour * 60 + now.minute
            val startMinute = settings.bedtimeStartHour * 60 + settings.bedtimeStartMinute
            val endMinute = settings.bedtimeEndHour * 60 + settings.bedtimeEndMinute
            if (ScheduleRules.isMinuteInWindow(minute, startMinute, endMinute)) {
                return BlockDecision(true, app.displayName, "Bedtime block active", app.strictModeEnabled)
            }
        }

        val dailyLimit = app.dailyTimeLimitMinutes
        if (dailyLimit != null && repository.totalUsageMillisToday(packageName, nowMillis) >= TimeUnit.MINUTES.toMillis(dailyLimit.toLong())) {
            return BlockDecision(true, app.displayName, "Daily limit reached", app.strictModeEnabled)
        }
        val active = repository.activeSessionFor(packageName)
        val sessionLimit = when {
            active?.limitType == com.ujwal.halter.data.LimitType.TIME -> active.chosenSessionLimit
            active == null -> app.sessionTimeLimitMinutes
            else -> null
        }
        if (active != null && sessionLimit != null &&
            repository.sessionConsumedMillis(active, nowMillis) >= TimeUnit.MINUTES.toMillis(sessionLimit.toLong())
        ) {
            return BlockDecision(true, app.displayName, "Session time limit reached. Take a break.", app.strictModeEnabled)
        }
        return BlockDecision.allowed(app.displayName)
    }

    suspend fun decisionForScroll(packageName: String, contentType: ContentType, nowMillis: Long = System.currentTimeMillis()): BlockDecision {
        val app = repository.getMonitoredApp(packageName) ?: return BlockDecision.allowed()
        if (app.partialShortVideoBlocked && contentType.isShortVideo()) {
            return BlockDecision(true, app.displayName, "Short-video surface blocked", app.strictModeEnabled)
        }
        val active = repository.activeSessionFor(packageName)
        val sessionScrollLimit = active?.chosenSessionLimit ?: app.scrollLimitPerSession
        if (sessionScrollLimit != null && (active?.scrollsUsed ?: 0) >= sessionScrollLimit) {
            return BlockDecision(true, app.displayName, "Scroll limit reached", app.strictModeEnabled)
        }
        val dailyScrollLimit = app.scrollLimitPerDay
        if (dailyScrollLimit != null && repository.scrollsToday(packageName, nowMillis) >= dailyScrollLimit) {
            return BlockDecision(true, app.displayName, "Daily scroll limit reached", app.strictModeEnabled)
        }
        return BlockDecision.allowed(app.displayName)
    }

    suspend fun settings(): HalterSettings = settingsRepository.settings.first()

    private suspend fun isRoutineBlocked(packageName: String, nowMillis: Long): Boolean {
        val now = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
        val minute = now.hour * 60 + now.minute
        val dayBit = ScheduleRules.dayBitForIsoDay(now.dayOfWeek.value)
        return repository.enabledRoutines().any { routine ->
            val apps = routine.packageNames.split(",")
            packageName in apps &&
            routine.daysOfWeekBitmask and dayBit != 0 &&
            ScheduleRules.isMinuteInWindow(minute, routine.startMinuteOfDay, routine.endMinuteOfDay)
        }
    }

    private suspend fun isScheduled(packageName: String, nowMillis: Long): Boolean {
        val now = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault())
        val minute = now.hour * 60 + now.minute
        val dayBit = ScheduleRules.dayBitForIsoDay(now.dayOfWeek.value)
        return repository.schedulesFor(packageName).any { schedule ->
            val dayMatches = schedule.daysOfWeekBitmask and dayBit != 0
            dayMatches && ScheduleRules.isMinuteInWindow(minute, schedule.startMinuteOfDay, schedule.endMinuteOfDay)
        }
    }
}

fun ContentType.isShortVideo(): Boolean = this == ContentType.REEL || this == ContentType.SHORT

object ScheduleRules {
    fun dayBitForIsoDay(isoDay: Int): Int = 1 shl ((isoDay + 6) % 7)

    fun isMinuteInWindow(minute: Int, startMinute: Int, endMinute: Int): Boolean =
        if (startMinute <= endMinute) {
            minute in startMinute until endMinute
        } else {
            minute >= startMinute || minute < endMinute
        }
}
