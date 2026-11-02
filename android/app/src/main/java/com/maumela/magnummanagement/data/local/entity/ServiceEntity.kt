package com.maumela.magnummanagement.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: Int,
    val providerId: Int,
    val providerName: String,
    val name: String,
    val description: String,
    val price: String,
    val category: String,
    val sector: String,
    val availability: Boolean,
    val rating: Double,
    val deliveryDays: Int,
    val createdAt: String,
    val updatedAt: String,
)