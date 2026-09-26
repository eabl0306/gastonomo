package com.bahoque.gastonomo.feature.home

import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.domain.period.PeriodPreset
import java.time.LocalDate

/**
 * Lo que el usuario puede hacer en Inicio, en un solo objeto.
 *
 * Asi la pantalla recibe dos parametros y se puede pintar en un test o en una captura sin
 * ViewModel: se le pasa un estado y unas acciones que no hacen nada.
 */
data class HomeActions(
    val onPreviousMonth: () -> Unit,
    val onNextOrClearRange: () -> Unit,
    val onOpenRangeSheet: () -> Unit,
    val onCloseRangeSheet: () -> Unit,
    val onPreset: (PeriodPreset) -> Unit,
    val onRangeFrom: (LocalDate) -> Unit,
    val onRangeTo: (LocalDate) -> Unit,
    val onApplyRange: () -> Unit,
    val onToggleHiddenAmounts: () -> Unit,
    val onRetry: () -> Unit,
    val onToastHidden: () -> Unit,
    val onNewMovement: (MovementKind) -> Unit,
    val onNewLoan: () -> Unit,
    val onOpenLoans: () -> Unit,
    val onOpenMovements: () -> Unit,
    val onOpenSettings: () -> Unit,
) {
    companion object {
        /** Unas acciones que no hacen nada, para tests y capturas. */
        val NONE = HomeActions(
            onPreviousMonth = {},
            onNextOrClearRange = {},
            onOpenRangeSheet = {},
            onCloseRangeSheet = {},
            onPreset = {},
            onRangeFrom = {},
            onRangeTo = {},
            onApplyRange = {},
            onToggleHiddenAmounts = {},
            onRetry = {},
            onToastHidden = {},
            onNewMovement = {},
            onNewLoan = {},
            onOpenLoans = {},
            onOpenMovements = {},
            onOpenSettings = {},
        )
    }
}
