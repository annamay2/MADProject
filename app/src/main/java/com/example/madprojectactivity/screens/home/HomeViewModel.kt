package com.example.madprojectactivity.screens.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.madprojectactivity.data.repository.ReceiptRepository
import com.example.madprojectactivity.data.repository.UserRepository
import com.google.firebase.Timestamp
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
    val uploadedToRevenue: Boolean = false,
    val imageUri: Uri? = null
)

data class HomeUiState(
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null,
    val receipts: List<Receipt> = emptyList()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(application)
    private val receiptRepository = ReceiptRepository(application)

    private var remoteListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(
        HomeUiState(
            isLoggedIn = userRepository.isLoggedIn,
            userEmail = null
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState

    private val authListener = userRepository.addAuthStateListener { uid ->
        if (uid != null) {
            observeUser(uid)
            observeLocalReceipts()
            startRemoteSync()
        } else {
            stopRemoteSync()
            _uiState.update { it.copy(isLoggedIn = false, userEmail = null, receipts = emptyList()) }
        }
    }

    init {
        userRepository.currentUserId?.let { uid ->
            viewModelScope.launch {
                userRepository.persistCurrentUser()
            }
            observeUser(uid)
            observeLocalReceipts()
            startRemoteSync()
        }
    }

    private fun observeUser(uid: String) {
        viewModelScope.launch {
            userRepository.observeUser(uid).collect { user ->
                _uiState.update { it.copy(isLoggedIn = true, userEmail = user?.email) }
            }
        }
    }

    private fun observeLocalReceipts() {
        viewModelScope.launch {
            receiptRepository.getReceiptsForUser().collect { entities ->
                val list = entities.map { entity ->
                    Receipt(
                        id = entity.id,
                        amount = entity.amount,
                        storeName = entity.storeName,
                        date = Timestamp(Date(entity.date)),
                        uploadedToRevenue = entity.uploadedToRevenue,
                        imageUri = entity.imageUri?.let { Uri.parse(it) }
                    )
                }
                _uiState.update { it.copy(receipts = list) }
            }
        }
    }

    private fun startRemoteSync() {
        remoteListener?.remove()
        remoteListener = receiptRepository.addRemoteListener()
    }

    private fun stopRemoteSync() {
        remoteListener?.remove()
        remoteListener = null
    }

    override fun onCleared() {
        userRepository.removeAuthStateListener(authListener)
        stopRemoteSync()
        super.onCleared()
    }

    fun logout() = userRepository.signOut()
}
