package com.example.madprojectactivity.screens.receipts

import android.net.Uri
import java.time.LocalDate

data class UploadReceiptUiState(
    val amount: String = "",
    val storeName: String = "",
    val glutenFreeItems: String = "",
    val uploadedToRevenue: Boolean = false,
    val date: LocalDate = LocalDate.now(),
    val celiacAmountText: String = "",
    val imageUri: Uri? = null,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)