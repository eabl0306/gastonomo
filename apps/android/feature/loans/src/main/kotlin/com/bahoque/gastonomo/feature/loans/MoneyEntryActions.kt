package com.bahoque.gastonomo.feature.loans

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.core.domain.money.CurrencyCode

/** Lo que el bloque de importe le pide a quien tiene el estado. */
@Immutable
data class MoneyEntryActions(
    val onAmount: (String) -> Unit,
    val onCurrency: (CurrencyCode) -> Unit,
    val onBase: (String?) -> Unit,
    val onQuery: (String) -> Unit,
    val onCurrencySheet: (Boolean) -> Unit,
)

/**
 * Construye las cinco acciones a partir de la unica que necesita el ViewModel: aplicar una
 * transformacion al trozo de estado del importe.
 *
 * Asi cada ViewModel expone un solo `editEntry` en vez de cinco setters que solo envuelven
 * un `copy`.
 */
fun moneyEntryActions(edit: (MoneyEntryState.() -> MoneyEntryState) -> Unit) = MoneyEntryActions(
    onAmount = { raw -> edit { withAmount(raw) } },
    onCurrency = { code -> edit { withCurrency(code) } },
    onBase = { raw -> edit { withBase(raw) } },
    onQuery = { query -> edit { withQuery(query) } },
    onCurrencySheet = { open -> edit { withCurrencySheet(open) } },
)
