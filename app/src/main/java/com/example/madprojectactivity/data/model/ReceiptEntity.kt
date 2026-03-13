package com.example.madprojectactivity.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val amount: Double,
    val storeName: String,
    val glutenFreeItems: String,
    val uploadedToRevenue: Boolean,
    val date: Long, // Store as timestamp
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)