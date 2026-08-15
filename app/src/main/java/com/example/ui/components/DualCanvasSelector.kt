package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CanvasMode
import com.example.util.LedgerParser

@Composable
fun DualCanvasSelector(
    activeMode: CanvasMode,
    onModeSelect: (CanvasMode) -> Unit,
    incomeSum: Double,
    expenseSum: Double,
    currencySymbol: String,
    useBengaliNumerals: Boolean
) {
    val incomeFormatted = LedgerParser.formatCurrency(incomeSum, currencySymbol, useBengaliNumerals)
    val expenseFormatted = LedgerParser.formatCurrency(expenseSum, currencySymbol, useBengaliNumerals)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Income Canvas Tab (Green Theme)
            val isIncomeActive = activeMode == CanvasMode.INCOME
            val incomeBg by animateColorAsState(
                targetValue = if (isIncomeActive) Color(0xFF059669) else Color.Transparent,
                label = "incomeBg"
            )
            val incomeText = if (isIncomeActive) Color.White else Color(0xFF059669)

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onModeSelect(CanvasMode.INCOME) }
                    .then(
                        if (!isIncomeActive) Modifier.border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        else Modifier
                    ),
                shape = RoundedCornerShape(14.dp),
                color = incomeBg
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (useBengaliNumerals) "📥 ইনকাম" else "📥 Income",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = incomeText
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "($incomeFormatted)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = incomeText.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Expense Canvas Tab (Coral / Orange Theme)
            val isExpenseActive = activeMode == CanvasMode.EXPENSE
            val expenseBg by animateColorAsState(
                targetValue = if (isExpenseActive) Color(0xFFEA580C) else Color.Transparent,
                label = "expenseBg"
            )
            val expenseText = if (isExpenseActive) Color.White else Color(0xFFEA580C)

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onModeSelect(CanvasMode.EXPENSE) }
                    .then(
                        if (!isExpenseActive) Modifier.border(1.dp, Color(0xFFF97316).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        else Modifier
                    ),
                shape = RoundedCornerShape(14.dp),
                color = expenseBg
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (useBengaliNumerals) "📤 খরচ" else "📤 Expense",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = expenseText
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "($expenseFormatted)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = expenseText.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
