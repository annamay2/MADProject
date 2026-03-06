package com.example.madprojectactivity.screens.home

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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

class HomeViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

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
            observeReceipts(user.uid)
        } else {
            _uiState.update { it.copy(receipts = emptyList()) }
        }
    }

    init {
        auth.addAuthStateListener(authListener)
        auth.currentUser?.let { observeReceipts(it.uid) }
    }

    private fun observeReceipts(uid: String) {
        db.collection("users")
            .document(uid)
            .collection("receipts")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                val list = snapshot?.documents?.mapNotNull { doc ->
                    Receipt(
                        id = doc.id,
                        amount = doc.getDouble("amount") ?: 0.0,
                        storeName = doc.getString("storeName") ?: "",
                        date = doc.getTimestamp("date"),
                        uploadedToRevenue = doc.getBoolean("uploadedToRevenue") ?: false
                    )
                } ?: emptyList()

                _uiState.update { it.copy(receipts = list) }
            }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    fun logout() = auth.signOut()
}
