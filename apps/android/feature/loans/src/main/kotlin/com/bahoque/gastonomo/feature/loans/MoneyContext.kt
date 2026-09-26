package com.bahoque.gastonomo.feature.loans

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates

/**
 * La moneda principal, las tasas de hoy y el catalogo del sheet de moneda.
 *
 * Las cuatro pantallas de esta tanda necesitan las tres cosas: sin la moneda principal y
 * las tasas no se puede calcular ni un saldo, porque la API nunca devuelve importes
 * convertidos. Se cargan una vez por pantalla y se pasan de aqui al sheet de abono, que no
 * vuelve a pedirlas.
 */
@Immutable
data class MoneyContext(
    val display: CurrencyCode = CurrencyCode.COP,
    val rates: ExchangeRates = ExchangeRates.EMPTY,
    val currencies: List<CurrencyInfo> = emptyList(),
)
