package com.ujwal.halter.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class HalterDatabase_Impl : HalterDatabase() {
  private val _monitoredAppDao: Lazy<MonitoredAppDao> = lazy {
    MonitoredAppDao_Impl(this)
  }

  private val _usageSessionDao: Lazy<UsageSessionDao> = lazy {
    UsageSessionDao_Impl(this)
  }

  private val _scrollEventDao: Lazy<ScrollEventDao> = lazy {
    ScrollEventDao_Impl(this)
  }

  private val _blockScheduleDao: Lazy<BlockScheduleDao> = lazy {
    BlockScheduleDao_Impl(this)
  }

  private val _focusSessionDao: Lazy<FocusSessionDao> = lazy {
    FocusSessionDao_Impl(this)
  }

  private val _journalDao: Lazy<JournalDao> = lazy {
    JournalDao_Impl(this)
  }

  private val _routineDao: Lazy<RoutineDao> = lazy {
    RoutineDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(4, "eaaaf380faa1f9ae495482528271ee97", "0553af1d89893129068473e8e8281a58") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `monitored_apps` (`packageName` TEXT NOT NULL, `displayName` TEXT NOT NULL, `isFlaggedHarmful` INTEGER NOT NULL, `dailyTimeLimitMinutes` INTEGER, `sessionTimeLimitMinutes` INTEGER, `scrollLimitPerSession` INTEGER, `scrollLimitPerDay` INTEGER, `strictModeEnabled` INTEGER NOT NULL, `category` TEXT NOT NULL, `isInstantBlocked` INTEGER NOT NULL, `instantBlockUntilEpochMillis` INTEGER, `cooldownUntilEpochMillis` INTEGER, `partialShortVideoBlocked` INTEGER NOT NULL, `holdToOpenSeconds` INTEGER, `excludedFromFocus` INTEGER NOT NULL, PRIMARY KEY(`packageName`))")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_monitored_apps_isFlaggedHarmful` ON `monitored_apps` (`isFlaggedHarmful`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `scroll_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `timestampEpochMillis` INTEGER NOT NULL, `contentType` TEXT NOT NULL, `count` INTEGER NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_scroll_events_packageName` ON `scroll_events` (`packageName`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_scroll_events_timestampEpochMillis` ON `scroll_events` (`timestampEpochMillis`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_scroll_events_packageName_timestampEpochMillis` ON `scroll_events` (`packageName`, `timestampEpochMillis`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `usage_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `startEpochMillis` INTEGER NOT NULL, `endEpochMillis` INTEGER, `scrollsUsed` INTEGER NOT NULL, `chosenSessionLimit` INTEGER, `limitType` TEXT NOT NULL, `accumulatedUsageMillis` INTEGER NOT NULL, `lastForegroundStartEpochMillis` INTEGER)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_sessions_packageName` ON `usage_sessions` (`packageName`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_sessions_startEpochMillis` ON `usage_sessions` (`startEpochMillis`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_sessions_endEpochMillis` ON `usage_sessions` (`endEpochMillis`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_sessions_packageName_endEpochMillis` ON `usage_sessions` (`packageName`, `endEpochMillis`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `block_schedules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `startMinuteOfDay` INTEGER NOT NULL, `endMinuteOfDay` INTEGER NOT NULL, `daysOfWeekBitmask` INTEGER NOT NULL, `isEnabled` INTEGER NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_block_schedules_packageName` ON `block_schedules` (`packageName`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_block_schedules_packageName_isEnabled` ON `block_schedules` (`packageName`, `isEnabled`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startEpochMillis` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `interruptionCount` INTEGER NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_completed` ON `focus_sessions` (`completed`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_startEpochMillis` ON `focus_sessions` (`startEpochMillis`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `journal_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `timestampEpochMillis` INTEGER NOT NULL, `reason` TEXT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entries_packageName` ON `journal_entries` (`packageName`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entries_timestampEpochMillis` ON `journal_entries` (`timestampEpochMillis`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `routines` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `packageNames` TEXT NOT NULL, `startMinuteOfDay` INTEGER NOT NULL, `endMinuteOfDay` INTEGER NOT NULL, `daysOfWeekBitmask` INTEGER NOT NULL, `isEnabled` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'eaaaf380faa1f9ae495482528271ee97')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `monitored_apps`")
        connection.execSQL("DROP TABLE IF EXISTS `scroll_events`")
        connection.execSQL("DROP TABLE IF EXISTS `usage_sessions`")
        connection.execSQL("DROP TABLE IF EXISTS `block_schedules`")
        connection.execSQL("DROP TABLE IF EXISTS `focus_sessions`")
        connection.execSQL("DROP TABLE IF EXISTS `journal_entries`")
        connection.execSQL("DROP TABLE IF EXISTS `routines`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsMonitoredApps: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsMonitoredApps.put("packageName", TableInfo.Column("packageName", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("displayName", TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("isFlaggedHarmful", TableInfo.Column("isFlaggedHarmful", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("dailyTimeLimitMinutes", TableInfo.Column("dailyTimeLimitMinutes", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("sessionTimeLimitMinutes", TableInfo.Column("sessionTimeLimitMinutes", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("scrollLimitPerSession", TableInfo.Column("scrollLimitPerSession", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("scrollLimitPerDay", TableInfo.Column("scrollLimitPerDay", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("strictModeEnabled", TableInfo.Column("strictModeEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("category", TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("isInstantBlocked", TableInfo.Column("isInstantBlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("instantBlockUntilEpochMillis", TableInfo.Column("instantBlockUntilEpochMillis", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("cooldownUntilEpochMillis", TableInfo.Column("cooldownUntilEpochMillis", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("partialShortVideoBlocked", TableInfo.Column("partialShortVideoBlocked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("holdToOpenSeconds", TableInfo.Column("holdToOpenSeconds", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMonitoredApps.put("excludedFromFocus", TableInfo.Column("excludedFromFocus", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMonitoredApps: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesMonitoredApps: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesMonitoredApps.add(TableInfo.Index("index_monitored_apps_isFlaggedHarmful", false, listOf("isFlaggedHarmful"), listOf("ASC")))
        val _infoMonitoredApps: TableInfo = TableInfo("monitored_apps", _columnsMonitoredApps, _foreignKeysMonitoredApps, _indicesMonitoredApps)
        val _existingMonitoredApps: TableInfo = read(connection, "monitored_apps")
        if (!_infoMonitoredApps.equals(_existingMonitoredApps)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |monitored_apps(com.ujwal.halter.data.MonitoredApp).
              | Expected:
              |""".trimMargin() + _infoMonitoredApps + """
              |
              | Found:
              |""".trimMargin() + _existingMonitoredApps)
        }
        val _columnsScrollEvents: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsScrollEvents.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsScrollEvents.put("packageName", TableInfo.Column("packageName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsScrollEvents.put("timestampEpochMillis", TableInfo.Column("timestampEpochMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsScrollEvents.put("contentType", TableInfo.Column("contentType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsScrollEvents.put("count", TableInfo.Column("count", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysScrollEvents: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesScrollEvents: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesScrollEvents.add(TableInfo.Index("index_scroll_events_packageName", false, listOf("packageName"), listOf("ASC")))
        _indicesScrollEvents.add(TableInfo.Index("index_scroll_events_timestampEpochMillis", false, listOf("timestampEpochMillis"), listOf("ASC")))
        _indicesScrollEvents.add(TableInfo.Index("index_scroll_events_packageName_timestampEpochMillis", false, listOf("packageName", "timestampEpochMillis"), listOf("ASC", "ASC")))
        val _infoScrollEvents: TableInfo = TableInfo("scroll_events", _columnsScrollEvents, _foreignKeysScrollEvents, _indicesScrollEvents)
        val _existingScrollEvents: TableInfo = read(connection, "scroll_events")
        if (!_infoScrollEvents.equals(_existingScrollEvents)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |scroll_events(com.ujwal.halter.data.ScrollEvent).
              | Expected:
              |""".trimMargin() + _infoScrollEvents + """
              |
              | Found:
              |""".trimMargin() + _existingScrollEvents)
        }
        val _columnsUsageSessions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsUsageSessions.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsageSessions.put("packageName", TableInfo.Column("packageName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsageSessions.put("startEpochMillis", TableInfo.Column("startEpochMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsageSessions.put("endEpochMillis", TableInfo.Column("endEpochMillis", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsageSessions.put("scrollsUsed", TableInfo.Column("scrollsUsed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsageSessions.put("chosenSessionLimit", TableInfo.Column("chosenSessionLimit", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsageSessions.put("limitType", TableInfo.Column("limitType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsageSessions.put("accumulatedUsageMillis", TableInfo.Column("accumulatedUsageMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsUsageSessions.put("lastForegroundStartEpochMillis", TableInfo.Column("lastForegroundStartEpochMillis", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysUsageSessions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesUsageSessions: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesUsageSessions.add(TableInfo.Index("index_usage_sessions_packageName", false, listOf("packageName"), listOf("ASC")))
        _indicesUsageSessions.add(TableInfo.Index("index_usage_sessions_startEpochMillis", false, listOf("startEpochMillis"), listOf("ASC")))
        _indicesUsageSessions.add(TableInfo.Index("index_usage_sessions_endEpochMillis", false, listOf("endEpochMillis"), listOf("ASC")))
        _indicesUsageSessions.add(TableInfo.Index("index_usage_sessions_packageName_endEpochMillis", false, listOf("packageName", "endEpochMillis"), listOf("ASC", "ASC")))
        val _infoUsageSessions: TableInfo = TableInfo("usage_sessions", _columnsUsageSessions, _foreignKeysUsageSessions, _indicesUsageSessions)
        val _existingUsageSessions: TableInfo = read(connection, "usage_sessions")
        if (!_infoUsageSessions.equals(_existingUsageSessions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |usage_sessions(com.ujwal.halter.data.UsageSession).
              | Expected:
              |""".trimMargin() + _infoUsageSessions + """
              |
              | Found:
              |""".trimMargin() + _existingUsageSessions)
        }
        val _columnsBlockSchedules: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBlockSchedules.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockSchedules.put("packageName", TableInfo.Column("packageName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockSchedules.put("startMinuteOfDay", TableInfo.Column("startMinuteOfDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockSchedules.put("endMinuteOfDay", TableInfo.Column("endMinuteOfDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockSchedules.put("daysOfWeekBitmask", TableInfo.Column("daysOfWeekBitmask", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBlockSchedules.put("isEnabled", TableInfo.Column("isEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBlockSchedules: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBlockSchedules: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesBlockSchedules.add(TableInfo.Index("index_block_schedules_packageName", false, listOf("packageName"), listOf("ASC")))
        _indicesBlockSchedules.add(TableInfo.Index("index_block_schedules_packageName_isEnabled", false, listOf("packageName", "isEnabled"), listOf("ASC", "ASC")))
        val _infoBlockSchedules: TableInfo = TableInfo("block_schedules", _columnsBlockSchedules, _foreignKeysBlockSchedules, _indicesBlockSchedules)
        val _existingBlockSchedules: TableInfo = read(connection, "block_schedules")
        if (!_infoBlockSchedules.equals(_existingBlockSchedules)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |block_schedules(com.ujwal.halter.data.BlockSchedule).
              | Expected:
              |""".trimMargin() + _infoBlockSchedules + """
              |
              | Found:
              |""".trimMargin() + _existingBlockSchedules)
        }
        val _columnsFocusSessions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFocusSessions.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("startEpochMillis", TableInfo.Column("startEpochMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("durationMinutes", TableInfo.Column("durationMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("completed", TableInfo.Column("completed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("interruptionCount", TableInfo.Column("interruptionCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFocusSessions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFocusSessions: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesFocusSessions.add(TableInfo.Index("index_focus_sessions_completed", false, listOf("completed"), listOf("ASC")))
        _indicesFocusSessions.add(TableInfo.Index("index_focus_sessions_startEpochMillis", false, listOf("startEpochMillis"), listOf("ASC")))
        val _infoFocusSessions: TableInfo = TableInfo("focus_sessions", _columnsFocusSessions, _foreignKeysFocusSessions, _indicesFocusSessions)
        val _existingFocusSessions: TableInfo = read(connection, "focus_sessions")
        if (!_infoFocusSessions.equals(_existingFocusSessions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |focus_sessions(com.ujwal.halter.data.FocusSession).
              | Expected:
              |""".trimMargin() + _infoFocusSessions + """
              |
              | Found:
              |""".trimMargin() + _existingFocusSessions)
        }
        val _columnsJournalEntries: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsJournalEntries.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsJournalEntries.put("packageName", TableInfo.Column("packageName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsJournalEntries.put("timestampEpochMillis", TableInfo.Column("timestampEpochMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsJournalEntries.put("reason", TableInfo.Column("reason", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysJournalEntries: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesJournalEntries: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesJournalEntries.add(TableInfo.Index("index_journal_entries_packageName", false, listOf("packageName"), listOf("ASC")))
        _indicesJournalEntries.add(TableInfo.Index("index_journal_entries_timestampEpochMillis", false, listOf("timestampEpochMillis"), listOf("ASC")))
        val _infoJournalEntries: TableInfo = TableInfo("journal_entries", _columnsJournalEntries, _foreignKeysJournalEntries, _indicesJournalEntries)
        val _existingJournalEntries: TableInfo = read(connection, "journal_entries")
        if (!_infoJournalEntries.equals(_existingJournalEntries)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |journal_entries(com.ujwal.halter.data.JournalEntry).
              | Expected:
              |""".trimMargin() + _infoJournalEntries + """
              |
              | Found:
              |""".trimMargin() + _existingJournalEntries)
        }
        val _columnsRoutines: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRoutines.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRoutines.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRoutines.put("packageNames", TableInfo.Column("packageNames", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRoutines.put("startMinuteOfDay", TableInfo.Column("startMinuteOfDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRoutines.put("endMinuteOfDay", TableInfo.Column("endMinuteOfDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRoutines.put("daysOfWeekBitmask", TableInfo.Column("daysOfWeekBitmask", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRoutines.put("isEnabled", TableInfo.Column("isEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRoutines: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRoutines: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRoutines: TableInfo = TableInfo("routines", _columnsRoutines, _foreignKeysRoutines, _indicesRoutines)
        val _existingRoutines: TableInfo = read(connection, "routines")
        if (!_infoRoutines.equals(_existingRoutines)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |routines(com.ujwal.halter.data.Routine).
              | Expected:
              |""".trimMargin() + _infoRoutines + """
              |
              | Found:
              |""".trimMargin() + _existingRoutines)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "monitored_apps", "scroll_events", "usage_sessions", "block_schedules", "focus_sessions", "journal_entries", "routines")
  }

  public override fun clearAllTables() {
    super.performClear(false, "monitored_apps", "scroll_events", "usage_sessions", "block_schedules", "focus_sessions", "journal_entries", "routines")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(MonitoredAppDao::class, MonitoredAppDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(UsageSessionDao::class, UsageSessionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ScrollEventDao::class, ScrollEventDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BlockScheduleDao::class, BlockScheduleDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FocusSessionDao::class, FocusSessionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(JournalDao::class, JournalDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(RoutineDao::class, RoutineDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun monitoredAppDao(): MonitoredAppDao = _monitoredAppDao.value

  public override fun usageSessionDao(): UsageSessionDao = _usageSessionDao.value

  public override fun scrollEventDao(): ScrollEventDao = _scrollEventDao.value

  public override fun blockScheduleDao(): BlockScheduleDao = _blockScheduleDao.value

  public override fun focusSessionDao(): FocusSessionDao = _focusSessionDao.value

  public override fun journalDao(): JournalDao = _journalDao.value

  public override fun routineDao(): RoutineDao = _routineDao.value
}
