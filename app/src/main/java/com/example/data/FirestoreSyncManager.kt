package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager private constructor(private val context: Context) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>("অফলাইন/রেডি")
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private var firestoreListener: ListenerRegistration? = null
    private var repository: LedgerRepository? = null

    init {
        ensureFirebaseApp(context)
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                _syncStatus.value = "ক্লাউড কানেক্টেড ☁️"
                startRealtimeSync()
            } else {
                _syncStatus.value = "অফলাইন মোড"
                stopRealtimeSync()
            }
        }
    }

    fun setRepository(repo: LedgerRepository) {
        this.repository = repo
        if (auth.currentUser != null) {
            startRealtimeSync()
        }
    }

    private fun startRealtimeSync() {
        val user = auth.currentUser ?: return
        val repo = repository ?: return

        firestoreListener?.remove()
        _isSyncing.value = true

        // 1. Initial One-time getDocs Fetch (Immediate sync)
        firestore.collection("users")
            .document(user.uid)
            .collection("ledger_entries")
            .get()
            .addOnSuccessListener { snapshot ->
                _isSyncing.value = false
                if (snapshot != null && !snapshot.isEmpty) {
                    CoroutineScope(Dispatchers.IO).launch {
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.getLong("id") ?: doc.id.hashCode().toLong()
                                val rawNote = doc.getString("rawNote") ?: ""
                                val totalAmount = doc.getDouble("totalAmount") ?: 0.0
                                val summaryText = doc.getString("summaryText") ?: ""
                                val tagsJson = doc.getString("tagsJson") ?: ""
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val yearMonth = doc.getString("yearMonth") ?: "2026-08"
                                val year = doc.getLong("year")?.toInt() ?: 2026
                                val month = doc.getLong("month")?.toInt() ?: 8
                                val entryType = doc.getString("entryType") ?: LedgerEntry.ENTRY_TYPE_EXPENSE
                                val ledgerAccount = doc.getString("ledgerAccount") ?: LedgerEntry.DEFAULT_ACCOUNT
                                val currencySymbol = doc.getString("currencySymbol") ?: "৳"

                                val entry = LedgerEntry(
                                    id = id,
                                    rawNote = rawNote,
                                    totalAmount = totalAmount,
                                    summaryText = summaryText,
                                    tagsJson = tagsJson,
                                    timestamp = timestamp,
                                    yearMonth = yearMonth,
                                    year = year,
                                    month = month,
                                    entryType = entryType,
                                    ledgerAccount = ledgerAccount,
                                    currencySymbol = currencySymbol
                                )
                                repo.insert(entry)
                            } catch (e: Exception) {
                                Log.e("FirestoreSync", "Error parsing initial doc ${doc.id}", e)
                            }
                        }
                        _syncStatus.value = "সব পুরানো ডাটা সফলভাবে ফেচ হয়েছে ☁️"
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("FirestoreSync", "Initial getDocs fetch notice: ${e.message}")
            }

        // 2. Realtime Snapshot Listener for live 2-way sync across all devices
        firestoreListener = firestore.collection("users")
            .document(user.uid)
            .collection("ledger_entries")
            .addSnapshotListener { snapshot, error ->
                _isSyncing.value = false
                if (error != null) {
                    Log.e("FirestoreSync", "Error listening to sync: ", error)
                    _syncStatus.value = "সিঙ্ক ত্রুটি: ${error.localizedMessage}"
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    CoroutineScope(Dispatchers.IO).launch {
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.getLong("id") ?: doc.id.hashCode().toLong()
                                val rawNote = doc.getString("rawNote") ?: ""
                                val totalAmount = doc.getDouble("totalAmount") ?: 0.0
                                val summaryText = doc.getString("summaryText") ?: ""
                                val tagsJson = doc.getString("tagsJson") ?: ""
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val yearMonth = doc.getString("yearMonth") ?: "2026-08"
                                val year = doc.getLong("year")?.toInt() ?: 2026
                                val month = doc.getLong("month")?.toInt() ?: 8
                                val entryType = doc.getString("entryType") ?: LedgerEntry.ENTRY_TYPE_EXPENSE
                                val ledgerAccount = doc.getString("ledgerAccount") ?: LedgerEntry.DEFAULT_ACCOUNT
                                val currencySymbol = doc.getString("currencySymbol") ?: "৳"

                                val entry = LedgerEntry(
                                    id = id,
                                    rawNote = rawNote,
                                    totalAmount = totalAmount,
                                    summaryText = summaryText,
                                    tagsJson = tagsJson,
                                    timestamp = timestamp,
                                    yearMonth = yearMonth,
                                    year = year,
                                    month = month,
                                    entryType = entryType,
                                    ledgerAccount = ledgerAccount,
                                    currencySymbol = currencySymbol
                                )
                                repo.insert(entry)
                            } catch (e: Exception) {
                                Log.e("FirestoreSync", "Error parsing doc ${doc.id}", e)
                            }
                        }
                        _syncStatus.value = "সব ক্লাউড ডাটা অটো-সিঙ্ক হয়েছে ☁️"
                    }
                }
            }
    }

    private fun stopRealtimeSync() {
        firestoreListener?.remove()
        firestoreListener = null
    }

    suspend fun syncEntryToCloud(entry: LedgerEntry) {
        val user = auth.currentUser ?: return
        _isSyncing.value = true
        try {
            val docRef = firestore.collection("users")
                .document(user.uid)
                .collection("ledger_entries")
                .document(entry.id.toString())

            val data = hashMapOf(
                "id" to entry.id,
                "rawNote" to entry.rawNote,
                "totalAmount" to entry.totalAmount,
                "summaryText" to entry.summaryText,
                "tagsJson" to entry.tagsJson,
                "timestamp" to entry.timestamp,
                "yearMonth" to entry.yearMonth,
                "year" to entry.year,
                "month" to entry.month,
                "entryType" to entry.entryType,
                "ledgerAccount" to entry.ledgerAccount,
                "currencySymbol" to entry.currencySymbol,
                "updatedAt" to System.currentTimeMillis()
            )

            docRef.set(data, SetOptions.merge()).await()
            _syncStatus.value = "ক্লাউডে সংরক্ষিত ☁️"
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Failed to upload entry to Firestore", e)
            _syncStatus.value = "ব্যাকআপ ফেইলড"
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun deleteEntryFromCloud(entryId: Long) {
        val user = auth.currentUser ?: return
        try {
            firestore.collection("users")
                .document(user.uid)
                .collection("ledger_entries")
                .document(entryId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Failed to delete entry from Firestore", e)
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        stopRealtimeSync()
    }

    companion object {
        @Volatile
        private var INSTANCE: FirestoreSyncManager? = null

        fun getInstance(context: Context): FirestoreSyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FirestoreSyncManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        private fun ensureFirebaseApp(context: Context) {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyAGe5xLtf1CPyy0RO9ZTg0pYegRoCiXbYg")
                    .setApplicationId("1:644109141949:web:8ea9702cf5db4b8ed0f4d4")
                    .setGcmSenderId("644109141949")
                    .setProjectId("anonto-notes")
                    .setStorageBucket("anonto-notes.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
        }
    }
}
