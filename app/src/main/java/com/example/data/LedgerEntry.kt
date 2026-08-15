package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ledger_entries")
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawNote: String,
    val totalAmount: Double,
    val summaryText: String,
    val tagsJson: String, // Comma separated tag names or category labels
    val timestamp: Long = System.currentTimeMillis(),
    val yearMonth: String, // Format: "YYYY-MM", e.g. "2026-08"
    val year: Int = 2026,
    val month: Int = 8,
    val entryType: String = ENTRY_TYPE_EXPENSE, // "EXPENSE" or "INCOME"
    val ledgerAccount: String = DEFAULT_ACCOUNT, // "ফ্যামিলি খাতা", "ব্যক্তিগত খাতা", "ব্যবসা খাতা"
    val currencySymbol: String = "৳"
) {
    companion object {
        const val ENTRY_TYPE_EXPENSE = "EXPENSE"
        const val ENTRY_TYPE_INCOME = "INCOME"

        const val ACCOUNT_FAMILY = "ফ্যামিলি খাতা"
        const val ACCOUNT_PERSONAL = "ব্যক্তিগত খাতা"
        const val ACCOUNT_BUSINESS = "ব্যবসা খাতা"
        const val DEFAULT_ACCOUNT = ACCOUNT_FAMILY

        val ALL_ACCOUNTS = listOf(ACCOUNT_FAMILY, ACCOUNT_PERSONAL, ACCOUNT_BUSINESS)
    }
}
