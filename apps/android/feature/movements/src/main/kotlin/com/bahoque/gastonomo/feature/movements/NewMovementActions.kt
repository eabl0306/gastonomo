package com.bahoque.gastonomo.feature.movements

import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import java.time.LocalDate

/**
 * Lo que el usuario puede hacer en el formulario de Nuevo movimiento, en un solo objeto, para
 * que la pantalla se pueda pintar en un test o en una captura sin ViewModel.
 */
data class NewMovementActions(
    val onKind: (MovementKind) -> Unit,
    val onAmount: (String) -> Unit,
    val onRate: (String?) -> Unit,
    val onCategory: (CategoryId) -> Unit,
    val onDate: (LocalDate) -> Unit,
    val onNote: (String) -> Unit,
    val onCurrencySheet: (Boolean) -> Unit,
    val onCurrencyQuery: (String) -> Unit,
    val onCurrency: (CurrencyCode) -> Unit,
    val onSave: () -> Unit,
    val onClose: () -> Unit,
) {
    companion object {
        /** Unas acciones que no hacen nada, para tests y capturas. */
        val NONE = NewMovementActions(
            onKind = {},
            onAmount = {},
            onRate = {},
            onCategory = {},
            onDate = {},
            onNote = {},
            onCurrencySheet = {},
            onCurrencyQuery = {},
            onCurrency = {},
            onSave = {},
            onClose = {},
        )
    }
}
