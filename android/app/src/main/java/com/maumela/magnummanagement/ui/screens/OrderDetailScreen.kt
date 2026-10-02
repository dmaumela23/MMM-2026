@file:OptIn(ExperimentalMaterial3Api::class)

package com.maumela.magnummanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.ui.components.ErrorView
import com.maumela.magnummanagement.ui.components.LoadingView
import com.maumela.magnummanagement.ui.components.MmmButton
import com.maumela.magnummanagement.ui.components.MmmButtonStyle
import com.maumela.magnummanagement.ui.components.MmmScaffold
import com.maumela.magnummanagement.ui.components.MmmTopBar
import com.maumela.magnummanagement.ui.components.OrderItemRow
import com.maumela.magnummanagement.ui.components.OrderStatusChip
import com.maumela.magnummanagement.utils.Formatters
import com.maumela.magnummanagement.utils.OrderRules
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.viewmodel.OrderDetailViewModel

/**
 * One order. The customer can cancel it while every line is still pending. The seller or provider of a
 * line can move it forward (Accept, Start work, Mark complete) or cancel it. The server checks every change.
 */
@Composable
fun OrderDetailScreen(
    viewModel: OrderDetailViewModel,
    user: UserDto?,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showCancel by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage
        if (message != null && state.order is UiState.Success) {
            viewModel.clearError()
            snackbar.showSnackbar(message)
        }
    }

    val orderState = state.order

    MmmScaffold(
        topBar = { MmmTopBar(title = "Order details", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (orderState) {
            UiState.Loading -> LoadingView(Modifier.padding(padding))
            is UiState.Failure -> ErrorView(orderState.error.message, Modifier.padding(padding), onRetry = viewModel::load)
            is UiState.Success -> {
                val order = orderState.data
                val canCancel = user != null && OrderRules.canCustomerCancel(order, user.id)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Order #${order.id}", style = MaterialTheme.typography.headlineMedium)
                        OrderStatusChip(order.status)
                    }
                    Text(
                        "Placed ${Formatters.dateTime(order.createdAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            order.items.forEachIndexed { index, item ->
                                if (index > 0) HorizontalDivider()
                                OrderItemRow(item = item) {
                                    if (OrderRules.isSellerOfItem(user, item)) {
                                        val actions = OrderRules.actionsFor(item.itemStatus)
                                        if (actions.isNotEmpty()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                actions.forEach { action ->
                                                    MmmButton(
                                                        text = action.label,
                                                        onClick = { viewModel.updateItemStatus(item.id, action.target) },
                                                        style = if (action.destructive) MmmButtonStyle.Danger else MmmButtonStyle.Secondary,
                                                        enabled = !state.isWorking,
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Total", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    Formatters.money(order.totalAmount),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    if (!order.notes.isNullOrBlank()) {
                        Text("Notes", style = MaterialTheme.typography.titleMedium)
                        Text(order.notes, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (canCancel) {
                        MmmButton(
                            text = "Cancel order",
                            onClick = { showCancel = true },
                            style = MmmButtonStyle.Danger,
                            loading = state.isWorking,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        "No payment is taken in this prototype: orders are recorded only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (showCancel) {
                    AlertDialog(
                        onDismissRequest = { showCancel = false },
                        title = { Text("Cancel this order?") },
                        text = { Text("Every item will be cancelled and any reserved stock released.") },
                        confirmButton = {
                            TextButton(onClick = { showCancel = false; viewModel.cancelOrder() }) { Text("Cancel order") }
                        },
                        dismissButton = { TextButton(onClick = { showCancel = false }) { Text("Keep order") } },
                    )
                }
            }
        }
    }
}
