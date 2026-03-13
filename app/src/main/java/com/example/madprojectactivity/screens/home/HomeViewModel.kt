package com.example.madprojectactivity.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.madprojectactivity.data.local.AppDatabase
import com.example.madprojectactivity.data.model.ReceiptEntity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

data class Receipt(
    val id: String = "",
    val amount: Double = 0.0,
    val storeName: String = "",
    val date: Timestamp? = null,
    val uploadedToRevenue: Boolean = false
)

data class HomeUiState(
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null,
    val receipts: List<Receipt> = emptyList()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val receiptDao = AppDatabase.getDatabase(application).receiptDao()

    private var remoteListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(
        HomeUiState(
            isLoggedIn = auth.currentUser != null,
            userEmail = auth.currentUser?.email
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState

    private val authListener = FirebaseAuth.AuthStateListener { a ->
        val user = a.currentUser
        _uiState.update { it.copy(
            isLoggedIn = user != null,
            userEmail = user?.email
        ) }
        if (user != null) {
            observeLocalReceipts(user.uid)
            startRemoteSync(user.uid)
        } else {
            stopRemoteSync()
            _uiState.update { it.copy(receipts = emptyList()) }
        }
    }

    init {
        auth.addAuthStateListener(authListener)
        auth.currentUser?.let { 
            observeLocalReceipts(it.uid)
            startRemoteSync(it.uid)
        }
    }

    private fun observeLocalReceipts(uid: String) {
        viewModelScope.launch {
            // Observing the local Room database
            receiptDao.getAllReceipts(uid).collect { entities ->
                val list = entities.map { entity ->
                    Receipt(
                        id = entity.id,
                        amount = entity.amount,
                        storeName = entity.storeName,
                        date = Timestamp(Date(entity.date)),
                        uploadedToRevenue = entity.uploadedToRevenue
                    )
                }
                _uiState.update { it.copy(receipts = list) }
            }
        }
    }

    private fun startRemoteSync(uid: String) {
        remoteListener?.remove()

        // Sync Firestore data into Room
        remoteListener = db.collection("users")
            .document(uid)
            .collection("receipts")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                viewModelScope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val entity = ReceiptEntity(
                                    id = doc.id,
                                    userId = uid,
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    storeName = doc.getString("storeName") ?: "",
                                    glutenFreeItems = doc.getString("glutenFreeItems") ?: "",
                                    uploadedToRevenue = doc.getBoolean("uploadedToRevenue") ?: false,
                                    date = doc.getTimestamp("date")?.toDate()?.time ?: 0L,
                                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                                    isSynced = true
                                )
                                receiptDao.insertReceipt(entity)
                            }
                            DocumentChange.Type.REMOVED -> {
                                receiptDao.deleteById(doc.id)
                            }
                        }
                    }
                }
            }
    }

    private fun stopRemoteSync() {
        remoteListener?.remove()
        remoteListener = null
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        stopRemoteSync()
        super.onCleared()
    }

    fun logout() = auth.signOut()
}
