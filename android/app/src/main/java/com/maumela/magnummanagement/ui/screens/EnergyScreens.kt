@file:OptIn(ExperimentalMaterial3Api::class)

package com.maumela.magnummanagement.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.data.model.EnergyPlanDto
import com.maumela.magnummanagement.data.model.EnergyPlansResponse
import com.maumela.magnummanagement.data.model.EnergyUsageResponse
import com.maumela.magnummanagement.ui.components.EmptyView
import com.maumela.magnummanagement.ui.components.EnergyBarChart
import com.maumela.magnummanagement.ui.components.EnergyLineChart
import com.maumela.magnummanagement.ui.components.EnergyStatusChip
import com.maumela.magnummanagement.ui.components.ErrorView
import com.maumela.magnummanagement.ui.components.LoadingView
import com.maumela.magnummanagement.ui.components.MmmButton
import com.maumela.magnummanagement.ui.components.MmmButtonStyle
import com.maumela.magnummanagement.ui.components.MmmScaffold
import com.maumela.magnummanagement.ui.components.MmmTopBar
import com.maumela.magnummanagement.ui.components.PrototypeBanner
import com.maumela.magnummanagement.ui.components.RefreshOnResume
import com.maumela.magnummanagement.ui.components.SectionHeader
import com.maumela.magnummanagement.utils.EnergyCalculator
import com.maumela.magnummanagement.utils.Formatters
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.viewmodel.EnergyViewModel
import com.maumela.magnummanagement.data.repository.EnergyRepository

// ALL energy data is SIMULATED by the backend. Every screen below shows the PrototypeBanner.

@Composable
fun EnergyDashboardScreen(
    viewModel: EnergyViewModel,
    onUsage: () -> Unit,
    onReports: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refresh)
    val usageState = state.usage
    val plansState = state.plans

    MmmScaffold(topBar = { MmmTopBar(title = "Energy Solutions") }) { padding ->
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { PrototypeBanner() }

                item {
                    when (usageState) {
                        UiState.Loading -> LoadingView(Modifier.height(160.dp))
                        is UiState.Failure -> ErrorView(usageState.error.message, Modifier.height(260.dp), onRetry = viewModel::refresh)
                        is UiState.Success -> UsageSummary(usageState.data)
                    }
                }

                val usage = (usageState as? UiState.Success)?.data
                if (usage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Usage, last ${usage.days} days (simulated)", style = MaterialTheme.typography.titleMedium)
                                EnergyLineChart(usage.history)
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        MmmButton("Usage history", onUsage, style = MmmButtonStyle.Secondary, modifier = Modifier.weight(1f))
                        MmmButton("Reports", onReports, style = MmmButtonStyle.Secondary, modifier = Modifier.weight(1f))
                    }
                }

                item { SectionHeader("Energy plans", horizontalPadding = 0.dp) }
                when (plansState) {
                    UiState.Loading -> item { LoadingView(Modifier.height(120.dp)) }
                    is UiState.Failure -> item { ErrorView(plansState.error.message, Modifier.height(240.dp), onRetry = viewModel::refresh) }
                    is UiState.Success -> {
                        val response: EnergyPlansResponse = plansState.data
                        if (response.plans.isEmpty()) {
                            item { EmptyView("No plans", "There are no energy plans yet.", Modifier.height(160.dp)) }
                        } else {
                            items(response.plans, key = { it.id }) { plan ->
                                PlanCard(plan, isCurrent = plan.id == response.currentPlanId)
                            }
                            item {
                                Text(
                                    "Plans are read-only in this prototype. Your plan was assigned when your account was created.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageSummary(usage: EnergyUsageResponse) {
    val percent = EnergyCalculator.percentOfLimit(usage.estimatedMonthlyKwh, usage.plan.maxKwh)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Your energy (simulated)", style = MaterialTheme.typography.titleMedium)
                EnergyStatusChip(usage.status)
            }
            StatRow("Current usage (latest day)", Formatters.kwh(usage.currentKwh))
            StatRow("Estimated monthly usage", Formatters.kwh(usage.estimatedMonthlyKwh))
            StatRow("Estimated monthly cost", Formatters.money(usage.estimatedCost))
            StatRow("Plan", usage.plan.name)
            Text(
                "$percent% of your plan limit (${usage.plan.maxKwh} kWh)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { (percent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PlanCard(plan: EnergyPlanDto, isCurrent: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else null,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(plan.name, style = MaterialTheme.typography.titleMedium)
                if (isCurrent) Text("Your plan", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
            if (plan.description.isNotBlank()) {
                Text(plan.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatRow("Price per kWh", Formatters.money(plan.pricePerKwh))
            StatRow("Monthly fee", Formatters.money(plan.monthlyFee))
            StatRow("Included limit", "${plan.maxKwh} kWh")
        }
    }
}

@Composable
fun EnergyUsageScreen(viewModel: EnergyViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val usageState = state.usage

    MmmScaffold(topBar = { MmmTopBar(title = "Energy usage", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PrototypeBanner() }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Range", style = MaterialTheme.typography.labelLarge)
                    EnergyRepository.ALLOWED_DAYS.forEach { days ->
                        FilterChip(
                            selected = state.selectedDays == days,
                            onClick = { viewModel.setDays(days) },
                            label = { Text("$days days") },
                        )
                    }
                }
            }
            when (usageState) {
                UiState.Loading -> item { LoadingView(Modifier.height(200.dp)) }
                is UiState.Failure -> item { ErrorView(usageState.error.message, Modifier.height(260.dp), onRetry = viewModel::refresh) }
                is UiState.Success -> {
                    val usage = usageState.data
                    item { EnergyLineChart(usage.history) }
                    item { SectionHeader("Daily readings (simulated)", horizontalPadding = 0.dp) }
                    items(usage.history.reversed(), key = { it.date }) { point ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(Formatters.shortDate(point.date), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(Formatters.kwh(point.kwh), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(Formatters.money(point.estimatedCost), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
fun EnergyReportsScreen(viewModel: EnergyViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reportsState = state.reports

    MmmScaffold(topBar = { MmmTopBar(title = "Energy reports", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PrototypeBanner() }
            when (reportsState) {
                UiState.Loading -> item { LoadingView(Modifier.height(200.dp)) }
                is UiState.Failure -> item { ErrorView(reportsState.error.message, Modifier.height(260.dp), onRetry = viewModel::refresh) }
                is UiState.Success -> {
                    val months = reportsState.data.months
                    if (months.isEmpty()) {
                        item { EmptyView("No report data", "Reports appear once usage has been recorded.", Modifier.height(200.dp)) }
                    } else {
                        item { EnergyBarChart(months) }
                        items(months.reversed(), key = { it.month }) { month ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(Formatters.month(month.month), style = MaterialTheme.typography.titleMedium)
                                    StatRow("Total usage", Formatters.kwh(month.totalKwh))
                                    StatRow("Daily average", Formatters.kwh(month.averageDailyKwh))
                                    StatRow("Estimated cost", Formatters.money(month.estimatedCost))
                                    Text(
                                        "${month.daysRecorded} days recorded (simulated)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
