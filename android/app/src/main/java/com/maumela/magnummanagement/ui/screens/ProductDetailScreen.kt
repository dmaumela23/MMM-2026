@file:OptIn(ExperimentalMaterial3Api::class)

package com.maumela.magnummanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.ui.components.ErrorView
import com.maumela.magnummanagement.ui.components.ListingEditorSheet
import com.maumela.magnummanagement.ui.components.ListingImage
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
import com.maumela.magnummanagement.utils.PriceCalculator
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.utils.addResultMessage
import com.maumela.magnummanagement.utils.toFormValues
import com.maumela.magnummanagement.utils.toUpdateRequest
import com.maumela.magnummanagement.viewmodel.AddResult
import com.maumela.magnummanagement.viewmodel.ProductDetailViewModel
import kotlinx.coroutines.launch

@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    user: UserDto?,
    onBack: () -> Unit,
    onAddToOrder: (ProductDto, Int) -> AddResult,
    onGoToOrders: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var quantity by rememberSaveable { mutableStateOf(1) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            showEditor = false
            viewModel.clearFlags()
            snackbar.showSnackbar("Product updated")
        }
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            viewModel.clearFlags()
            onBack()
        }
    }
    // Errors from delete (for example 409: the product has order history) appear as a snackbar.
    LaunchedEffect(state.errorMessage, showEditor) {
        val message = state.errorMessage
        if (message != null && !showEditor && state.product is UiState.Success) {
            viewModel.clearFlags()
            snackbar.showSnackbar(message)
        }
    }

    val productState = state.product

    MmmScaffold(
        topBar = { MmmTopBar(title = "Product details", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (productState) {
            UiState.Loading -> LoadingView(Modifier.padding(padding))
            is UiState.Failure -> ErrorView(productState.error.message, Modifier.padding(padding), onRetry = viewModel::load)
            is UiState.Success -> {
                val product = productState.data
                val availability = DisplayRules.productAvailability(product.availability, product.stockQuantity)
                val maxQuantity = minOf(PriceCalculator.MAX_PRODUCT_QUANTITY, product.stockQuantity).coerceAtLeast(1)
                val shownQuantity = quantity.coerceIn(1, maxQuantity)
                val isOwner = Permissions.ownsProduct(user, product)

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (productState.isStale) StaleBanner(onRetry = viewModel::load)
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    ) {
                        ListingImage(url = product.imageUrl, modifier = Modifier.fillMaxWidth().height(240.dp))
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                product.category.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(product.name, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                Formatters.money(product.price),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                StatusChip(availability.label, availability.tone)
                                RatingBar(product.rating)
                            }
                            Text(
                                "Sold by ${product.sellerName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (product.availability) {
                                Text(
                                    "${product.stockQuantity} in stock",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (product.description.isNotBlank()) {
                                Text(product.description, style = MaterialTheme.typography.bodyLarge)
                            }

                            if (availability.canOrder) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Quantity", style = MaterialTheme.typography.labelLarge)
                                    OutlinedButton(
                                        onClick = { quantity = (shownQuantity - 1).coerceAtLeast(1) },
                                        enabled = shownQuantity > 1,
                                        modifier = Modifier.semantics { contentDescription = "Decrease quantity" },
                                    ) { Text("-") }
                                    Text("$shownQuantity", style = MaterialTheme.typography.titleMedium)
                                    OutlinedButton(
                                        onClick = { quantity = (shownQuantity + 1).coerceAtMost(maxQuantity) },
                                        enabled = shownQuantity < maxQuantity,
                                        modifier = Modifier.semantics { contentDescription = "Increase quantity" },
                                    ) { Text("+") }
                                }
                                Text(
                                    "Total: ${Formatters.money(PriceCalculator.lineTotal(product.price, shownQuantity))}",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }

                            MmmButton(
                                text = "Add to order",
                                enabled = availability.canOrder,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val result = onAddToOrder(product, shownQuantity)
                                    scope.launch { snackbar.showSnackbar(addResultMessage(result, product.name)) }
                                },
                            )
                            MmmButton("Go to order", onGoToOrders, style = MmmButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())

                            if (isOwner) {
                                Text("Manage this product", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
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
                        kind = ListingKind.PRODUCT,
                        initial = product.toFormValues(),
                        isSaving = state.isWorking,
                        errorMessage = state.errorMessage,
                        onDismiss = { showEditor = false; viewModel.clearFlags() },
                        onSubmitProduct = { request -> viewModel.update(request.toUpdateRequest()) },
                    )
                }
                if (showDelete) {
                    AlertDialog(
                        onDismissRequest = { showDelete = false },
                        title = { Text("Delete product?") },
                        text = { Text("\"${product.name}\" will be removed. A product that already has orders cannot be deleted; set it to unavailable instead.") },
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
