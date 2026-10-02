package com.maumela.magnummanagement.data.local

import com.maumela.magnummanagement.data.local.entity.OrderEntity
import com.maumela.magnummanagement.data.local.entity.ProductEntity
import com.maumela.magnummanagement.data.local.entity.ServiceEntity
import com.maumela.magnummanagement.data.model.OrderStatus
import com.maumela.magnummanagement.data.model.OrderSummaryDto
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.Sector
import com.maumela.magnummanagement.data.model.ServiceDto
import java.math.BigDecimal

// DTO <-> cache entity. Money round-trips through plain text, so "1499.00" stays "1499.00".

fun ProductDto.toEntity() = ProductEntity(
    id = id, sellerId = sellerId, sellerName = sellerName, name = name, description = description,
    price = price.toPlainString(), category = category, sector = sector.name, imageUrl = imageUrl,
    stockQuantity = stockQuantity, availability = availability, rating = rating,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun ProductEntity.toDto() = ProductDto(
    id = id, sellerId = sellerId, sellerName = sellerName, name = name, description = description,
    price = BigDecimal(price), category = category, sector = Sector.valueOf(sector), imageUrl = imageUrl,
    stockQuantity = stockQuantity, availability = availability, rating = rating,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun ServiceDto.toEntity() = ServiceEntity(
    id = id, providerId = providerId, providerName = providerName, name = name, description = description,
    price = price.toPlainString(), category = category, sector = sector.name, availability = availability,
    rating = rating, deliveryDays = deliveryDays, createdAt = createdAt, updatedAt = updatedAt,
)

fun ServiceEntity.toDto() = ServiceDto(
    id = id, providerId = providerId, providerName = providerName, name = name, description = description,
    price = BigDecimal(price), category = category, sector = Sector.valueOf(sector), availability = availability,
    rating = rating, deliveryDays = deliveryDays, createdAt = createdAt, updatedAt = updatedAt,
)

fun OrderSummaryDto.toEntity() = OrderEntity(
    id = id, status = status.name, totalAmount = totalAmount.toPlainString(),
    createdAt = createdAt, itemCount = itemCount, firstItemName = firstItemName,
)

fun OrderEntity.toDto() = OrderSummaryDto(
    id = id, status = OrderStatus.valueOf(status), totalAmount = BigDecimal(totalAmount),
    createdAt = createdAt, itemCount = itemCount, firstItemName = firstItemName,
)