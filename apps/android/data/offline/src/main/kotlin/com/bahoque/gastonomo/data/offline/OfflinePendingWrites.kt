package com.bahoque.gastonomo.data.offline

import com.bahoque.gastonomo.core.application.port.PendingDraft
import com.bahoque.gastonomo.core.application.port.PendingWrite
import com.bahoque.gastonomo.core.application.port.PendingWritesGateway
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPaymentId
import com.bahoque.gastonomo.core.domain.movement.MovementId
import com.bahoque.gastonomo.data.database.local.LocalStore
import kotlinx.coroutines.flow.Flow

/**
 * La cola vista desde fuera: lo que la pantalla de registros sin enviar necesita.
 *
 * Lo que mas importa aqui es [discard]: tirar la escritura sin tirar tambien su registro
 * provisional dejaria un movimiento fantasma contando en el balance para siempre, sin ninguna
 * forma de quitarlo.
 */
internal class OfflinePendingWrites(
    private val store: LocalStore,
    private val uploader: PendingUploader,
) : PendingWritesGateway {

    override fun observe(): Flow<List<PendingWrite>> = store.pending.observe()

    override suspend fun correct(id: String, draft: PendingDraft) {
        store.pending.replaceDraft(id, draft)
        uploader.uploadAll()
    }

    override suspend fun discard(id: String) {
        val write = store.pending.all().firstOrNull { it.id == id } ?: return
        forgetRecordOf(write)
        store.pending.remove(id)
    }

    override suspend fun retry() {
        uploader.uploadAll()
    }

    /** Quita el registro provisional, y el movimiento que acompana a un prestamo o a un abono. */
    private suspend fun forgetRecordOf(write: PendingWrite) {
        when (write.draft) {
            is PendingDraft.Movement -> store.movements.forget(MovementId(write.id))
            is PendingDraft.Loan -> {
                store.loans.forget(LoanId(write.id))
                store.movements.forget(MovementId(OfflineQueue.sideMovementId(write.id)))
            }
            is PendingDraft.Payment -> {
                store.loans.forgetPayment(LoanPaymentId(write.id))
                store.movements.forget(MovementId(OfflineQueue.sideMovementId(write.id)))
            }
        }
    }
}
