package com.maumela.magnummanagement.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.maumela.magnummanagement.data.model.MonthlyReportDto
import com.maumela.magnummanagement.data.model.UsagePointDto
import com.maumela.magnummanagement.utils.DisplayRules
import com.maumela.magnummanagement.utils.Formatters

/**
 * Daily usage line chart (MPAndroidChart, a View-based library, wrapped in AndroidView).
 * The data is SIMULATED; screens show a PrototypeBanner next to it.
 */
@Composable
fun EnergyLineChart(points: List<UsagePointDto>, modifier: Modifier = Modifier, height: Dp = 220.dp) {
    val lineColor = MaterialTheme.colorScheme.secondary.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f).toArgb()
    val labels = remember(points) { points.map { Formatters.shortDate(it.date) } }
    val summary = remember(points) { "Line chart of daily energy use. " + DisplayRules.kwhSummary(points.map { it.kwh }) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = summary },
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                setScaleEnabled(false)
                setNoDataText("No usage data")
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                axisLeft.axisMinimum = 0f
            }
        },
        update = { chart ->
            chart.xAxis.textColor = textColor
            chart.axisLeft.textColor = textColor
            chart.axisLeft.gridColor = gridColor
            chart.setNoDataTextColor(textColor)
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.setLabelCount(labels.size.coerceIn(2, 5), false)
            if (points.isEmpty()) {
                chart.clear()
            } else {
                val entries = points.mapIndexed { index, point -> Entry(index.toFloat(), point.kwh.toFloat()) }
                val dataSet = LineDataSet(entries, "kWh").apply {
                    setColor(lineColor)
                    setLineWidth(2.5f)
                    setDrawCircles(false)
                    setDrawValues(false)
                    setMode(LineDataSet.Mode.CUBIC_BEZIER)
                    setDrawFilled(true)
                    setFillColor(lineColor)
                    setFillAlpha(50)
                }
                chart.data = LineData(dataSet)
            }
            chart.invalidate()
        },
    )
}

/** Monthly totals bar chart (SIMULATED data). */
@Composable
fun EnergyBarChart(months: List<MonthlyReportDto>, modifier: Modifier = Modifier, height: Dp = 220.dp) {
    val barColor = MaterialTheme.colorScheme.secondary.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f).toArgb()
    val labels = remember(months) { months.map { Formatters.month(it.month) } }
    val summary = remember(months) {
        "Bar chart of monthly energy use. " + DisplayRules.kwhSummary(months.map { it.totalKwh })
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = summary },
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                setScaleEnabled(false)
                setNoDataText("No report data")
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                axisLeft.axisMinimum = 0f
            }
        },
        update = { chart ->
            chart.xAxis.textColor = textColor
            chart.axisLeft.textColor = textColor
            chart.axisLeft.gridColor = gridColor
            chart.setNoDataTextColor(textColor)
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.setLabelCount(labels.size.coerceIn(2, 12), false)
            if (months.isEmpty()) {
                chart.clear()
            } else {
                val entries = months.mapIndexed { index, month -> BarEntry(index.toFloat(), month.totalKwh.toFloat()) }
                val dataSet = BarDataSet(entries, "Total kWh").apply {
                    setColor(barColor)
                    setDrawValues(true)
                    setValueTextColor(textColor)
                    setValueTextSize(10f)
                }
                chart.data = BarData(dataSet).apply { setBarWidth(0.6f) }
            }
            chart.invalidate()
        },
    )
}