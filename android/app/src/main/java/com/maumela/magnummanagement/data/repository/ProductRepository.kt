package com.maumela.magnummanagement.data.repository

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.api.ApiErrorParser
import com.maumela.magnummanagement.data.local.dao.ProductDao
import com.maumela.magnummanagement.data.local.entity.ProductEntity
import com.maumela.magnummanagement.data.model.*
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.CacheFilters
import com.maumela.magnummanagement.utils.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class ProductRepository(
    private val api: ApiService,
    private val productDao: ProductDao
) {
    suspend fun getProducts(filter: ListingFilter? = null): ApiResult<Cached<List<ProductDto>>> = withContext(Dispatchers.IO) {
        val query = filter?.query?.trim()
        val category = filter?.category?.trim()
        val sector = filter?.sector?.name
        val minPrice = filter?.minPrice?.toString()
        val maxPrice = filter?.maxPrice?.toString()
        val minRating = filter?.minRating
        val availability = if (filter?.availableOnly == true) true else null

        if (filter?.ownerOnly == true) {
            return@withContext when (val result = safeApiCall { api.getProducts(query, category, sector, minPrice, maxPrice, minRating, availability, "me") }) {
                is ApiResult.Success -> ApiResult.Success(Cached(result.data, isStale = false))
                is ApiResult.Failure -> ApiResult.Failure(result.error)
            }
        }

        val result = fetchWithCache(
            fetch = { api.getProducts(query, category, sector, minPrice, maxPrice, minRating, availability, null) },
            save = { dtos ->
                val entities = dtos.map { dto ->
                    ProductEntity(
                        id = dto.id,
                        sellerId = dto.sellerId,
                        sellerName = dto.sellerName,
                        name = dto.name,
                        description = dto.description,
                        price = dto.price.toString(),
                        category = dto.category,
                        sector = dto.sector.name,
                        imageUrl = dto.imageUrl,
                        stockQuantity = dto.stockQuantity,
                        availability = dto.availability,
                        rating = dto.rating,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        isSynced = true
                    )
                }
                if (filter?.hasFilters == true) {
                    productDao.upsertAll(entities)
                } else {
                    productDao.syncFromServer(entities)
                }
            },
            readCache = {
                productDao.getAll().map { entity ->
                    ProductDto(
                        id = entity.id,
                        sellerId = entity.sellerId,
                        sellerName = entity.sellerName,
                        name = entity.name,
                        description = entity.description,
                        price = BigDecimal(entity.price),
                        category = entity.category,
                        sector = Sector.valueOf(entity.sector),
                        imageUrl = entity.imageUrl,
                        stockQuantity = entity.stockQuantity,
                        availability = entity.availability,
                        rating = entity.rating,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt
                    )
                }
            }
        )

        if (result is ApiResult.Success && filter != null) {
            val filtered = CacheFilters.filterProducts(result.data.data, filter)
            ApiResult.Success(result.data.copy(data = filtered))
        } else {
            result
        }
    }

    suspend fun getProduct(id: Int): ApiResult<Cached<ProductDto>> = withContext(Dispatchers.IO) {
        fetchWithCache(
            fetch = { api.getProduct(id) },
            save = { dto ->
                val entity = ProductEntity(
                    id = dto.id,
                    sellerId = dto.sellerId,
                    sellerName = dto.sellerName,
                    name = dto.name,
                    description = dto.description,
                    price = dto.price.toString(),
                    category = dto.category,
                    sector = dto.sector.name,
                    imageUrl = dto.imageUrl,
                    stockQuantity = dto.stockQuantity,
                    availability = dto.availability,
                    rating = dto.rating,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    isSynced = true
                )
                productDao.upsert(entity)
            },
            readCache = {
                productDao.getById(id)?.let { entity ->
                    ProductDto(
                        id = entity.id,
                        sellerId = entity.sellerId,
                        sellerName = entity.sellerName,
                        name = entity.name,
                        description = entity.description,
                        price = BigDecimal(entity.price),
                        category = entity.category,
                        sector = Sector.valueOf(entity.sector),
                        imageUrl = entity.imageUrl,
                        stockQuantity = entity.stockQuantity,
                        availability = entity.availability,
                        rating = entity.rating,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt
                    )
                }
            }
        )
    }

    suspend fun createProduct(request: ProductRequest): ApiResult<ProductDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.createProduct(request)
            val entity = ProductEntity(
                id = response.id,
                sellerId = response.sellerId,
                sellerName = response.sellerName,
                name = response.name,
                description = response.description,
                price = response.price.toString(),
                category = response.category,
                sector = response.sector.name,
                imageUrl = response.imageUrl,
                stockQuantity = response.stockQuantity,
                availability = response.availability,
                rating = response.rating,
                createdAt = response.createdAt,
                updatedAt = response.updatedAt,
                isSynced = true
            )
            productDao.upsert(entity)
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Failure(ApiErrorParser.parse(e))
        }
    }

    suspend fun updateProduct(id: Int, request: ProductUpdateRequest): ApiResult<ProductDto> = withContext(Dispatchers.IO) {
        safeApiCall { api.updateProduct(id, request) }
    }

    suspend fun deleteProduct(id: Int): ApiResult<MessageResponse> = withContext(Dispatchers.IO) {
        val result = safeApiCall { api.deleteProduct(id) }
        if (result is ApiResult.Success) {
            cacheWrite { productDao.deleteById(id) }
        }
        result
    }

    suspend fun syncPendingProducts() = withContext(Dispatchers.IO) {
        val unsynced = productDao.getUnsyncedProducts()
        for (product in unsynced) {
            try {
                productDao.markAsSynced(product.id)
            } catch (_: Exception) {}
        }
    }
}
