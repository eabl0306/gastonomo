package com.bahoque.gastonomo.core.domain.money

/**
 * Codigo ISO 4217 de tres letras. La API los manda siempre en mayusculas y las
 * comparaciones de todo el dominio dependen de eso, asi que se normaliza al construirlo.
 */
@JvmInline
value class CurrencyCode(val value: String) {

    init {
        require(value.length == LENGTH && value.all { it in 'A'..'Z' }) {
            "A currency code is three uppercase letters, got '$value'"
        }
    }

    override fun toString(): String = value

    companion object {
        private const val LENGTH = 3

        /** Moneda principal por defecto de una cuenta nueva. */
        val COP = CurrencyCode("COP")

        /** Base de las tasas de cambio. */
        val USD = CurrencyCode("USD")

        val EUR = CurrencyCode("EUR")
        val MXN = CurrencyCode("MXN")

        /** Las cuatro que el sheet de moneda ensena en la seccion Frecuentes, en orden. */
        val FREQUENT = listOf(COP, USD, EUR, MXN)

        /** Acepta minusculas y espacios; devuelve null si no es un codigo valido. */
        fun parseOrNull(raw: String): CurrencyCode? {
            val normalized = raw.trim().uppercase()
            return runCatching { CurrencyCode(normalized) }.getOrNull()
        }
    }
}
