package com.bahoque.gastonomo.feature.loans.form

import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import java.time.LocalDate

/**
 * Lo que el formulario de Nuevo prestamo trae ya escrito al abrirse.
 *
 * Solo llega con algo desde el dictado, que abre el formulario con lo que se entendio para poder
 * corregirlo. Cada campo es opcional **por separado**: lo que el interprete acerto no se pierde
 * porque fallara otra cosa.
 */
data class LoanPrefill(
    val person: String = "",
    val direction: LoanDirection? = null,
    val amount: String = "",
    val currency: CurrencyCode? = null,
    val note: String = "",
    val openedOn: LocalDate? = null,
) {
    companion object {
        /** Vacio: el formulario se abre como siempre. */
        val NONE = LoanPrefill()

        /**
         * Lee los argumentos de la ruta. Un valor ilegible se ignora en vez de tumbar la pantalla:
         * el formulario con un campo menos sigue sirviendo, y quedarse sin formulario no.
         */
        fun of(route: GastonomoRoute.NewLoan): LoanPrefill = LoanPrefill(
            person = route.person,
            direction = LoanDirection.fromWire(route.direction),
            amount = route.amount,
            currency = route.currency.takeIf { it.isNotBlank() }?.let(::CurrencyCode),
            note = route.note,
            openedOn = route.openedOn.takeIf { it.isNotBlank() }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        )
    }
}
