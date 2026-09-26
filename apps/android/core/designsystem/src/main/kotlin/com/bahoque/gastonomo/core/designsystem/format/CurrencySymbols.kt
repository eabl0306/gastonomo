package com.bahoque.gastonomo.core.designsystem.format

import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode

/**
 * Traduce un codigo de moneda al simbolo que ensena la interfaz. Las reglas son las del
 * prototipo y estan pensadas para que dos monedas distintas no se vean igual:
 *
 * - COP usa `$`, porque es la moneda principal por defecto y es su casa.
 * - USD usa `US$`.
 * - Cualquier otra cuyo simbolo tambien sea `$` usa las dos primeras letras de su codigo
 *   mas `$`: MX$, AR$, CA$, CL$.
 * - El resto usa su simbolo, y el codigo si la API no manda ninguno.
 *
 * Los simbolos salen de `GET /v1/currencies`; hasta que llegue el catalogo se usa la
 * tabla de reserva, que cubre las monedas que el sheet ensena en Frecuentes.
 */
class CurrencySymbols(private val raw: Map<String, String> = FALLBACK_SYMBOLS) {

    /** El simbolo que se pinta delante del importe. */
    fun of(currency: CurrencyCode): String {
        if (currency == CurrencyCode.USD) return DOLLAR_US
        val symbol = raw[currency.value]?.takeIf { it.isNotBlank() } ?: return currency.value
        if (symbol == DOLLAR && currency != CurrencyCode.COP) {
            return currency.value.take(PREFIX_LENGTH) + DOLLAR
        }
        return symbol
    }

    /** Anade el catalogo de la API sin perder lo que ya habia. */
    fun withCatalog(catalog: List<CurrencyInfo>): CurrencySymbols {
        if (catalog.isEmpty()) return this
        val merged = raw.toMutableMap()
        catalog.forEach { merged[it.code.value] = it.symbol }
        return CurrencySymbols(merged)
    }

    companion object {
        private const val DOLLAR = "$"
        private const val DOLLAR_US = "US$"
        private const val PREFIX_LENGTH = 2

        /**
         * Tabla de reserva. Es la del prototipo: sirve para arrancar y para el caso de
         * arranque en frio, cuando `GET /v1/currencies` devuelve la lista vacia.
         */
        val FALLBACK_SYMBOLS: Map<String, String> = mapOf(
            "COP" to "$",
            "USD" to "$",
            "EUR" to "€",
            "MXN" to "$",
            "GBP" to "£",
            "BRL" to "R$",
            "ARS" to "$",
            "CLP" to "$",
            "PEN" to "S/",
            "CAD" to "$",
            "JPY" to "¥",
            "CHF" to "CHF",
        )

        val DEFAULT = CurrencySymbols()
    }
}
