package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.model.ProductUpdateRequest
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.model.ServiceUpdateRequest
import com.maumela.magnummanagement.utils.ListingForm
import com.maumela.magnummanagement.utils.ListingFormValues
import com.maumela.magnummanagement.utils.ListingKind
import com.maumela.magnummanagement.utils.toFormValues
import com.maumela.magnummanagement.utils.toUpdateRequest
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListingFormTest {
    private val validProduct = ListingFormValues(
        name = "  Solar Panel ", description = " 550W panel ", price = "1499.00", category = " Solar ",
        sector = Sector.ENERGY, imageUrl = " https://example.com/p.png ", stock = "10", availability = true,
    )
    private val validService = ListingFormValues(
        name = "Installation", price = "250,50", category = "Installation", sector = Sector.SERVICE, deliveryDays = "5",
    )

    // Verifies a valid product form has no errors and converts to a trimmed, exact-decimal request.
    @Test
    fun validProduct_convertsToRequest() {
        assertFalse(ListingForm.validate(ListingKind.PRODUCT, validProduct).hasErrors)
        val request = ListingForm.toProductRequest(validProduct)!!
        assertEquals("Solar Panel", request.name)
        assertEquals("550W panel", request.description)
        assertEquals(BigDecimal("1499.00"), request.price)
        assertEquals("Solar", request.category)
        assertEquals("https://example.com/p.png", request.imageUrl)
        assertEquals(10, request.stockQuantity)
    }

    // Verifies a blank image link becomes null (not an empty string).
    @Test
    fun blankImageLink_becomesNull() {
        assertNull(ListingForm.toProductRequest(validProduct.copy(imageUrl = "  "))!!.imageUrl)
    }

    // Verifies a valid service converts, accepting a comma as the decimal separator.
    @Test
    fun validService_convertsToRequest() {
        assertFalse(ListingForm.validate(ListingKind.SERVICE, validService).hasErrors)
        val request = ListingForm.toServiceRequest(validService)!!
        assertEquals(BigDecimal("250.50"), request.price)
        assertEquals(5, request.deliveryDays)
    }

    // Verifies every invalid product field is flagged.
    @Test
    fun invalidProduct_flagsEachField() {
        val errors = ListingForm.validate(
            ListingKind.PRODUCT,
            ListingFormValues(name = "A", price = "0", category = "", imageUrl = "ftp://x", stock = "-1"),
        )
        assertNotNull(errors.name)
        assertNotNull(errors.price)
        assertNotNull(errors.category)
        assertNotNull(errors.imageUrl)
        assertNotNull(errors.stock)
        assertNull(errors.deliveryDays) // does not apply to products
    }

    // Verifies a service needs delivery days but ignores product-only fields.
    @Test
    fun service_requiresDeliveryDaysAndIgnoresProductFields() {
        val errors = ListingForm.validate(
            ListingKind.SERVICE,
            validService.copy(deliveryDays = "0", stock = "oops", imageUrl = "junk"),
        )
        assertNotNull(errors.deliveryDays)
        assertNull(errors.stock)
        assertNull(errors.imageUrl)
    }

    // Verifies an over-long description is rejected.
    @Test
    fun longDescription_isRejected() {
        val errors = ListingForm.validate(ListingKind.PRODUCT, validProduct.copy(description = "x".repeat(2001)))
        assertNotNull(errors.description)
    }

    // Verifies an invalid form never produces a request to send.
    @Test
    fun invalidForm_producesNoRequest() {
        assertNull(ListingForm.toProductRequest(validProduct.copy(price = "abc")))
        assertNull(ListingForm.toServiceRequest(validService.copy(deliveryDays = "")))
    }

    // Verifies a new product form starts as Digital and a new service form as Service.
    @Test
    fun emptyForm_defaultsSectorByKind() {
        assertEquals(Sector.DIGITAL, ListingFormValues.empty(ListingKind.PRODUCT).sector)
        assertEquals(Sector.SERVICE, ListingFormValues.empty(ListingKind.SERVICE).sector)
    }

    // Verifies editing a product: values load into the form and convert back to the same request.
    @Test
    fun product_roundTripsThroughTheForm() {
        val product = testProduct(price = "1499.00", stock = 7)
        val request = ListingForm.toProductRequest(product.toFormValues())!!
        assertEquals(product.name, request.name)
        assertEquals(product.price, request.price)
        assertEquals(7, request.stockQuantity)
        assertNull(request.imageUrl)
    }

    // Verifies editing a service: values load into the form and convert back.
    @Test
    fun service_roundTripsThroughTheForm() {
        val service = testService(price = "250.50")
        val request = ListingForm.toServiceRequest(service.toFormValues())!!
        assertEquals(service.price, request.price)
        assertEquals(service.deliveryDays, request.deliveryDays)
    }

    // Verifies an edit is sent as an update containing every form field.
    @Test
    fun requests_convertToUpdateRequests() {
        val product = ListingForm.toProductRequest(validProduct)!!.toUpdateRequest()
        assertEquals(
            ProductUpdateRequest(
                name = "Solar Panel", description = "550W panel", price = BigDecimal("1499.00"), category = "Solar",
                sector = Sector.ENERGY, imageUrl = "https://example.com/p.png", stockQuantity = 10, availability = true,
            ),
            product,
        )
        val service = ListingForm.toServiceRequest(validService)!!.toUpdateRequest()
        assertEquals(
            ServiceUpdateRequest(
                name = "Installation", description = "", price = BigDecimal("250.50"), category = "Installation",
                sector = Sector.SERVICE, deliveryDays = 5, availability = true,
            ),
            service,
        )
        assertTrue(service.availability == true)
    }
}