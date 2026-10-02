package com.maumela.magnummanagement.utils

import com.maumela.magnummanagement.data.model.ItemStatus
import com.maumela.magnummanagement.data.model.OrderDto
import com.maumela.magnummanagement.data.model.OrderItemDto
import com.maumela.magnummanagement.data.model.UserDto

/** A button a seller can press on an order line. */
data class ItemAction(val label: String, val target: ItemStatus, val destructive: Boolean = false)

/** Order display rules. The backend remains the authority for every transition. */
object OrderRules {

    /** Customers may cancel only while every non-cancelled line is still PENDING. */
    fun canCustomerCancel(order: OrderDto, userId: Int): Boolean {
        if (order.customerId != userId) return false
        val active = order.items.filter { it.itemStatus != ItemStatus.CANCELLED }
        return active.isNotEmpty() && active.all { it.itemStatus == ItemStatus.PENDING }
    }

    /** The next steps for a line: PENDING > ACCEPTED > IN_PROGRESS > COMPLETED, or cancel early on. */
    fun actionsFor(status: ItemStatus): List<ItemAction> = when (status) {
        ItemStatus.PENDING -> listOf(
            ItemAction("Accept", ItemStatus.ACCEPTED),
            ItemAction("Cancel line", ItemStatus.CANCELLED, destructive = true),
        )
        ItemStatus.ACCEPTED -> listOf(
            ItemAction("Start work", ItemStatus.IN_PROGRESS),
            ItemAction("Cancel line", ItemStatus.CANCELLED, destructive = true),
        )
        ItemStatus.IN_PROGRESS -> listOf(ItemAction("Mark complete", ItemStatus.COMPLETED))
        ItemStatus.COMPLETED, ItemStatus.CANCELLED -> emptyList()
    }

    /**
     * Lines have no seller id in the API response, so the seller is recognised by name. This only
     * decides whether to SHOW the buttons: the server rejects anyone who is not the real seller (403).
     */
    fun isSellerOfItem(user: UserDto?, item: OrderItemDto): Boolean =
        user != null && Permissions.isSeller(user.role) && item.sellerName == user.fullName

    fun summaryLine(firstItemName: String?, itemCount: Int): String {
        val name = firstItemName?.takeIf { it.isNotBlank() } ?: return "Order"
        return if (itemCount <= 1) name else "$name and ${itemCount - 1} more"
    }
}
