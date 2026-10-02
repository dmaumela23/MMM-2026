@file:OptIn(ExperimentalMaterial3Api::class)

package com.maumela.magnummanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.ui.components.ErrorView
import com.maumela.magnummanagement.ui.components.ListingEditorSheet
import com.maumela.magnummanagement.ui.components.LoadingView
import com.maumela.magnummanagement.ui.components.MmmButton
import com.maumela.magnummanagement.ui.components.MmmButtonStyle
import com.maumela.magnummanagement.ui.components.MmmScaffold
import com.maumela.magnummanagement.ui.components.MmmTopBar
import com.maumela.magnummanagement.ui.components.RatingBar
import com.maumela.magnummanagement.ui.components.StaleBanner
import com.maumela.magnummanagement.ui.components.StatusChip
import com.maumela.magnummanagement.utils.DisplayRules
import com.maumela.magnummanagement.utils.Formatters
import com.maumela.magnummanagement.utils.ListingKind
import com.maumela.magnummanagement.utils.Permissions
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.utils.addResultMessage
import com.maumela.magnummanagement.utils.toFormValues
import com.maumela.magnummanagement.utils.toUpdateRequest
import com.maumela.magnummanagement.viewmodel.AddResult
import com.maumela.magnummanagement.viewmodel.ServiceDetailViewModel
import kotlinx.coroutines.launch

@Composable
fun ServiceDetailScreen(
    viewModel: ServiceDetailViewModel,
    user: UserDto?,
    onBack: () -> Unit,
    onAddToOrder: (ServiceDto) -> AddResult,
    onGoToOrders: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            showEditor = false
            viewModel.clearFlags()
            snackbar.showSnackbar("Service updated")
        }
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            viewModel.clearFlags()
            onBack()
        }
    }
    LaunchedEffect(state.errorMessage, showEditor) {
        val message = state.errorMessage
        if (message != null && !showEditor && state.service is UiState.Success) {
            viewModel.clearFlags()
            snackbar.showSnackbar(message)
        }
    }

    val serviceState = state.service

    MmmScaffold(
        topBar = { MmmTopBar(title = "Service details", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (serviceState) {
            UiState.Loading -> LoadingView(Modifier.padding(padding))
            is UiState.Failure -> ErrorView(serviceState.error.message, Modifier.padding(padding), onRetry = viewModel::load)
            is UiState.Success -> {
                val service = serviceState.data
                val availability = DisplayRules.serviceAvailability(service.availability)
                val isOwner = Permissions.ownsService(user, service)

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (serviceState.isStale) StaleBanner(onRetry = viewModel::load)
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                service.category.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(service.name, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                Formatters.money(service.price),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                StatusChip(availability.label, availability.tone)
                                RatingBar(service.rating)
                            }
                            Text(
                                "Provided by ${service.providerName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                DisplayRules.deliveryLabel(service.deliveryDays),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (service.description.isNotBlank()) {
                                Text(service.description, style = MaterialTheme.typography.bodyLarge)
                            }
                            Text(
                                "Quantity is always 1. Add notes for the provider when you place your order.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            MmmButton(
                                text = "Add to order",
                                enabled = availability.canOrder,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val result = onAddToOrder(service)
                                    scope.launch { snackbar.showSnackbar(addResultMessage(result, service.name)) }
                                },
                            )
                            MmmButton("Go to order", onGoToOrders, style = MmmButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())

                            if (isOwner) {
                                Text("Manage this service", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    MmmButton("Edit", { showEditor = true }, style = MmmButtonStyle.Secondary, modifier = Modifier.weight(1f))
                                    MmmButton(
                                        "Delete", { showDelete = true }, style = MmmButtonStyle.Danger,
                                        enabled = !state.isWorking, modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }

                if (showEditor) {
                    ListingEditorSheet(
                        kind = ListingKind.SERVICE,
                        initial = service.toFormValues(),
                        isSaving = state.isWorking,
                        errorMessage = state.errorMessage,
                        onDismiss = { showEditor = false; viewModel.clearFlags() },
                        onSubmitService = { request -> viewModel.update(request.toUpdateRequest()) },
                    )
                }
                if (showDelete) {
                    AlertDialog(
                        onDismissRequest = { showDelete = false },
                        title = { Text("Delete service?") },
                        text = { Text("\"${service.name}\" will be removed. A service that already has orders cannot be deleted; set it to unavailable instead.") },
                        confirmButton = {
                            TextButton(onClick = { showDelete = false; viewModel.delete() }) { Text("Delete") }
                        },
                        dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
                    )
                }
            }
        }
    }
}
