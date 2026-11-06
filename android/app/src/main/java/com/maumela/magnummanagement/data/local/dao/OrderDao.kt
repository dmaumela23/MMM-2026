package com.maumela.magnummanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.maumela.magnummanagement.data.local.entity.OrderEntity

@Dao
abstract class OrderDao {
    @Query("SELECT * FROM orders ORDER BY id DESC")
    abstract suspend fun getAll(): List<OrderEntity>

    @Upsert
    abstract suspend fun upsertAll(items: List<OrderEntity>)

    @Upsert
    abstract suspend fun upsert(item: OrderEntity)

    @Query("DELETE FROM orders")
    abstract suspend fun clear()

    /** Fetch only orders that were created locally but haven't synced to the server yet. */
    @Query("SELECT * FROM orders WHERE isSynced = 0")
    abstract suspend fun getUnsyncedOrders(): List<OrderEntity>

    /** Mark a specific order as successfully synced with the server. */
    @Query("UPDATE orders SET isSynced = 1 WHERE id = :orderId")
    abstract suspend fun markAsSynced(orderId: Int)

    /** "My orders" is fetched in full, but we must preserve any local unsynced orders so they aren't wiped out by a server refresh. */
    @Transaction
    open suspend fun syncFromServer(serverItems: List<OrderEntity>) {
        val unsynced = getUnsyncedOrders()
        clear()
        upsertAll(serverItems)
        if (unsynced.isNotEmpty()) {
            upsertAll(unsynced) // Re-apply local pending orders on top
        }
    }
}