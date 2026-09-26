package com.bahoque.gastonomo.core.domain.currency

import com.bahoque.gastonomo.core.domain.money.CurrencyCode

/**
 * Una fila del sheet de moneda. La API ya filtra el catalogo: solo llegan las monedas con
 * tasa, sin metales ni XDR.
 *
 * @property symbol el simbolo que manda la API, que puede venir vacio.
 */
data class CurrencyInfo(
    val code: CurrencyCode,
    val name: String,
    val symbol: String,
)
