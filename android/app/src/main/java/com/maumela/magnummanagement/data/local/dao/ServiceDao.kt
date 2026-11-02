package com.maumela.magnummanagement.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.maumela.magnummanagement.data.local.entity.ServiceEntity

@Dao
abstract class ServiceDao {
    @Query("SELECT * FROM services ORDER BY id DESC")
    abstract suspend fun getAll(): List<ServiceEntity>

    @Query("SELECT * FROM services WHERE id = :id")
    abstract suspend fun getById(id: Int): ServiceEntity?

    @Upsert
    abstract suspend fun upsert(item: ServiceEntity)

    @Upsert
    abstract suspend fun upsertAll(items: List<ServiceEntity>)

    @Query("DELETE FROM services WHERE id = :id")
    abstract suspend fun deleteById(id: Int)

    @Query("DELETE FROM services")
    abstract suspend fun clear()

    @Transaction
    open suspend fun replaceAll(items: List<ServiceEntity>) {
        clear()
        upsertAll(items)
    }
}