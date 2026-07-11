// SPDX-License-Identifier: GPL-3.0-or-later
package com.ujwal.halter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MonitoredApp::class,
        ScrollEvent::class,
        UsageSession::class,
        BlockSchedule::class,
        FocusSession::class,
        JournalEntry::class,
        Routine::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(HalterConverters::class)
abstract class HalterDatabase : RoomDatabase() {
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun scrollEventDao(): ScrollEventDao
    abstract fun blockScheduleDao(): BlockScheduleDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun journalDao(): JournalDao
    abstract fun routineDao(): RoutineDao

    companion object {
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE monitored_apps ADD COLUMN cooldownUntilEpochMillis INTEGER")
                db.execSQL("ALTER TABLE usage_sessions ADD COLUMN accumulatedUsageMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE usage_sessions ADD COLUMN lastForegroundStartEpochMillis INTEGER")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN detail TEXT")
            }
        }

        fun create(context: Context): HalterDatabase = Room.databaseBuilder(
            context.applicationContext,
            HalterDatabase::class.java,
            "halter.db"
        ).addMigrations(MIGRATION_3_4, MIGRATION_4_5).fallbackToDestructiveMigration(false).build()
    }
}
