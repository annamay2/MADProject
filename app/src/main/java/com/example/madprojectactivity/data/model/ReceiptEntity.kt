// AI-generated (Claude): Changed imageUri from Uri? to String? for local image storage.
package com.example.madprojectactivity.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val imageUri: String? = null,
    val userId: String,
    val amount: Double,
    val storeName: String,
    val glutenFreeItems: String,
    val uploadedToRevenue: Boolean,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
