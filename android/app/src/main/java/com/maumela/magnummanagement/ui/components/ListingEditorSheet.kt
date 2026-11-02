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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.maumela.magnummanagement.data.model.ProductRequest
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.model.ServiceRequest
import com.maumela.magnummanagement.utils.Formatters
import com.maumela.magnummanagement.utils.ListingForm
import com.maumela.magnummanagement.utils.ListingFormErrors
import com.maumela.magnummanagement.utils.ListingFormValues
import com.maumela.magnummanagement.utils.ListingKind

/**
 * One bottom sheet for creating AND editing a product or a service (it is not a screen).
 * Input is validated here; the ViewModel decides what to do with the finished request.
 *
 * @param initial null = create a new listing; otherwise the values of the listing being edited
 * @param errorMessage a server error to show (for example a 403), set by the caller
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingEditorSheet(
    kind: ListingKind,
    initial: ListingFormValues?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmitProduct: (ProductRequest) -> Unit = {},
    onSubmitService: (ServiceRequest) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val start = initial ?: ListingFormValues.empty(kind)

    var name by rememberSaveable { mutableStateOf(start.name) }
    var description by rememberSaveable { mutableStateOf(start.description) }
    var price by rememberSaveable { mutableStateOf(start.price) }
    var category by rememberSaveable { mutableStateOf(start.category) }
    var sector by rememberSaveable { mutableStateOf(start.sector.name) }
    var imageUrl by rememberSaveable { mutableStateOf(start.imageUrl) }
    var stock by rememberSaveable { mutableStateOf(start.stock) }
    var deliveryDays by rememberSaveable { mutableStateOf(start.deliveryDays) }
    var availability by rememberSaveable { mutableStateOf(start.availability) }
    var errors by remember { mutableStateOf(ListingFormErrors()) }

    val noun = if (kind == ListingKind.PRODUCT) "product" else "service"

    fun submit() {
        val values = ListingFormValues(
            name = name, description = description, price = price, category = category,
            sector = Sector.valueOf(sector), imageUrl = imageUrl, stock = stock,
            deliveryDays = deliveryDays, availability = availability,
        )
        errors = ListingForm.validate(kind, values)
        if (errors.hasErrors) return
        when (kind) {
            ListingKind.PRODUCT -> ListingForm.toProductRequest(values)?.let(onSubmitProduct)
            ListingKind.SERVICE -> ListingForm.toServiceRequest(values)?.let(onSubmitService)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = (if (initial == null) "Add $noun" else "Edit $noun"),
                style = MaterialTheme.typography.titleLarge,
            )

            MmmTextField(
                value = name, onValueChange = { name = it }, label = "Name",
                error = errors.name, capitalization = KeyboardCapitalization.Sentences,
                modifier = Modifier.fillMaxWidth(),
            )
            MmmTextField(
                value = description, onValueChange = { description = it }, label = "Description (optional)",
                error = errors.description, capitalization = KeyboardCapitalization.Sentences,
                singleLine = false, minLines = 2, maxLines = 4, imeAction = ImeAction.Default,
                modifier = Modifier.fillMaxWidth(),
            )
            MmmTextField(
                value = price, onValueChange = { price = it }, label = "Price (R)",
                error = errors.price, helper = "For example 1499.00",
                keyboardType = KeyboardType.Decimal, modifier = Modifier.fillMaxWidth(),
            )
            MmmTextField(
                value = category, onValueChange = { category = it }, label = "Category",
                error = errors.category, capitalization = KeyboardCapitalization.Words,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Sector", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sector.entries.forEach { option ->
                    FilterChip(
                        selected = sector == option.name,
                        onClick = { sector = option.name },
                        label = { Text(Formatters.enumLabel(option.name)) },
                    )
                }
            }

            if (kind == ListingKind.PRODUCT) {
                MmmTextField(
                    value = stock, onValueChange = { stock = it }, label = "Stock quantity",
                    error = errors.stock, keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth(),
                )
                MmmTextField(
                    value = imageUrl, onValueChange = { imageUrl = it }, label = "Image link (optional)",
                    error = errors.imageUrl, helper = "A link starting with https://",
                    keyboardType = KeyboardType.Uri, modifier = Modifier.fillMaxWidth(),
                )
            } else {
                MmmTextField(
                    value = deliveryDays, onValueChange = { deliveryDays = it }, label = "Delivery time (days)",
                    error = errors.deliveryDays, keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = availability, role = Role.Switch, onValueChange = { availability = it }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Available for ordering", modifier = Modifier.weight(1f))
                Switch(checked = availability, onCheckedChange = null)
            }

            if (errorMessage != null) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MmmButton(
                    text = "Cancel", onClick = onDismiss, style = MmmButtonStyle.Secondary,
                    enabled = !isSaving, modifier = Modifier.weight(1f),
                )
                MmmButton(
                    text = "Save", onClick = ::submit, loading = isSaving, modifier = Modifier.weight(1f),
                )
            }
        }
    }
}