package com.maumela.magnummanagement

import android.app.Application
import android.content.Context
import androidx.work.*
import com.maumela.magnummanagement.data.local.AppDatabase
import com.maumela.magnummanagement.data.local.PreferencesStore
import com.maumela.magnummanagement.data.api.ApiClient
import com.maumela.magnummanagement.data.repository.OrderRepository
import com.maumela.magnummanagement.data.repository.ProductRepository
import com.maumela.magnummanagement.data.repository.ServiceRepository
import java.util.concurrent.TimeUnit

class MmmApplication : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val preferences by lazy { PreferencesStore(this) }
    val apiService by lazy { ApiClient.create(preferences) }

    val productRepository by lazy { ProductRepository(apiService, database.productDao()) }
    val serviceRepository by lazy { ServiceRepository(apiService, database.serviceDao()) }
    val orderRepository by lazy { OrderRepository(apiService, database.orderDao()) }

    override fun onCreate() {
        super.onCreate()
        setupPeriodicSync()
    }

    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as MmmApplication
            app.productRepository.syncPendingProducts()
            app.serviceRepository.syncPendingServices()
            app.orderRepository.syncPendingOrders()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
