package com.maumela.magnummanagement.data.repository

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.api.AppError
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.local.dao.OrderDao
import com.maumela.magnummanagement.data.local.toDto
import com.maumela.magnummanagement.data.local.toEntity
import com.maumela.magnummanagement.data.model.IncomingItemDto
import com.maumela.magnummanagement.data.model.ItemStatus
import com.maumela.magnummanagement.data.model.ItemStatusUpdateRequest
import com.maumela.magnummanagement.data.model.OrderCancelRequest
import com.maumela.magnummanagement.data.model.OrderCreateRequest
import com.maumela.magnummanagement.data.model.OrderDto
import com.maumela.magnummanagement.data.model.OrderItemDto
import com.maumela.magnummanagement.data.model.OrderItemRequest
import com.maumela.magnummanagement.data.model.OrderSummaryDto
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.safeApiCall

class OrderRepository(
    private val api: ApiService,
    private val dao: OrderDao,
) {
    /**
     * Sends ids and quantities ONLY. The server loads current prices and calculates the total,
     * so nothing the app sends can change what an order costs.
     */
    suspend fun createOrder(items: List<OrderItemRequest>, notes: String?): ApiResult<OrderDto> {
        if (items.isEmpty()) {
            return ApiResult.Failure(
                AppError(ErrorKind.BAD_REQUEST, "Your order is empty. Add a product or service first."),
            )
        }
        // Service quantity is always 1.
        val normalised = items.map { if (it.serviceId != null) it.copy(quantity = 1) else it }
        val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
        return safeApiCall { api.createOrder(OrderCreateRequest(normalised, cleanNotes)) }
    }

    /** My orders (list). Cached for offline viewing; details always come from the server. */
    suspend fun getOrders(): ApiResult<Cached<List<OrderSummaryDto>>> =
        fetchWithCache(
            fetch = { api.getOrders() },
            save = { dao.replaceAll(it.map { summary -> summary.toEntity() }) },
            readCache = { dao.getAll().takeIf { it.isNotEmpty() }?.map { it.toDto() } },
        )

    suspend fun getOrder(id: Int): ApiResult<OrderDto> = safeApiCall { api.getOrder(id) }

    suspend fun getIncomingItems(): ApiResult<List<IncomingItemDto>> =
        safeApiCall { api.getIncomingItems() }

    suspend fun cancelOrder(id: Int): ApiResult<OrderDto> =
        safeApiCall { api.cancelOrder(id, OrderCancelRequest()) }

    suspend fun updateItemStatus(orderId: Int, itemId: Int, status: ItemStatus): ApiResult<OrderItemDto> =
        safeApiCall { api.updateItemStatus(orderId, itemId, ItemStatusUpdateRequest(status)) }
}