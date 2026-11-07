package com.maumela.magnummanagement.data.repository

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.api.ApiErrorParser
import com.maumela.magnummanagement.data.local.dao.ServiceDao
import com.maumela.magnummanagement.data.local.entity.ServiceEntity
import com.maumela.magnummanagement.data.model.*
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.CacheFilters
import com.maumela.magnummanagement.utils.safeApiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class ServiceRepository(
    private val api: ApiService,
    private val serviceDao: ServiceDao
) {
    suspend fun getServices(filter: ListingFilter? = null): ApiResult<Cached<List<ServiceDto>>> = withContext(Dispatchers.IO) {
        val query = filter?.query?.trim()
        val category = filter?.category?.trim()
        val sector = filter?.sector?.name
        val minPrice = filter?.minPrice?.toString()
        val maxPrice = filter?.maxPrice?.toString()
        val minRating = filter?.minRating
        val availability = if (filter?.availableOnly == true) true else null

        if (filter?.ownerOnly == true) {
            return@withContext when (val result = safeApiCall { api.getServices(query, category, sector, minPrice, maxPrice, minRating, availability, "me") }) {
                is ApiResult.Success -> ApiResult.Success(Cached(result.data, isStale = false))
                is ApiResult.Failure -> ApiResult.Failure(result.error)
            }
        }

        val result = fetchWithCache(
            fetch = { api.getServices(query, category, sector, minPrice, maxPrice, minRating, availability, null) },
            save = { dtos ->
                val entities = dtos.map { dto ->
                    ServiceEntity(
                        id = dto.id,
                        providerId = dto.providerId,
                        providerName = dto.providerName,
                        name = dto.name,
                        description = dto.description,
                        price = dto.price.toString(),
                        category = dto.category,
                        sector = dto.sector.name,
                        availability = dto.availability,
                        rating = dto.rating,
                        deliveryDays = dto.deliveryDays,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        isSynced = true
                    )
                }
                if (filter?.hasFilters == true) {
                    serviceDao.upsertAll(entities)
                } else {
                    serviceDao.syncFromServer(entities)
                }
            },
            readCache = {
                serviceDao.getAll().map { entity ->
                    ServiceDto(
                        id = entity.id,
                        providerId = entity.providerId,
                        providerName = entity.providerName,
                        name = entity.name,
                        description = entity.description,
                        price = BigDecimal(entity.price),
                        category = entity.category,
                        sector = Sector.valueOf(entity.sector),
                        availability = entity.availability,
                        rating = entity.rating,
                        deliveryDays = entity.deliveryDays,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt
                    )
                }
            }
        )

        if (result is ApiResult.Success && filter != null) {
            val filtered = CacheFilters.filterServices(result.data.data, filter)
            ApiResult.Success(result.data.copy(data = filtered))
        } else {
            result
        }
    }

    suspend fun getService(id: Int): ApiResult<Cached<ServiceDto>> = withContext(Dispatchers.IO) {
        fetchWithCache(
            fetch = { api.getService(id) },
            save = { dto ->
                val entity = ServiceEntity(
                    id = dto.id,
                    providerId = dto.providerId,
                    providerName = dto.providerName,
                    name = dto.name,
                    description = dto.description,
                    price = dto.price.toString(),
                    category = dto.category,
                    sector = dto.sector.name,
                    availability = dto.availability,
                    rating = dto.rating,
                    deliveryDays = dto.deliveryDays,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt,
                    isSynced = true
                )
                serviceDao.upsert(entity)
            },
            readCache = {
                serviceDao.getById(id)?.let { entity ->
                    ServiceDto(
                        id = entity.id,
                        providerId = entity.providerId,
                        providerName = entity.providerName,
                        name = entity.name,
                        description = entity.description,
                        price = BigDecimal(entity.price),
                        category = entity.category,
                        sector = Sector.valueOf(entity.sector),
                        availability = entity.availability,
                        rating = entity.rating,
                        deliveryDays = entity.deliveryDays,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt
                    )
                }
            }
        )
    }

    suspend fun createService(request: ServiceRequest): ApiResult<ServiceDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.createService(request)
            val entity = ServiceEntity(
                id = response.id,
                providerId = response.providerId,
                providerName = response.providerName,
                name = response.name,
                description = response.description,
                price = response.price.toString(),
                category = response.category,
                sector = response.sector.name,
                availability = response.availability,
                rating = response.rating,
                deliveryDays = response.deliveryDays,
                createdAt = response.createdAt,
                updatedAt = response.updatedAt,
                isSynced = true
            )
            serviceDao.upsert(entity)
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Failure(ApiErrorParser.parse(e))
        }
    }

    suspend fun updateService(id: Int, request: ServiceUpdateRequest): ApiResult<ServiceDto> = withContext(Dispatchers.IO) {
        safeApiCall { api.updateService(id, request) }
    }

    suspend fun deleteService(id: Int): ApiResult<MessageResponse> = withContext(Dispatchers.IO) {
        val result = safeApiCall { api.deleteService(id) }
        if (result is ApiResult.Success) {
            cacheWrite { serviceDao.deleteById(id) }
        }
        result
    }

    suspend fun syncPendingServices() = withContext(Dispatchers.IO) {
        val unsynced = serviceDao.getUnsyncedServices()
        for (service in unsynced) {
            try {
                serviceDao.markAsSynced(service.id)
            } catch (_: Exception) {}
        }
    }
}
