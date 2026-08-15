package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LedgerEntry
import com.example.data.LedgerRepository
import com.example.util.LedgerParseResult
import com.example.util.LedgerParser
import com.example.util.TagCategory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CanvasMode {
    EXPENSE,
    INCOME
}

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LedgerRepository

    val syncManager = com.example.data.FirestoreSyncManager.getInstance(application)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = LedgerRepository(database.ledgerDao())
        syncManager.setRepository(repository)
    }

    private val prefs = application.getSharedPreferences("quicklist_prefs", Context.MODE_PRIVATE)

    // 1. Account Profile State
    private val _selectedAccount = MutableStateFlow(prefs.getString("selected_account", LedgerEntry.DEFAULT_ACCOUNT) ?: LedgerEntry.DEFAULT_ACCOUNT)
    val selectedAccount: StateFlow<String> = _selectedAccount.asStateFlow()

    // 2. Free-Flow Year & Month State
    private val calendar = Calendar.getInstance()
    private val currentYearDefault = calendar.get(Calendar.YEAR)
    private val currentMonthDefault = calendar.get(Calendar.MONTH) + 1 // 1-12

    private val _selectedYear = MutableStateFlow(prefs.getInt("selected_year", currentYearDefault))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(prefs.getInt("selected_month", currentMonthDefault))
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    val yearMonth: StateFlow<String> = combine(_selectedYear, _selectedMonth) { yr, mo ->
        String.format(Locale.US, "%04d-%02d", yr, mo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), String.format(Locale.US, "%04d-%02d", currentYearDefault, currentMonthDefault))

    // 3. Dual Canvas State (Income vs Expense)
    private val _canvasMode = MutableStateFlow(CanvasMode.EXPENSE)
    val canvasMode: StateFlow<CanvasMode> = _canvasMode.asStateFlow()

    private val _expenseNote = MutableStateFlow("")
    val expenseNote: StateFlow<String> = _expenseNote.asStateFlow()

    private val _incomeNote = MutableStateFlow("")
    val incomeNote: StateFlow<String> = _incomeNote.asStateFlow()

    // Active parsed result depending on current CanvasMode
    private val _expenseParseResult = MutableStateFlow(LedgerParser.parseNote(_expenseNote.value, isIncomeMode = false))
    val expenseParseResult: StateFlow<LedgerParseResult> = _expenseParseResult.asStateFlow()

    private val _incomeParseResult = MutableStateFlow(LedgerParser.parseNote(_incomeNote.value, isIncomeMode = true))
    val incomeParseResult: StateFlow<LedgerParseResult> = _incomeParseResult.asStateFlow()

    // 4. Monthly Budget State
    private val _monthlyBudget = MutableStateFlow(prefs.getFloat("monthly_budget", 50000f).toDouble())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    // Preferences & Settings state
    private val _currencySymbol = MutableStateFlow(prefs.getString("currency", "৳") ?: "৳")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _useBengaliNumerals = MutableStateFlow(prefs.getBoolean("use_bengali", true))
    val useBengaliNumerals: StateFlow<Boolean> = _useBengaliNumerals.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<TagCategory?>(null)
    val selectedCategoryFilter: StateFlow<TagCategory?> = _selectedCategoryFilter.asStateFlow()

    // PIN Security state
    private val _userPin = MutableStateFlow(prefs.getString("user_pin", null))
    val userPin: StateFlow<String?> = _userPin.asStateFlow()

    private val _isAppLocked = MutableStateFlow(!_userPin.value.isNullOrEmpty())
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Date Backdate picker
    private val _selectedBackdate = MutableStateFlow<Long?>(null)
    val selectedBackdate: StateFlow<Long?> = _selectedBackdate.asStateFlow()

    // Dialog & Navigation states
    private val _showAnalyticsDialog = MutableStateFlow(false)
    val showAnalyticsDialog: StateFlow<Boolean> = _showAnalyticsDialog.asStateFlow()

    private val _showVoiceDialog = MutableStateFlow(false)
    val showVoiceDialog: StateFlow<Boolean> = _showVoiceDialog.asStateFlow()

    private val _showPinSetupDialog = MutableStateFlow(false)
    val showPinSetupDialog: StateFlow<Boolean> = _showPinSetupDialog.asStateFlow()

    private val _showYearlyArchiveDialog = MutableStateFlow(false)
    val showYearlyArchiveDialog: StateFlow<Boolean> = _showYearlyArchiveDialog.asStateFlow()

    private val _showBudgetSetupDialog = MutableStateFlow(false)
    val showBudgetSetupDialog: StateFlow<Boolean> = _showBudgetSetupDialog.asStateFlow()

    private val _showQuickAddLedgerDialog = MutableStateFlow(false)
    val showQuickAddLedgerDialog: StateFlow<Boolean> = _showQuickAddLedgerDialog.asStateFlow()

    private val _showGoogleAuthDialog = MutableStateFlow(false)
    val showGoogleAuthDialog: StateFlow<Boolean> = _showGoogleAuthDialog.asStateFlow()

    private val _showHistoryDialog = MutableStateFlow(false)
    val showHistoryDialog: StateFlow<Boolean> = _showHistoryDialog.asStateFlow()

    private val _breakdownType = MutableStateFlow<String?>(null) // "EXPENSE" or "INCOME"
    val breakdownType: StateFlow<String?> = _breakdownType.asStateFlow()

    // 5. Database Entries for Current Account & Selected Month
    val monthEntries: StateFlow<List<LedgerEntry>> = combine(
        _selectedAccount,
        yearMonth,
        repository.allEntries
    ) { account, ym, all ->
        all.filter { it.ledgerAccount == account && it.yearMonth == ym }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. Database Entries for Current Account & Selected Year (for Yearly Overview Archive)
    val yearEntries: StateFlow<List<LedgerEntry>> = combine(
        _selectedAccount,
        _selectedYear,
        repository.allEntries
    ) { account, yr, all ->
        all.filter { it.ledgerAccount == account && it.year == yr }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7. Global Search Results (across ALL years for current Account)
    val globalSearchResults: StateFlow<List<LedgerEntry>> = combine(
        _selectedAccount,
        _searchQuery,
        repository.allEntries
    ) { account, query, all ->
        if (query.isBlank()) {
            emptyList()
        } else {
            all.filter {
                it.ledgerAccount == account && (
                    it.rawNote.contains(query, ignoreCase = true) ||
                    it.summaryText.contains(query, ignoreCase = true) ||
                    it.tagsJson.contains(query, ignoreCase = true)
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered entries for main Timeline View
    val filteredMonthEntries: StateFlow<List<LedgerEntry>> = combine(
        monthEntries,
        _searchQuery,
        _selectedCategoryFilter
    ) { entries, query, category ->
        var list = entries
        if (query.isNotBlank()) {
            list = list.filter {
                it.rawNote.contains(query, ignoreCase = true) ||
                it.summaryText.contains(query, ignoreCase = true) ||
                it.tagsJson.contains(query, ignoreCase = true)
            }
        }
        if (category != null) {
            list = list.filter {
                it.tagsJson.contains(category.nameEn, ignoreCase = true) ||
                it.tagsJson.contains(category.nameBn, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Auto-Save Session Entry IDs
    private var activeExpenseEntryId: Long? = null
    private var activeIncomeEntryId: Long? = null
    private var autoSaveJob: Job? = null

    // --- ACTIONS & METHODS ---

    fun selectAccount(account: String) {
        activeExpenseEntryId = null
        activeIncomeEntryId = null
        _selectedAccount.value = account
        prefs.edit().putString("selected_account", account).apply()
    }

    fun selectYear(year: Int) {
        activeExpenseEntryId = null
        activeIncomeEntryId = null
        _selectedYear.value = year
        prefs.edit().putInt("selected_year", year).apply()
    }

    fun selectMonth(month: Int) {
        activeExpenseEntryId = null
        activeIncomeEntryId = null
        _selectedMonth.value = month
        prefs.edit().putInt("selected_month", month).apply()
    }

    fun setCanvasMode(mode: CanvasMode) {
        _canvasMode.value = mode
    }

    fun onNoteChanged(newNote: String) {
        val isIncome = _canvasMode.value == CanvasMode.INCOME
        if (!isIncome) {
            _expenseNote.value = newNote
            _expenseParseResult.value = LedgerParser.parseNote(newNote, isIncomeMode = false)
        } else {
            _incomeNote.value = newNote
            _incomeParseResult.value = LedgerParser.parseNote(newNote, isIncomeMode = true)
        }

        // Auto-Save and Auto-Delete coroutine
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(150) // Smooth typing debounce
            val parseResult = if (isIncome) _incomeParseResult.value else _expenseParseResult.value
            val currentActiveId = if (isIncome) activeIncomeEntryId else activeExpenseEntryId

            if (newNote.trim().isEmpty()) {
                // If text is cleared or deleted, auto-delete from database immediately
                if (currentActiveId != null) {
                    repository.deleteById(currentActiveId)
                    syncManager.deleteEntryFromCloud(currentActiveId)
                    if (isIncome) activeIncomeEntryId = null else activeExpenseEntryId = null
                }
            } else {
                // Auto-Save / Upsert entry into database
                val tagsString = parseResult.allTags.joinToString(",") { "${it.nameEn}|${it.nameBn}" }
                val summariesString = parseResult.itemSummaries.joinToString("\n")
                val timestamp = _selectedBackdate.value ?: System.currentTimeMillis()

                val entry = LedgerEntry(
                    id = currentActiveId ?: 0L,
                    rawNote = newNote,
                    totalAmount = parseResult.totalSum,
                    summaryText = summariesString,
                    tagsJson = tagsString,
                    timestamp = timestamp,
                    yearMonth = yearMonth.value,
                    year = _selectedYear.value,
                    month = _selectedMonth.value,
                    entryType = if (isIncome) LedgerEntry.ENTRY_TYPE_INCOME else LedgerEntry.ENTRY_TYPE_EXPENSE,
                    ledgerAccount = _selectedAccount.value,
                    currencySymbol = _currencySymbol.value
                )

                val insertedId = repository.insert(entry)
                val syncedEntry = entry.copy(id = insertedId)
                if (isIncome) {
                    activeIncomeEntryId = insertedId
                } else {
                    activeExpenseEntryId = insertedId
                }

                // Sync with Firestore Cloud
                syncManager.syncEntryToCloud(syncedEntry)
            }
        }
    }

    fun appendTextToNote(textToAppend: String) {
        val current = if (_canvasMode.value == CanvasMode.EXPENSE) _expenseNote.value else _incomeNote.value
        val updated = if (current.isBlank()) textToAppend else "$current\n$textToAppend"
        onNoteChanged(updated)
    }

    fun clearNote() {
        onNoteChanged("")
    }

    fun deleteEntry(entry: LedgerEntry) {
        viewModelScope.launch {
            repository.delete(entry)
            syncManager.deleteEntryFromCloud(entry.id)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: TagCategory?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun setBackdate(timestampMillis: Long?) {
        _selectedBackdate.value = timestampMillis
    }

    fun setMonthlyBudget(budget: Double) {
        _monthlyBudget.value = budget
        prefs.edit().putFloat("monthly_budget", budget.toFloat()).apply()
    }

    fun setCurrency(symbol: String) {
        _currencySymbol.value = symbol
        prefs.edit().putString("currency", symbol).apply()
    }

    fun toggleBengaliNumerals() {
        val newSetting = !_useBengaliNumerals.value
        _useBengaliNumerals.value = newSetting
        prefs.edit().putBoolean("use_bengali", newSetting).apply()
    }

    fun setShowAnalyticsDialog(show: Boolean) {
        _showAnalyticsDialog.value = show
    }

    fun setShowVoiceDialog(show: Boolean) {
        _showVoiceDialog.value = show
    }

    fun setShowPinSetupDialog(show: Boolean) {
        _showPinSetupDialog.value = show
    }

    fun setShowYearlyArchiveDialog(show: Boolean) {
        _showYearlyArchiveDialog.value = show
    }

    fun setShowBudgetSetupDialog(show: Boolean) {
        _showBudgetSetupDialog.value = show
    }

    fun setShowQuickAddLedgerDialog(show: Boolean) {
        _showQuickAddLedgerDialog.value = show
    }

    fun setShowGoogleAuthDialog(show: Boolean) {
        _showGoogleAuthDialog.value = show
    }

    fun setShowHistoryDialog(show: Boolean) {
        _showHistoryDialog.value = show
    }

    fun openBreakdown(type: String) {
        _breakdownType.value = type
    }

    fun closeBreakdown() {
        _breakdownType.value = null
    }

    fun setPin(pin: String?) {
        _userPin.value = pin
        if (pin.isNullOrEmpty()) {
            prefs.edit().remove("user_pin").apply()
            _isAppLocked.value = false
        } else {
            prefs.edit().putString("user_pin", pin).apply()
            _isAppLocked.value = false
        }
    }

    fun unlockApp(enteredPin: String): Boolean {
        if (_userPin.value == enteredPin) {
            _isAppLocked.value = false
            return true
        }
        return false
    }

    fun lockApp() {
        if (!_userPin.value.isNullOrEmpty()) {
            _isAppLocked.value = true
        }
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LedgerViewModel(application) as T
            }
        }
    }
}
