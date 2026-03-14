// AI-generated (Claude): Added updateLocalImageUri query for setting local path after
// downloading images from Firebase Storage.
package com.example.madprojectactivity.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.madprojectactivity.data.model.ReceiptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity)

    @Query("SELECT * FROM receipts WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllReceipts(userId: String): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getReceiptById(id: String): ReceiptEntity?

    @Query("SELECT * FROM receipts WHERE isSynced = 0")
    suspend fun getUnsyncedReceipts(): List<ReceiptEntity>

    @Query("UPDATE receipts SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE receipts SET imageUri = :localUri WHERE id = :id")
    suspend fun updateLocalImageUri(id: String, localUri: String)
}
