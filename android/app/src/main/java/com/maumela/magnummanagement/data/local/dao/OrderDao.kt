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

    @Query("DELETE FROM orders")
    abstract suspend fun clear()

    /** "My orders" is always fetched in full, so the cache is replaced atomically. */
    @Transaction
    open suspend fun replaceAll(items: List<OrderEntity>) {
        clear()
        upsertAll(items)
    }
}