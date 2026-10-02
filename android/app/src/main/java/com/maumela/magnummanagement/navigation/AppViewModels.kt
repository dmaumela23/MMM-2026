package com.maumela.magnummanagement.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maumela.magnummanagement.AppContainer
import com.maumela.magnummanagement.viewmodel.ViewModelFactory

/**
 * Creates (or finds) a ViewModel for the current screen. Detail screens pass the id from the route.
 * Each screen entry owns its ViewModel, which is cleared when the screen is popped.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    container: AppContainer,
    productId: Int? = null,
    serviceId: Int? = null,
    orderId: Int? = null,
): VM = viewModel<VM>(factory = ViewModelFactory(container, productId, serviceId, orderId))
