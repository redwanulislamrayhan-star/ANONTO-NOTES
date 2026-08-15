package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LedgerEntry
import com.example.util.LedgerParser
import com.example.util.TagCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineView(
    entries: List<LedgerEntry>,
    globalSearchResults: List<LedgerEntry>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategoryFilter: TagCategory?,
    onCategoryFilterSelect: (TagCategory?) -> Unit,
    onDeleteEntry: (LedgerEntry) -> Unit,
    currencySymbol: String,
    useBengaliNumerals: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Group entries by date
    val displayEntries = if (searchQuery.isNotBlank()) globalSearchResults else entries
    val groupedEntries = displayEntries.groupBy { entry ->
        formatDateGroupHeader(entry.timestamp, useBengaliNumerals)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Search & Filter Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("timeline_search_input"),
                placeholder = {
                    Text(
                        text = if (useBengaliNumerals) "গ্লোবাল ফিল্টার (যেমন: ডাক্তার, ওষুধ, ২০০)..." else "Global search across years...",
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Global Search Summary Result Banner (Cross-Year Search Result)
        if (searchQuery.isNotBlank()) {
            val totalQueryExpense = globalSearchResults
                .filter { it.entryType == LedgerEntry.ENTRY_TYPE_EXPENSE }
                .sumOf { it.totalAmount }
            val formattedQuerySum = LedgerParser.formatCurrency(totalQueryExpense, currencySymbol, useBengaliNumerals)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEFF6FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (useBengaliNumerals) {
                            "আপনি ২০২৩ থেকে ২০২৬ পর্যন্ত '$searchQuery' বাবদ মোট $formattedQuerySum খরচ করেছেন।"
                        } else {
                            "Total spent on '$searchQuery' across years: $formattedQuerySum"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                }
            }
        }

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { onCategoryFilterSelect(null) },
                    label = { Text(if (useBengaliNumerals) "সব (All)" else "All") }
                )
            }
            items(LedgerParser.ALL_CATEGORIES) { cat ->
                val isSel = selectedCategoryFilter == cat
                FilterChip(
                    selected = isSel,
                    onClick = { onCategoryFilterSelect(cat) },
                    leadingIcon = { Text(cat.iconEmoji, fontSize = 12.sp) },
                    label = { Text(if (useBengaliNumerals) cat.nameBn else cat.nameEn) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cat.color.copy(alpha = 0.2f),
                        selectedLabelColor = cat.color
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Empty state or list
        if (groupedEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (useBengaliNumerals) "কোনো হিসাবের হিস্ট্রি নেই" else "No ledger history found",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            groupedEntries.forEach { (dateHeader, dateEntries) ->
                // Date Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateHeader,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                dateEntries.forEach { entry ->
                    LedgerEntryCard(
                        entry = entry,
                        onDelete = { onDeleteEntry(entry) },
                        onShare = { shareLedgerEntry(context, entry, useBengaliNumerals) },
                        currencySymbol = currencySymbol,
                        useBengaliNumerals = useBengaliNumerals
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LedgerEntryCard(
    entry: LedgerEntry,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    currencySymbol: String,
    useBengaliNumerals: Boolean,
    modifier: Modifier = Modifier
) {
    val isIncome = entry.entryType == LedgerEntry.ENTRY_TYPE_INCOME
    val formattedTime = SimpleDateFormat("hh:mm a", Locale.US).format(Date(entry.timestamp))
    val dispTime = if (useBengaliNumerals) LedgerParser.convertEnglishDigitsToBengali(formattedTime) else formattedTime

    val totalFormatted = LedgerParser.formatCurrency(
        amount = entry.totalAmount,
        currencySymbol = currencySymbol,
        useBengaliNumerals = useBengaliNumerals
    )

    val tagNames = entry.tagsJson.split(",").filter { it.isNotBlank() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ledger_entry_card_${entry.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Card Header: Entry Type Badge, Amount, Time & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Type Badge: [ 📥 ইনকাম ] vs [ 📤 খরচ ]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isIncome) Color(0xFFD1FAE5) else Color(0xFFFFEDD5)
                    ) {
                        Text(
                            text = if (isIncome) {
                                if (useBengaliNumerals) "📥 ইনকাম" else "📥 Income"
                            } else {
                                if (useBengaliNumerals) "📤 খরচ" else "📤 Expense"
                            },
                            color = if (isIncome) Color(0xFF047857) else Color(0xFFC2410C),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = totalFormatted,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isIncome) Color(0xFF059669) else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = dispTime,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Raw Note Content Text
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = entry.rawNote,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Tag Pills Row
            if (tagNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tagNames.forEach { tStr ->
                        val category = LedgerParser.parseTagFromString(tStr)
                        TagBadgePill(tag = category, useBengali = useBengaliNumerals)
                    }
                }
            }
        }
    }
}

private fun formatDateGroupHeader(timestamp: Long, useBengali: Boolean): String {
    val now = System.currentTimeMillis()
    val diffDays = ((now - timestamp) / (1000 * 60 * 60 * 24)).toInt()

    return if (diffDays == 0) {
        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(Date(timestamp))
        val disp = if (useBengali) LedgerParser.convertEnglishDigitsToBengali(dateStr) else dateStr
        if (useBengali) "আজ ($disp)" else "Today ($disp)"
    } else if (diffDays == 1) {
        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(Date(timestamp))
        val disp = if (useBengali) LedgerParser.convertEnglishDigitsToBengali(dateStr) else dateStr
        if (useBengali) "গতকাল ($disp)" else "Yesterday ($disp)"
    } else {
        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.US).format(Date(timestamp))
        if (useBengali) LedgerParser.convertEnglishDigitsToBengali(dateStr) else dateStr
    }
}

private fun shareLedgerEntry(context: Context, entry: LedgerEntry, useBengali: Boolean) {
    val dateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.US).format(Date(entry.timestamp))
    val total = LedgerParser.formatCurrency(entry.totalAmount, entry.currencySymbol, useBengali)

    val shareText = """
        🧾 QuickList Ledger Receipt
        📅 Date: $dateStr
        📁 Profile: ${entry.ledgerAccount}
        
        📝 Items Note:
        ${entry.rawNote}
        
        💰 Total Amount: $total
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Ledger Entry"))
}
