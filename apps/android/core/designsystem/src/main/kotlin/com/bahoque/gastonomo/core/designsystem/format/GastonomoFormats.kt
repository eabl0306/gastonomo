package com.bahoque.gastonomo.core.designsystem.format

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.CurrencyDecimals
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import java.math.BigDecimal
import java.util.Locale

/**
 * Todo el formato que ensena la aplicacion: dinero, tasas, fechas y la entrada de
 * importes. Es el sitio donde mas facil es que cada pantalla se desvie, asi que no hay
 * nada de esto suelto en ningun composable.
 *
 * Se lee del tema y no se construye a mano:
 * ```
 * val formats = GastonomoTheme.formats
 * Text(formats.amount(movement.money.original))          // "US$ 140,00"
 * Text(formats.signed(shown, movement.kind.isIncome))    // "+ 450.000" con el signo correcto
 * Text(formats.dates.short(movement.occurredOn))          // "22 sept"
 * ```
 */
@Immutable
class GastonomoFormats(
    val language: AppLanguage = AppLanguage.DEFAULT,
    val displayCurrency: CurrencyCode = CurrencyCode.COP,
    private val symbols: CurrencySymbols = CurrencySymbols.DEFAULT,
) {
    private val numbers = LocalizedNumbers(Locale.forLanguageTag(language.localeTag))

    /** Fechas en el idioma vigente. */
    val dates: DateFormats = DateFormats(language)

    /** El teclado de importes en el idioma vigente. */
    val amountInput: AmountInput = AmountInput(numbers.decimalSeparator) { numbers.fixed(it, 0) }

    /** Nombres de moneda en el idioma vigente, para el sheet de moneda y el chip. */
    val currencyNames: CurrencyNames = CurrencyNames(Locale.forLanguageTag(language.localeTag))

    /**
     * "$ 186.400" o "US$ 140,00". El simbolo va delante con un espacio y los decimales
     * son los de la moneda. El importe sale siempre en valor absoluto: el signo lo pone
     * [signed] o [net].
     */
    fun amount(value: BigDecimal, currency: CurrencyCode = displayCurrency): String =
        symbols.of(currency) + " " + numbers.fixed(value.abs(), CurrencyDecimals.of(currency))

    /** Lo mismo para un [Money], que ya lleva su moneda. */
    fun amount(money: Money): String = amount(money.amount, money.currency)

    /**
     * El importe con su signo: "+" en un ingreso y el menos U+2212 en un gasto. Nunca el
     * guion normal, que en la tipografia del diseno queda mas corto y desalineado.
     */
    fun signed(value: BigDecimal, isIncome: Boolean, currency: CurrencyCode = displayCurrency): String =
        (if (isIncome) PLUS else MINUS) + amount(value, currency)

    /** Un neto o un balance: "+" si no es negativo y el menos U+2212 si lo es. */
    fun net(value: BigDecimal, currency: CurrencyCode = displayCurrency): String =
        signed(value, value.signum() >= 0, currency)

    /** Un balance negativo lleva el menos delante; uno positivo no lleva nada. */
    fun balance(value: BigDecimal, currency: CurrencyCode = displayCurrency): String =
        (if (value.signum() < 0) MINUS else "") + amount(value, currency)

    /** "$ ••••••", lo que ensena la tarjeta de balance con el ojo cerrado. */
    fun masked(currency: CurrencyCode = displayCurrency): String =
        symbols.of(currency) + " " + MASK

    /**
     * Una tasa de cambio: 6 decimales si es menor que 1, 4 si es menor que 100 y 2 en el
     * resto. Sin ceros de relleno, igual que el prototipo.
     */
    fun rate(value: BigDecimal): String = numbers.upTo(value, rateDecimals(value))

    /** El simbolo de una moneda, para pintarlo separado del numero. */
    fun symbolOf(currency: CurrencyCode): String = symbols.of(currency)

    /** Copia con el catalogo de simbolos de la API ya aplicado. */
    fun withSymbols(updated: CurrencySymbols): GastonomoFormats =
        GastonomoFormats(language, displayCurrency, updated)

    private fun rateDecimals(value: BigDecimal): Int = when {
        value < BigDecimal.ONE -> RATE_DECIMALS_SMALL
        value < ONE_HUNDRED -> RATE_DECIMALS_MEDIUM
        else -> RATE_DECIMALS_LARGE
    }

    companion object {
        /** El menos tipografico U+2212. El guion normal no se usa en ningun importe. */
        const val MINUS = "−"

        /** El mas de un ingreso. */
        const val PLUS = "+"

        /** Lo que sustituye al importe cuando el ojo esta cerrado. */
        const val MASK = "••••••"

        private const val RATE_DECIMALS_SMALL = 6
        private const val RATE_DECIMALS_MEDIUM = 4
        private const val RATE_DECIMALS_LARGE = 2
        private val ONE_HUNDRED = BigDecimal(100)
    }
}

/** Lo publica [com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme]. */
val LocalGastonomoFormats = staticCompositionLocalOf { GastonomoFormats() }
