package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LedgerEntry
import com.example.util.LedgerParser

@Composable
fun YearlyOverviewDialog(
    selectedYear: Int,
    selectedAccount: String,
    yearEntries: List<LedgerEntry>,
    currencySymbol: String,
    useBengali: Boolean,
    onDismiss: () -> Unit
) {
    val totalYearIncome = yearEntries.filter { it.entryType == LedgerEntry.ENTRY_TYPE_INCOME }.sumOf { it.totalAmount }
    val totalYearExpense = yearEntries.filter { it.entryType == LedgerEntry.ENTRY_TYPE_EXPENSE }.sumOf { it.totalAmount }
    val totalYearSavings = totalYearIncome - totalYearExpense

    val yearDisp = if (useBengali) LedgerParser.convertEnglishDigitsToBengali(selectedYear.toString()) else selectedYear.toString()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (useBengali) "বন্ধ করুন" else "Close")
            }
        },
        title = {
            Column {
                Text(
                    text = if (useBengali) "বার্ষিক রিভিউ ($yearDisp)" else "Yearly Overview Archive ($selectedYear)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "📁 $selectedAccount",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Annual Financial Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (useBengali) "মোট বার্ষিক ইনকাম:" else "Yearly Income:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = LedgerParser.formatCurrency(totalYearIncome, currencySymbol, useBengali),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (useBengali) "মোট বার্ষিক খরচ:" else "Yearly Expense:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = LedgerParser.formatCurrency(totalYearExpense, currencySymbol, useBengali),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (useBengali) "মোট সঞ্চয় (Savings):" else "Net Savings:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = LedgerParser.formatCurrency(totalYearSavings, currencySymbol, useBengali),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (totalYearSavings >= 0) Color(0xFF047857) else Color(0xFFDC2626)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (useBengali) "মাসিক বিবরণী (12-Month Archive):" else "Monthly Breakdown:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 12 Months Table Breakdown
                (1..12).forEach { monthIdx ->
                    val ym = String.format(java.util.Locale.US, "%04d-%02d", selectedYear, monthIdx)
                    val mEntries = yearEntries.filter { it.yearMonth == ym }

                    val mIncome = mEntries.filter { it.entryType == LedgerEntry.ENTRY_TYPE_INCOME }.sumOf { it.totalAmount }
                    val mExpense = mEntries.filter { it.entryType == LedgerEntry.ENTRY_TYPE_EXPENSE }.sumOf { it.totalAmount }
                    val monthName = LedgerParser.getMonthName(monthIdx, useBengali)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "আয়: ${LedgerParser.formatCurrency(mIncome, currencySymbol, useBengali)}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF047857)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ব্যয়: ${LedgerParser.formatCurrency(mExpense, currencySymbol, useBengali)}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFC2410C)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
