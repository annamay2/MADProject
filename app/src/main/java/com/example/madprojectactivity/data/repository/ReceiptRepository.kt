package com.example.madprojectactivity.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.madprojectactivity.data.local.AppDatabase
import com.example.madprojectactivity.data.local.ReceiptDao
import com.example.madprojectactivity.data.model.ReceiptEntity
import com.example.madprojectactivity.data.worker.SyncWorker
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class ReceiptRepository(private val context: Context) {

    private val receiptDao: ReceiptDao = AppDatabase.getDatabase(context).receiptDao()
    private val userRepository = UserRepository(context)
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val workManager: WorkManager = WorkManager.getInstance(context)
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val currentUserId: String
        get() = userRepository.currentUserId
            ?: throw IllegalStateException("User not logged in")

    suspend fun getReceipt(receiptId: String): ReceiptEntity? {
        // Try local Room database first
        val local = receiptDao.getReceiptById(receiptId)
        if (local != null) return local

        // Fall back to Firestore if not found locally
        val userId = currentUserId
        return fetchReceiptFromFirestore(userId, receiptId)?.also { entity ->
            // stores the Firestore result locally
            receiptDao.insertReceipt(entity)
        }
    }

    // ── Local (Room) operations ──

    fun getReceiptsForUser(): Flow<List<ReceiptEntity>> {
        return receiptDao.getAllReceipts(currentUserId)
    }

    suspend fun insertReceipt(receipt: ReceiptEntity) {
        receiptDao.insertReceipt(receipt)
        scheduleSyncWorker()
    }

    suspend fun getUnsyncedReceipts(): List<ReceiptEntity> {
        return receiptDao.getUnsyncedReceipts()
    }

    suspend fun markAsSynced(receiptId: String) {
        receiptDao.markAsSynced(receiptId)
    }

    suspend fun updateReceipt(receiptId: String, fields: Map<String, Any>) {
        val userId = currentUserId
        // 1. Update local entity
        val existing = receiptDao.getReceiptById(receiptId) ?: return
        val updated = existing.copy(
            amount = fields["amount"] as? Double ?: existing.amount,
            storeName = fields["storeName"] as? String ?: existing.storeName,
            uploadedToRevenue = fields["uploadedToRevenue"] as? Boolean ?: existing.uploadedToRevenue,
            isSynced = false
        )
        receiptDao.insertReceipt(updated)

        // 2. Try immediate Firestore update then fall back to background sync
        try {
            updateReceiptInFirestore(userId, receiptId, fields)
            receiptDao.markAsSynced(receiptId)
        } catch (_: Exception) {
            scheduleSyncWorker()
        }
    }

    suspend fun deleteReceipt(receiptId: String) {
        val userId = currentUserId
        receiptDao.deleteById(receiptId)

        try {
            deleteReceiptFromFirestore(userId, receiptId)
        } catch (_: Exception) {
            // Remote deletion failed; local is already removed
        }
    }

    private suspend fun fetchReceiptFromFirestore(userId: String, receiptId: String): ReceiptEntity? {
        val doc = firestore.collection("users")
            .document(userId)
            .collection("receipts")
            .document(receiptId)
            .get()
            .await()

        if (!doc.exists()) return null

        return ReceiptEntity(
            id = doc.id,
            userId = userId,
            amount = doc.getDouble("amount") ?: 0.0,
            storeName = doc.getString("storeName") ?: "",
            glutenFreeItems = doc.getString("glutenFreeItems") ?: "",
            uploadedToRevenue = doc.getBoolean("uploadedToRevenue") ?: false,
            date = doc.getTimestamp("date")?.toDate()?.time ?: 0L,
            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
            isSynced = true
        )
    }

    private suspend fun updateReceiptInFirestore(userId: String, receiptId: String, fields: Map<String, Any>) {
        firestore.collection("users")
            .document(userId)
            .collection("receipts")
            .document(receiptId)
            .update(fields)
            .await()
    }

    private suspend fun deleteReceiptFromFirestore(userId: String, receiptId: String) {
        firestore.collection("users")
            .document(userId)
            .collection("receipts")
            .document(receiptId)
            .delete()
            .await()
    }

    suspend fun syncReceiptToFirestore(receipt: ReceiptEntity) {
        val receiptMap = mutableMapOf<String, Any>(
            "amount" to receipt.amount,
            "storeName" to receipt.storeName,
            "glutenFreeItems" to receipt.glutenFreeItems,
            "uploadedToRevenue" to receipt.uploadedToRevenue,
            "date" to Timestamp(Date(receipt.date)),
            "createdAt" to Timestamp(Date(receipt.createdAt))
        )

        firestore.collection("users")
            .document(receipt.userId)
            .collection("receipts")
            .document(receipt.id)
            .set(receiptMap)
            .await()

        receiptDao.markAsSynced(receipt.id)
    }

    // Firestore snapshot listener for real-time sync

    fun addRemoteListener(): ListenerRegistration {
        val userId = currentUserId
        return firestore.collection("users")
            .document(userId)
            .collection("receipts")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                repositoryScope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                val existing = receiptDao.getReceiptById(doc.id)
                                val entity = ReceiptEntity(
                                    id = doc.id,
                                    userId = userId,
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    storeName = doc.getString("storeName") ?: "",
                                    glutenFreeItems = doc.getString("glutenFreeItems") ?: "",
                                    uploadedToRevenue = doc.getBoolean("uploadedToRevenue") ?: false,
                                    date = doc.getTimestamp("date")?.toDate()?.time ?: 0L,
                                    createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
                                        ?: System.currentTimeMillis(),
                                    isSynced = true,
                                    imageUri = existing?.imageUri
                                )
                                receiptDao.insertReceipt(entity)
                            }
                            DocumentChange.Type.REMOVED -> {
                                receiptDao.deleteById(doc.id)
                            }
                        }
                    }
                }
            }
    }

    // WorkManager scheduling

    fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "receipt_sync_work",
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }
}
