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
public class FocusSessionDao_Impl(
  __db: RoomDatabase,
) : FocusSessionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFocusSession: EntityInsertAdapter<FocusSession>
  init {
    this.__db = __db
    this.__insertAdapterOfFocusSession = object : EntityInsertAdapter<FocusSession>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `focus_sessions` (`id`,`startEpochMillis`,`durationMinutes`,`completed`,`interruptionCount`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FocusSession) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.startEpochMillis)
        statement.bindLong(3, entity.durationMinutes.toLong())
        val _tmp: Int = if (entity.completed) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindLong(5, entity.interruptionCount.toLong())
      }
    }
  }

  public override suspend fun upsert(session: FocusSession): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfFocusSession.insertAndReturnId(_connection, session)
    _result
  }

  public override fun observeAll(): Flow<List<FocusSession>> {
    val _sql: String = "SELECT * FROM focus_sessions ORDER BY startEpochMillis DESC"
    return createFlow(__db, false, arrayOf("focus_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartEpochMillis: Int = getColumnIndexOrThrow(_stmt, "startEpochMillis")
        val _columnIndexOfDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "durationMinutes")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfInterruptionCount: Int = getColumnIndexOrThrow(_stmt, "interruptionCount")
        val _result: MutableList<FocusSession> = mutableListOf()
        while (_stmt.step()) {
          val _item: FocusSession
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartEpochMillis: Long
          _tmpStartEpochMillis = _stmt.getLong(_columnIndexOfStartEpochMillis)
          val _tmpDurationMinutes: Int
          _tmpDurationMinutes = _stmt.getLong(_columnIndexOfDurationMinutes).toInt()
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpInterruptionCount: Int
          _tmpInterruptionCount = _stmt.getLong(_columnIndexOfInterruptionCount).toInt()
          _item = FocusSession(_tmpId,_tmpStartEpochMillis,_tmpDurationMinutes,_tmpCompleted,_tmpInterruptionCount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun active(): FocusSession? {
    val _sql: String = "SELECT * FROM focus_sessions WHERE completed = 0 ORDER BY startEpochMillis DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfStartEpochMillis: Int = getColumnIndexOrThrow(_stmt, "startEpochMillis")
        val _columnIndexOfDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "durationMinutes")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfInterruptionCount: Int = getColumnIndexOrThrow(_stmt, "interruptionCount")
        val _result: FocusSession?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpStartEpochMillis: Long
          _tmpStartEpochMillis = _stmt.getLong(_columnIndexOfStartEpochMillis)
          val _tmpDurationMinutes: Int
          _tmpDurationMinutes = _stmt.getLong(_columnIndexOfDurationMinutes).toInt()
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpInterruptionCount: Int
          _tmpInterruptionCount = _stmt.getLong(_columnIndexOfInterruptionCount).toInt()
          _result = FocusSession(_tmpId,_tmpStartEpochMillis,_tmpDurationMinutes,_tmpCompleted,_tmpInterruptionCount)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun incrementInterruptions(id: Long) {
    val _sql: String = "UPDATE focus_sessions SET interruptionCount = interruptionCount + 1 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
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
