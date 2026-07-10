package com.ujwal.halter.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
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
public class ScrollEventDao_Impl(
  __db: RoomDatabase,
) : ScrollEventDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfScrollEvent: EntityInsertAdapter<ScrollEvent>

  private val __halterConverters: HalterConverters = HalterConverters()
  init {
    this.__db = __db
    this.__insertAdapterOfScrollEvent = object : EntityInsertAdapter<ScrollEvent>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `scroll_events` (`id`,`packageName`,`timestampEpochMillis`,`contentType`,`count`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ScrollEvent) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.packageName)
        statement.bindLong(3, entity.timestampEpochMillis)
        val _tmp: String = __halterConverters.contentTypeToString(entity.contentType)
        statement.bindText(4, _tmp)
        statement.bindLong(5, entity.count.toLong())
      }
    }
  }

  public override suspend fun insert(event: ScrollEvent): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfScrollEvent.insert(_connection, event)
  }

  public override suspend fun scrollsToday(packageName: String, dayStartMillis: Long): Int {
    val _sql: String = "SELECT COALESCE(SUM(count), 0) FROM scroll_events WHERE packageName = ? AND timestampEpochMillis >= ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, packageName)
        _argIndex = 2
        _stmt.bindLong(_argIndex, dayStartMillis)
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeSince(fromMillis: Long): Flow<List<ScrollEvent>> {
    val _sql: String = "SELECT * FROM scroll_events WHERE timestampEpochMillis >= ? ORDER BY timestampEpochMillis DESC"
    return createFlow(__db, false, arrayOf("scroll_events")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, fromMillis)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPackageName: Int = getColumnIndexOrThrow(_stmt, "packageName")
        val _columnIndexOfTimestampEpochMillis: Int = getColumnIndexOrThrow(_stmt, "timestampEpochMillis")
        val _columnIndexOfContentType: Int = getColumnIndexOrThrow(_stmt, "contentType")
        val _columnIndexOfCount: Int = getColumnIndexOrThrow(_stmt, "count")
        val _result: MutableList<ScrollEvent> = mutableListOf()
        while (_stmt.step()) {
          val _item: ScrollEvent
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPackageName: String
          _tmpPackageName = _stmt.getText(_columnIndexOfPackageName)
          val _tmpTimestampEpochMillis: Long
          _tmpTimestampEpochMillis = _stmt.getLong(_columnIndexOfTimestampEpochMillis)
          val _tmpContentType: ContentType
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfContentType)
          _tmpContentType = __halterConverters.stringToContentType(_tmp)
          val _tmpCount: Int
          _tmpCount = _stmt.getLong(_columnIndexOfCount).toInt()
          _item = ScrollEvent(_tmpId,_tmpPackageName,_tmpTimestampEpochMillis,_tmpContentType,_tmpCount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
