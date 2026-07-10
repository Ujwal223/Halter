// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {
    @Query("SELECT * FROM monitored_apps ORDER BY displayName")
    fun observeAll(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun get(packageName: String): MonitoredApp?

    @Query("SELECT * FROM monitored_apps WHERE isFlaggedHarmful = 1 ORDER BY displayName")
    fun observeFlagged(): Flow<List<MonitoredApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: MonitoredApp)

    @Query("DELETE FROM monitored_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

@Dao
interface UsageSessionDao {
    @Query("SELECT * FROM usage_sessions WHERE endEpochMillis IS NULL ORDER BY startEpochMillis DESC LIMIT 1")
    suspend fun activeSession(): UsageSession?

    @Query("SELECT * FROM usage_sessions WHERE packageName = :packageName AND endEpochMillis IS NULL ORDER BY startEpochMillis DESC LIMIT 1")
    suspend fun activeForPackage(packageName: String): UsageSession?

    @Query("SELECT * FROM usage_sessions WHERE startEpochMillis >= :fromMillis ORDER BY startEpochMillis DESC")
    fun observeSince(fromMillis: Long): Flow<List<UsageSession>>

    @Query("SELECT COALESCE(SUM(COALESCE(endEpochMillis, :nowMillis) - startEpochMillis), 0) FROM usage_sessions WHERE packageName = :packageName AND startEpochMillis >= :dayStartMillis")
    suspend fun totalUsageMillis(packageName: String, dayStartMillis: Long, nowMillis: Long): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: UsageSession): Long

    @Query("SELECT COUNT(*) > 0 FROM usage_sessions")
    suspend fun anySessionExists(): Boolean

    @Query("UPDATE usage_sessions SET endEpochMillis = :endMillis WHERE id = :id")
    suspend fun close(id: Long, endMillis: Long)

    @Query("UPDATE usage_sessions SET scrollsUsed = scrollsUsed + :count WHERE id = :id")
    suspend fun addScrolls(id: Long, count: Int)

    @Query(
        """
        UPDATE usage_sessions
        SET accumulatedUsageMillis = accumulatedUsageMillis + :elapsedMillis,
            lastForegroundStartEpochMillis = NULL
        WHERE id = :id
        """
    )
    suspend fun pauseUsage(id: Long, elapsedMillis: Long)

    @Query("UPDATE usage_sessions SET lastForegroundStartEpochMillis = :startMillis WHERE id = :id")
    suspend fun resumeUsage(id: Long, startMillis: Long)
}

@Dao
interface ScrollEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ScrollEvent)

    @Query("SELECT COALESCE(SUM(count), 0) FROM scroll_events WHERE packageName = :packageName AND timestampEpochMillis >= :dayStartMillis")
    suspend fun scrollsToday(packageName: String, dayStartMillis: Long): Int

    @Query("SELECT * FROM scroll_events WHERE timestampEpochMillis >= :fromMillis ORDER BY timestampEpochMillis DESC")
    fun observeSince(fromMillis: Long): Flow<List<ScrollEvent>>
}

@Dao
interface BlockScheduleDao {
    @Query("SELECT * FROM block_schedules WHERE packageName = :packageName AND isEnabled = 1")
    suspend fun enabledForPackage(packageName: String): List<BlockSchedule>

    @Query("SELECT * FROM block_schedules WHERE packageName = :packageName ORDER BY startMinuteOfDay")
    fun observeForPackage(packageName: String): Flow<List<BlockSchedule>>

    @Query("SELECT * FROM block_schedules ORDER BY packageName, startMinuteOfDay")
    fun observeAll(): Flow<List<BlockSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: BlockSchedule): Long

    @Query("DELETE FROM block_schedules WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startEpochMillis DESC")
    fun observeAll(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE completed = 0 ORDER BY startEpochMillis DESC LIMIT 1")
    suspend fun active(): FocusSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: FocusSession): Long

    @Query("UPDATE focus_sessions SET interruptionCount = interruptionCount + 1 WHERE id = :id")
    suspend fun incrementInterruptions(id: Long)
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY timestampEpochMillis DESC")
    fun observeAll(): Flow<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntry)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE isEnabled = 1")
    suspend fun enabled(): List<Routine>

    @Query("SELECT * FROM routines ORDER BY name")
    fun observeAll(): Flow<List<Routine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(routine: Routine): Long

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun delete(id: Long)
}
