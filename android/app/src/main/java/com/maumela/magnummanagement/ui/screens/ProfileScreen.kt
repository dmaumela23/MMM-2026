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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.R
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
        topBar = { MmmTopBar(title = stringResource(R.string.profile_title)) },
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
                Text(stringResource(R.string.msg_not_logged_in), style = MaterialTheme.typography.bodyLarge)
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
                        InfoRow(stringResource(R.string.label_email_info), account.email)
                        InfoRow(stringResource(R.string.label_phone_info), account.phone ?: stringResource(R.string.val_not_provided))
                        InfoRow(stringResource(R.string.label_account_type_info), Formatters.roleLabel(account.role.name))
                        InfoRow(stringResource(R.string.label_member_since), Formatters.date(account.createdAt))
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenOrders),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(stringResource(R.string.label_orders_placed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.orderCount?.toString() ?: "-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (Permissions.canCreateProducts(role)) {
                    SectionHeader(stringResource(R.string.section_my_products), horizontalPadding = 0.dp)
                    when (products) {
                        UiState.Loading -> Text(stringResource(R.string.loading_generic), style = MaterialTheme.typography.bodyMedium)
                        is UiState.Failure -> Text(products.error.message, color = MaterialTheme.colorScheme.error)
                        is UiState.Success -> {
                            if (products.data.isEmpty()) Text(stringResource(R.string.empty_my_products), style = MaterialTheme.typography.bodyMedium)
                            products.data.forEach { product ->
                                ListingRow(
                                    title = product.name,
                                    subtitle = stringResource(R.string.product_subtitle_format, Formatters.money(product.price), product.stockQuantity),
                                    onClick = { onProduct(product.id) },
                                )
                            }
                        }
                    }
                    MmmButton(stringResource(R.string.btn_add_product), { editorKind = ListingKind.PRODUCT.name }, style = MmmButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                }

                if (Permissions.canCreateServices(role)) {
                    SectionHeader(stringResource(R.string.section_my_services), horizontalPadding = 0.dp)
                    when (services) {
                        UiState.Loading -> Text(stringResource(R.string.loading_generic), style = MaterialTheme.typography.bodyMedium)
                        is UiState.Failure -> Text(services.error.message, color = MaterialTheme.colorScheme.error)
                        is UiState.Success -> {
                            if (services.data.isEmpty()) Text(stringResource(R.string.empty_my_services), style = MaterialTheme.typography.bodyMedium)
                            services.data.forEach { service ->
                                ListingRow(
                                    title = service.name,
                                    subtitle = stringResource(R.string.service_subtitle_format, Formatters.money(service.price), service.deliveryDays),
                                    onClick = { onService(service.id) },
                                )
                            }
                        }
                    }
                    MmmButton(stringResource(R.string.btn_add_service), { editorKind = ListingKind.SERVICE.name }, style = MmmButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                }

                MmmButton(stringResource(R.string.action_settings), onSettings, style = MmmButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                MmmButton(stringResource(R.string.btn_log_out), onLogout, style = MmmButtonStyle.Danger, modifier = Modifier.fillMaxWidth())
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