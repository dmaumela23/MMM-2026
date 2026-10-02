@file:OptIn(ExperimentalMaterial3Api::class)

package com.maumela.magnummanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.ui.components.EmptyView
import com.maumela.magnummanagement.ui.components.ErrorView
import com.maumela.magnummanagement.ui.components.LoadingView
import com.maumela.magnummanagement.ui.components.ProductCard
import com.maumela.magnummanagement.ui.components.StaleBanner
import com.maumela.magnummanagement.utils.UiState

/** The Products tab of the Marketplace: loading, error, empty and list states. */
@Composable
fun ProductsTab(
    state: UiState<List<ProductDto>>,
    isFiltered: Boolean,
    onRefresh: () -> Unit,
    onProduct: (Int) -> Unit,
    onAddToOrder: (ProductDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        UiState.Loading -> LoadingView(modifier, message = "Loading products...")
        is UiState.Failure -> ErrorView(state.error.message, modifier, onRetry = onRefresh)
        is UiState.Success -> Column(modifier) {
            if (state.isStale) StaleBanner(onRetry = onRefresh)
            if (state.data.isEmpty()) {
                EmptyView(
                    title = "No products found",
                    message = if (isFiltered) "Try a different search or clear your filters." else "There are no products yet.",
                    modifier = Modifier.weight(1f),
                )
            } else {
                PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.data, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                onClick = { onProduct(product.id) },
                                onAddToOrder = { onAddToOrder(product) },
                            )
                        }
                    }
                }
            }
        }
    }
}
