// AI-generated (Claude): Added Firebase Storage image upload before Firestore sync.
// Uploads receipt photo to receipts/{userId}/{receiptId}.jpg and stores download URL in Firestore.
package com.example.madprojectactivity.data.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.madprojectactivity.data.local.AppDatabase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Date

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.receiptDao()
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        val unsyncedReceipts = dao.getUnsyncedReceipts()

        if (unsyncedReceipts.isEmpty()) {
            return Result.success()
        }

        var hasError = false

        for (receipt in unsyncedReceipts) {
            try {
                var imageDownloadUrl: String? = null

                // Upload image to Firebase Storage if present
                if (receipt.imageUri != null) {
                    val storageRef = storage.reference
                        .child("receipts/${receipt.userId}/${receipt.id}.jpg")
                    storageRef.putFile(Uri.parse(receipt.imageUri)).await()
                    imageDownloadUrl = storageRef.downloadUrl.await().toString()
                }

                val receiptMap = mutableMapOf<String, Any>(
                    "amount" to receipt.amount,
                    "storeName" to receipt.storeName,
                    "glutenFreeItems" to receipt.glutenFreeItems,
                    "uploadedToRevenue" to receipt.uploadedToRevenue,
                    "date" to Timestamp(Date(receipt.date)),
                    "createdAt" to Timestamp(Date(receipt.createdAt))
                )

                if (imageDownloadUrl != null) {
                    receiptMap["imageUrl"] = imageDownloadUrl
                }

                // Use the local UUID as the Firestore document ID
                firestore.collection("users")
                    .document(receipt.userId)
                    .collection("receipts")
                    .document(receipt.id)
                    .set(receiptMap)
                    .await()

                dao.markAsSynced(receipt.id)
            } catch (e: Exception) {
                hasError = true
            }
        }

        return if (hasError) Result.retry() else Result.success()
    }
}
