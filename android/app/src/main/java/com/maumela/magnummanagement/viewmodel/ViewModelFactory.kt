package com.maumela.magnummanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maumela.magnummanagement.AppContainer

/** Manual ViewModel factory used by the Compose screens. Detail ids are supplied by the route. */
class ViewModelFactory(
    private val container: AppContainer,
    private val productId: Int? = null,
    private val serviceId: Int? = null,
    private val orderId: Int? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(SplashViewModel::class.java) -> SplashViewModel(container.authRepository)
        modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(container.authRepository)
        modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(container.authRepository, container.productRepository, container.serviceRepository)
        modelClass.isAssignableFrom(MarketplaceViewModel::class.java) -> MarketplaceViewModel(container.productRepository, container.serviceRepository)
        modelClass.isAssignableFrom(OrderDraftViewModel::class.java) -> OrderDraftViewModel(container.orderRepository)
        modelClass.isAssignableFrom(OrdersViewModel::class.java) -> OrdersViewModel(container.authRepository, container.orderRepository)
        modelClass.isAssignableFrom(OrderDetailViewModel::class.java) -> OrderDetailViewModel(container.orderRepository, requireNotNull(orderId) { "OrderDetailViewModel requires orderId" })
        modelClass.isAssignableFrom(ProductDetailViewModel::class.java) -> ProductDetailViewModel(container.productRepository, productId)
        modelClass.isAssignableFrom(ServiceDetailViewModel::class.java) -> ServiceDetailViewModel(container.serviceRepository, serviceId)
        modelClass.isAssignableFrom(EnergyViewModel::class.java) -> EnergyViewModel(container.energyRepository)
        modelClass.isAssignableFrom(ProfileOverviewViewModel::class.java) -> ProfileOverviewViewModel(container.authRepository, container.orderRepository, container.productRepository, container.serviceRepository)
        modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(container.authRepository)
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container.settingsRepository, container.preferencesStore)
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    } as T
}
