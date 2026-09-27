package com.bahoque.gastonomo.feature.home

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.MovementPage
import com.bahoque.gastonomo.core.application.state.PeriodFilter
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.application.usecase.auth.LoadProfile
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoans
import com.bahoque.gastonomo.core.application.usecase.movement.LoadPeriodMovements
import com.bahoque.gastonomo.core.application.usecase.movement.LoadRecentMovements
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.application.usecase.settings.ObserveDeviceSettings
import com.bahoque.gastonomo.core.application.usecase.settings.SetHideAmounts
import com.bahoque.gastonomo.core.testing.fake.FakeAuthGateway
import com.bahoque.gastonomo.core.testing.fake.FakeCurrencyGateway
import com.bahoque.gastonomo.core.testing.fake.FakeDeviceSettingsStore
import com.bahoque.gastonomo.core.testing.fake.FakeLoanGateway
import com.bahoque.gastonomo.core.testing.fake.FakeMovementGateway
import com.bahoque.gastonomo.core.testing.fake.FakeSettingsGateway
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import com.bahoque.gastonomo.core.testing.fake.SampleFinance

/**
 * Dobles de todo lo que necesita Inicio, para montarla sin red ni dispositivo.
 *
 * Por defecto devuelve los datos de ejemplo del prototipo; cada test cambia lo que le importa.
 */
internal class TestHomeDependencies(
    movementsFailure: ApiError? = null,
    ratesFailure: ApiError? = null,
) : HomeDependencies {

    /** El ojo de la tarjeta de balance, para comprobar que se guarda en el dispositivo. */
    val device = FakeDeviceSettingsStore()

    override val periodFilter = PeriodFilter.startingOn(FixedClock.PROTOTYPE_TODAY)
    override val messages = TransientMessages()

    // Dos instancias: el doble de movimientos pagina, y las dos consultas de Inicio son
    // independientes. Con una sola, la segunda se llevaria la pagina vacia.
    private val page = MovementPage(SampleFinance.movements, null)
    private val currencyGateway = FakeCurrencyGateway(failure = ratesFailure)

    private val periodGateway = FakeMovementGateway(listOf(page), movementsFailure)

    /** Cuantas veces se pidio el periodo, para contar recargas. */
    val periodCalls: Int get() = periodGateway.listCalls

    override val loadPeriodMovements = LoadPeriodMovements(periodGateway)
    override val loadRecentMovements =
        LoadRecentMovements(FakeMovementGateway(listOf(page), movementsFailure))
    override val loadLoans = LoadLoans(FakeLoanGateway())
    override val loadExchangeRates = LoadExchangeRates(currencyGateway)
    override val loadSettings = LoadSettings(FakeSettingsGateway())
    override val loadProfile = LoadProfile(FakeAuthGateway())
    override val setHideAmounts = SetHideAmounts(device)
    override val observeDeviceSettings = ObserveDeviceSettings(device)
    override val clock = FixedClock()
}
