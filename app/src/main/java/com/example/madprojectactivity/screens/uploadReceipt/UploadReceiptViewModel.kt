package com.example.madprojectactivity.screens.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class UploadReceiptViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

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
                // Save under: users/{uid}/receipts/{autoId}
                val doc = mapOf(
                    "celiacAmount" to amount,
                    "createdAt" to Timestamp.now()
                    // Later you can add: "receiptImageUrl" to "..."
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
                val receipt = mapOf(
                    "amount" to amountDouble,
                    "storeName" to _uiState.value.storeName,
                    "glutenFreeItems" to _uiState.value.glutenFreeItems,
                    "uploadedToRevenue" to _uiState.value.uploadedToRevenue,
                    "date" to Timestamp(
                        _uiState.value.date
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                    ),
                    "createdAt" to Timestamp.now()
                )

                db.collection("users")
                    .document(uid)
                    .collection("receipts")
                    .add(receipt)
                    .await()

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        successMessage = "Receipt saved!",
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