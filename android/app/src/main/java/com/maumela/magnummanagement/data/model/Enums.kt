package com.maumela.magnummanagement.data.model

import kotlinx.serialization.Serializable

// Names match the backend values exactly (they are sent and received as these strings).

@Serializable
enum class UserRole { CUSTOMER, PROVIDER, BUSINESS }

@Serializable
enum class Sector { DIGITAL, SERVICE, ENERGY }

@Serializable
enum class OrderStatus { PENDING, PROCESSING, COMPLETED, CANCELLED }

@Serializable
enum class ItemStatus { PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED }

@Serializable
enum class ItemType { PRODUCT, SERVICE }

@Serializable
enum class Theme { LIGHT, DARK, SYSTEM }

@Serializable
enum class EnergyStatus { NORMAL, HIGH, OVER_LIMIT }