package com.example.madprojectactivity.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.madprojectactivity.data.repository.ReceiptRepository

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = ReceiptRepository(applicationContext)
        val unsyncedReceipts = repository.getUnsyncedReceipts()

        if (unsyncedReceipts.isEmpty()) {
            return Result.success()
        }

        var hasError = false

        for (receipt in unsyncedReceipts) {
            try {
                repository.syncReceiptToFirestore(receipt)
            } catch (e: Exception) {
                hasError = true
            }
        }

        return if (hasError) Result.retry() else Result.success()
    }
}
