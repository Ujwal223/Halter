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
public class JournalDao_Impl(
  __db: RoomDatabase,
) : JournalDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfJournalEntry: EntityInsertAdapter<JournalEntry>

  private val __halterConverters: HalterConverters = HalterConverters()
  init {
    this.__db = __db
    this.__insertAdapterOfJournalEntry = object : EntityInsertAdapter<JournalEntry>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `journal_entries` (`id`,`packageName`,`timestampEpochMillis`,`reason`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: JournalEntry) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.packageName)
        statement.bindLong(3, entity.timestampEpochMillis)
        val _tmp: String = __halterConverters.journalReasonToString(entity.reason)
        statement.bindText(4, _tmp)
      }
    }
  }

  public override suspend fun insert(entry: JournalEntry): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfJournalEntry.insert(_connection, entry)
  }

  public override fun observeAll(): Flow<List<JournalEntry>> {
    val _sql: String = "SELECT * FROM journal_entries ORDER BY timestampEpochMillis DESC"
    return createFlow(__db, false, arrayOf("journal_entries")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPackageName: Int = getColumnIndexOrThrow(_stmt, "packageName")
        val _columnIndexOfTimestampEpochMillis: Int = getColumnIndexOrThrow(_stmt, "timestampEpochMillis")
        val _columnIndexOfReason: Int = getColumnIndexOrThrow(_stmt, "reason")
        val _result: MutableList<JournalEntry> = mutableListOf()
        while (_stmt.step()) {
          val _item: JournalEntry
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPackageName: String
          _tmpPackageName = _stmt.getText(_columnIndexOfPackageName)
          val _tmpTimestampEpochMillis: Long
          _tmpTimestampEpochMillis = _stmt.getLong(_columnIndexOfTimestampEpochMillis)
          val _tmpReason: JournalReason
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfReason)
          _tmpReason = __halterConverters.stringToJournalReason(_tmp)
          _item = JournalEntry(_tmpId,_tmpPackageName,_tmpTimestampEpochMillis,_tmpReason)
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
