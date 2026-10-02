package com.maumela.magnummanagement.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    @SerialName("full_name") val fullName: String,
    val email: String,
    val phone: String? = null,
    val password: String,
    @SerialName("confirm_password") val confirmPassword: String,
    val role: UserRole,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class UserDto(
    val id: Int,
    @SerialName("full_name") val fullName: String,
    val email: String,
    val phone: String? = null,
    val role: UserRole,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("energy_plan_id") val energyPlanId: Int? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: UserDto,
)

/** Partial profile update: null fields are omitted from the JSON and left unchanged. */
@Serializable
data class UserUpdateRequest(
    @SerialName("full_name") val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
)

@Serializable
data class PasswordChangeRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

/** Used for {"detail": "..."} success bodies such as "Deleted" and "Password updated". */
@Serializable
data class MessageResponse(val detail: String)