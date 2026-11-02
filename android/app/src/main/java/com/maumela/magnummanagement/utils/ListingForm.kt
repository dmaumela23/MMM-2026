package com.maumela.magnummanagement.utils

import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.ProductRequest
import com.maumela.magnummanagement.data.model.ProductUpdateRequest
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.model.ServiceRequest
import com.maumela.magnummanagement.data.model.ServiceUpdateRequest

enum class ListingKind { PRODUCT, SERVICE }

/** Everything typed in the create/edit form, as the raw text the user entered. */
data class ListingFormValues(
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val category: String = "",
    val sector: Sector = Sector.DIGITAL,
    val imageUrl: String = "",     // products only
    val stock: String = "0",       // products only
    val deliveryDays: String = "", // services only
    val availability: Boolean = true,
) {
    companion object {
        fun empty(kind: ListingKind) = ListingFormValues(
            sector = if (kind == ListingKind.PRODUCT) Sector.DIGITAL else Sector.SERVICE,
        )
    }
}

data class ListingFormErrors(
    val name: String? = null,
    val description: String? = null,
    val price: String? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val stock: String? = null,
    val deliveryDays: String? = null,
) {
    val hasErrors: Boolean
        get() = listOf(name, description, price, category, imageUrl, stock, deliveryDays).any { it != null }
}

/** Validation and conversion for the listing editor. Only fields that apply to the kind are checked. */
object ListingForm {
    const val DESCRIPTION_MAX = 2000

    fun validate(kind: ListingKind, values: ListingFormValues) = ListingFormErrors(
        name = Validators.listingName(values.name),
        description = if (values.description.length > DESCRIPTION_MAX) {
            "Description must be at most $DESCRIPTION_MAX characters"
        } else null,
        price = Validators.price(values.price),
        category = Validators.listingCategory(values.category),
        imageUrl = if (kind == ListingKind.PRODUCT) Validators.imageUrl(values.imageUrl) else null,
        stock = if (kind == ListingKind.PRODUCT) Validators.stockQuantity(values.stock) else null,
        deliveryDays = if (kind == ListingKind.SERVICE) Validators.deliveryDays(values.deliveryDays) else null,
    )

    /** Returns null when the form is not valid. */
    fun toProductRequest(values: ListingFormValues): ProductRequest? {
        if (validate(ListingKind.PRODUCT, values).hasErrors) return null
        return ProductRequest(
            name = values.name.trim(),
            description = values.description.trim(),
            price = Validators.parsePrice(values.price) ?: return null,
            category = values.category.trim(),
            sector = values.sector,
            imageUrl = values.imageUrl.trim().ifEmpty { null },
            stockQuantity = values.stock.trim().toInt(),
            availability = values.availability,
        )
    }

    fun toServiceRequest(values: ListingFormValues): ServiceRequest? {
        if (validate(ListingKind.SERVICE, values).hasErrors) return null
        return ServiceRequest(
            name = values.name.trim(),
            description = values.description.trim(),
            price = Validators.parsePrice(values.price) ?: return null,
            category = values.category.trim(),
            sector = values.sector,
            deliveryDays = values.deliveryDays.trim().toInt(),
            availability = values.availability,
        )
    }
}

fun ProductDto.toFormValues() = ListingFormValues(
    name = name, description = description, price = price.toPlainString(), category = category,
    sector = sector, imageUrl = imageUrl.orEmpty(), stock = stockQuantity.toString(),
    availability = availability,
)

fun ServiceDto.toFormValues() = ListingFormValues(
    name = name, description = description, price = price.toPlainString(), category = category,
    sector = sector, deliveryDays = deliveryDays.toString(), availability = availability,
)

// Editing sends every form field. (An image link cannot be cleared this way, since an empty link is not sent.)
fun ProductRequest.toUpdateRequest() = ProductUpdateRequest(
    name = name, description = description, price = price, category = category, sector = sector,
    imageUrl = imageUrl, stockQuantity = stockQuantity, availability = availability,
)

fun ServiceRequest.toUpdateRequest() = ServiceUpdateRequest(
    name = name, description = description, price = price, category = category, sector = sector,
    deliveryDays = deliveryDays, availability = availability,
)