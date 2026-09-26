package com.bahoque.gastonomo.core.domain.money

import java.math.BigDecimal

/**
 * La regla central de multimoneda. Todo lo que se ensena, se suma o se exporta pasa por
 * aqui: totales, saldos de prestamo, graficos y exportaciones.
 *
 * Es la funcion `disp` del prototipo, con las cuatro ramas en el mismo orden:
 * 1. El registro ya esta en la moneda que se ensena: se usa su importe.
 * 2. La equivalencia guardada ya esta en esa moneda: se usa, y con ella la tasa que el
 *    usuario pudo editar a mano.
 * 3. Se convierte la equivalencia guardada con las tasas de hoy.
 * 4. Si eso no se puede, se convierte el importe original; y si tampoco, se devuelve la
 *    equivalencia en bruto antes que no ensenar nada.
 */
object DisplayedAmount {

    /** El valor de [value] en [display], en bruto y sin redondear. */
    fun of(value: MonetaryAmount, display: CurrencyCode, rates: ExchangeRates): BigDecimal = when {
        value.currency == display -> value.amount
        value.baseCurrency == display -> value.base
        else -> rates.convert(value.base, value.baseCurrency, display)
            ?: rates.convert(value.amount, value.currency, display)
            ?: value.base
    }

    /** Lo mismo, ya envuelto en [Money]. */
    fun money(value: MonetaryAmount, display: CurrencyCode, rates: ExchangeRates): Money =
        Money(of(value, display, rates), display)

    /** Suma una lista de registros en la moneda que se ensena. */
    fun sum(
        values: Iterable<MonetaryAmount>,
        display: CurrencyCode,
        rates: ExchangeRates,
    ): Money = values.fold(Money.zero(display)) { total, value ->
        total + money(value, display, rates)
    }
}
