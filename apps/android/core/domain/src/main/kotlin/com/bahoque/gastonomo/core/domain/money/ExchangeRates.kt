package com.bahoque.gastonomo.core.domain.money

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate

/** Frescura de la tabla de tasas, tal como la marca `GET /v1/exchange-rates`. */
enum class RatesStatus {
    /** Tasas al dia. */
    OK,

    /** El ultimo refresco fallo y los datos pasan de 48 horas. Ajustes pinta "Sin conexion". */
    STALE,

    /** Todavia no hay ninguna pasada guardada: la API responde 503 y esto es lo que queda. */
    UNAVAILABLE,
}

/**
 * Tabla de tasas con base USD. La API nunca devuelve importes convertidos: la conversion
 * la hace el cliente con esta tabla.
 */
data class ExchangeRates(
    val base: CurrencyCode = CurrencyCode.USD,
    val date: LocalDate? = null,
    val status: RatesStatus = RatesStatus.UNAVAILABLE,
    val rates: Map<CurrencyCode, BigDecimal> = emptyMap(),
) {
    /** True si la tabla puede convertir a y desde [currency]. */
    fun covers(currency: CurrencyCode): Boolean = rates[currency]?.signum() == 1

    /**
     * `convert(n, a, b) = n * rate[b] / rate[a]`, con las tasas en base USD. Devuelve
     * null si falta alguna de las dos tasas, para que quien llama pueda caer al plan B.
     */
    fun convert(amount: BigDecimal, from: CurrencyCode, to: CurrencyCode): BigDecimal? {
        if (from == to) return amount
        val fromRate = rates[from]?.takeIf { it.signum() == 1 } ?: return null
        val toRate = rates[to]?.takeIf { it.signum() == 1 } ?: return null
        return amount.multiply(toRate, PRECISION).divide(fromRate, PRECISION)
    }

    /** La tasa de una unidad de [from] en [to], que es lo que ensena la caja de conversion. */
    fun rateOf(from: CurrencyCode, to: CurrencyCode): BigDecimal? =
        convert(BigDecimal.ONE, from, to)

    companion object {
        /**
         * 20 cifras significativas. Las tasas llegan con 10 decimales y las divisiones
         * encadenadas de la conversion no pueden quedarse sin resto.
         */
        val PRECISION: MathContext = MathContext(20, RoundingMode.HALF_UP)

        val EMPTY = ExchangeRates()
    }
}
