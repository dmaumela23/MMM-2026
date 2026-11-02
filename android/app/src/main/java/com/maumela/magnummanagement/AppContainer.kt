package com.maumela.magnummanagement

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.api.RetrofitClient
import com.maumela.magnummanagement.data.api.SessionEvents
import com.maumela.magnummanagement.data.local.KeystoreTokenCipher
import com.maumela.magnummanagement.data.local.MmmDatabase
import com.maumela.magnummanagement.data.local.PreferencesStore
import com.maumela.magnummanagement.data.local.TokenStore
import com.maumela.magnummanagement.data.repository.AuthRepository
import com.maumela.magnummanagement.data.repository.EnergyRepository
import com.maumela.magnummanagement.data.repository.OrderRepository
import com.maumela.magnummanagement.data.repository.ProductRepository
import com.maumela.magnummanagement.data.repository.ServiceRepository
import com.maumela.magnummanagement.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// DataStore instances must be declared once, at file level.
private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = "mmm_secure")
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "mmm_settings")

/**
 * Manual dependency injection: builds every long-lived object once and wires them together.
 * (Hilt would be overkill for a prototype.)
 *
 *   ViewModel -> Repository -> ApiService (Retrofit) -> FastAPI -> PostgreSQL
 *                   |
 *                   +-> Room (read-only cache) / TokenStore / PreferencesStore
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val sessionEvents = SessionEvents()
    val tokenStore = TokenStore(appContext.secureDataStore, KeystoreTokenCipher())
    val preferencesStore = PreferencesStore(appContext.settingsDataStore)

    private val database: MmmDatabase by lazy { MmmDatabase.create(appContext) }

    private val api: ApiService = RetrofitClient(
        baseUrl = BuildConfig.MMM_BASE_URL,
        tokenProvider = tokenStore::currentToken,
        sessionEvents = sessionEvents,
    ).api

    val authRepository = AuthRepository(api, tokenStore, preferencesStore) {
        // Room must not run on the main thread.
        withContext(Dispatchers.IO) { database.clearAllTables() }
    }
    val productRepository by lazy { ProductRepository(api, database.productDao()) }
    val serviceRepository by lazy { ServiceRepository(api, database.serviceDao()) }
    val orderRepository by lazy { OrderRepository(api, database.orderDao()) }
    val energyRepository = EnergyRepository(api)
    val settingsRepository = SettingsRepository(api, preferencesStore)
}