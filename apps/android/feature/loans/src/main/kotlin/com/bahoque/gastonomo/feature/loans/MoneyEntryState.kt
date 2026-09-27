package com.bahoque.gastonomo.feature.loans

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.core.designsystem.format.AmountInput
import com.bahoque.gastonomo.core.domain.money.ConversionSuggestion
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.Money
import java.math.BigDecimal

/**
 * El trozo de estado que comparten Nuevo prestamo y Registrar abono: el importe tecleado,
 * la moneda del registro, la equivalencia editada a mano y el sheet de moneda abierto
 * encima.
 *
 * El importe se guarda en bruto (con punto decimal y sin miles) y no como se ve: el campo
 * lo pinta con el separador del idioma cada vez, asi que cambiar de idioma no corrompe lo
 * que el usuario llevaba escrito.
 *
 * @property baseRaw null mientras el usuario no toque la caja de conversion, que es la
 *   senal de "usa la equivalencia sugerida".
 */
@Immutable
data class MoneyEntryState(
    val amountRaw: String = "",
    val currency: CurrencyCode = CurrencyCode.COP,
    val baseRaw: String? = null,
    val currencySheetOpen: Boolean = false,
    val currencyQuery: String = "",
    /**
     * Si se puede registrar en otra moneda. Sin conexion no: guardar un importe sin conocer su
     * equivalencia romperia la regla de los cuatro campos, y las tasas guardadas pueden ser
     * viejas. El chip se apaga con una explicacion y el registro va en la moneda principal.
     */
    val canConvert: Boolean = true,
) {
    /** El importe tecleado. Una cadena vacia o a medio teclear vale cero. */
    val amount: BigDecimal get() = AmountInput.parse(amountRaw)

    /** La caja de conversion medida contra la moneda principal y las tasas de hoy. */
    fun suggestion(context: MoneyContext): ConversionSuggestion = ConversionSuggestion.of(
        entered = Money(amount, currency),
        display = context.display,
        rates = context.rates,
        editedBase = baseRaw?.let(AmountInput::parse),
    )

    fun withAmount(raw: String): MoneyEntryState = copy(amountRaw = raw)

    /** Cambiar de moneda borra la equivalencia editada y cierra el sheet. */
    fun withCurrency(code: CurrencyCode): MoneyEntryState =
        copy(currency = code, baseRaw = null, currencySheetOpen = false, currencyQuery = "")

    /** null restaura la equivalencia sugerida, que es lo que hace "Usar sugerida". */
    fun withBase(raw: String?): MoneyEntryState = copy(baseRaw = raw)

    fun withQuery(query: String): MoneyEntryState = copy(currencyQuery = query)

    /**
     * Abre o cierra el sheet de moneda. Cerrarlo no toca nada mas, asi que se vuelve al
     * formulario o al sheet de abono con todo lo escrito en su sitio.
     */
    fun withCurrencySheet(open: Boolean): MoneyEntryState =
        copy(currencySheetOpen = open, currencyQuery = if (open) "" else currencyQuery)

    /**
     * Se pierde o se recupera la conexion. Al perderla el registro vuelve a la moneda principal:
     * dejarlo en otra guardaria un importe con una equivalencia que nadie puede calcular.
     */
    fun withConnection(canConvert: Boolean, display: CurrencyCode): MoneyEntryState = when {
        canConvert -> copy(canConvert = true)
        else -> copy(canConvert = false, currency = display, baseRaw = null, currencySheetOpen = false)
    }

    companion object {
        /** Un registro nuevo empieza en la moneda principal. */
        fun of(display: CurrencyCode, canConvert: Boolean = true): MoneyEntryState =
            MoneyEntryState(currency = display, canConvert = canConvert)
    }
}
