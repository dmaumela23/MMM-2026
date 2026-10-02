package com.maumela.magnummanagement.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cached order SUMMARY (the list). Order details are always loaded from the server. */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: Int,
    val status: String,
    val totalAmount: String,
    val createdAt: String,
    val itemCount: Int,
    val firstItemName: String?,
)