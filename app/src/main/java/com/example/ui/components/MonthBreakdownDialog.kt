package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.LedgerEntry
import com.example.util.LedgerParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MonthItemDetail(
    val rawLineText: String,
    val amount: Double,
    val timestamp: Long,
    val account: String,
    val entryType: String
)

@Composable
fun MonthBreakdownDialog(
    entryType: String, // "EXPENSE" or "INCOME"
    entries: List<LedgerEntry>,
    selectedYear: Int,
    selectedMonth: Int,
    currencySymbol: String,
    useBengali: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isIncome = entryType == LedgerEntry.ENTRY_TYPE_INCOME
    val accentColor = if (isIncome) Color(0xFF10B981) else Color(0xFFF97316)
    val headerBgColor = if (isIncome) Color(0xFF064E3B) else Color(0xFF7C2D12)

    val monthName = LedgerParser.getMonthName(selectedMonth, useBengali)
    val yrStr = if (useBengali) LedgerParser.convertEnglishDigitsToBengali(selectedYear.toString()) else selectedYear.toString()

    // Filter relevant entries for this type
    val relevantEntries = remember(entries, entryType) {
        entries.filter { it.entryType == entryType }.sortedBy { it.timestamp }
    }

    // Extract all item lines
    val extractedItems = remember(relevantEntries) {
        val list = mutableListOf<MonthItemDetail>()
        for (entry in relevantEntries) {
            val lines = entry.rawNote.split("\n")
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotBlank()) {
                    // Check if line has multiple '+' items
                    val plusTokens = trimmed.split("+")
                    if (plusTokens.size > 1) {
                        for (token in plusTokens) {
                            val tTrim = token.trim()
                            if (tTrim.isNotBlank()) {
                                val normalized = LedgerParser.convertBengaliDigitsToEnglish(tTrim)
                                val amt = LedgerParser.calculateLineMath(normalized)
                                list.add(
                                    MonthItemDetail(
                                        rawLineText = tTrim,
                                        amount = amt,
                                        timestamp = entry.timestamp,
                                        account = entry.ledgerAccount,
                                        entryType = entry.entryType
                                    )
                                )
                            }
                        }
                    } else {
                        val normalized = LedgerParser.convertBengaliDigitsToEnglish(trimmed)
                        val amt = LedgerParser.calculateLineMath(normalized)
                        list.add(
                            MonthItemDetail(
                                rawLineText = trimmed,
                                amount = amt,
                                timestamp = entry.timestamp,
                                account = entry.ledgerAccount,
                                entryType = entry.entryType
                            )
                        )
                    }
                }
            }
        }
        list
    }

    val totalSum = remember(extractedItems) {
        extractedItems.sumOf { it.amount }
    }

    // Format all lines combined for one-click sharing
    val shareableText = remember(extractedItems, totalSum, monthName, yrStr, isIncome) {
        val title = if (isIncome) {
            "📥 অনন্ত নোটস (Anonto Notes) • $monthName $yrStr - আয়ের তালিকা"
        } else {
            "📤 অনন্ত নোটস (Anonto Notes) • $monthName $yrStr - খরচের তালিকা"
        }

        val itemsBody = extractedItems.mapIndexed { idx, item ->
            val numStr = if (useBengali) LedgerParser.convertEnglishDigitsToBengali((idx + 1).toString()) else (idx + 1).toString()
            "$numStr. ${item.rawLineText}"
        }.joinToString("\n")

        val totalStr = LedgerParser.formatCurrency(totalSum, currencySymbol, useBengali)
        val sumLine = if (isIncome) "মোট আয়: $totalStr" else "মোট খরচ: $totalStr"

        "$title\n${"-".repeat(36)}\n$itemsBody\n${"-".repeat(36)}\n$sumLine"
    }

    // Selected item index for expanded Date & Time inspection
    var selectedItemIndex by remember { mutableStateOf<Int?>(null) }

    val dateFormatter = remember { SimpleDateFormat("dd MMMM, yyyy (EEEE)", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .testTag("month_breakdown_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isIncome) {
                                    if (useBengali) "এই মাসের সমস্ত ইনকাম" else "All Incomes this Month"
                                } else {
                                    if (useBengali) "এই মাসের সমস্ত খরচ" else "All Expenses this Month"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$monthName $yrStr • ${extractedItems.size} ${if (useBengali) "টি হিসাব" else "items"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Total Sum Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = headerBgColor.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isIncome) {
                                if (useBengali) "সর্বমোট আয়:" else "Total Income:"
                            } else {
                                if (useBengali) "সর্বমোট খরচ:" else "Total Expense:"
                            },
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = LedgerParser.formatCurrency(totalSum, currencySymbol, useBengali),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Action Bar: Share All & Copy All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareableText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "হিসাবের তালিকা শেয়ার করুন"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_all_items_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (useBengali) "সব একসাথে শেয়ার" else "Share All",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Anonto Notes", shareableText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(
                                context,
                                if (useBengali) "সব হিসাব ক্লিপবোর্ডে কপি হয়েছে!" else "Copied all entries to clipboard!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_all_items_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (useBengali) "সব লেখা কপি" else "Copy Text",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Instruction / Hint Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (useBengali) "💡 যেকোনো লাইনে ট্যাপ করে লেখার সঠিক তারিখ ও সময় দেখুন" else "💡 Tap any item to reveal exact date & time",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Items List
                if (extractedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📝", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (useBengali) "এই মাসে এখনও কোনো ${if (isIncome) "আয়" else "খরচ"} লেখা হয়নি" else "No ${if (isIncome) "income" else "expense"} records this month",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(extractedItems) { index, item ->
                            val isSelected = selectedItemIndex == index

                            val dateFormatted = dateFormatter.format(Date(item.timestamp))
                            val timeFormatted = timeFormatter.format(Date(item.timestamp))

                            val dispDate = if (useBengali) LedgerParser.convertEnglishDigitsToBengali(dateFormatted) else dateFormatted
                            val dispTime = if (useBengali) LedgerParser.convertEnglishDigitsToBengali(timeFormatted) else timeFormatted
                            val dispAmount = LedgerParser.formatCurrency(item.amount, currencySymbol, useBengali)

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        accentColor.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        selectedItemIndex = if (isSelected) null else index
                                    }
                                    .testTag("breakdown_item_$index")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Row 1: Line Text and Amount
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.rawLineText,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (item.amount > 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = accentColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = dispAmount,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = accentColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Expandable Timestamp & Details Box on Tap
                                    AnimatedVisibility(
                                        visible = isSelected,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.CalendarMonth,
                                                        contentDescription = "Date",
                                                        tint = accentColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (useBengali) "তারিখ: $dispDate" else "Date: $dispDate",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.AccessTime,
                                                        contentDescription = "Time",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (useBengali) "সময়: $dispTime" else "Time: $dispTime",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Text(
                                                    text = "অ্যাকাউন্ট: ${item.account}",
                                                    fontSize = 10.sp,
                                                    color = accentColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
