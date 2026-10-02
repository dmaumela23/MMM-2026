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

    /** Replaces the whole cache atomically (used after an unfiltered fetch). */
    @Transaction
    open suspend fun replaceAll(items: List<ProductEntity>) {
        clear()
        upsertAll(items)
    }
}