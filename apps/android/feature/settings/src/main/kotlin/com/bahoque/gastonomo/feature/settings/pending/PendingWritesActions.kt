package com.bahoque.gastonomo.feature.settings.pending

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.core.application.port.PendingDraft
import com.bahoque.gastonomo.core.application.port.PendingWrite
import com.bahoque.gastonomo.feature.settings.R

/** Lo que se puede hacer en la pantalla de registros sin enviar. */
@Immutable
data class PendingWritesActions(
    val onBack: () -> Unit,
    val onEdit: (PendingWrite) -> Unit,
    val onAmount: (String) -> Unit,
    val onCancelEdit: () -> Unit,
    val onSave: () -> Unit,
    val onAskDiscard: (PendingWrite) -> Unit,
    val onCancelDiscard: () -> Unit,
    val onConfirmDiscard: () -> Unit,
    val onRetry: () -> Unit,
)

/** Como se llama cada tipo de registro en la lista. */
internal fun labelOf(write: PendingWrite): Int = when (val draft = write.draft) {
    is PendingDraft.Movement ->
        if (draft.value.kind.isIncome) R.string.pending_movement_in else R.string.pending_movement_out
    is PendingDraft.Loan -> R.string.pending_loan
    is PendingDraft.Payment -> R.string.pending_payment
}
