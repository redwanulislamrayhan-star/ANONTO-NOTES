package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entries WHERE ledgerAccount = :account ORDER BY timestamp DESC")
    fun getEntriesForAccount(account: String): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entries WHERE ledgerAccount = :account AND yearMonth = :yearMonth ORDER BY timestamp DESC")
    fun getEntriesForMonth(account: String, yearMonth: String): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entries WHERE ledgerAccount = :account AND year = :year ORDER BY timestamp DESC")
    fun getEntriesForYear(account: String, year: Int): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entries WHERE ledgerAccount = :account AND (rawNote LIKE '%' || :query || '%' OR summaryText LIKE '%' || :query || '%' OR tagsJson LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchEntriesGlobally(account: String, query: String): Flow<List<LedgerEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntry): Long

    @Update
    suspend fun updateEntry(entry: LedgerEntry)

    @Delete
    suspend fun deleteEntry(entry: LedgerEntry)

    @Query("DELETE FROM ledger_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM ledger_entries")
    suspend fun clearAll()
}
