package com.example.ui.components

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceInputDialog(
    onDismiss: () -> Unit,
    onSpeechResult: (String) -> Unit,
    useBengali: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var spokenText by remember { mutableStateOf("") }

    val samplePhrases = listOf(
        "চাল ২০০০ + মাছ ১৫০০ + ওষুধ ৮০০",
        "আম্মু ডাক্তার ১৫০০",
        "ইভা রিমশা ওষুধ ৬০০",
        "বিদ্যুৎ বিল ২৫০০",
        "বাসা ভাড়া ১২০০০",
        "স্কুল টিউশন ফি ৩০০০"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (spokenText.isNotBlank()) {
                        onSpeechResult(spokenText)
                        onDismiss()
                    }
                },
                enabled = spokenText.isNotBlank(),
                modifier = Modifier.testTag("voice_confirm_button")
            ) {
                Text(if (useBengali) "নোটবুক-এ যোগ করুন" else "Append to Ledger")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (useBengali) "বাতিল" else "Cancel")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (useBengali) "ভয়েস ইনপুট (Voice Input)" else "Voice-to-Ledger Input",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (useBengali) {
                        "মুখে বাংলায় বা ইংরেজিতে বলুন। যেমন: 'চাল ২০০০ টাকা, ওষুধ ৫০০, বাজার ১৫০০'"
                    } else {
                        "Speak items naturally e.g., 'Rice 2000, Medicine 500, Grocery 1500'"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mic Pulse Animation Button
                Surface(
                    onClick = {
                        isListening = !isListening
                        if (isListening) {
                            Toast.makeText(context, if (useBengali) "ভয়েস শুনছে..." else "Listening...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = CircleShape,
                    color = if (isListening) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("voice_mic_tap")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isListening) (if (useBengali) "বলা শুরু করুন..." else "Listening...") else (if (useBengali) "মাইক্রোফোনে চাপ দিন" else "Tap microphone"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isListening) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (spokenText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = spokenText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Voice Sample Chips
                Text(
                    text = if (useBengali) "অথবা দ্রুত ডেমো প্রেস করুন:" else "Or tap a sample phrase:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    samplePhrases.forEach { sample ->
                        Surface(
                            onClick = {
                                spokenText = sample
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = sample,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
