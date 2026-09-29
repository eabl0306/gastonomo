package com.bahoque.gastonomo.feature.movements

import com.bahoque.gastonomo.core.domain.period.PeriodPreset
import java.time.LocalDate

/**
 * Lo que el usuario puede hacer en la lista de Movimientos, en un solo objeto, para que la
 * pantalla se pueda pintar en un test o en una captura sin ViewModel.
 */
data class MovementsActions(
    val onPreviousMonth: () -> Unit,
    val onNextOrClearRange: () -> Unit,
    val onOpenRangeSheet: () -> Unit,
    val onCloseRangeSheet: () -> Unit,
    val onPreset: (PeriodPreset) -> Unit,
    val onRangeFrom: (LocalDate) -> Unit,
    val onRangeTo: (LocalDate) -> Unit,
    val onApplyRange: () -> Unit,
    val onFilter: (MovementFilter) -> Unit,
    val onRetry: () -> Unit,
    val onToastHidden: () -> Unit,
    val onNewMovement: () -> Unit,
    val onNewLoan: () -> Unit,
    val onDictate: () -> Unit,
) {
    companion object {
        /** Unas acciones que no hacen nada, para tests y capturas. */
        val NONE = MovementsActions(
            onPreviousMonth = {},
            onNextOrClearRange = {},
            onOpenRangeSheet = {},
            onCloseRangeSheet = {},
            onPreset = {},
            onRangeFrom = {},
            onRangeTo = {},
            onApplyRange = {},
            onFilter = {},
            onRetry = {},
            onToastHidden = {},
            onNewMovement = {},
            onNewLoan = {},
            onDictate = {},
        )
    }
}
