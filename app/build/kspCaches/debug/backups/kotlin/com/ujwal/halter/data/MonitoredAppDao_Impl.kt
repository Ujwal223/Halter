package com.ujwal.halter.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MonitoredAppDao_Impl(
  __db: RoomDatabase,
) : MonitoredAppDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMonitoredApp: EntityInsertAdapter<MonitoredApp>

  private val __halterConverters: HalterConverters = HalterConverters()
  init {
    this.__db = __db
    this.__insertAdapterOfMonitoredApp = object : EntityInsertAdapter<MonitoredApp>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `monitored_apps` (`packageName`,`displayName`,`isFlaggedHarmful`,`dailyTimeLimitMinutes`,`sessionTimeLimitMinutes`,`scrollLimitPerSession`,`scrollLimitPerDay`,`strictModeEnabled`,`category`,`isInstantBlocked`,`instantBlockUntilEpochMillis`,`cooldownUntilEpochMillis`,`partialShortVideoBlocked`,`holdToOpenSeconds`,`excludedFromFocus`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MonitoredApp) {
        statement.bindText(1, entity.packageName)
        statement.bindText(2, entity.displayName)
        val _tmp: Int = if (entity.isFlaggedHarmful) 1 else 0
        statement.bindLong(3, _tmp.toLong())
        val _tmpDailyTimeLimitMinutes: Int? = entity.dailyTimeLimitMinutes
        if (_tmpDailyTimeLimitMinutes == null) {
          statement.bindNull(4)
        } else {
          statement.bindLong(4, _tmpDailyTimeLimitMinutes.toLong())
        }
        val _tmpSessionTimeLimitMinutes: Int? = entity.sessionTimeLimitMinutes
        if (_tmpSessionTimeLimitMinutes == null) {
          statement.bindNull(5)
        } else {
          statement.bindLong(5, _tmpSessionTimeLimitMinutes.toLong())
        }
        val _tmpScrollLimitPerSession: Int? = entity.scrollLimitPerSession
        if (_tmpScrollLimitPerSession == null) {
          statement.bindNull(6)
        } else {
          statement.bindLong(6, _tmpScrollLimitPerSession.toLong())
        }
        val _tmpScrollLimitPerDay: Int? = entity.scrollLimitPerDay
        if (_tmpScrollLimitPerDay == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpScrollLimitPerDay.toLong())
        }
        val _tmp_1: Int = if (entity.strictModeEnabled) 1 else 0
        statement.bindLong(8, _tmp_1.toLong())
        val _tmp_2: String = __halterConverters.appCategoryToString(entity.category)
        statement.bindText(9, _tmp_2)
        val _tmp_3: Int = if (entity.isInstantBlocked) 1 else 0
        statement.bindLong(10, _tmp_3.toLong())
        val _tmpInstantBlockUntilEpochMillis: Long? = entity.instantBlockUntilEpochMillis
        if (_tmpInstantBlockUntilEpochMillis == null) {
          statement.bindNull(11)
        } else {
          statement.bindLong(11, _tmpInstantBlockUntilEpochMillis)
        }
        val _tmpCooldownUntilEpochMillis: Long? = entity.cooldownUntilEpochMillis
        if (_tmpCooldownUntilEpochMillis == null) {
          statement.bindNull(12)
        } else {
          statement.bindLong(12, _tmpCooldownUntilEpochMillis)
        }
        val _tmp_4: Int = if (entity.partialShortVideoBlocked) 1 else 0
        statement.bindLong(13, _tmp_4.toLong())
        val _tmpHoldToOpenSeconds: Int? = entity.holdToOpenSeconds
        if (_tmpHoldToOpenSeconds == null) {
          statement.bindNull(14)
        } else {
          statement.bindLong(14, _tmpHoldToOpenSeconds.toLong())
        }
        val _tmp_5: Int = if (entity.excludedFromFocus) 1 else 0
        statement.bindLong(15, _tmp_5.toLong())
      }
    }
  }

  public override suspend fun upsert(app: MonitoredApp): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfMonitoredApp.insert(_connection, app)
  }

  public override fun observeAll(): Flow<List<MonitoredApp>> {
    val _sql: String = "SELECT * FROM monitored_apps ORDER BY displayName"
    return createFlow(__db, false, arrayOf("monitored_apps")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfPackageName: Int = getColumnIndexOrThrow(_stmt, "packageName")
        val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
        val _columnIndexOfIsFlaggedHarmful: Int = getColumnIndexOrThrow(_stmt, "isFlaggedHarmful")
        val _columnIndexOfDailyTimeLimitMinutes: Int = getColumnIndexOrThrow(_stmt, "dailyTimeLimitMinutes")
        val _columnIndexOfSessionTimeLimitMinutes: Int = getColumnIndexOrThrow(_stmt, "sessionTimeLimitMinutes")
        val _columnIndexOfScrollLimitPerSession: Int = getColumnIndexOrThrow(_stmt, "scrollLimitPerSession")
        val _columnIndexOfScrollLimitPerDay: Int = getColumnIndexOrThrow(_stmt, "scrollLimitPerDay")
        val _columnIndexOfStrictModeEnabled: Int = getColumnIndexOrThrow(_stmt, "strictModeEnabled")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsInstantBlocked: Int = getColumnIndexOrThrow(_stmt, "isInstantBlocked")
        val _columnIndexOfInstantBlockUntilEpochMillis: Int = getColumnIndexOrThrow(_stmt, "instantBlockUntilEpochMillis")
        val _columnIndexOfCooldownUntilEpochMillis: Int = getColumnIndexOrThrow(_stmt, "cooldownUntilEpochMillis")
        val _columnIndexOfPartialShortVideoBlocked: Int = getColumnIndexOrThrow(_stmt, "partialShortVideoBlocked")
        val _columnIndexOfHoldToOpenSeconds: Int = getColumnIndexOrThrow(_stmt, "holdToOpenSeconds")
        val _columnIndexOfExcludedFromFocus: Int = getColumnIndexOrThrow(_stmt, "excludedFromFocus")
        val _result: MutableList<MonitoredApp> = mutableListOf()
        while (_stmt.step()) {
          val _item: MonitoredApp
          val _tmpPackageName: String
          _tmpPackageName = _stmt.getText(_columnIndexOfPackageName)
          val _tmpDisplayName: String
          _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
          val _tmpIsFlaggedHarmful: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFlaggedHarmful).toInt()
          _tmpIsFlaggedHarmful = _tmp != 0
          val _tmpDailyTimeLimitMinutes: Int?
          if (_stmt.isNull(_columnIndexOfDailyTimeLimitMinutes)) {
            _tmpDailyTimeLimitMinutes = null
          } else {
            _tmpDailyTimeLimitMinutes = _stmt.getLong(_columnIndexOfDailyTimeLimitMinutes).toInt()
          }
          val _tmpSessionTimeLimitMinutes: Int?
          if (_stmt.isNull(_columnIndexOfSessionTimeLimitMinutes)) {
            _tmpSessionTimeLimitMinutes = null
          } else {
            _tmpSessionTimeLimitMinutes = _stmt.getLong(_columnIndexOfSessionTimeLimitMinutes).toInt()
          }
          val _tmpScrollLimitPerSession: Int?
          if (_stmt.isNull(_columnIndexOfScrollLimitPerSession)) {
            _tmpScrollLimitPerSession = null
          } else {
            _tmpScrollLimitPerSession = _stmt.getLong(_columnIndexOfScrollLimitPerSession).toInt()
          }
          val _tmpScrollLimitPerDay: Int?
          if (_stmt.isNull(_columnIndexOfScrollLimitPerDay)) {
            _tmpScrollLimitPerDay = null
          } else {
            _tmpScrollLimitPerDay = _stmt.getLong(_columnIndexOfScrollLimitPerDay).toInt()
          }
          val _tmpStrictModeEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfStrictModeEnabled).toInt()
          _tmpStrictModeEnabled = _tmp_1 != 0
          val _tmpCategory: AppCategory
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfCategory)
          _tmpCategory = __halterConverters.stringToAppCategory(_tmp_2)
          val _tmpIsInstantBlocked: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsInstantBlocked).toInt()
          _tmpIsInstantBlocked = _tmp_3 != 0
          val _tmpInstantBlockUntilEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfInstantBlockUntilEpochMillis)) {
            _tmpInstantBlockUntilEpochMillis = null
          } else {
            _tmpInstantBlockUntilEpochMillis = _stmt.getLong(_columnIndexOfInstantBlockUntilEpochMillis)
          }
          val _tmpCooldownUntilEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfCooldownUntilEpochMillis)) {
            _tmpCooldownUntilEpochMillis = null
          } else {
            _tmpCooldownUntilEpochMillis = _stmt.getLong(_columnIndexOfCooldownUntilEpochMillis)
          }
          val _tmpPartialShortVideoBlocked: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfPartialShortVideoBlocked).toInt()
          _tmpPartialShortVideoBlocked = _tmp_4 != 0
          val _tmpHoldToOpenSeconds: Int?
          if (_stmt.isNull(_columnIndexOfHoldToOpenSeconds)) {
            _tmpHoldToOpenSeconds = null
          } else {
            _tmpHoldToOpenSeconds = _stmt.getLong(_columnIndexOfHoldToOpenSeconds).toInt()
          }
          val _tmpExcludedFromFocus: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfExcludedFromFocus).toInt()
          _tmpExcludedFromFocus = _tmp_5 != 0
          _item = MonitoredApp(_tmpPackageName,_tmpDisplayName,_tmpIsFlaggedHarmful,_tmpDailyTimeLimitMinutes,_tmpSessionTimeLimitMinutes,_tmpScrollLimitPerSession,_tmpScrollLimitPerDay,_tmpStrictModeEnabled,_tmpCategory,_tmpIsInstantBlocked,_tmpInstantBlockUntilEpochMillis,_tmpCooldownUntilEpochMillis,_tmpPartialShortVideoBlocked,_tmpHoldToOpenSeconds,_tmpExcludedFromFocus)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun `get`(packageName: String): MonitoredApp? {
    val _sql: String = "SELECT * FROM monitored_apps WHERE packageName = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, packageName)
        val _columnIndexOfPackageName: Int = getColumnIndexOrThrow(_stmt, "packageName")
        val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
        val _columnIndexOfIsFlaggedHarmful: Int = getColumnIndexOrThrow(_stmt, "isFlaggedHarmful")
        val _columnIndexOfDailyTimeLimitMinutes: Int = getColumnIndexOrThrow(_stmt, "dailyTimeLimitMinutes")
        val _columnIndexOfSessionTimeLimitMinutes: Int = getColumnIndexOrThrow(_stmt, "sessionTimeLimitMinutes")
        val _columnIndexOfScrollLimitPerSession: Int = getColumnIndexOrThrow(_stmt, "scrollLimitPerSession")
        val _columnIndexOfScrollLimitPerDay: Int = getColumnIndexOrThrow(_stmt, "scrollLimitPerDay")
        val _columnIndexOfStrictModeEnabled: Int = getColumnIndexOrThrow(_stmt, "strictModeEnabled")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsInstantBlocked: Int = getColumnIndexOrThrow(_stmt, "isInstantBlocked")
        val _columnIndexOfInstantBlockUntilEpochMillis: Int = getColumnIndexOrThrow(_stmt, "instantBlockUntilEpochMillis")
        val _columnIndexOfCooldownUntilEpochMillis: Int = getColumnIndexOrThrow(_stmt, "cooldownUntilEpochMillis")
        val _columnIndexOfPartialShortVideoBlocked: Int = getColumnIndexOrThrow(_stmt, "partialShortVideoBlocked")
        val _columnIndexOfHoldToOpenSeconds: Int = getColumnIndexOrThrow(_stmt, "holdToOpenSeconds")
        val _columnIndexOfExcludedFromFocus: Int = getColumnIndexOrThrow(_stmt, "excludedFromFocus")
        val _result: MonitoredApp?
        if (_stmt.step()) {
          val _tmpPackageName: String
          _tmpPackageName = _stmt.getText(_columnIndexOfPackageName)
          val _tmpDisplayName: String
          _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
          val _tmpIsFlaggedHarmful: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFlaggedHarmful).toInt()
          _tmpIsFlaggedHarmful = _tmp != 0
          val _tmpDailyTimeLimitMinutes: Int?
          if (_stmt.isNull(_columnIndexOfDailyTimeLimitMinutes)) {
            _tmpDailyTimeLimitMinutes = null
          } else {
            _tmpDailyTimeLimitMinutes = _stmt.getLong(_columnIndexOfDailyTimeLimitMinutes).toInt()
          }
          val _tmpSessionTimeLimitMinutes: Int?
          if (_stmt.isNull(_columnIndexOfSessionTimeLimitMinutes)) {
            _tmpSessionTimeLimitMinutes = null
          } else {
            _tmpSessionTimeLimitMinutes = _stmt.getLong(_columnIndexOfSessionTimeLimitMinutes).toInt()
          }
          val _tmpScrollLimitPerSession: Int?
          if (_stmt.isNull(_columnIndexOfScrollLimitPerSession)) {
            _tmpScrollLimitPerSession = null
          } else {
            _tmpScrollLimitPerSession = _stmt.getLong(_columnIndexOfScrollLimitPerSession).toInt()
          }
          val _tmpScrollLimitPerDay: Int?
          if (_stmt.isNull(_columnIndexOfScrollLimitPerDay)) {
            _tmpScrollLimitPerDay = null
          } else {
            _tmpScrollLimitPerDay = _stmt.getLong(_columnIndexOfScrollLimitPerDay).toInt()
          }
          val _tmpStrictModeEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfStrictModeEnabled).toInt()
          _tmpStrictModeEnabled = _tmp_1 != 0
          val _tmpCategory: AppCategory
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfCategory)
          _tmpCategory = __halterConverters.stringToAppCategory(_tmp_2)
          val _tmpIsInstantBlocked: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsInstantBlocked).toInt()
          _tmpIsInstantBlocked = _tmp_3 != 0
          val _tmpInstantBlockUntilEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfInstantBlockUntilEpochMillis)) {
            _tmpInstantBlockUntilEpochMillis = null
          } else {
            _tmpInstantBlockUntilEpochMillis = _stmt.getLong(_columnIndexOfInstantBlockUntilEpochMillis)
          }
          val _tmpCooldownUntilEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfCooldownUntilEpochMillis)) {
            _tmpCooldownUntilEpochMillis = null
          } else {
            _tmpCooldownUntilEpochMillis = _stmt.getLong(_columnIndexOfCooldownUntilEpochMillis)
          }
          val _tmpPartialShortVideoBlocked: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfPartialShortVideoBlocked).toInt()
          _tmpPartialShortVideoBlocked = _tmp_4 != 0
          val _tmpHoldToOpenSeconds: Int?
          if (_stmt.isNull(_columnIndexOfHoldToOpenSeconds)) {
            _tmpHoldToOpenSeconds = null
          } else {
            _tmpHoldToOpenSeconds = _stmt.getLong(_columnIndexOfHoldToOpenSeconds).toInt()
          }
          val _tmpExcludedFromFocus: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfExcludedFromFocus).toInt()
          _tmpExcludedFromFocus = _tmp_5 != 0
          _result = MonitoredApp(_tmpPackageName,_tmpDisplayName,_tmpIsFlaggedHarmful,_tmpDailyTimeLimitMinutes,_tmpSessionTimeLimitMinutes,_tmpScrollLimitPerSession,_tmpScrollLimitPerDay,_tmpStrictModeEnabled,_tmpCategory,_tmpIsInstantBlocked,_tmpInstantBlockUntilEpochMillis,_tmpCooldownUntilEpochMillis,_tmpPartialShortVideoBlocked,_tmpHoldToOpenSeconds,_tmpExcludedFromFocus)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeFlagged(): Flow<List<MonitoredApp>> {
    val _sql: String = "SELECT * FROM monitored_apps WHERE isFlaggedHarmful = 1 ORDER BY displayName"
    return createFlow(__db, false, arrayOf("monitored_apps")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfPackageName: Int = getColumnIndexOrThrow(_stmt, "packageName")
        val _columnIndexOfDisplayName: Int = getColumnIndexOrThrow(_stmt, "displayName")
        val _columnIndexOfIsFlaggedHarmful: Int = getColumnIndexOrThrow(_stmt, "isFlaggedHarmful")
        val _columnIndexOfDailyTimeLimitMinutes: Int = getColumnIndexOrThrow(_stmt, "dailyTimeLimitMinutes")
        val _columnIndexOfSessionTimeLimitMinutes: Int = getColumnIndexOrThrow(_stmt, "sessionTimeLimitMinutes")
        val _columnIndexOfScrollLimitPerSession: Int = getColumnIndexOrThrow(_stmt, "scrollLimitPerSession")
        val _columnIndexOfScrollLimitPerDay: Int = getColumnIndexOrThrow(_stmt, "scrollLimitPerDay")
        val _columnIndexOfStrictModeEnabled: Int = getColumnIndexOrThrow(_stmt, "strictModeEnabled")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfIsInstantBlocked: Int = getColumnIndexOrThrow(_stmt, "isInstantBlocked")
        val _columnIndexOfInstantBlockUntilEpochMillis: Int = getColumnIndexOrThrow(_stmt, "instantBlockUntilEpochMillis")
        val _columnIndexOfCooldownUntilEpochMillis: Int = getColumnIndexOrThrow(_stmt, "cooldownUntilEpochMillis")
        val _columnIndexOfPartialShortVideoBlocked: Int = getColumnIndexOrThrow(_stmt, "partialShortVideoBlocked")
        val _columnIndexOfHoldToOpenSeconds: Int = getColumnIndexOrThrow(_stmt, "holdToOpenSeconds")
        val _columnIndexOfExcludedFromFocus: Int = getColumnIndexOrThrow(_stmt, "excludedFromFocus")
        val _result: MutableList<MonitoredApp> = mutableListOf()
        while (_stmt.step()) {
          val _item: MonitoredApp
          val _tmpPackageName: String
          _tmpPackageName = _stmt.getText(_columnIndexOfPackageName)
          val _tmpDisplayName: String
          _tmpDisplayName = _stmt.getText(_columnIndexOfDisplayName)
          val _tmpIsFlaggedHarmful: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFlaggedHarmful).toInt()
          _tmpIsFlaggedHarmful = _tmp != 0
          val _tmpDailyTimeLimitMinutes: Int?
          if (_stmt.isNull(_columnIndexOfDailyTimeLimitMinutes)) {
            _tmpDailyTimeLimitMinutes = null
          } else {
            _tmpDailyTimeLimitMinutes = _stmt.getLong(_columnIndexOfDailyTimeLimitMinutes).toInt()
          }
          val _tmpSessionTimeLimitMinutes: Int?
          if (_stmt.isNull(_columnIndexOfSessionTimeLimitMinutes)) {
            _tmpSessionTimeLimitMinutes = null
          } else {
            _tmpSessionTimeLimitMinutes = _stmt.getLong(_columnIndexOfSessionTimeLimitMinutes).toInt()
          }
          val _tmpScrollLimitPerSession: Int?
          if (_stmt.isNull(_columnIndexOfScrollLimitPerSession)) {
            _tmpScrollLimitPerSession = null
          } else {
            _tmpScrollLimitPerSession = _stmt.getLong(_columnIndexOfScrollLimitPerSession).toInt()
          }
          val _tmpScrollLimitPerDay: Int?
          if (_stmt.isNull(_columnIndexOfScrollLimitPerDay)) {
            _tmpScrollLimitPerDay = null
          } else {
            _tmpScrollLimitPerDay = _stmt.getLong(_columnIndexOfScrollLimitPerDay).toInt()
          }
          val _tmpStrictModeEnabled: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfStrictModeEnabled).toInt()
          _tmpStrictModeEnabled = _tmp_1 != 0
          val _tmpCategory: AppCategory
          val _tmp_2: String
          _tmp_2 = _stmt.getText(_columnIndexOfCategory)
          _tmpCategory = __halterConverters.stringToAppCategory(_tmp_2)
          val _tmpIsInstantBlocked: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsInstantBlocked).toInt()
          _tmpIsInstantBlocked = _tmp_3 != 0
          val _tmpInstantBlockUntilEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfInstantBlockUntilEpochMillis)) {
            _tmpInstantBlockUntilEpochMillis = null
          } else {
            _tmpInstantBlockUntilEpochMillis = _stmt.getLong(_columnIndexOfInstantBlockUntilEpochMillis)
          }
          val _tmpCooldownUntilEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfCooldownUntilEpochMillis)) {
            _tmpCooldownUntilEpochMillis = null
          } else {
            _tmpCooldownUntilEpochMillis = _stmt.getLong(_columnIndexOfCooldownUntilEpochMillis)
          }
          val _tmpPartialShortVideoBlocked: Boolean
          val _tmp_4: Int
          _tmp_4 = _stmt.getLong(_columnIndexOfPartialShortVideoBlocked).toInt()
          _tmpPartialShortVideoBlocked = _tmp_4 != 0
          val _tmpHoldToOpenSeconds: Int?
          if (_stmt.isNull(_columnIndexOfHoldToOpenSeconds)) {
            _tmpHoldToOpenSeconds = null
          } else {
            _tmpHoldToOpenSeconds = _stmt.getLong(_columnIndexOfHoldToOpenSeconds).toInt()
          }
          val _tmpExcludedFromFocus: Boolean
          val _tmp_5: Int
          _tmp_5 = _stmt.getLong(_columnIndexOfExcludedFromFocus).toInt()
          _tmpExcludedFromFocus = _tmp_5 != 0
          _item = MonitoredApp(_tmpPackageName,_tmpDisplayName,_tmpIsFlaggedHarmful,_tmpDailyTimeLimitMinutes,_tmpSessionTimeLimitMinutes,_tmpScrollLimitPerSession,_tmpScrollLimitPerDay,_tmpStrictModeEnabled,_tmpCategory,_tmpIsInstantBlocked,_tmpInstantBlockUntilEpochMillis,_tmpCooldownUntilEpochMillis,_tmpPartialShortVideoBlocked,_tmpHoldToOpenSeconds,_tmpExcludedFromFocus)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(packageName: String) {
    val _sql: String = "DELETE FROM monitored_apps WHERE packageName = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, packageName)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
