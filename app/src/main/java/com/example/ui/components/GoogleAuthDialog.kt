package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirestoreSyncManager
import com.example.util.AuthResult
import com.example.util.GoogleAuthHelper
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun GoogleAuthDialog(
    currentUser: FirebaseUser?,
    syncManager: FirestoreSyncManager,
    useBengali: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isAuthLoading by remember { mutableStateOf(false) }
    var loadingStatusText by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf(currentUser?.email ?: "redwanulislamrayhan@gmail.com") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var debugLog by remember { mutableStateOf<String?>(null) }
    var showDebugInfo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isAuthLoading) onDismiss()
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isAuthLoading
            ) {
                Text(if (useBengali) "বন্ধ করুন" else "Close")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Cloud Sync",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (useBengali) "গুগল সাইন-ইন ও ক্লাউড সিঙ্ক" else "Google Sign-In & Cloud Sync",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentUser != null) {
                    // Logged In Status Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (currentUser.displayName?.take(1) ?: currentUser.email?.take(1) ?: "G").uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Google User",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = currentUser.email ?: "Firebase User (${currentUser.uid.take(8)})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Cloud Connected",
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (useBengali) "ফায়ারবেস ক্লাউড ব্যাকআপ সক্রিয় ☁️" else "Firestore Cloud Sync Active ☁️",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            syncManager.signOut()
                            Toast.makeText(
                                context,
                                if (useBengali) "লগআউট সফল হয়েছে" else "Logged Out Successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (useBengali) "লগআউট করুন" else "Sign Out")
                    }
                } else {
                    // Not Logged In - Sign In Interface
                    Text(
                        text = if (useBengali)
                            "গুগল দিয়ে লগইন করলে সমস্ত নোট ও হিসাব ফায়ারবেস ক্লাউডে রিয়েলটাইমে সেভ থাকবে এবং যেকোনো ফোন বা ব্রাউজার থেকে নিমেষেই ভেসে উঠবে।"
                        else
                            "Sign in with Google to automatically backup all your ledger notes to Firebase Firestore and sync across devices in real-time.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Error Box Banner if any error occurred
                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (useBengali) "সাইন-ইন সমস্যা হয়েছে" else "Sign-in Notice",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )

                                if (debugLog != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showDebugInfo = !showDebugInfo },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (showDebugInfo) "লগ লুকান ▲" else "কনসোল এরর বিস্তারিত দেখুন ▼",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    AnimatedVisibility(visible = showDebugInfo) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 6.dp)
                                                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = debugLog ?: "",
                                                color = Color(0xFF34D399),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isAuthLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = loadingStatusText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Method 1: Google One-Tap / Popup
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isAuthLoading = true
                                    errorMessage = null
                                    debugLog = null
                                    loadingStatusText = if (useBengali) "গুগল সাইন-ইন পপ-আপ খোলা হচ্ছে..." else "Opening Google Sign-In..."

                                    val result = GoogleAuthHelper.signInWithGoogleCredentialManager(context)
                                    if (result.isSuccess) {
                                        Toast.makeText(
                                            context,
                                            if (useBengali) "গুগল দিয়ে লগইন সফল হয়েছে! ☁️" else "Signed in successfully! ☁️",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onDismiss()
                                    } else {
                                        errorMessage = result.errorMessage ?: "গুগল পপ-আপ রেসপন্স পাওয়া যায়নি।"
                                        debugLog = result.debugInfo

                                        // Fallback attempt with direct email if email entered
                                        if (inputEmail.isNotBlank() && inputEmail.contains("@")) {
                                            loadingStatusText = if (useBengali) "জিমেইল দিয়ে স্বয়ংক্রিয় সিঙ্ক হচ্ছে..." else "Connecting via Gmail..."
                                            val fbResult = GoogleAuthHelper.signInWithEmailDirect(inputEmail)
                                            if (fbResult.isSuccess) {
                                                Toast.makeText(
                                                    context,
                                                    if (useBengali) "জিমেইল দিয়ে লগইন সফল হয়েছে! ☁️" else "Gmail Sync Connected! ☁️",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                onDismiss()
                                            } else {
                                                errorMessage = "${result.errorMessage}\n(${fbResult.errorMessage})"
                                                debugLog = "${result.debugInfo}\n\nEmail Auth: ${fbResult.debugInfo}"
                                            }
                                        }
                                    }
                                    isAuthLoading = false
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_signin_popup_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (useBengali) "গুগল দিয়ে ১-ক্লিকে লগইন" else "Sign in with Google",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Divider(modifier = Modifier.weight(1f))
                            Text(
                                text = if (useBengali) " অথবা সরাসরি জিমেইল " else " OR DIRECT GMAIL ",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Divider(modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Method 2: Direct Gmail Sync input
                        OutlinedTextField(
                            value = inputEmail,
                            onValueChange = { inputEmail = it },
                            singleLine = true,
                            label = { Text(if (useBengali) "আপনার জিমেইল (Gmail)" else "Your Gmail") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Email") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gmail_input_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (inputEmail.isBlank() || !inputEmail.contains("@")) {
                                    Toast.makeText(
                                        context,
                                        if (useBengali) "দয়া করে সঠিক জিমেইল লিখুন" else "Please enter a valid Gmail",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                coroutineScope.launch {
                                    isAuthLoading = true
                                    errorMessage = null
                                    debugLog = null
                                    loadingStatusText = if (useBengali) "ফায়ারবেস ক্লাউডে কানেক্ট হচ্ছে..." else "Connecting to Firebase..."

                                    val result = GoogleAuthHelper.signInWithEmailDirect(
                                        email = inputEmail,
                                        displayName = inputEmail.substringBefore("@")
                                    )

                                    if (result.isSuccess) {
                                        Toast.makeText(
                                            context,
                                            if (useBengali) "ক্লাউড সিঙ্ক সফলভাবে সংযুক্ত হয়েছে! ☁️" else "Cloud Sync Connected! ☁️",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onDismiss()
                                    } else {
                                        errorMessage = result.errorMessage
                                        debugLog = result.debugInfo
                                    }
                                    isAuthLoading = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("direct_gmail_sync_button")
                        ) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = "Sync")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (useBengali) "এই জিমেইল দিয়ে দ্রুত সিঙ্ক করুন" else "Quick Sync with this Gmail",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Method 3: Instant Guest/Anonymous Sync
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isAuthLoading = true
                                    errorMessage = null
                                    debugLog = null
                                    loadingStatusText = if (useBengali) "গেস্ট ক্লাউড সিঙ্ক চালু হচ্ছে..." else "Starting Instant Cloud Sync..."

                                    val result = GoogleAuthHelper.signInAsGuest()
                                    if (result.isSuccess) {
                                        Toast.makeText(
                                            context,
                                            if (useBengali) "গেস্ট ক্লাউড ব্যাকআপ চালু হয়েছে! ☁️" else "Guest Cloud Backup Active! ☁️",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onDismiss()
                                    } else {
                                        errorMessage = result.errorMessage
                                        debugLog = result.debugInfo
                                    }
                                    isAuthLoading = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("guest_sync_button")
                        ) {
                            Text(
                                text = if (useBengali) "পাসওয়ার্ড ছাড়া ইনস্ট্যান্ট ক্লাউড সিঙ্ক ⚡" else "Instant Cloud Sync (No Password) ⚡",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    )
}
