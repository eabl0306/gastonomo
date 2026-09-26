package com.bahoque.gastonomo.core.domain.money

import java.math.BigDecimal

/**
 * La caja de conversion de los tres formularios de creacion, como estado del dominio.
 *
 * Aparece cuando el registro se teclea en una moneda distinta de la principal. Propone la
 * equivalencia con la tasa vigente y **deja que el usuario la edite**: lo que quede se
 * guarda como `base`, asi que se respeta su tasa. Cambiar la moneda del formulario borra la
 * equivalencia editada, y eso lo consigue quien tiene el estado pasando [editedBase] a null.
 *
 * ```
 * val suggestion = ConversionSuggestion.of(
 *     entered = Money(amount, state.currency),
 *     display = state.displayCurrency,
 *     rates = state.rates,
 *     editedBase = state.baseRaw?.let(AmountInput::parse),
 * )
 * if (suggestion.isValid) save(suggestion.money)
 * ```
 *
 * @property editedBase lo que el usuario escribio en la caja, o null para usar la sugerida.
 */
data class ConversionSuggestion(
    val entered: Money,
    val display: CurrencyCode,
    val suggestedRate: BigDecimal?,
    val editedBase: BigDecimal?,
) {
    /** True si hay que ensenar la caja: el registro no esta en la moneda principal. */
    val isForeign: Boolean get() = entered.currency != display

    /** True si el usuario cambio la equivalencia y el enlace "Usar sugerida" tiene sentido. */
    val isEdited: Boolean get() = editedBase != null

    /** La equivalencia que propone la tasa vigente, o null si no hay tasa. */
    val suggestedBase: BigDecimal?
        get() = suggestedRate?.multiply(entered.amount, ExchangeRates.PRECISION)

    /**
     * La equivalencia que se guardara. En la moneda principal es el propio importe; en otra
     * es lo que escribio el usuario o, si no escribio nada, la sugerida.
     */
    val base: BigDecimal
        get() = when {
            !isForeign -> entered.amount
            editedBase != null -> editedBase
            else -> suggestedBase ?: BigDecimal.ZERO
        }

    /** La tasa que resulta de lo que escribio el usuario: 1 moneda del registro = X principal. */
    val ownRate: BigDecimal?
        get() = if (entered.amount.signum() > 0 && base.signum() > 0) {
            base.divide(entered.amount, ExchangeRates.PRECISION)
        } else {
            null
        }

    /** El boton de guardar se habilita con importe mayor que 0 y equivalencia mayor que 0. */
    val isValid: Boolean get() = entered.amount.signum() > 0 && base.signum() > 0

    /** Los cuatro campos que se mandan a la API, ya redondeados a su moneda. */
    val money: MonetaryAmount
        get() = MonetaryAmount.of(
            original = entered.rounded(),
            equivalent = Money(base, display).rounded(),
        )

    companion object {
        /** Calcula la sugerencia con las tasas vigentes. */
        fun of(
            entered: Money,
            display: CurrencyCode,
            rates: ExchangeRates,
            editedBase: BigDecimal? = null,
        ): ConversionSuggestion = ConversionSuggestion(
            entered = entered,
            display = display,
            suggestedRate = rates.rateOf(entered.currency, display),
            editedBase = editedBase,
        )
    }
}
