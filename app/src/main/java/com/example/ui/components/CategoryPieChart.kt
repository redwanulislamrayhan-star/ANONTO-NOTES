package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LedgerEntry
import com.example.util.LedgerParser
import com.example.util.TagCategory

data class CategoryShare(
    val category: TagCategory,
    val amount: Double,
    val percentage: Float
)

@Composable
fun CategoryPieChart(
    entries: List<LedgerEntry>,
    currencySymbol: String,
    useBengaliNumerals: Boolean,
    modifier: Modifier = Modifier
) {
    val totalExpense = entries.sumOf { it.totalAmount }

    // Map categories to total amounts
    val categoryMap = mutableMapOf<TagCategory, Double>()

    entries.forEach { entry ->
        val tags = entry.tagsJson.split(",")
        val amount = entry.totalAmount

        if (tags.isNotEmpty() && amount > 0) {
            val amountPerTag = amount / tags.size
            tags.forEach { tagStr ->
                val category = LedgerParser.parseTagFromString(tagStr)
                categoryMap[category] = (categoryMap[category] ?: 0.0) + amountPerTag
            }
        }
    }

    val shares = categoryMap.map { (cat, amt) ->
        val pct = if (totalExpense > 0) (amt / totalExpense * 100).toFloat() else 0f
        CategoryShare(cat, amt, pct)
    }.sortedByDescending { it.amount }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = if (useBengaliNumerals) "ক্যাটেগরি অনুযায়ী খরচের পাই-চার্ট" else "Expenses by Category Breakdown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (shares.isEmpty() || totalExpense <= 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (useBengaliNumerals) "কোনো খরচের ডেটা পাওয়া যায়নি" else "No expense data available for this period",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom Donut / Pie Chart Canvas
                    Box(
                        modifier = Modifier.size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(120.dp)) {
                            var startAngle = -90f
                            shares.forEach { share ->
                                val sweepAngle = (share.percentage / 100f) * 360f
                                drawArc(
                                    color = share.category.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt),
                                    size = Size(size.width, size.height)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        // Center total display
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (useBengaliNumerals) "মোট" else "Total",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val shortTotal = if (totalExpense >= 1000) "${(totalExpense / 1000).toInt()}k" else totalExpense.toInt().toString()
                            val dispTotal = if (useBengaliNumerals) LedgerParser.convertEnglishDigitsToBengali(shortTotal) else shortTotal
                            Text(
                                text = "$currencySymbol $dispTotal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right Side: Category Progress List
                    Column(modifier = Modifier.weight(1f)) {
                        shares.take(4).forEach { share ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = share.category.iconEmoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (useBengaliNumerals) share.category.nameBn else share.category.nameEn,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    val pctString = if (useBengaliNumerals) {
                                        "${LedgerParser.convertEnglishDigitsToBengali(share.percentage.toInt().toString())}%"
                                    } else "${share.percentage.toInt()}%"

                                    Text(
                                        text = pctString,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = share.category.color
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                LinearProgressIndicator(
                                    progress = { share.percentage / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = share.category.color,
                                    trackColor = share.category.color.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
