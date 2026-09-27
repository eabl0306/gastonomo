package com.bahoque.gastonomo.feature.movements

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.MovementPage
import com.bahoque.gastonomo.core.application.state.PeriodFilter
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.application.usecase.category.LoadCategories
import com.bahoque.gastonomo.core.application.usecase.currency.LoadCurrencyCatalog
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.movement.CreateMovement
import com.bahoque.gastonomo.core.application.usecase.movement.LoadPeriodMovements
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.testing.fake.FakeCategoryGateway
import com.bahoque.gastonomo.core.testing.fake.FakeCurrencyGateway
import com.bahoque.gastonomo.core.testing.fake.FakeMovementGateway
import com.bahoque.gastonomo.core.testing.fake.FakeSettingsGateway
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import com.bahoque.gastonomo.core.testing.fake.SampleFinance

/**
 * Dobles de todo lo que necesitan las dos pantallas de este modulo.
 *
 * Por defecto devuelve los datos de ejemplo del prototipo; cada test cambia lo que le importa.
 */
internal class TestMovementsDependencies(
    movementsFailure: ApiError? = null,
    createFailure: ApiError? = null,
    replay: Boolean = false,
    ratesFailure: ApiError? = null,
) : MovementsDependencies {

    override val periodFilter = PeriodFilter.startingOn(FixedClock.PROTOTYPE_TODAY)
    override val messages = TransientMessages()

    /** El doble de creacion, para mirar con que claves se llamo. */
    val movementGateway = FakeMovementGateway(
        pages = listOf(MovementPage(SampleFinance.movements, null)),
        failure = createFailure ?: movementsFailure,
        replay = replay,
    )

    private val listGateway = FakeMovementGateway(
        pages = listOf(MovementPage(SampleFinance.movements, null)),
        failure = movementsFailure,
    )

    /** Cuantas veces se pidio la lista del periodo, para contar recargas. */
    val listCalls: Int get() = listGateway.listCalls

    override val loadPeriodMovements = LoadPeriodMovements(listGateway)
    override val createMovement = CreateMovement(movementGateway)
    override val loadCategories = LoadCategories(FakeCategoryGateway())
    override val loadCurrencyCatalog = LoadCurrencyCatalog(FakeCurrencyGateway())
    override val loadExchangeRates = LoadExchangeRates(FakeCurrencyGateway(failure = ratesFailure))
    override val loadSettings = LoadSettings(FakeSettingsGateway())
    override val clock = FixedClock()
}
