package com.bahoque.gastonomo.core.domain.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Un importe con su moneda. Siempre sobre [BigDecimal]: la API manda los importes como
 * cadena justamente para que nadie los meta en un Double y pierda centavos.
 */
data class Money(val amount: BigDecimal, val currency: CurrencyCode) : Comparable<Money> {

    val isZero: Boolean get() = amount.signum() == 0
    val isNegative: Boolean get() = amount.signum() < 0

    operator fun plus(other: Money): Money = Money(amount + sameCurrency(other), currency)

    operator fun minus(other: Money): Money = Money(amount - sameCurrency(other), currency)

    operator fun unaryMinus(): Money = Money(amount.negate(), currency)

    fun abs(): Money = Money(amount.abs(), currency)

    /** Redondea a los decimales de la moneda. Es lo que se manda a la API. */
    fun rounded(): Money =
        Money(amount.setScale(CurrencyDecimals.of(currency), RoundingMode.HALF_UP), currency)

    override fun compareTo(other: Money): Int = amount.compareTo(sameCurrency(other))

    private fun sameCurrency(other: Money): BigDecimal {
        require(other.currency == currency) {
            "Cannot combine ${currency.value} with ${other.currency.value}"
        }
        return other.amount
    }

    companion object {
        fun zero(currency: CurrencyCode): Money = Money(BigDecimal.ZERO, currency)

        /** Lee un importe tal como lo manda la API: una cadena decimal. */
        fun parse(raw: String, currency: CurrencyCode): Money = Money(BigDecimal(raw), currency)

        /** Devuelve null si la cadena no es un decimal. */
        fun parseOrNull(raw: String, currency: CurrencyCode): Money? =
            runCatching { parse(raw, currency) }.getOrNull()
    }
}
