package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LedgerEntry
import com.example.util.LedgerParser

@Composable
fun NetFinancialCard(
    entries: List<LedgerEntry>,
    selectedYear: Int,
    selectedMonth: Int,
    monthlyBudget: Double,
    currencySymbol: String,
    useBengaliNumerals: Boolean,
    onOpenYearlyArchive: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenBudgetSetup: () -> Unit,
    onToggleLock: () -> Unit,
    isLocked: Boolean,
    onIncomeBoxClick: () -> Unit = {},
    onExpenseBoxClick: () -> Unit = {}
) {
    // Calculate total income and expense for current month entries
    val totalIncome = entries.filter { it.entryType == LedgerEntry.ENTRY_TYPE_INCOME }.sumOf { it.totalAmount }
    val totalExpense = entries.filter { it.entryType == LedgerEntry.ENTRY_TYPE_EXPENSE }.sumOf { it.totalAmount }
    val netSavings = totalIncome - totalExpense

    // Budget calculation
    val budgetProgress = if (monthlyBudget > 0) (totalExpense / monthlyBudget).coerceIn(0.0, 1.0).toFloat() else 0f
    val budgetRatioPercent = if (monthlyBudget > 0) ((totalExpense / monthlyBudget) * 100).toInt() else 0

    val gaugeColor = when {
        budgetRatioPercent > 80 -> Color(0xFFEF4444) // Bright Red warning
        budgetRatioPercent > 60 -> Color(0xFFF59E0B) // Amber/Yellow
        else -> Color(0xFF10B981) // Mint Green
    }

    val monthName = LedgerParser.getMonthName(selectedMonth, useBengaliNumerals)
    val yrStr = if (useBengaliNumerals) LedgerParser.convertEnglishDigitsToBengali(selectedYear.toString()) else selectedYear.toString()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E1B4B), // Indigo Dark
                            Color(0xFF312E81), // Deep Blue Indigo
                            Color(0xFF1E293B)  // Slate Blue
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Header Row: Month Title & Archive / Analytics / Lock Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (useBengaliNumerals) "আর্থিক সামারি ($monthName $yrStr)" else "Financial Overview ($monthName $yrStr)",
                            color = Color(0xFF93C5FD),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (useBengaliNumerals) "অবশিষ্ট জমা: " else "Net Savings: ",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = LedgerParser.formatCurrency(netSavings, currencySymbol, useBengaliNumerals),
                                color = if (netSavings >= 0) Color(0xFF34D399) else Color(0xFFF87171),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Yearly Archive Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { onOpenYearlyArchive() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Yearly Archive",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (useBengaliNumerals) "বার্ষিক রিভিউ" else "Annual",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Category Pie Chart Analytics Button
                        IconButton(
                            onClick = onOpenAnalytics,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Analytics",
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // App Quick Lock Button
                        IconButton(
                            onClick = onToggleLock,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock App",
                                tint = if (isLocked) Color(0xFFF87171) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Income vs Expense Dual Cards (Clickable for full month breakdown & sharing)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income Summary Box (Green Theme)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onIncomeBoxClick() }
                            .testTag("income_summary_card_button"),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF064E3B).copy(alpha = 0.6f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = Color(0xFF34D399),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (useBengaliNumerals) "📥 মোট ইনকাম" else "📥 Income",
                                        color = Color(0xFFA7F3D0),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = "🔍",
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = LedgerParser.formatCurrency(totalIncome, currencySymbol, useBengaliNumerals),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (useBengaliNumerals) "লিস্ট ও শেয়ার 👆" else "Tap for list & share",
                                color = Color(0xFFA7F3D0).copy(alpha = 0.7f),
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Expense Summary Box (Coral/Red Theme)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFFF97316).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onExpenseBoxClick() }
                            .testTag("expense_summary_card_button"),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF7C2D12).copy(alpha = 0.6f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF97316).copy(alpha = 0.2f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = Color(0xFFFB923C),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (useBengaliNumerals) "📤 মোট খরচ" else "📤 Expense",
                                        color = Color(0xFFFED7AA),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = "🔍",
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = LedgerParser.formatCurrency(totalExpense, currencySymbol, useBengaliNumerals),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (useBengaliNumerals) "লিস্ট ও শেয়ার 👆" else "Tap for list & share",
                                color = Color(0xFFFED7AA).copy(alpha = 0.7f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Smart Budget Progress Gauge Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (useBengaliNumerals) "বাজেট গেজ (Budget Limits)" else "Budget Gauge",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (budgetRatioPercent > 80) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val budgetStr = LedgerParser.formatCurrency(monthlyBudget, currencySymbol, useBengaliNumerals)
                            val pctStr = if (useBengaliNumerals) LedgerParser.convertEnglishDigitsToBengali("$budgetRatioPercent%") else "$budgetRatioPercent%"

                            Text(
                                text = "$pctStr (সীমা: $budgetStr)",
                                color = gaugeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = onOpenBudgetSetup,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Set Budget",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = gaugeColor,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    if (budgetRatioPercent > 80) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (useBengaliNumerals) "⚠️ সতর্কতা: আপনার মাসিক বাজেটের ৮০% এর বেশি খরচ হয়ে গেছে!" else "⚠️ Warning: You have exceeded 80% of your monthly budget limit!",
                            color = Color(0xFFFCA5A5),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
