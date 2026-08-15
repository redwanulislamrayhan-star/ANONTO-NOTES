package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LedgerEntry
import com.example.util.LedgerParser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HeroCard(
    entries: List<LedgerEntry>,
    selectedYearMonth: String,
    onYearMonthSelected: (String) -> Unit,
    currencySymbol: String,
    useBengaliNumerals: Boolean,
    onOpenAnalytics: () -> Unit,
    onToggleLock: () -> Unit,
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    var monthMenuExpanded by remember { mutableStateOf(false) }

    // Calculate monthly statistics
    val monthlyTotal = entries.sumOf { it.totalAmount }
    val entryCount = entries.size

    // Calculate daily average
    val daysInMonth = getDaysInSelectedMonth(selectedYearMonth)
    val dailyAverage = if (daysInMonth > 0) monthlyTotal / daysInMonth else 0.0

    val monthDisplayName = getFormattedMonthDisplay(selectedYearMonth, useBengaliNumerals)

    // Royal Blue Gradient Card
    val cardGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E3A8A), // Deep Indigo / Royal Blue
            Color(0xFF2563EB), // Soft Royal Blue
            Color(0xFF1D4ED8)
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(cardGradient)
                .padding(20.dp)
        ) {
            Column {
                // Top Row: Month Dropdown Selector & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Month Dropdown Button
                    Box {
                        Surface(
                            onClick = { monthMenuExpanded = true },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Select Month",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = monthDisplayName,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Month Selection Dropdown
                        DropdownMenu(
                            expanded = monthMenuExpanded,
                            onDismissRequest = { monthMenuExpanded = false }
                        ) {
                            val availableMonths = getPastMonthsList()
                            availableMonths.forEach { monthKey ->
                                val label = getFormattedMonthDisplay(monthKey, useBengaliNumerals)
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onYearMonthSelected(monthKey)
                                        monthMenuExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (useBengaliNumerals) "সব মাস (All)" else "All Months") },
                                onClick = {
                                    onYearMonthSelected("ALL")
                                    monthMenuExpanded = false
                                }
                            )
                        }
                    }

                    // Action Icons: Analytics & Security Lock
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onOpenAnalytics,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "Category Analytics",
                                    tint = Color(0xFFA7F3D0) // Mint Accent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            onClick = onToggleLock,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "PIN Lock",
                                    tint = if (isLocked) Color(0xFFFBBF24) else Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Monthly Total Amount Header
                Text(
                    text = if (useBengaliNumerals) "এই মাসের মোট খরচ" else "Total Expenses This Month",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                val formattedTotal = LedgerParser.formatCurrency(
                    amount = monthlyTotal,
                    currencySymbol = currencySymbol,
                    useBengaliNumerals = useBengaliNumerals
                )

                Text(
                    text = formattedTotal,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Stats Row: Daily Average & Total Entries
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Daily Average Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color(0xFFA7F3D0),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val formattedAvg = LedgerParser.formatCurrency(
                                amount = dailyAverage,
                                currencySymbol = currencySymbol,
                                useBengaliNumerals = useBengaliNumerals
                            )
                            Text(
                                text = if (useBengaliNumerals) "দৈনিক গড়: $formattedAvg" else "Daily Avg: $formattedAvg",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Entry Count Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val countStr = if (useBengaliNumerals) {
                                LedgerParser.convertEnglishDigitsToBengali(entryCount.toString())
                            } else entryCount.toString()

                            Text(
                                text = if (useBengaliNumerals) "মোট হিসাব: $countStr টি" else "Entries: $countStr",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getPastMonthsList(): List<String> {
    val list = mutableListOf<String>()
    val cal = Calendar.getInstance()
    val format = SimpleDateFormat("yyyy-MM", Locale.US)
    for (i in 0..11) {
        list.add(format.format(cal.time))
        cal.add(Calendar.MONTH, -1)
    }
    return list
}

private fun getFormattedMonthDisplay(yearMonthKey: String, useBengali: Boolean): String {
    if (yearMonthKey == "ALL") return if (useBengali) "সব সময়" else "All Time"
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = sdf.parse(yearMonthKey) ?: Date()

        val monthNameBn = when (SimpleDateFormat("M", Locale.US).format(date)) {
            "1" -> "জানুয়ারি"
            "2" -> "ফেব্রুয়ারি"
            "3" -> "মার্চ"
            "4" -> "এপ্রিল"
            "5" -> "মে"
            "6" -> "জুন"
            "7" -> "জুলাই"
            "8" -> "আগস্ট"
            "9" -> "সেপ্টেম্বর"
            "10" -> "অক্টোবর"
            "11" -> "নভেম্বর"
            "12" -> "ডিসেম্বর"
            else -> ""
        }
        val yearBn = LedgerParser.convertEnglishDigitsToBengali(SimpleDateFormat("yyyy", Locale.US).format(date))

        if (useBengali) "$monthNameBn $yearBn" else SimpleDateFormat("MMMM yyyy", Locale.US).format(date)
    } catch (e: Exception) {
        yearMonthKey
    }
}

private fun getDaysInSelectedMonth(yearMonthKey: String): Int {
    if (yearMonthKey == "ALL") return 30
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = sdf.parse(yearMonthKey) ?: return 30
        val cal = Calendar.getInstance()
        cal.time = date
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    } catch (e: Exception) {
        30
    }
}
