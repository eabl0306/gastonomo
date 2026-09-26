package com.bahoque.gastonomo.core.domain.money

/**
 * Cuantos decimales ensena una moneda. La lista de monedas sin fraccion es la del
 * prototipo, copiada entera: es la que decide el formato de los importes, el maximo de
 * decimales que acepta el teclado y el epsilon con el que se da por pagado un prestamo.
 */
object CurrencyDecimals {

    /** Decimales de una moneda con fraccion. */
    const val DEFAULT = 2

    /** Decimales de una moneda sin fraccion. */
    const val NONE = 0

    private val ZERO_DECIMAL: Set<String> = (
        "COP CLP JPY KRW IDR VND PYG HUF ISK UGX IRR LAK KHR MMK LBP IQD UZS " +
            "GNF MGA RWF BIF XAF XOF KPW SYP TZS MNT KMF DJF VUV XPF SLE SOS SSP"
        ).split(' ').toSet()

    /** Decimales de [currency]: 0 en las monedas sin fraccion y 2 en el resto. */
    fun of(currency: CurrencyCode): Int =
        if (currency.value in ZERO_DECIMAL) NONE else DEFAULT

    /** True si la moneda no admite parte decimal. */
    fun hasNoFraction(currency: CurrencyCode): Boolean = of(currency) == NONE
}
