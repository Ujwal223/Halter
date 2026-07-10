// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.ujwal.halter.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

data class InstalledApp(
    val packageName: String,
    val displayName: String,
    val isSystemApp: Boolean
)

class HalterRepository(
    private val context: Context,
    private val monitoredAppDao: MonitoredAppDao,
    private val usageSessionDao: UsageSessionDao,
    private val scrollEventDao: ScrollEventDao,
    private val blockScheduleDao: BlockScheduleDao,
    private val focusSessionDao: FocusSessionDao,
    private val journalDao: JournalDao,
    private val routineDao: RoutineDao,
    private val settingsRepository: SettingsRepository
) {
    fun observeMonitoredApps(): Flow<List<MonitoredApp>> = monitoredAppDao.observeAll()
    fun observeFlaggedApps(): Flow<List<MonitoredApp>> = monitoredAppDao.observeFlagged()
    fun observeFocusSessions(): Flow<List<FocusSession>> = focusSessionDao.observeAll()
    fun observeJournal(): Flow<List<JournalEntry>> = journalDao.observeAll()
    fun observeRecentUsage(fromMillis: Long): Flow<List<UsageSession>> = usageSessionDao.observeSince(fromMillis)
    fun observeRecentScrolls(fromMillis: Long): Flow<List<ScrollEvent>> = scrollEventDao.observeSince(fromMillis)

    suspend fun getMonitoredApp(packageName: String): MonitoredApp? = monitoredAppDao.get(packageName)
    suspend fun saveMonitoredApp(app: MonitoredApp) = monitoredAppDao.upsert(app)
    suspend fun removeMonitoredApp(packageName: String) = monitoredAppDao.delete(packageName)
    suspend fun activeFocusSession(): FocusSession? = focusSessionDao.active()
    suspend fun saveFocusSession(session: FocusSession): Long = focusSessionDao.upsert(session)
    suspend fun incrementFocusInterruptions(id: Long) = focusSessionDao.incrementInterruptions(id)
    suspend fun recordJournal(entry: JournalEntry) = journalDao.insert(entry)
    fun observeSchedulesFor(packageName: String): Flow<List<BlockSchedule>> = blockScheduleDao.observeForPackage(packageName)
    suspend fun schedulesFor(packageName: String): List<BlockSchedule> = blockScheduleDao.enabledForPackage(packageName)
    suspend fun saveSchedule(schedule: BlockSchedule): Long = blockScheduleDao.upsert(schedule)
    suspend fun deleteSchedule(id: Long) = blockScheduleDao.delete(id)
    fun observeRoutines(): Flow<List<Routine>> = routineDao.observeAll()
    suspend fun enabledRoutines(): List<Routine> = routineDao.enabled()
    suspend fun saveRoutine(routine: Routine): Long = routineDao.upsert(routine)
    suspend fun deleteRoutine(id: Long) = routineDao.delete(id)

    suspend fun startSession(packageName: String, chosenLimit: Int?, limitType: LimitType): Long =
        usageSessionDao.upsert(
            UsageSession(
                packageName = packageName,
                startEpochMillis = System.currentTimeMillis(),
                endEpochMillis = null,
                chosenSessionLimit = chosenLimit,
                limitType = limitType,
                lastForegroundStartEpochMillis = System.currentTimeMillis()
            )
        )

    suspend fun closeActiveSession(nowMillis: Long = System.currentTimeMillis()) {
        usageSessionDao.activeSession()?.let { usageSessionDao.close(it.id, nowMillis) }
    }

    suspend fun activeSessionFor(packageName: String): UsageSession? = usageSessionDao.activeForPackage(packageName)

    suspend fun pauseSession(packageName: String, nowMillis: Long = System.currentTimeMillis()) {
        val session = usageSessionDao.activeForPackage(packageName) ?: return
        val startedAt = session.lastForegroundStartEpochMillis ?: return
        val elapsed = (nowMillis - startedAt).coerceAtLeast(0)
        usageSessionDao.pauseUsage(session.id, elapsed)
    }

    suspend fun resumeSession(packageName: String, nowMillis: Long = System.currentTimeMillis()) {
        val session = usageSessionDao.activeForPackage(packageName) ?: return
        if (session.lastForegroundStartEpochMillis == null) {
            usageSessionDao.resumeUsage(session.id, nowMillis)
        }
    }

    suspend fun closeActiveSessionFor(packageName: String, nowMillis: Long = System.currentTimeMillis()) {
        val session = usageSessionDao.activeForPackage(packageName) ?: return
        if (session.lastForegroundStartEpochMillis != null) {
            pauseSession(packageName, nowMillis)
        }
        usageSessionDao.close(session.id, nowMillis)
    }

    fun sessionConsumedMillis(session: UsageSession, nowMillis: Long = System.currentTimeMillis()): Long {
        val liveSlice = session.lastForegroundStartEpochMillis
            ?.let { (nowMillis - it).coerceAtLeast(0) }
            ?: 0L
        return session.accumulatedUsageMillis + liveSlice
    }

    suspend fun addScroll(packageName: String, contentType: ContentType, timestamp: Long = System.currentTimeMillis()) {
        scrollEventDao.insert(ScrollEvent(packageName = packageName, timestampEpochMillis = timestamp, contentType = contentType))
        usageSessionDao.activeForPackage(packageName)?.let { usageSessionDao.addScrolls(it.id, 1) }
    }

    suspend fun totalUsageMillisToday(packageName: String, nowMillis: Long = System.currentTimeMillis()): Long =
        usageSessionDao.totalUsageMillis(packageName, localDayStartMillis(nowMillis), nowMillis)

    suspend fun scrollsToday(packageName: String, nowMillis: Long = System.currentTimeMillis()): Int =
        scrollEventDao.scrollsToday(packageName, localDayStartMillis(nowMillis))

    /** Count consecutive days (going back from today) where all monitored apps' daily limits were respected.
     *  Returns 0 when no usage history exists (fresh install or no monitored apps). */
    suspend fun currentStreak(): Int {
        val apps = monitoredAppDao.observeAll().first()
        if (apps.isEmpty()) return 0

        val zone = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zone)

        // Guard: if there are zero usage sessions in the entire database, it's a fresh install
        val anySessionEver = usageSessionDao.anySessionExists()
        if (!anySessionEver) return 0

        // Cap streak to days since first install to prevent stale/migrated data from
        // producing impossibly long streaks (e.g. "366 days" on a fresh install).
        val installMillis = settingsRepository.firstInstallEpochMillis()
        val installDate = java.time.Instant.ofEpochMilli(installMillis).atZone(zone).toLocalDate()
        val maxDaysBack = today.toEpochDay() - installDate.toEpochDay()

        var streak = 0
        val maxCheckDays = maxDaysBack.coerceAtMost(365)
        for (daysBack in 0..maxCheckDays) {
            val day = today.minusDays(daysBack.toLong())
            val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            var anyUsage = false
            var clean = true
            for (app in apps) {
                val used = usageSessionDao.totalUsageMillis(app.packageName, dayStart, dayEnd)
                if (used > 0) anyUsage = true
                val dailyLimit = app.dailyTimeLimitMinutes
                if (dailyLimit != null && used > TimeUnit.MINUTES.toMillis(dailyLimit.toLong())) {
                    clean = false
                    break
                }
            }
            if (!anyUsage) break          // no data = stop counting
            if (clean) streak++ else break
        }
        return streak
    }

    fun installedApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter {
                // Show all user-installed apps; system apps only if they have a launcher intent
                val isSystem = it.flags and ApplicationInfo.FLAG_SYSTEM != 0
                if (isSystem) packageManager.getLaunchIntentForPackage(it.packageName) != null
                else true
            }
            .map {
                InstalledApp(
                    packageName = it.packageName,
                    displayName = it.loadLabel(packageManager).toString(),
                    isSystemApp = it.flags and ApplicationInfo.FLAG_SYSTEM != 0
                )
            }
            .sortedBy { it.displayName.lowercase() }
    }
}

fun localDayStartMillis(nowMillis: Long): Long {
    val zone = java.time.ZoneId.systemDefault()
    return java.time.Instant.ofEpochMilli(nowMillis)
        .atZone(zone)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
}
