package com.bahoque.gastonomo.core.designsystem.format

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.CurrencyDecimals
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * El teclado de importes de los tres formularios. Es la traduccion literal de `numIn`,
 * `showIn` y `toRaw` del prototipo, con [BigDecimal] en vez de Double.
 *
 * Hay dos representaciones y conviene no mezclarlas:
 * - **raw**: lo que se guarda en el estado. Siempre con punto como separador decimal,
 *   sin separador de miles. Es lo que [parse] convierte a numero.
 * - **display**: lo que se pinta en el campo, con el separador decimal del idioma y
 *   agrupacion de miles.
 *
 * ```
 * val raw = formats.amountInput.sanitize(typed, currency)   // "1250000.5"
 * val shown = formats.amountInput.display(raw)              // "1.250.000,5"
 * val value = AmountInput.parse(raw)                        // BigDecimal("1250000.5")
 * ```
 */
class AmountInput internal constructor(
    private val decimalSeparator: Char,
    private val groupingFormatter: (BigDecimal) -> String,
) {

    /**
     * Deja solo digitos y, como maximo, un separador decimal. Recorta los decimales a los
     * de la moneda, no admite separador en las monedas sin fraccion, quita los ceros a la
     * izquierda y corta la parte entera a 11 digitos.
     */
    fun sanitize(typed: String, currency: CurrencyCode): String =
        sanitize(typed, CurrencyDecimals.of(currency))

    /**
     * Lo mismo para una **tasa**, que no es un importe y no se rige por los decimales de la
     * moneda. Con las reglas de importe, una tasa hacia una moneda sin fraccion como el peso
     * colombiano se quedaria en numeros enteros y teclear 3199,41 daria 3199: el registro se
     * guardaria con otra equivalencia y nada avisaria.
     */
    fun sanitizeRate(typed: String): String = sanitize(typed, MAX_RATE_DECIMALS)

    private fun sanitize(typed: String, maxDecimals: Int): String {
        val integer = StringBuilder()
        val fraction = StringBuilder()
        var separatorSeen = false
        for (character in typed) {
            when {
                !character.isDigit() ->
                    if (character == decimalSeparator && maxDecimals > 0) separatorSeen = true
                !separatorSeen -> integer.append(character)
                fraction.length < maxDecimals -> fraction.append(character)
            }
        }
        val head = integer.toString().trimStart('0').take(MAX_INTEGER_DIGITS)
            .ifEmpty { if (separatorSeen) "0" else "" }
        return if (separatorSeen) "$head.$fraction" else head
    }

    /** Pinta un valor raw con el separador y la agrupacion del idioma. */
    fun display(raw: String): String {
        if (raw.isEmpty()) return ""
        val parts = raw.split('.')
        val grouped = groupingFormatter(parts[0].toBigDecimalOrNull() ?: BigDecimal.ZERO)
        return if (parts.size > 1) grouped + decimalSeparator + parts[1] else grouped
    }

    /** El separador decimal del idioma, que es el que el campo deja teclear. */
    fun separator(): Char = decimalSeparator

    companion object {
        /** Maximo de digitos de la parte entera. */
        const val MAX_INTEGER_DIGITS = 11

        /**
         * Decimales que se admiten al teclear una tasa. Seis es lo que hace falta para las
         * tasas menores que uno, que son las que mas decimales necesitan, y es el mismo tope
         * con el que se pintan.
         */
        const val MAX_RATE_DECIMALS = 6

        /** Lee un valor raw. Una cadena vacia o a medio teclear vale cero. */
        fun parse(raw: String): BigDecimal =
            raw.removeSuffix(".").toBigDecimalOrNull() ?: BigDecimal.ZERO

        /**
         * Donde dejar el cursor en [shown] para que quede justo detras del digito numero
         * [digits], contando desde la izquierda.
         *
         * Hace falta porque el campo reescribe el texto en cada pulsacion: teclear un digito
         * puede meter un separador de miles y correr todo lo que hay a la derecha. Sin esto el
         * cursor se iba al final y editar el medio de un importe era imposible.
         */
        fun caretAfter(shown: String, digits: Int): Int {
            if (digits <= 0) return 0
            var seen = 0
            shown.forEachIndexed { index, character ->
                if (character.isDigit()) {
                    seen++
                    if (seen == digits) return index + 1
                }
            }
            return shown.length
        }

        /**
         * Convierte un numero calculado en un valor raw, redondeado a los decimales de la
         * moneda y sin ceros de relleno. Es lo que rellena la caja de conversion y los
         * chips "Todo el saldo" y "La mitad".
         */
        fun toRaw(value: BigDecimal, currency: CurrencyCode): String =
            toRaw(value, CurrencyDecimals.of(currency))

        /** Lo mismo para una tasa, con sus propios decimales. */
        fun rateToRaw(value: BigDecimal): String = toRaw(value, MAX_RATE_DECIMALS)

        private fun toRaw(value: BigDecimal, decimals: Int): String {
            val scaled = value.setScale(decimals, RoundingMode.HALF_UP)
            if (scaled.signum() == 0) return "0"
            return scaled.stripTrailingZeros().toPlainString()
        }
    }
}
