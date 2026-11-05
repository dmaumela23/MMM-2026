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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.R
import com.maumela.magnummanagement.data.model.EnergyPlanDto
import com.maumela.magnummanagement.data.model.EnergyPlansResponse
import com.maumela.magnummanagement.data.model.EnergyUsageResponse
import com.maumela.magnummanagement.data.repository.EnergyRepository
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

    MmmScaffold(topBar = { MmmTopBar(title = stringResource(R.string.energy_solutions_title)) }) { padding ->
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
                                Text(stringResource(R.string.energy_usage_history_days, usage.days), style = MaterialTheme.typography.titleMedium)
                                EnergyLineChart(usage.history)
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        MmmButton(stringResource(R.string.btn_usage_history), onUsage, style = MmmButtonStyle.Secondary, modifier = Modifier.weight(1f))
                        MmmButton(stringResource(R.string.btn_reports), onReports, style = MmmButtonStyle.Secondary, modifier = Modifier.weight(1f))
                    }
                }

                item { SectionHeader(stringResource(R.string.section_energy_plans), horizontalPadding = 0.dp) }
                when (plansState) {
                    UiState.Loading -> item { LoadingView(Modifier.height(120.dp)) }
                    is UiState.Failure -> item { ErrorView(plansState.error.message, Modifier.height(240.dp), onRetry = viewModel::refresh) }
                    is UiState.Success -> {
                        val response: EnergyPlansResponse = plansState.data
                        if (response.plans.isEmpty()) {
                            item { EmptyView(stringResource(R.string.empty_no_plans_title), stringResource(R.string.empty_no_plans_subtitle), Modifier.height(160.dp)) }
                        } else {
                            items(response.plans, key = { it.id }) { plan ->
                                PlanCard(plan, isCurrent = plan.id == response.currentPlanId)
                            }
                            item {
                                Text(
                                    stringResource(R.string.msg_plans_readonly),
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
                Text(stringResource(R.string.your_energy_simulated), style = MaterialTheme.typography.titleMedium)
                EnergyStatusChip(usage.status)
            }
            StatRow(stringResource(R.string.stat_current_usage_latest), Formatters.kwh(usage.currentKwh))
            StatRow(stringResource(R.string.stat_estimated_monthly_usage), Formatters.kwh(usage.estimatedMonthlyKwh))
            StatRow(stringResource(R.string.stat_estimated_monthly_cost), Formatters.money(usage.estimatedCost))
            StatRow(stringResource(R.string.stat_plan), usage.plan.name)
            Text(
                stringResource(R.string.plan_limit_format, percent, usage.plan.maxKwh),
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
                if (isCurrent) Text(stringResource(R.string.label_your_plan), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
            if (plan.description.isNotBlank()) {
                Text(plan.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatRow(stringResource(R.string.stat_price_per_kwh), Formatters.money(plan.pricePerKwh))
            StatRow(stringResource(R.string.stat_monthly_fee), Formatters.money(plan.monthlyFee))
            StatRow(stringResource(R.string.stat_included_limit), "${plan.maxKwh} kWh")
        }
    }
}

@Composable
fun EnergyUsageScreen(viewModel: EnergyViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val usageState = state.usage

    MmmScaffold(topBar = { MmmTopBar(title = stringResource(R.string.energy_usage_title), onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PrototypeBanner() }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.label_range), style = MaterialTheme.typography.labelLarge)
                    EnergyRepository.ALLOWED_DAYS.forEach { days ->
                        FilterChip(
                            selected = state.selectedDays == days,
                            onClick = { viewModel.setDays(days) },
                            label = { Text(stringResource(R.string.days_format, days)) },
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
                    item { SectionHeader(stringResource(R.string.section_daily_readings), horizontalPadding = 0.dp) }
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

    MmmScaffold(topBar = { MmmTopBar(title = stringResource(R.string.energy_reports_title), onBack = onBack) }) { padding ->
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
                        item { EmptyView(stringResource(R.string.empty_no_reports_title), stringResource(R.string.empty_no_reports_subtitle), Modifier.height(200.dp)) }
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
                                    StatRow(stringResource(R.string.stat_total_usage), Formatters.kwh(month.totalKwh))
                                    StatRow(stringResource(R.string.stat_daily_average), Formatters.kwh(month.averageDailyKwh))
                                    StatRow(stringResource(R.string.stat_estimated_cost), Formatters.money(month.estimatedCost))
                                    Text(
                                        stringResource(R.string.days_recorded_simulated, month.daysRecorded),
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