@file:OptIn(ExperimentalMaterial3Api::class)

package com.maumela.magnummanagement.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.utils.Formatters
import com.maumela.magnummanagement.viewmodel.MarketplaceFilters

/**
 * Price, rating, sector and availability filters. The choices are sent to the SERVER, which filters
 * with SQL, so the data really changes (not just the screen).
 *
 * [onApply] returns true when the choices were valid and applied.
 */
@Composable
fun MarketplaceFilterSheet(
    filters: MarketplaceFilters,
    errorMessage: String?,
    onApply: (Sector?, String, String, Double?, Boolean) -> Boolean,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sector by rememberSaveable { mutableStateOf(filters.sector?.name) }
    var minPrice by rememberSaveable { mutableStateOf(filters.minPrice?.toPlainString().orEmpty()) }
    var maxPrice by rememberSaveable { mutableStateOf(filters.maxPrice?.toPlainString().orEmpty()) }
    var minRating by rememberSaveable { mutableStateOf(filters.minRating) }
    var availableOnly by rememberSaveable { mutableStateOf(filters.availableOnly) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Filters", style = MaterialTheme.typography.titleLarge)

            Text("Sector", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = sector == null, onClick = { sector = null }, label = { Text("All") })
                Sector.entries.forEach { option ->
                    FilterChip(
                        selected = sector == option.name,
                        onClick = { sector = option.name },
                        label = { Text(Formatters.enumLabel(option.name)) },
                    )
                }
            }

            Text("Price (R)", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MmmTextField(
                    value = minPrice, onValueChange = { minPrice = it }, label = "Minimum",
                    keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f),
                )
                MmmTextField(
                    value = maxPrice, onValueChange = { maxPrice = it }, label = "Maximum",
                    keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f),
                )
            }

            Text("Minimum rating", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf<Pair<String, Double?>>("Any" to null, "3+" to 3.0, "4+" to 4.0, "4.5+" to 4.5).forEach { (label, value) ->
                    FilterChip(selected = minRating == value, onClick = { minRating = value }, label = { Text(label) })
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = availableOnly, role = Role.Switch, onValueChange = { availableOnly = it }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Available only (in stock)", modifier = Modifier.weight(1f))
                Switch(checked = availableOnly, onCheckedChange = null)
            }

            if (errorMessage != null) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MmmButton(
                    text = "Reset", style = MmmButtonStyle.Secondary, modifier = Modifier.weight(1f),
                    onClick = { onReset(); onDismiss() },
                )
                MmmButton(
                    text = "Apply", modifier = Modifier.weight(1f),
                    onClick = {
                        val selected = sector?.let { name -> Sector.entries.firstOrNull { it.name == name } }
                        if (onApply(selected, minPrice, maxPrice, minRating, availableOnly)) onDismiss()
                    },
                )
            }
        }
    }
}
