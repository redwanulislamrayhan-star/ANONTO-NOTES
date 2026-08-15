package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.LedgerParser

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddLedgerDialog(
    initialYear: Int,
    initialMonth: Int,
    useBengali: Boolean,
    onSelectSheet: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var yearInputText by remember { mutableStateOf(initialYear.toString()) }
    var selectedMo by remember { mutableIntStateOf(initialMonth) }

    val currentCalYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val yearPresets = listOf(currentCalYear - 2, currentCalYear - 1, currentCalYear, currentCalYear + 1, currentCalYear + 2, currentCalYear + 5, currentCalYear + 10)

    val selectedYr = LedgerParser.convertBengaliDigitsToEnglish(yearInputText).toIntOrNull() ?: initialYear

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onSelectSheet(selectedYr, selectedMo)
                    onDismiss()
                }
            ) {
                Text(if (useBengali) "খাতা খুলুন" else "Open Ledger Sheet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (useBengali) "বাতিল" else "Cancel")
            }
        },
        title = {
            Text(
                text = if (useBengali) "+ নতুন হিসাব খাতা যোগ করুন" else "+ Open New Ledger Sheet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (useBengali) "অতীত বা ভবিষ্যতের যেকোনো মাসের নতুন পাতা খুলুন:" else "Select year and month to open ledger sheet:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Year Input Field & Chips
                Text(
                    text = if (useBengali) "বছর ইনপুট দিন (Type Any Year):" else "Enter Any Year:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = yearInputText,
                    onValueChange = { yearInputText = it },
                    singleLine = true,
                    label = { Text(if (useBengali) "বছর (Year)" else "Year") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    yearPresets.forEach { yr ->
                        val isSel = yr == selectedYr
                        val yrDisp = if (useBengali) LedgerParser.convertEnglishDigitsToBengali(yr.toString()) else yr.toString()

                        Surface(
                            onClick = { yearInputText = yr.toString() },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = yrDisp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Month Selection Grid
                Text(
                    text = if (useBengali) "মাস নির্বাচন (Month):" else "Select Month:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..12).forEach { mo ->
                        val isSel = mo == selectedMo
                        val moName = LedgerParser.getMonthName(mo, useBengali)

                        Surface(
                            onClick = { selectedMo = mo },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = moName,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    )
}
