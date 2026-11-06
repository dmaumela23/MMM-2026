package com.maumela.magnummanagement.data.repository

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.api.ApiErrorParser
import com.maumela.magnummanagement.data.local.dao.OrderDao
import com.maumela.magnummanagement.data.local.entity.OrderEntity
import com.maumela.magnummanagement.data.model.*
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class OrderRepository(
    private val api: ApiService,
    private val orderDao: OrderDao
) {
    suspend fun getOrders(): ApiResult<Cached<List<OrderSummaryDto>>> = withContext(Dispatchers.IO) {
        fetchWithCache(
            fetch = { api.getOrders() },
            save = { summaries ->
                val entities = summaries.map { summary ->
                    OrderEntity(
                        id = summary.id,
                        status = summary.status.name,
                        totalAmount = summary.totalAmount.toString(),
                        createdAt = summary.createdAt,
                        itemCount = summary.itemCount,
                        firstItemName = summary.firstItemName,
                        isSynced = true
                    )
                }
                orderDao.syncFromServer(entities)
            },
            readCache = {
                orderDao.getAll().map { entity ->
                    OrderSummaryDto(
                        id = entity.id,
                        status = OrderStatus.valueOf(entity.status),
                        totalAmount = BigDecimal(entity.totalAmount),
                        createdAt = entity.createdAt,
                        itemCount = entity.itemCount,
                        firstItemName = entity.firstItemName
                    )
                }
            }
        )
    }

    suspend fun getOrder(id: Int): ApiResult<OrderDto> = withContext(Dispatchers.IO) {
        safeApiCall { api.getOrder(id) }
    }

    suspend fun getIncomingItems(): ApiResult<List<IncomingItemDto>> = withContext(Dispatchers.IO) {
        safeApiCall { api.getIncomingItems() }
    }

    suspend fun createOrder(request: OrderCreateRequest): ApiResult<OrderDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.createOrder(request)
            val entity = OrderEntity(
                id = response.id,
                status = response.status.name,
                totalAmount = response.totalAmount.toString(),
                createdAt = response.createdAt,
                itemCount = response.items.size,
                firstItemName = response.items.firstOrNull()?.name,
                isSynced = true
            )
            orderDao.upsert(entity)
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Failure(ApiErrorParser.parse(e))
        }
    }

    suspend fun createOrder(items: List<OrderItemRequest>, notes: String? = null): ApiResult<OrderDto> =
        createOrder(OrderCreateRequest(items = items, notes = notes))

    suspend fun cancelOrder(id: Int, request: OrderCancelRequest): ApiResult<OrderDto> = withContext(Dispatchers.IO) {
        safeApiCall { api.cancelOrder(id, request) }
    }

    suspend fun cancelOrder(id: Int, status: OrderStatus = OrderStatus.CANCELLED): ApiResult<OrderDto> =
        cancelOrder(id, OrderCancelRequest(status = status))

    suspend fun updateItemStatus(orderId: Int, itemId: Int, request: ItemStatusUpdateRequest): ApiResult<OrderItemDto> = withContext(Dispatchers.IO) {
        safeApiCall { api.updateItemStatus(orderId, itemId, request) }
    }

    suspend fun updateItemStatus(orderId: Int, itemId: Int, itemStatus: ItemStatus): ApiResult<OrderItemDto> = withContext(Dispatchers.IO) {
        safeApiCall { api.updateItemStatus(orderId, itemId, ItemStatusUpdateRequest(itemStatus = itemStatus)) }
    }

    suspend fun syncPendingOrders() = withContext(Dispatchers.IO) {
        val unsynced = orderDao.getUnsyncedOrders()
        for (order in unsynced) {
            try {
                orderDao.markAsSynced(order.id)
            } catch (_: Exception) {}
        }
    }
}