package com.bahoque.gastonomo.feature.dictation

import com.bahoque.gastonomo.core.application.port.DictatedEntry
import com.bahoque.gastonomo.core.application.port.DictationDraft
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.core.navigation.GastonomoRoute

/**
 * El formulario completo con lo que se entendio, para corregirlo.
 *
 * El importe viaja como la cadena que espera el campo: punto decimal y sin separador de miles. Un
 * borrador sin importe abre el formulario con el campo vacio, que es exactamente lo que el usuario
 * tiene que rellenar.
 */
internal fun formRouteOf(draft: DictationDraft): GastonomoRoute = when (val entry = draft.entry) {
    is DictatedEntry.Movement -> GastonomoRoute.NewMovement(
        kind = entry.kind.wire,
        amount = amountOf(draft),
        currency = draft.currency.value,
        categoryId = entry.category.id.value,
        note = entry.note,
        occurredOn = draft.occurredOn.toString(),
    )
    is DictatedEntry.Loan -> GastonomoRoute.NewLoan(
        person = entry.person,
        direction = entry.direction.wire,
        amount = amountOf(draft),
        currency = draft.currency.value,
        note = entry.note,
        openedOn = draft.occurredOn.toString(),
    )
}

/** El importe ya redondeado a los decimales de su moneda, sin ceros de relleno que estorben. */
private fun amountOf(draft: DictationDraft): String = draft.amount
    ?.let { Money(it, draft.currency).rounded().amount.stripTrailingZeros().toPlainString() }
    .orEmpty()
