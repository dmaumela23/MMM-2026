package com.maumela.magnummanagement.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.ui.components.EmptyView
import com.maumela.magnummanagement.ui.components.ErrorView
import com.maumela.magnummanagement.ui.components.LoadingView
import com.maumela.magnummanagement.ui.components.MmmButton
import com.maumela.magnummanagement.ui.components.MmmButtonStyle
import com.maumela.magnummanagement.ui.components.MmmScaffold
import com.maumela.magnummanagement.ui.components.MmmTextField
import com.maumela.magnummanagement.ui.components.ProductCard
import com.maumela.magnummanagement.ui.components.RefreshOnResume
import com.maumela.magnummanagement.ui.components.SectionHeader
import com.maumela.magnummanagement.ui.components.SectorTile
import com.maumela.magnummanagement.ui.components.ServiceCard
import com.maumela.magnummanagement.ui.theme.Gold
import com.maumela.magnummanagement.ui.theme.Grey200Dark
import com.maumela.magnummanagement.ui.theme.Navy700
import com.maumela.magnummanagement.ui.theme.Navy900
import com.maumela.magnummanagement.utils.Formatters
import com.maumela.magnummanagement.utils.SectorRegistry
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.utils.addResultMessage
import com.maumela.magnummanagement.viewmodel.AddResult
import com.maumela.magnummanagement.viewmodel.HomeViewModel
import com.maumela.magnummanagement.viewmodel.MarketplaceTab
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenMarketplace: (MarketplaceTab?, String?) -> Unit,
    onOpenEnergy: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onProduct: (Int) -> Unit,
    onService: (Int) -> Unit,
    onAddProduct: (ProductDto) -> AddResult,
    onAddService: (ServiceDto) -> AddResult,
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    RefreshOnResume(viewModel::load)

    val featuredProducts = state.featuredProducts
    val featuredServices = state.featuredServices

    MmmScaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { HomeHeader(user) }

            item {
                MmmTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "Search products and services",
                    imeAction = ImeAction.Search,
                    onImeAction = { onOpenMarketplace(null, query) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            item { SectionHeader("Our sectors") }
            items(SectorRegistry.all, key = { it.sector.name }) { info ->
                SectorTile(
                    info = info,
                    onClick = {
                        when (info.sector) {
                            Sector.DIGITAL -> onOpenMarketplace(MarketplaceTab.PRODUCTS, null)
                            Sector.SERVICE -> onOpenMarketplace(MarketplaceTab.SERVICES, null)
                            Sector.ENERGY -> onOpenEnergy()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            item { SectionHeader("Quick actions") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MmmButton("Orders", onOpenOrders, style = MmmButtonStyle.Secondary, modifier = Modifier.weight(1f))
                    MmmButton("Account", onOpenProfile, style = MmmButtonStyle.Secondary, modifier = Modifier.weight(1f))
                    MmmButton("Settings", onOpenSettings, style = MmmButtonStyle.Secondary, modifier = Modifier.weight(1f))
                }
            }

            item { SectionHeader("Featured products") }
            item {
                when (featuredProducts) {
                    UiState.Loading -> LoadingView(Modifier.height(120.dp))
                    is UiState.Failure -> ErrorView(featuredProducts.error.message, Modifier.height(240.dp), onRetry = viewModel::load)
                    is UiState.Success -> if (featuredProducts.data.isEmpty()) {
                        EmptyView("No products yet", "Products will appear here.", Modifier.height(160.dp))
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(featuredProducts.data, key = { it.id }) { product ->
                                ProductCard(
                                    product = product,
                                    onClick = { onProduct(product.id) },
                                    onAddToOrder = {
                                        val result = onAddProduct(product)
                                        scope.launch { snackbar.showSnackbar(addResultMessage(result, product.name)) }
                                    },
                                    modifier = Modifier.width(300.dp),
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Featured services") }
            item {
                when (featuredServices) {
                    UiState.Loading -> LoadingView(Modifier.height(120.dp))
                    is UiState.Failure -> ErrorView(featuredServices.error.message, Modifier.height(240.dp), onRetry = viewModel::load)
                    is UiState.Success -> if (featuredServices.data.isEmpty()) {
                        EmptyView("No services yet", "Services will appear here.", Modifier.height(160.dp))
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(featuredServices.data, key = { it.id }) { service ->
                                ServiceCard(
                                    service = service,
                                    onClick = { onService(service.id) },
                                    onRequest = {
                                        val result = onAddService(service)
                                        scope.launch { snackbar.showSnackbar(addResultMessage(result, service.name)) }
                                    },
                                    modifier = Modifier.width(300.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(user: UserDto?) {
    val firstName = user?.fullName?.trim()?.split(" ")?.firstOrNull().orEmpty()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Navy900, Navy700)))
            .statusBarsPadding()
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("MMM", style = MaterialTheme.typography.displayMedium, color = Gold)
            Text(
                if (firstName.isEmpty()) "Welcome to MMM" else "Welcome back, $firstName",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Text(
                if (user != null) Formatters.roleLabel(user.role.name) + " account" else "Maumela Magnum Management",
                style = MaterialTheme.typography.bodyMedium,
                color = Grey200Dark,
            )
        }
    }
}
