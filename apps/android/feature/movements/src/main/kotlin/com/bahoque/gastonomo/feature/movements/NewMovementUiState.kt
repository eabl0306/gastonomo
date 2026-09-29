package com.bahoque.gastonomo.feature.movements

import com.bahoque.gastonomo.core.application.error.FieldError
import com.bahoque.gastonomo.core.designsystem.format.AmountInput
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.ConversionSuggestion
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import java.math.BigDecimal
import java.time.LocalDate

/**
 * El formulario de Nuevo movimiento.
 *
 * Los importes se guardan en crudo (`amountRaw`, `rateRaw`), con punto decimal y sin separador
 * de miles: lo que se ensena lo construye el campo con el formato del idioma. Asi lo que se
 * teclea y lo que se manda a la API no dependen de como se pinte.
 *
 * @property rateRaw la tasa que escribio el usuario. null significa usar la sugerida,
 *   y cambiar la moneda del formulario lo vuelve a poner a null.
 */
data class NewMovementUiState(
    val today: LocalDate,
    val kind: MovementKind,
    val date: LocalDate,
    val amountRaw: String = "",
    val currency: CurrencyCode = CurrencyCode.COP,
    val displayCurrency: CurrencyCode = CurrencyCode.COP,
    val rateRaw: String? = null,
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: CategoryId? = null,
    val rates: ExchangeRates = ExchangeRates.EMPTY,
    val currencies: List<CurrencyInfo> = emptyList(),
    val currencySheetOpen: Boolean = false,
    val currencyQuery: String = "",

    /**
     * Si se puede registrar en otra moneda. Sin conexion, o sin ninguna pasada de tasas guardada,
     * no: guardar un importe sin conocer su equivalencia romperia la regla de los cuatro campos.
     */
    val canConvert: Boolean = true,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: MovementsError? = null,
    val fieldErrors: List<FieldError> = emptyList(),
) {
    /** El importe que teclo el usuario. Una cadena a medio teclear vale cero. */
    val amount: BigDecimal get() = AmountInput.parse(amountRaw)

    /** La caja de conversion: solo se ensena si la moneda no es la principal. */
    val suggestion: ConversionSuggestion
        get() = ConversionSuggestion.of(
            entered = Money(amount, currency),
            display = displayCurrency,
            rates = rates,
            editedRate = rateRaw?.let(AmountInput::parse),
        )

    /**
     * Las categorias que se ofrecen: las del tipo del movimiento mas las de tipo `both`, que
     * son Prestamo, Abono prestamo y Otros.
     */
    val offeredCategories: List<Category>
        get() = categories.filter { it.kind.accepts(kind) }

    /** Guardar se habilita con importe mayor que 0, equivalencia mayor que 0 y categoria. */
    val canSave: Boolean
        get() = suggestion.isValid && selectedCategoryId != null && !saving

    /** El mensaje que manda la API para un campo, ya en castellano: no se traduce. */
    fun messageFor(field: String): String? =
        fieldErrors.firstOrNull { it.field == field }?.message
}
