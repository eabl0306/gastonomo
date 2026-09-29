package com.bahoque.gastonomo.core.domain.money

import java.math.BigDecimal

/**
 * La caja de conversion de los tres formularios de creacion, como estado del dominio.
 *
 * Aparece cuando el registro se teclea en una moneda distinta de la principal. Propone la tasa
 * vigente y **deja que el usuario la edite**; la equivalencia sale de multiplicar el importe por
 * la tasa que quede, y es la que se guarda como `base`. Cambiar la moneda del formulario borra
 * la tasa editada, y eso lo consigue quien tiene el estado pasando [editedRate] a null.
 *
 * **Lo que se edita es la tasa, no la equivalencia.** Antes era al reves, y era pedirle al
 * usuario la cuenta ya hecha: quien recibe dolares sabe a como esta el cambio, no cuanto suman
 * ciento cuarenta dolares en pesos. Con la tasa, la equivalencia la calcula la aplicacion.
 *
 * ```
 * val suggestion = ConversionSuggestion.of(
 *     entered = Money(amount, state.currency),
 *     display = state.displayCurrency,
 *     rates = state.rates,
 *     editedRate = state.rateRaw?.let(AmountInput::parse),
 * )
 * if (suggestion.isValid) save(suggestion.money)
 * ```
 *
 * @property editedRate la tasa que escribio el usuario, o null para usar la sugerida.
 */
data class ConversionSuggestion(
    val entered: Money,
    val display: CurrencyCode,
    val suggestedRate: BigDecimal?,
    val editedRate: BigDecimal?,
) {
    /** True si hay que ensenar la caja: el registro no esta en la moneda principal. */
    val isForeign: Boolean get() = entered.currency != display

    /** True si el usuario cambio la tasa y el enlace "Usar sugerida" tiene sentido. */
    val isEdited: Boolean get() = editedRate != null

    /** La tasa que se usa: la del usuario si la escribio, y si no la vigente. */
    val rate: BigDecimal? get() = editedRate ?: suggestedRate

    /**
     * La equivalencia que se guardara. En la moneda principal es el propio importe; en otra es
     * el importe por la tasa. Sin tasa no hay equivalencia, y entonces no se puede guardar.
     */
    val base: BigDecimal
        get() = when {
            !isForeign -> entered.amount
            else -> rate?.multiply(entered.amount, ExchangeRates.PRECISION) ?: BigDecimal.ZERO
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
            editedRate: BigDecimal? = null,
        ): ConversionSuggestion = ConversionSuggestion(
            entered = entered,
            display = display,
            suggestedRate = rates.rateOf(entered.currency, display),
            editedRate = editedRate,
        )
    }
}
