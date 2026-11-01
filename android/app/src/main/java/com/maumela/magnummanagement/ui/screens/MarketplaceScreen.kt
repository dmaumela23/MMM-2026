@file:OptIn(ExperimentalMaterial3Api::class)

package com.maumela.magnummanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.ui.components.MarketplaceFilterSheet
import com.maumela.magnummanagement.ui.components.MmmButton
import com.maumela.magnummanagement.ui.components.MmmButtonStyle
import com.maumela.magnummanagement.ui.components.MmmScaffold
import com.maumela.magnummanagement.ui.components.MmmTextField
import com.maumela.magnummanagement.ui.components.MmmTopBar
import com.maumela.magnummanagement.ui.components.RefreshOnResume
import com.maumela.magnummanagement.utils.addResultMessage
import com.maumela.magnummanagement.viewmodel.AddResult
import com.maumela.magnummanagement.viewmodel.MarketplaceTab
import com.maumela.magnummanagement.viewmodel.MarketplaceViewModel
import kotlinx.coroutines.launch

/**
 * Browse both catalogues. Search and filters are sent to the server, which filters with SQL.
 * [tabArg] and [queryArg] come from the route (Home tiles and the Home search bar).
 */
@Composable
fun MarketplaceScreen(
    viewModel: MarketplaceViewModel,
    tabArg: String?,
    queryArg: String?,
    draftCount: Int,
    onProduct: (Int) -> Unit,
    onService: (Int) -> Unit,
    onAddProduct: (ProductDto) -> AddResult,
    onAddService: (ServiceDto) -> AddResult,
    onOpenOrders: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Plain "marketplace" (bottom bar) keeps the user's current search; arguments set it.
    LaunchedEffect(tabArg, queryArg) {
        if (tabArg != null || queryArg != null) {
            viewModel.openWith(MarketplaceTab.fromArg(tabArg), queryArg)
        } else {
            viewModel.ensureLoaded()
        }
    }
    RefreshOnResume(viewModel::refresh)

    val filters = state.filters
    val isFiltered = filters.activeFilterCount > 0 || filters.query.isNotBlank()
    val categories = if (state.tab == MarketplaceTab.PRODUCTS) state.productCategories else state.serviceCategories
    val selectedCategory = if (state.tab == MarketplaceTab.PRODUCTS) filters.productCategory else filters.serviceCategory

    MmmScaffold(
        topBar = {
            MmmTopBar(
                title = "Marketplace",
                actions = {
                    IconButton(onClick = onOpenOrders) {
                        BadgedBox(badge = { if (draftCount > 0) Badge { Text("$draftCount") } }) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Your order, $draftCount items")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MmmTextField(
                value = filters.query,
                onValueChange = viewModel::setQuery,
                label = "Search products and services",
                imeAction = ImeAction.Search,
                onImeAction = viewModel::submitSearch,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MmmButton("Search", onClick = viewModel::submitSearch)
                MmmButton(
                    text = if (filters.activeFilterCount > 0) "Filters (${filters.activeFilterCount})" else "Filters",
                    onClick = { showFilters = true },
                    style = MmmButtonStyle.Secondary,
                )
                if (isFiltered) {
                    TextButton(onClick = { viewModel.clearSearch(); viewModel.clearFilters() }) { Text("Clear all") }
                }
            }

            PrimaryTabRow(selectedTabIndex = state.tab.ordinal) {
                MarketplaceTab.entries.forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = { Text(if (tab == MarketplaceTab.PRODUCTS) "Products" else "Services") },
                    )
                }
            }

            if (categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { setCategory(viewModel, state.tab, null) },
                            label = { Text("All") },
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory.equals(category, ignoreCase = true),
                            onClick = { setCategory(viewModel, state.tab, category) },
                            label = { Text(category) },
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (state.tab) {
                    MarketplaceTab.PRODUCTS -> ProductsTab(
                        state = state.products,
                        isFiltered = isFiltered,
                        onRefresh = viewModel::refresh,
                        onProduct = onProduct,
                        onAddToOrder = { product ->
                            val result = onAddProduct(product)
                            scope.launch { snackbar.showSnackbar(addResultMessage(result, product.name)) }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    MarketplaceTab.SERVICES -> ServicesTab(
                        state = state.services,
                        isFiltered = isFiltered,
                        onRefresh = viewModel::refresh,
                        onService = onService,
                        onRequest = { service ->
                            val result = onAddService(service)
                            scope.launch { snackbar.showSnackbar(addResultMessage(result, service.name)) }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    if (showFilters) {
        MarketplaceFilterSheet(
            filters = filters,
            errorMessage = state.filterError,
            onApply = viewModel::applyFilters,
            onReset = viewModel::clearFilters,
            onDismiss = {
                showFilters = false
                viewModel.clearFilterError()
            },
        )
    }
}

private fun setCategory(viewModel: MarketplaceViewModel, tab: MarketplaceTab, category: String?) {
    if (tab == MarketplaceTab.PRODUCTS) viewModel.setProductCategory(category) else viewModel.setServiceCategory(category)
}
