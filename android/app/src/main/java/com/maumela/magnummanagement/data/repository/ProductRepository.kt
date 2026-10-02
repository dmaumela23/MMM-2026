package com.maumela.magnummanagement.data.repository

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.local.dao.ProductDao
import com.maumela.magnummanagement.data.local.toDto
import com.maumela.magnummanagement.data.local.toEntity
import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.ProductRequest
import com.maumela.magnummanagement.data.model.ProductUpdateRequest
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.CacheFilters
import com.maumela.magnummanagement.utils.map
import com.maumela.magnummanagement.utils.safeApiCall

class ProductRepository(
    private val api: ApiService,
    private val dao: ProductDao,
) {
    /** Server first (it applies the filters in SQL); the local cache only when the network fails. */
    suspend fun getProducts(filter: ListingFilter = ListingFilter()): ApiResult<Cached<List<ProductDto>>> =
        fetchWithCache(
            allowFallback = !filter.ownerOnly, // "my products" needs the server
            fetch = {
                api.getProducts(
                    query = filter.query?.trim()?.takeIf { it.isNotEmpty() },
                    category = filter.category?.trim()?.takeIf { it.isNotEmpty() },
                    sector = filter.sector?.name,
                    minPrice = filter.minPrice?.toPlainString(),
                    maxPrice = filter.maxPrice?.toPlainString(),
                    minRating = filter.minRating,
                    availability = if (filter.availableOnly) true else null,
                    owner = if (filter.ownerOnly) "me" else null,
                )
            },
            save = { items ->
                when {
                    filter.ownerOnly -> Unit // a partial list must not overwrite the shared cache
                    filter.hasFilters -> dao.upsertAll(items.map { it.toEntity() })
                    else -> dao.replaceAll(items.map { it.toEntity() }) // full list: replace atomically
                }
            },
            readCache = {
                dao.getAll()
                    .takeIf { it.isNotEmpty() } // empty cache = nothing to show, report the real error
                    ?.map { it.toDto() }
                    ?.let { CacheFilters.filterProducts(it, filter) }
            },
        )

    suspend fun getProduct(id: Int): ApiResult<Cached<ProductDto>> =
        fetchWithCache(
            fetch = { api.getProduct(id) },
            save = { dao.upsert(it.toEntity()) },
            readCache = { dao.getById(id)?.toDto() },
        )

    suspend fun createProduct(request: ProductRequest): ApiResult<ProductDto> {
        val result = safeApiCall { api.createProduct(request) }
        if (result is ApiResult.Success) cacheWrite { dao.upsert(result.data.toEntity()) }
        return result
    }

    suspend fun updateProduct(id: Int, request: ProductUpdateRequest): ApiResult<ProductDto> {
        val result = safeApiCall { api.updateProduct(id, request) }
        if (result is ApiResult.Success) cacheWrite { dao.upsert(result.data.toEntity()) }
        return result
    }

    suspend fun deleteProduct(id: Int): ApiResult<Unit> {
        val result = safeApiCall { api.deleteProduct(id) }
        if (result is ApiResult.Success) cacheWrite { dao.deleteById(id) }
        return result.map { }
    }
}