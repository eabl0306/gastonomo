package com.bahoque.gastonomo.feature.settings

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** El estado y las acciones de Ajustes, con dobles de los casos de uso. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `it loads the profile, the settings, the catalog and the category count`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.working())
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(SampleAuth.user, state.user)
        assertEquals(CurrencyCode.COP, state.settings.displayCurrency)
        assertEquals(SampleFinance.currencies.size, state.currencies.size)
        assertEquals(SampleFinance.categories.size, state.categoryCount)
        assertEquals(CloudSyncState.SYNCED, state.cloudSync)
        assertEquals(RatesIndicator.Ready(SampleFinance.rates.date), state.rates)
        assertTrue(!state.loading)
    }

    @Test
    fun `without network the sync indicator is paused and the rates are offline`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.offline())
        advanceUntilIdle()
        assertEquals(CloudSyncState.PAUSED, viewModel.state.value.cloudSync)
        assertEquals(RatesIndicator.Offline, viewModel.state.value.rates)
    }

    @Test
    fun `changing the currency closes the sheet and announces the new one`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.working())
        advanceUntilIdle()
        viewModel.showSheet(SettingsSheet.CURRENCY)
        viewModel.onCurrencySelected(CurrencyCode.USD)
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(CurrencyCode.USD, state.settings.displayCurrency)
        assertNull(state.sheet)
        assertEquals(SettingsMessage.CurrencyChanged(CurrencyCode.USD), state.message)
    }

    @Test
    fun `picking the currency already in use says nothing`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.working())
        advanceUntilIdle()
        viewModel.onCurrencySelected(CurrencyCode.COP)
        advanceUntilIdle()
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `a 503 while saving is told as the catalog not being ready`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.catalogNotReady())
        advanceUntilIdle()
        viewModel.onCurrencySelected(CurrencyCode.EUR)
        advanceUntilIdle()
        assertEquals(SettingsMessage.CatalogNotReady, viewModel.state.value.message)
    }

    @Test
    fun `changing the language stores it and closes the sheet`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.working())
        advanceUntilIdle()
        viewModel.showSheet(SettingsSheet.LANGUAGE)
        assertEquals(SettingsSheet.LANGUAGE, viewModel.state.value.sheet)
        viewModel.onLanguageSelected(AppLanguage.ENGLISH)
        advanceUntilIdle()
        assertEquals(AppLanguage.ENGLISH, viewModel.state.value.settings.language)
        assertNull(viewModel.state.value.sheet)
    }

    @Test
    fun `turning the lock on asks for a pin and stores it`() = runTest {
        val dependencies = FakeSettingsDependencies.working()
        val viewModel = SettingsViewModel(dependencies)
        advanceUntilIdle()
        viewModel.onLockToggled(true)
        assertEquals(SettingsSheet.PIN, viewModel.state.value.sheet)
        viewModel.onPinChanged("12a345")
        assertEquals("1234", viewModel.state.value.pin)
        viewModel.confirmPin()
        advanceUntilIdle()
        assertEquals(SettingsMessage.LockEnabled, viewModel.state.value.message)
        assertTrue(dependencies.deviceSettings.current.lockEnabled)
        assertTrue(dependencies.localLock.verifyPin("1234"))
    }

    @Test
    fun `a short pin cannot be stored`() = runTest {
        val dependencies = FakeSettingsDependencies.working()
        val viewModel = SettingsViewModel(dependencies)
        advanceUntilIdle()
        viewModel.onLockToggled(true)
        viewModel.onPinChanged("12")
        viewModel.confirmPin()
        advanceUntilIdle()
        assertEquals(SettingsMessage.SaveFailed, viewModel.state.value.message)
        assertTrue(!dependencies.deviceSettings.current.lockEnabled)
    }

    @Test
    fun `turning the lock off clears the pin without asking`() = runTest {
        val dependencies = FakeSettingsDependencies.working()
        val viewModel = SettingsViewModel(dependencies)
        advanceUntilIdle()
        viewModel.onLockToggled(false)
        advanceUntilIdle()
        assertEquals(SettingsMessage.LockDisabled, viewModel.state.value.message)
        assertTrue(!dependencies.localLock.hasPin())
    }

    @Test
    fun `signing out revokes the session and tells the caller`() = runTest {
        val dependencies = FakeSettingsDependencies.working()
        val viewModel = SettingsViewModel(dependencies)
        advanceUntilIdle()
        var done = false
        viewModel.signOut { done = true }
        advanceUntilIdle()
        assertTrue(done)
        assertTrue(dependencies.authGateway.calls.contains("logout"))
    }

    @Test
    fun `the message is cleared once the toast has been shown`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.working())
        advanceUntilIdle()
        viewModel.onLockToggled(false)
        advanceUntilIdle()
        viewModel.onMessageShown()
        assertNull(viewModel.state.value.message)
    }
}
