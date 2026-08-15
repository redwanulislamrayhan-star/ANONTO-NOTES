package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.LedgerParser

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetSetupDialog(
    currentBudget: Double,
    currencySymbol: String,
    useBengali: Boolean,
    onSaveBudget: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var budgetText by remember { mutableStateOf(currentBudget.toInt().toString()) }

    val presetValues = listOf(20000, 30000, 50000, 75000, 100000)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = budgetText.toDoubleOrNull() ?: 50000.0
                    onSaveBudget(amount)
                    onDismiss()
                }
            ) {
                Text(if (useBengali) "সেভ করুন" else "Save Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (useBengali) "বাতিল" else "Cancel")
            }
        },
        title = {
            Text(
                text = if (useBengali) "মাসিক বাজেট নির্ধারণ (Set Budget)" else "Set Monthly Budget Limit",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (useBengali) "আপনার মাসের সর্বোচ্চ খরচের সীমা নির্ধারণ করুন:" else "Enter your maximum monthly expense target:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = LedgerParser.convertBengaliDigitsToEnglish(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Budget Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (useBengali) "কুইক সিলেকশন:" else "Quick Presets:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetValues.forEach { preset ->
                        val label = LedgerParser.formatCurrency(preset.toDouble(), currencySymbol, useBengali)
                        Surface(
                            onClick = { budgetText = preset.toString() },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    )
}
