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
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class UsageSessionDao_Impl(
  __db: RoomDatabase,
) : UsageSessionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfUsageSession: EntityInsertAdapter<UsageSession>

  private val __halterConverters: HalterConverters = HalterConverters()
  init {
    this.__db = __db
    this.__insertAdapterOfUsageSession = object : EntityInsertAdapter<UsageSession>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `usage_sessions` (`id`,`packageName`,`startEpochMillis`,`endEpochMillis`,`scrollsUsed`,`chosenSessionLimit`,`limitType`,`accumulatedUsageMillis`,`lastForegroundStartEpochMillis`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: UsageSession) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.packageName)
        statement.bindLong(3, entity.startEpochMillis)
        val _tmpEndEpochMillis: Long? = entity.endEpochMillis
        if (_tmpEndEpochMillis == null) {
          statement.bindNull(4)
        } else {
          statement.bindLong(4, _tmpEndEpochMillis)
        }
        statement.bindLong(5, entity.scrollsUsed.toLong())
        val _tmpChosenSessionLimit: Int? = entity.chosenSessionLimit
        if (_tmpChosenSessionLimit == null) {
          statement.bindNull(6)
        } else {
          statement.bindLong(6, _tmpChosenSessionLimit.toLong())
        }
        val _tmp: String = __halterConverters.limitTypeToString(entity.limitType)
        statement.bindText(7, _tmp)
        statement.bindLong(8, entity.accumulatedUsageMillis)
        val _tmpLastForegroundStartEpochMillis: Long? = entity.lastForegroundStartEpochMillis
        if (_tmpLastForegroundStartEpochMillis == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpLastForegroundStartEpochMillis)
        }
      }
    }
  }

  public override suspend fun upsert(session: UsageSession): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfUsageSession.insertAndReturnId(_connection, session)
    _result
  }

  public override suspend fun activeSession(): UsageSession? {
    val _sql: String = "SELECT * FROM usage_sessions WHERE endEpochMillis IS NULL ORDER BY startEpochMillis DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPackageName: Int = getColumnIndexOrThrow(_stmt, "packageName")
        val _columnIndexOfStartEpochMillis: Int = getColumnIndexOrThrow(_stmt, "startEpochMillis")
        val _columnIndexOfEndEpochMillis: Int = getColumnIndexOrThrow(_stmt, "endEpochMillis")
        val _columnIndexOfScrollsUsed: Int = getColumnIndexOrThrow(_stmt, "scrollsUsed")
        val _columnIndexOfChosenSessionLimit: Int = getColumnIndexOrThrow(_stmt, "chosenSessionLimit")
        val _columnIndexOfLimitType: Int = getColumnIndexOrThrow(_stmt, "limitType")
        val _columnIndexOfAccumulatedUsageMillis: Int = getColumnIndexOrThrow(_stmt, "accumulatedUsageMillis")
        val _columnIndexOfLastForegroundStartEpochMillis: Int = getColumnIndexOrThrow(_stmt, "lastForegroundStartEpochMillis")
        val _result: UsageSession?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPackageName: String
          _tmpPackageName = _stmt.getText(_columnIndexOfPackageName)
          val _tmpStartEpochMillis: Long
          _tmpStartEpochMillis = _stmt.getLong(_columnIndexOfStartEpochMillis)
          val _tmpEndEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfEndEpochMillis)) {
            _tmpEndEpochMillis = null
          } else {
            _tmpEndEpochMillis = _stmt.getLong(_columnIndexOfEndEpochMillis)
          }
          val _tmpScrollsUsed: Int
          _tmpScrollsUsed = _stmt.getLong(_columnIndexOfScrollsUsed).toInt()
          val _tmpChosenSessionLimit: Int?
          if (_stmt.isNull(_columnIndexOfChosenSessionLimit)) {
            _tmpChosenSessionLimit = null
          } else {
            _tmpChosenSessionLimit = _stmt.getLong(_columnIndexOfChosenSessionLimit).toInt()
          }
          val _tmpLimitType: LimitType
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfLimitType)
          _tmpLimitType = __halterConverters.stringToLimitType(_tmp)
          val _tmpAccumulatedUsageMillis: Long
          _tmpAccumulatedUsageMillis = _stmt.getLong(_columnIndexOfAccumulatedUsageMillis)
          val _tmpLastForegroundStartEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfLastForegroundStartEpochMillis)) {
            _tmpLastForegroundStartEpochMillis = null
          } else {
            _tmpLastForegroundStartEpochMillis = _stmt.getLong(_columnIndexOfLastForegroundStartEpochMillis)
          }
          _result = UsageSession(_tmpId,_tmpPackageName,_tmpStartEpochMillis,_tmpEndEpochMillis,_tmpScrollsUsed,_tmpChosenSessionLimit,_tmpLimitType,_tmpAccumulatedUsageMillis,_tmpLastForegroundStartEpochMillis)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun activeForPackage(packageName: String): UsageSession? {
    val _sql: String = "SELECT * FROM usage_sessions WHERE packageName = ? AND endEpochMillis IS NULL ORDER BY startEpochMillis DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, packageName)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPackageName: Int = getColumnIndexOrThrow(_stmt, "packageName")
        val _columnIndexOfStartEpochMillis: Int = getColumnIndexOrThrow(_stmt, "startEpochMillis")
        val _columnIndexOfEndEpochMillis: Int = getColumnIndexOrThrow(_stmt, "endEpochMillis")
        val _columnIndexOfScrollsUsed: Int = getColumnIndexOrThrow(_stmt, "scrollsUsed")
        val _columnIndexOfChosenSessionLimit: Int = getColumnIndexOrThrow(_stmt, "chosenSessionLimit")
        val _columnIndexOfLimitType: Int = getColumnIndexOrThrow(_stmt, "limitType")
        val _columnIndexOfAccumulatedUsageMillis: Int = getColumnIndexOrThrow(_stmt, "accumulatedUsageMillis")
        val _columnIndexOfLastForegroundStartEpochMillis: Int = getColumnIndexOrThrow(_stmt, "lastForegroundStartEpochMillis")
        val _result: UsageSession?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPackageName: String
          _tmpPackageName = _stmt.getText(_columnIndexOfPackageName)
          val _tmpStartEpochMillis: Long
          _tmpStartEpochMillis = _stmt.getLong(_columnIndexOfStartEpochMillis)
          val _tmpEndEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfEndEpochMillis)) {
            _tmpEndEpochMillis = null
          } else {
            _tmpEndEpochMillis = _stmt.getLong(_columnIndexOfEndEpochMillis)
          }
          val _tmpScrollsUsed: Int
          _tmpScrollsUsed = _stmt.getLong(_columnIndexOfScrollsUsed).toInt()
          val _tmpChosenSessionLimit: Int?
          if (_stmt.isNull(_columnIndexOfChosenSessionLimit)) {
            _tmpChosenSessionLimit = null
          } else {
            _tmpChosenSessionLimit = _stmt.getLong(_columnIndexOfChosenSessionLimit).toInt()
          }
          val _tmpLimitType: LimitType
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfLimitType)
          _tmpLimitType = __halterConverters.stringToLimitType(_tmp)
          val _tmpAccumulatedUsageMillis: Long
          _tmpAccumulatedUsageMillis = _stmt.getLong(_columnIndexOfAccumulatedUsageMillis)
          val _tmpLastForegroundStartEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfLastForegroundStartEpochMillis)) {
            _tmpLastForegroundStartEpochMillis = null
          } else {
            _tmpLastForegroundStartEpochMillis = _stmt.getLong(_columnIndexOfLastForegroundStartEpochMillis)
          }
          _result = UsageSession(_tmpId,_tmpPackageName,_tmpStartEpochMillis,_tmpEndEpochMillis,_tmpScrollsUsed,_tmpChosenSessionLimit,_tmpLimitType,_tmpAccumulatedUsageMillis,_tmpLastForegroundStartEpochMillis)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeSince(fromMillis: Long): Flow<List<UsageSession>> {
    val _sql: String = "SELECT * FROM usage_sessions WHERE startEpochMillis >= ? ORDER BY startEpochMillis DESC"
    return createFlow(__db, false, arrayOf("usage_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, fromMillis)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPackageName: Int = getColumnIndexOrThrow(_stmt, "packageName")
        val _columnIndexOfStartEpochMillis: Int = getColumnIndexOrThrow(_stmt, "startEpochMillis")
        val _columnIndexOfEndEpochMillis: Int = getColumnIndexOrThrow(_stmt, "endEpochMillis")
        val _columnIndexOfScrollsUsed: Int = getColumnIndexOrThrow(_stmt, "scrollsUsed")
        val _columnIndexOfChosenSessionLimit: Int = getColumnIndexOrThrow(_stmt, "chosenSessionLimit")
        val _columnIndexOfLimitType: Int = getColumnIndexOrThrow(_stmt, "limitType")
        val _columnIndexOfAccumulatedUsageMillis: Int = getColumnIndexOrThrow(_stmt, "accumulatedUsageMillis")
        val _columnIndexOfLastForegroundStartEpochMillis: Int = getColumnIndexOrThrow(_stmt, "lastForegroundStartEpochMillis")
        val _result: MutableList<UsageSession> = mutableListOf()
        while (_stmt.step()) {
          val _item: UsageSession
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPackageName: String
          _tmpPackageName = _stmt.getText(_columnIndexOfPackageName)
          val _tmpStartEpochMillis: Long
          _tmpStartEpochMillis = _stmt.getLong(_columnIndexOfStartEpochMillis)
          val _tmpEndEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfEndEpochMillis)) {
            _tmpEndEpochMillis = null
          } else {
            _tmpEndEpochMillis = _stmt.getLong(_columnIndexOfEndEpochMillis)
          }
          val _tmpScrollsUsed: Int
          _tmpScrollsUsed = _stmt.getLong(_columnIndexOfScrollsUsed).toInt()
          val _tmpChosenSessionLimit: Int?
          if (_stmt.isNull(_columnIndexOfChosenSessionLimit)) {
            _tmpChosenSessionLimit = null
          } else {
            _tmpChosenSessionLimit = _stmt.getLong(_columnIndexOfChosenSessionLimit).toInt()
          }
          val _tmpLimitType: LimitType
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfLimitType)
          _tmpLimitType = __halterConverters.stringToLimitType(_tmp)
          val _tmpAccumulatedUsageMillis: Long
          _tmpAccumulatedUsageMillis = _stmt.getLong(_columnIndexOfAccumulatedUsageMillis)
          val _tmpLastForegroundStartEpochMillis: Long?
          if (_stmt.isNull(_columnIndexOfLastForegroundStartEpochMillis)) {
            _tmpLastForegroundStartEpochMillis = null
          } else {
            _tmpLastForegroundStartEpochMillis = _stmt.getLong(_columnIndexOfLastForegroundStartEpochMillis)
          }
          _item = UsageSession(_tmpId,_tmpPackageName,_tmpStartEpochMillis,_tmpEndEpochMillis,_tmpScrollsUsed,_tmpChosenSessionLimit,_tmpLimitType,_tmpAccumulatedUsageMillis,_tmpLastForegroundStartEpochMillis)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun totalUsageMillis(
    packageName: String,
    dayStartMillis: Long,
    nowMillis: Long,
  ): Long {
    val _sql: String = "SELECT COALESCE(SUM(COALESCE(endEpochMillis, ?) - startEpochMillis), 0) FROM usage_sessions WHERE packageName = ? AND startEpochMillis >= ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, nowMillis)
        _argIndex = 2
        _stmt.bindText(_argIndex, packageName)
        _argIndex = 3
        _stmt.bindLong(_argIndex, dayStartMillis)
        val _result: Long
        if (_stmt.step()) {
          val _tmp: Long
          _tmp = _stmt.getLong(0)
          _result = _tmp
        } else {
          _result = 0L
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun anySessionExists(): Boolean {
    val _sql: String = "SELECT COUNT(*) > 0 FROM usage_sessions"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Boolean
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp != 0
        } else {
          _result = false
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun close(id: Long, endMillis: Long) {
    val _sql: String = "UPDATE usage_sessions SET endEpochMillis = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, endMillis)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun addScrolls(id: Long, count: Int) {
    val _sql: String = "UPDATE usage_sessions SET scrollsUsed = scrollsUsed + ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, count.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun pauseUsage(id: Long, elapsedMillis: Long) {
    val _sql: String = """
        |
        |        UPDATE usage_sessions
        |        SET accumulatedUsageMillis = accumulatedUsageMillis + ?,
        |            lastForegroundStartEpochMillis = NULL
        |        WHERE id = ?
        |        
        """.trimMargin()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, elapsedMillis)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun resumeUsage(id: Long, startMillis: Long) {
    val _sql: String = "UPDATE usage_sessions SET lastForegroundStartEpochMillis = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startMillis)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id)
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
