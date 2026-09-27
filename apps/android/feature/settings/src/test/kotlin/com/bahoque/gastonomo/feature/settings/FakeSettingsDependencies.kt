package com.bahoque.gastonomo.feature.settings

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.application.port.CurrencyGateway
import com.bahoque.gastonomo.core.application.port.SettingsGateway
import com.bahoque.gastonomo.core.application.usecase.auth.ChangePassword
import com.bahoque.gastonomo.core.application.usecase.auth.LoadProfile
import com.bahoque.gastonomo.core.application.usecase.auth.SetUpLocalLock
import com.bahoque.gastonomo.core.application.usecase.auth.SignOut
import com.bahoque.gastonomo.core.application.usecase.category.LoadCategories
import com.bahoque.gastonomo.core.application.usecase.currency.LoadCurrencyCatalog
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.application.usecase.settings.ObserveDeviceSettings
import com.bahoque.gastonomo.core.application.usecase.settings.UpdateSettings
import com.bahoque.gastonomo.core.application.usecase.sync.CorrectPendingWrite
import com.bahoque.gastonomo.core.application.usecase.sync.DiscardPendingWrite
import com.bahoque.gastonomo.core.application.usecase.sync.ObserveConnection
import com.bahoque.gastonomo.core.application.usecase.sync.ObservePendingWrites
import com.bahoque.gastonomo.core.application.usecase.sync.RetryPendingWrites
import com.bahoque.gastonomo.core.testing.fake.FakeAuthGateway
import com.bahoque.gastonomo.core.testing.fake.FakeCategoryGateway
import com.bahoque.gastonomo.core.testing.fake.FakeCurrencyGateway
import com.bahoque.gastonomo.core.testing.fake.FakeDeviceSettingsStore
import com.bahoque.gastonomo.core.testing.fake.FakeLocalLock
import com.bahoque.gastonomo.core.testing.fake.FakeNetworkMonitor
import com.bahoque.gastonomo.core.testing.fake.FakePendingWritesGateway
import com.bahoque.gastonomo.core.testing.fake.FakeSessionStore
import com.bahoque.gastonomo.core.testing.fake.FakeSettingsGateway

/**
 * Dobles de todo lo que necesita Ajustes. Cada puerta se puede sustituir para probar el
 * camino de error sin tocar las demas.
 */
@Suppress("LongParameterList") // Un parametro por puerta: es lo que deja fallar solo una.
internal class FakeSettingsDependencies(
    settingsGateway: SettingsGateway = FakeSettingsGateway(),
    currencyGateway: CurrencyGateway = FakeCurrencyGateway(),
    categoryGateway: CategoryGateway = FakeCategoryGateway(),
    val authGateway: FakeAuthGateway = FakeAuthGateway(),
    val localLock: FakeLocalLock = FakeLocalLock(),
    val deviceSettings: FakeDeviceSettingsStore = FakeDeviceSettingsStore(),
    val pendingGateway: FakePendingWritesGateway = FakePendingWritesGateway(),
    val network: FakeNetworkMonitor = FakeNetworkMonitor(),
) : SettingsDependencies {
    override val loadSettings = LoadSettings(settingsGateway)
    override val updateSettings = UpdateSettings(settingsGateway)
    override val loadProfile = LoadProfile(authGateway)
    override val loadCurrencyCatalog = LoadCurrencyCatalog(currencyGateway)
    override val loadExchangeRates = LoadExchangeRates(currencyGateway)
    override val signOut = SignOut(authGateway, FakeSessionStore(), localLock)
    override val changePassword = ChangePassword(authGateway)
    override val setUpLocalLock = SetUpLocalLock(localLock, deviceSettings)
    override val observeDeviceSettings = ObserveDeviceSettings(deviceSettings)
    override val loadCategories = LoadCategories(categoryGateway)
    override val observePendingWrites = ObservePendingWrites(pendingGateway)
    override val correctPendingWrite = CorrectPendingWrite(pendingGateway)
    override val discardPendingWrite = DiscardPendingWrite(pendingGateway)
    override val retryPendingWrites = RetryPendingWrites(pendingGateway)
    override val observeConnection = ObserveConnection(network)

    companion object {
        /** Las dependencias con todo en verde, que es lo que usan las capturas. */
        fun working(): FakeSettingsDependencies = FakeSettingsDependencies()

        /** Las dependencias que fallan al guardar con el 503 del catalogo vacio. */
        fun catalogNotReady(): FakeSettingsDependencies = FakeSettingsDependencies(
            settingsGateway = FakeSettingsGateway(failure = ApiError.ServiceUnavailable),
        )

        /** Las dependencias sin red: la sincronizacion sale Pausada. */
        fun offline(): FakeSettingsDependencies = FakeSettingsDependencies(
            settingsGateway = FakeSettingsGateway(failure = ApiError.Network),
            currencyGateway = FakeCurrencyGateway(failure = ApiError.Network),
        )
    }
}
