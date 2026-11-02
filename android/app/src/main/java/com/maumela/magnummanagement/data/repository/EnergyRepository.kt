package com.maumela.magnummanagement.data.repository

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.api.AppError
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.model.EnergyPlansResponse
import com.maumela.magnummanagement.data.model.EnergyReportsResponse
import com.maumela.magnummanagement.data.model.EnergyUsageResponse
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.safeApiCall

/** ALL energy data is SIMULATED by the backend. It is not cached: it is always fetched fresh. */
class EnergyRepository(private val api: ApiService) {

    suspend fun getUsage(days: Int = 30): ApiResult<EnergyUsageResponse> {
        if (days !in ALLOWED_DAYS) {
            return ApiResult.Failure(AppError(ErrorKind.BAD_REQUEST, "Choose 7, 30 or 90 days."))
        }
        return safeApiCall { api.getEnergyUsage(days) }
    }

    suspend fun getPlans(): ApiResult<EnergyPlansResponse> = safeApiCall { api.getEnergyPlans() }

    suspend fun getReports(): ApiResult<EnergyReportsResponse> = safeApiCall { api.getEnergyReports() }

    companion object {
        val ALLOWED_DAYS = listOf(7, 30, 90)
    }
}