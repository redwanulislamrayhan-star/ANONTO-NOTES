package com.example.data

import kotlinx.coroutines.flow.Flow

class LedgerRepository(private val ledgerDao: LedgerDao) {
    val allEntries: Flow<List<LedgerEntry>> = ledgerDao.getAllEntries()

    fun getEntriesForAccount(account: String): Flow<List<LedgerEntry>> {
        return ledgerDao.getEntriesForAccount(account)
    }

    fun getEntriesForMonth(account: String, yearMonth: String): Flow<List<LedgerEntry>> {
        return ledgerDao.getEntriesForMonth(account, yearMonth)
    }

    fun getEntriesForYear(account: String, year: Int): Flow<List<LedgerEntry>> {
        return ledgerDao.getEntriesForYear(account, year)
    }

    fun searchGlobally(account: String, query: String): Flow<List<LedgerEntry>> {
        return ledgerDao.searchEntriesGlobally(account, query)
    }

    suspend fun insert(entry: LedgerEntry): Long = ledgerDao.insertEntry(entry)

    suspend fun update(entry: LedgerEntry) = ledgerDao.updateEntry(entry)

    suspend fun delete(entry: LedgerEntry) = ledgerDao.deleteEntry(entry)

    suspend fun deleteById(id: Long) = ledgerDao.deleteById(id)

    suspend fun clearAll() = ledgerDao.clearAll()
}
