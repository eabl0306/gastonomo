package com.bahoque.gastonomo.core.domain.money

import java.math.BigDecimal

/**
 * Los cuatro campos que lleva todo registro de dinero: lo que teclo el usuario
 * ([amount] y [currency]) y su equivalencia en la moneda principal vigente al registrar
 * ([base] y [baseCurrency]).
 *
 * La equivalencia se guarda porque el usuario puede editar la tasa a mano, y cambiar la
 * moneda principal despues no debe tocar ningun registro.
 */
data class MonetaryAmount(
    val amount: BigDecimal,
    val currency: CurrencyCode,
    val base: BigDecimal,
    val baseCurrency: CurrencyCode,
) {
    /** El importe tal como lo teclo el usuario. */
    val original: Money get() = Money(amount, currency)

    /** La equivalencia guardada. */
    val equivalent: Money get() = Money(base, baseCurrency)

    /** True si hay que ensenar el importe original como texto secundario. */
    fun isForeignTo(display: CurrencyCode): Boolean = currency != display

    companion object {
        /**
         * Precision para recalcular la equivalencia al corregir un importe rechazado. Se conserva
         * la tasa que tenia el registro: es el mismo gasto del mismo dia con otro numero, no un
         * registro nuevo al cambio de hoy.
         */
        val CORRECTION_PRECISION: java.math.MathContext =
            java.math.MathContext(20, java.math.RoundingMode.HALF_UP)

        /**
         * Registro en la moneda principal: la equivalencia es el propio importe. Es el
         * caso en el que la API deja omitir `base` y `baseCurrency` los dos juntos.
         */
        fun single(amount: BigDecimal, currency: CurrencyCode): MonetaryAmount =
            MonetaryAmount(amount, currency, amount, currency)

        fun of(original: Money, equivalent: Money): MonetaryAmount = MonetaryAmount(
            amount = original.amount,
            currency = original.currency,
            base = equivalent.amount,
            baseCurrency = equivalent.currency,
        )
    }
}
