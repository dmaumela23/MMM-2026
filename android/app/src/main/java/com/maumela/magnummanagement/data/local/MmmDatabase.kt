package com.maumela.magnummanagement.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.maumela.magnummanagement.data.local.dao.OrderDao
import com.maumela.magnummanagement.data.local.dao.ProductDao
import com.maumela.magnummanagement.data.local.dao.ServiceDao
import com.maumela.magnummanagement.data.local.entity.OrderEntity
import com.maumela.magnummanagement.data.local.entity.ProductEntity
import com.maumela.magnummanagement.data.local.entity.ServiceEntity

/**
 * LOCAL CACHE ONLY. The server (PostgreSQL) is the source of truth; this database is
 * rebuilt from API responses, shown only when the network fails, and wiped on logout.
 */
@Database(
    entities = [ProductEntity::class, ServiceEntity::class, OrderEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MmmDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun serviceDao(): ServiceDao
    abstract fun orderDao(): OrderDao

    companion object {
        fun create(context: Context): MmmDatabase =
            Room.databaseBuilder(context.applicationContext, MmmDatabase::class.java, "mmm_cache.db")
                // Safe for a cache: if the schema changes, rebuild it from the server.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}