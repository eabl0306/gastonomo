package com.bahoque.gastonomo.data.cache

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Cuanto vale una respuesta guardada. Hay dos plazos, no uno:
 *
 * - Antes de [fresh] la entrada se sirve tal cual y **no se toca la red**.
 * - Entre [fresh] y [maxStale] se sirve igual, al instante, y se lanza una revalidacion en
 *   segundo plano que deja la siguiente lectura ya actualizada.
 * - Pasado [maxStale] la entrada ya no se sirve: se espera a la red. Ensenar datos de hace
 *   horas como si fueran de ahora es peor que ensenar un indicador de carga.
 *
 * [maxEntries] existe porque la clave de los movimientos lleva el periodo: quien recorre un
 * ano mes a mes genera doce entradas de hasta 500 registros cada una. Sin tope, la cache
 * crece con la navegacion y no baja nunca.
 */
data class CachePolicy(
    val fresh: Duration,
    val maxStale: Duration,
    val maxEntries: Int,
) {
    init {
        require(fresh <= maxStale) { "fresh must not be longer than maxStale" }
        require(maxEntries > 0) { "maxEntries must be positive" }
    }

    companion object {
        /**
         * Lo que cambia porque el usuario escribe: movimientos, prestamos, categorias,
         * ajustes y perfil. Cada escritura invalida a mano lo que toca, asi que estos plazos
         * solo son la red de seguridad por si algo cambio desde otro dispositivo.
         */
        val WRITEABLE = CachePolicy(fresh = 45.seconds, maxStale = 5.minutes, maxEntries = 32)

        /**
         * El catalogo de monedas y las tasas. La API las refresca dos veces al dia, asi que
         * pedirlas mas de una vez cada media hora no aporta nada.
         */
        val CATALOG = CachePolicy(fresh = 30.minutes, maxStale = 6.hours, maxEntries = 4)
    }
}
