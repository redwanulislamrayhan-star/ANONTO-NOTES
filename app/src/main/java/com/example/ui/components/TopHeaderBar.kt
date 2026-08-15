package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.LedgerEntry
import com.example.util.LedgerParser

@Composable
fun TopHeaderBar(
    selectedAccount: String,
    onAccountSelect: (String) -> Unit,
    selectedYear: Int,
    onYearSelect: (Int) -> Unit,
    selectedMonth: Int,
    onMonthSelect: (Int) -> Unit,
    onQuickAddLedgerClick: () -> Unit,
    currencySymbol: String,
    onCurrencySelect: (String) -> Unit,
    useBengaliNumerals: Boolean,
    onToggleBengali: () -> Unit,
    onPinSettingsClick: () -> Unit,
    isPinSet: Boolean,
    onHistoryClick: () -> Unit = {},
    onGoogleAuthClick: () -> Unit = {},
    currentUserEmail: String? = null
) {
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var yearMenuExpanded by remember { mutableStateOf(false) }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    val currentCalYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val dynamicYearsList = remember(selectedYear) {
        val baseList = ((currentCalYear - 5)..(currentCalYear + 10)).toList()
        if (selectedYear !in baseList) (baseList + selectedYear).sorted() else baseList
    }
    var showCustomYearDialog by remember { mutableStateOf(false) }
    var customYearInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 8.dp)
    ) {
        // Row 1: Brand Title, Language, Currency, PIN Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.anonto_notes_logo_1786812074216),
                    contentDescription = "Anonto Notes Logo",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (useBengaliNumerals) "অনন্ত নোটস (Anonto Notes)" else "Anonto Notes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (useBengaliNumerals) "অটো সেভ স্মার্ট নোটপ্যাড" else "Auto-Save Smart Ledger",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Currency Selector Menu
                Box {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { currencyMenuExpanded = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = currencySymbol,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = currencyMenuExpanded,
                        onDismissRequest = { currencyMenuExpanded = false }
                    ) {
                        listOf("৳", "$", "₹", "€").forEach { symbol ->
                            DropdownMenuItem(
                                text = { Text("Currency: $symbol") },
                                onClick = {
                                    onCurrencySelect(symbol)
                                    currencyMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Language / Numeral Toggle Button
                IconButton(
                    onClick = onToggleBengali,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("numeral_language_toggle")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (useBengaliNumerals) "বাংলা" else "ENG",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // PIN Security Button
                IconButton(
                    onClick = onPinSettingsClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("pin_setting_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "PIN Settings",
                        tint = if (isPinSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // History / Timeline Modal Trigger Button
                IconButton(
                    onClick = onHistoryClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("history_timeline_button")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🕒", fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Google Sign-In & Firebase Cloud Sync Avatar
                IconButton(
                    onClick = onGoogleAuthClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("google_cloud_auth_button")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (currentUserEmail != null) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (currentUserEmail != null) {
                                Text(
                                    text = currentUserEmail.take(1).uppercase(),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            } else {
                                Text(
                                    text = "☁️",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.padding(top = 10.dp))

        // Row 2: Navigation Wireframe Bar -> Account Profile Dropdown | Year Selector | Month Selector | + New Sheet
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Account Profile Dropdown: [ 📁 ফ্যামিলি খাতা ▾ ]
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { accountMenuExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedAccount,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = accountMenuExpanded,
                    onDismissRequest = { accountMenuExpanded = false }
                ) {
                    LedgerEntry.ALL_ACCOUNTS.forEach { account ->
                        DropdownMenuItem(
                            text = { Text("📁 $account", fontWeight = if (account == selectedAccount) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                onAccountSelect(account)
                                accountMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Year & Month Selectors: [ 2026 ▾ ] [ আগস্ট ▾ ]
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Year Selector
                Box {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { yearMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val yearDisp = if (useBengaliNumerals) LedgerParser.convertEnglishDigitsToBengali(selectedYear.toString()) else selectedYear.toString()
                            Text(
                                text = yearDisp,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = yearMenuExpanded,
                        onDismissRequest = { yearMenuExpanded = false }
                    ) {
                        dynamicYearsList.forEach { yr ->
                            val yrDisp = if (useBengaliNumerals) LedgerParser.convertEnglishDigitsToBengali(yr.toString()) else yr.toString()
                            DropdownMenuItem(
                                text = { Text(yrDisp, fontWeight = if (yr == selectedYear) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onYearSelect(yr)
                                    yearMenuExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (useBengaliNumerals) "+ কাস্টম বছর টাইপ করুন" else "+ Type Custom Year",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                yearMenuExpanded = false
                                customYearInput = selectedYear.toString()
                                showCustomYearDialog = true
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Month Selector
                Box {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { monthMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val monthName = LedgerParser.getMonthName(selectedMonth, useBengaliNumerals)
                            Text(
                                text = monthName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = monthMenuExpanded,
                        onDismissRequest = { monthMenuExpanded = false }
                    ) {
                        (1..12).forEach { mo ->
                            val moName = LedgerParser.getMonthName(mo, useBengaliNumerals)
                            DropdownMenuItem(
                                text = { Text(moName, fontWeight = if (mo == selectedMonth) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onMonthSelect(mo)
                                    monthMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Quick Add Ledger Sheet Button: [ + ]
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { onQuickAddLedgerClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Sheet",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showCustomYearDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustomYearDialog = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val parsed = LedgerParser.convertBengaliDigitsToEnglish(customYearInput).toIntOrNull()
                        if (parsed != null && parsed in 1900..2100) {
                            onYearSelect(parsed)
                        }
                        showCustomYearDialog = false
                    }
                ) {
                    Text(if (useBengaliNumerals) "ঠিক আছে" else "OK")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCustomYearDialog = false }) {
                    Text(if (useBengaliNumerals) "বাতিল" else "Cancel")
                }
            },
            title = {
                Text(
                    text = if (useBengaliNumerals) "যেকোনো বছর টাইপ করুন" else "Enter Any Custom Year",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (useBengaliNumerals) "যেমন: ২০২৫, ২০৩০, ২০৫০..." else "E.g. 2025, 2030, 2050...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = customYearInput,
                        onValueChange = { customYearInput = it },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        )
    }
}
