package com.example.madprojectactivity.screens.viewReceipt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.madprojectactivity.data.local.AppDatabase
import com.example.madprojectactivity.data.worker.SyncWorker
import com.example.madprojectactivity.screens.home.Receipt
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

data class ViewReceiptUiState(
    val receipt: Receipt? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isEditing: Boolean = false,
    val editAmount: String = "",
    val editStoreName: String = "",
    val editUploadedToRevenue: Boolean = false
)

class ViewReceiptViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val receiptDao = AppDatabase.getDatabase(application).receiptDao()
    private val workManager = WorkManager.getInstance(application)

    private val _uiState = MutableStateFlow(ViewReceiptUiState())
    val uiState: StateFlow<ViewReceiptUiState> = _uiState

    fun loadReceipt(receiptId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Load from local database first to support offline
                val entity = receiptDao.getReceiptById(receiptId)

                if (entity != null) {
                    val receipt = Receipt(
                        id = entity.id,
                        amount = entity.amount,
                        storeName = entity.storeName,
                        date = Timestamp(Date(entity.date)),
                        uploadedToRevenue = entity.uploadedToRevenue
                    )
                    _uiState.update { it.copy(receipt = receipt, isLoading = false) }
                } else {
                    // Fallback to Firestore if not found locally (unlikely with the sync)
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val doc = db.collection("users")
                            .document(uid)
                            .collection("receipts")
                            .document(receiptId)
                            .get()
                            .await()

                        if (doc.exists()) {
                            val receipt = Receipt(
                                id = doc.id,
                                amount = doc.getDouble("amount") ?: 0.0,
                                storeName = doc.getString("storeName") ?: "",
                                date = doc.getTimestamp("date"),
                                uploadedToRevenue = doc.getBoolean("uploadedToRevenue") ?: false
                            )
                            _uiState.update { it.copy(receipt = receipt, isLoading = false) }
                        } else {
                            _uiState.update { it.copy(isLoading = false, errorMessage = "Receipt not found.") }
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "User not logged in.") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load receipt.") }
            }
        }
    }

    fun startEditing() {
        val r = _uiState.value.receipt ?: return
        _uiState.update { it.copy(
            isEditing = true,
            editAmount = r.amount.toString(),
            editStoreName = r.storeName,
            editUploadedToRevenue = r.uploadedToRevenue
        ) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun onEditAmountChange(v: String) {
        _uiState.update { it.copy(editAmount = v) }
    }

    fun onEditStoreNameChange(v: String) {
        _uiState.update { it.copy(editStoreName = v) }
    }

    fun onEditUploadedToRevenueChange(v: Boolean) {
        _uiState.update { it.copy(editUploadedToRevenue = v) }
    }

    fun saveChanges() {
        val uid = auth.currentUser?.uid ?: return
        val receiptId = _uiState.value.receipt?.id ?: return
        val amount = _uiState.value.editAmount.toDoubleOrNull() ?: return
        val storeName = _uiState.value.editStoreName
        val uploadedToRevenue = _uiState.value.editUploadedToRevenue

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // 1. Update local database first
                val existingEntity = receiptDao.getReceiptById(receiptId)
                if (existingEntity != null) {
                    val updatedEntity = existingEntity.copy(
                        amount = amount,
                        storeName = storeName,
                        uploadedToRevenue = uploadedToRevenue,
                        isSynced = false // Mark for sync
                    )
                    receiptDao.insertReceipt(updatedEntity)
                }

                // 2. Enqueue SyncWorker to update remote
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .build()
                workManager.enqueue(syncRequest)
                
                // 3. Update UI state
                val updated = _uiState.value.receipt?.copy(
                    amount = amount, 
                    storeName = storeName,
                    uploadedToRevenue = uploadedToRevenue
                )
                _uiState.update { it.copy(receipt = updated, isEditing = false, isSaving = false) }

                // 4. Try updating Firestore immediately (best effort)
                launch {
                    try {
                        db.collection("users")
                            .document(uid)
                            .collection("receipts")
                            .document(receiptId)
                            .update(mapOf(
                                "amount" to amount,
                                "storeName" to storeName,
                                "uploadedToRevenue" to uploadedToRevenue
                            ))
                            .await()
                        // If immediate update succeeds, we could mark as synced, 
                        // but SyncWorker will handle it safely anyway.
                    } catch (e: Exception) {
                        // Ignore immediate update failure (SyncWorker handles it)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun toggleUploadedToRevenue() {
        val uid = auth.currentUser?.uid ?: return
        val receipt = _uiState.value.receipt ?: return
        val newValue = !receipt.uploadedToRevenue

        viewModelScope.launch {
            try {
                // 1. Update local database
                val existingEntity = receiptDao.getReceiptById(receipt.id)
                if (existingEntity != null) {
                    val updatedEntity = existingEntity.copy(
                        uploadedToRevenue = newValue,
                        isSynced = false
                    )
                    receiptDao.insertReceipt(updatedEntity)
                }

                // 2. Enqueue sync
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .build()
                workManager.enqueue(syncRequest)

                // 3. Update UI
                _uiState.update { it.copy(receipt = receipt.copy(uploadedToRevenue = newValue)) }

                // 4. Try updating Firestore immediately
                launch {
                    try {
                        db.collection("users")
                            .document(uid)
                            .collection("receipts")
                            .document(receipt.id)
                            .update("uploadedToRevenue", newValue)
                            .await()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteReceipt(onDeleted: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val receiptId = _uiState.value.receipt?.id ?: return

        viewModelScope.launch {
            try {
                // 1. Delete from Local Room Database first (immediate feedback)
                receiptDao.deleteById(receiptId)
                
                // 2. Delete from Remote Firestore
                launch {
                    try {
                        db.collection("users")
                            .document(uid)
                            .collection("receipts")
                            .document(receiptId)
                            .delete()
                            .await()
                    } catch (e: Exception) {
                        // If remote deletion fails, we might need a "pending deletion" flag 
                        // in Room to handle it properly offline, but for now we delete locally.
                    }
                }
                
                onDeleted()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}
