package com.example.madprojectactivity.screens.viewReceipt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.madprojectactivity.data.repository.ReceiptRepository
import com.example.madprojectactivity.screens.home.Receipt
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import androidx.core.net.toUri

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
    private val repository = ReceiptRepository(application)

    private val _uiState = MutableStateFlow(ViewReceiptUiState())
    val uiState: StateFlow<ViewReceiptUiState> = _uiState

    fun loadReceipt(receiptId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val entity = repository.getReceipt(receiptId)

                if (entity != null) {
                    val receipt = Receipt(
                        id = entity.id,
                        amount = entity.amount,
                        storeName = entity.storeName,
                        date = Timestamp(Date(entity.date)),
                        uploadedToRevenue = entity.uploadedToRevenue,
                        imageUri = entity.imageUri?.toUri()
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
        val receiptId = _uiState.value.receipt?.id ?: return
        val amount = _uiState.value.editAmount.toDoubleOrNull() ?: return
        val storeName = _uiState.value.editStoreName
        val uploadedToRevenue = _uiState.value.editUploadedToRevenue

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repository.updateReceipt(receiptId, mapOf(
                    "amount" to amount,
                    "storeName" to storeName,
                    "uploadedToRevenue" to uploadedToRevenue
                ))

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
        val receipt = _uiState.value.receipt ?: return
        val newValue = !receipt.uploadedToRevenue

        // Update UI immediately so the Switch responds instantly
        _uiState.update { it.copy(receipt = receipt.copy(uploadedToRevenue = newValue)) }

        viewModelScope.launch {
            try {
                repository.updateReceipt(receipt.id, mapOf(
                    "uploadedToRevenue" to newValue
                ))
            } catch (e: Exception) {
                // Revert on failure
                _uiState.update { it.copy(receipt = receipt.copy(uploadedToRevenue = !newValue), errorMessage = e.message) }
            }
        }
    }

    fun deleteReceipt(onDeleted: () -> Unit) {
        val receiptId = _uiState.value.receipt?.id ?: return

        viewModelScope.launch {
            try {
                repository.deleteReceipt(receiptId)
                onDeleted()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}
