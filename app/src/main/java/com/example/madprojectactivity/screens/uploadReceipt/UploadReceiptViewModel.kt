// AI-generated (Claude): Added onImageUriChange method and pass imageUri to
// ReceiptEntity on save for local + remote image persistence.
package com.example.madprojectactivity.screens.receipts

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.madprojectactivity.data.local.AppDatabase
import com.example.madprojectactivity.data.model.ReceiptEntity
import com.example.madprojectactivity.data.worker.SyncWorker
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId

class UploadReceiptViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val receiptDao = AppDatabase.getDatabase(application).receiptDao()
    private val workManager = WorkManager.getInstance(application)

    private val _uiState = MutableStateFlow(UploadReceiptUiState())
    val uiState: StateFlow<UploadReceiptUiState> = _uiState

    fun onCeliacAmountChange(newValue: String) {
        val filtered = newValue.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(celiacAmountText = filtered, errorMessage = null, successMessage = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun saveCeliacSpend() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.update { it.copy(errorMessage = "You must be logged in to save this.") }
            return
        }

        val amount = _uiState.value.celiacAmountText.toDoubleOrNull()
        if (amount == null || amount < 0.0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount (e.g. 12.50).") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            try {
                val doc = mapOf(
                    "celiacAmount" to amount,
                    "createdAt" to Timestamp.now()
                )

                db.collection("users")
                    .document(uid)
                    .collection("receipts")
                    .add(doc)
                    .await()

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        successMessage = "Saved!",
                        celiacAmountText = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save."
                    )
                }
            }
        }
    }

    fun onAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(amount = filtered) }
    }

    fun onStoreNameChange(value: String) {
        _uiState.update { it.copy(storeName = value) }
    }

    fun onGlutenFreeItemsChange(value: String) {
        _uiState.update { it.copy(glutenFreeItems = value) }
    }

    fun onDateChange(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
    }

    fun onUploadedToRevenueChange(value: Boolean) {
        _uiState.update { it.copy(uploadedToRevenue = value) }
    }

    fun onImageUriChange(uri: Uri?) {
        _uiState.update { it.copy(imageUri = uri) }
    }

    fun saveReceipt() {
        val uid = auth.currentUser?.uid ?: run {
            _uiState.update { it.copy(errorMessage = "Not logged in") }
            return
        }

        val amountDouble = _uiState.value.amount.toDoubleOrNull()
        if (amountDouble == null) {
            _uiState.update { it.copy(errorMessage = "Enter valid amount") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                // 1. Save to local Room database first
                val receiptEntity = ReceiptEntity(
                    userId = uid,
                    imageUri = _uiState.value.imageUri?.toString(),
                    amount = amountDouble,
                    storeName = _uiState.value.storeName,
                    glutenFreeItems = _uiState.value.glutenFreeItems,
                    uploadedToRevenue = _uiState.value.uploadedToRevenue,
                    date = _uiState.value.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    isSynced = false
                )
                
                receiptDao.insertReceipt(receiptEntity)

                // 2. Schedule SyncWorker
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .build()

                workManager.enqueue(syncRequest)

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        successMessage = "Receipt saved locally and will sync when online!",
                        amount = "",
                        storeName = "",
                        glutenFreeItems = ""
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Save failed"
                    )
                }
            }
        }
    }
}