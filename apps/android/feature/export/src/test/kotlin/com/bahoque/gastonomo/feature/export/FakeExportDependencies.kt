package com.bahoque.gastonomo.feature.export

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.Clock
import com.bahoque.gastonomo.core.application.port.CurrencyGateway
import com.bahoque.gastonomo.core.application.port.LoanGateway
import com.bahoque.gastonomo.core.application.port.MovementGateway
import com.bahoque.gastonomo.core.application.port.MovementPage
import com.bahoque.gastonomo.core.application.port.SettingsGateway
import com.bahoque.gastonomo.core.application.state.PeriodFilter
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoans
import com.bahoque.gastonomo.core.application.usecase.movement.LoadPeriodMovements
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.common.DispatcherProvider
import com.bahoque.gastonomo.core.testing.fake.FakeCurrencyGateway
import com.bahoque.gastonomo.core.testing.fake.FakeLoanGateway
import com.bahoque.gastonomo.core.testing.fake.FakeMovementGateway
import com.bahoque.gastonomo.core.testing.fake.FakeSettingsGateway
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.CoroutineDispatcher

/** Dobles de Exportar. El dispatcher es el del test, para que todo sea determinista. */
@Suppress("LongParameterList") // Un parametro por puerta: es lo que deja fallar solo una.
internal class FakeExportDependencies(
    dispatcher: CoroutineDispatcher,
    movementGateway: MovementGateway = FakeMovementGateway(),
    loanGateway: LoanGateway = FakeLoanGateway(),
    settingsGateway: SettingsGateway = FakeSettingsGateway(),
    currencyGateway: CurrencyGateway = FakeCurrencyGateway(),
    override val clock: Clock = FixedClock(),
    override val periodFilter: PeriodFilter = PeriodFilter.startingOn(FixedClock().today()),
) : ExportDependencies {
    override val loadPeriodMovements = LoadPeriodMovements(movementGateway)
    override val loadLoans = LoadLoans(loanGateway)
    override val loadSettings = LoadSettings(settingsGateway)
    override val loadExchangeRates = LoadExchangeRates(currencyGateway)
    override val dispatchers: DispatcherProvider = TestDispatchers(dispatcher)

    companion object {
        fun working(dispatcher: CoroutineDispatcher): FakeExportDependencies =
            FakeExportDependencies(
                dispatcher = dispatcher,
                movementGateway = FakeMovementGateway(
                    pages = listOf(MovementPage(SampleFinance.movements, null)),
                ),
            )

        /** Sin red: la pantalla ensena su estado de error con reintento. */
        fun offline(dispatcher: CoroutineDispatcher): FakeExportDependencies =
            FakeExportDependencies(
                dispatcher = dispatcher,
                movementGateway = FakeMovementGateway(failure = ApiError.Network),
                loanGateway = FakeLoanGateway(failure = ApiError.Network),
            )
    }
}

/** Un proveedor que devuelve siempre el dispatcher del test. */
internal class TestDispatchers(private val dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
    override val main: CoroutineDispatcher get() = dispatcher
}
