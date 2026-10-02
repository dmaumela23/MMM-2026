package com.maumela.magnummanagement.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val sellerId: Int,
    val sellerName: String,
    val name: String,
    val description: String,
    val price: String, // BigDecimal stored as plain text, e.g. "1499.00"
    val category: String,
    val sector: String,
    val imageUrl: String?,
    val stockQuantity: Int,
    val availability: Boolean,
    val rating: Double,
    val createdAt: String,
    val updatedAt: String,
)