package com.maumela.magnummanagement.utils

import com.maumela.magnummanagement.viewmodel.AddResult

/** The snackbar text shown after "Add to order". */
fun addResultMessage(result: AddResult, name: String): String = when (result) {
    AddResult.ADDED -> "$name added to your order"
    AddResult.QUANTITY_INCREASED -> "Quantity updated for $name"
    AddResult.ALREADY_IN_ORDER -> "$name is already in your order"
    AddResult.LIMIT_REACHED -> "You have reached the maximum quantity for $name"
    AddResult.UNAVAILABLE -> "$name is not available right now"
}
