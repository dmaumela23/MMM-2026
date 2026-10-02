@file:OptIn(ExperimentalMaterial3Api::class)

package com.maumela.magnummanagement.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.maumela.magnummanagement.ui.components.ListingEditorSheet
import com.maumela.magnummanagement.ui.components.MmmButton
import com.maumela.magnummanagement.ui.components.MmmButtonStyle
import com.maumela.magnummanagement.ui.components.MmmScaffold
import com.maumela.magnummanagement.ui.components.MmmTopBar
import com.maumela.magnummanagement.ui.components.RefreshOnResume
import com.maumela.magnummanagement.ui.components.SectionHeader
import com.maumela.magnummanagement.ui.theme.Gold
import com.maumela.magnummanagement.ui.theme.Navy900
import com.maumela.magnummanagement.utils.Formatters
import com.maumela.magnummanagement.utils.ListingKind
import com.maumela.magnummanagement.utils.Permissions
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.viewmodel.ProfileOverviewViewModel

/** Account overview, plus "My listings" (with Add buttons) for Provider and Business accounts. */
@Composable
fun ProfileScreen(
    viewModel: ProfileOverviewViewModel,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onProduct: (Int) -> Unit,
    onService: (Int) -> Unit,
    onOpenOrders: () -> Unit,
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editorKind by rememberSaveable { mutableStateOf<String?>(null) }

    RefreshOnResume(viewModel::load)

    LaunchedEffect(state.savedMessage) {
        val message = state.savedMessage
        if (message != null) {
            editorKind = null
            viewModel.clearMessages()
            snackbar.showSnackbar(message)
        }
    }

    val role = user?.role
    val account = user
    val products = state.products
    val services = state.services

    MmmScaffold(
        topBar = { MmmTopBar(title = "Profile") },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (account == null) {
                Text("You are not logged in.", style = MaterialTheme.typography.bodyLarge)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp).background(Gold, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(Formatters.initials(account.fullName), style = MaterialTheme.typography.headlineMedium, color = Navy900)
                        }
                        Text(account.fullName, style = MaterialTheme.typography.titleLarge)
                        Text(Formatters.roleLabel(account.role.name), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoRow("Email", account.email)
                        InfoRow("Phone", account.phone ?: "Not provided")
                        InfoRow("Account type", Formatters.roleLabel(account.role.name))
                        InfoRow("Member since", Formatters.date(account.createdAt))
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenOrders),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Orders placed", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.orderCount?.toString() ?: "-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (Permissions.canCreateProducts(role)) {
                    SectionHeader("My products", horizontalPadding = 0.dp)
                    when (products) {
                        UiState.Loading -> Text("Loading...", style = MaterialTheme.typography.bodyMedium)
                        is UiState.Failure -> Text(products.error.message, color = MaterialTheme.colorScheme.error)
                        is UiState.Success -> {
                            if (products.data.isEmpty()) Text("You have not added any products yet.", style = MaterialTheme.typography.bodyMedium)
                            products.data.forEach { product ->
                                ListingRow(
                                    title = product.name,
                                    subtitle = "${Formatters.money(product.price)} · ${product.stockQuantity} in stock",
                                    onClick = { onProduct(product.id) },
                                )
                            }
                        }
                    }
                    MmmButton("Add product", { editorKind = ListingKind.PRODUCT.name }, style = MmmButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                }

                if (Permissions.canCreateServices(role)) {
                    SectionHeader("My services", horizontalPadding = 0.dp)
                    when (services) {
                        UiState.Loading -> Text("Loading...", style = MaterialTheme.typography.bodyMedium)
                        is UiState.Failure -> Text(services.error.message, color = MaterialTheme.colorScheme.error)
                        is UiState.Success -> {
                            if (services.data.isEmpty()) Text("You have not added any services yet.", style = MaterialTheme.typography.bodyMedium)
                            services.data.forEach { service ->
                                ListingRow(
                                    title = service.name,
                                    subtitle = "${Formatters.money(service.price)} · ${service.deliveryDays} days",
                                    onClick = { onService(service.id) },
                                )
                            }
                        }
                    }
                    MmmButton("Add service", { editorKind = ListingKind.SERVICE.name }, style = MmmButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                }

                MmmButton("Settings", onSettings, style = MmmButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                MmmButton("Log out", onLogout, style = MmmButtonStyle.Danger, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    val kind = editorKind?.let { name -> ListingKind.entries.firstOrNull { it.name == name } }
    if (kind != null) {
        ListingEditorSheet(
            kind = kind,
            initial = null,
            isSaving = state.isSaving,
            errorMessage = state.errorMessage,
            onDismiss = { editorKind = null; viewModel.clearMessages() },
            onSubmitProduct = { viewModel.createProduct(it) },
            onSubmitService = { viewModel.createService(it) },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ListingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
