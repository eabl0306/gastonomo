package com.bahoque.gastonomo.feature.export

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.period.Period
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import com.bahoque.gastonomo.core.testing.fake.SampleFinance

/** Las columnas de cada conjunto, copiadas del README del diseno. */
internal val MOVEMENT_HEADERS = listOf(
    "Fecha",
    "Tipo",
    "Categoría",
    "Descripción",
    "Monto original",
    "Moneda",
    "Monto (COP)",
)

internal val LOAN_HEADERS = listOf(
    "Fecha",
    "Persona",
    "Dirección",
    "Nota",
    "Monto original",
    "Moneda",
    "Monto (COP)",
    "Abonado (COP)",
    "Saldo pendiente (COP)",
    "Estado",
)

internal val PAYMENT_HEADERS = listOf(
    "Fecha",
    "Persona",
    "Concepto",
    "Monto original",
    "Moneda",
    "Monto (COP)",
)

/** Los textos de la exportacion en castellano, tal como los resuelve la pantalla. */
internal val spanishLabels = ExportLabels(
    movements = ExportTableLabels(
        name = "Movimientos",
        headers = MOVEMENT_HEADERS,
    ),
    loans = ExportTableLabels(
        name = "Préstamos",
        headers = LOAN_HEADERS,
    ),
    payments = ExportTableLabels(
        name = "Abonos",
        headers = PAYMENT_HEADERS,
    ),
    income = "Ingreso",
    expense = "Gasto",
    owedToMe = "Me debe",
    iOwe = "Debo",
    paid = "Pagado",
    partial = "Parcial",
    pending = "Pendiente",
    payment = "Abono",
)

/** El estado cargado con los datos de ejemplo del prototipo. */
internal fun loadedExportState(): ExportUiState = ExportUiState(
    loading = false,
    today = FixedClock.PROTOTYPE_TODAY,
    period = Period.currentMonth(FixedClock.PROTOTYPE_TODAY),
    movements = SampleFinance.movements,
    loans = SampleFinance.loans,
    displayCurrency = CurrencyCode.COP,
    rates = SampleFinance.rates,
)
