package com.maumela.magnummanagement.ui.screens

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maumela.magnummanagement.data.model.EnergyStatus
import com.maumela.magnummanagement.data.model.ItemStatus
import com.maumela.magnummanagement.data.model.MonthlyReportDto
import com.maumela.magnummanagement.data.model.OrderStatus
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.model.UsagePointDto
import com.maumela.magnummanagement.ui.components.EmptyView
import com.maumela.magnummanagement.ui.components.EnergyBarChart
import com.maumela.magnummanagement.ui.components.EnergyLineChart
import com.maumela.magnummanagement.ui.components.EnergyStatusChip
import com.maumela.magnummanagement.ui.components.ErrorView
import com.maumela.magnummanagement.ui.components.ItemStatusChip
import com.maumela.magnummanagement.ui.components.ListingEditorSheet
import com.maumela.magnummanagement.ui.components.LoadingView
import com.maumela.magnummanagement.ui.components.MmmButton
import com.maumela.magnummanagement.ui.components.MmmButtonStyle
import com.maumela.magnummanagement.ui.components.MmmTextField
import com.maumela.magnummanagement.ui.components.MmmTopBar
import com.maumela.magnummanagement.ui.components.OrderStatusChip
import com.maumela.magnummanagement.ui.components.PrototypeBanner
import com.maumela.magnummanagement.ui.components.ProductCard
import com.maumela.magnummanagement.ui.components.RatingBar
import com.maumela.magnummanagement.ui.components.SectorTile
import com.maumela.magnummanagement.ui.components.ServiceCard
import com.maumela.magnummanagement.ui.components.StaleBanner
import com.maumela.magnummanagement.utils.ListingKind
import com.maumela.magnummanagement.utils.SectorRegistry
import java.math.BigDecimal
import java.time.LocalDate

/** TEMPORARY: shows every reusable component with sample data. Removed when real screens exist. */
@Composable
fun ComponentGallery() {
    var showEditor by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val product = remember {
        ProductDto(
            id = 1, sellerId = 1, sellerName = "Maumela Demo Business", name = "550W Monocrystalline Solar Panel",
            description = "", price = BigDecimal("2899.00"), category = "Solar", sector = Sector.ENERGY,
            imageUrl = "https://picsum.photos/seed/mmm-solar-panel/600/400", stockQuantity = 4,
            availability = true, rating = 4.8, createdAt = "2026-09-30T10:00:00Z", updatedAt = "2026-09-30T10:00:00Z",
        )
    }
    val service = remember {
        ServiceDto(
            id = 1, providerId = 2, providerName = "Demo Provider", name = "Website Design & Development",
            description = "", price = BigDecimal("7500.00"), category = "Web Development", sector = Sector.SERVICE,
            availability = true, rating = 4.6, deliveryDays = 14,
            createdAt = "2026-09-30T10:00:00Z", updatedAt = "2026-09-30T10:00:00Z",
        )
    }
    val usage = remember {
        (0 until 30).map {
            UsagePointDto(
                date = LocalDate.of(2026, 9, 1).plusDays(it.toLong()).toString(),
                kwh = 12.0 + (it * 7 % 9), estimatedCost = BigDecimal("31.20"), isSimulated = true,
            )
        }
    }
    val months = remember {
        listOf(
            MonthlyReportDto("2026-07", 412.4, 13.3, 31, BigDecimal("1221.24")),
            MonthlyReportDto("2026-08", 455.9, 14.7, 31, BigDecimal("1335.34")),
            MonthlyReportDto("2026-09", 380.2, 12.7, 30, BigDecimal("1138.52")),
        )
    }

    Scaffold(topBar = { MmmTopBar(title = "Component gallery", onBack = {}) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text("Sector tiles", style = MaterialTheme.typography.titleMedium) }
            items(SectorRegistry.all) { info -> SectorTile(info, onClick = {}, modifier = Modifier.fillMaxWidth()) }

            item { Text("Buttons", style = MaterialTheme.typography.titleMedium) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MmmButton("Primary", {})
                    MmmButton("Secondary", {}, style = MmmButtonStyle.Secondary)
                    MmmButton("Delete", {}, style = MmmButtonStyle.Danger)
                }
            }
            item { MmmButton("Saving", {}, loading = true) }
            item { MmmButton("Disabled", {}, enabled = false) }

            item { Text("Text fields", style = MaterialTheme.typography.titleMedium) }
            item { MmmTextField(email, { email = it }, "Email", error = "Enter a valid email address", modifier = Modifier.fillMaxWidth()) }
            item { MmmTextField(password, { password = it }, "Password", isPassword = true, helper = "At least 8 characters", modifier = Modifier.fillMaxWidth()) }

            item { Text("Status chips and rating", style = MaterialTheme.typography.titleMedium) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ItemStatusChip(ItemStatus.PENDING)
                    ItemStatusChip(ItemStatus.IN_PROGRESS)
                    OrderStatusChip(OrderStatus.COMPLETED)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrderStatusChip(OrderStatus.CANCELLED)
                    EnergyStatusChip(EnergyStatus.HIGH)
                }
            }
            item { RatingBar(4.6) }

            item { Text("Cards", style = MaterialTheme.typography.titleMedium) }
            item { ProductCard(product, onClick = {}, onAddToOrder = {}) }
            item { ServiceCard(service, onClick = {}, onRequest = {}) }

            item { Text("Banners", style = MaterialTheme.typography.titleMedium) }
            item { StaleBanner(onRetry = {}) }
            item { PrototypeBanner() }

            item { Text("Energy charts (simulated)", style = MaterialTheme.typography.titleMedium) }
            item { EnergyLineChart(usage) }
            item { EnergyBarChart(months) }

            item { Text("State views", style = MaterialTheme.typography.titleMedium) }
            item { LoadingView(Modifier.height(140.dp), message = "Loading products...") }
            item { ErrorView("Can't reach the MMM server. Make sure it is running and try again.", Modifier.height(240.dp), onRetry = {}) }
            item { EmptyView("No products found", "Try a different search or filter.", Modifier.height(200.dp)) }

            item { Text("Listing editor", style = MaterialTheme.typography.titleMedium) }
            item { MmmButton("Open editor", { showEditor = true }) }
        }
    }

    if (showEditor) {
        ListingEditorSheet(
            kind = ListingKind.PRODUCT,
            initial = null,
            isSaving = false,
            errorMessage = null,
            onDismiss = { showEditor = false },
            onSubmitProduct = { showEditor = false },
        )
    }
}