package com.maumela.magnummanagement.data.repository

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.local.dao.ServiceDao
import com.maumela.magnummanagement.data.local.toDto
import com.maumela.magnummanagement.data.local.toEntity
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.model.ServiceRequest
import com.maumela.magnummanagement.data.model.ServiceUpdateRequest
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.CacheFilters
import com.maumela.magnummanagement.utils.map
import com.maumela.magnummanagement.utils.safeApiCall

class ServiceRepository(
    private val api: ApiService,
    private val dao: ServiceDao,
) {
    suspend fun getServices(filter: ListingFilter = ListingFilter()): ApiResult<Cached<List<ServiceDto>>> =
        fetchWithCache(
            allowFallback = !filter.ownerOnly,
            fetch = {
                api.getServices(
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
                    filter.ownerOnly -> Unit
                    filter.hasFilters -> dao.upsertAll(items.map { it.toEntity() })
                    else -> dao.replaceAll(items.map { it.toEntity() })
                }
            },
            readCache = {
                dao.getAll()
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.toDto() }
                    ?.let { CacheFilters.filterServices(it, filter) }
            },
        )

    suspend fun getService(id: Int): ApiResult<Cached<ServiceDto>> =
        fetchWithCache(
            fetch = { api.getService(id) },
            save = { dao.upsert(it.toEntity()) },
            readCache = { dao.getById(id)?.toDto() },
        )

    suspend fun createService(request: ServiceRequest): ApiResult<ServiceDto> {
        val result = safeApiCall { api.createService(request) }
        if (result is ApiResult.Success) cacheWrite { dao.upsert(result.data.toEntity()) }
        return result
    }

    suspend fun updateService(id: Int, request: ServiceUpdateRequest): ApiResult<ServiceDto> {
        val result = safeApiCall { api.updateService(id, request) }
        if (result is ApiResult.Success) cacheWrite { dao.upsert(result.data.toEntity()) }
        return result
    }

    suspend fun deleteService(id: Int): ApiResult<Unit> {
        val result = safeApiCall { api.deleteService(id) }
        if (result is ApiResult.Success) cacheWrite { dao.deleteById(id) }
        return result.map { }
    }
}