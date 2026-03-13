package com.example.madprojectactivity.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.madprojectactivity.data.local.AppDatabase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
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

        val unsyncedReceipts = dao.getUnsyncedReceipts()

        if (unsyncedReceipts.isEmpty()) {
            return Result.success()
        }

        var hasError = false

        for (receipt in unsyncedReceipts) {
            try {
                val receiptMap = mapOf(
                    "amount" to receipt.amount,
                    "storeName" to receipt.storeName,
                    "glutenFreeItems" to receipt.glutenFreeItems,
                    "uploadedToRevenue" to receipt.uploadedToRevenue,
                    "date" to Timestamp(Date(receipt.date)),
                    "createdAt" to Timestamp(Date(receipt.createdAt))
                )

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
