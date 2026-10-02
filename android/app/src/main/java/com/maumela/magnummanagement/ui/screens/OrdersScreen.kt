@file:OptIn(ExperimentalMaterial3Api::class)

package com.maumela.magnummanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.data.model.IncomingItemDto
import com.maumela.magnummanagement.data.model.OrderSummaryDto
import com.maumela.magnummanagement.ui.components.EmptyView
import com.maumela.magnummanagement.ui.components.ErrorView
import com.maumela.magnummanagement.ui.components.IncomingItemCard
import com.maumela.magnummanagement.ui.components.LoadingView
import com.maumela.magnummanagement.ui.components.MessageCard
import com.maumela.magnummanagement.ui.components.MmmButton
import com.maumela.magnummanagement.ui.components.MmmButtonStyle
import com.maumela.magnummanagement.ui.components.MmmScaffold
import com.maumela.magnummanagement.ui.components.MmmTextField
import com.maumela.magnummanagement.ui.components.MmmTopBar
import com.maumela.magnummanagement.ui.components.OrderSummaryCard
import com.maumela.magnummanagement.ui.components.RefreshOnResume
import com.maumela.magnummanagement.ui.components.StaleBanner
import com.maumela.magnummanagement.utils.Formatters
import com.maumela.magnummanagement.utils.Permissions
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.viewmodel.DraftItem
import com.maumela.magnummanagement.viewmodel.DraftUiState
import com.maumela.magnummanagement.viewmodel.OrderDraftViewModel
import com.maumela.magnummanagement.viewmodel.OrdersUiState
import com.maumela.magnummanagement.viewmodel.OrdersViewModel
import com.maumela.magnummanagement.data.model.ItemType

/**
 * Orders: the draft order being built (checkout), my placed orders, and, for Provider and
 * Business accounts, the incoming requests for their own products and services.
 */
@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel,
    draftViewModel: OrderDraftViewModel,
    onOrder: (Int) -> Unit,
    onBrowse: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by draftViewModel.state.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    val isSeller = Permissions.isSeller(user?.role)
    var tab by rememberSaveable { mutableStateOf(0) }

    RefreshOnResume(viewModel::refresh)

    // After a successful submit: forget the draft and open the new order.
    LaunchedEffect(draft.createdOrderId) {
        val id = draft.createdOrderId
        if (id != null) {
            draftViewModel.onOrderCreatedHandled()
            viewModel.refresh()
            onOrder(id)
        }
    }

    MmmScaffold(topBar = { MmmTopBar(title = "Orders") }) { padding ->
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    DraftSection(
                        draft = draft,
                        onQuantity = draftViewModel::setQuantity,
                        onRemove = draftViewModel::remove,
                        onNotes = draftViewModel::setNotes,
                        onSubmit = draftViewModel::submit,
                        onBrowse = onBrowse,
                    )
                }
                if (isSeller) {
                    item {
                        PrimaryTabRow(selectedTabIndex = tab) {
                            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("My orders") })
                            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Incoming") })
                        }
                    }
                }
                if (!isSeller || tab == 0) {
                    myOrdersItems(state, viewModel::refresh, onOrder, onBrowse)
                } else {
                    incomingItems(state, viewModel::refresh, onOrder)
                }
            }
        }
    }
}

private fun LazyListScope.myOrdersItems(
    state: OrdersUiState,
    onRetry: () -> Unit,
    onOrder: (Int) -> Unit,
    onBrowse: () -> Unit,
) {
    val orders = state.orders
    when (orders) {
        UiState.Loading -> item { LoadingView(Modifier.height(160.dp)) }
        is UiState.Failure -> item { ErrorView(orders.error.message, Modifier.height(260.dp), onRetry = onRetry) }
        is UiState.Success -> {
            if (orders.isStale) item { StaleBanner(onRetry = onRetry) }
            if (orders.data.isEmpty()) {
                item {
                    EmptyView(
                        title = "No orders yet",
                        message = "Orders you place will appear here.",
                        modifier = Modifier.height(220.dp),
                        actionLabel = "Browse marketplace",
                        onAction = onBrowse,
                    )
                }
            } else {
                items(orders.data, key = { "order-${it.id}" }) { order: OrderSummaryDto ->
                    OrderSummaryCard(order = order, onClick = { onOrder(order.id) })
                }
            }
        }
    }
}

private fun LazyListScope.incomingItems(
    state: OrdersUiState,
    onRetry: () -> Unit,
    onOrder: (Int) -> Unit,
) {
    val incoming = state.incoming
    when (incoming) {
        UiState.Loading -> item { LoadingView(Modifier.height(160.dp)) }
        is UiState.Failure -> item { ErrorView(incoming.error.message, Modifier.height(260.dp), onRetry = onRetry) }
        is UiState.Success -> {
            if (incoming.data.isEmpty()) {
                item {
                    EmptyView(
                        title = "No incoming requests",
                        message = "Requests for your products and services will appear here.",
                        modifier = Modifier.height(220.dp),
                    )
                }
            } else {
                items(incoming.data, key = { "incoming-${it.itemId}" }) { item: IncomingItemDto ->
                    IncomingItemCard(item = item, onClick = { onOrder(item.orderId) })
                }
            }
        }
    }
}

@Composable
private fun DraftSection(
    draft: DraftUiState,
    onQuantity: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onNotes: (String) -> Unit,
    onSubmit: () -> Unit,
    onBrowse: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your order", style = MaterialTheme.typography.titleLarge)

            if (draft.isEmpty) {
                Text(
                    "Your order is empty. Add products or services from the marketplace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MmmButton("Browse marketplace", onBrowse, style = MmmButtonStyle.Secondary)
            } else {
                draft.items.forEach { item ->
                    DraftItemRow(item, onQuantity = { onQuantity(item.key, it) }, onRemove = { onRemove(item.key) })
                    HorizontalDivider()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Estimated total", style = MaterialTheme.typography.titleMedium)
                    Text(Formatters.money(draft.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text(
                    "The final price is calculated by the server when you submit. Payments are not part of this prototype.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MmmTextField(
                    value = draft.notes,
                    onValueChange = onNotes,
                    label = "Notes for the seller or provider (optional)",
                    error = draft.notesError,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                val message = draft.errorMessage
                if (message != null) MessageCard(message, isError = true, modifier = Modifier.fillMaxWidth())
                MmmButton(
                    text = "Submit order",
                    onClick = onSubmit,
                    loading = draft.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DraftItemRow(item: DraftItem, onQuantity: (Int) -> Unit, onRemove: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(item.name, style = MaterialTheme.typography.titleMedium)
        Text(
            (if (item.itemType == ItemType.PRODUCT) "Sold by " else "Provided by ") + item.sellerName +
                " · " + Formatters.money(item.unitPrice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.itemType == ItemType.PRODUCT) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onQuantity(item.quantity - 1) },
                        enabled = item.quantity > 1,
                        modifier = Modifier.semantics { contentDescription = "Decrease quantity of ${item.name}" },
                    ) { Text("-") }
                    Text("${item.quantity}", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = { onQuantity(item.quantity + 1) },
                        enabled = item.quantity < item.maxQuantity,
                        modifier = Modifier.semantics { contentDescription = "Increase quantity of ${item.name}" },
                    ) { Text("+") }
                }
            } else {
                Text("Quantity: 1", style = MaterialTheme.typography.bodyMedium)
            }
            Text(Formatters.money(item.lineTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}
