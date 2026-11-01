package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.model.ItemStatus
import com.maumela.magnummanagement.data.model.UserRole
import com.maumela.magnummanagement.utils.OrderRules
import com.maumela.magnummanagement.utils.Permissions
import com.maumela.magnummanagement.utils.addResultMessage
import com.maumela.magnummanagement.viewmodel.AddResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionsTest {

    // Verifies only Business accounts can create products.
    @Test
    fun onlyBusinessCreatesProducts() {
        assertFalse(Permissions.canCreateProducts(UserRole.CUSTOMER))
        assertFalse(Permissions.canCreateProducts(UserRole.PROVIDER))
        assertTrue(Permissions.canCreateProducts(UserRole.BUSINESS))
        assertFalse(Permissions.canCreateProducts(null))
    }

    // Verifies Provider and Business accounts can create services, Customers cannot.
    @Test
    fun providerAndBusinessCreateServices() {
        assertFalse(Permissions.canCreateServices(UserRole.CUSTOMER))
        assertTrue(Permissions.canCreateServices(UserRole.PROVIDER))
        assertTrue(Permissions.canCreateServices(UserRole.BUSINESS))
        assertFalse(Permissions.canCreateServices(null))
    }

    // Verifies seller roles are exactly the roles that receive incoming order lines.
    @Test
    fun sellerRoles() {
        assertFalse(Permissions.isSeller(UserRole.CUSTOMER))
        assertTrue(Permissions.isSeller(UserRole.PROVIDER))
        assertTrue(Permissions.isSeller(UserRole.BUSINESS))
    }

    // Verifies only the owning Business account sees product Edit/Delete (the sample product belongs to user 3).
    @Test
    fun productOwnership() {
        val product = testProduct()
        assertTrue(Permissions.ownsProduct(testUser(id = 3, role = UserRole.BUSINESS), product))
        assertFalse(Permissions.ownsProduct(testUser(id = 9, role = UserRole.BUSINESS), product))
        assertFalse(Permissions.ownsProduct(testUser(id = 3, role = UserRole.PROVIDER), product))
        assertFalse(Permissions.ownsProduct(null, product))
    }

    // Verifies only the owning Provider or Business account sees service Edit/Delete (the sample service belongs to user 4).
    @Test
    fun serviceOwnership() {
        val service = testService()
        assertTrue(Permissions.ownsService(testUser(id = 4, role = UserRole.PROVIDER), service))
        assertTrue(Permissions.ownsService(testUser(id = 4, role = UserRole.BUSINESS), service))
        assertFalse(Permissions.ownsService(testUser(id = 4, role = UserRole.CUSTOMER), service))
        assertFalse(Permissions.ownsService(testUser(id = 5, role = UserRole.PROVIDER), service))
        assertFalse(Permissions.ownsService(null, service))
    }
}

class OrderRulesTest {
    private val order = testOrder() // customer 1, one PENDING service line sold by "Pieter"
    private val line = order.items.first()

    // Verifies a customer can cancel while every line is pending, and nobody else can.
    @Test
    fun customerCancel_allPending() {
        assertTrue(OrderRules.canCustomerCancel(order, userId = 1))
        assertFalse(OrderRules.canCustomerCancel(order, userId = 2))
    }

    // Verifies cancelling is refused once any line has been accepted.
    @Test
    fun customerCancel_blockedOnceWorkStarted() {
        val started = order.copy(items = listOf(line, line.copy(id = 2, itemStatus = ItemStatus.ACCEPTED)))
        assertFalse(OrderRules.canCustomerCancel(started, userId = 1))
    }

    // Verifies cancelled lines are ignored, and a fully cancelled order cannot be cancelled again.
    @Test
    fun customerCancel_ignoresCancelledLines() {
        val mixed = order.copy(items = listOf(line, line.copy(id = 2, itemStatus = ItemStatus.CANCELLED)))
        assertTrue(OrderRules.canCustomerCancel(mixed, userId = 1))
        val allCancelled = order.copy(items = listOf(line.copy(itemStatus = ItemStatus.CANCELLED)))
        assertFalse(OrderRules.canCustomerCancel(allCancelled, userId = 1))
    }

    // Verifies the seller's buttons follow the workflow PENDING > ACCEPTED > IN_PROGRESS > COMPLETED.
    @Test
    fun sellerActions_followTheWorkflow() {
        assertEquals(
            listOf(ItemStatus.ACCEPTED, ItemStatus.CANCELLED),
            OrderRules.actionsFor(ItemStatus.PENDING).map { it.target },
        )
        assertEquals(
            listOf(ItemStatus.IN_PROGRESS, ItemStatus.CANCELLED),
            OrderRules.actionsFor(ItemStatus.ACCEPTED).map { it.target },
        )
        assertEquals(listOf(ItemStatus.COMPLETED), OrderRules.actionsFor(ItemStatus.IN_PROGRESS).map { it.target })
    }

    // Verifies finished lines have no actions, and only cancelling is styled as destructive.
    @Test
    fun sellerActions_terminalStatesAndDestructiveFlag() {
        assertTrue(OrderRules.actionsFor(ItemStatus.COMPLETED).isEmpty())
        assertTrue(OrderRules.actionsFor(ItemStatus.CANCELLED).isEmpty())
        val pending = OrderRules.actionsFor(ItemStatus.PENDING)
        assertFalse(pending.first { it.target == ItemStatus.ACCEPTED }.destructive)
        assertTrue(pending.first { it.target == ItemStatus.CANCELLED }.destructive)
    }

    // Verifies the seller buttons are shown only to a seller-role user whose name matches the line's seller.
    @Test
    fun isSellerOfItem_rules() {
        assertTrue(OrderRules.isSellerOfItem(testUser(role = UserRole.PROVIDER, fullName = "Pieter"), line))
        assertTrue(OrderRules.isSellerOfItem(testUser(role = UserRole.BUSINESS, fullName = "Pieter"), line))
        assertFalse(OrderRules.isSellerOfItem(testUser(role = UserRole.CUSTOMER, fullName = "Pieter"), line))
        assertFalse(OrderRules.isSellerOfItem(testUser(role = UserRole.PROVIDER, fullName = "Someone Else"), line))
        assertFalse(OrderRules.isSellerOfItem(null, line))
    }

    // Verifies the one-line order summary for none, one and several items.
    @Test
    fun summaryLine_rules() {
        assertEquals("Order", OrderRules.summaryLine(null, 0))
        assertEquals("Battery", OrderRules.summaryLine("Battery", 1))
        assertEquals("Battery and 2 more", OrderRules.summaryLine("Battery", 3))
    }
}

class AddResultMessagesTest {

    // Verifies every possible "add to order" outcome has a distinct, readable message naming the item.
    @Test
    fun everyResultHasAMessage() {
        val messages = AddResult.entries.map { addResultMessage(it, "Solar Panel") }
        assertEquals(AddResult.entries.size, messages.toSet().size)
        assertTrue(messages.all { it.contains("Solar Panel") })
        assertEquals("Solar Panel added to your order", addResultMessage(AddResult.ADDED, "Solar Panel"))
    }
}
