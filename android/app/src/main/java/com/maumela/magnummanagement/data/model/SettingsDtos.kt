package com.maumela.magnummanagement.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsDto(
    @SerialName("notification_enabled") val notificationEnabled: Boolean,
    val theme: Theme,
    @SerialName("fcm_token") val fcmToken: String? = null,
)

/**
 * Partial update. Null fields are omitted (unchanged).
 * To CLEAR the FCM token on the server, send an empty string: the backend stores it as null.
 */
@Serializable
data class SettingsUpdateRequest(
    @SerialName("notification_enabled") val notificationEnabled: Boolean? = null,
    val theme: Theme? = null,
    @SerialName("fcm_token") val fcmToken: String? = null,
)