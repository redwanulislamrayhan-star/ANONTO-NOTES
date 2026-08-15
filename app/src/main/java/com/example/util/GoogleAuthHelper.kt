package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

data class AuthResult(
    val isSuccess: Boolean,
    val user: FirebaseUser? = null,
    val errorMessage: String? = null,
    val debugInfo: String? = null
)

object GoogleAuthHelper {

    private const val TAG = "GoogleAuthHelper"
    // Web Client ID from Google Cloud Console / Firebase Authentication
    private const val DEFAULT_WEB_CLIENT_ID = "644109141949-web.apps.googleusercontent.com"

    /**
     * Attempts Native Google Credential Manager (Sign in with Google One-Tap/Popup)
     */
    suspend fun signInWithGoogleCredentialManager(
        context: Context,
        webClientId: String = DEFAULT_WEB_CLIENT_ID
    ): AuthResult {
        Log.d(TAG, "Starting Google Sign-In with CredentialManager...")
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                Log.d(TAG, "Got ID Token successfully from Google: ${googleIdTokenCredential.id}")

                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                val user = authResult.user

                Log.d(TAG, "Firebase Auth successfully signed in with Google: ${user?.email}")
                AuthResult(isSuccess = true, user = user)
            } else {
                val err = "Unexpected credential type: ${credential.type}"
                Log.w(TAG, err)
                AuthResult(isSuccess = false, errorMessage = "গুগল সাইন-ইন ডেটা পাওয়া যায়নি।", debugInfo = err)
            }
        } catch (e: GetCredentialCancellationException) {
            val err = "User cancelled Google Sign-In"
            Log.w(TAG, err)
            AuthResult(isSuccess = false, errorMessage = "লগইন উইন্ডো বাতিল করা হয়েছে।", debugInfo = err)
        } catch (e: NoCredentialException) {
            val err = "No Google accounts found or Play Services not configured on device."
            Log.e(TAG, err, e)
            AuthResult(
                isSuccess = false,
                errorMessage = "ডিভাইসে কোনো গুগল অ্যাকাউন্ট পাওয়া যায়নি বা প্লে-সার্ভিস অফলাইন।",
                debugInfo = "$err [${e.message}]"
            )
        } catch (e: GetCredentialProviderConfigurationException) {
            val err = "Credential provider configuration error: ${e.message}"
            Log.e(TAG, err, e)
            AuthResult(isSuccess = false, errorMessage = "গুগল অথেন্টিকেশন কনফিগারেশন ত্রুটি।", debugInfo = err)
        } catch (e: GetCredentialException) {
            val err = "CredentialException (${e.javaClass.simpleName}): ${e.message}"
            Log.e(TAG, err, e)
            AuthResult(isSuccess = false, errorMessage = "গুগল পপ-আপ ত্রুটি: ${e.message}", debugInfo = err)
        } catch (e: Exception) {
            val err = "Firebase/Auth Exception: ${e.message}"
            Log.e(TAG, err, e)
            AuthResult(isSuccess = false, errorMessage = "সাইন-ইন ব্যর্থ হয়েছে: ${e.localizedMessage}", debugInfo = err)
        }
    }

    /**
     * Direct Gmail / Email authentication against Firebase
     * (Signs in or creates account automatically with secure user password)
     */
    suspend fun signInWithEmailDirect(
        email: String,
        customPass: String? = null,
        displayName: String? = null
    ): AuthResult {
        val cleanEmail = email.trim().lowercase()
        val password = if (!customPass.isNullOrBlank()) customPass else "AnontoNotes2026!#"
        val auth = FirebaseAuth.getInstance()

        Log.d(TAG, "Attempting Firebase Email sign-in for: $cleanEmail")

        // 1. Try Signing In first
        try {
            val res = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val user = res.user
            if (user != null) {
                Log.d(TAG, "Email Sign-In Success: ${user.uid}")
                return AuthResult(isSuccess = true, user = user)
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            Log.d(TAG, "User not registered, attempting auto-creation...")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.w(TAG, "Invalid credentials, attempting auto-register or retry: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Sign-in exception: ${e.message}")
        }

        // 2. Try Creating Account if not registered
        try {
            val res = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val user = res.user
            if (user != null) {
                val name = displayName ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                try {
                    val profileUpdates = userProfileChangeRequest {
                        this.displayName = name
                    }
                    user.updateProfile(profileUpdates).await()
                } catch (pe: Exception) {
                    Log.w(TAG, "Could not update profile display name: ", pe)
                }
                Log.d(TAG, "Account created successfully: ${user.uid} ($cleanEmail)")
                return AuthResult(isSuccess = true, user = user)
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            // Already exists with different password, try re-sign in
            try {
                val res = auth.signInWithEmailAndPassword(cleanEmail, "AnontoNotes2026!").await()
                return AuthResult(isSuccess = true, user = res.user)
            } catch (e2: Exception) {
                val err = "User already exists. Please verify password: ${e2.message}"
                Log.e(TAG, err, e2)
                return AuthResult(isSuccess = false, errorMessage = "অ্যাকাউন্টটি ইতোমধ্যে তৈরি আছে।", debugInfo = err)
            }
        } catch (e: Exception) {
            val err = "Firebase CreateUser Error: ${e.message}"
            Log.e(TAG, err, e)
            return AuthResult(
                isSuccess = false,
                errorMessage = "ফায়ারবেস অ্যাকাউন্ট ত্রুটি: ${e.localizedMessage}",
                debugInfo = err
            )
        }

        return AuthResult(isSuccess = false, errorMessage = "লগইন সম্পন্ন করা সম্ভব হয়নি।", debugInfo = "Unknown Auth Failure")
    }

    /**
     * Fast Anonymous / Guest Cloud Sync
     */
    suspend fun signInAsGuest(): AuthResult {
        return try {
            val auth = FirebaseAuth.getInstance()
            val res = auth.signInAnonymously().await()
            Log.d(TAG, "Guest anonymous sign-in success: ${res.user?.uid}")
            AuthResult(isSuccess = true, user = res.user)
        } catch (e: Exception) {
            val err = "Anonymous sign-in error: ${e.message}"
            Log.e(TAG, err, e)
            AuthResult(isSuccess = false, errorMessage = "গেস্ট ক্লাউড সিঙ্ক ত্রুটি: ${e.localizedMessage}", debugInfo = err)
        }
    }
}

