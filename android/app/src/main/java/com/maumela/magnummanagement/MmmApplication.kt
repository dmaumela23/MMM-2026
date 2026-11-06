package com.maumela.magnummanagement

import android.app.Application
import android.content.Context
import androidx.work.*
import com.maumela.magnummanagement.notifications.NotificationChannels
import java.util.concurrent.TimeUnit

class MmmApplication : Application() {

    lateinit var container: AppContainer
        private set

    val productRepository by lazy { container.productRepository }
    val serviceRepository by lazy { container.serviceRepository }
    val orderRepository by lazy { container.orderRepository }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.create(this)
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
