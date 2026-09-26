package com.bahoque.gastonomo.feature.movements

import com.bahoque.gastonomo.core.application.state.PeriodSelection
import com.bahoque.gastonomo.core.domain.analysis.DayGrouping
import com.bahoque.gastonomo.core.domain.analysis.PeriodSummary
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import com.bahoque.gastonomo.core.testing.fake.SampleFinance

/**
 * El estado de las dos pantallas con los datos del prototipo, para las capturas y los tests.
 * Se calcula con el dominio de verdad: si el calculo cambia, la captura cambia con el.
 */
internal object MovementsSamples {

    private val today = FixedClock.PROTOTYPE_TODAY
    private val selection = PeriodSelection.currentMonth(today)
    private val display = CurrencyCode.COP
    private val rates = SampleFinance.rates

    val list = MovementsUiState(
        today = today,
        selection = selection,
        displayCurrency = display,
        rates = rates,
        summary = PeriodSummary.of(SampleFinance.movements, display, rates),
        groups = DayGrouping.of(SampleFinance.movements, display, rates),
        loading = false,
    )

    val emptyList = MovementsUiState(today = today, selection = selection, loading = false)

    val form = NewMovementUiState(
        today = today,
        kind = MovementKind.OUT,
        date = today,
        categories = SampleFinance.categories,
        currencies = SampleFinance.currencies,
        rates = rates,
        loading = false,
    )

    /** Un gasto listo para guardar. */
    val filledForm = form.copy(
        amountRaw = "186400",
        selectedCategoryId = SampleFinance.food.id,
        note = "Mercado",
    )

    /** El mismo gasto en dolares: sale la caja de conversion. */
    val foreignForm = form.copy(
        amountRaw = "140",
        currency = CurrencyCode.USD,
        selectedCategoryId = SampleFinance.food.id,
    )
}
