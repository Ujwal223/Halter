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
public class RoutineDao_Impl(
  __db: RoomDatabase,
) : RoutineDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfRoutine: EntityInsertAdapter<Routine>
  init {
    this.__db = __db
    this.__insertAdapterOfRoutine = object : EntityInsertAdapter<Routine>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `routines` (`id`,`name`,`packageNames`,`startMinuteOfDay`,`endMinuteOfDay`,`daysOfWeekBitmask`,`isEnabled`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Routine) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.packageNames)
        statement.bindLong(4, entity.startMinuteOfDay.toLong())
        statement.bindLong(5, entity.endMinuteOfDay.toLong())
        statement.bindLong(6, entity.daysOfWeekBitmask.toLong())
        val _tmp: Int = if (entity.isEnabled) 1 else 0
        statement.bindLong(7, _tmp.toLong())
      }
    }
  }

  public override suspend fun upsert(routine: Routine): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfRoutine.insertAndReturnId(_connection, routine)
    _result
  }

  public override suspend fun enabled(): List<Routine> {
    val _sql: String = "SELECT * FROM routines WHERE isEnabled = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfPackageNames: Int = getColumnIndexOrThrow(_stmt, "packageNames")
        val _columnIndexOfStartMinuteOfDay: Int = getColumnIndexOrThrow(_stmt, "startMinuteOfDay")
        val _columnIndexOfEndMinuteOfDay: Int = getColumnIndexOrThrow(_stmt, "endMinuteOfDay")
        val _columnIndexOfDaysOfWeekBitmask: Int = getColumnIndexOrThrow(_stmt, "daysOfWeekBitmask")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _result: MutableList<Routine> = mutableListOf()
        while (_stmt.step()) {
          val _item: Routine
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpPackageNames: String
          _tmpPackageNames = _stmt.getText(_columnIndexOfPackageNames)
          val _tmpStartMinuteOfDay: Int
          _tmpStartMinuteOfDay = _stmt.getLong(_columnIndexOfStartMinuteOfDay).toInt()
          val _tmpEndMinuteOfDay: Int
          _tmpEndMinuteOfDay = _stmt.getLong(_columnIndexOfEndMinuteOfDay).toInt()
          val _tmpDaysOfWeekBitmask: Int
          _tmpDaysOfWeekBitmask = _stmt.getLong(_columnIndexOfDaysOfWeekBitmask).toInt()
          val _tmpIsEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp != 0
          _item = Routine(_tmpId,_tmpName,_tmpPackageNames,_tmpStartMinuteOfDay,_tmpEndMinuteOfDay,_tmpDaysOfWeekBitmask,_tmpIsEnabled)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAll(): Flow<List<Routine>> {
    val _sql: String = "SELECT * FROM routines ORDER BY name"
    return createFlow(__db, false, arrayOf("routines")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfPackageNames: Int = getColumnIndexOrThrow(_stmt, "packageNames")
        val _columnIndexOfStartMinuteOfDay: Int = getColumnIndexOrThrow(_stmt, "startMinuteOfDay")
        val _columnIndexOfEndMinuteOfDay: Int = getColumnIndexOrThrow(_stmt, "endMinuteOfDay")
        val _columnIndexOfDaysOfWeekBitmask: Int = getColumnIndexOrThrow(_stmt, "daysOfWeekBitmask")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _result: MutableList<Routine> = mutableListOf()
        while (_stmt.step()) {
          val _item: Routine
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpPackageNames: String
          _tmpPackageNames = _stmt.getText(_columnIndexOfPackageNames)
          val _tmpStartMinuteOfDay: Int
          _tmpStartMinuteOfDay = _stmt.getLong(_columnIndexOfStartMinuteOfDay).toInt()
          val _tmpEndMinuteOfDay: Int
          _tmpEndMinuteOfDay = _stmt.getLong(_columnIndexOfEndMinuteOfDay).toInt()
          val _tmpDaysOfWeekBitmask: Int
          _tmpDaysOfWeekBitmask = _stmt.getLong(_columnIndexOfDaysOfWeekBitmask).toInt()
          val _tmpIsEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp != 0
          _item = Routine(_tmpId,_tmpName,_tmpPackageNames,_tmpStartMinuteOfDay,_tmpEndMinuteOfDay,_tmpDaysOfWeekBitmask,_tmpIsEnabled)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(id: Long) {
    val _sql: String = "DELETE FROM routines WHERE id = ?"
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
