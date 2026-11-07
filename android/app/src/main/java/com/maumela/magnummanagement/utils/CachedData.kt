package com.maumela.magnummanagement.utils

data class CachedData<out T>(
    val data: T,
    val isStale: Boolean = false
)