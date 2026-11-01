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
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.ui.components.EmptyView
import com.maumela.magnummanagement.ui.components.ErrorView
import com.maumela.magnummanagement.ui.components.LoadingView
import com.maumela.magnummanagement.ui.components.ServiceCard
import com.maumela.magnummanagement.ui.components.StaleBanner
import com.maumela.magnummanagement.utils.UiState

/** The Services tab of the Marketplace. */
@Composable
fun ServicesTab(
    state: UiState<List<ServiceDto>>,
    isFiltered: Boolean,
    onRefresh: () -> Unit,
    onService: (Int) -> Unit,
    onRequest: (ServiceDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        UiState.Loading -> LoadingView(modifier, message = "Loading services...")
        is UiState.Failure -> ErrorView(state.error.message, modifier, onRetry = onRefresh)
        is UiState.Success -> Column(modifier) {
            if (state.isStale) StaleBanner(onRetry = onRefresh)
            if (state.data.isEmpty()) {
                EmptyView(
                    title = "No services found",
                    message = if (isFiltered) "Try a different search or clear your filters." else "There are no services yet.",
                    modifier = Modifier.weight(1f),
                )
            } else {
                PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.data, key = { it.id }) { service ->
                            ServiceCard(
                                service = service,
                                onClick = { onService(service.id) },
                                onRequest = { onRequest(service) },
                            )
                        }
                    }
                }
            }
        }
    }
}
