package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.ApiService
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.local.PreferencesStore
import com.maumela.magnummanagement.data.model.SettingsDto
import com.maumela.magnummanagement.data.model.SettingsUpdateRequest
import com.maumela.magnummanagement.data.model.Theme
import com.maumela.magnummanagement.data.repository.EnergyRepository
import com.maumela.magnummanagement.data.repository.SettingsRepository
import com.maumela.magnummanagement.utils.ApiResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsAndEnergyRepositoryTest {
    private val api = mockk<ApiService>()
    private val preferences = mockk<PreferencesStore>(relaxed = true)
    private val settings = SettingsRepository(api, preferences)
    private val energy = EnergyRepository(api)

    // Verifies the theme is applied on the device first, then sent to the server.
    @Test
    fun setTheme_savesLocallyAndSendsOnlyTheme() = runTest {
        coEvery { api.updateSettings(SettingsUpdateRequest(theme = Theme.DARK)) } returns SettingsDto(true, Theme.DARK, null)
        assertTrue(settings.setTheme(Theme.DARK) is ApiResult.Success)
        coVerify { preferences.setTheme(Theme.DARK) }
    }

    // Verifies the theme choice survives a server failure (it stays saved on the device).
    @Test
    fun setTheme_serverDown_keepsLocalChoiceAndReportsFailure() = runTest {
        coEvery { api.updateSettings(any()) } throws UnknownHostException()
        val failure = settings.setTheme(Theme.LIGHT) as ApiResult.Failure
        assertEquals(ErrorKind.NO_INTERNET, failure.error.kind)
        coVerify { preferences.setTheme(Theme.LIGHT) }
    }

    // Verifies switching notifications OFF clears the FCM token on the server (blank = clear).
    @Test
    fun disableNotifications_clearsServerToken() = runTest {
        val clear = SettingsUpdateRequest(notificationEnabled = false, fcmToken = "")
        coEvery { api.updateSettings(clear) } returns SettingsDto(false, Theme.SYSTEM, null)
        assertTrue(settings.setNotificationsEnabled(false) is ApiResult.Success)
        coVerify { preferences.setNotificationsEnabled(false) }
        coVerify { api.updateSettings(clear) }
    }

    // Verifies switching notifications ON registers the device token.
    @Test
    fun enableNotifications_registersToken() = runTest {
        val enable = SettingsUpdateRequest(notificationEnabled = true, fcmToken = "token-abc")
        coEvery { api.updateSettings(enable) } returns SettingsDto(true, Theme.SYSTEM, "token-abc")
        assertTrue(settings.setNotificationsEnabled(true, "token-abc") is ApiResult.Success)
        coVerify { api.updateSettings(enable) }
    }

    // Verifies loading settings brings the local preferences in line with the server copy.
    @Test
    fun getSettings_syncsLocalPreferences() = runTest {
        coEvery { api.getSettings() } returns SettingsDto(false, Theme.DARK, null)
        settings.getSettings()
        coVerify { preferences.setTheme(Theme.DARK) }
        coVerify { preferences.setNotificationsEnabled(false) }
    }

    // Verifies a new Firebase token is sent on its own, without touching other settings.
    @Test
    fun registerFcmToken_sendsOnlyTheToken() = runTest {
        coEvery { api.updateSettings(SettingsUpdateRequest(fcmToken = "new-token")) } returns
                SettingsDto(true, Theme.SYSTEM, "new-token")
        assertTrue(settings.registerFcmToken("new-token") is ApiResult.Success)
    }

    // Verifies an unsupported energy range is rejected on the device (the server allows 7, 30, 90).
    @Test
    fun energyUsage_invalidDays_isRejectedLocally() = runTest {
        val failure = energy.getUsage(15) as ApiResult.Failure
        assertEquals(ErrorKind.BAD_REQUEST, failure.error.kind)
        coVerify(exactly = 0) { api.getEnergyUsage(any()) }
    }

    // Verifies a supported range is passed to the server unchanged.
    @Test
    fun energyUsage_validDays_callsServer() = runTest {
        for (days in EnergyRepository.ALLOWED_DAYS) {
            coEvery { api.getEnergyUsage(days) } throws httpException(500, "{}")
            assertTrue(energy.getUsage(days) is ApiResult.Failure)
            coVerify { api.getEnergyUsage(days) }
        }
    }
}