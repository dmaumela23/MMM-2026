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

    /** Fetch only services created or edited locally that haven't synced to the server yet. */
    @Query("SELECT * FROM services WHERE isSynced = 0")
    abstract suspend fun getUnsyncedServices(): List<ServiceEntity>

    /** Mark a specific service as successfully synced with the backend server. */
    @Query("UPDATE services SET isSynced = 1 WHERE id = :serviceId")
    abstract suspend fun markAsSynced(serviceId: Int)

    /** Replaces the cache with server items while preserving local unsynced mutations. */
    @Transaction
    open suspend fun syncFromServer(serverItems: List<ServiceEntity>) {
        val unsynced = getUnsyncedServices()
        clear()
        upsertAll(serverItems)
        if (unsynced.isNotEmpty()) {
            upsertAll(unsynced) // Re-apply local pending services on top so they aren't lost
        }
    }
}