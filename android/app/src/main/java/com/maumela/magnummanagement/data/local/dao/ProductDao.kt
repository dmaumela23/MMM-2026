package com.maumela.magnummanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.maumela.magnummanagement.data.local.entity.ProductEntity

@Dao
abstract class ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    abstract suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    abstract suspend fun getById(id: Int): ProductEntity?

    @Upsert
    abstract suspend fun upsert(item: ProductEntity)

    @Upsert
    abstract suspend fun upsertAll(items: List<ProductEntity>)

    @Query("DELETE FROM products WHERE id = :id")
    abstract suspend fun deleteById(id: Int)

    @Query("DELETE FROM products")
    abstract suspend fun clear()

    /** Fetch only products created or edited locally that haven't synced to the server yet. */
    @Query("SELECT * FROM products WHERE isSynced = 0")
    abstract suspend fun getUnsyncedProducts(): List<ProductEntity>

    /** Mark a specific product as successfully synced with the backend server. */
    @Query("UPDATE products SET isSynced = 1 WHERE id = :productId")
    abstract suspend fun markAsSynced(productId: Int)

    /** Replaces the cache with server items while preserving local unsynced mutations. */
    @Transaction
    open suspend fun syncFromServer(serverItems: List<ProductEntity>) {
        val unsynced = getUnsyncedProducts()
        clear()
        upsertAll(serverItems)
        if (unsynced.isNotEmpty()) {
            upsertAll(unsynced) // Re-apply local pending products on top so they aren't lost
        }
    }
}