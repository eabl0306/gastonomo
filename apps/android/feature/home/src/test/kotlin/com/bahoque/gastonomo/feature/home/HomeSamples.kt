package com.bahoque.gastonomo.feature.home

import com.bahoque.gastonomo.core.application.state.PeriodSelection
import com.bahoque.gastonomo.core.domain.analysis.BalanceTrend
import com.bahoque.gastonomo.core.domain.analysis.CategoryBreakdown
import com.bahoque.gastonomo.core.domain.analysis.PeriodSummary
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import java.math.BigDecimal

/**
 * El estado de Inicio con los datos del prototipo, para las capturas y los tests de pantalla.
 * Se calcula con el dominio de verdad: si el calculo cambia, la captura cambia con el.
 */
internal object HomeSamples {

    private val today = FixedClock.PROTOTYPE_TODAY
    private val selection = PeriodSelection.currentMonth(today)
    private val rates = SampleFinance.rates
    private val display = CurrencyCode.COP

    val loaded = HomeUiState(
        today = today,
        selection = selection,
        displayCurrency = display,
        rates = rates,
        user = SampleAuth.user,
        loading = false,
        content = HomeContent(
            summary = PeriodSummary.of(SampleFinance.movements, display, rates),
            trend = BalanceTrend.of(SampleFinance.movements, selection.period, today, display, rates),
            breakdown = CategoryBreakdown.ofExpenses(SampleFinance.movements, display, rates),
            recent = SampleFinance.movements,
            owedToMe = LoanTotal(BigDecimal("50000"), 1),
            iOwe = LoanTotal(BigDecimal("600000"), 1),
        ),
    )

    /** Un periodo sin nada: todos los estados vacios del diseno a la vez. */
    val empty = HomeUiState(
        today = today,
        selection = selection,
        user = SampleAuth.user,
        loading = false,
    )
}
