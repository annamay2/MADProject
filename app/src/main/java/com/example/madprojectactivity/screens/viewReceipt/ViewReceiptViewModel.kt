package com.example.madprojectactivity.screens.viewReceipt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.madprojectactivity.data.local.AppDatabase
import com.example.madprojectactivity.screens.home.Receipt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    private val _uiState = MutableStateFlow(ViewReceiptUiState())
    val uiState: StateFlow<ViewReceiptUiState> = _uiState

    fun loadReceipt(receiptId: String) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
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
                
                val updated = _uiState.value.receipt?.copy(
                    amount = amount, 
                    storeName = storeName,
                    uploadedToRevenue = uploadedToRevenue
                )
                _uiState.update { it.copy(receipt = updated, isEditing = false, isSaving = false) }
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
                db.collection("users")
                    .document(uid)
                    .collection("receipts")
                    .document(receipt.id)
                    .update("uploadedToRevenue", newValue)
                    .await()
                
                _uiState.update { it.copy(receipt = receipt.copy(uploadedToRevenue = newValue)) }
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
                // 1. Delete from Remote Firestore
                db.collection("users")
                    .document(uid)
                    .collection("receipts")
                    .document(receiptId)
                    .delete()
                    .await()
                
                // 2. Delete from Local Room Database
                receiptDao.deleteById(receiptId)
                
                onDeleted()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}
