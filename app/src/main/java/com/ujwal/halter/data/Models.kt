// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val packageNames: String, // comma-separated
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val daysOfWeekBitmask: Int,
    val isEnabled: Boolean = true
)

@Entity(tableName = "monitored_apps", indices = [Index("isFlaggedHarmful")])
data class MonitoredApp(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val isFlaggedHarmful: Boolean = false,
    val dailyTimeLimitMinutes: Int? = null,
    val sessionTimeLimitMinutes: Int? = null,
    val scrollLimitPerSession: Int? = null,
    val scrollLimitPerDay: Int? = null,
    val strictModeEnabled: Boolean = false,
    val category: AppCategory = AppCategory.OTHER,
    val isInstantBlocked: Boolean = false,
    val instantBlockUntilEpochMillis: Long? = null,
    val cooldownUntilEpochMillis: Long? = null,
    val partialShortVideoBlocked: Boolean = false,
    val holdToOpenSeconds: Int? = null,
    val excludedFromFocus: Boolean = false
)

enum class AppCategory { SOCIAL, SHORT_VIDEO, GAMES, NEWS, SHOPPING, PRODUCTIVITY, OTHER }

@Entity(
    tableName = "scroll_events",
    indices = [Index("packageName"), Index("timestampEpochMillis"), Index(value = ["packageName", "timestampEpochMillis"])]
)
data class ScrollEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestampEpochMillis: Long,
    val contentType: ContentType,
    val count: Int = 1
)

enum class ContentType { REEL, SHORT, FEED, UNKNOWN }

@Entity(
    tableName = "usage_sessions",
    indices = [Index("packageName"), Index("startEpochMillis"), Index("endEpochMillis"), Index(value = ["packageName", "endEpochMillis"])]
)
data class UsageSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long?,
    val scrollsUsed: Int = 0,
    val chosenSessionLimit: Int?,
    val limitType: LimitType,
    val accumulatedUsageMillis: Long = 0,
    val lastForegroundStartEpochMillis: Long? = null
)

enum class LimitType { TIME, SCROLL_COUNT }

@Entity(tableName = "block_schedules", indices = [Index("packageName"), Index(value = ["packageName", "isEnabled"])])
data class BlockSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val daysOfWeekBitmask: Int,
    val isEnabled: Boolean = true
)

@Entity(tableName = "focus_sessions", indices = [Index("completed"), Index("startEpochMillis")])
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMillis: Long,
    val durationMinutes: Int,
    val completed: Boolean,
    val interruptionCount: Int = 0
)

@Entity(tableName = "journal_entries", indices = [Index("packageName"), Index("timestampEpochMillis")])
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val timestampEpochMillis: Long,
    val reason: JournalReason
)

enum class JournalReason { BOREDOM, HABIT, ACTUAL_NEED, NOTIFICATION }
