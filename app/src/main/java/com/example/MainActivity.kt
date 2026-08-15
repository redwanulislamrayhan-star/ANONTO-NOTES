package com.example

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.LedgerEntry
import com.example.ui.CanvasMode
import com.example.ui.LedgerViewModel
import com.example.ui.components.BudgetSetupDialog
import com.example.ui.components.CategoryPieChart
import com.example.ui.components.DetailedHistoryDialog
import com.example.ui.components.DualCanvasSelector
import com.example.ui.components.GoogleAuthDialog
import com.example.ui.components.LiveSumPill
import com.example.ui.components.MonthBreakdownDialog
import com.example.ui.components.NetFinancialCard
import com.example.ui.components.NotepadCanvas
import com.example.ui.components.PinSetupDialog
import com.example.ui.components.PinUnlockOverlay
import com.example.ui.components.QuickAddLedgerDialog
import com.example.ui.components.TimelineView
import com.example.ui.components.TopHeaderBar
import com.example.ui.components.VoiceInputDialog
import com.example.ui.components.YearlyOverviewDialog
import com.example.ui.theme.QuickListLedgerTheme
import com.example.util.LedgerParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickListLedgerTheme {
                val context = LocalContext.current
                val app = context.applicationContext as Application
                val viewModel: LedgerViewModel = viewModel(factory = LedgerViewModel.Factory(app))

                val isLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

                if (isLocked) {
                    PinUnlockOverlay(
                        onUnlockAttempt = { pin -> viewModel.unlockApp(pin) },
                        useBengali = viewModel.useBengaliNumerals.collectAsStateWithLifecycle().value
                    )
                } else {
                    QuickListLedgerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickListLedgerApp(viewModel: LedgerViewModel) {
    val context = LocalContext.current

    // Observe ViewModel States
    val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val canvasMode by viewModel.canvasMode.collectAsStateWithLifecycle()

    val expenseNote by viewModel.expenseNote.collectAsStateWithLifecycle()
    val incomeNote by viewModel.incomeNote.collectAsStateWithLifecycle()
    val expenseParseResult by viewModel.expenseParseResult.collectAsStateWithLifecycle()
    val incomeParseResult by viewModel.incomeParseResult.collectAsStateWithLifecycle()

    val currentNote = if (canvasMode == CanvasMode.INCOME) incomeNote else expenseNote
    val currentParseResult = if (canvasMode == CanvasMode.INCOME) incomeParseResult else expenseParseResult

    val monthEntries by viewModel.monthEntries.collectAsStateWithLifecycle()
    val yearEntries by viewModel.yearEntries.collectAsStateWithLifecycle()
    val filteredMonthEntries by viewModel.filteredMonthEntries.collectAsStateWithLifecycle()
    val globalSearchResults by viewModel.globalSearchResults.collectAsStateWithLifecycle()

    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val useBengaliNumerals by viewModel.useBengaliNumerals.collectAsStateWithLifecycle()
    val selectedBackdate by viewModel.selectedBackdate.collectAsStateWithLifecycle()
    val userPin by viewModel.userPin.collectAsStateWithLifecycle()

    // Dialog States
    val currentUser by viewModel.syncManager.currentUser.collectAsStateWithLifecycle()
    val showAnalyticsDialog by viewModel.showAnalyticsDialog.collectAsStateWithLifecycle()
    val showVoiceDialog by viewModel.showVoiceDialog.collectAsStateWithLifecycle()
    val showPinSetupDialog by viewModel.showPinSetupDialog.collectAsStateWithLifecycle()
    val showYearlyArchiveDialog by viewModel.showYearlyArchiveDialog.collectAsStateWithLifecycle()
    val showBudgetSetupDialog by viewModel.showBudgetSetupDialog.collectAsStateWithLifecycle()
    val showQuickAddLedgerDialog by viewModel.showQuickAddLedgerDialog.collectAsStateWithLifecycle()
    val showGoogleAuthDialog by viewModel.showGoogleAuthDialog.collectAsStateWithLifecycle()
    val showHistoryDialog by viewModel.showHistoryDialog.collectAsStateWithLifecycle()
    val breakdownType by viewModel.breakdownType.collectAsStateWithLifecycle()

    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Monthly Sums for Dual Canvas Selector
    val monthIncomeTotal = monthEntries.filter { it.entryType == LedgerEntry.ENTRY_TYPE_INCOME }.sumOf { it.totalAmount }
    val monthExpenseTotal = monthEntries.filter { it.entryType == LedgerEntry.ENTRY_TYPE_EXPENSE }.sumOf { it.totalAmount }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Navigation Header Bar (Profile Account | Year | Month | + Quick Add)
            TopHeaderBar(
                selectedAccount = selectedAccount,
                onAccountSelect = { viewModel.selectAccount(it) },
                selectedYear = selectedYear,
                onYearSelect = { viewModel.selectYear(it) },
                selectedMonth = selectedMonth,
                onMonthSelect = { viewModel.selectMonth(it) },
                onQuickAddLedgerClick = { viewModel.setShowQuickAddLedgerDialog(true) },
                currencySymbol = currencySymbol,
                onCurrencySelect = { viewModel.setCurrency(it) },
                useBengaliNumerals = useBengaliNumerals,
                onToggleBengali = { viewModel.toggleBengaliNumerals() },
                onPinSettingsClick = { viewModel.setShowPinSetupDialog(true) },
                isPinSet = !userPin.isNullOrEmpty(),
                onHistoryClick = { viewModel.setShowHistoryDialog(true) },
                onGoogleAuthClick = { viewModel.setShowGoogleAuthDialog(true) },
                currentUserEmail = currentUser?.email
            )

            // Prominent Google Sign-In & Firestore Backup Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (currentUser != null) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { viewModel.setShowGoogleAuthDialog(true) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (currentUser != null) Color(0xFF10B981) else Color(0xFF4285F4),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                val symbol = if (currentUser != null) "✓" else "G"
                                Text(
                                    text = symbol,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (currentUser != null) {
                                    if (useBengaliNumerals) "ক্লাউড ব্যাকআপ সক্রিয়: ${currentUser?.email}" else "Cloud Backup Active: ${currentUser?.email}"
                                } else {
                                    if (useBengaliNumerals) "গুগল সাইন-ইন ও ক্লাউড ব্যাকআপ (Firebase)" else "Sign in with Google & Cloud Backup"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentUser != null) Color(0xFF065F46) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentUser != null) {
                                    if (useBengaliNumerals) "সব নোট ক্লাউডে সেভ হচ্ছে ☁️" else "All notes synced to Firestore ☁️"
                                } else {
                                    if (useBengaliNumerals) "যেকোনো ডিভাইস থেকে ডাটা পেতে লগইন করুন" else "Tap to connect Gmail for multi-device sync"
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = if (currentUser != null) (if (useBengaliNumerals) "ম্যানেজ" else "Manage") else (if (useBengaliNumerals) "লগইন" else "Login"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Central Net Financial Overview Card & Budget Gauge
            NetFinancialCard(
                entries = monthEntries,
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                monthlyBudget = monthlyBudget,
                currencySymbol = currencySymbol,
                useBengaliNumerals = useBengaliNumerals,
                onOpenYearlyArchive = { viewModel.setShowYearlyArchiveDialog(true) },
                onOpenAnalytics = { viewModel.setShowAnalyticsDialog(true) },
                onOpenBudgetSetup = { viewModel.setShowBudgetSetupDialog(true) },
                onToggleLock = {
                    if (!userPin.isNullOrEmpty()) {
                        viewModel.lockApp()
                    } else {
                        viewModel.setShowPinSetupDialog(true)
                    }
                },
                isLocked = !userPin.isNullOrEmpty(),
                onIncomeBoxClick = { viewModel.openBreakdown(LedgerEntry.ENTRY_TYPE_INCOME) },
                onExpenseBoxClick = { viewModel.openBreakdown(LedgerEntry.ENTRY_TYPE_EXPENSE) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Dual Canvas Toggle Tabs: [ 📥 ইনকাম ] vs [ 📤 খরচ ]
            DualCanvasSelector(
                activeMode = canvasMode,
                onModeSelect = { viewModel.setCanvasMode(it) },
                incomeSum = monthIncomeTotal,
                expenseSum = monthExpenseTotal,
                currencySymbol = currencySymbol,
                useBengaliNumerals = useBengaliNumerals
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Backdate Indicator Pill if past date selected
            if (selectedBackdate != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(Date(selectedBackdate!!))
                            val dispDate = if (useBengaliNumerals) LedgerParser.convertEnglishDigitsToBengali(dateStr) else dateStr
                            Text(
                                text = if (useBengaliNumerals) "পুরাতন তারিখ নির্বাচিত: $dispDate" else "Backdate Selected: $dispDate",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }

                        TextButton(onClick = { viewModel.setBackdate(null) }) {
                            Text(if (useBengaliNumerals) "রিসেট" else "Reset", fontSize = 11.sp, color = Color(0xFFD97706))
                        }
                    }
                }
            }

            // 4. Smart Note Canvas Input Editor
            NotepadCanvas(
                noteText = currentNote,
                onNoteChange = { viewModel.onNoteChanged(it) },
                parseResult = currentParseResult,
                canvasMode = canvasMode,
                onVoiceInputClick = { viewModel.setShowVoiceDialog(true) },
                onClearNote = { viewModel.clearNote() },
                useBengaliNumerals = useBengaliNumerals
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Live Sum Floating Pill & Auto-Save Indicator
            LiveSumPill(
                totalSum = currentParseResult.totalSum,
                currencySymbol = currencySymbol,
                useBengaliNumerals = useBengaliNumerals,
                canvasMode = canvasMode,
                backdateTimestamp = selectedBackdate,
                onPickBackdate = { showDatePickerDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Date-Grouped Timeline & Entry History with Cross-Year Search Banner
            TimelineView(
                entries = filteredMonthEntries,
                globalSearchResults = globalSearchResults,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                selectedCategoryFilter = selectedCategoryFilter,
                onCategoryFilterSelect = { viewModel.setCategoryFilter(it) },
                onDeleteEntry = { viewModel.deleteEntry(it) },
                currencySymbol = currencySymbol,
                useBengaliNumerals = useBengaliNumerals
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Modal 1: Category Analytics Pie Chart
    if (showAnalyticsDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowAnalyticsDialog(false) },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowAnalyticsDialog(false) }) {
                    Text(if (useBengaliNumerals) "বন্ধ করুন" else "Close")
                }
            },
            title = {
                Text(
                    text = if (useBengaliNumerals) "খরচের অ্যানালিটিক্স (Analytics)" else "Spending Analytics",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                CategoryPieChart(
                    entries = monthEntries,
                    currencySymbol = currencySymbol,
                    useBengaliNumerals = useBengaliNumerals
                )
            }
        )
    }

    // Modal 2: Voice Input Helper Modal
    if (showVoiceDialog) {
        VoiceInputDialog(
            onDismiss = { viewModel.setShowVoiceDialog(false) },
            onSpeechResult = { text -> viewModel.appendTextToNote(text) },
            useBengali = useBengaliNumerals
        )
    }

    // Modal 3: Date Picker for Backdate input
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            viewModel.setBackdate(selectedMillis)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text(if (useBengaliNumerals) "ঠিক আছে" else "Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(if (useBengaliNumerals) "বাতিল" else "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Modal 4: PIN Security Settings
    if (showPinSetupDialog) {
        PinSetupDialog(
            currentPin = userPin,
            onSavePin = { newPin -> viewModel.setPin(newPin) },
            onDismiss = { viewModel.setShowPinSetupDialog(false) },
            useBengali = useBengaliNumerals
        )
    }

    // Modal 5: Yearly Overview Archive Modal
    if (showYearlyArchiveDialog) {
        YearlyOverviewDialog(
            selectedYear = selectedYear,
            selectedAccount = selectedAccount,
            yearEntries = yearEntries,
            currencySymbol = currencySymbol,
            useBengali = useBengaliNumerals,
            onDismiss = { viewModel.setShowYearlyArchiveDialog(false) }
        )
    }

    // Modal 6: Smart Budget Setup Modal
    if (showBudgetSetupDialog) {
        BudgetSetupDialog(
            currentBudget = monthlyBudget,
            currencySymbol = currencySymbol,
            useBengali = useBengaliNumerals,
            onSaveBudget = { viewModel.setMonthlyBudget(it) },
            onDismiss = { viewModel.setShowBudgetSetupDialog(false) }
        )
    }

    // Modal 7: Quick Add Ledger Sheet Modal
    if (showQuickAddLedgerDialog) {
        QuickAddLedgerDialog(
            initialYear = selectedYear,
            initialMonth = selectedMonth,
            useBengali = useBengaliNumerals,
            onSelectSheet = { yr, mo ->
                viewModel.selectYear(yr)
                viewModel.selectMonth(mo)
            },
            onDismiss = { viewModel.setShowQuickAddLedgerDialog(false) }
        )
    }

    // Modal 8: Google Sign-In & Firebase Cloud Sync
    if (showGoogleAuthDialog) {
        GoogleAuthDialog(
            currentUser = currentUser,
            syncManager = viewModel.syncManager,
            useBengali = useBengaliNumerals,
            onDismiss = { viewModel.setShowGoogleAuthDialog(false) }
        )
    }

    // Modal 9: Detailed Date & Time Wise History
    if (showHistoryDialog) {
        DetailedHistoryDialog(
            allEntries = yearEntries,
            currencySymbol = currencySymbol,
            useBengali = useBengaliNumerals,
            onDeleteEntry = { viewModel.deleteEntry(it) },
            onDismiss = { viewModel.setShowHistoryDialog(false) }
        )
    }

    // Modal 10: Month Items Breakdown, Date-Time inspection & Share All
    if (breakdownType != null) {
        MonthBreakdownDialog(
            entryType = breakdownType!!,
            entries = monthEntries,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            currencySymbol = currencySymbol,
            useBengali = useBengaliNumerals,
            onDismiss = { viewModel.closeBreakdown() }
        )
    }
}
