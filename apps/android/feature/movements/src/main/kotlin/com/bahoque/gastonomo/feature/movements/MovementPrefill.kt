package com.bahoque.gastonomo.feature.movements

import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import java.time.LocalDate

/**
 * Lo que el formulario trae ya escrito al abrirse.
 *
 * Solo llega con algo desde el dictado, que abre el formulario con lo que se entendio para poder
 * corregirlo. Todo es opcional **por separado**: el interprete puede acertar el importe y fallar
 * la categoria, y lo que acerto no se pierde.
 */
data class MovementPrefill(
    val amount: String = "",
    val currency: CurrencyCode? = null,
    val categoryId: CategoryId? = null,
    val note: String = "",
    val occurredOn: LocalDate? = null,
) {
    /** True si no hay nada que rellenar, que es como se abre el formulario desde el FAB. */
    val isEmpty: Boolean
        get() = amount.isBlank() && currency == null && categoryId == null &&
            note.isBlank() && occurredOn == null

    companion object {
        /** Vacio: el formulario se abre como siempre. */
        val NONE = MovementPrefill()

        /**
         * Lee los argumentos de la ruta.
         *
         * Un valor ilegible se ignora en vez de tumbar la pantalla: el formulario abierto con un
         * campo menos sigue sirviendo, y quedarse sin formulario no.
         */
        fun of(route: GastonomoRoute.NewMovement): MovementPrefill = MovementPrefill(
            amount = route.amount,
            currency = route.currency.takeIf { it.isNotBlank() }?.let(::CurrencyCode),
            categoryId = route.categoryId.takeIf { it.isNotBlank() }?.let(::CategoryId),
            note = route.note,
            occurredOn = route.occurredOn.takeIf { it.isNotBlank() }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        )
    }
}
