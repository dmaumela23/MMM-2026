package com.maumela.magnummanagement.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maumela.magnummanagement.MmmApplication

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Safely retrieve the MmmApplication instance and its AppContainer
        val app = applicationContext as? MmmApplication ?: return Result.failure()
        val container = app.container

        return try {
            // Execute background synchronization for pending local mutations
            container.orderRepository.syncPendingOrders()
            container.productRepository.syncPendingProducts()
            container.serviceRepository.syncPendingServices()

            Result.success()
        } catch (e: Exception) {
            // If network drops or an error occurs, WorkManager will retry later based on constraints
            Result.retry()
        }
    }
}