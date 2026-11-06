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
import com.maumela.magnummanagement.notifications.NotificationChannels
import java.util.concurrent.TimeUnit

/** Container to satisfy legacy references in MainActivity and MmmMessagingService */
class AppContainer(context: Context) {
    val database = AppDatabase.getInstance(context)
    val preferences = PreferencesStore(context)
    val apiService = ApiClient.create(preferences)

    val productRepository = ProductRepository(apiService, database.productDao())
    val serviceRepository = ServiceRepository(apiService, database.serviceDao())
    val orderRepository = OrderRepository(apiService, database.orderDao())
}

class MmmApplication : Application() {

    lateinit var container: AppContainer
        private set

    val database by lazy { container.database }
    val preferences by lazy { container.preferences }
    val apiService by lazy { container.apiService }

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
