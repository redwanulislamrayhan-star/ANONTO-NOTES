package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CanvasMode
import com.example.util.LedgerParser

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LiveSumPill(
    totalSum: Double,
    currencySymbol: String,
    useBengaliNumerals: Boolean,
    canvasMode: CanvasMode,
    backdateTimestamp: Long?,
    onPickBackdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = canvasMode == CanvasMode.INCOME
    val containerBg = if (isIncome) Color(0xFF064E3B) else Color(0xFF7C2D12)
    val buttonBg = if (isIncome) Color(0xFF10B981) else Color(0xFFEA580C)
    val labelColor = if (isIncome) Color(0xFFA7F3D0) else Color(0xFFFED7AA)

    val formattedSum = LedgerParser.formatCurrency(
        amount = totalSum,
        currencySymbol = currencySymbol,
        useBengaliNumerals = useBengaliNumerals
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_sum_pill_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Live Sum Display & Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = buttonBg,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = "Sum",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                AnimatedContent(
                    targetState = formattedSum,
                    transitionSpec = { fadeIn() with fadeOut() },
                    label = "live_sum_anim"
                ) { text ->
                    Column {
                        Text(
                            text = if (isIncome) {
                                if (useBengaliNumerals) "মোট ইনকাম (Live Income)" else "Live Total Income"
                            } else {
                                if (useBengaliNumerals) "মোট খরচ (Live Expense)" else "Live Total Expense"
                            },
                            color = labelColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right Side: Backdate Button & Auto-Saved Status Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Backdate Picker Button
                Surface(
                    onClick = onPickBackdate,
                    shape = CircleShape,
                    color = if (backdateTimestamp != null) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick Date",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Live Auto-Saved Badge Indicator
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = buttonBg.copy(alpha = 0.3f),
                    modifier = Modifier.testTag("auto_saved_indicator")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Auto-Saved",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (useBengaliNumerals) "অটো-সেভড ⚡" else "Auto-Saved ⚡",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
