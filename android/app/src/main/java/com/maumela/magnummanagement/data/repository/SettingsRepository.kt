package com.maumela.magnummanagement.data.repository

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.local.PreferencesStore
import com.maumela.magnummanagement.data.model.SettingsDto
import com.maumela.magnummanagement.data.model.SettingsUpdateRequest
import com.maumela.magnummanagement.data.model.Theme
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.safeApiCall

/**
 * Settings are applied on the device IMMEDIATELY (so a theme change is instant) and then
 * sent to the server. If the server call fails, the device keeps the choice and the UI can
 * say it was saved on this device only.
 */
class SettingsRepository(
    private val api: ApiService,
    private val preferences: PreferencesStore,
) {
    /** Loads the server copy and brings the local preferences in line with it. */
    suspend fun getSettings(): ApiResult<SettingsDto> {
        val result = safeApiCall { api.getSettings() }
        if (result is ApiResult.Success) {
            preferences.setTheme(result.data.theme)
            preferences.setNotificationsEnabled(result.data.notificationEnabled)
        }
        return result
    }

    suspend fun setTheme(theme: Theme): ApiResult<SettingsDto> {
        preferences.setTheme(theme)
        return safeApiCall { api.updateSettings(SettingsUpdateRequest(theme = theme)) }
    }

    /**
     * Turning notifications OFF clears the FCM token on the server (blank = clear).
     * Turning them ON registers the given token, if there is one.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean, fcmToken: String? = null): ApiResult<SettingsDto> {
        preferences.setNotificationsEnabled(enabled)
        val body = if (enabled) {
            SettingsUpdateRequest(notificationEnabled = true, fcmToken = fcmToken)
        } else {
            SettingsUpdateRequest(notificationEnabled = false, fcmToken = "")
        }
        return safeApiCall { api.updateSettings(body) }
    }

    /** Called when Firebase issues a (new) device token. */
    suspend fun registerFcmToken(token: String): ApiResult<SettingsDto> =
        safeApiCall { api.updateSettings(SettingsUpdateRequest(fcmToken = token)) }
}