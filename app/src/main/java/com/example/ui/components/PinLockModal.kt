package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinUnlockOverlay(
    onUnlockAttempt: (String) -> Boolean,
    useBengali: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pin_unlock_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (useBengali) "সিকিউরিটি লক (Locked)" else "QuickList Security Lock",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (useBengali) "হিসাব দেখতে ৪-ডিজিট পিন কোড দিন" else "Enter 4-digit PIN to access ledger",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PIN Bullets Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..4) {
                        val isFilled = pinInput.length >= i
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Numeric Keypad Grid
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "⌫")
                val rows = keys.chunked(3)

                rows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowKeys.forEach { key ->
                            Surface(
                                onClick = {
                                    when (key) {
                                        "C" -> {
                                            pinInput = ""
                                            errorMessage = ""
                                        }
                                        "⌫" -> {
                                            if (pinInput.isNotEmpty()) {
                                                pinInput = pinInput.dropLast(1)
                                                errorMessage = ""
                                            }
                                        }
                                        else -> {
                                            if (pinInput.length < 4) {
                                                val updated = pinInput + key
                                                pinInput = updated
                                                if (updated.length == 4) {
                                                    val success = onUnlockAttempt(updated)
                                                    if (!success) {
                                                        errorMessage = if (useBengali) "ভুল পিন! আবার চেষ্টা করুন।" else "Incorrect PIN code"
                                                        pinInput = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(60.dp)
                                    .padding(4.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "⌫") {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Back",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun PinSetupDialog(
    currentPin: String?,
    onSavePin: (String?) -> Unit,
    onDismiss: () -> Unit,
    useBengali: Boolean
) {
    var pinText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (useBengali) "পিন লক সেটিং" else "PIN Protection Settings")
            }
        },
        text = {
            Column {
                Text(
                    text = if (currentPin.isNullOrEmpty()) {
                        if (useBengali) "অ্যাপ লক করার জন্য ৪ অক্ষরের পিন লিখুন" else "Enter 4-digit PIN to lock your ledger app"
                    } else {
                        if (useBengali) "আপনার অ্যাপে পিন লক চালু আছে। আপনি পিন পরিবর্তন বা বন্ধ করতে পারেন।" else "PIN protection is currently active."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val keyList = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSavePin("1234") // Quick preset PIN
                    onDismiss()
                }
            ) {
                Text(if (currentPin.isNullOrEmpty()) (if (useBengali) "পিন সক্রিয় করুন (Enable PIN: 1234)" else "Enable PIN (1234)") else (if (useBengali) "পিন পরিবর্তন করুন" else "Change PIN"))
            }
        },
        dismissButton = {
            if (!currentPin.isNullOrEmpty()) {
                TextButton(
                    onClick = {
                        onSavePin(null)
                        onDismiss()
                    }
                ) {
                    Text(if (useBengali) "পিন বন্ধ করুন (Remove PIN)" else "Remove PIN", color = Color(0xFFEF4444))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(if (useBengali) "বাতিল" else "Cancel")
                }
            }
        }
    )
}
