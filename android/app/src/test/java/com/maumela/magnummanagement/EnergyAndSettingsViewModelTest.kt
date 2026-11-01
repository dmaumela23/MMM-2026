package com.maumela.magnummanagement

import com.maumela.magnummanagement.data.api.AppError
import com.maumela.magnummanagement.data.api.ErrorKind
import com.maumela.magnummanagement.data.local.PreferencesStore
import com.maumela.magnummanagement.data.model.EnergyPlanDto
import com.maumela.magnummanagement.data.model.EnergyPlansResponse
import com.maumela.magnummanagement.data.model.EnergyReportsResponse
import com.maumela.magnummanagement.data.model.EnergyStatus
import com.maumela.magnummanagement.data.model.EnergyUsageResponse
import com.maumela.magnummanagement.data.model.SettingsDto
import com.maumela.magnummanagement.data.model.Theme
import com.maumela.magnummanagement.data.repository.EnergyRepository
import com.maumela.magnummanagement.data.repository.SettingsRepository
import com.maumela.magnummanagement.utils.ApiResult
import com.maumela.magnummanagement.utils.UiState
import com.maumela.magnummanagement.viewmodel.EnergyViewModel
import com.maumela.magnummanagement.viewmodel.SettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EnergyViewModelFlagsTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<EnergyRepository>()
    private val plan = EnergyPlanDto(1, "Home Standard", "", BigDecimal("2.60"), BigDecimal("150.00"), 700)

    private fun usage(days: Int) = EnergyUsageResponse(
        days = days, currentKwh = 12.0, estimatedMonthlyKwh = 360.0, estimatedCost = BigDecimal("1086.00"),
        status = EnergyStatus.NORMAL, plan = plan, history = emptyList(), isSimulated = true,
        disclaimer = "SIMULATION: not a real meter.",
    )

    private fun stubAll() {
        coEvery { repository.getUsage(30) } returns ApiResult.Success(usage(30))
        coEvery { repository.getUsage(7) } returns ApiResult.Success(usage(7))
        coEvery { repository.getPlans() } returns
            ApiResult.Success(EnergyPlansResponse(1, listOf(plan), isSimulated = true, disclaimer = "SIMULATION"))
        coEvery { repository.getReports() } returns
            ApiResult.Success(EnergyReportsResponse(emptyList(), isSimulated = true, disclaimer = "SIMULATION"))
    }

    // Verifies live (simulated) energy data is NOT flagged as "saved data", so the offline banner never shows wrongly.
    @Test
    fun simulatedData_isNotMarkedStale() {
        stubAll()
        val vm = EnergyViewModel(repository)
        val usage = vm.state.value.usage as UiState.Success
        assertFalse(usage.isStale)
        assertFalse((vm.state.value.plans as UiState.Success).isStale)
        assertFalse((vm.state.value.reports as UiState.Success).isStale)
        assertTrue(usage.data.isSimulated)
    }

    // Verifies choosing 7 days reloads the usage with that range.
    @Test
    fun setDays_validRange_reloadsUsage() {
        stubAll()
        val vm = EnergyViewModel(repository)
        vm.setDays(7)
        assertEquals(7, vm.state.value.selectedDays)
        assertEquals(7, (vm.state.value.usage as UiState.Success).data.days)
        coVerify { repository.getUsage(7) }
    }

    // Verifies a failed request becomes a friendly error state for that section only.
    @Test
    fun failure_becomesErrorStatePerSection() {
        val error = AppError(ErrorKind.NO_INTERNET, "No internet connection.")
        coEvery { repository.getUsage(any()) } returns ApiResult.Failure(error)
        coEvery { repository.getPlans() } returns
            ApiResult.Success(EnergyPlansResponse(null, emptyList(), isSimulated = true, disclaimer = "SIMULATION"))
        coEvery { repository.getReports() } returns ApiResult.Failure(error)
        val vm = EnergyViewModel(repository)
        assertEquals(UiState.Failure(error), vm.state.value.usage)
        assertTrue(vm.state.value.plans is UiState.Success)
        assertEquals(UiState.Failure(error), vm.state.value.reports)
    }
}

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<SettingsRepository>()
    private val preferences = mockk<PreferencesStore>()

    private fun viewModel(): SettingsViewModel {
        every { preferences.theme } returns flowOf(Theme.SYSTEM)
        every { preferences.notificationsEnabled } returns flowOf(true)
        coEvery { repository.getSettings() } returns ApiResult.Success(SettingsDto(true, Theme.SYSTEM, null))
        return SettingsViewModel(repository, preferences)
    }

    // Verifies the server's saved settings are loaded when the screen opens.
    @Test
    fun init_loadsServerSettings() {
        val vm = viewModel()
        assertTrue(vm.state.value.settings is UiState.Success)
        assertEquals(Theme.SYSTEM, vm.state.value.theme)
        assertTrue(vm.state.value.notificationsEnabled)
    }

    // Verifies choosing a theme is applied immediately and sent to the server.
    @Test
    fun setTheme_appliesAndSaves() {
        coEvery { repository.setTheme(Theme.DARK) } returns ApiResult.Success(SettingsDto(true, Theme.DARK, null))
        val vm = viewModel()
        vm.setTheme(Theme.DARK)
        assertEquals(Theme.DARK, vm.state.value.theme)
        assertFalse(vm.state.value.isSaving)
        coVerify { repository.setTheme(Theme.DARK) }
    }

    // Verifies the theme choice stays on the device when the server cannot be reached, and a message is shown.
    @Test
    fun setTheme_serverDown_keepsLocalChoice() {
        coEvery { repository.setTheme(Theme.LIGHT) } returns
            ApiResult.Failure(AppError(ErrorKind.NO_INTERNET, "No internet connection."))
        val vm = viewModel()
        vm.setTheme(Theme.LIGHT)
        assertEquals(Theme.LIGHT, vm.state.value.theme)
        assertEquals("No internet connection.", vm.state.value.errorMessage)
        vm.clearError()
        assertEquals(null, vm.state.value.errorMessage)
    }

    // Verifies switching notifications off sends no token, and switching on sends the device token.
    @Test
    fun notifications_toggleSendsTheRightToken() {
        coEvery { repository.setNotificationsEnabled(false, null) } returns
            ApiResult.Success(SettingsDto(false, Theme.SYSTEM, null))
        coEvery { repository.setNotificationsEnabled(true, "token-abc") } returns
            ApiResult.Success(SettingsDto(true, Theme.SYSTEM, "token-abc"))
        val vm = viewModel()

        vm.setNotificationsEnabled(false)
        assertFalse(vm.state.value.notificationsEnabled)
        vm.setNotificationsEnabled(true, "token-abc")
        assertTrue(vm.state.value.notificationsEnabled)

        coVerify { repository.setNotificationsEnabled(false, null) }
        coVerify { repository.setNotificationsEnabled(true, "token-abc") }
    }

    // Verifies a blank Firebase token is never sent to the server.
    @Test
    fun registerFcmToken_blankIsIgnored() {
        val vm = viewModel()
        vm.registerFcmToken("   ")
        coVerify(exactly = 0) { repository.registerFcmToken(any()) }
    }

    // Verifies a real token is registered.
    @Test
    fun registerFcmToken_sendsTheToken() {
        coEvery { repository.registerFcmToken("new-token") } returns
            ApiResult.Success(SettingsDto(true, Theme.SYSTEM, "new-token"))
        val vm = viewModel()
        vm.registerFcmToken("new-token")
        coVerify { repository.registerFcmToken("new-token") }
    }
}
